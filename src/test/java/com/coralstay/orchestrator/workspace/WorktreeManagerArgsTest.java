// ---
// title: WorktreeManagerArgsTest
// description: WorktreeManager의 null 태스크 ID 거부 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-06'
// ---
// AO-32: WorktreeManager의 null 태스크 ID 거부를 덮는다(형식 오류는 기존 테스트가 본다).
package com.coralstay.orchestrator.workspace;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorktreeManagerArgsTest {

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 null 태스크 ID는 git을 부르기 전에 IllegalArgumentException")
    void nullTaskIdIsRejected() {  // ---- 1
        assertThrows(IllegalArgumentException.class, () -> WorktreeManager.branchName(null));
    }

    // == Coverage ==
}

// ======== 노트 ========
// 1 null ID는 git을 부르기 전에 거부
//   설명: ID 검증이 git 호출보다 앞서야 경로 조작과 의미 없는 git 실패를 막는다.
//   관련: workspace/WorktreeManager.java
