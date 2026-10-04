---
id: AO-16
title: ModelPolicy 테스트를 먼저 쓴다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:38'
updated_date: '2026-10-04 02:05'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-10
references:
  - decision-10
  - decision-8
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/policy/ModelPolicyTest.java
  - src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java
priority: high
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
draft-4 (ModelPolicy)을 구현하기 전에 테스트부터 쓴다. 공개 API 시그니처 스텁을 만들어 컴파일되게 하고, 근거 카드의 AC를 pending 테스트로 옮긴다. 구현 카드가 pending 태그를 떼고 초록으로 만드는 것이 그 카드의 완료 조건이 된다.

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/policy/ModelPolicyTest.java`
- `src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java` (시그니처 스텁)

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `ModelPolicy` 시그니처 스텁(load, choose, 결과 타입)을 만들고 컴파일된다
- [x] #2 draft-4 #1~3(설정 파싱·형식 오류 줄 번호, 구간 선택·경계값, 설정 파일 없을 때 내장 기본값 표시)을 pending 테스트로 쓴다
- [x] #3 `pendingTest`에서 이 테스트들이 UnsupportedOperationException으로 실패하는 것을 확인한다
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
구간 경계: '이하'인지 '미만'인지 테스트로 못박는다(draft-4는 '이하').

## 2026-10-04 검증 (f0618cd, 032818a)
- API: `ModelPolicy.load(Path)`(파일 없으면 builtIn), `builtIn()`, `choose(long estimatedInOut)` → `Choice(model, source)`+`builtIn()`, `buckets()`(SortedMap 상한→모델), `defaultModel()`, `source()`(FILE/BUILT_IN), `PolicyFormatException(lineNumber)`(default 누락은 줄 0), 상수 `BUILT_IN_SMALL_UP_TO=200000`/haiku/sonnet.
- `./gradlew test`: BUILD SUCCESSFUL (pending 제외).
- `./gradlew pendingTest`: 24 tests completed, 24 failed — 전부 ModelPolicyTest(DRAFT-4#1 11건: 파싱 1, default만 1, 형식오류 8, default 누락 1 / #2 10건: 경계 9 + 음수 1 / #3 3건). 각 실패의 원인은 UnsupportedOperationException("pending: DRAFT-4") (assertThrows 감싼 3+8건은 AssertionFailedError caused by UOE). 다른 클래스 실패 없음.
- 경계: 상한과 같으면 그 구간("이하") — 200000→haiku, 200001→sonnet으로 못박음.
- 메모: CsvSource textBlock은 `#`로 시작하는 행을 주석으로 버리므로 주석 줄 케이스는 행 중간에 둠.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
ModelPolicy 시그니처 스텁(load/builtIn/choose, Choice(model, source), PolicyFormatException(lineNumber), 내장 기본값 상수)과 DRAFT-4 #1~3 pending 테스트 24건(파싱·형식 오류 줄 번호 11, 구간 선택 경계값 '이하' 10, 내장 기본값 표시 3)을 추가했다. ./gradlew test 통과, ./gradlew pendingTest는 이 24건만 UnsupportedOperationException(pending: DRAFT-4)으로 실패. DRAFT-4 구현 카드는 pending 태그를 떼고 초록으로 만들면 된다.
<!-- SECTION:FINAL_SUMMARY:END -->
