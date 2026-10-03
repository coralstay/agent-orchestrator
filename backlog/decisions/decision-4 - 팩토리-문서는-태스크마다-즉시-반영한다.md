---
id: decision-4
title: 팩토리 문서는 태스크마다 즉시 반영한다
date: '2026-10-03 03:15'
status: accepted
---
## Context

agent-orchestartor는 coralstay-software-factory 설계 중 JVM 오케스트레이터만 구현한다. 팩토리 문서는 스스로를
"검증해야 할 가설 목록"이라 부르며, 이 저장소의 산출물(실측·정정)은 팩토리 README · 아키텍처.md · RFC html/PDF · diagrams로
반영되는 구조다(doc-2).

## Decision

각 태스크의 AC에 **팩토리 문서 해당 절 수정**을 포함하고, 태스크를 끝낼 때마다 팩토리 저장소의
`docs/orchestrator-skeleton` 브랜치에 커밋한다. PR 생성·병합은 유저 확인 후에만 한다.
검증 상태는 doc-1 표에서 관리하고, 팩토리 README "출처와 검증 상태" 표에 결과를 옮긴다.

## Alternatives

- 검증 노트를 모아 마일스톤 끝에 일괄 반영: 반영 시점이 늦어져 팩토리 문서와 구현이 오래 어긋난다.

## Consequences

- RFC html이나 diagrams를 고치면 팩토리에서 `./build-diagrams.py && ./build-pdf.sh`로 재생성해 같은 커밋에 넣는다.
- 팩토리의 "원본은 하나다" 규칙(RFC → README 순)을 따른다.
