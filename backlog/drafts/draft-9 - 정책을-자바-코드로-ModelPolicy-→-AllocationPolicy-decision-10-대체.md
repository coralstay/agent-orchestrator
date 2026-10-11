---
id: DRAFT-9
title: '정책을 자바 코드로: ModelPolicy → AllocationPolicy (decision-10 대체)'
status: Draft
assignee: []
created_date: '2026-10-11 08:10'
labels:
  - design
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
최상위 Sigkill Foundry가 정책을 설정 파일이 아니라 자바 코드로 쓰기로 정했다(최상위 ORC-010 · 3.04 정책 계약 · ADR-003, 2026-10-11 유저 결정). 이 저장소의 decision-10(key=value 설정 파일)을 대체하고 설계 문서를 맞춘다. 구현은 별도 태스크.

수용 기준(승격 시 AC로 등록):
1. decision-12를 새로 남겨 decision-10을 대체한다 — AllocationPolicy 인터페이스, 내장 DefaultAllocationPolicy(토큰 구간 · 모델은 클래스 상수), 교체 순서 -Dpolicy.class → ServiceLoader(정확히 하나, 둘 이상이면 시작 거부) → 기본 구현, 외부 의존성 없음. decision-10 상태는 superseded
2. design/ 1.01 · 1.02 · 3.01 · 3.05 골격 · 4.01 골격 · 4.03 골격에서 key=value · YAML 불일치 서술을 고치고, ModelPolicy를 AllocationPolicy 설계로 바꿔 적는다(현재 코드는 ModelPolicy 스텁임을 명시). 01-packages 다이어그램 갱신
3. ModelPolicy 구현 draft(DRAFT-4)의 설명을 AllocationPolicy 기준으로 고친다
<!-- SECTION:DESCRIPTION:END -->
