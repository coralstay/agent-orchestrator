---
id: AO-10
title: 빌드에서 pending 테스트를 분리한다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:37'
updated_date: '2026-10-04 01:50'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-1
references:
  - decision-1
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - build.gradle.kts
  - README.md
priority: high
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
아직 구현이 없는 테스트(`@Tag("pending")`)를 기본 `./gradlew test`에서 빼고 `./gradlew pendingTest`로 따로 돌린다. claude-rails 커밋 게이트(`testCommand: ./gradlew test`)를 깨지 않고 테스트를 먼저 커밋하기 위한 장치다.

## 대상 파일
- `build.gradle.kts`
- `README.md` (사용법 한 줄)

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `test` 태스크가 `pending` 태그 테스트를 제외한다(태그 붙은 샘플 테스트가 `test`에서 실행되지 않음을 확인)
- [x] #2 `pendingTest` 태스크가 `pending` 태그 테스트만 실행한다
- [x] #3 pending 테스트가 0개여도 `pendingTest`가 실패하지 않는다
- [x] #4 README에 `./gradlew pendingTest` 사용법을 한 줄 적었다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 `./gradlew test`가 통과한다(pending 제외)
- [x] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
- [x] #3 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(merge commit으로 병합)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. `tasks.test { useJUnitPlatform { excludeTags("pending") } }`.
2. `tasks.register<Test>("pendingTest")` — 같은 classpath, `includeTags("pending")`, `failOnNoDiscoveredTests = false`(Gradle 9 옵션 확인).
3. 임시 pending 샘플로 두 태스크 동작 확인 후 샘플은 지운다.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
이 카드는 다른 테스트 선작성 카드 전부의 선행 조건이다. Gradle 9.4에서 테스트 0개일 때의 기본 동작을 확인할 것.

## 검증 증거 (Gradle 9.4.1, 2026-10-04)

- Gradle 9.4.1 기본 동작: `failOnNoDiscoveredTests`를 주석 처리한 채 pending 테스트 0개로 `./gradlew pendingTest --rerun-tasks` → `BUILD SUCCESSFUL` (태그 필터가 있으면 기본으로 실패하지 않음). 그래도 의도를 명시하려고 `failOnNoDiscoveredTests = false`를 남겼다.
- AC3: 플래그 켠 상태, pending 0개 → `> Task :pendingTest` / `BUILD SUCCESSFUL in 5s`.
- AC1: 임시 `PendingSampleTest`(`@Tag("pending")`, 본문 `fail(...)`) 추가 후 `./gradlew test --rerun-tasks` → `BUILD SUCCESSFUL in 7s`, 출력에 PendingSample 없음, `build/test-results/test/`에 PendingSampleTest XML 없음(기존 5개 클래스만), PASSED 61건.
- AC2: 같은 상태에서 `./gradlew pendingTest --rerun-tasks` → `PendingSampleTest > AO-10 sample: pending only FAILED`, `1 test completed, 1 failed`, `build/test-results/pendingTest/`에 `TEST-com.coralstay.orchestrator.PendingSampleTest.xml`만 존재.
- 샘플 삭제 후(커밋하지 않음) `./gradlew test pendingTest --rerun-tasks` → `BUILD SUCCESSFUL in 7s`.
- DoD2: 이 카드의 기대 실패 목록 = 비어 있음. 실제 `pendingTest` 실패 0건.

커밋: da99f4f (build.gradle.kts), c308b79 (README.md)

PR: https://github.com/coralstay/agent-orchestartor/pull/4 (merge commit으로 병합)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
`test`가 `@Tag("pending")` 테스트를 제외하고, 새 `pendingTest` 태스크(같은 test classpath)가 pending만 실행한다. pending 0개여도 성공(`failOnNoDiscoveredTests = false`; Gradle 9.4.1은 태그 필터 시 기본으로도 실패하지 않음을 확인). 임시 샘플로 두 태스크 동작을 확인한 뒤 삭제했고 README에 사용법 한 줄을 추가했다.
<!-- SECTION:FINAL_SUMMARY:END -->
