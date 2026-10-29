---
id: decision-14
title: 'decision-9 대체: 착수는 에이전트 세션이 task 브랜치에서 하고 오케스트레이터는 claim 커밋을 하지 않는다'
date: '2026-10-29 02:36'
status: accepted
---
## Context

decision-9는 실행 직전 오케스트레이터가 main의 카드를 `In Progress` + `@claude`로 커밋(claim)하고 그 커밋에서 워크트리를 갈라, 사람 락(`In Progress` + 사람 담당)과 에이전트 실행분을 구분했다.
유저 결정(2026-10-29, 최상위 coralstay-software-factory TASK-19 · 최상위 decision-3)이 착수 시점을 바꿨다.

- 착수는 미병합 `task/<ID>` 브랜치의 존재다. `In Progress` 전환은 그 브랜치 위의 기록이다.
- 착수 검사(계획 승인 게이트 · 계획/AC/DoD 형식 · 작업 순서 최신성)는 claude-code-agile-hooks가 두 시점에 한다: `task/<ID>` 브랜치 생성 · 전환(사람 · Claude 세션의 명령)과 `backlog task edit <ID> -s "In Progress"`. 오케스트레이터는 `task/<ID>`를 자기 자바 프로세스(`WorktreeManager`의 `git worktree add`)로 만들어 첫 시점이 훅을 거치지 않으므로, 오케스트레이터 실행분의 검사 지점은 에이전트 세션의 `In Progress` 전환이다(유저 결정 2026-10-29 보완).
- 오케스트레이터는 훅을 모른다 — 훅 없이도 쓸 수 있어야 한다.
- 사람이 착수해도 `task/<ID>` 브랜치가 생기므로, 브랜치 존재가 사람 · 에이전트 착수를 함께 가린다.

claim 커밋은 main에 오케스트레이터 커밋을 남겨 squash · rebase 병합을 막았고(decision-9 결과), 세션 밖에서 일어나 훅의 게이트를 거치지 않았다.

## Decision

1. 오케스트레이터는 main의 카드를 고치지 않는다. 워크트리와 `task/<ID>` 브랜치는 main HEAD에서 만든다(decision-3).
2. 카드를 `In Progress`로 바꾸는 것은 에이전트 세션이 `task/<ID>` 브랜치에서 한다. 그 뒤의 카드 변경(AC 체크 · Done · Token Budget 실제치 · 시도 기록)도 태스크 브랜치에서만 한다.
3. 배정 조건(ORC-005)은 `isReady` ∧ `status = To Do` ∧ 미병합 `task/<ID>` 브랜치 없음이다. 브랜치는 로컬 · 원격을 모두 보고, 병합 여부는 `git cherry`로 판정한다. `backlog/work-order.md`는 읽지 않는다.
4. 세션이 `In Progress` 전환에 실패하면(훅의 착수 검사가 막은 경우 포함 — 오케스트레이터 실행분에서는 이 전환이 훅의 유일한 착수 검사 지점이다) '착수 안 됨' 일반 실패로 회수한다. 오케스트레이터는 실패 사유를 훅 규칙으로 해석하지 않는다.
5. 같은 카드의 중복 실행은 오케스트레이터 자체 잠금(`SessionRegistry.admit`의 `putIfAbsent`)으로 막는다.
6. 사람 락(`In Progress` + 사람 담당)과 에이전트 실행분(`@claude`)의 구분은 두지 않는다 — 둘 다 브랜치 존재로 배정에서 빠진다.

## Alternatives

- decision-9 유지(main claim 커밋): 세션 밖 착수라 훅의 승인 · 작업 순서 게이트를 거치지 않고(세션의 `In Progress` 전환이 일어나지 않는다), main에 오케스트레이터 커밋이 생겨 merge commit 병합을 강제한다.
- 오케스트레이터가 승인 · 작업 순서를 직접 판정: 훅 규칙을 이중으로 구현하고 오케스트레이터가 훅에 묶인다.
- 훅이 브랜치 생성 · 전환 때만 검사: 오케스트레이터의 `git worktree add`는 훅을 거치지 않아 에이전트 실행분이 승인 게이트를 건너뛴다.

## Consequences

- 오케스트레이터가 직접 하는 커밋은 실제치 기록(`recordAttempt` · `recordActual`)뿐이다(decision-5).
- PR 병합 방식 제약(merge commit만)이 사라진다.
- 알려진 한계: backlog.md 1.51.0은 main에서 브랜치를 ID 할당에만 훑으므로, 착수한 카드도 main 보드에는 `To Do`로 보인다.
- 실패한 실행은 브랜치에 attempt 기록과 함께 남는다. 브랜치가 남아 있는 한 그 카드는 다시 배정되지 않고 사람이 판단한다.
- 코드(`claim` · 사람 락 판정 · `isRunnable`의 브랜치 검사)는 AO-30에서 맞춘다.
