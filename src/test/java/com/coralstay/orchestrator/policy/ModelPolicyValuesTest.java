// ---
// title: ModelPolicyValuesTest
// description: ModelPolicy 값 타입(Choice·PolicyFormatException) 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-06'
// ---
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
    void choiceBuiltInFollowsSource() {  // ---- 1
        assertTrue(new Choice(ModelPolicy.BUILT_IN_SMALL_MODEL, Source.BUILT_IN).builtIn());
        assertFalse(new Choice("claude-opus-4-1", Source.FILE).builtIn());
    }

    @Test
    @DisplayName("AO-32#3 PolicyFormatException은 줄 번호와 메시지를 함께 담는다")
    void formatExceptionCarriesLineNumber() {
        var e = new PolicyFormatException(3, "3번째 줄: 모르는 키 model.max");  // ---- 2

        assertEquals(3, e.lineNumber());
        assertEquals("3번째 줄: 모르는 키 model.max", e.getMessage());
        assertEquals(0, new PolicyFormatException(0, "model.default 없음").lineNumber());
    }

    // == Coverage ==
}

// ======== 노트 ========
// 1 builtIn은 출처로 정해진다
//   설명: Choice.builtIn()은 source가 BUILT_IN일 때만 참이다.
//   관련: policy/ModelPolicy.java
// 2 형식 오류는 줄 번호를 담는다
//   설명: 설정 파일 오류는 줄 번호(1부터)와 메시지를 함께 담는다. 특정 줄이 없는 오류는 0이다.
//   관련: decision-10
