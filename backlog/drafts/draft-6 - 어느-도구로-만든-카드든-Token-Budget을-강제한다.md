---
id: DRAFT-6
title: 어느 도구로 만든 카드든 Token Budget을 강제한다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-03 13:09'
labels:
  - m2
  - card
milestone: m-1
dependencies:
  - DRAFT-2
references:
  - decision-6
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - AGENTS.md
priority: medium
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
사용자가 어떤 LLM 도구로 카드를 만들어도 Token Budget 소절이 빠지지 않게 한다.

## 맥락
지금은 사용자의 Claude(claude-rails)만 카드 규칙을 강제하고, 오케스트레이터 입구 검증이 마지막 방어선이다. 모든 도구가 거치는 곳은 git 커밋이다.

## 대상 파일
- `AGENTS.md`(카드 작성 지침)
- git 훅 카드 스키마 검사기(설치 스크립트 포함)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 AGENTS.md에 카드 작성 규칙(Token Budget 형식·단위)을 적는다
- [ ] #2 `backlog/tasks/*.md` 커밋 시 Token Budget 스키마를 검사하는 git 훅을 만든다
- [ ] #3 기존 git-trail 훅과 함께 설치되는 방식을 정한다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 draft 검토 후 승격 여부를 유저가 결정했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
backlog init의 agent instructions 옵션(claude/agents/gemini/copilot/cursor) 활용 검토.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
git-trail이 core.hooksPath를 쓰므로 훅 체이닝 방식 확인 필요.
<!-- SECTION:NOTES:END -->
