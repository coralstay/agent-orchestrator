---
id: DRAFT-9
title: 테스트 먼저를 AC 단위 TDD로(decision-11 대체)와 DRAFT-4 정리
status: Draft
assignee: []
created_date: '2026-10-11 08:34'
labels:
  - design
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
유저 결정(2026-10-11): 테스트 먼저 = AC 하나씩 TDD + 비즈니스 규칙 테스트, 규칙 목록은 카드 구성 요소, claude-code-agile-hooks가 강제(최상위 RAIL-019 · ADR-020). decision-11(테스트 카드 먼저 · 스텁 + pending)을 대체한다. 함께 DRAFT-4를 이 방식과 정책 결정 2(폴백 없음 · 고른 정책 기록)에 맞춘다. 계획: ~/.claude/plans/typed-chasing-conway.md

수용 기준(승격 시 AC로 등록):
1. decision-13을 남겨 decision-11을 대체한다(훅이 강제함을 기록), .claude-code-agile-hooks.json에 testFirst(src/main/java/** · src/test/java/**)를 켠다
2. DRAFT-4: 별도 테스트 카드 없이 AC마다 TDD, ## 비즈니스 규칙 절 추가, ModelPolicyTest 제거 계획(살릴 2개는 규칙 테스트로), AC #2 · #3을 폴백 없음 · 고른 정책 클래스 기록으로
3. decision-12와 design 문서(1.01 · 1.02 · 3.01 · 골격)에서 기본 구현 폴백 삭제 · 테스트 방식 변경을 반영한다
<!-- SECTION:DESCRIPTION:END -->
