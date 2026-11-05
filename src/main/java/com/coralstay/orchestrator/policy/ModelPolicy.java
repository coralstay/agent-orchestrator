package com.coralstay.orchestrator.policy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedMap;

/**
 * 카드의 예상 토큰(session 단위 in+out, decision-8)으로 Claude 모델을 고르는 정책 (decision-10).
 *
 * <p>설정은 의존성 없는 key=value 파일이다. 빈 줄과 {@code #} 주석 줄은 무시한다:
 *
 * <pre>{@code
 * model.upTo.200000=claude-haiku-4-5
 * model.upTo.800000=claude-sonnet-4-5
 * model.default=claude-opus-4-1
 * }</pre>
 *
 * <p>{@link #choose}는 상한이 예상치 <b>이상</b>인(즉 예상치가 상한 "이하"인) 가장 작은 구간의 모델을, 그런 구간이 없으면
 * {@code model.default}를 돌려준다.
 *
 * <p>DRAFT-4 구현 전 시그니처 스텁이다 (decision-11). 본문은 모두 {@code pending: DRAFT-4}로 던진다.
 */
public final class ModelPolicy {

    /*
     * 내장 기본값. 실측이 쌓이기 전의 임시 값으로 근거가 없다 — 실측(runs.jsonl)이 쌓이면 다시 정한다.
     * 모델 이름은 git-trail의 AI-Model 기록과 같은 형식을 쓴다.
     */

    /** 내장 기본값의 작은 구간 상한(토큰, 이하). */
    public static final long BUILT_IN_SMALL_UP_TO = 200_000L;

    /** 내장 기본값의 작은 구간 모델. */
    public static final String BUILT_IN_SMALL_MODEL = "claude-haiku-4-5";

    /** 내장 기본값의 default 모델. */
    public static final String BUILT_IN_DEFAULT_MODEL = "claude-sonnet-4-5";

    /** 정책이 어디서 왔는지. */
    public enum Source {
        /** 설정 파일에서 읽었다. */
        FILE,
        /** 설정 파일이 없어 내장 기본값을 썼다. */
        BUILT_IN
    }

    /**
     * {@link #choose}의 결과.
     *
     * @param model 고른 모델 이름
     * @param source 이 결과를 낸 정책의 출처 — 내장 기본값이면 {@link Source#BUILT_IN}
     */
    public record Choice(String model, Source source) {
        /** 내장 기본값으로 고른 결과인지. */
        public boolean builtIn() {
            return source == Source.BUILT_IN;
        }
    }

    /** 설정 파일 형식 오류. 문제가 된 줄 번호(1부터)를 담는다. */
    public static final class PolicyFormatException extends RuntimeException {
        private final int lineNumber;

        /**
         * @param lineNumber 문제가 된 줄 번호(1부터). 특정 줄이 없는 오류(예: {@code model.default} 누락)는 0
         * @param message 설명. 줄 번호가 있으면 메시지에도 포함한다
         */
        public PolicyFormatException(int lineNumber, String message) {
            super(message);
            this.lineNumber = lineNumber;
        }

        public int lineNumber() {
            return lineNumber;
        }
    }

    private ModelPolicy() {}

    /**
     * 설정 파일을 읽는다. 파일이 없으면 {@link #builtIn()}을 돌려준다.
     *
     * <p>허용 키는 {@code model.upTo.<양의 정수 토큰수>}와 {@code model.default}뿐이다. {@code =}이 없는 줄, 모르는 키,
     * 숫자가 아닌/0 이하 토큰수, 빈 모델 이름, 중복 키는 줄 번호와 함께 {@link PolicyFormatException}. {@code
     * model.default}가 없으면 줄 번호 0의 {@link PolicyFormatException}.
     *
     * @throws IOException 파일은 있으나 읽을 수 없을 때
     */
    public static ModelPolicy load(Path path) throws IOException {
        throw new UnsupportedOperationException("pending: DRAFT-4");
    }

    /** 내장 기본값 정책: {@value #BUILT_IN_SMALL_UP_TO} 이하 → haiku, 그 외 → sonnet. */
    public static ModelPolicy builtIn() {
        throw new UnsupportedOperationException("pending: DRAFT-4");
    }

    /**
     * 예상 in+out 토큰으로 모델을 고른다.
     *
     * @param estimatedInOut session 단위 예상 in+out(캐시 포함 입력). 0 이상
     * @throws IllegalArgumentException 음수일 때
     */
    public Choice choose(long estimatedInOut) {
        throw new UnsupportedOperationException("pending: DRAFT-4");
    }

    /** 구간 상한(이하) → 모델, 상한 오름차순. */
    public SortedMap<Long, String> buckets() {
        throw new UnsupportedOperationException("pending: DRAFT-4");
    }

    /** 어느 구간에도 들지 않을 때의 모델. */
    public String defaultModel() {
        throw new UnsupportedOperationException("pending: DRAFT-4");
    }

    /** 정책의 출처. */
    public Source source() {
        throw new UnsupportedOperationException("pending: DRAFT-4");
    }
}
