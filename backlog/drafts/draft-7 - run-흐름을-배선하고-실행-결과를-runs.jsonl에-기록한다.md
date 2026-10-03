---
id: DRAFT-7
title: run 흐름을 배선하고 실행 결과를 runs.jsonl에 기록한다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:18'
labels:
  - skeleton
  - cli
  - docs-sync
milestone: m-0
dependencies:
  - DRAFT-2
  - DRAFT-3
  - DRAFT-4
  - DRAFT-5
  - DRAFT-6
references:
  - decision-2
  - decision-3
  - decision-4
documentation:
  - ../coralstay-software-factory/아키텍처.md
  - ../coralstay-software-factory/README.md
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
  - backlog/docs/doc-2 - 팩토리-문서-반영-구조.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/RunCommand.java
  - src/main/java/com/coralstay/orchestrator/Main.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
`orchestrator run <taskId>`가 BacklogClient → WorktreeManager → SessionRegistry.admit → AgentSpawner → retire →
CommitLedger 순으로 실행되고, 한 줄 JSON 결과를 `.orchestrator/runs.jsonl`에 추가하는 명령.

## 맥락
RFC 로드맵 1번 walking skeleton의 완성 지점. 기록 항목 중 **커밋 수 vs AC 수**는 아키텍처.md 5절 "자가 치유 재시도
배수" 미결 질문의 첫 표본이다. Implementer 실패 시 재스폰하지 않고 worktree를 남겨 사람이 판단한다(RFC 07절).
문서 측면에서는 아키텍처.md의 WatchService 확정 서술(D2), "Dev 모드가 스스로 done"(D6), README "코드 한 줄도 없음"(D8)을 정정한다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/RunCommand.java`
- `src/main/java/com/coralstay/orchestrator/Main.java` (RunCommand 호출 연결)
- `src/test/java/com/coralstay/orchestrator/RunCommandTest.java` (각 컴포넌트를 가짜로 주입)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `execute(taskId)`: isRunnable이 false면 스폰하지 않고 사유와 함께 exit 3으로 끝난다
- [ ] #2 `execute(taskId)`: worktree 생성 → admit → spawn → retire → ledger 판독 순서로 실행하고, 예외가 나도 retire는 반드시 실행된다
- [ ] #3 `record(result)`: task, 종료코드, timedOut, duration, 커밋 수, AC 수, 토큰·도구 호출 합계, unavailable 개수를 한 줄 JSON으로 `.orchestrator/runs.jsonl`에 추가한다
- [ ] #4 `cleanup(result)`: 성공(종료코드 0)이면 worktree를 제거하고, 실패·타임아웃이면 남겨 경로를 출력한다
- [ ] #5 팩토리 문서 D2·D6·D8 반영: 아키텍처.md 1·4절(감시 방식 미정, Implementer 단독 범위)과 README 상단 경고문을 정정했다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-logbook 형식 검사를 통과했다
- [ ] #3 팩토리 `docs/orchestrator-skeleton` 브랜치에 문서 반영 커밋이 있다
- [ ] #4 doc-1 해당 행을 ✅(팩토리 커밋 해시) 또는 ❌로 갱신했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. RunCommand 생성자에 5개 컴포넌트 주입(인터페이스 없이 구체 클래스 + 테스트용 서브클래스/람다 실행기).
2. Implementer 프롬프트: 태스크 ID와 "backlog task view로 읽고 claude-rails 절차대로 AC마다 커밋" 지시 — 포인터만 싣는다(RFC 11절①).
3. 순서·finally retire 테스트, isRunnable false 테스트.
4. JSON 직렬화는 의존성 없이 직접(값이 단순).
5. Main 연결 후 `./gradlew run --args="run AO-x"`로 스모크(가짜 claude).
6. 팩토리 D2·D6·D8 반영.

의존(계획 키 기준): T2, T3, T4, T5, T6 — dependencies 필드에 draft ID로 설정됨. promote 후 AO ID로 바뀌었는지 확인하고 아니면 `--dep`로 재설정한다.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
토큰 필드명은 tokensUsedApprox처럼 근사임을 드러낸다(decision-5). JVM shutdown hook에서 Registry.live()의 남은 세션을 retire.
<!-- SECTION:NOTES:END -->
