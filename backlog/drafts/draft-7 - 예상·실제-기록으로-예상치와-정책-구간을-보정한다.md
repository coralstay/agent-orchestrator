---
id: DRAFT-7
title: 예상·실제 기록으로 예상치와 정책 구간을 보정한다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-03 13:09'
labels:
  - m2
  - policy
milestone: m-1
dependencies:
  - DRAFT-4
references:
  - decision-8
  - decision-10
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java
priority: low
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
쌓인 예상·실제 토큰 기록으로 다음 예상치와 ModelPolicy 구간을 보정한다.

## 맥락
카드 Token Budget의 estimated/actual과 runs.jsonl이 자료다(decision-6·8). LLM이 카드를 만들 때 이 기록을 참고하게 하는 것이 최종 목표.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/policy/`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 예상 대비 실제 비율 분포를 카드·runs.jsonl에서 집계한다
- [ ] #2 정책 구간을 분포 기반으로 제안한다
- [ ] #3 카드 작성 도구에 참고 자료로 줄 요약 형식을 정한다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 draft 검토 후 승격 여부를 유저가 결정했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
M1 E2E와 실사용으로 표본이 쌓인 뒤 시작.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
표본 수가 적으면 보정하지 않는다는 기준을 함께 정한다.
<!-- SECTION:NOTES:END -->
