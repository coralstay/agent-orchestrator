---
id: DRAFT-10
title: >-
  충돌 결정 반영(코드): claim · 사람 락 제거, CLAUDE_PID 제거, 브랜치 있으면 배정 안 함, Token Budget은
  session만
status: Draft
assignee: []
created_date: '2026-10-29 02:31'
updated_date: '2026-10-29 02:31'
labels:
  - code
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
DRAFT-9(문서)의 코드 반영. 최상위 DRAFT-1(TASK-19 예정) 전파.

## 비즈니스 규칙
- B1 (AC #3) task/<ID> 브랜치가 로컬이나 원격에 있고 main에 병합되지 않았으면 그 카드는 배정하지 않는다
- B2 (AC #2) 오케스트레이터가 직접 하는 기록 커밋의 환경에는 AI_AGENT · CLAUDE_CODE_SESSION_ID · CLAUDE_PID가 없다
- B3 (AC #4) commits 값이 없어도 Token Budget 실제치는 session 값만으로 기록된다
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 TaskCardSource.claim · Card.claimedBy와 BacklogCardSource.claim · isLocked · LockKind · LockVerdict, 관련 pending 테스트를 삭제한다
- [ ] #2 BacklogCardSource.STRIPPED_ENV에 CLAUDE_PID를 넣고 recordAttempt · recordActual 커밋 환경 테스트를 맞춘다
- [ ] #3 BacklogClient.isRunnable이 task/<ID> 브랜치(로컬 · 원격, git cherry 기준 미병합)가 있으면 false를 돌려준다
- [ ] #4 Token Budget 실제치 형식에서 commits를 생략할 수 있게 하고 session만 기록하는 경로를 테스트한다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 ./gradlew test 통과
- [ ] #2 pending 테스트(./gradlew pendingTest) 상태를 final summary에 남긴다 (수동)
- [ ] #3 각 커밋 본문에 Refs: coralstay-software-factory TASK-19 (수동)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Subjective
문서 카드에서 정한 착수 · 커밋 환경 · 배정 조건 · 토큰 형식을 코드에 맞춘다(유저 결정 2026-10-29).

### Objective
- BacklogCardSource.java:34 STRIPPED_ENV = AI_AGENT, CLAUDE_CODE_SESSION_ID. claim/isLocked는 :102-124에 스텁(pending DRAFT-2).
- BacklogClient.isRunnable(:115-128)은 To Do + 직접 의존 Done만 본다. WorktreeManager.create(:74-77)는 브랜치가 있으면 거부한다(유지).
- TaskCardSource Attempt/Actual(:113-117)은 commits · session을 둘 다 받는다.

### Assessment
claim을 지우면 사람 락 판정도 근거가 없어지고, 착수 판단은 브랜치 존재로 옮긴다. 미병합 판정은 최상위 3.03 L44의 git cherry 기준을 따른다.

### Plan
AC 하나씩 테스트(AC 표식 · B 표식) → 구현 → 커밋: #1 삭제 → #2 환경변수 → #3 isRunnable → #4 Token Budget.
<!-- SECTION:PLAN:END -->
