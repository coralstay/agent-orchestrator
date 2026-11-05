---
id: decision-6
title: 카드의 예상·실제 토큰은 Description의 Token Budget 섹션에 둔다
date: '2026-10-03 13:03'
status: accepted
---
> **2026-10-29 단서(AO-29).** 아래 형식의 `commits …` 항목은 최상위 Q-18 이후로 보류한다(decision-8 단서).
> 그때까지 `attempt n` · `actual` 줄에는 `session`만 적는다.

## Context

오케스트레이터는 카드에서 예상 토큰을 읽고, 끝나면 실제 토큰을 카드에 기록해야 한다. 카드는 오케스트레이터가 아니라 사용자의 LLM 도구가 만든다.
backlog.md 1.51.0에는 숫자 필드(estimate·points 등)가 없고, 직접 넣은 frontmatter 키는 다음 `task edit`에서 지워진다(실측, upstream issue #918 미해결).
라벨은 편집해도 남지만 제작자 의도상 분류·필터용(issue #229)이라 값을 담는 데 쓰지 않는다. 본문은 편집해도 보존된다.

## Decision

카드 Description 안에 `### Token Budget` 소절을 두고, 상태에 따라 아래처럼 채운다.

```
### Token Budget

estimated: in=200000 out=8000
attempt 1: commits in=… out=… · session in=… out=… · model=claude-sonnet-… · timeout
actual: commits in=… out=… · session in=… out=… · attempts 2
```

- Draft·To Do: `estimated`만. 카드를 만드는 도구(사람의 LLM 도구)가 쓴다.
- 실행이 Done에 이르지 못하면 오케스트레이터가 `attempt n` 줄을 추가하고 카드를 다시 실행 가능 상태로 둔다.
- Done이면 오케스트레이터가 `actual` 줄(모든 시도의 합계)을 추가한다.
- 오케스트레이터는 `backlog task edit -d`로 Description 전체를 다시 쓰되 `### Token Budget` 소절만 바꾼다.
- `estimated`가 없거나 형식이 틀린 카드는 실행하지 않고 사유를 남긴다.

## Alternatives

- 라벨(`est-tokens:N`): 편집에 살아남고 `--json`에 나오지만 라벨의 분류 용도를 해친다.
- frontmatter 필드: #918이 해결되기 전에는 편집 때 지워진다.
- `--append-notes`: 예상치와 실제치가 다른 섹션으로 갈라진다.

## Consequences

- 카드 작성 지침에 Token Budget 형식과 단위(decision-8)를 넣어야 한다 — 지금은 사용자의 Claude(claude-rails)가 강제하고, 오케스트레이터가 입구에서 한 번 더 검사한다.
- 다른 도구로 카드를 만들 때의 강제(AGENTS.md, git 훅 스키마 검사)는 M2.
- #918이 해결되면 frontmatter로 옮길 수 있다. 카드 접근은 `TaskCardSource` 포트 뒤에 두어 오케스트레이터 코어는 바뀌지 않게 한다.
