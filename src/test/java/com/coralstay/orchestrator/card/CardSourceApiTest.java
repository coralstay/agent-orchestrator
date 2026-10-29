package com.coralstay.orchestrator.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 카드 포트의 공개 API 모양(decision-14 · decision-5). 동작 테스트는 pending({@link TaskCardSourceTest},
 * {@link BacklogCardSourceTest})이고, 여기서는 구현 없이도 확인할 수 있는 계약만 본다.
 */
class CardSourceApiTest {

    static List<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).map(Method::getName).toList();
    }

    static List<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).toList();
    }

    static List<String> nestedNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredClasses()).map(Class::getSimpleName).toList();
    }

    // --- AO-30#1 claim · 사람 락 제거(decision-14) ---

    @Test
    @DisplayName("AO-30#1 TaskCardSource에는 claim · IN_PROGRESS가, Card에는 claimedBy가 없다")
    void taskCardSourceHasNoClaim() {
        assertFalse(methodNames(TaskCardSource.class).contains("claim"));
        assertFalse(fieldNames(TaskCardSource.class).contains("IN_PROGRESS"));
        assertFalse(methodNames(TaskCardSource.Card.class).contains("claimedBy"));
    }

    @Test
    @DisplayName("AO-30#1 BacklogCardSource에는 claim · isLocked · LockKind · LockVerdict · AGENT가 없다")
    void backlogCardSourceHasNoClaimOrHumanLock() {
        assertFalse(methodNames(BacklogCardSource.class).contains("claim"));
        assertFalse(methodNames(BacklogCardSource.class).contains("isLocked"));
        assertEquals(List.of("CommandRunner"), nestedNames(BacklogCardSource.class));
        assertFalse(fieldNames(BacklogCardSource.class).contains("AGENT"));
    }

    // --- AO-30#2 커밋 환경에서 지우는 변수(decision-5) ---

    @Test
    @DisplayName("AO-30#2 AO-30#B2 오케스트레이터 커밋 환경에서 AI_AGENT · CLAUDE_CODE_SESSION_ID · CLAUDE_PID를 지운다")
    void strippedEnvCoversAgentDetectionVariables() {
        assertEquals(
                List.of("AI_AGENT", "CLAUDE_CODE_SESSION_ID", "CLAUDE_PID"), BacklogCardSource.STRIPPED_ENV);
    }

    // --- AO-30#4 Token Budget 실제치는 commits 없이 session만(decision-6 · decision-8, 최상위 Q-18 전) ---

    @Test
    @DisplayName("AO-30#4 AO-30#B3 commits 없이 session만으로 Attempt · Actual을 만든다")
    void attemptAndActualWithSessionOnly() {
        var session = new TaskCardSource.Tokens(150000, 3500);

        var attempt = new TaskCardSource.Attempt(session, "claude-sonnet-4-5", "timeout");
        var actual = new TaskCardSource.Actual(session, 2);

        assertEquals(Optional.empty(), attempt.commits());
        assertEquals(session, attempt.session());
        assertEquals("claude-sonnet-4-5", attempt.model());
        assertEquals("timeout", attempt.reason());
        assertEquals(Optional.empty(), actual.commits());
        assertEquals(session, actual.session());
        assertEquals(2, actual.attempts());
    }
}
