package com.coralstay.orchestrator.backlog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * backlog.md CLI({@code backlog task view <id> --plain})를 감싸는 클라이언트.
 *
 * <p>파서는 backlog 1.51.0 출력 기준이며, 포맷 변화에 덜 깨지도록 "제목 줄 + 대시 밑줄" 섹션 헤더 단위로 느슨하게 읽는다.
 */
public final class BacklogClient {

    /** 수용 기준 한 항목. {@code - [x] #1 본문}. */
    public record Criterion(int index, boolean checked, String text) {}

    /** {@code task view --plain}에서 뽑은 값 객체. dependencies는 직접 의존 태스크 ID만 담는다. */
    public record Task(
            String id,
            String title,
            String status,
            List<Criterion> acceptanceCriteria,
            List<String> dependencies) {

        public Task {
            acceptanceCriteria = List.copyOf(acceptanceCriteria);
            dependencies = List.copyOf(dependencies);
        }
    }

    /** 외부 프로세스 실행 결과. */
    public record ProcessResult(int exitCode, String stdout, String stderr) {}

    /** {@code backlog}가 0이 아닌 종료코드로 끝났을 때. stderr를 그대로 담는다. */
    public static final class BacklogException extends RuntimeException {
        private final int exitCode;
        private final String stderr;

        public BacklogException(List<String> command, int exitCode, String stderr) {
            super(String.join(" ", command) + " 실패(exit " + exitCode + "): " + stderr.strip());
            this.exitCode = exitCode;
            this.stderr = stderr;
        }

        public int exitCode() {
            return exitCode;
        }

        public String stderr() {
            return stderr;
        }
    }

    private final Function<List<String>, ProcessResult> executor;

    /** 명령 실행기를 주입받는다. 테스트는 가짜 실행기를, 운영은 {@link #processExecutor(Path)}를 넘긴다. */
    public BacklogClient(Function<List<String>, ProcessResult> executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    /** {@code backlog task view <id> --plain}을 실행해 Task로 해석한다. 종료코드≠0이면 {@link BacklogException}. */
    public Task view(String id) {
        List<String> command = List.of("backlog", "task", "view", id, "--plain");
        ProcessResult result = executor.apply(command);
        if (result.exitCode() != 0) {
            throw new BacklogException(command, result.exitCode(), result.stderr());
        }
        return parse(result.stdout());
    }

    /** workingDir에서 ProcessBuilder로 명령을 실행하는 실행기. stdout·stderr를 동시에 읽어 파이프 교착을 피한다. */
    public static Function<List<String>, ProcessResult> processExecutor(Path workingDir) {
        return command -> {
            try {
                Process process = new ProcessBuilder(command).directory(workingDir.toFile()).start();
                process.getOutputStream().close();
                try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                    Future<String> stderr = pool.submit(() -> read(process.getErrorStream()));
                    String stdout = read(process.getInputStream());
                    int exitCode = process.waitFor();
                    return new ProcessResult(exitCode, stdout, stderr.get());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted: " + command, e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("stderr read failed: " + command, e.getCause());
            }
        };
    }

    private static String read(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static final String TO_DO = "To Do";
    public static final String IN_PROGRESS = "In Progress";
    public static final String DONE = "Done";

    /** 병합 여부를 판정하는 기준 브랜치. */
    public static final String MAIN_BRANCH = "main";

    /**
     * 배정해도 되는 태스크인가(ORC-005, decision-14): 상태가 To Do이고, 직접 의존 태스크가 전부 Done이고, 미병합
     * {@code task/<ID>} 브랜치가 없을 때만 true.
     *
     * <p>착수는 미병합 {@code task/<ID>} 브랜치의 존재다 — 사람이든 에이전트 세션이든 착수하면 브랜치가 생기므로 브랜치가
     * 있는 카드는 main 보드에서 To Do로 보여도 배정하지 않는다. backlog의 {@code isReady}/{@code task list --ready}는
     * In Progress도 ready로 치므로(1.51.0 실측) 그대로 쓰지 않고 여기서 직접 판정한다.
     *
     * @param lookup 의존 태스크 ID → Task (보통 {@code this::view})
     * @param hasUnmergedTaskBranch 태스크 ID → 미병합 {@code task/<ID>} 브랜치 존재 여부(보통 {@code
     *     this::hasUnmergedTaskBranch})
     */
    public static boolean isRunnable(
            Task task, Function<String, Task> lookup, Predicate<String> hasUnmergedTaskBranch) {
        if (!TO_DO.equals(task.status())) {
            return false;
        }
        if (!task.dependencies().stream().allMatch(id -> DONE.equals(lookup.apply(id).status()))) {
            return false;
        }
        return !hasUnmergedTaskBranch.test(task.id());
    }

    /**
     * {@code task/<id>} 브랜치가 로컬({@code refs/heads}) 또는 원격({@code refs/remotes/<remote>})에 있고 {@link
     * #MAIN_BRANCH}에 병합되지 않았는가.
     *
     * <p>병합 여부는 {@code git cherry main <브랜치>}로 본다: main에 같은 패치가 없는 커밋({@code +})이 하나라도 있으면
     * 미병합이다. 커밋이 전부 main에 있거나(merge commit · fast-forward · cherry-pick · rebase 병합) main 위에 커밋이
     * 없는 브랜치는 병합된 것으로 본다. 커밋 여러 개를 squash로 병합하면 패치가 달라져 미병합으로 남는다(브랜치를 지우면
     * 풀린다). 실행기는 저장소 루트에서 명령을 실행해야 한다.
     *
     * @throws IllegalStateException git이 0이 아닌 종료코드로 끝났을 때
     */
    public boolean hasUnmergedTaskBranch(String id) {
        String local = "refs/heads/task/" + id;
        String remoteSuffix = "/task/" + id;
        List<String> refs = git("for-each-ref", "--format=%(refname)", local, "refs/remotes")
                .lines()
                .map(String::strip)
                .filter(ref -> ref.equals(local) || (ref.startsWith("refs/remotes/") && ref.endsWith(remoteSuffix)))
                .toList();
        for (String ref : refs) {
            if (git("cherry", MAIN_BRANCH, ref).lines().anyMatch(line -> line.startsWith("+"))) {
                return true;
            }
        }
        return false;
    }

    private String git(String... args) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        ProcessResult result = executor.apply(command);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(String.join(" ", command)
                    + " 실패(exit " + result.exitCode() + "): " + result.stderr().strip());
        }
        return result.stdout();
    }

    private static final Pattern TITLE = Pattern.compile("^Task (\\S+) - (.+)$");
    /** {@code Status: ○ To Do} — 앞의 상태 글리프(○ ◒ ✔ 등)는 버린다. */
    private static final Pattern STATUS =
            Pattern.compile("^Status:\\s*(?:[^\\p{L}\\p{N}\\s]+\\s+)?(.+?)\\s*$");
    private static final Pattern SECTION_RULE = Pattern.compile("^-{3,}$");
    private static final Pattern CRITERION = Pattern.compile("^- \\[([ xX])\\] #(\\d+) (.*)$");
    /** 의존성 트리의 1단계(직접 의존) 줄: {@code ├─ AO-2 - 제목 [상태]}. 들여쓴 줄은 전이 의존. */
    private static final Pattern DIRECT_DEPENDENCY = Pattern.compile("^[├└]─ (\\S+) - ");

    static final String SECTION_DEPENDENCIES = "Dependency Graph:";
    static final String SECTION_ACCEPTANCE = "Acceptance Criteria:";

    /** {@code --plain} 출력 전체를 Task로 해석한다. 부수효과 없는 순수 함수. */
    public static Task parse(String plainText) {
        List<String> lines = plainText.lines().toList();
        String id = null;
        String title = null;
        String status = null;
        for (String line : lines) {
            Matcher m;
            if (id == null && (m = TITLE.matcher(line)).matches()) {
                id = m.group(1);
                title = m.group(2).strip();
            } else if (status == null && (m = STATUS.matcher(line)).matches()) {
                status = m.group(1);
            }
        }
        if (id == null || status == null) {
            throw new IllegalArgumentException("backlog --plain 출력에서 Task/Status 줄을 찾지 못했습니다");
        }
        Map<String, List<String>> sections = sections(lines);
        return new Task(
                id,
                title,
                status,
                criteria(sections.getOrDefault(SECTION_ACCEPTANCE, List.of())),
                directDependencies(sections.getOrDefault(SECTION_DEPENDENCIES, List.of())));
    }

    /** "헤더 줄 + 대시 밑줄"로 시작하는 섹션들을 헤더 → 본문 줄로 나눈다. */
    static Map<String, List<String>> sections(List<String> lines) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        List<String> current = null;
        for (int i = 0; i < lines.size(); i++) {
            boolean isHeader = i + 1 < lines.size() && SECTION_RULE.matcher(lines.get(i + 1)).matches();
            if (isHeader) {
                current = new ArrayList<>();
                sections.put(lines.get(i).strip(), current);
                i++; // 밑줄 건너뜀
            } else if (current != null) {
                current.add(lines.get(i));
            }
        }
        return sections;
    }

    private static List<Criterion> criteria(List<String> body) {
        List<Criterion> result = new ArrayList<>();
        for (String line : body) {
            Matcher m = CRITERION.matcher(line.strip());
            if (m.matches()) {
                result.add(new Criterion(
                        Integer.parseInt(m.group(2)), !m.group(1).isBlank(), m.group(3).strip()));
            }
        }
        return result;
    }

    /** "Depends on" 블록에서 1단계 줄만 읽는다. "Dependents" 블록(역방향)은 무시한다. */
    private static List<String> directDependencies(List<String> body) {
        List<String> result = new ArrayList<>();
        boolean inDependsOn = false;
        for (String line : body) {
            if (line.startsWith("Depends on")) {
                inDependsOn = true;
                continue;
            }
            if (inDependsOn) {
                if (line.isBlank() || line.startsWith("Dependents")) {
                    break;
                }
                Matcher m = DIRECT_DEPENDENCY.matcher(line);
                if (m.find()) {
                    result.add(m.group(1));
                }
            }
        }
        return result;
    }
}
