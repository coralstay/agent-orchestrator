// AO-32: WorktreeManager의 null 태스크 ID 거부를 덮는다(형식 오류는 기존 테스트가 본다).
package com.coralstay.orchestrator.workspace;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorktreeManagerArgsTest {

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 null 태스크 ID는 git을 부르기 전에 IllegalArgumentException")
    void nullTaskIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> WorktreeManager.branchName(null));
    }

    // == Coverage ==
}
