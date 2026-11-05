---
id: AO-15
title: AgentSpawnerTest에 AO-4 AC 표식을 붙이고 virtual thread 대기를 검증한다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:37'
updated_date: '2026-10-04 02:04'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-10
  - AO-4
references:
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/spawn/AgentSpawnerTest.java
priority: high
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
AgentSpawnerTest의 모든 테스트에 `AO-4#<AC>` 표식을 붙이고, 빠져 있던 AO-4#2의 "virtual thread에서 waitFor" 검증을 추가한다.

## 맥락
2026-10-04 대조 결과: AO-4#2는 결과 필드만 검증하고 대기 스레드가 virtual인지 단언하지 않는다.

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/spawn/AgentSpawnerTest.java`

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 AO-4의 코드 AC마다 최소 하나의 테스트가 `AO-4#<AC>` 표식을 가진다(보조 테스트는 `AO-4 보조:`)
- [x] #2 AO-4#2: 대기·출력 소비 스레드가 virtual thread임을 단언하는 테스트를 추가한다(예: 실행 중 스레드 덤프에서 `agent-wait-<pid>` 스레드의 isVirtual)
- [x] #3 기존 테스트의 동작은 바꾸지 않는다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 `./gradlew test`가 통과한다(pending 제외)
- [x] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
- [x] #3 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(merge commit으로 병합)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 표식 추가.
2. virtual thread 단언 방법 결정 — Thread.getAllStackTraces()는 virtual thread를 포함하지 않으므로 다른 방법(스레드 이름으로 찾기 어려우면 테스트 가능한 훅 노출 여부 검토).
3. 코드 변경이 필요하면 최소 범위로(테스트용 관찰 지점).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
virtual thread는 Thread.getAllStackTraces()에 안 나온다. 코드에 관찰 지점을 추가해야 하면 AgentSpawner 수정이 이 카드 범위에 들어간다 — 그 경우 카드 메모에 기록.

증거: ./gradlew test 통과(62 tests, 실패 0; AgentSpawnerTest 12개 모두 @DisplayName). ./gradlew pendingTest: pending 태그 테스트 0개, 실패 0 — 이 카드는 pending 테스트를 추가하지 않으므로 기대와 일치.
virtual thread 단언: 프로덕션 변경 없이 com.sun.management.HotSpotDiagnosticMXBean.dumpThreads(JSON)를 사용(virtual thread 포함, 스레드마다 "virtual": true). 가짜 실행 파일 sleep 30 실행 중 덤프에서 agent-wait/stdout/stderr-<pid>를 찾고, 대조군으로 현재 플랫폼 스레드가 false로 읽히는지도 확인. 변이 확인: agent-wait를 Thread.ofPlatform()으로 바꾸면 실패함을 확인 후 원복. AgentSpawner.java 수정 없음.

PR: https://github.com/coralstay/agent-orchestartor/pull/9
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
AgentSpawnerTest 11개 기존 테스트에 AO-4#1/#2/#3 또는 'AO-4 보조:' @DisplayName을 붙이고(동작 변경 없음), AO-4#2 누락 검증으로 대기·stdout·stderr 소비 스레드가 virtual thread임을 JSON 스레드 덤프(HotSpotDiagnosticMXBean.dumpThreads)로 단언하는 테스트를 추가했다. 프로덕션 코드 변경 없음. ./gradlew test 62개 통과.
<!-- SECTION:FINAL_SUMMARY:END -->
