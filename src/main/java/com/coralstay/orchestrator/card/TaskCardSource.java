package com.coralstay.orchestrator.card;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 카드 접근 포트(decision-6). 오케스트레이터 코어는 카드 시스템(backlog.md 등)을 모른 채 이 포트로 카드를 읽고,
 * 검증하고, 시도·실제치를 기록한다. 카드를 In Progress로 바꾸는 착수는 에이전트 세션이 태스크 브랜치에서 하므로
 * 이 포트에 없다(decision-14).
 *
 * <p>상태 전이와 Token Budget 파싱·출력은 값 타입({@link Card}, {@link TokenBudget}, {@link Tokens})에 두고,
 * 구현체는 저장·커밋만 맡는다. 예상·실제 토큰은 카드 Description의 {@code ### Token Budget} 소절에 둔다:
 *
 * <pre>
 * estimated: in=200000 out=8000
 * attempt 1: session in=… out=… · model=claude-sonnet-… · timeout
 * actual: session in=… out=… · attempts 2
 * </pre>
 *
 * <p>{@code commits in=… out=… · }(커밋 트레일러 합계)는 {@code session} 앞에 올 수 있는 생략 가능한 항목이다. 트레일러를
 * 남기는 도구가 없어 최상위 Q-18(토큰 계량 컴포넌트) 전까지는 {@code session}만 기록한다(decision-6 · decision-8).
 *
 * <p>시그니처 스텁(decision-11): 동작은 DRAFT-1에서 구현한다.
 */
public interface TaskCardSource {

    /** 카드를 읽는다. Token Budget 형식이 틀려도 예외 없이 {@link TokenBudget#unparsed()}에 담아 돌려준다. */
    Card read(String id);

    /** 실행 가능 여부를 사유와 함께 돌려준다. 누락·형식 오류로 예외를 던지지 않는다. */
    default CardCheck validate(Card card) {
        throw new UnsupportedOperationException("pending: DRAFT-1");
    }

    /** Done에 이르지 못한 시도를 {@code attempt n} 줄로 덧붙인다(n은 1부터 순서대로). */
    void recordAttempt(String id, Attempt attempt);

    /** Done에 이른 실행의 합계를 {@code actual} 줄로 기록한다. */
    void recordActual(String id, Actual actual);

    /** 카드. {@code tokenBudget}이 비면 {@code ### Token Budget} 소절이 없는 것이다. */
    record Card(
            String id,
            String title,
            String status,
            List<String> assignees,
            List<String> dependencies,
            List<String> acceptanceCriteria,
            List<String> targetFiles,
            Optional<TokenBudget> tokenBudget) {

        /** Token Budget만 바꾼 사본. */
        public Card withTokenBudget(TokenBudget budget) {
            throw new UnsupportedOperationException("pending: DRAFT-1");
        }
    }

    /**
     * {@code ### Token Budget} 소절의 내용. {@code unparsed}는 인식하지 못한 비어 있지 않은 줄(형식 오류)이다.
     */
    record TokenBudget(
            Optional<Tokens> estimated,
            List<Attempt> attempts,
            Optional<Actual> actual,
            List<String> unparsed) {

        /** 소절 본문(제목 줄 아래)을 파싱한다. 형식 오류는 예외 대신 {@code unparsed}에 담는다. */
        public static TokenBudget parse(String sectionBody) {
            throw new UnsupportedOperationException("pending: DRAFT-1");
        }

        /** 소절 본문을 decision-6 형식으로 출력한다(estimated → attempt 1..n → actual). */
        public String render() {
            throw new UnsupportedOperationException("pending: DRAFT-1");
        }

        /** 시도를 맨 뒤에 덧붙인 사본. */
        public TokenBudget withAttempt(Attempt attempt) {
            throw new UnsupportedOperationException("pending: DRAFT-1");
        }

        /** 실제치를 넣은 사본. */
        public TokenBudget withActual(Actual actual) {
            throw new UnsupportedOperationException("pending: DRAFT-1");
        }
    }

    /**
     * 토큰 수(decision-8). 입력은 캐시 쓰기·읽기를 포함한다. 텍스트 형식은 git-trail {@code Tokens-Used}와 같은
     * {@code in=N out=M}이다.
     */
    record Tokens(long in, long out) {

        /** {@code in=N out=M}을 파싱한다. 형식이 틀리면 {@link IllegalArgumentException}. */
        public static Tokens parse(String text) {
            throw new UnsupportedOperationException("pending: DRAFT-1");
        }

        /** {@code in=N out=M}. */
        @Override
        public String toString() {
            throw new UnsupportedOperationException("pending: DRAFT-1");
        }
    }

    /**
     * Done에 이르지 못한 시도 하나: {@code commits}는 트레일러 합계(생략 가능 — Q-18 전까지 비어 있다), {@code session}은
     * 헤드리스 JSON usage 합계.
     */
    record Attempt(Optional<Tokens> commits, Tokens session, String model, String reason) {

        public Attempt {
            Objects.requireNonNull(commits);
            Objects.requireNonNull(session);
        }

        /** {@code session}만 있는 시도. */
        public Attempt(Tokens session, String model, String reason) {
            this(Optional.empty(), session, model, reason);
        }
    }

    /** Done 실행의 실제치(모든 시도의 합계)와 시도 횟수. {@code commits}는 생략 가능하다({@link Attempt} 참고). */
    record Actual(Optional<Tokens> commits, Tokens session, int attempts) {

        public Actual {
            Objects.requireNonNull(commits);
            Objects.requireNonNull(session);
        }

        /** {@code session}만 있는 실제치. */
        public Actual(Tokens session, int attempts) {
            this(Optional.empty(), session, attempts);
        }
    }

    /** {@link #validate} 결과. {@code ok}이면 {@code reasons}는 비어 있다. */
    record CardCheck(boolean ok, List<String> reasons) {}
}
