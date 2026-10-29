---
id: AO-20
title: RunCommand 테스트를 먼저 쓴다
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-04 01:38'
updated_date: '2026-10-29 11:06'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-16
  - AO-17
  - AO-18
  - AO-19
references:
  - decision-6
  - decision-7
  - decision-8
  - decision-10
  - decision-11
  - decision-14
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/RunCommandTest.java
  - src/main/java/com/coralstay/orchestrator/RunCommand.java
priority: high
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
AO-7 (RunCommand)을 구현하기 전에 테스트부터 쓴다. 공개 API 시그니처 스텁을 만들어 컴파일되게 하고, 근거 카드의 AC를 pending 테스트로 옮긴다. 구현 카드가 pending 태그를 떼고 초록으로 만드는 것이 그 카드의 완료 조건이 된다.

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/RunCommandTest.java`
- `src/main/java/com/coralstay/orchestrator/RunCommand.java` (시그니처 스텁)

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `RunCommand` 시그니처 스텁(생성자 포트 주입, execute, judge, record)을 만들고 컴파일된다
- [ ] #2 AO-7 #1~5(실행 불가 시 exit 3, 순서와 finally retire, 브랜치 상태 판정, actual/attempt 분기와 워크트리 정리, runs.jsonl 한 줄)를 모든 포트를 가짜로 주입한 pending 테스트로 쓴다
- [ ] #3 미병합 `task/<id>` 브랜치가 있으면 스폰하지 않고, 없으면 main HEAD 기준으로 워크트리를 만들며 main 카드를 고치지 않는지 검증하는 테스트를 포함한다(decision-14)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test`가 통과한다(pending 제외)
- [ ] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
- [ ] #3 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(병합 방식 무관, 병합 뒤 브랜치 삭제 — decision-14)
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
2026-10-29(AO-29): AO-6(CommitLedger)은 최상위 Q-18 의존으로 보류해 의존에서 뺐다. 실제치 테스트는 session만 다룬다(decision-5 · 8 개정).
<!-- SECTION:NOTES:END -->
