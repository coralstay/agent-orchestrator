package com.coralstay.orchestrator.agent;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 에이전트 실행 포트 (decision-7). 구현은 Claude Code 하나({@link ClaudeCodeRunner})뿐이며, 포트는 "띄우고 기다려 결과를
 * 받는다"만 약속한다. 에이전트 안의 규칙은 claude-code-agile-hooks가 강제한다.
 *
 * <p>AO-19 시점에는 시그니처 스텁이다(decision-11). 실제 동작은 DRAFT-3이 spawn/AgentSpawner를 옮겨 채운다.
 */
public interface AgentRunner {

    /**
     * 프로세스를 띄우고 핸들을 바로 돌려준다. 핸들이 돌아오는 시점에 PID를 읽을 수 있다.
     *
     * @param role 에이전트 역할 (스켈레톤은 Implementer 단독)
     * @param model {@code --model} 값 (decision-10 정책 파일에서 고른 모델)
     * @param timeout 양수. 지나면 프로세스와 자손을 종료하고 {@link AgentRun#timedOut()}=true
     */
    RunningAgent start(String role, Path cwd, String prompt, String model, Duration timeout);

    /** {@link #start} 후 {@link RunningAgent#await()}. */
    default AgentRun run(String role, Path cwd, String prompt, String model, Duration timeout) {
        return start(role, cwd, prompt, model, timeout).await();
    }

    /** 시작된 세션 핸들. 세션 레지스트리는 {@link #process()}로 추적·회수하고, 실행 흐름은 {@link #await()}로 결과를 받는다. */
    interface RunningAgent {
        String role();

        long pid();

        ProcessHandle process();

        Instant startedAt();

        /** 프로세스 종료(또는 타임아웃 처리)까지 기다려 결과를 돌려준다. 여러 번 불러도 같은 결과. */
        AgentRun await();
    }

    /**
     * 한 세션의 실행 결과.
     *
     * @param exitCode 종료코드 (타임아웃으로 죽였으면 신호 종료 코드, 보통 143/137)
     * @param timedOut 제한 시간 안에 끝나지 않아 프로세스와 자손을 종료시켰는가
     * @param duration 시작부터 종료(또는 강제 종료)까지
     * @param session 헤드리스 JSON에서 읽은 세션 정보 — 읽지 못했으면 {@link Session#unavailable}
     */
    record AgentRun(
            int exitCode,
            String stdout,
            String stderr,
            long pid,
            boolean timedOut,
            Duration duration,
            Session session) {}

    /**
     * git-trail 단위 토큰 (decision-8): in = input + cache_creation + cache_read, out = output.
     *
     * <p>메모: 카드 Token Budget 쪽(card 패키지)에 같은 모양의 Tokens가 생기면 하나로 합친다.
     */
    record Tokens(long in, long out) {
        public Tokens {
            if (in < 0 || out < 0) {
                throw new IllegalArgumentException("토큰은 음수일 수 없습니다: in=" + in + " out=" + out);
            }
        }
    }

    /**
     * 세션 총량. 읽었으면 sessionId·tokens가 있고 unavailableReason은 null, 못 읽었으면 그 반대(사유만 있음).
     *
     * @param sessionId 헤드리스 JSON의 {@code session_id} (없으면 null)
     * @param tokens 세션 총량 (없으면 null)
     * @param unavailableReason 읽지 못한 사유 (읽었으면 null)
     */
    record Session(String sessionId, Tokens tokens, String unavailableReason) {
        public static Session of(String sessionId, Tokens tokens) {
            return new Session(Objects.requireNonNull(sessionId, "sessionId"),
                    Objects.requireNonNull(tokens, "tokens"), null);
        }

        public static Session unavailable(String reason) {
            return new Session(null, null, Objects.requireNonNull(reason, "reason"));
        }

        public boolean available() {
            return unavailableReason == null;
        }
    }

    /**
     * Claude Code 구현: {@code claude -p} 헤드리스 프로세스. 헤드리스 JSON 파싱은 여기에만 둔다(decision-8).
     *
     * <p>{@code --bare}·{@code --dangerously-skip-permissions}는 절대 쓰지 않는다 — 훅(claude-code-agile-hooks, git-logbook)이 꺼진다.
     */
    final class ClaudeCodeRunner implements AgentRunner {

        public static final String DEFAULT_EXECUTABLE = "claude";
        public static final String DEFAULT_PERMISSION_MODE = "auto";

        /** 헤드리스 세션이 원격에 푸시하지 못하게 막는 {@code --disallowedTools} 값. */
        public static final String DISALLOWED_TOOLS = "Bash(git push:*)";

        private final String executable;
        private final String permissionMode;

        public ClaudeCodeRunner() {
            this(DEFAULT_EXECUTABLE, DEFAULT_PERMISSION_MODE);
        }

        public ClaudeCodeRunner(String executable, String permissionMode) {
            this.executable = executable;
            this.permissionMode = permissionMode;
        }

        /**
         * {@code [executable, -p, <prompt>, --model, <m>, --permission-mode, <mode>, --output-format, json,
         * --disallowedTools, Bash(git push:*)]}. 프롬프트는 argv 원소 하나.
         */
        public List<String> command(String prompt, String model) {
            throw new UnsupportedOperationException("pending: DRAFT-3");
        }

        /**
         * 헤드리스 JSON({@code --output-format json})에서 session_id와 usage를 읽는다. 실패해도 던지지 않고
         * {@link Session#unavailable}을 돌려준다.
         */
        public static Session parseSession(String stdoutJson) {
            throw new UnsupportedOperationException("pending: DRAFT-3");
        }

        @Override
        public RunningAgent start(String role, Path cwd, String prompt, String model, Duration timeout) {
            throw new UnsupportedOperationException("pending: DRAFT-3");
        }
    }
}
