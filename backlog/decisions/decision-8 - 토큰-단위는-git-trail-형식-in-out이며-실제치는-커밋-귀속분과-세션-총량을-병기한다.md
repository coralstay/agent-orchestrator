---
id: decision-8
title: 토큰 단위는 git-trail 형식 in/out이며 실제치는 커밋 귀속분과 세션 총량을 병기한다
date: '2026-10-03 13:03'
status: accepted
---
> **2026-10-29 단서(AO-29, 최상위 TASK-19).** `commits`는 최상위 Q-18(토큰 계량 컴포넌트) 이후로 보류한다 —
> coding-agent-git-commit-tool이 더 이상 `Tokens-Used`를 남기지 않는다(최상위 ADR-021). 그때까지 실제치는 `session`만 적는다.
> 헤드리스 JSON 파싱(`ClaudeCodeRunner`)은 최상위에 ADR-021의 예외로 기록돼 있다.

## Context

예상치와 실제치를 비교하려면 같은 단위여야 한다. git-trail은 `Tokens-Used`를 `in=<입력> out=<출력>`으로 남기고, 입력에는 캐시 쓰기·읽기가 포함된다(입력이 전체의 대부분).
스파이크에서 트레일러 합계는 마지막 커밋 뒤의 턴을 포함하지 않아 세션 전체보다 입력이 약 26% 적었다(80,943 중 21,085 누락). 헤드리스 JSON(`--output-format json`)의 `usage`는 세션 전체를 준다.

## Decision

- 단위는 `in=N out=M`(입력은 캐시 포함). 예상치도 같은 형식으로 적는다.
- 실제치는 두 값을 병기한다.
  - `commits`: 태스크 브랜치 커밋 트레일러 합계(git log 기준, 커밋에 귀속된 사용량) — Q-18 이후, 그때까지 생략
  - `session`: 헤드리스 JSON `usage` 합계(input + cache_creation + cache_read / output)
- 정책 판단과 보정은 `session` 기준, 두 값의 차이는 "커밋 밖 사용량"으로 본다.
- 헤드리스 JSON 파싱은 `ClaudeCodeRunner` 안에만 둔다(decision-7의 포트 뒤).

## Alternatives

- 트레일러 합계만: git log 원칙엔 맞지만 과소 집계.
- 세션 총량만: 실제치가 git log를 거치지 않는다.

## Consequences

- 카드 작성자는 캐시 포함 입력을 예상해야 한다. 처음엔 감이 없으므로 실측이 쌓여야 정책 구간이 의미를 갖는다.
- `CommitLedger`는 `in/out` 형식을 파싱해야 한다(AO-6 AC5). AO-6은 Q-18 의존으로 보류다.
