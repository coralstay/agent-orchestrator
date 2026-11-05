---
id: AO-25
title: '정책을 자바 코드로: ModelPolicy → AllocationPolicy (decision-10 대체)'
status: Done
assignee: []
created_date: '2026-10-11 08:10'
updated_date: '2026-10-11 08:13'
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 decision-12를 새로 남겨 decision-10을 대체한다
- [x] #2 design/의 key=value · YAML 불일치 서술을 고치고 ModelPolicy를 AllocationPolicy 설계로 바꿔 적는다(현재 코드는 ModelPolicy 스텁임을 명시), 01-packages 다이어그램 갱신
- [x] #3 ModelPolicy 구현 draft의 설명을 AllocationPolicy 기준으로 고친다
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
decision-12(정책은 자바 코드 — AllocationPolicy + -Dpolicy.class + ServiceLoader, 외부 의존성 없음)를 새로 남기고 decision-10을 superseded로 바꿨다(삭제하지 않음, 대체 안내 한 줄). design/ 1.01 · 1.02 · 3.01과 골격 장 3.03 · 3.04 · 3.05 · 4.01 · 4.03에서 key=value · YAML 불일치 서술을 걷어내고 AllocationPolicy/DefaultAllocationPolicy 설계로 고쳤다(현재 코드는 ModelPolicy 스텁, DRAFT-4가 대체). 1.02의 P1 ↔ 최상위 ORC-010 관계는 '맞는다'로 바뀌었다. 01-packages.dot의 policy 노드를 'AllocationPolicy (설계) / ModelPolicy (스텁)'로 바꾸고 build-diagrams.py로 SVG를 다시 렌더했다. DRAFT-4 제목 · 설명 · AC · 계획 · 참조를 AllocationPolicy 구현 기준으로 바꿨다. src/는 건드리지 않았다. ./gradlew test BUILD SUCCESSFUL.
<!-- SECTION:FINAL_SUMMARY:END -->
