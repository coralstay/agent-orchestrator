// AO-32: 카드 포트의 측정 대상 값 타입 CardCheck를 덮는다(메서드가 전부 스텁인 타입은 측정 제외).
package com.coralstay.orchestrator.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.card.TaskCardSource.CardCheck;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardValuesTest {

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 CardCheck는 판정과 사유를 값으로 담는다(같은 내용이면 같다)")
    void cardCheckIsValue() {
        CardCheck ok = new CardCheck(true, List.of());
        CardCheck missing = new CardCheck(false, List.of("AC 없음"));

        assertTrue(ok.ok());
        assertEquals(List.of("AC 없음"), missing.reasons());
        assertEquals(new CardCheck(false, List.of("AC 없음")), missing);
        assertNotEquals(ok, missing);
    }

    // == Coverage ==
}
