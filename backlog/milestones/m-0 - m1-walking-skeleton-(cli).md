---
id: m-0
title: "M1 Walking skeleton (CLI)"
---

## Description

**목표** — coralstay-software-factory RFC 로드맵 1번 walking skeleton을 CLI로 구현한다: `orchestrator run <taskId>` 한 번으로 backlog 태스크를 읽고, git worktree + `task/<id>` 브랜치에서 `claude -p` Implementer 1개를 Semaphore 경유로 스폰·대기·회수하고, 태스크 브랜치 커밋의 git-logbook 트레일러를 읽어 결과를 `.orchestrator/runs.jsonl`에 남긴다.

**범위** — CLI 진입(Main) · BacklogClient · WorktreeManager · AgentSpawner · SessionRegistry · CommitLedger · RunCommand · E2E 픽스처. 각 태스크는 팩토리 문서 해당 절을 함께 정정한다(decision-4, doc-1).

**범위 밖** — AllocationPolicy 주입, Board Watcher/Request Queue, 그래프 길이·폭 계측, Reviewer/Verifier/Logger, 임베디드 DB·Javalin·JavaParser, cgroups/tmpfs.

**완료 조건** — (1) `./gradlew test` 통과 (2) 픽스처 E2E에서 worktree 생성 → claude 종료 → Registry 비어 있음 + 잔존 프로세스 없음 → 트레일러 합계가 runs.jsonl과 일치 (3) doc-1 D1–D9 전부 ✅ 또는 ❌ (4) 팩토리 `docs/orchestrator-skeleton` 브랜치에 반영 커밋 존재.
