---
id: decision-3
title: 워킹스켈레톤 범위에 git worktree 격리를 포함한다
date: '2026-10-03 03:15'
status: accepted
---
## Context

RFC 12절의 walking skeleton 정의는 "Implementer 1개 스폰 → 작업 → 토큰 집계 → /clear → 회수"이고
worktree는 언급하지 않는다. 그러나 계층 대응(태스크 = 파일, 에이전트 배정 = 파일)과 사람 선점 락의 전제는
"워크트리 분리로 에이전트끼리 물리적으로 갈라져 있다"는 것이다.

## Decision

스켈레톤 범위에 **git worktree 생성·제거**를 포함한다. 태스크마다 `../<repo>-wt/<id>` 디렉토리와 `task/<id>` 브랜치를 만들고
Implementer를 그 안에서 실행한다. 브랜치명은 git-logbook `Task-Id` 추출 규칙(`<prefix>-<번호>`)과 호환된다.

## Alternatives

- RFC 정의 그대로(worktree 없음): 가장 얇지만 파일 격리 가설을 다음 단계로 미루게 된다.
- Watcher까지 포함: decision-2로 기각.

## Consequences

- WorktreeManager 태스크(AO T3)가 추가된다.
- 회수 시 worktree를 지울지 남길지 정책이 필요하다 — 실패 시에는 사람이 판단하도록 남긴다(RFC 07절 "Implementer 재스폰 안 함 — 부분 커밋 상태라 사람이 판단").
