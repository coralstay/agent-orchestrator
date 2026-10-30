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
        var registry = new SessionRegistry(1);

        assertThrows(IllegalArgumentException.class, () -> registry.admit(null, "implementer"));
        assertThrows(IllegalArgumentException.class, () -> registry.admit("AO-1", null));
        assertEquals(1, registry.availablePermits());
    }

    // == Coverage ==
}
