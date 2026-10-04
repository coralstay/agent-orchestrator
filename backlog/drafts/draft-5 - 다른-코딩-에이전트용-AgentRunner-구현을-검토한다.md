---
id: DRAFT-5
title: 다른 코딩 에이전트용 AgentRunner 구현을 검토한다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-03 13:09'
labels:
  - m2
  - agent
milestone: m-1
dependencies:
  - DRAFT-3
references:
  - decision-7
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/agent/AgentRunner.java
priority: low
type: spike
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
Codex·Gemini CLI·Copilot CLI·Cursor·OpenCode·Qwen Code·Amp 등의 AgentRunner 구현 여부를 결정할 근거를 만든다.

## 맥락
2026-10-03 조사: 대부분 헤드리스 실행을 지원하지만 토큰 보고 방식이 제각각(Cursor는 없음)이고, git-trail은 Claude 외 도구를 `unavailable (no-usage-channel)`로 남긴다. Aider는 기본이 `--no-verify` 커밋이라 훅을 건너뛴다. 제품마다 claude-rails 같은 규칙 강제가 없다(decision-7 기각 사유).

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/agent/` (제품별 구현 후보)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 제품별 헤드리스 명령·토큰 보고·커밋 방식 표를 실측으로 갱신한다
- [ ] #2 규칙 강제(테스트 게이트·상태 전환)를 오케스트레이터가 넘겨받는 방안을 정리한다
- [ ] #3 git-trail에 제품별 사용량 채널을 추가할지 오케스트레이터가 출력에서 읽을지 결정안을 낸다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 draft 검토 후 승격 여부를 유저가 결정했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
조사 결과(대화 기록의 21개 CLI 표)를 출발점으로, 설치 가능한 2~3개를 실측.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
승격 전 판단 기준: M1 E2E에서 Claude 단독으로 충분한지.
<!-- SECTION:NOTES:END -->
