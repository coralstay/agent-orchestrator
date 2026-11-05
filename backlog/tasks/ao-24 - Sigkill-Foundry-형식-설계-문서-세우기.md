---
id: AO-24
title: Sigkill Foundry 형식 설계 문서 세우기
status: Done
assignee: []
created_date: '2026-10-05 11:52'
updated_date: '2026-10-10 22:38'
labels:
  - docs
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
최상위 Sigkill Foundry 설계 문서와 같은 5문서 세트(design/: 0 문서 정보, 1 제안서 ~ 5 구현 결과 보고서, 부록)를 이 저장소에 세우고, Claude 앱 문서를 편집 원본으로 둔다. 이 저장소의 README·backlog 레퍼런스 문서·decision에서 1.01 배경과 문제, 1.02 원칙, 3.01 시스템 구성을 채우고 나머지 장은 골격으로 둔다(원천은 기존 문서). 진척 서술 금지.

수용 기준(승격 시 AC로 등록):
1. design/ 구조·장 형식이 최상위 Sigkill Foundry와 같다
2. 1.01 · 1.02 · 3.01을 기존 문서 근거로 채우고 나머지는 골격으로 둔다
3. Claude 앱에 장별 문서와 목차 문서를 만들고 design/README.md에 링크한다
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 design/ 구조·장 형식이 최상위 Sigkill Foundry와 같다
- [x] #2 1.01 · 1.02 · 3.01을 기존 문서 근거로 채우고 나머지는 골격으로 둔다
- [x] #3 설계 문서의 원본은 design/ 하나다 — Claude 앱 편집 원본 문구 · 링크 칸을 두지 않고, 목차 상태는 실제 파일 기준
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
최상위 Sigkill Foundry와 같은 5문서 설계 세트(design/)를 세웠다. 장 파일 구성은 최상위와 일치.
- 0 문서 정보: 최상위 ORC 번호를 그대로 이어받음(하위 번호 · 로컬 대역 없음), 추적 사슬 최상위 ORC → 3.03/3.04 → 4.02 · AO 태스크 → 테스트 @DisplayName → 5.02.
- 1.01 배경과 문제(실행 기반 컴포넌트, run <taskId> 8단계), 1.02 원칙(최상위 P1~P9 적용 지점 + 로컬 P1~P10), 3.01 시스템 구성(src/main/java 패키지 지도 · 구현/pending 여부 · 테스트 클래스, 패키지 의존 DOT 다이어그램, 최상위 09-orchestrator 노드 ↔ 패키지 ↔ ORC 대응). 나머지는 골격.
- 최상위와의 불일치를 장에 기록: 정책 형식(decision-10 key=value ↔ 최상위 ORC-010 — 최상위는 이후 자바 클래스 정책으로 바뀜, 이 저장소 반영은 후속 태스크), BacklogClient --plain ↔ BL-002 --json, CommitLedger · RunCommand 미존재.
- 원본은 design/ 하나(Claude 앱 편집 원본 규칙 없음).
- 검증: ./gradlew test 통과(커밋 게이트).
<!-- SECTION:FINAL_SUMMARY:END -->
