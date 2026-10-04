package com.coralstay.orchestrator.agent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.agent.AgentRunner.ClaudeCodeRunner;
import com.coralstay.orchestrator.agent.AgentRunner.Session;
import com.coralstay.orchestrator.agent.AgentRunner.Tokens;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * DRAFT-3 (AgentRunner/ClaudeCodeRunner) 테스트 — AO-19에서 구현보다 먼저 쓴다(decision-11).
 *
 * <p>DRAFT-3#1(AgentSpawner 이동 후 기존 테스트 초록)은 구현 시점 AC다: AgentSpawnerTest를 이 패키지로 옮겨 그대로 통과시키는
 * 것이 그 검증이므로 여기 pending 테스트로 쓰지 않는다. DRAFT-3#4는 문서 AC라 테스트 대상이 아니다.
 *
 * <p>실제 claude는 띄우지 않는다. 세션 파싱은 2026-10-03 스파이크의 실제 헤드리스 출력(클래스패스
 * {@code agent/claude-headless-result.json}, 세션과 무관한 필드만 덜어냄)을 쓴다.
 */
class AgentRunnerTest {

    static final String RESOURCE = "/agent/claude-headless-result.json";

    static String resource() throws IOException {
        try (InputStream in = AgentRunnerTest.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("리소스 없음: " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String valueAfter(List<String> argv, String flag) {
        int i = argv.indexOf(flag);
        assertTrue(i >= 0 && i + 1 < argv.size(), flag + " 와 값이 있어야 한다: " + argv);
        return argv.get(i + 1);
    }

    // --- DRAFT-3#2 command() ---

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-3#2 프롬프트를 -p 뒤 argv 원소 하나로 넘긴다")
    void commandPassesPromptAsSingleArgument() {
        String prompt = "승인·승격된 태스크 AO-1을 수행하라.\n\"따옴표\" $HOME `x`";
        List<String> argv = new ClaudeCodeRunner("/opt/claude", "auto").command(prompt, "sonnet");

        assertEquals("/opt/claude", argv.getFirst());
        assertEquals(prompt, valueAfter(argv, "-p"));
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-3#2 --model·--permission-mode auto·--output-format json을 넣는다")
    void commandIncludesModelPermissionModeAndJsonOutput() {
        List<String> argv = new ClaudeCodeRunner().command("x", "opus");

        assertEquals("claude", argv.getFirst());
        assertEquals("opus", valueAfter(argv, "--model"));
        assertEquals("auto", valueAfter(argv, "--permission-mode"));
        assertEquals("json", valueAfter(argv, "--output-format"));
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-3#2 --disallowedTools로 git push를 막고 마지막 인자로 둔다")
    void commandDisallowsGitPushAsLastArgument() {
        List<String> argv = new ClaudeCodeRunner().command("x", "sonnet");

        assertEquals("Bash(git push:*)", valueAfter(argv, "--disallowedTools"));
        // --disallowedTools <tools...>는 가변 인자라 뒤에 오는 인자를 도구 이름으로 삼킨다
        assertEquals(argv.size() - 2, argv.indexOf("--disallowedTools"), "가변 인자 옵션은 맨 끝: " + argv);
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-3#2 --bare·--dangerously-skip-permissions는 넣지 않는다")
    void commandNeverBareOrSkipPermissions() {
        List<String> argv = new ClaudeCodeRunner().command("x", "sonnet");

        assertFalse(argv.contains("--bare"), "bare는 훅을 끈다 (RFC 11절①)");
        assertFalse(argv.contains("--dangerously-skip-permissions"));
        assertFalse(argv.contains("bypassPermissions"));
    }

    // --- DRAFT-3#3 parseSession() ---

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-3#3 실제 헤드리스 JSON에서 session_id와 in=80943 out=782를 읽는다")
    void parseSessionReadsSpikeOutput() throws IOException {
        Session session = ClaudeCodeRunner.parseSession(resource());

        assertTrue(session.available(), () -> "unavailable: " + session.unavailableReason());
        assertEquals("79832a45-15df-46ec-854b-6eb5bc141ba6", session.sessionId());
        // in = input 8 + cache_creation 12831 + cache_read 68104 (usage.iterations는 합산하지 않는다)
        assertEquals(new Tokens(80_943, 782), session.tokens());
        assertNull(session.unavailableReason());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "   ",
        "{",
        "not json",
        "[]",
        "{\"type\":\"result\",\"session_id\":\"s\"}",
        "{\"type\":\"result\",\"session_id\":\"s\",\"usage\":{\"input_tokens\":\"many\"}}"
    })
    @Tag("pending")
    @DisplayName("DRAFT-3#3 비었거나 깨졌거나 usage가 없으면 예외 없이 unavailable 사유를 남긴다")
    void parseSessionFailureIsUnavailable(String stdout) {
        Session session = assertDoesNotThrow(() -> ClaudeCodeRunner.parseSession(stdout));

        assertFalse(session.available());
        assertNull(session.tokens());
        assertFalse(session.unavailableReason().isBlank());
    }
}
