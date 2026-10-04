---
id: AO-21
title: E2E 스모크 확인 항목 골격을 먼저 쓴다
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
  - AO-20
references:
  - decision-6
  - decision-9
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - scripts/smoke.sh
priority: medium
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
AO-8의 E2E 확인 항목 (a)~(f)를 `scripts/smoke.sh`의 함수로 먼저 만든다. 구현 전에는 각 항목이 `FAIL not implemented`를 출력한다.

## 대상 파일
- `scripts/smoke.sh`

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 (a) claim (b) 워크트리·브랜치 (c) 잔존 claude 프로세스 없음 (d) AC 커밋 트레일러 (e) 브랜치 카드 Done + Token Budget actual (f) runs.jsonl 일치를 각각 함수로 만든다
- [ ] #2 각 함수는 지금은 `FAIL <항목> not implemented`를 출력하고, 전체 결과 요약과 종료코드(실패 있으면 1)를 낸다
- [ ] #3 실제 claude를 실행하지 않는다(실행 부분은 AO-8에서 채움)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test`가 통과한다(pending 제외)
- [ ] #2 `./gradlew pendingTest`의 실패 목록이 이 카드에서 기대한 테스트와 일치한다
- [ ] #3 `task/AO-<n>` 브랜치를 푸시하고 PR을 열었다(merge commit으로 병합)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 함수 골격과 요약 출력.
2. shellcheck 통과.
3. `./gradlew test`와 무관(게이트 영향 없음).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
AO-8 #2의 확인 항목과 이름을 맞춘다.
<!-- SECTION:NOTES:END -->
