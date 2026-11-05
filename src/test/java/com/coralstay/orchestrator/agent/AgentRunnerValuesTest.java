// AO-32: AgentRunner의 기본 run · Tokens · Session 값 규칙을 덮는다(ClaudeCodeRunner 스텁은 측정 제외).
package com.coralstay.orchestrator.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.agent.AgentRunner.AgentRun;
import com.coralstay.orchestrator.agent.AgentRunner.RunningAgent;
import com.coralstay.orchestrator.agent.AgentRunner.Session;
import com.coralstay.orchestrator.agent.AgentRunner.Tokens;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgentRunnerValuesTest {

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 기본 run은 start에 인자를 그대로 넘기고 그 핸들의 await 결과를 돌려준다")
    void runStartsThenAwaits() {
        AgentRun expected = new AgentRun(0, "out", "err", 42L, false, Duration.ofSeconds(1),
                Session.unavailable("no json"));
        List<Object> started = new ArrayList<>();
        AgentRunner runner = (role, cwd, prompt, model, timeout) -> {
            started.addAll(List.of(role, cwd, prompt, model, timeout));
            return new RunningAgent() {
                public String role() {
                    return role;
                }

                public long pid() {
                    return 42L;
                }

                public ProcessHandle process() {
                    return ProcessHandle.current();
                }

                public Instant startedAt() {
                    return Instant.EPOCH;
                }

                public AgentRun await() {
                    return expected;
                }
            };
        };

        AgentRun run = runner.run("implementer", Path.of("/w"), "p", "sonnet", Duration.ofMinutes(1));

        assertSame(expected, run);
        assertEquals(List.of("implementer", Path.of("/w"), "p", "sonnet", Duration.ofMinutes(1)), started);
    }

    @Test
    @DisplayName("AO-32#3 Tokens는 0은 받고 in · out 어느 쪽이든 음수면 거부한다")
    void tokensRejectNegative() {
        assertEquals(0L, new Tokens(0, 0).in());
        assertThrows(IllegalArgumentException.class, () -> new Tokens(-1, 0));
        var e = assertThrows(IllegalArgumentException.class, () -> new Tokens(0, -1));
        assertTrue(e.getMessage().contains("out=-1"), e.getMessage());
    }

    @Test
    @DisplayName("AO-32#3 Session.of는 읽은 세션, unavailable은 사유만 있는 세션이다")
    void sessionAvailability() {
        Session read = Session.of("s-1", new Tokens(10, 2));
        assertTrue(read.available());
        assertNull(read.unavailableReason());

        Session missing = Session.unavailable("파싱 실패");
        assertFalse(missing.available());
        assertNull(missing.sessionId());
        assertNull(missing.tokens());
        assertEquals("파싱 실패", missing.unavailableReason());
    }

    @Test
    @DisplayName("AO-32#3 Session 팩토리는 null 값을 받지 않는다")
    void sessionFactoriesRejectNull() {
        assertThrows(NullPointerException.class, () -> Session.of(null, new Tokens(1, 1)));
        assertThrows(NullPointerException.class, () -> Session.of("s", null));
        assertThrows(NullPointerException.class, () -> Session.unavailable(null));
    }

    // == Coverage ==
}
