package com.coralstay.orchestrator.spawn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@code claude -p} 헤드리스 프로세스를 띄우고 기다리는 스포너 (RFC 06절 Agent Spawner).
 *
 * <p>{@code --bare}는 절대 쓰지 않는다(RFC 11절①, decision-5) — bare 모드는 훅을 건너뛰므로 git-logbook 트레일러와
 * claude-code-agile-hooks 게이트가 함께 꺼진다.
 *
 * <p>BacklogClient의 실행기({@code Function<List<String>, ProcessResult>})는 끝난 결과만 돌려주므로 쓰지 않는다. 스포너는
 * 살아 있는 Process가 필요하다(PID 노출, 타임아웃, 자손 종료). 대신 실행 파일 경로를 주입받아 테스트는 셸 스크립트로 대체한다.
 */
public final class AgentSpawner {

    /**
     * 한 세션의 실행 결과.
     *
     * @param exitCode 종료코드 (타임아웃으로 죽였으면 신호 종료 코드, 보통 143/137)
     * @param timedOut 제한 시간 안에 끝나지 않아 스포너가 프로세스와 자손을 종료시켰는가
     * @param duration 시작부터 종료(또는 강제 종료)까지
     */
    public record AgentResult(
            int exitCode, String stdout, String stderr, long pid, boolean timedOut, Duration duration) {}

    /**
     * 시작된 세션 핸들. {@link #start}가 돌아오는 시점에 프로세스는 이미 떠 있어 PID를 바로 읽을 수 있다 — 세션
     * 레지스트리(AO-5)는 {@link #process()}로 추적·회수하고, 실행 흐름(AO-7)은 {@link #await()}로 결과를 받는다.
     */
    public static final class SpawnedAgent {
        private final String role;
        private final ProcessHandle process;
        private final Instant startedAt;
        private final CompletableFuture<AgentResult> result;

        private SpawnedAgent(
                String role, ProcessHandle process, Instant startedAt, CompletableFuture<AgentResult> result) {
            this.role = role;
            this.process = process;
            this.startedAt = startedAt;
            this.result = result;
        }

        public String role() {
            return role;
        }

        public long pid() {
            return process.pid();
        }

        public ProcessHandle process() {
            return process;
        }

        public Instant startedAt() {
            return startedAt;
        }

        /** 프로세스 종료(또는 타임아웃 처리)까지 기다려 결과를 돌려준다. 여러 번 불러도 같은 결과. */
        public AgentResult await() {
            try {
                return result.join();
            } catch (CompletionException e) {
                throw e.getCause() instanceof RuntimeException r ? r : e;
            }
        }
    }

    public static final String DEFAULT_EXECUTABLE = "claude";

    /**
     * 기본 권한 모드 {@code auto}.
     *
     * <p>헤드리스 세션은 승인 프롬프트에 답할 사람이 없으므로 묻지 않고 진행할 모드가 필요하다. {@code auto}는 분류기가
     * 승인을 대신해 멈추지 않으면서도 {@code bypassPermissions}(= {@code --dangerously-skip-permissions})처럼 검사를
     * 통째로 끄지 않는다. {@code acceptEdits}는 파일 편집만 자동 승인하므로 Implementer가 돌려야 하는
     * {@code ./gradlew test}·{@code git commit}이 거부될 수 있다. 최종 선택은 실제 claude로 돌리는 E2E(AO-8)에서 확인한다.
     */
    public static final String DEFAULT_PERMISSION_MODE = "auto";

    private final String executable;
    private final String permissionMode;

    /** {@code claude}(PATH 탐색) + {@link #DEFAULT_PERMISSION_MODE}. */
    public AgentSpawner() {
        this(DEFAULT_EXECUTABLE, DEFAULT_PERMISSION_MODE);
    }

    /**
     * @param executable claude 실행 파일 경로 또는 PATH상 이름 (테스트는 가짜 스크립트)
     * @param permissionMode {@code --permission-mode} 값 (claude 2.1.x 선택지: acceptEdits, auto, bypassPermissions,
     *     manual, dontAsk, plan)
     */
    public AgentSpawner(String executable, String permissionMode) {
        this.executable = requireText(executable, "executable");
        this.permissionMode = requireText(permissionMode, "permissionMode");
    }

    /**
     * {@code [executable, -p, <prompt>, --permission-mode, <mode>, --permission-prompts, none]}.
     *
     * <p>프롬프트는 셸을 거치지 않고 argv 원소 하나로 넘긴다 — 따옴표·개행·{@code $}가 그대로 전달된다.
     * {@code --permission-prompts none}은 권한 모드가 허용하지 않은 동작을 묻지 않고 거부하게 해, 헤드리스 세션이 답할 사람
     * 없는 프롬프트에서 멈추지 않게 한다.
     *
     * @param role 에이전트 역할 (스켈레톤은 Implementer 단독이라 아직 argv에 반영하지 않는다)
     */
    public List<String> command(String role, String prompt) {
        requireText(role, "role");
        Objects.requireNonNull(prompt, "prompt");
        return List.of(
                executable,
                "-p", prompt,
                "--permission-mode", permissionMode,
                "--permission-prompts", "none");
    }

    /**
     * 프로세스를 띄우고 핸들을 바로 돌려준다. 대기·타임아웃 처리는 virtual thread에서 진행된다.
     *
     * <p>timeout이 지나면 프로세스와 자손 전부를 종료하고 timedOut=true인 결과를 낸다({@link #terminate}).
     *
     * <p>stdin은 즉시 닫는다 — {@code claude -p}는 파이프된 stdin을 프롬프트로 읽으려 기다릴 수 있다. stdout·stderr는
     * 각각 별도 virtual thread로 끝까지 읽어, 출력이 커도 파이프 버퍼가 차서 프로세스가 멈추지 않게 한다.
     */
    public SpawnedAgent start(String role, Path cwd, String prompt, Duration timeout) {
        Objects.requireNonNull(cwd, "cwd");
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout은 양수여야 합니다: " + timeout);
        }
        List<String> command = command(role, prompt);
        Process process;
        Instant startedAt = Instant.now();
        try {
            process = new ProcessBuilder(command).directory(cwd.toFile()).start();
            process.getOutputStream().close();
        } catch (IOException e) {
            throw new UncheckedIOException("에이전트 프로세스 시작 실패: " + command.getFirst(), e);
        }
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();
        Thread outDrain = Thread.ofVirtual().name("agent-stdout-" + process.pid())
                .start(() -> drain(process.getInputStream(), stdout));
        Thread errDrain = Thread.ofVirtual().name("agent-stderr-" + process.pid())
                .start(() -> drain(process.getErrorStream(), stderr));

        var result = new CompletableFuture<AgentResult>();
        Thread.ofVirtual().name("agent-wait-" + process.pid()).start(() -> {
            try {
                boolean exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!exited) {
                    terminate(process.toHandle());
                }
                int exitCode = process.waitFor();
                Duration duration = Duration.between(startedAt, Instant.now());
                outDrain.join(DRAIN_GRACE);
                errDrain.join(DRAIN_GRACE);
                result.complete(new AgentResult(
                        exitCode, text(stdout), text(stderr), process.pid(), !exited, duration));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                terminate(process.toHandle());
                result.completeExceptionally(new IllegalStateException("에이전트 대기 중 인터럽트", e));
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        });
        return new SpawnedAgent(role, process.toHandle(), startedAt, result);
    }

    /** {@link #start} 후 {@link SpawnedAgent#await()}. */
    public AgentResult spawn(String role, Path cwd, String prompt, Duration timeout) {
        return start(role, cwd, prompt, timeout).await();
    }

    /**
     * 프로세스가 끝난 뒤 drain 스레드를 기다리는 한도. 백그라운드 자손이 파이프를 물고 남으면 EOF가 오지 않으므로, 그때는
     * 그때까지 읽은 출력만 담는다.
     */
    private static final Duration DRAIN_GRACE = Duration.ofSeconds(2);

    /** 정상 종료(SIGTERM) 후 강제 종료(SIGKILL)로 넘어가기 전 기다리는 시간. */
    static final Duration TERMINATE_GRACE = Duration.ofMillis(500);

    /**
     * 프로세스와 그 자손 전부를 종료한다: destroy → {@link #TERMINATE_GRACE} 대기 → 남은 것은 destroyForcibly → 다시
     * {@link #TERMINATE_GRACE}까지 종료를 기다린다. 타임아웃 처리와 세션 레지스트리 회수(AO-5)가 함께 쓰는 단일 구현이다.
     *
     * <p>자손 목록은 부모를 죽이기 <em>전에</em> 떠 둔다 — 부모가 먼저 죽으면 자손이 init으로 재부모화되어
     * {@code descendants()}로 더는 찾을 수 없다. 같은 이유로 root가 <em>이미 끝난</em> 뒤에 부르면 남은 자손을 찾지
     * 못한다(정상 종료 후 백그라운드 자손). 스냅샷 이후 새로 생긴 자손도 놓칠 수 있다(스켈레톤 범위 밖).
     *
     * @return 대상(스냅샷한 자손 + root)이 모두 종료됐는가 — SIGKILL 후에도 유예 안에 안 끝난 것이 있으면 false
     */
    public static boolean terminate(ProcessHandle root) {
        List<ProcessHandle> all = new ArrayList<>(root.descendants().toList());
        all.add(root);
        all.forEach(ProcessHandle::destroy);
        awaitExit(all, TERMINATE_GRACE);
        List<ProcessHandle> survivors = all.stream().filter(ProcessHandle::isAlive).toList();
        survivors.forEach(ProcessHandle::destroyForcibly);
        return awaitExit(survivors, TERMINATE_GRACE);
    }

    /** 모두 끝나거나 grace가 지날 때까지 기다린 뒤, 모두 끝났는지 돌려준다. */
    private static boolean awaitExit(List<ProcessHandle> handles, Duration grace) {
        long deadline = System.nanoTime() + grace.toNanos();
        for (ProcessHandle handle : handles) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                break;
            }
            try {
                handle.onExit().get(remaining, TimeUnit.NANOSECONDS);
            } catch (TimeoutException | ExecutionException e) {
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return handles.stream().noneMatch(ProcessHandle::isAlive);
    }

    private static void drain(InputStream in, ByteArrayOutputStream sink) {
        byte[] buffer = new byte[8192];
        try (in) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                synchronized (sink) {
                    sink.write(buffer, 0, n);
                }
            }
        } catch (IOException e) {
            // 프로세스가 죽으며 스트림이 닫힌 경우 — 그때까지 읽은 것만 남긴다
        }
    }

    private static String text(ByteArrayOutputStream sink) {
        synchronized (sink) {
            return sink.toString(StandardCharsets.UTF_8);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "가 비어 있습니다");
        }
        return value;
    }
}
