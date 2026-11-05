---
id: doc-2
title: 팩토리 문서 반영 구조
type: guide
created_date: '2026-10-03 03:15'
---

# 팩토리 문서 반영 구조

agent-orchestartor는 coralstay-software-factory 설계 중 **JVM 오케스트레이터만** 구현하는 저장소다.
이 저장소의 산출물(코드로 확인한 사실, 실측값, 정정)은 팩토리 문서로 흘러간다 — 반대 방향은 없다.

## 흐름

```
agent-orchestartor
  backlog 태스크 (AC에 "팩토리 문서 Dn 반영" 포함)
  backlog/docs/doc-1 검증 로그 (상태)
        │
        ▼ 태스크 완료 시마다 (decision-4)
coralstay-software-factory  — 브랜치 docs/orchestrator-skeleton
  ├── 에이전트_레일_파이프라인.html   RFC 원본 (수치·근거·인용의 원본)
  ├── diagrams/*.dot, *.mmd           다이어그램 원본
  ├── 아키텍처.md                      실행 기반 상세의 원본
  ├── README.md                        요약 · "출처와 검증 상태" 표
  └── 에이전트_레일_파이프라인.pdf     생성물
```

## 팩토리 쪽 규칙 (팩토리 README "이 문서를 고치는 규칙" 요약)

1. 원본을 먼저 고친다: 수치·근거는 RFC html, 실행 기반은 아키텍처.md. README는 그다음에 맞춘다.
2. 수치는 README에 옮겨 적지 않는다.
3. 링크는 README 맨 아래 링크 블록에서만 정의한다.
4. 검증 상태는 README "출처와 검증 상태" 표에서만 바꾼다.
5. 다이어그램은 `diagrams/`가 원본 — HTML 안의 `<svg>`를 직접 고치지 않는다. DOT 우선, 시퀀스만 mermaid.
6. RFC html·diagrams를 고치면 팩토리 루트에서 `./build-diagrams.py && ./build-pdf.sh`를 돌려 PDF까지 같은 커밋에 넣는다.
   빌드 전제: graphviz `dot`, mermaid 렌더용 `npx` + 로컬 Chrome.

## 이 저장소 쪽 절차

1. 태스크의 "팩토리 문서 반영" AC에 해당하는 doc-1 행을 🔍로 바꾼다.
2. 팩토리 저장소에서 `docs/orchestrator-skeleton` 브랜치로 전환해 원본 → README 순으로 고친다.
3. 팩토리 커밋은 팩토리 저장소의 커밋 규칙을 따른다.
4. doc-1 행을 ✅ + 팩토리 커밋 해시로 바꾸고 이 저장소에 커밋한다.
5. 팩토리 PR 생성·병합은 유저 확인 후.
