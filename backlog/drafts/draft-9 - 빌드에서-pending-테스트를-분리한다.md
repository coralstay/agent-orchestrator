---
id: DRAFT-9
title: 빌드에서 pending 테스트를 분리한다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-04 01:37'
updated_date: '2026-10-04 01:38'
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
- [ ] #1 `test` 태스크가 `pending` 태그 테스트를 제외한다(태그 붙은 샘플 테스트가 `test`에서 실행되지 않음을 확인)
- [ ] #2 `pendingTest` 태스크가 `pending` 태그 테스트만 실행한다
- [ ] #3 pending 테스트가 0개여도 `pendingTest`가 실패하지 않는다
- [ ] #4 README에 `./gradlew pendingTest` 사용법을 한 줄 적었다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test`가 통과한다(pending 제외)
- [ ] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
- [ ] #3 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(merge commit으로 병합)
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
<!-- SECTION:NOTES:END -->
