---
id: AO-30
title: >-
  충돌 결정 반영(코드): claim · 사람 락 제거, CLAUDE_PID 제거, 브랜치 있으면 배정 안 함, Token Budget은
  session만
status: Done
assignee: []
created_date: '2026-10-29 02:31'
updated_date: '2026-10-29 05:50'
labels:
  - code
dependencies:
  - AO-29
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
AO-29(문서)의 코드 반영. 최상위 TASK-19 전파.

## 비즈니스 규칙
- B1 (AC #3) task/<ID> 브랜치가 로컬이나 원격에 있고 main에 병합되지 않았으면 그 카드는 배정하지 않는다
- B2 (AC #2) 오케스트레이터가 직접 하는 기록 커밋의 환경에는 AI_AGENT · CLAUDE_CODE_SESSION_ID · CLAUDE_PID가 없다
- B3 (AC #4) commits 값이 없어도 Token Budget 실제치는 session 값만으로 기록된다
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 TaskCardSource.claim · Card.claimedBy와 BacklogCardSource.claim · isLocked · LockKind · LockVerdict, 관련 pending 테스트를 삭제한다
- [x] #2 BacklogCardSource.STRIPPED_ENV에 CLAUDE_PID를 넣고 recordAttempt · recordActual 커밋 환경 테스트를 맞춘다
- [x] #3 BacklogClient.isRunnable이 task/<ID> 브랜치(로컬 · 원격, git cherry 기준 미병합)가 있으면 false를 돌려준다
- [x] #4 Token Budget 실제치 형식에서 commits를 생략할 수 있게 하고 session만 기록하는 경로를 테스트한다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 ./gradlew test 통과
- [x] #2 pending 테스트(./gradlew pendingTest) 상태를 final summary에 남긴다 (수동)
- [x] #3 각 커밋 본문에 Refs: coralstay-software-factory TASK-19 (수동)
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

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
claim · 사람 락 제거, 커밋 환경 CLAUDE_PID 제거, 미병합 task/<ID> 브랜치가 있으면 배정 안 함, Token Budget 실제치 commits 생략을 코드에 반영했다(decision-14 · 5 · 6 · 8).

- AC#1 (055e1e1): TaskCardSource.claim · IN_PROGRESS, Card.claimedBy, BacklogCardSource.claim · isLocked · LockKind · LockVerdict · AGENT 삭제, pending 테스트 DRAFT-1#3 claim · DRAFT-2#2 · DRAFT-2#4 삭제. CardSourceApiTest(비 pending)가 리플렉션으로 제거를 확인.
- AC#2 · B2 (f85224f): STRIPPED_ENV = AI_AGENT, CLAUDE_CODE_SESSION_ID, CLAUDE_PID. CardSourceApiTest가 목록을 확인, recordAttempt · recordActual pending 테스트의 부모 환경 · 검증에 CLAUDE_PID 추가.
- AC#3 · B1 (480f848): BacklogClient.isRunnable(task, lookup, hasUnmergedTaskBranch)로 바꾸고 hasUnmergedTaskBranch(id) 추가 — for-each-ref로 refs/heads/task/<ID>와 refs/remotes/*/task/<ID>를 찾고 git cherry main <ref>에 + 줄이 있으면 미병합. 가짜 실행기 테스트 + 임시 저장소 실제 git 테스트(원격 · 로컬 미병합, ff 병합 · cherry-pick 뒤 병합). 2인자 isRunnable은 브랜치 검사를 건너뛸 수 없게 없앴다. WorktreeManager.create의 브랜치 존재 거부는 유지.
- AC#4 · B3 (98eef27): Attempt · Actual의 commits를 Optional<Tokens>로 바꾸고 session만 받는 생성자 추가, 포트 javadoc 형식을 session만으로. TokenBudget.render · parse와 recordAttempt · recordActual은 DRAFT-1/2 스텁이라 형식 경로는 pending 테스트(session만 줄 + commits 있는 줄 읽기)와 픽스처 ao-90 · ao-91로 고정하고, 실행되는 검증은 CardSourceApiTest의 값 타입 테스트다.

테스트 증거: ./gradlew test — 73 통과, 실패 0. ./gradlew pendingTest — pending 66개 모두 실패(전부 UnsupportedOperationException 스텁: TaskCardSourceTest 22 · BacklogCardSourceTest 7 · AgentRunnerTest 13 · ModelPolicyTest 24), 컴파일 정상.
<!-- SECTION:FINAL_SUMMARY:END -->
