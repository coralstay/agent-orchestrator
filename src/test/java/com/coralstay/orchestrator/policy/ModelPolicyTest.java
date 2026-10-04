package com.coralstay.orchestrator.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.policy.ModelPolicy.Choice;
import com.coralstay.orchestrator.policy.ModelPolicy.PolicyFormatException;
import com.coralstay.orchestrator.policy.ModelPolicy.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 설정 파일은 @TempDir에 직접 쓴다. CsvSource textBlock에서 {@code #}로 시작하는 줄은 주석이므로 행 첫머리에 두지 않는다.
 *
 * <p>DRAFT-4 구현 전이라 전부 pending (decision-11).
 */
class ModelPolicyTest {

    static final String HAIKU = "claude-haiku-4-5";
    static final String SONNET = "claude-sonnet-4-5";
    static final String OPUS = "claude-opus-4-1";

    /** 일부러 오름차순이 아니게 쓴다 — 파일 순서가 아니라 상한 순서로 골라야 한다. */
    static final String VALID = """
            # 예상 in+out(session) → 모델
            model.upTo.800000=%s

            model.upTo.200000=%s
            model.default=%s
            """.formatted(SONNET, HAIKU, OPUS);

    @TempDir
    Path dir;

    Path write(String content) throws IOException {
        Path file = dir.resolve("orchestrator.properties");
        Files.writeString(file, content);
        return file;
    }

    ModelPolicy valid() throws IOException {
        return ModelPolicy.load(write(VALID));
    }

    // --- #1 load: 파싱과 형식 오류 줄 번호 ---

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-4#1 upTo 구간과 default를 읽는다(빈 줄·주석 무시, 상한 오름차순)")
    void loadParsesBucketsAndDefault() throws IOException {
        ModelPolicy policy = valid();

        assertEquals(new TreeMap<>(Map.of(200_000L, HAIKU, 800_000L, SONNET)), policy.buckets());
        assertEquals(List.of(200_000L, 800_000L), List.copyOf(policy.buckets().keySet()));
        assertEquals(OPUS, policy.defaultModel());
        assertEquals(Source.FILE, policy.source());
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-4#1 default만 있어도 된다 — 모든 예상치가 default")
    void loadAcceptsDefaultOnly() throws IOException {
        ModelPolicy policy = ModelPolicy.load(write("model.default=" + SONNET + "\n"));

        assertTrue(policy.buckets().isEmpty());
        assertEquals(SONNET, policy.choose(0).model());
        assertEquals(SONNET, policy.choose(Long.MAX_VALUE).model());
    }

    @ParameterizedTest(name = "[{index}] 줄 {1}: {2}")
    @Tag("pending")
    @DisplayName("DRAFT-4#1 형식 오류는 줄 번호와 함께 예외")
    @CsvSource(
            delimiter = '|',
            textBlock = """
            model.default=claude-sonnet-4-5\\nmodel.upTo.200000                | 2 | '=' 없음
            model.default=x\\n# c\\n\\nmodel.upTo.abc=claude-haiku-4-5          | 4 | 숫자 아닌 토큰수(주석·빈 줄도 줄 수에 셈)
            model.upTo.0=claude-haiku-4-5\\nmodel.default=x                    | 1 | 0 토큰수
            model.upTo.-5=claude-haiku-4-5\\nmodel.default=x                   | 1 | 음수 토큰수
            model.default=x\\nmodel.upTo.200000=                               | 2 | 빈 모델 이름
            model.default=x\\nmodel.fallback=y                                 | 2 | 모르는 키
            model.upTo.200000=a\\nmodel.default=x\\nmodel.upTo.200000=b        | 3 | 중복 upTo 키
            model.default=x\\nmodel.default=y                                  | 2 | 중복 default
            """)
    void loadRejectsMalformedLineWithLineNumber(String content, int line, String why) throws IOException {
        Path file = write(content.replace("\\n", "\n") + "\n");

        var e = assertThrows(PolicyFormatException.class, () -> ModelPolicy.load(file), why);

        assertEquals(line, e.lineNumber(), why);
        assertTrue(e.getMessage().contains(String.valueOf(line)), "메시지에 줄 번호: " + e.getMessage());
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-4#1 model.default가 없으면 형식 오류(줄 번호 0)")
    void loadRequiresDefault() throws IOException {
        Path file = write("model.upTo.200000=" + HAIKU + "\n");

        var e = assertThrows(PolicyFormatException.class, () -> ModelPolicy.load(file));

        assertEquals(0, e.lineNumber());
        assertTrue(e.getMessage().contains("model.default"), e.getMessage());
    }

    // --- #2 choose: 이하인 가장 작은 구간, 없으면 default ---

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @Tag("pending")
    @DisplayName("DRAFT-4#2 예상 in+out 이하인 가장 작은 구간을 고른다(경계값 포함)")
    @CsvSource({
        "0,       " + HAIKU,
        "1,       " + HAIKU,
        "199999,  " + HAIKU,
        "200000,  " + HAIKU, // 상한과 같으면 그 구간("이하"), 다음 구간 아님
        "200001,  " + SONNET,
        "799999,  " + SONNET,
        "800000,  " + SONNET,
        "800001,  " + OPUS, // 어느 구간에도 안 들면 default
        "9223372036854775807, " + OPUS,
    })
    void chooseSmallestBucketAtOrAbove(long estimated, String expected) throws IOException {
        Choice choice = valid().choose(estimated);

        assertEquals(expected, choice.model());
        assertEquals(Source.FILE, choice.source());
        assertFalse(choice.builtIn());
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-4#2 음수 예상치는 IllegalArgumentException")
    void chooseRejectsNegative() throws IOException {
        ModelPolicy policy = valid();

        assertThrows(IllegalArgumentException.class, () -> policy.choose(-1));
    }

    // --- #3 설정 파일이 없으면 내장 기본값 ---

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-4#3 설정 파일이 없으면 내장 기본값(작은 구간 haiku, 기본 sonnet)")
    void loadFallsBackToBuiltInWhenFileMissing() throws IOException {
        ModelPolicy policy = ModelPolicy.load(dir.resolve("missing.properties"));

        assertEquals(Source.BUILT_IN, policy.source());
        assertEquals(
                new TreeMap<>(Map.of(ModelPolicy.BUILT_IN_SMALL_UP_TO, ModelPolicy.BUILT_IN_SMALL_MODEL)),
                policy.buckets());
        assertEquals(ModelPolicy.BUILT_IN_DEFAULT_MODEL, policy.defaultModel());
        assertTrue(ModelPolicy.BUILT_IN_SMALL_MODEL.contains("haiku"));
        assertTrue(ModelPolicy.BUILT_IN_DEFAULT_MODEL.contains("sonnet"));
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-4#3 내장 기본값으로 고른 결과는 그 사실을 표시한다(경계값 포함)")
    void builtInChoiceIsMarked() throws IOException {
        ModelPolicy policy = ModelPolicy.load(dir.resolve("missing.properties"));
        long upTo = ModelPolicy.BUILT_IN_SMALL_UP_TO;

        Choice small = policy.choose(upTo);
        Choice large = policy.choose(upTo + 1);

        assertEquals(new Choice(ModelPolicy.BUILT_IN_SMALL_MODEL, Source.BUILT_IN), small);
        assertEquals(new Choice(ModelPolicy.BUILT_IN_DEFAULT_MODEL, Source.BUILT_IN), large);
        assertTrue(small.builtIn());
        assertTrue(large.builtIn());
    }

    @Test
    @Tag("pending")
    @DisplayName("DRAFT-4#3 builtIn()은 파일 없을 때의 load와 같은 정책")
    void builtInMatchesMissingFileLoad() throws IOException {
        ModelPolicy missing = ModelPolicy.load(dir.resolve("missing.properties"));
        ModelPolicy builtIn = ModelPolicy.builtIn();

        assertEquals(missing.buckets(), builtIn.buckets());
        assertEquals(missing.defaultModel(), builtIn.defaultModel());
        assertEquals(Source.BUILT_IN, builtIn.source());
    }
}
