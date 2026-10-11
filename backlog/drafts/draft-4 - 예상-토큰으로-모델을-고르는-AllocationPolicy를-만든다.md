---
id: DRAFT-4
title: 예상 토큰으로 모델을 고르는 AllocationPolicy를 만든다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-11 08:13'
labels:
  - skeleton
  - policy
milestone: m-0
dependencies:
  - AO-1
  - AO-16
references:
  - decision-8
  - decision-12
  - decision-7
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java
priority: medium
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
카드의 예상 토큰(in+out)으로 Claude 모델을 고르는 정책을 자바 코드로 만든다. 정책 확장 지점 `AllocationPolicy` 인터페이스, 내장 기본 구현 `DefaultAllocationPolicy`(토큰 구간 · 모델 이름은 클래스 상수), 구현 선택 로직(`-Dpolicy.class` → `ServiceLoader` → 기본 구현). 현재 `ModelPolicy` 시그니처 스텁(decision-10의 key=value 설정 파일 계획)을 대체한다.

## 맥락
decision-12(decision-10 대체): 정책은 설정 파일이 아니라 자바 클래스로 쓴다 — 최상위 ORC-009 · ORC-010, 3.04 정책 계약, ADR-003. 교체는 JDK 표준만 써서 외부 의존성이 없다(decision-1 · P1). 현재 범위는 모델 선택뿐이지만(Claude 전용, decision-7) 인터페이스 이름은 AllocationPolicy로 두어 나중에 역할 · 개수 · 시점을 더해도 호출부를 바꾸지 않는다. 정책 모델 이름은 git-trail의 AI-Model 기록과 맞춘다. 기준값은 session 단위 예상 in+out(decision-8). 예상치가 없는 카드는 실행 전에 검증에서 걸러지므로 정책은 예상치가 있다고 가정한다.

기존 `ModelPolicyTest`(AO-16)는 decision-10의 key=value 형식을 검증하므로 decision-11(테스트 먼저)에 따라 AllocationPolicy 기준으로 다시 써야 한다 — 이 카드 안에서 먼저 할지 별도 테스트 카드로 뗄지는 승격 전에 정한다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/policy/AllocationPolicy.java`
- `src/main/java/com/coralstay/orchestrator/policy/DefaultAllocationPolicy.java`
- 선택 로직(예: `AllocationPolicies.load()` — 이름은 구현 때 정한다)
- `src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java` 제거
- `src/test/java/com/coralstay/orchestrator/policy/` 테스트(ModelPolicyTest 대체)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `AllocationPolicy` 인터페이스와 내장 `DefaultAllocationPolicy`: 예상 in+out 이하인 가장 작은 구간의 모델, 없으면 기본 모델을 돌려준다(구간 · 모델은 클래스 상수)
- [ ] #2 `-Dpolicy.class=<FQCN>`가 있으면 그 클래스를 쓴다. 클래스가 없거나 AllocationPolicy가 아니거나 인스턴스화에 실패하면 사유와 함께 시작을 거부한다
- [ ] #3 프로퍼티가 없으면 `ServiceLoader.load(AllocationPolicy.class)` — 정확히 하나면 그것, 둘 이상이면 모호함으로 시작 거부, 없으면 `DefaultAllocationPolicy`. 어느 경로로 골랐는지 결과에 표시한다
- [ ] #4 외부 의존성을 추가하지 않고 `ModelPolicy` 스텁을 제거한다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-trail 형식 검사를 통과했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. AllocationPolicy 기준 테스트로 ModelPolicyTest 대체(decision-11).
2. DefaultAllocationPolicy 경계값.
3. -Dpolicy.class 경로(정상/오류).
4. ServiceLoader 경로(0/1/2개, 테스트용 META-INF/services).
5. ModelPolicy 스텁 제거.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
구간 기준은 세션 총량 단위(캐시 포함 입력). 초기 구간 값은 실측이 쌓이기 전이라 임시 — DefaultAllocationPolicy 상수에 근거 없음을 주석으로 남긴다. decision-10의 key=value 계획(load(path) · model.upTo.<N>)은 decision-12로 폐기.
<!-- SECTION:NOTES:END -->
