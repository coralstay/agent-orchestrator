---
id: AO-3
title: 태스크별 git worktree를 만들고 지우는 WorktreeManager를 만든다
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:25'
labels:
  - skeleton
  - worktree
  - docs-sync
milestone: m-0
dependencies:
  - AO-1
references:
  - decision-3
documentation:
  - ../coralstay-software-factory/아키텍처.md
  - ../coralstay-software-factory/README.md
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/workspace/WorktreeManager.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
태스크 ID 하나에 대해 `../<repo>-wt/<id>` 디렉토리와 `task/<id>` 브랜치를 만들고, 회수 시 지우는 관리자.

## 맥락
decision-3: 스켈레톤 범위에 worktree 격리를 포함한다. 계층 대응(에이전트 배정 = 파일 하나)은 워크트리 분리를
전제로 에이전트끼리 같은 파일을 만질 일이 없다고 주장한다. 브랜치명 `task/AO-n`은 git-logbook `Task-Id` 추출
규칙(`<prefix>-<번호>`, `gitformat.taskPrefix=AO`)과 claude-rails 브랜치 확인을 동시에 만족해야 한다.
아키텍처.md 6절 디렉토리 구조(allocator/interceptor/parser/web)가 RFC 06절 컴포넌트명과 어긋나는 문제(doc-1 D3)를
실제 패키지 구조로 정정한다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/workspace/WorktreeManager.java`
- `src/test/java/com/coralstay/orchestrator/workspace/WorktreeManagerTest.java` (임시 git 저장소에서 실제 git 실행)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `create(taskId)`: `git worktree add -b task/<id> ../<repo>-wt/<id>`를 실행하고 생성된 경로를 돌려준다
- [ ] #2 `create(taskId)`: 같은 경로나 브랜치가 이미 있으면 덮어쓰지 않고 원인을 담은 예외를 던진다
- [ ] #3 `remove(taskId)`: worktree를 제거하고 `git worktree prune`까지 실행한다 (브랜치는 남긴다)
- [ ] #4 팩토리 문서 D3 반영: 아키텍처.md 6절·README 6절 디렉토리 구조를 실제 패키지(backlog/workspace/spawn/registry/ledger)로 갱신했다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-logbook 형식 검사를 통과했다
- [ ] #3 팩토리 `docs/orchestrator-skeleton` 브랜치에 문서 반영 커밋이 있다
- [ ] #4 doc-1 해당 행을 ✅(팩토리 커밋 해시) 또는 ❌로 갱신했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 테스트에서 @TempDir에 `git init` + 첫 커밋으로 저장소를 만들고(git-logbook 훅 영향 없게 hooksPath 비움) create/remove 검증.
2. 경로 규칙: `repoRoot.getParent().resolve(repoName + "-wt").resolve(taskId)`.
3. 충돌 케이스(경로 존재, 브랜치 존재) 테스트.
4. remove는 `git worktree remove --force` 대신 기본 remove — 미커밋 변경이 있으면 실패하게 두어 사람 판단으로 넘긴다.
5. 팩토리 D3 반영.

의존: AO-1 — promote가 draft ID 의존성을 갱신하지 않아(backlog 1.51.0 실측) promote 후 AO ID로 재설정함.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
브랜치는 지우지 않는다 — Done 시 커밋을 읽어 카드에 기록하는 RFC 06절 흐름과 스쿼시/머지 미결 문제 때문에 브랜치 처리는 사람 몫으로 남긴다.
<!-- SECTION:NOTES:END -->
