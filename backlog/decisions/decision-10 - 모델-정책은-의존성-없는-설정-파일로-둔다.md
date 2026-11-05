---
id: decision-10
title: 모델 정책은 의존성 없는 설정 파일로 둔다
date: '2026-10-03 13:03'
status: superseded
---
> decision-12(정책은 자바 코드로 쓴다 — AllocationPolicy + -Dpolicy.class + ServiceLoader)로 대체됐다.

## Context

예상 토큰으로 모델을 고르는 정책이 필요하다. RFC는 AllocationPolicy + YAML을 제안했지만 decision-1은 런타임 의존성을 두지 않기로 했다.
Claude 전용(decision-7)이라 정책은 "예상 토큰 구간 → 모델" 정도로 단순하다.

## Decision

- `policy/ModelPolicy`가 의존성 없는 설정 파일(`orchestrator.properties` 형식, 예: `model.upTo.200000=claude-haiku-…`, `model.default=claude-sonnet-…`)을 읽는다.
- 기준값은 `session` 단위(decision-8)의 예상 `in+out`.
- 정책 모델은 git-trail의 AI-Model 기록과 맞는 이름만 쓴다.
- 인터페이스는 하나로 두어 나중에 다른 구현(예: 실측 기반 보정)으로 바꿀 수 있게 한다.

## Alternatives

- YAML + SnakeYAML: decision-1과 충돌.

## Consequences

- 설정 파일 형식은 단순 key=value라 중첩 정책은 표현하지 않는다. 필요해지면 그때 형식을 다시 정한다.
