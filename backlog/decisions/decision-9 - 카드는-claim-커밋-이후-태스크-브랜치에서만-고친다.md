---
id: decision-9
title: 카드는 claim 커밋 이후 태스크 브랜치에서만 고친다
date: '2026-10-03 13:03'
status: accepted
---
## Context

카드 파일은 main 작업 트리와 태스크 워크트리 양쪽에 있다. 양쪽에서 같은 카드를 고치면 병합 충돌이 나고, 에이전트가 브랜치에서만 상태를 바꾸면 main 보드에서는 To Do로 보여 사람이 같은 일을 시작할 수 있다.
또 In Progress를 사람 락으로 보는 규칙 때문에, 에이전트가 잡았다가 죽은 카드를 사람 락과 구분하지 못한다.

## Decision

1. 실행 직전 오케스트레이터가 main의 카드를 **In Progress + 담당자 `@claude`**로 바꿔 커밋한다(claim).
2. 워크트리는 그 claim 커밋을 기준으로 만든다.
3. 그 뒤 카드 변경(AC 체크, Done, Token Budget 실제치·시도 기록)은 **태스크 브랜치에서만** 한다. 병합하면 main에 반영된다.
4. 실행 가능 판정: In Progress + 사람 담당 = 락(같은 대상 파일을 가진 다른 카드도 포함, 파일 단위). In Progress + `@claude` = 에이전트 실행분(비정상 종료면 사람에게 보고).

## Alternatives

- 카드 변경을 전부 태스크 브랜치에서만: main 보드에서 에이전트 작업이 안 보인다.
- 카드 변경을 전부 main에서: 브랜치와 main이 같은 카드를 따로 고쳐 충돌.

## Consequences

- claim 이후 main은 카드를 고치지 않으므로 병합 충돌이 없다.
- 실패한 실행은 브랜치에 attempt 기록과 함께 남고, main 카드는 In Progress + `@claude`로 남아 사람이 판단한다.
