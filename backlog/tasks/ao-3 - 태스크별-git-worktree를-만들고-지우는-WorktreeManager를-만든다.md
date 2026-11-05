---
id: AO-3
title: 태스크별 git worktree를 만들고 지우는 WorktreeManager를 만든다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:37'
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
- [x] #1 `create(taskId)`: `git worktree add -b task/<id> ../<repo>-wt/<id>`를 실행하고 생성된 경로를 돌려준다
- [x] #2 `create(taskId)`: 같은 경로나 브랜치가 이미 있으면 덮어쓰지 않고 원인을 담은 예외를 던진다
- [x] #3 `remove(taskId)`: worktree를 제거하고 `git worktree prune`까지 실행한다 (브랜치는 남긴다)
- [x] #4 팩토리 문서 D3 반영: 아키텍처.md 6절·README 6절 디렉토리 구조를 실제 패키지(backlog/workspace/spawn/registry/ledger)로 갱신했다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 `./gradlew test` 통과 로그를 확인했다
- [x] #2 AC마다 커밋 1개 이상이며 git-logbook 형식 검사를 통과했다
- [x] #3 팩토리 `docs/orchestrator-skeleton` 브랜치에 문서 반영 커밋이 있다
- [x] #4 doc-1 해당 행을 ✅(팩토리 커밋 해시) 또는 ❌로 갱신했다
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

구현 결과(AO-3):
- WorktreeManager는 BacklogClient.ProcessResult/processExecutor를 재사용(실행기 주입). git 실패는 WorktreeException(명령+exit+stderr).
- create: git은 빈 디렉토리가 이미 있으면 worktree add를 그대로 진행한다(실측, git 2.54) → 경로 존재를 직접 검사. 브랜치는 rev-parse --verify로 검사. 둘 다 아무것도 바꾸지 않고 예외.
- 태스크 ID는 [A-Za-z0-9][A-Za-z0-9_-]*만 허용 — ../ 경로 이탈 차단.
- remove: --force 없이 worktree remove → prune. 미커밋 변경 시 실패하고 worktree 유지. 디렉토리가 이미 없으면 prune만(멱등). 브랜치 유지.
- 테스트: @TempDir 실제 git 저장소, 로컬 core.hooksPath=빈 디렉토리, user.name/email·commit.gpgsign 로컬 설정. WorktreeManagerTest 7개 + 전체 34개 통과(./gradlew test --rerun-tasks).
- 팩토리 6a20bc0(docs/orchestrator-skeleton): 아키텍처.md 6·7절, README 6절 + [impl] 링크. doc-1 D3 ✅.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
태스크별 git worktree를 만들고 지우는 WorktreeManager(workspace 패키지) 구현. create는 <parent>/<repo>-wt/<id>에 task/<id> 브랜치로 worktree를 만들고, 경로(빈 디렉토리 포함)·브랜치가 있으면 아무것도 바꾸지 않고 예외. remove는 --force 없이 worktree remove 후 prune, 브랜치 유지(미커밋 변경 시 실패). 실제 git 임시 저장소 테스트 7개 포함 34개 통과. 팩토리 문서 D3(디렉토리 구조 ↔ RFC 06절 컴포넌트명) 반영: 6a20bc0.
<!-- SECTION:FINAL_SUMMARY:END -->
