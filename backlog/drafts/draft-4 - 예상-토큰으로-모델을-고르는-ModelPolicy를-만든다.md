---
id: DRAFT-4
title: 예상 토큰으로 모델을 고르는 ModelPolicy를 만든다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-04 01:47'
labels:
  - skeleton
  - policy
milestone: m-0
dependencies:
  - AO-1
  - AO-16
references:
  - decision-10
  - decision-8
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
카드의 예상 토큰(in+out)으로 Claude 모델을 고르는 정책. 의존성 없는 key=value 설정 파일을 읽는다.

## 맥락
decision-10: YAML 대신 의존성 없는 설정. 정책 모델 이름은 git-trail의 AI-Model 기록과 맞춘다. 예상치가 없는 카드는 실행 전에 검증에서 걸러지므로 정책은 예상치가 있다고 가정한다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/policy/ModelPolicy.java`
- `src/test/java/com/coralstay/orchestrator/policy/ModelPolicyTest.java`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `load(path)`: `model.upTo.<토큰수>=<모델>`, `model.default=<모델>` 형식을 읽고 형식 오류는 줄 번호와 함께 예외
- [ ] #2 `choose(estimated)`: 예상 in+out 이하인 가장 작은 구간의 모델, 없으면 default를 돌려준다
- [ ] #3 설정 파일이 없으면 내장 기본값(작은 구간 haiku, 기본 sonnet)을 쓰고 그 사실을 결과에 표시한다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-trail 형식 검사를 통과했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 파서 테스트(정상/형식 오류/중복 키).
2. choose 경계값 테스트.
3. 기본값 경로.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
구간 기준은 세션 총량 단위(캐시 포함 입력). 초기 구간 값은 실측이 쌓이기 전이라 임시 — 기본값에 근거 없음을 주석으로 남긴다.
<!-- SECTION:NOTES:END -->
