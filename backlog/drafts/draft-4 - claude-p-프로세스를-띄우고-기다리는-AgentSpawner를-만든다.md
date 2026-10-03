---
id: DRAFT-4
title: claude -p 프로세스를 띄우고 기다리는 AgentSpawner를 만든다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:18'
labels:
  - skeleton
  - spawner
  - docs-sync
milestone: m-0
dependencies:
  - DRAFT-1
references:
  - decision-5
documentation:
  - ../coralstay-software-factory/에이전트_레일_파이프라인.html
  - ../coralstay-software-factory/diagrams/07-task-sequence.mmd
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/spawn/AgentSpawner.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
역할·작업 디렉토리·프롬프트를 받아 `claude -p` 헤드리스 프로세스를 virtual thread에서 실행하고, 타임아웃 안에
종료를 기다려 종료코드·stdout·stderr·PID를 담은 `AgentResult`를 돌려주는 스포너.

## 맥락
RFC 06절 Agent Spawner. RFC 11절①에 따라 `--bare`를 쓰지 않는다 — bare 모드는 훅을 건너뛰므로 git-logbook
트레일러와 claude-rails 게이트가 함께 꺼진다(decision-5). RFC 04절은 "회수 시 `/clear`"를 말하지만 헤드리스
프로세스는 종료가 곧 컨텍스트 폐기다 — 이를 실측해 문구를 정정한다(doc-1 D4).

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/spawn/AgentSpawner.java`
- `src/test/java/com/coralstay/orchestrator/spawn/AgentSpawnerTest.java` (claude 대신 가짜 실행 파일 스크립트 사용)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `command(role, prompt)`: `claude -p <prompt>`와 권한 모드 인자를 조립하고 `--bare`를 절대 포함하지 않는다
- [ ] #2 `spawn(role, cwd, prompt, timeout)`: virtual thread에서 프로세스를 시작해 `waitFor`로 기다리고 종료코드·stdout·stderr·PID를 `AgentResult`로 돌려준다
- [ ] #3 `spawn`: 타임아웃이 지나면 프로세스와 자손을 종료하고 timedOut=true인 결과를 돌려준다
- [ ] #4 팩토리 문서 D4 반영(스폰 측): RFC 04절·fig.5 시퀀스(`07-task-sequence.mmd`)의 `/clear` 서술을 실측 결과에 맞게 정정했다
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
1. 실행 파일 경로를 생성자에서 주입(기본 `claude`) — 테스트는 sleep/echo 셸 스크립트로 대체.
2. `command` 테스트 → 구현. 프롬프트는 인자 하나로 전달(셸 경유 금지).
3. `spawn`: `Thread.ofVirtual()` + `Process.waitFor(timeout)`; stdout/stderr는 별도 virtual thread로 drain해 파이프 막힘 방지.
4. 타임아웃 테스트: `ProcessHandle.descendants()`까지 destroy 후 isAlive=false 확인.
5. D4 정정은 T5(Registry)의 회수 실측과 함께 판단 — 이 태스크에서는 스폰 측 서술만.

의존(계획 키 기준): T1 — dependencies 필드에 draft ID로 설정됨. promote 후 AO ID로 바뀌었는지 확인하고 아니면 `--dep`로 재설정한다.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
권한 모드는 비대화형에서 승인 프롬프트로 멈추지 않도록 지정해야 한다 — 어떤 모드를 쓸지는 E2E(T8)에서 확인하고 기본값은 설정으로 뺀다. stdout을 drain하지 않으면 큰 출력에서 프로세스가 블록된다.
<!-- SECTION:NOTES:END -->
