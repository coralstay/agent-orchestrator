---
id: decision-5
title: 토큰 측정은 외부 도구가 하고 오케스트레이터는 커밋 트레일러만 읽는다
date: '2026-10-03 03:15'
status: accepted
---
> **2026-10-29 개정(AO-29, 최상위 TASK-19).** coding-agent-git-commit-tool은 더 이상 `Tokens-Used`를 남기지 않는다(최상위 ADR-021).
> 트레일러 합산(`CommitLedger`)은 토큰 계량 컴포넌트를 정하는 최상위 Q-18 이후로 보류하고, 그때까지 실제치는 `session`만 기록한다(decision-8).
> 오케스트레이터가 직접 하는 커밋은 실제치 기록뿐이다(claim 커밋은 decision-14로 폐기).

## Context

오케스트레이터는 태스크마다 실제로 쓴 토큰을 알아야 한다. 토큰 수는 모델이 아니라 API 응답 메타데이터라서 측정은 세션 트랜스크립트를 읽을 수 있는 도구가 해야 한다.
현재 그 도구는 git-trail(`~/githubs/git-trail`)이다. post-commit 훅이 커밋마다 `Tokens-Used`/`Tool-Calls`/`AI-Model`/`Task-Id` 트레일러를 남긴다.
2026-10-03 스파이크에서 헤드리스 `claude -p` 세션의 커밋에도 실측값(`Tokens-Used: in=59858 out=465`)이 붙고, 같은 세션 트랜스크립트를 커밋 시점까지 합산한 값과 일치함을 확인했다.

## Decision

- 오케스트레이터는 측정 코드를 두지 않는다. 특정 측정 도구의 코드나 설치 방식에 의존하지 않고 **커밋 트레일러 계약**만 안다.
- 태스크 브랜치 커밋의 트레일러를 `git log`로 읽어 합산한다(`ledger/CommitLedger`). **보류**: 트레일러를 남기는 도구가 없어져, 합산은 최상위 Q-18(토큰 계량 컴포넌트)이 정해진 뒤에 다시 정한다(AO-6 보류).
- 트레일러가 `unavailable (사유)`이면 재측정·추정하지 않고 사유와 개수를 그대로 기록한다.
- 세션 총량은 decision-8에 따라 기록한다. Q-18 전까지는 이것만 기록한다.

## Alternatives

- 오케스트레이터가 트랜스크립트를 직접 합산: 측정 도구의 일을 중복한다.
- 헤드리스 JSON usage만 사용: 커밋 단위 귀속(커밋 = AC)이 사라진다.

## Consequences

- 에이전트는 `--bare` 없이 띄운다 — bare 모드는 훅을 건너뛰어 트레일러가 붙지 않는다.
- 대상 저장소에 트레일러를 남기는 도구(현재 git-trail)가 설치돼 있어야 한다.
- 오케스트레이터가 직접 하는 커밋은 실제치 기록(`recordAttempt`·`recordActual`)뿐이고, `AI_AGENT`·`CLAUDE_CODE_SESSION_ID`·`CLAUDE_PID`를 지우고 실행한다. 그렇지 않으면 부모 Claude 세션의 커밋으로 표시된다(스파이크로 확인한 `unavailable (transcript-not-found)`는 트레일러가 있던 때의 증상). coding-agent-git-commit-tool GF-130이 `CLAUDE_CODE_SESSION_ID`·`CLAUDE_PID`로 에이전트를 판별할 예정이라 `CLAUDE_PID`를 더했다. 지금은 사람 커밋처럼 보이고, '프로그램 커밋' 구분은 GF-130 때 정한다.
