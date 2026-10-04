---
id: AO-11
title: MainTest에 AO-1 AC 표식을 붙인다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:37'
updated_date: '2026-10-04 02:02'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-10
  - AO-1
references:
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/MainTest.java
priority: medium
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
MainTest의 모든 테스트에 `AO-1#<AC>` 표식(@DisplayName)을 붙여, 카드 AC와 테스트를 1:1로 추적할 수 있게 한다. AC에 매핑되지 않는 보조 테스트는 `AO-1 보조:`로 표시한다.

## 맥락
2026-10-04 대조 결과: AO-1의 코드 AC는 모두 테스트가 있지만 어떤 테스트가 어떤 AC인지 표식이 없다. AO-1#3(Gradle·Java 25·JUnit 설정)은 빌드 자체가 검증하므로 테스트를 두지 않고 카드 메모에 적는다.

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/MainTest.java`

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 AO-1의 코드 AC마다 최소 하나의 테스트가 `AO-1#<AC>` 표식을 가진다
- [x] #2 AC에 매핑되지 않는 테스트는 `AO-1 보조:` 표식을 가진다
- [x] #3 표식만 붙이고 테스트 동작은 바꾸지 않는다(테스트 수·결과 동일)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 `./gradlew test`가 통과한다(pending 제외)
- [x] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
- [ ] #3 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(merge commit으로 병합)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 카드 AC 목록과 테스트 메서드를 대조(2026-10-04 대조표 참고).
2. @DisplayName 추가.
3. `./gradlew test`로 수·결과가 그대로인지 확인.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
대조표: AO-1~6 AC↔테스트 매핑은 이 카드를 만들 때의 탐색 결과에 있다. AO-1#3(Gradle·Java 25·JUnit 설정)은 빌드 자체가 검증하므로 테스트를 두지 않고 카드 메모에 적는다.

검증(2026-10-04, 커밋 bd21f24):
- AC1: AO-1#1 → parsesRunWithTaskId, AO-1#2 → noArgsIsUsageError, unknownCommandIsUsageError, badArgsPrintUsageToStderrAndExit2(파라미터화 3건, 이름 패턴 "{displayName} [{index}] args=..."). AO-1#3(Gradle·Java 25·JUnit·testCommand)은 빌드 자체(./gradlew test 통과)가 검증하므로 테스트 없음. AO-1#4는 문서 AC라 테스트 대상 아님.
- AC2: runWithoutTaskIdIsUsageError, runWithExtraArgsIsUsageError, blankTaskIdIsUsageError, runPrintsTaskIdAndExits0 → 'AO-1 보조:' 표식.
- AC3: build/test-results 비교 — MainTest 전/후 tests=10 skipped=0 failures=0 errors=0, 전체 61건 동일. 변경은 @DisplayName/이름 패턴과 import뿐.
- DoD1: ./gradlew test BUILD SUCCESSFUL(61건 통과).
- DoD2: ./gradlew pendingTest BUILD SUCCESSFUL, pending 테스트 0건(이 카드는 pending 테스트를 추가하지 않으므로 기대한 실패 목록=없음과 일치).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
MainTest의 10개 테스트 케이스 전부에 AO-1 AC 표식(@DisplayName)을 붙였다. AO-1#1·#2는 매핑 테스트, 나머지 4개는 'AO-1 보조:'. AO-1#3은 빌드가 검증하므로 메모로 남겼다. 테스트 수·결과는 전후 동일(MainTest 10, 전체 61, 실패 0).
<!-- SECTION:FINAL_SUMMARY:END -->
