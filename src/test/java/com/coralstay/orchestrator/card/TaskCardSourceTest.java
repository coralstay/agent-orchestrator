package com.coralstay.orchestrator.card;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.card.TaskCardSource.Actual;
import com.coralstay.orchestrator.card.TaskCardSource.Attempt;
import com.coralstay.orchestrator.card.TaskCardSource.Card;
import com.coralstay.orchestrator.card.TaskCardSource.CardCheck;
import com.coralstay.orchestrator.card.TaskCardSource.TokenBudget;
import com.coralstay.orchestrator.card.TaskCardSource.Tokens;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 포트 계약 테스트(decision-11). 메모리 구현은 이 파일 안의 가짜이며 저장만 한다 — 파싱·출력·상태 전이는 포트의 값
 * 타입이 맡으므로 DRAFT-1 구현 전까지 이 테스트들은 pending이다.
 */
@Tag("pending")
class TaskCardSourceTest {

    /** Token Budget 소절 본문을 텍스트로 들고 있다가 read 때 파싱하고, 기록 때 다시 출력하는 메모리 구현. */
    static final class InMemoryCardSource implements TaskCardSource {

        private final Map<String, Card> cards = new HashMap<>();
        private final Map<String, String> sections = new HashMap<>();

        /** {@code sectionBody}가 null이면 Token Budget 소절이 없는 카드다. */
        void put(Card card, String sectionBody) {
            cards.put(card.id(), card);
            if (sectionBody == null) {
                sections.remove(card.id());
            } else {
                sections.put(card.id(), sectionBody);
            }
        }

        String section(String id) {
            return sections.get(id);
        }

        @Override
        public Card read(String id) {
            Card card = cards.get(id);
            if (card == null) {
                throw new IllegalArgumentException("unknown card: " + id);
            }
            String body = sections.get(id);
            return body == null ? card : card.withTokenBudget(TokenBudget.parse(body));
        }

        @Override
        public void recordAttempt(String id, Attempt attempt) {
            TokenBudget budget = read(id).tokenBudget().orElseThrow();
            sections.put(id, budget.withAttempt(attempt).render());
        }

        @Override
        public void recordActual(String id, Actual actual) {
            TokenBudget budget = read(id).tokenBudget().orElseThrow();
            sections.put(id, budget.withActual(actual).render());
        }
    }

    static Card card(String id, String status, List<String> assignees) {
        return new Card(
                id,
                "카드 접근 포트를 만든다",
                status,
                assignees,
                List.of("AO-1", "AO-17"),
                List.of("read가 필드를 돌려준다", "validate가 사유를 돌려준다"),
                List.of("src/main/java/com/coralstay/orchestrator/card/TaskCardSource.java"),
                Optional.empty());
    }

    static InMemoryCardSource sourceWith(String sectionBody) {
        var source = new InMemoryCardSource();
        source.put(card("AO-99", "To Do", List.of()), sectionBody);
        return source;
    }

    static final Attempt TIMEOUT = new Attempt(
            new Tokens(120000, 3000), new Tokens(150000, 3500), "claude-sonnet-4-5", "timeout");

    // --- DRAFT-1#1 read ---

    @Test
    @DisplayName("DRAFT-1#1 read는 카드 필드와 estimated TokenBudget을 돌려준다")
    void readReturnsCardFieldsAndEstimated() {
        var source = sourceWith("estimated: in=200000 out=8000\n");

        Card card = source.read("AO-99");

        assertEquals("AO-99", card.id());
        assertEquals("카드 접근 포트를 만든다", card.title());
        assertEquals("To Do", card.status());
        assertEquals(List.of(), card.assignees());
        assertEquals(List.of("AO-1", "AO-17"), card.dependencies());
        assertEquals(List.of("read가 필드를 돌려준다", "validate가 사유를 돌려준다"), card.acceptanceCriteria());
        assertEquals(
                List.of("src/main/java/com/coralstay/orchestrator/card/TaskCardSource.java"), card.targetFiles());
        TokenBudget budget = card.tokenBudget().orElseThrow();
        assertEquals(Optional.of(new Tokens(200000, 8000)), budget.estimated());
        assertEquals(List.of(), budget.attempts());
        assertEquals(Optional.empty(), budget.actual());
        assertEquals(List.of(), budget.unparsed());
    }

    @Test
    @DisplayName("DRAFT-1#1 read는 attempt n 줄과 actual 줄을 TokenBudget에 담는다")
    void readParsesAttemptsAndActual() {
        var source = sourceWith("""
                estimated: in=200000 out=8000
                attempt 1: commits in=120000 out=3000 · session in=150000 out=3500 · model=claude-sonnet-4-5 · timeout
                actual: commits in=300000 out=9000 · session in=380000 out=9800 · attempts 2
                """);

        TokenBudget budget = source.read("AO-99").tokenBudget().orElseThrow();

        assertEquals(List.of(TIMEOUT), budget.attempts());
        assertEquals(
                Optional.of(new Actual(new Tokens(300000, 9000), new Tokens(380000, 9800), 2)), budget.actual());
        assertEquals(List.of(), budget.unparsed());
    }

    @Test
    @DisplayName("DRAFT-1#1 read는 형식이 틀린 줄을 예외 없이 unparsed로 돌려준다")
    void readKeepsMalformedLinesAsUnparsed() {
        var source = sourceWith("estimated: 200k tokens\n");

        Card card = assertDoesNotThrow(() -> source.read("AO-99"));

        TokenBudget budget = card.tokenBudget().orElseThrow();
        assertEquals(Optional.empty(), budget.estimated());
        assertEquals(List.of("estimated: 200k tokens"), budget.unparsed());
    }

    // --- DRAFT-1#2 validate ---

    @Test
    @DisplayName("DRAFT-1#2 estimated가 올바르면 ok이고 사유가 없다")
    void validateAcceptsWellFormedBudget() {
        var source = sourceWith("estimated: in=200000 out=8000\n");

        CardCheck check = source.validate(source.read("AO-99"));

        assertEquals(new CardCheck(true, List.of()), check);
    }

    @Test
    @DisplayName("DRAFT-1#2 Token Budget 소절이 없으면 사유와 함께 거절한다")
    void validateRejectsMissingSection() {
        var source = sourceWith(null);

        CardCheck check = assertDoesNotThrow(() -> source.validate(source.read("AO-99")));

        assertFalse(check.ok());
        assertTrue(check.reasons().stream().anyMatch(r -> r.contains("Token Budget")), check.reasons().toString());
    }

    @Test
    @DisplayName("DRAFT-1#2 estimated 줄이 없으면 사유와 함께 거절한다")
    void validateRejectsMissingEstimated() {
        var source = sourceWith(
                "attempt 1: commits in=1 out=1 · session in=2 out=2 · model=claude-sonnet-4-5 · timeout\n");

        CardCheck check = assertDoesNotThrow(() -> source.validate(source.read("AO-99")));

        assertFalse(check.ok());
        assertTrue(check.reasons().stream().anyMatch(r -> r.contains("estimated")), check.reasons().toString());
    }

    @Test
    @DisplayName("DRAFT-1#2 형식이 틀린 줄은 그 줄을 사유에 담아 거절한다")
    void validateRejectsMalformedLine() {
        var source = sourceWith("estimated: in=200000 out=8000\nestimated tokens: lots\n");

        CardCheck check = assertDoesNotThrow(() -> source.validate(source.read("AO-99")));

        assertFalse(check.ok());
        assertTrue(
                check.reasons().stream().anyMatch(r -> r.contains("estimated tokens: lots")),
                check.reasons().toString());
    }

    // --- DRAFT-1#3 recordAttempt / recordActual ---

    @Test
    @DisplayName("DRAFT-1#3 recordAttempt는 attempt n 줄을 순서대로 덧붙인다")
    void recordAttemptAppendsNumberedLines() {
        var source = sourceWith("estimated: in=200000 out=8000\n");
        var second = new Attempt(new Tokens(90000, 2000), new Tokens(95000, 2100), "claude-opus-4-1", "tests-failed");

        source.recordAttempt("AO-99", TIMEOUT);
        source.recordAttempt("AO-99", second);

        assertEquals(List.of(TIMEOUT, second), source.read("AO-99").tokenBudget().orElseThrow().attempts());
        assertEquals("""
                estimated: in=200000 out=8000
                attempt 1: commits in=120000 out=3000 · session in=150000 out=3500 · model=claude-sonnet-4-5 · timeout
                attempt 2: commits in=90000 out=2000 · session in=95000 out=2100 · model=claude-opus-4-1 · tests-failed
                """.strip(), source.section("AO-99").strip());
    }

    @Test
    @DisplayName("DRAFT-1#3 recordActual은 시도 기록을 두고 actual 줄을 더한다")
    void recordActualKeepsAttemptsAndAddsActual() {
        var source = sourceWith("estimated: in=200000 out=8000\n");
        var actual = new Actual(new Tokens(300000, 9000), new Tokens(380000, 9800), 2);

        source.recordAttempt("AO-99", TIMEOUT);
        source.recordActual("AO-99", actual);

        TokenBudget budget = source.read("AO-99").tokenBudget().orElseThrow();
        assertEquals(Optional.of(new Tokens(200000, 8000)), budget.estimated());
        assertEquals(List.of(TIMEOUT), budget.attempts());
        assertEquals(Optional.of(actual), budget.actual());
        assertEquals("""
                estimated: in=200000 out=8000
                attempt 1: commits in=120000 out=3000 · session in=150000 out=3500 · model=claude-sonnet-4-5 · timeout
                actual: commits in=300000 out=9000 · session in=380000 out=9800 · attempts 2
                """.strip(), source.section("AO-99").strip());
    }

    // --- Tokens in=N out=M (CommitLedger Tokens-Used 트레일러와 같은 벡터, AO-6 AC5) ---

    @Test
    @DisplayName("DRAFT-1 보조: Tokens는 git-trail 트레일러 값 in=59858 out=465를 파싱한다")
    void tokensParsesTrailerValue() {
        assertEquals(new Tokens(59858, 465), Tokens.parse("in=59858 out=465"));
        assertEquals(new Tokens(0, 0), Tokens.parse("in=0 out=0"));
        assertEquals(new Tokens(3_000_000_000L, 1), Tokens.parse("in=3000000000 out=1"));
    }

    @Test
    @DisplayName("DRAFT-1 보조: Tokens는 in=N out=M으로 출력하고 왕복한다")
    void tokensFormatsAndRoundTrips() {
        assertEquals("in=59858 out=465", new Tokens(59858, 465).toString());
        assertEquals(new Tokens(59858, 465), Tokens.parse(new Tokens(59858, 465).toString()));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {
        "",
        "in=59858",
        "out=465",
        "out=465 in=59858",
        "in=abc out=465",
        "in=-1 out=465",
        "in=59,858 out=465",
        "in=59858 out=465 extra",
        "IN=59858 OUT=465",
        "unavailable (no-session-id)"
    })
    @DisplayName("DRAFT-1 보조: Tokens는 in=N out=M이 아닌 값을 거절한다")
    void tokensRejectsInvalidForms(String text) {
        assertThrows(IllegalArgumentException.class, () -> Tokens.parse(text));
    }
}
