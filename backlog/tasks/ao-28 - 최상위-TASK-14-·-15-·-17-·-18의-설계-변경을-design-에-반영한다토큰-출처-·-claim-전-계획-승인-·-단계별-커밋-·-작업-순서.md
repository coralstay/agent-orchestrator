---
id: AO-28
title: >-
  최상위 TASK-14 · 15 · 17 · 18의 설계 변경을 design/에 반영한다(토큰 출처 · claim 전 계획 승인 · 단계별
  커밋 · 작업 순서)
status: Done
assignee: []
created_date: '2026-10-22 05:53'
updated_date: '2026-10-22 06:23'
labels:
  - design
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
최상위 coralstay-software-factory의 설계 변경 전파(RAIL-024). TASK-14: coding-agent-git-commit-tool은 포맷 레이어가 되고 토큰 계량은 별도 '토큰 계량 컴포넌트(이름 미정)'로 분리됐다(ADR-021, ORC-021 토큰 출처 변경) — 이 저장소 design/은 아직 'Tokens-Used 트레일러를 읽는다'(1.01 · 1.02 P5 · 2.02 · 3.04)고 적고 있다. TASK-15 · 17 · 18: 착수(In Progress) 전에 사람의 계획 승인 커밋(Plan-Approved: <ID>, 사람 서명자 humanSigners)이 있어야 하고(ADR-022), 단계마다 커밋하며, 앞으로 할 일이 생길 때마다 작업 순서 파일을 다시 계산한다(ADR-023, BL-016 · RAIL-023). 이 저장소의 claim(카드를 In Progress + @claude로 바꿔 커밋, decision-9)이 이 게이트를 거쳐야 한다. 커밋 본문에 'Refs: coralstay-software-factory TASK-14 · TASK-15 · TASK-17 · TASK-18'을 적는다.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 토큰 출처: 1.01 · 1.02(P5) · 2.02 · 3.04 등에서 Tokens-Used 트레일러를 읽는다는 서술을 최상위 ADR-021 · ORC-021에 맞춰 토큰 계량 컴포넌트(이름 미정, 측정 방식 TBD)로 바꾼다
- [x] #2 claim과 계획 승인: claim(→ In Progress) 전에 사람의 계획 승인 커밋(Plan-Approved: <ID>, humanSigners 서명자)이 있어야 하고, 없으면 그 카드는 실행 가능 판정에서 빠지거나 claim이 막힌다는 것을 동작 · 인터페이스 · 결정 장에 반영한다(최상위 ADR-022)
- [x] #3 단계별 커밋 · 작업 순서: claim · 완료 커밋이 단계별 커밋 규칙(상태 전환 커밋은 그 카드 파일만)을 따르고, 실행 순서 판정이 작업 순서 파일(같은 단계 = 병렬)과 맞는다는 것을 반영한다(최상위 ADR-023, BL-016 · RAIL-023)
- [x] #4 각 커밋 본문에 'Refs: coralstay-software-factory TASK-14 · TASK-15 · TASK-17 · TASK-18'을 적는다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 design/ 상대 링크 검사에서 깨진 링크가 없다
- [x] #2 파일 하나에 커밋 하나, push 전 Done + final summary
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Subjective
유저가 최상위 설계 변경(TASK-14 · 15 · 17 · 18)의 컴포넌트 전파를 별도 카드로 하자고 했다(2026-10-22).

### Objective
- 이 저장소 design/: Tokens-Used 3곳 · 토큰 19곳(P5 '토큰은 커밋 트레일러 계약만 읽는다'), claim은 카드를 In Progress + @claude로 바꿔 커밋(decision-9).
- 최상위: ADR-021(토큰 계량 분리), ADR-022(계획 승인 · 사람 서명자), ADR-023(단계별 커밋 · 작업 순서), BL-016 · RAIL-023.
- 전파 훅(hooks TASK-63)을 읽기 전용으로 돌리면 이 저장소가 TASK-14 · 15 · 17 · 18 모두에서 누락으로 나온다.

### Assessment
토큰 출처는 서술 교체로 끝나지만, claim 전 계획 승인은 오케스트레이터 동작을 바꾼다 — 사람 승인이 없는 카드는 실행 가능 판정에서 빼는 쪽이 자연스럽다(구현은 별도 카드). 이 카드는 설계 문서만 맞춘다.

### Plan
1. 최상위 TASK-14 · 15 · 17 · 18 diff를 장 대응표(2.03 ORC · 3.03 §1 · 3.04 §1 §2 §6 등)로 정리.
2. 장마다 반영, 파일 하나에 커밋 하나(Refs 줄).
3. 링크 검사. 구현 변경이 필요하면 별도 draft로 제안.
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
최상위 coralstay-software-factory TASK-14 · 15 · 17 · 18의 설계 변경을 이 저장소 design/에 전파했다(RAIL-024). 커밋 12개, 파일당 하나, 모두 본문에 'Refs: coralstay-software-factory TASK-14 · TASK-15 · TASK-17 · TASK-18'.

- AC #1 토큰 출처: Tokens-Used 트레일러를 읽는다는 서술을 토큰 계량 컴포넌트(이름 미정, 측정 TBD 최상위 Q-18)로 — 1.01 · 1.02 P5 · P6 · 1.04 · 2.02 · 2.03 ORC-021 · 3.01 · 3.03 · 3.04 · 4.03. 남은 Tokens-Used 5곳은 decision-5 · 8 출처가 사라졌다는 이력 메모
- AC #2 claim 전 계획 승인: Plan-Approved: <ID>(%G?=G ∧ %GS ∈ humanSigners, 최상위 ADR-022) 없이는 실행 가능 판정에서 빠지거나 claim이 막힌다는 설계 요구 — 1.01 · 1.02 P7 · 2.02 · 2.03 ORC-005 · BL-017 · 3.01 · 3.02(실패 경로) · 3.04 · 3.05. 구현은 별도 카드
- AC #3 단계별 커밋 · 작업 순서: claim · 실제 기록 커밋은 카드 파일만, 실행 순서는 work-order.md(같은 단계 = 병렬, 동시성 허가가 상한), backlog/ 미커밋 변경 중 claim 금지 — 1.01 · 1.02 P4 · 2.02 · 2.03 BL-016 · 3.02 · 3.04
- AC #4 12개 커밋 모두 Refs 줄 확인(grep)

검증: design/ 상대 링크 검사 → 백틱 안 템플릿 예시(<이름>.svg) 1건뿐(기존), git status 깨끗, 다이어그램 변경 없음.
이 저장소 결정과의 어긋남(3.05 · 1.02에 기록, 결정 자체는 고치지 않음): decision-5(트레일러만 읽음)는 ADR-021과 충돌 — Q-18 확정 뒤 대체 결정 필요. decision-8 commits 값 출처 상실. decision-9는 claim 전 계획 승인 · 작업 순서 최신 확인을 요구하지 않음. 최상위 3.04 §3은 착수를 task/<ID>에서, decision-9는 main에서 커밋 — 미기록 충돌 가능성.
후속 카드 후보: isRunnable/claim에 승인 검사, claim 전 작업 순서 최신 확인, commits 값을 계량 컴포넌트 출력으로(Q-18 대기), decision-5 · 8 · 9 대체 결정, 이 저장소 00에 ADR-025 전파 규칙.
<!-- SECTION:FINAL_SUMMARY:END -->
