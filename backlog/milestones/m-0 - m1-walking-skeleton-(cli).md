---
id: m-0
title: "M1 Walking skeleton (CLI)"
---

## Description

**목표** — coralstay-software-factory RFC 로드맵 1번 walking skeleton을 CLI로 구현한다: `orchestrator run <taskId>` 한 번으로 사용자의 LLM 도구가 만든 카드에서 예상 토큰(`### Token Budget`)을 읽고, 정책으로 모델을 골라 git worktree + `task/<id>` 브랜치에서 Claude Code를 헤드리스로 실행하고, 실제 토큰(커밋 트레일러 합계와 세션 총량)을 카드와 `.orchestrator/runs.jsonl`에 병기한다.

**범위** — CLI 진입(Main) · BacklogClient · WorktreeManager · AgentSpawner→AgentRunner · SessionRegistry · CommitLedger · TaskCardSource/BacklogCardSource · ModelPolicy · RunCommand · E2E 픽스처. 에이전트는 Claude Code 전용이고 실행부만 포트로 분리한다(decision-7). 각 태스크는 팩토리 문서 해당 절을 함께 정정한다(decision-4, doc-1).

**범위 밖** — 다른 코딩 에이전트 구현, 도구 무관 카드 강제, 예상치 보정, 실시간 예산 감시(전부 M2 draft), Board Watcher/Request Queue, 그래프 길이·폭 계측, Reviewer/Verifier/Logger, 임베디드 DB·Javalin·JavaParser, cgroups/tmpfs.

**완료 조건** — (1) `./gradlew test` 통과 (2) 픽스처 E2E에서 claim → 워크트리 → Claude 실행 → 잔존 프로세스 없음 → AC별 커밋 트레일러 → 카드 Token Budget에 실제치 병기 → runs.jsonl 일치 (3) doc-1 행 전부 ✅ 또는 ❌ (4) 팩토리 `docs/orchestrator-skeleton` 브랜치 반영이 PR로 올라가 있음.
