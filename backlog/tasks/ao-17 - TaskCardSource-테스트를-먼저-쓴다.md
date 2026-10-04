---
id: AO-17
title: TaskCardSource 테스트를 먼저 쓴다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:38'
updated_date: '2026-10-04 02:04'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-10
references:
  - decision-6
  - decision-9
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/card/TaskCardSourceTest.java
  - src/main/java/com/coralstay/orchestrator/card/TaskCardSource.java
priority: high
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
draft-1 (TaskCardSource)을 구현하기 전에 테스트부터 쓴다. 공개 API 시그니처 스텁을 만들어 컴파일되게 하고, 근거 카드의 AC를 pending 테스트로 옮긴다. 구현 카드가 pending 태그를 떼고 초록으로 만드는 것이 그 카드의 완료 조건이 된다.

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/card/TaskCardSourceTest.java`
- `src/main/java/com/coralstay/orchestrator/card/TaskCardSource.java` (시그니처 스텁)

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `TaskCardSource` 포트와 값 타입(Card, TokenBudget, Tokens, Attempt, CardCheck) 시그니처를 만들고 컴파일된다
- [x] #2 draft-1 #1~3(read 반환 필드, validate 사유, claim/attempt/actual 상태 전이)과 Tokens `in=N out=M` 파싱·출력을 pending 테스트로 쓴다
- [x] #3 메모리 구현은 테스트 안의 가짜로 두고, 포트 계약만 검증한다
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
Tokens 파싱 규칙은 CommitLedger(AO-6 AC5)와 같아야 한다 — 같은 테스트 벡터를 쓴다.

API(TaskCardSource.java, 시그니처 스텁): read(id) → Card, default validate(card) → CardCheck, claim(id, agent), recordAttempt(id, Attempt), recordActual(id, Actual). 값 타입: Card(id, title, status, assignees, dependencies, acceptanceCriteria, targetFiles, Optional<TokenBudget>) + claimedBy/withTokenBudget, TokenBudget(Optional<Tokens> estimated, List<Attempt> attempts, Optional<Actual> actual, List<String> unparsed) + parse(sectionBody)/render()/withAttempt/withActual, Tokens(in, out) + parse/toString, Attempt(commits, session, model, reason), Actual(commits, session, attempts), CardCheck(ok, reasons).
설계: 파싱·출력·상태 전이는 값 타입에 두고 구현체(메모리 가짜, BacklogCardSource)는 저장·커밋만 한다 — 그래서 가짜가 있어도 테스트는 DRAFT-1 구현 전까지 빨강이다. actual 줄(commits·session·attempts k)을 담을 값 타입이 draft-1 목록에 없어 Actual을 추가했다.
Tokens 벡터는 AO-6 AC5(CommitLedger in/out 파싱)와 같아야 한다: 유효 `in=59858 out=465`, `in=0 out=0`, `in=3000000000 out=1`(long); 무효 "", `in=59858`, `out=465`, `out=465 in=59858`, `in=abc out=465`, `in=-1 out=465`, `in=59,858 out=465`, `in=59858 out=465 extra`, `IN=59858 OUT=465`, `unavailable (no-session-id)`. 한쪽으로 모을 때 이 벡터를 그대로 옮긴다.
검증: ./gradlew test BUILD SUCCESSFUL(61 tests, 실패 0, pending 제외). ./gradlew pendingTest: 22 tests completed, 22 failed — 전부 TaskCardSourceTest, 원인은 모두 `pending: DRAFT-1` UnsupportedOperationException(8 직접, 4 assertDoesNotThrow, 10 assertThrows 타입 불일치). 목록: DRAFT-1#1 ×3(read 필드·estimated / attempt·actual 파싱 / 형식 오류 unparsed), DRAFT-1#2 ×4(ok / 소절 없음 / estimated 없음 / 형식 오류 줄), DRAFT-1#3 ×3(claim / recordAttempt 순번 / recordActual), DRAFT-1 보조 Tokens ×12(파싱 1, 출력·왕복 1, 무효 10).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
TaskCardSource 포트와 값 타입 시그니처 스텁(pending: DRAFT-1)과 TaskCardSourceTest pending 계약 테스트 22개(DRAFT-1#1~3, Tokens in=N out=M 벡터)를 추가했다. 메모리 가짜는 저장만 하고 파싱·출력·전이는 값 타입이 맡아 DRAFT-1 구현 시 초록이 된다. Actual 값 타입을 추가했다. ./gradlew test 통과, pendingTest 22/22 실패(모두 pending 스텁).
<!-- SECTION:FINAL_SUMMARY:END -->
