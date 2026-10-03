---
id: AO-9
title: 재설계를 백로그에 반영한다
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-03 13:05'
updated_date: '2026-10-03 13:09'
labels:
  - backlog
  - redesign
  - docs-sync
milestone: m-0
dependencies: []
references:
  - decision-5
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - backlog/decisions
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
  - backlog/tasks
  - backlog/drafts
priority: high
type: docs
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
승인된 재설계(카드의 예상 토큰으로 Claude를 헤드리스 배정하고 실제 토큰을 카드에 기록)를 backlog에 반영한다: 결정 문서, doc-1 행, AO-7·AO-8 개편, 새 M1 draft 4개, M2 마일스톤과 draft.

## 맥락
- 2026-10-03 재설계 계획 승인. 범위는 Claude Code 전용 + 에이전트 실행부만 포트로 분리.
- 실측 스파이크로 헤드리스 `claude -p` 세션도 git-trail 트레일러가 실측됨을 확인. 트레일러는 마지막 커밋 뒤 턴을 포함하지 않아 세션 총량(헤드리스 JSON)과 병기하기로 함.
- 카드 필드는 Description의 `### Token Budget` 소절(라벨은 분류용, frontmatter는 편집 시 소실 — backlog.md #918).
- main 반영은 PR로 하고, git-trail은 브랜치명에 Task-Id를 요구하므로 이 정리 작업도 태스크로 진행한다.

## 대상 파일
- `backlog/decisions/decision-5..10`
- `backlog/docs/doc-1 - 팩토리-문서-검증-로그.md`
- `backlog/tasks/ao-7*`, `ao-8*`
- `backlog/drafts/*`, `backlog/milestones/*`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 decision-6~10을 추가하고 decision-5를 도구 중립(커밋 트레일러 계약) 기준으로 다시 썼다
- [x] #2 doc-1에 스파이크 결과(D5 헤드리스 실측, D9 peak RSS)와 재설계 반영 행을 추가했다
- [ ] #3 AO-7을 새 실행 순서(카드 검증 → 판정 → 정책 → claim → 워크트리 → 실행 → 실제치 → 브랜치 상태 판정)로 다시 썼다
- [ ] #4 AO-8을 Token Budget 카드 픽스처와 실제치 병기 확인 기준으로 다시 썼다
- [x] #5 M1 draft 4개(TaskCardSource, BacklogCardSource, AgentRunner, ModelPolicy)를 모든 필드를 채워 만들었다
- [x] #6 M2 마일스톤과 draft(다른 에이전트 어댑터, 도구 무관 카드 강제, 예상치 보정, 실시간 예산 감시)를 만들었다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `backlog draft list --plain`·`task view`로 새 항목의 공란을 점검했다
- [ ] #2 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(병합은 유저)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 결정 문서: decision-6~10 본문, decision-5 재작성(이미 작성해 stash에 있음 → task 브랜치에서 복원).
2. doc-1: D5에 스파이크 결과(트레일러 원문·세션 총량 차이), D9 peak RSS 240~290MB, 새 행(카드 스키마·배정 흐름·실제치 병기·Claude 전용 범위 → 팩토리 반영).
3. AO-7: 설명·AC·계획을 새 실행 순서로 교체, 의존에 새 M1 태스크 추가(promote 후).
4. AO-8: 픽스처 카드에 Token Budget, 확인 항목에 실제치 병기·attempt 기록.
5. M1 draft 4개 생성(모든 필드), M2 마일스톤 + draft 4개.
6. 공란 점검 → 커밋 → push → PR.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
draft→promote 순서상 이 카드 자체는 main(예외 브랜치)에서 만들고 승격한다. 이후 작업은 task/AO-<n> 브랜치. 라이선스 PR(#1)이 먼저 병합되지 않으면 이 브랜치 PR에 라이선스 커밋이 함께 보인다.
<!-- SECTION:NOTES:END -->
