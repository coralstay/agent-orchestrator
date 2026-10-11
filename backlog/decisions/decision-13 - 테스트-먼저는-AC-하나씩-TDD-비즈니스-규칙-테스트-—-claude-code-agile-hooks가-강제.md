---
id: decision-13
title: 테스트 먼저는 AC 하나씩 TDD + 비즈니스 규칙 테스트 — claude-code-agile-hooks가 강제
date: '2026-10-11 08:35'
status: accepted
---
## Context

decision-11은 테스트 먼저를 "테스트 카드가 구현 카드보다 먼저, 아직 없는 코드는 공개 API 시그니처 스텁 + `@Tag("pending")`"으로 정했다. 이 방식은 약속으로만 지켜졌고, 테스트 카드와 구현 카드가 갈라져 스텁 · pending 목록 · 카드 의존이 함께 늘었다. 테스트도 AC만 가리켜 카드에 적힌 비즈니스 규칙(경계값 · 거부 조건 등)이 빠질 수 있었다.
유저 결정(2026-10-11): 테스트 먼저 = AC 하나씩 TDD + 비즈니스 규칙 테스트, 규칙 목록은 카드 구성 요소, 그리고 이것을 훅으로 강제한다(최상위 Sigkill Foundry RAIL-019 · ADR-020).

## Decision

- 카드 안에서 **AC 하나씩 TDD**로 진행한다: AC #n의 테스트(실패) → 최소 구현 → 정리 → 커밋 → AC #n+1.
- 테스트는 AC만이 아니라 카드의 **비즈니스 규칙**도 다룬다. 규칙은 카드 설명의 고정 절 `## 비즈니스 규칙`에 `- B<k> (AC #<n>) <규칙>` 형식으로 적는다. 규칙마다 관련 AC를 하나 적고, 그 AC를 구현하기 전에 그 규칙의 테스트도 있어야 한다.
- 테스트 표식: `@DisplayName`에 `<카드 ID>#<n>`(AC) 또는 `<카드 ID>#B<k>`(비즈니스 규칙)를 넣는다. 예: `@DisplayName("AO-27#1 …")`, `@DisplayName("AO-27#B2 …")`.
- **강제**: claude-code-agile-hooks의 훅 `require_test_first.py`가 막는다(최상위 RAIL-019 · ADR-020). 이 저장소는 `.claude-code-agile-hooks.json`의 `testFirst`(`sourceGlobs: src/main/java/**`, `testGlobs: src/test/java/**`)로 켠다.
  - `src/main/java/**` 편집 시: 체크 안 된 첫 AC n의 표식과 관련 AC가 n인 모든 B 표식이 `src/test/java/**`에 있어야 허용.
  - `-s Done` 전환 시: 카드의 모든 AC · B 표식이 테스트에 있어야 허용.
- 별도 테스트 카드를 두지 않는다. 앞으로는 시그니처 스텁 + `@Tag("pending")`을 새로 만들지 않는다.
- 이미 있는 pending 테스트 · 스텁은 해당 구현 카드를 실행할 때 그 카드에서 정리한다(새 표식으로 다시 쓰거나 지운다).
- `build.gradle.kts`의 `test`의 pending 제외와 `pendingTest` 태스크는 남은 pending 테스트가 없어질 때까지 당분간 둔다.
- 문서 AC(팩토리 · 설계 문서 반영)는 단위 테스트 대상이 아니다.
- decision-11을 대체한다.

## Alternatives

- 테스트 카드 먼저 + 스텁 + pending(decision-11): 구현 전 타입 검사는 되지만 카드가 둘로 갈리고 스텁이 main 코드에 먼저 들어간다. 강제 수단이 없다.
- 카드의 테스트를 전부 먼저 쓰기: 한 번에 빨간 테스트가 많아 커밋 게이트(`./gradlew test`)와 부딪힌다.
- ATDD 이중 루프 · 커밋 시점 검사: 최상위 ADR-020에서 기각한 대안을 따른다.

## Consequences

- 테스트 먼저가 약속이 아니라 훅으로 강제된다. 표식 없는 소스 편집은 막힌다.
- 훅은 표식 문자열만 본다 — Bash로 소스를 쓰는 경우나 테스트가 실제로 실패했는지(빨강)는 보지 않는다.
- 구현 draft는 승격할 때 `## 비즈니스 규칙` 절을 갖춰야 한다(규칙 없는 카드는 AC 표식만 검사).
- 기존 `ModelPolicyTest` 등 pending 테스트는 해당 구현 카드(DRAFT-4 등)가 정리한다.
