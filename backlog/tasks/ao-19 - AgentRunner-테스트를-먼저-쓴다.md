---
id: AO-19
title: AgentRunner 테스트를 먼저 쓴다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:38'
updated_date: '2026-10-04 02:03'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-10
references:
  - decision-7
  - decision-8
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/agent/AgentRunnerTest.java
  - src/main/java/com/coralstay/orchestrator/agent/AgentRunner.java
priority: high
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
draft-3 (AgentRunner)을 구현하기 전에 테스트부터 쓴다. 공개 API 시그니처 스텁을 만들어 컴파일되게 하고, 근거 카드의 AC를 pending 테스트로 옮긴다. 구현 카드가 pending 태그를 떼고 초록으로 만드는 것이 그 카드의 완료 조건이 된다.

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/agent/AgentRunnerTest.java`
- `src/main/java/com/coralstay/orchestrator/agent/AgentRunner.java` (시그니처 스텁)

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `AgentRunner` 포트와 ClaudeCodeRunner 시그니처 스텁을 만들고 컴파일된다(기존 AgentSpawner는 아직 그대로)
- [x] #2 draft-3 #2(명령에 --model·--output-format json·push 금지, --bare 없음)와 #3(헤드리스 JSON에서 session_id·usage → in/out, 실패 시 unavailable)을 pending 테스트로 쓴다
- [x] #3 세션 총량 파싱은 2026-10-03 스파이크의 실제 out.json을 테스트 리소스로 쓴다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 `./gradlew test`가 통과한다(pending 제외)
- [x] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
- [ ] #3 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(merge commit으로 병합)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 근거 카드 AC를 읽고 테스트 목록 작성.
2. 시그니처 스텁 → 컴파일.
3. pending 테스트 작성(표식 포함).
4. `./gradlew test` 초록, `./gradlew pendingTest` 빨강 확인.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## 요약
- API: `agent/AgentRunner` 포트 — `start(role, cwd, prompt, model, timeout) → RunningAgent`(role/pid/process/startedAt/await), 기본 `run()`, 결과 `AgentRun(exitCode, stdout, stderr, pid, timedOut, duration, Session)`. `Tokens(in, out)`, `Session(sessionId, tokens, unavailableReason)` + `of()`/`unavailable()`/`available()`. 중첩 `ClaudeCodeRunner`: `command(prompt, model)`, static `parseSession(stdoutJson)`, `start()` — 본문 `pending: DRAFT-3`. 상수 `DISALLOWED_TOOLS = "Bash(git push:*)"`.
- 메모: card 패키지에 Tokens가 생기면 `AgentRunner.Tokens`와 합친다.
- 리소스: `src/test/resources/agent/claude-headless-result.json` — 스파이크 out.json에서 세션 무관 필드(subagent_stats, fast_mode_*, 타이밍 ms, uuid 등)를 덜고 result를 첫 문장으로 줄임. session_id·usage(iterations 포함 — 합산 함정 확인용)·modelUsage·is_error·subtype 유지. 민감 정보 없음.
- pending 테스트(13건, 6메서드): DRAFT-3#2 프롬프트 단일 인자 / --model·--permission-mode auto·--output-format json / --disallowedTools git push가 마지막 인자(claude --help상 `<tools...>` 가변 인자라 뒤 인자를 삼킴) / --bare·skip-permissions 없음; DRAFT-3#3 스파이크 JSON → session_id, in=80943 out=782 / null·빈·공백·깨짐·배열·usage 없음·usage 타입 오류 → 예외 없이 unavailable(파라미터 8건).
- DRAFT-3#1(AgentSpawner 이동 후 기존 테스트 초록)은 구현 시점 AC — AgentSpawnerTest를 옮겨 통과시키는 것이 검증이라 pending으로 쓰지 않음. AgentSpawner는 손대지 않음.
- 증거: `./gradlew test --rerun` 61 passed 0 failed, BUILD SUCCESSFUL. `./gradlew pendingTest` 13 tests completed, 13 failed(전부 AgentRunnerTest, UnsupportedOperationException).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
AgentRunner 포트와 ClaudeCodeRunner 시그니처 스텁, 스파이크 실제 헤드리스 JSON 리소스, DRAFT-3 #2·#3 pending 테스트 13건을 추가했다. 기본 test 초록, pendingTest 13건 빨강.
<!-- SECTION:FINAL_SUMMARY:END -->
