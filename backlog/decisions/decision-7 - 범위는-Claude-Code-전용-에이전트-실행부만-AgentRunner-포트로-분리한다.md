---
id: decision-7
title: '범위는 Claude Code 전용, 에이전트 실행부만 AgentRunner 포트로 분리한다'
date: '2026-10-03 13:03'
status: accepted
---
## Context

여러 코딩 에이전트 CLI(Codex, Gemini, Copilot, Cursor 등)를 함께 지원하는 안을 검토했다. 제품마다 권한 모델·출력 형식·토큰 보고·커밋 방식이 달라서,
지원 제품이 늘수록 오케스트레이터가 보장할 수 있는 것은 "띄우고 결과를 받는다"로 줄어든다. 이 저장소의 출발점은 claude-rails를 역할별로 확장하는 것이다.

## Decision

- 구현하는 에이전트는 **Claude Code 하나**다.
- 에이전트 실행부만 `agent/AgentRunner` 포트로 분리하고, 지금의 `spawn/AgentSpawner`를 그 Claude 구현으로 옮긴다.
- 에이전트 안의 규칙(task view 필독, In Progress, AC마다 커밋, 커밋 전 테스트, Done)은 claude-rails가 강제한다. 오케스트레이터는 프롬프트로 절차를 지시하고 결과를 브랜치 상태로 판정한다.

## Alternatives

- 다중 제품 어댑터: 결함 목록(훅 이식 불가, 커밋 대행으로 AC 단위 커밋 소실, 제품별 토큰 형식, 권한 모델 차이)이 커서 기각.

## Consequences

- 커밋은 Claude가 claude-rails대로 AC마다 한다. 오케스트레이터는 코드를 대신 커밋하지 않는다.
- 헤드리스 Claude도 전역 claude-rails 훅과 CLAUDE.md를 따른다. 프롬프트에 "승인·승격된 태스크"임을 밝혀야 승인 대기 없이 진행한다(스파이크로 확인).
- 다른 제품 지원은 M2 draft로만 남긴다.
