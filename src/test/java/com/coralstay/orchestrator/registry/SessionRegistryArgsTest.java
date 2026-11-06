// ---
// title: SessionRegistryArgsTest
// description: SessionRegistry.admit의 null 인자 거부 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-06'
// ---
// AO-32: SessionRegistry.admit의 null 인자 거부를 덮는다(빈 문자열은 기존 테스트가 본다).
package com.coralstay.orchestrator.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SessionRegistryArgsTest {

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 admit은 null taskId · role을 거부하고 허가를 소비하지 않는다")
    void admitRejectsNullWithoutTakingPermit() {
        var registry = new SessionRegistry(1);  // ---- 1

        assertThrows(IllegalArgumentException.class, () -> registry.admit(null, "implementer"));
        assertThrows(IllegalArgumentException.class, () -> registry.admit("AO-1", null));
        assertEquals(1, registry.availablePermits());
    }

    // == Coverage ==
}

// ======== 노트 ========
// 1 null 인자는 허가를 소비하지 않는다
//   설명: null taskId · role은 permits.acquire 전에 거부한다. 거부된 뒤에도 용량 1의 허가가 그대로 남아 있어야 한다.
//   관련: registry/SessionRegistry.java
//   개념: 허가 누수
