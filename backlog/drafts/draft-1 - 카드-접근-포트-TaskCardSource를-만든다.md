---
id: DRAFT-1
title: 카드 접근 포트 TaskCardSource를 만든다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-29 02:44'
labels:
  - skeleton
  - card
  - port
milestone: m-0
dependencies:
  - AO-1
  - AO-17
references:
  - decision-6
  - decision-14
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/card/TaskCardSource.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
오케스트레이터 코어가 카드 시스템(backlog.md 등)을 모른 채 카드를 읽고, 검증하고, 시도·실제치를 기록할 수 있게 하는 포트. 카드의 In Progress 전환은 에이전트 세션이 태스크 브랜치에서 하므로 포트에 두지 않는다(decision-14).

## 맥락
카드는 사용자의 LLM 도구가 만든다. 예상·실제 토큰은 Description의 `### Token Budget` 소절에 둔다(decision-6). 카드 시스템이 바뀌거나 backlog.md #918이 해결돼 frontmatter로 옮겨도 코어는 바뀌지 않아야 한다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/card/TaskCardSource.java` (인터페이스 + 값 타입: Card, TokenBudget, Tokens(in, out), Attempt, CardCheck)
- `src/test/java/com/coralstay/orchestrator/card/TaskCardSourceTest.java` (메모리 구현으로 계약 확인)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `TaskCardSource.read(id)`: Card(id, 제목, 상태, 담당자, 의존, AC 목록, 대상 파일, TokenBudget)를 돌려준다 — TokenBudget은 estimated(Tokens in/out)·attempts·actual
- [ ] #2 `validate(card)`: Token Budget 누락·형식 오류를 사유와 함께 CardCheck로 돌려준다(예외로 흐름을 끊지 않음)
- [ ] #3 `recordAttempt(id, attempt)`·`recordActual(id, actual)` 계약을 정의하고 메모리 구현 테스트로 상태 전이를 확인한다(실제치는 `session`만, `commits`는 최상위 Q-18 이후)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-trail 형식 검사를 통과했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 값 타입부터: Tokens(in,out) — `in=N out=M` 파싱/출력, TokenBudget, Attempt(session, model, reason). `commits`는 최상위 Q-18 이후(decision-8 단서).
2. 인터페이스 메서드와 CardCheck.
3. 테스트용 메모리 구현으로 attempt → actual 흐름 확인.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
실제 backlog.md 구현은 BacklogCardSource 태스크. Tokens 파싱 규칙(`in=N out=M`)은 CommitLedger(AO-6, 최상위 Q-18 의존으로 보류)가 재개되면 같은 규칙을 쓴다 — 중복이면 한쪽으로 모은다.
<!-- SECTION:NOTES:END -->
