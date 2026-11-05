---
id: decision-12
title: 정책은 자바 코드로 쓴다 — AllocationPolicy + -Dpolicy.class + ServiceLoader
date: '2026-10-11 08:11'
status: accepted
---
## Context

decision-10은 모델 정책을 의존성 없는 key=value 설정 파일(`model.upTo.<N>` · `model.default`)로 두었다. 최상위 Sigkill Foundry는 2026-10-11 유저 결정으로 정책을 설정 파일이 아니라 자바 코드로 쓰기로 했다(최상위 ORC-009 · ORC-010, 3.04 정책 계약, ADR-003).
정책에는 조건 판단(예상 토큰 · 역할별 분기 등)이 들어가므로 데이터 형식보다 코드가 맞고, 컴파일러 타입 검사와 IDE 지원을 받는다. 이 저장소의 런타임 의존성 없음(decision-1, 1.02 P1)은 그대로 지켜야 한다.

## Decision

- 정책 확장 지점은 `policy/AllocationPolicy` 인터페이스 하나다. 정책은 이 인터페이스를 구현한 자바 클래스로 쓴다.
- 내장 구현은 `DefaultAllocationPolicy`다. 예상 토큰 구간과 모델 이름은 클래스 상수로 둔다. 기준값은 `session` 단위(decision-8)의 예상 `in+out`이고, 모델 이름은 git-trail의 AI-Model 기록과 맞는 것만 쓴다.
- 구현 선택 순서:
  1. `-Dpolicy.class=<FQCN>` 시스템 프로퍼티가 있으면 그 클래스.
  2. 없으면 `java.util.ServiceLoader.load(AllocationPolicy.class)` — 구현이 정확히 하나면 그것, 둘 이상이면 시작을 거부한다(모호함).
  3. 둘 다 정책을 정하지 못하면(프로퍼티 없음 · ServiceLoader 0개), 또는 지정한 클래스를 불러오지 못하면(클래스 없음 · AllocationPolicy 아님 · 인스턴스화 실패) 사유를 담은 에러 메시지를 내고 시작하지 않는다. 내장 구현으로 폴백하지 않는다(정책 결정 2, 2026-10-11).
- `DefaultAllocationPolicy`는 `-Dpolicy.class` 또는 `ServiceLoader` 등록으로 명시적으로 골랐을 때만 쓴다.
- 고른 정책 클래스의 정규 이름과 경로(`-Dpolicy.class` / `ServiceLoader`)를 결과 · 실행 기록에 남긴다.
- ②의 구현은 별도 jar에 `META-INF/services/com.coralstay.orchestrator.policy.AllocationPolicy` 등록 파일과 함께 넣어 클래스패스에 둔다.
- 둘 다 JDK 표준이라 외부 의존성이 없다(decision-1 · P1 유지).
- 현재 범위는 여전히 **모델 선택**뿐이다(Claude 전용, decision-7). 그래도 인터페이스 이름과 결과 타입은 `AllocationPolicy`로 두어, 나중에 역할 · 개수 · 시점을 정책 대상으로 더해도 호출부를 바꾸지 않는다.
- decision-10을 대체한다.

## Alternatives

- key=value 설정 파일(decision-10): 의존성은 없지만 계층 · 목록 · 조건을 표현하지 못해 정책 범위가 넓어지면 키 규약과 손으로 짠 검증이 늘어난다.
- YAML + SnakeYAML: JDK에 파서가 없어 외부 의존성이 필요하다(decision-1 위반). 조건 판단도 표현하지 못한다.
- 런타임 컴파일(`javax.tools.JavaCompiler`로 정책 `.java`를 시작 시 컴파일): 재빌드 없이 바꿀 수 있지만 정책 파일이 곧 실행 코드라 에이전트가 쓸 수 없는 위치와 보호 장치를 따로 설계해야 하고(보안 경계), 실행 환경에 JDK가 필요하다.
- 정책 미지정 시 내장 `DefaultAllocationPolicy`로 폴백(이 결정의 첫 판): 설정 실수가 조용히 기본 모델로 돌아 드러나지 않는다. 정책 결정 2(2026-10-11)로 기각했다.

## Consequences

- 최상위 ORC-010과 어긋나던 지점(YAML · key=value)이 사라진다.
- 정책을 바꾸려면 클래스를 새로 쓰고 컴파일해 클래스패스에 둬야 한다 — 파일 하나 고치는 것보다 무겁지만 타입 검사를 받는다.
- 폴백이 없으므로 정책을 지정하지 않은 설정 실수는 조용히 기본 모델로 돌지 않고 시작 시점에 드러난다. 대신 실행하려면 정책을 반드시 하나 지정해야 한다(내장 구현을 쓰더라도 `-Dpolicy.class=…DefaultAllocationPolicy` 등으로).
- 어떤 정책으로 모델을 골랐는지 기록에서 바로 알 수 있다.
- 현재 코드의 `policy/ModelPolicy`는 decision-10 기준 시그니처 스텁이다. `AllocationPolicy` · `DefaultAllocationPolicy` · 선택 로직으로 바꾸는 일은 구현 draft(DRAFT-4)가 맡는다.
