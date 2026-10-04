---
id: AO-18
title: BacklogCardSource 테스트를 먼저 쓴다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-04 01:38'
updated_date: '2026-10-04 06:17'
labels:
  - test-first
  - skeleton
milestone: m-0
dependencies:
  - AO-17
references:
  - decision-6
  - decision-9
  - decision-5
  - decision-11
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/test/java/com/coralstay/orchestrator/card/BacklogCardSourceTest.java
  - src/main/java/com/coralstay/orchestrator/card/BacklogCardSource.java
priority: high
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
draft-2 (BacklogCardSource)을 구현하기 전에 테스트부터 쓴다. 공개 API 시그니처 스텁을 만들어 컴파일되게 하고, 근거 카드의 AC를 pending 테스트로 옮긴다. 구현 카드가 pending 태그를 떼고 초록으로 만드는 것이 그 카드의 완료 조건이 된다.

## 대상 파일
- `src/test/java/com/coralstay/orchestrator/card/BacklogCardSourceTest.java`
- `src/main/java/com/coralstay/orchestrator/card/BacklogCardSource.java` (시그니처 스텁)

## 공통 규칙
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 카드·AC 번호를 붙인다(draft는 `DRAFT-n#k`, 승격 때 교체).
- 아직 구현이 없는 테스트는 `@Tag("pending")` — 기본 `./gradlew test`에서 빠지고 `./gradlew pendingTest`로 본다.
- 아직 없는 클래스는 공개 API 시그니처만 두고 본문은 `throw new UnsupportedOperationException("pending: <카드>")`.
- 문서 AC(팩토리 반영)는 테스트 대상이 아니다.
- 헬퍼는 기존 패턴을 쓴다: 실행기 주입 람다, `@TempDir` 실제 git 저장소(빈 `core.hooksPath`), 가짜 실행 파일 스크립트, 실제 CLI 출력 픽스처.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `BacklogCardSource` 시그니처 스텁을 만들고 컴파일된다
- [x] #2 draft-2 #1~4(Token Budget 파싱, claim 명령과 환경변수 제거, Description 소절만 교체하는 `-d` 기록, 파일 단위 사람 락 판정)를 pending 테스트로 쓴다
- [x] #3 소절 교체가 나머지 Description을 바꾸지 않는지, claim 커밋 환경에 AI_AGENT·CLAUDE_CODE_SESSION_ID가 없는지 검증하는 테스트를 포함한다
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
실행기 주입으로 명령 인자와 환경을 캡처한다. 실제 backlog 출력 픽스처에 Token Budget 소절이 있는 카드를 추가.

결과(2026-10-04):
- API: BacklogCardSource(CommandRunner runner, Path repoRoot, Function<String,Path> worktreeOf, Map<String,String> parentEnv). CommandRunner.run(argv, workingDir, env)의 env는 자식 프로세스 전체 환경(대체). STRIPPED_ENV = AI_AGENT, CLAUDE_CODE_SESSION_ID. processRunner() 운영 실행기 스텁. isLocked(card, all) → LockVerdict(LockKind FREE|HUMAN_LOCK|AGENT_OWNED, cardIds, files), HUMAN_LOCK 우선, 담당자 없는 In Progress는 사람 락.
- 픽스처: src/test/resources/card/ao-90-plain.txt(estimated/attempt/actual), ao-91-plain.txt(소절 앞뒤 텍스트 보존 검증용).
- ./gradlew test: BUILD SUCCESSFUL(pending 제외).
- ./gradlew pendingTest: 76 tests completed, 76 failed = 기존 59 + BacklogCardSourceTest 17(#1 3, #2 3, #3 4, #4 7). 17개 모두 UnsupportedOperationException: pending: DRAFT-2.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
BacklogCardSource 시그니처 스텁(CommandRunner 주입: argv·작업 디렉토리·전체 환경, repoRoot, 워크트리 경로 함수, 부모 환경; isLocked → LockVerdict)과 DRAFT-2 #1~4 pending 테스트 17개를 추가했다. claim/record 커밋 환경에서 AI_AGENT·CLAUDE_CODE_SESSION_ID 제거, -d 소절 교체 시 앞뒤 텍스트 바이트 보존, 워크트리 기준 실행, 파일 단위 사람 락을 검증한다. test 초록, pendingTest 76 실패(기존 59 + 신규 17).
<!-- SECTION:FINAL_SUMMARY:END -->
