---
id: DRAFT-3
title: 에이전트 실행 포트 AgentRunner와 Claude 구현을 만든다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-04 01:38'
labels:
  - skeleton
  - agent
  - port
  - docs-sync
milestone: m-0
dependencies:
  - AO-4
  - DRAFT-18
references:
  - decision-7
  - decision-8
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
  - ../coralstay-software-factory/에이전트_레일_파이프라인.html
  - ../coralstay-software-factory/README.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/agent/AgentRunner.java
  - src/main/java/com/coralstay/orchestrator/spawn/AgentSpawner.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
에이전트 실행을 AgentRunner 포트 뒤로 옮기고, 지금의 AgentSpawner를 그 Claude 구현(ClaudeCodeRunner)으로 바꾼다. 실행 결과에 헤드리스 JSON의 세션 총량을 담는다.

## 맥락
- 범위는 Claude Code 전용, 실행부만 포트(decision-7).
- 실제치는 커밋 귀속분과 세션 총량 병기(decision-8) — 세션 총량은 `--output-format json`의 `usage`(input + cache_creation + cache_read / output).
- 스파이크: 헤드리스 Claude는 프롬프트에 "승인·승격된 태스크"라고 밝혀야 전역 CLAUDE.md의 승인 절차에서 멈추지 않는다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/agent/AgentRunner.java` (포트 + ClaudeCodeRunner — spawn/AgentSpawner.java를 옮김)
- 테스트: 기존 AgentSpawnerTest를 옮기고 JSON usage 파싱 테스트 추가
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `AgentRunner` 포트(start/await, AgentRun 결과)를 정의하고 AgentSpawner를 ClaudeCodeRunner로 옮긴다 — 기존 테스트가 모두 통과한다
- [ ] #2 `command()`: `claude -p <prompt> --model <m> --permission-mode auto --output-format json --disallowedTools "Bash(git push:*)"`를 조립하고 `--bare`는 넣지 않는다
- [ ] #3 `parseSession(stdout)`: 헤드리스 JSON에서 session_id와 usage를 읽어 Tokens(in=input+cache_creation+cache_read, out=output)로 돌려주고, 실패하면 unavailable 사유를 남긴다
- [ ] #4 팩토리 문서 D12 반영: RFC 06절의 다중 프로바이더 서술을 Claude 전용 + 실행부 포트로 정정했다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-trail 형식 검사를 통과했다
- [ ] #3 팩토리 `docs/orchestrator-skeleton` 브랜치에 문서 반영 커밋이 있다
- [ ] #4 doc-1 해당 행을 ✅(팩토리 커밋 해시) 또는 ❌로 갱신했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 포트 정의, AgentSpawner 이동(동작 변경 없음) → 테스트 통과 확인 후 커밋.
2. 명령 조립 변경(모델·json·push 금지).
3. JSON 파서 — 스파이크의 실제 out.json을 테스트 리소스로.
4. 팩토리 D12.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
SessionRegistry는 ProcessHandle만 받으므로 영향 없음. 스파이크 원본 출력: scratchpad/spike-headless/out.json. --disallowedTools 문법은 claude --help로 다시 확인.
<!-- SECTION:NOTES:END -->
