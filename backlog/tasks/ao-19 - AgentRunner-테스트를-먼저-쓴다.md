---
id: AO-19
title: AgentRunner 테스트를 먼저 쓴다
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-04 01:38'
updated_date: '2026-10-04 01:47'
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
- [ ] #1 `AgentRunner` 포트와 ClaudeCodeRunner 시그니처 스텁을 만들고 컴파일된다(기존 AgentSpawner는 아직 그대로)
- [ ] #2 draft-3 #2(명령에 --model·--output-format json·push 금지, --bare 없음)와 #3(헤드리스 JSON에서 session_id·usage → in/out, 실패 시 unavailable)을 pending 테스트로 쓴다
- [ ] #3 세션 총량 파싱은 2026-10-03 스파이크의 실제 out.json을 테스트 리소스로 쓴다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test`가 통과한다(pending 제외)
- [ ] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
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
스파이크 원본: scratchpad/spike-headless/out.json — 개인 정보(session_id 외) 없는지 확인 후 리소스로.
<!-- SECTION:NOTES:END -->
