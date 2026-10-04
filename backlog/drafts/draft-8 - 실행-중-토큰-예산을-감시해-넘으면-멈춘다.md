---
id: DRAFT-8
title: 실행 중 토큰 예산을 감시해 넘으면 멈춘다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-03 13:09'
labels:
  - m2
  - budget
milestone: m-1
dependencies:
  - AO-5
  - AO-6
references:
  - decision-5
  - decision-8
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/budget/BudgetGuard.java
priority: low
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
커밋을 지켜보다 예산을 넘으면 사람을 부르고, 한도를 넘으면 프로세스를 종료한다.

## 맥락
M1은 사후 합산까지만 한다(유저 결정). 감지 단위가 커밋 경계라는 한계가 있다. retire는 멱등이라 실행 중 kill과 run의 finally가 충돌하지 않는다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/budget/BudgetGuard.java`
- `src/main/java/com/coralstay/orchestrator/notify/HumanNotifier.java`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 주기적으로 CommitLedger를 읽어 예상치 대비 누적을 계산한다
- [ ] #2 soft 초과 시 사람 호출 1회, hard 초과 시 retire(kill) + 사람 호출
- [ ] #3 unavailable 연속 시 측정 불능으로 사람을 부른다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 draft 검토 후 승격 여부를 유저가 결정했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
사람 호출 기본 구현은 stderr + `backlog task edit --comment`.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
세션 총량 기준 감시가 필요하면 stream-json 실시간 usage를 쓰는 방안도 검토.
<!-- SECTION:NOTES:END -->
