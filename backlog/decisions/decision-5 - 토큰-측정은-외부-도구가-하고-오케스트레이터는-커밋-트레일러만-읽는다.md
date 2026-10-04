---
id: decision-5
title: 토큰 측정은 외부 도구가 하고 오케스트레이터는 커밋 트레일러만 읽는다
date: '2026-10-03 03:15'
status: accepted
---
## Context

오케스트레이터는 태스크마다 실제로 쓴 토큰을 알아야 한다. 토큰 수는 모델이 아니라 API 응답 메타데이터라서 측정은 세션 트랜스크립트를 읽을 수 있는 도구가 해야 한다.
현재 그 도구는 git-trail(`~/githubs/git-trail`)이다. post-commit 훅이 커밋마다 `Tokens-Used`/`Tool-Calls`/`AI-Model`/`Task-Id` 트레일러를 남긴다.
2026-10-03 스파이크에서 헤드리스 `claude -p` 세션의 커밋에도 실측값(`Tokens-Used: in=59858 out=465`)이 붙고, 같은 세션 트랜스크립트를 커밋 시점까지 합산한 값과 일치함을 확인했다.

## Decision

- 오케스트레이터는 측정 코드를 두지 않는다. 특정 측정 도구의 코드나 설치 방식에 의존하지 않고 **커밋 트레일러 계약**만 안다.
- 태스크 브랜치 커밋의 트레일러를 `git log`로 읽어 합산한다(`ledger/CommitLedger`).
- 트레일러가 `unavailable (사유)`이면 재측정·추정하지 않고 사유와 개수를 그대로 기록한다.
- 세션 총량은 decision-8에 따라 함께 기록한다.

## Alternatives

- 오케스트레이터가 트랜스크립트를 직접 합산: 측정 도구의 일을 중복한다.
- 헤드리스 JSON usage만 사용: 커밋 단위 귀속(커밋 = AC)이 사라진다.

## Consequences

- 에이전트는 `--bare` 없이 띄운다 — bare 모드는 훅을 건너뛰어 트레일러가 붙지 않는다.
- 대상 저장소에 트레일러를 남기는 도구(현재 git-trail)가 설치돼 있어야 한다.
- 오케스트레이터가 직접 하는 커밋(claim·실제치 기록)은 `AI_AGENT`·`CLAUDE_CODE_SESSION_ID`를 지우고 실행한다. 그렇지 않으면 부모 Claude 세션의 커밋으로 표시되고 `unavailable (transcript-not-found)`가 붙는다(스파이크로 확인).
