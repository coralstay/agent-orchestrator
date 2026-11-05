// AO-32: ModelPolicy의 값 타입(Choice · Source · PolicyFormatException)을 덮는다(ModelPolicy 본체 스텁은 측정 제외).
package com.coralstay.orchestrator.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.policy.ModelPolicy.Choice;
import com.coralstay.orchestrator.policy.ModelPolicy.PolicyFormatException;
import com.coralstay.orchestrator.policy.ModelPolicy.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModelPolicyValuesTest {

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 Choice.builtIn은 출처가 BUILT_IN일 때만 참이다")
    void choiceBuiltInFollowsSource() {
        assertTrue(new Choice(ModelPolicy.BUILT_IN_SMALL_MODEL, Source.BUILT_IN).builtIn());
        assertFalse(new Choice("claude-opus-4-1", Source.FILE).builtIn());
    }

    @Test
    @DisplayName("AO-32#3 PolicyFormatException은 줄 번호와 메시지를 함께 담는다")
    void formatExceptionCarriesLineNumber() {
        var e = new PolicyFormatException(3, "3번째 줄: 모르는 키 model.max");

        assertEquals(3, e.lineNumber());
        assertEquals("3번째 줄: 모르는 키 model.max", e.getMessage());
        assertEquals(0, new PolicyFormatException(0, "model.default 없음").lineNumber());
    }

    // == Coverage ==
}
