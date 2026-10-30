---
id: AO-31
title: '최상위 TASK-21 전파: design/ 주의점을 AO-30 이후 코드와 최상위 BL-002 · ORC-021에 맞춘다'
status: To Do
assignee: []
created_date: '2026-10-30T16:24:00Z'
updated_date: '2026-10-30T16:35:00Z'
labels:
  - design
dependencies: []
documentation:
  - /Users/flynn_macpro/.claude/plans/starry-greeting-curry.md
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
최상위 coralstay-software-factory TASK-21(agent-orchestrator 서술을 코드에 맞춤, 2026-10-31)의 전파(RAIL-024). 최상위는 BL-002를 'task view --plain으로 상태 · 직접 의존을 읽고 readiness · isReady는 쓰지 않는다'로 고쳤고, ORC-021에 '커밋 트레일러 판독은 미구현(AO-6, Q-18 보류)' 단서를 달았다. 이 저장소 design/3-아키텍처-설계서/01-시스템-구성.md 주의점은 아직 'BL-002는 --json을 요구한다', 'isRunnable은 브랜치를 보지 않는다', 'LockKind가 남아 있다 — AO-30에서 맞춘다'를 적고 있지만 AO-30 이후 코드(BacklogClient.java:119-167, LockKind 없음)와 다르다. 커밋 본문에 'Refs: coralstay-software-factory TASK-21'을 적는다.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 3.01 주의점의 BL-002 차이 서술과 isRunnable · LockKind 서술을 AO-30 이후 코드에 맞춘다 (수동)
- [ ] #2 2.03에서 최상위 BL-002 · ORC-021 변경을 같은 ID로 이어받는다 (수동)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 grep --json · LockKind · isReady 결과를 노트에 남긴다 (수동)
- [ ] #2 파일 하나에 커밋 하나, 각 커밋 본문에 Refs: coralstay-software-factory TASK-21 (수동)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Subjective
최상위 TASK-21 전파. 유저 결정(2026-10-31): 코드 → 문서, BL-002는 코드에 맞춰 고침, AO 전파는 지금 카드로.

### Objective
- 01-시스템-구성.md:51 BL-002 --json 차이 서술, :52 isRunnable 브랜치 미확인 · LockKind 잔존 서술.
- 코드(main 909fa4a): BacklogClient.isRunnable(:131-140)이 미병합 task/<ID> 브랜치를 git cherry로 본다(:153-167). src/main에 LockKind 없음.
- 최상위 2.03 BL-002 · ORC-021 문구 변경(6affdfd).

### Assessment
문서만 바꾼다. 최상위 문구를 같은 ID로 이어받고, 해소된 차이 서술은 지운다.

### Plan
1. 3.01 주의점을 코드에 맞춘다.
2. 2.03의 BL-002 · ORC-021 이어받는 문구를 최상위와 맞춘다(있으면).
3. grep(--json · LockKind · isReady ∧)으로 잔존 확인, 파일마다 커밋.
<!-- SECTION:PLAN:END -->
