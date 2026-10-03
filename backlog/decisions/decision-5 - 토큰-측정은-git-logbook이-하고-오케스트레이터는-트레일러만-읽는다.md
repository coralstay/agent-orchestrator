---
id: decision-5
title: 토큰 측정은 git-logbook이 하고 오케스트레이터는 트레일러만 읽는다
date: '2026-10-03 03:15'
status: accepted
---
## Context

RFC 06절은 토큰 측정을 "git-format post-commit의 `measure_claude_code_token_usage()` 방식 재사용"으로 서술한다.
그 도구는 2026-10-03 decision-25(git-format 저장소)로 **git-logbook**으로 개명됐고, 목적이 형식 · 출처 기록 · 이력 불변의
세 기둥으로 재정의됐다. 토큰은 그 출처 기록 기둥이 커밋 트레일러(`Tokens-Used`/`Tool-Calls`/`AI-Model`/`Task-Id`)로 남긴다.
계획 초안에 있던 "`claude -p --output-format json` usage와 트랜스크립트를 오케스트레이터가 직접 재서 비교" 안은 유저가 기각했다.

## Decision

**토큰 측정은 git-logbook이 한다.** 오케스트레이터는 측정 코드를 두지 않고, 태스크 브랜치의 커밋 트레일러를
`git log`로 읽어 합산만 한다(`CommitLedger`). 트레일러가 `unavailable (사유)`이면 재측정·추정하지 않고 그 사유와 개수를 그대로 기록한다.

## Alternatives

- 오케스트레이터가 `claude -p` JSON의 `usage`를 직접 집계: 측정 주체가 둘이 되어 커밋 단위 귀속(커밋 = 함수)과 어긋난다.
- 트랜스크립트 jsonl 직접 합산: git-logbook이 이미 하는 일의 중복.

## Consequences

- Spawner는 `--bare`를 쓰지 않는다 — 훅이 꺼지면 git-logbook·claude-rails 트레일러/게이트도 꺼진다.
- 대상 저장소(픽스처 포함)에 git-logbook이 설치돼 있어야 한다.
- 헤드리스 `claude -p` 세션에서 트레일러가 실측값으로 붙는지는 E2E에서 확인할 가설로 남긴다(doc-1 D5).
- 팩토리 문서의 "git-format" 명칭을 git-logbook으로 정정한다.
