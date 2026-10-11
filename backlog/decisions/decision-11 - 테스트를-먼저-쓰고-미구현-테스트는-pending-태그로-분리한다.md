---
id: decision-11
title: 테스트를 먼저 쓰고 미구현 테스트는 pending 태그로 분리한다
date: '2026-10-04 01:38'
status: superseded
---
## Context

유저 지시(2026-10-04): 지금까지 수행한 태스크와 앞으로 수행할 태스크 모두 테스트 코드부터 쓴다. 2026-10-04 대조 결과 AO-1~6의 코드 AC는 대부분 테스트가 있으나, 어떤 테스트가 어떤 AC인지 표식이 없고 AO-6 AC5·AO-4 AC2 검증이 빠져 있었다.
claude-rails는 `./gradlew test`가 실패하면 커밋을 막으므로, 아직 없는 코드의 테스트를 그냥 커밋할 수 없다.

## Decision

- 구현 카드보다 **테스트 카드가 먼저**다. 구현 draft는 대응하는 테스트 카드에 의존한다.
- 테스트마다 `@DisplayName("<카드>#<AC> …")`로 AC를 추적한다. AC에 매핑되지 않는 테스트는 `<카드> 보조:`.
- 아직 없는 코드는 **공개 API 시그니처 스텁**(본문 `UnsupportedOperationException("pending: <카드>")`)으로 컴파일만 되게 하고, 그 테스트는 `@Tag("pending")`.
- 기본 `./gradlew test`는 pending 제외(커밋 게이트 유지), `./gradlew pendingTest`가 빨간 목록을 보여 준다.
- 구현 카드의 완료 조건은 해당 pending 태그를 떼고 초록으로 만드는 것이다.
- 문서 AC(팩토리 반영)는 단위 테스트 대상이 아니다.

## Alternatives

- 별도 소스 셋(`src/pendingTest/java`): 스텁이 필요 없지만 구현 전까지 테스트가 타입 검사되지 않는다.
- `@Disabled`: 빨간 목록을 한 번에 볼 수 없고 스텁은 여전히 필요하다.

## Consequences

- 스텁 클래스가 main 코드에 먼저 들어간다. 실수로 호출되면 `pending:` 메시지로 바로 드러난다.
- M2 draft는 승격 전까지 테스트를 쓰지 않는다. 승격할 때 같은 방식으로 테스트부터 쓴다.

> superseded by decision-13 (AC 하나씩 TDD + 비즈니스 규칙 테스트, claude-code-agile-hooks가 강제).
