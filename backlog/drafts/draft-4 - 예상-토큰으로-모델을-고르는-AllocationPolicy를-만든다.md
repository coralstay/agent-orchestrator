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
references:
  - decision-8
  - decision-12
  - decision-13
  - decision-7
  - AO-16
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java
  - src/test/java/com/coralstay/orchestrator/policy/ModelPolicyTest.java
priority: medium
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
카드의 예상 토큰(in+out)으로 Claude 모델을 고르는 정책을 자바 코드로 만든다. 정책 확장 지점 `AllocationPolicy` 인터페이스, 내장 구현 `DefaultAllocationPolicy`(토큰 구간 · 모델 이름은 클래스 상수), 구현 선택 로직(`-Dpolicy.class` → `ServiceLoader`, 둘 다 정책을 못 정하면 에러 메시지와 함께 시작하지 않는다 — 폴백 없음). 고른 정책 클래스의 정규 이름과 경로를 결과에 남긴다. 현재 `ModelPolicy` 시그니처 스텁(decision-10의 key=value 설정 파일 계획)을 대체한다.

## 맥락
decision-12(decision-10 대체): 정책은 설정 파일이 아니라 자바 클래스로 쓴다 — 최상위 ORC-009 · ORC-010, 3.04 정책 계약, ADR-003. 교체는 JDK 표준만 써서 외부 의존성이 없다(decision-1 · P1). 정책 결정 2(2026-10-11): 정책 미지정 · 로딩 실패면 내장 구현으로 폴백하지 않고 에러 메시지와 함께 시작하지 않는다. `DefaultAllocationPolicy`는 `-Dpolicy.class` 또는 `ServiceLoader` 등록으로 명시적으로 골랐을 때만 쓴다. 고른 정책 클래스(정규 이름)와 경로는 결과 · 실행 기록에 남긴다.
현재 범위는 모델 선택뿐이지만(Claude 전용, decision-7) 인터페이스 이름은 AllocationPolicy로 두어 나중에 역할 · 개수 · 시점을 더해도 호출부를 바꾸지 않는다. 정책 모델 이름은 git-trail의 AI-Model 기록과 맞춘다. 기준값은 session 단위 예상 in+out(decision-8). 예상치가 없는 카드는 실행 전에 검증에서 걸러지므로 정책은 예상치가 있다고 가정한다.

테스트 방식은 decision-13(decision-11 대체): 별도 테스트 카드 없이 이 카드 안에서 AC 하나씩 TDD — AC #n과 그 AC의 비즈니스 규칙 테스트(실패) → 최소 구현 → 정리 → 커밋 → 다음 AC. 테스트 표식은 `@DisplayName("<카드 ID>#<n> …")` · `@DisplayName("<카드 ID>#B<k> …")`이고 `require_test_first.py` 훅이 강제한다. 시그니처 스텁 · `@Tag("pending")`은 새로 만들지 않는다.
기존 `ModelPolicyTest`(AO-16, decision-10의 key=value 형식 검증, 전부 pending)는 지운다. 그중 경계값 테스트(`chooseSmallestBucketAtOrAbove`)와 음수 거부 테스트(`chooseRejectsNegative`)의 논리만 살려 B1 · B2 테스트로 새 표식과 함께 다시 쓴다(AO-16은 참고로만 둔다).

## 비즈니스 규칙
- B1 (AC #1) 예상 in+out이 구간 상한과 같으면 그 구간의 모델을 고른다(경계 포함)
- B2 (AC #1) 예상치가 음수면 IllegalArgumentException을 던진다
- B3 (AC #1) 어느 구간에도 들지 않으면 기본 모델을 고른다
- B4 (AC #2) `-Dpolicy.class`로 지정한 클래스가 없거나 AllocationPolicy가 아니거나 인스턴스화에 실패하면 사유를 담은 에러 메시지를 내고 시작하지 않는다
- B5 (AC #3) ServiceLoader가 구현을 2개 이상 찾으면 모호함 에러를 내고 시작하지 않는다
- B6 (AC #3) 정책이 지정되지 않으면(`-Dpolicy.class` 없음, ServiceLoader 0개) 에러를 내고 시작하지 않는다 — 폴백 없음
- B7 (AC #3) 결과에 고른 정책 클래스의 정규 이름과 경로(`-Dpolicy.class` / ServiceLoader)를 남긴다

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/policy/AllocationPolicy.java`
- `src/main/java/com/coralstay/orchestrator/policy/DefaultAllocationPolicy.java`
- 선택 로직(예: `AllocationPolicies.load()` — 이름은 구현 때 정한다)
- `src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java` 제거
- `src/test/java/com/coralstay/orchestrator/policy/ModelPolicyTest.java` 제거
- `src/test/java/com/coralstay/orchestrator/policy/` 새 테스트(AC · B 표식)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `AllocationPolicy` 인터페이스와 내장 `DefaultAllocationPolicy`: 예상 in+out 이하인 가장 작은 구간의 모델, 없으면 기본 모델을 돌려준다(구간 · 모델은 클래스 상수)
- [ ] #2 `-Dpolicy.class=<FQCN>`가 있으면 그 클래스를 쓴다. 클래스가 없거나 AllocationPolicy가 아니거나 인스턴스화에 실패하면 사유와 함께 시작을 거부한다
- [ ] #3 프로퍼티가 없으면 `ServiceLoader.load(AllocationPolicy.class)` — 정확히 하나면 그것, 둘 이상이면 모호함으로, 없으면 정책 미지정으로 시작을 거부한다(내장 구현 폴백 없음, `DefaultAllocationPolicy`는 명시적으로 골랐을 때만). 고른 정책 클래스의 정규 이름과 경로를 결과에 남긴다
- [ ] #4 외부 의존성을 추가하지 않고 `ModelPolicy` 스텁과 `ModelPolicyTest`를 제거한다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-trail 형식 검사를 통과했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
AC 하나씩 TDD(decision-13): AC #n과 그 AC의 B 규칙 테스트(실패) → 최소 구현 → 정리 → 커밋 → 다음 AC.
1. AC #1 + B1 · B2 · B3: AllocationPolicy · DefaultAllocationPolicy. B1 · B2는 ModelPolicyTest의 경계값 · 음수 테스트 논리를 새 표식으로 다시 쓴다.
2. AC #2 + B4: -Dpolicy.class 경로(정상 · 클래스 없음 · 타입 불일치 · 인스턴스화 실패).
3. AC #3 + B5 · B6 · B7: ServiceLoader 경로(0 · 1 · 2개, 테스트용 META-INF/services), 고른 정책 기록.
4. AC #4: ModelPolicy 스텁과 ModelPolicyTest 제거, 의존성 무변경 확인.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
구간 기준은 세션 총량 단위(캐시 포함 입력). 초기 구간 값은 실측이 쌓이기 전이라 임시 — DefaultAllocationPolicy 상수에 근거 없음을 주석으로 남긴다. decision-10의 key=value 계획(load(path) · model.upTo.<N>)은 decision-12로 폐기.
<!-- SECTION:NOTES:END -->
