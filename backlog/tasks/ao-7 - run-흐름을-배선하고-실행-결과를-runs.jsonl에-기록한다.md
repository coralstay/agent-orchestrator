---
id: AO-7
title: run 흐름을 배선하고 실제 토큰을 카드와 runs.jsonl에 기록한다
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 13:10'
labels:
  - skeleton
  - cli
  - docs-sync
milestone: m-0
dependencies:
  - AO-2
  - AO-3
  - AO-4
  - AO-5
  - AO-6
references:
  - decision-2
  - decision-3
  - decision-4
  - decision-6
  - decision-7
  - decision-8
  - decision-9
  - decision-10
documentation:
  - ../coralstay-software-factory/아키텍처.md
  - ../coralstay-software-factory/README.md
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
  - backlog/docs/doc-2 - 팩토리-문서-반영-구조.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/RunCommand.java
  - src/main/java/com/coralstay/orchestrator/Main.java
  - src/main/java/com/coralstay/orchestrator/workspace/WorktreeManager.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
`orchestrator run <taskId>` 한 번으로 카드의 예상 토큰에 맞는 모델을 골라 Claude Code를 헤드리스로 실행하고, 실제 토큰(커밋 귀속분과 세션 총량)을 카드에 병기한다. 결과는 브랜치 상태로 판정한다.

## 실행 순서 (decision-6~10)
1. 카드 읽기·Token Budget 검증(main 작업 트리) — 실패하면 실행 안 함, 사유 출력.
2. 실행 가능 판정 — To Do, 의존 Done, 같은 대상 파일을 가진 다른 카드에 사람 담당 In Progress 없음.
3. ModelPolicy로 모델 선택.
4. claim: main 카드 In Progress + `@claude` 커밋.
5. claim 커밋 기준으로 워크트리 생성(`task/<id>`).
6. admit → AgentRunner(Claude) 실행. 프롬프트: 승인·승격된 태스크, claude-rails 절차대로 AC마다 커밋, 끝나면 Done, push 금지.
7. await → retire(finally).
8. 실제치 수집: CommitLedger(`main..task/<id>` 트레일러 in/out) + 실행 결과의 세션 총량.
9. 성공 판정: 브랜치 카드 Done · AC 전부 체크 · 커밋 수 ≥ AC 수. 종료코드만으로 판정하지 않는다.
10. 성공이면 워크트리 카드에 actual 기록 후 워크트리 제거, 실패면 attempt 기록 후 워크트리 보존.
11. `.orchestrator/runs.jsonl`에 한 줄(예상·실제 두 값·모델·종료코드·판정·커밋 수 대 AC 수).

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/RunCommand.java`
- `src/main/java/com/coralstay/orchestrator/Main.java` (RunCommand 연결)
- `src/test/java/com/coralstay/orchestrator/RunCommandTest.java` (모든 포트를 가짜로 주입)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `execute(taskId)`: 카드 검증 실패·실행 불가(상태·의존·파일 락)면 스폰하지 않고 사유와 함께 exit 3으로 끝난다
- [ ] #2 `execute(taskId)`: 정책 선택 → claim → claim 커밋 기준 워크트리 → admit → 실행 → retire 순서로 진행하고, 예외가 나도 retire는 반드시 실행된다
- [ ] #3 `judge(branch)`: 브랜치의 카드 상태·AC 체크·커밋 수로 성공 여부를 판정한다(종료코드 0만으로 성공 처리하지 않음)
- [ ] #4 성공이면 actual, 실패면 attempt를 카드 Token Budget에 기록하고(커밋 귀속분과 세션 총량 병기), 성공 시에만 워크트리를 제거한다
- [ ] #5 `record(result)`: 예상·실제(두 값)·모델·종료코드·판정·커밋 수·AC 수를 `.orchestrator/runs.jsonl`에 한 줄로 추가한다
- [ ] #6 팩토리 문서 D2·D6·D8·D11·D13 반영: 감시 방식 미정, 실행 흐름(claim·브랜치 상태 판정·실제치 병기), README 상단 문구를 정정했다
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
1. RunCommand 생성자에 포트 주입: TaskCardSource, ModelPolicy, WorktreeManager, SessionRegistry, AgentRunner, CommitLedger.
2. 판정 단계(검증·상태·락) 테스트 → 구현.
3. claim → 워크트리 기준 커밋 전달 → 실행 → finally retire 순서 테스트.
4. judge와 actual/attempt 기록 분기 테스트.
5. runs.jsonl 직렬화(의존성 없이).
6. Main 연결, 가짜 claude로 스모크.
7. 팩토리 문서 반영.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
WorktreeManager.create는 지금 HEAD 기준이다 — claim 커밋을 기준점으로 받도록 인자를 추가해야 한다(AO-3 코드 수정, 이 태스크 범위에 포함).
의존: 승격 후 TaskCardSource·BacklogCardSource·AgentRunner·ModelPolicy 태스크 ID를 --dep에 추가한다(promote는 draft 의존을 갱신하지 않음).
<!-- SECTION:NOTES:END -->
