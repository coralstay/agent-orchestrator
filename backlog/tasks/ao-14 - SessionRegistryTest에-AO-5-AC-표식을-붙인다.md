---
id: AO-14
title: SessionRegistryTest에 AO-5 AC 표식을 붙인다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:37'
updated_date: '2026-10-04 02:03'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-10
  - AO-5
references:
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/registry/SessionRegistryTest.java
priority: medium
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
SessionRegistryTest의 모든 테스트에 `AO-5#<AC>` 표식(@DisplayName)을 붙여, 카드 AC와 테스트를 1:1로 추적할 수 있게 한다. AC에 매핑되지 않는 보조 테스트는 `AO-5 보조:`로 표시한다.

## 맥락
2026-10-04 대조 결과: AO-5의 코드 AC는 모두 테스트가 있지만 어떤 테스트가 어떤 AC인지 표식이 없다. 

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/registry/SessionRegistryTest.java`

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 AO-5의 코드 AC마다 최소 하나의 테스트가 `AO-5#<AC>` 표식을 가진다
- [x] #2 AC에 매핑되지 않는 테스트는 `AO-5 보조:` 표식을 가진다
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
대조표: AO-1~6 AC↔테스트 매핑은 이 카드를 만들 때의 탐색 결과에 있다.

증거: ./gradlew test 통과 — SessionRegistryTest tests=16 failures=0 (변경 전후 동일), 전체 61개 동일. AO-5#1 3개, #2 4개, #3 3개, 보조 6개. ./gradlew pendingTest BUILD SUCCESSFUL, 실패 0 — 이 카드는 pending 테스트를 추가하지 않으므로 기대와 일치. 커밋 ce386df.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
SessionRegistryTest의 16개 테스트 전부에 @DisplayName 표식을 붙였다: AO-5#1(admit) 3개, #2(retire) 4개, #3(live) 3개, 'AO-5 보조:' 6개. 동작 변경 없음(16/16 통과, 전체 61개 동일). pendingTest 실패 0.
<!-- SECTION:FINAL_SUMMARY:END -->
