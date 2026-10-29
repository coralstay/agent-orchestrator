---
id: DRAFT-9
title: '충돌 결정 반영(문서): claim 커밋 폐기 · 착수 = task 브랜치 · 토큰은 session만 · 배정 조건에 브랜치 없음'
status: Draft
assignee: []
created_date: '2026-10-29 02:31'
updated_date: '2026-10-29 02:31'
labels:
  - design
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
최상위 coralstay-software-factory DRAFT-1(승격 후 TASK-19 예정)의 전파. 유저 결정(2026-10-29) A1 · A2 · A3 · A4 · A5가 이 저장소의 decision-5 · 8 · 9와 design/, 그리고 열린 PR #20(AO-28)이 넣은 claim · 작업 순서 문구와 충돌한다. 유저 결정: PR #20은 단독 병합하지 않고 이 카드를 task/AO-28 위에 쌓아 고친 뒤 함께 병합한다.

- A1 · A2: main claim 커밋(decision-9)을 없앤다. 에이전트 세션이 task/<ID> 브랜치에서 In Progress로 바꾸고, 착수 판단은 미병합 task/<ID> 브랜치의 존재다. 전환 실패는 '착수 안 됨' 일반 실패로 회수, 같은 카드 중복 실행은 오케스트레이터 자체 잠금(SessionRegistry.admit). 오케스트레이터는 훅(계획 승인 · 작업 순서 검사)을 모른다.
- A3: 오케스트레이터가 직접 하는 커밋은 실제치 기록뿐이고, AI_AGENT · CLAUDE_CODE_SESSION_ID · CLAUDE_PID를 지우고 실행한다. '프로그램 커밋' 구분은 coding-agent-git-commit-tool GF-130 때.
- A4: 토큰 실제치는 당분간 session(헤드리스 JSON usage, ClaudeCodeRunner)만. commits(CommitLedger 트레일러 합산)는 최상위 Q-18 이후 — coding-agent-git-commit-tool은 더 이상 Tokens-Used를 남기지 않는다(최상위 ADR-021).
- A5: 배정 조건(ORC-005) = isReady ∧ To Do ∧ task/<ID> 브랜치 없음. work-order.md는 읽지 않는다.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 decision-14를 만들어 decision-9(claim 커밋)를 대체한다 — 착수는 에이전트 세션이 task/<ID> 브랜치에서, 전환 실패는 '착수 안 됨' 회수, 중복 실행은 자체 잠금
- [ ] #2 decision-5를 고친다(CommitLedger · 트레일러 합산은 Q-18 이후로 보류, 지우는 환경변수에 CLAUDE_PID 추가, 직접 커밋은 실제치 기록뿐)하고 decision-8에 commits는 Q-18 이후라는 단서를 단다
- [ ] #3 design/(01-배경과-문제 · 02-원칙 P4 · P5 · P7 · P9 · 04 리스크 · 2.02 · 2.03 ORC-005 · ORC-021 · 3.01 · 3.02 · 3.04 · 3.05 · 4.03 · 용어집)의 claim · 사람 락 · work-order 문구를 결정에 맞게 고친다 — PR #20이 넣은 문구 포함
- [ ] #4 영향받는 카드 · draft를 고친다: AO-7(실행 순서 · AC#2), AO-20 AC#3, AO-8 · AO-21 스모크 (a), m-0 완료 조건, doc-1 D11, DRAFT-1 · DRAFT-2. AO-6(CommitLedger)은 Q-18 의존으로 보류 메모를 남기고 AO-7 · AO-20 의존에서 뺀다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 옛 표현 grep 0건(claim, 사람 락, work-order) (수동)
- [ ] #2 파일 하나에 커밋 하나, 각 커밋 본문에 Refs: coralstay-software-factory TASK-19 (수동)
- [ ] #3 push는 PR #20과 함께, 단독 병합 금지 (수동)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Subjective
유저 결정(2026-10-29) A1~A5를 이 저장소 결정 · 설계 문서 · 카드에 반영한다. PR #20(AO-28)은 이 카드를 위에 쌓아 고친 뒤 함께 병합한다(유저 선택).

### Objective
- decision-9:14-17 · 27-30이 main claim 커밋 · 사람 락을 정한다. decision-5:15-17 · 29가 CommitLedger 트레일러 합산과 claim · 실제치 커밋의 환경변수 2개 제거를 정한다.
- PR #20이 claim 전 계획 승인 · 작업 순서 게이트를 01 · 02 · 2.02 · 2.03 · 3.01 · 3.02 · 3.04 · 3.05 · 4.03 · 용어집에 넣었다.
- CommitLedger는 로컬 task/AO-6(미push, 25일 정지)에만 있다. 배정 코드는 없고 BacklogClient.isRunnable이 가장 가깝다.

### Assessment
문서를 먼저 고치고(최상위 00 편집 규칙) 코드는 다음 카드에서 맞춘다. claim이 사라지면 사람 락(LockKind)도 근거를 잃으므로 함께 대체한다.

### Plan
1. decision-14 작성, decision-9 · 5 · 8 수정.
2. design/ 장마다 고치고 파일마다 커밋.
3. 카드 · draft 수정(backlog CLI).
4. grep 확인 후 Done.
<!-- SECTION:PLAN:END -->
