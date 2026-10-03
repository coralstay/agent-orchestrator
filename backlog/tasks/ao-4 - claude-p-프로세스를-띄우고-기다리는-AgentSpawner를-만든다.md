---
id: AO-4
title: claude -p 프로세스를 띄우고 기다리는 AgentSpawner를 만든다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:42'
labels:
  - skeleton
  - spawner
  - docs-sync
milestone: m-0
dependencies:
  - AO-1
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
- [x] #1 `command(role, prompt)`: `claude -p <prompt>`와 권한 모드 인자를 조립하고 `--bare`를 절대 포함하지 않는다
- [x] #2 `spawn(role, cwd, prompt, timeout)`: virtual thread에서 프로세스를 시작해 `waitFor`로 기다리고 종료코드·stdout·stderr·PID를 `AgentResult`로 돌려준다
- [x] #3 `spawn`: 타임아웃이 지나면 프로세스와 자손을 종료하고 timedOut=true인 결과를 돌려준다
- [x] #4 팩토리 문서 D4 반영(스폰 측): RFC 04절·fig.5 시퀀스(`07-task-sequence.mmd`)의 `/clear` 서술을 실측 결과에 맞게 정정했다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 `./gradlew test` 통과 로그를 확인했다
- [x] #2 AC마다 커밋 1개 이상이며 git-logbook 형식 검사를 통과했다
- [x] #3 팩토리 `docs/orchestrator-skeleton` 브랜치에 문서 반영 커밋이 있다
- [ ] #4 doc-1 해당 행을 ✅(팩토리 커밋 해시) 또는 ❌로 갱신했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 실행 파일 경로를 생성자에서 주입(기본 `claude`) — 테스트는 sleep/echo 셸 스크립트로 대체.
2. `command` 테스트 → 구현. 프롬프트는 인자 하나로 전달(셸 경유 금지).
3. `spawn`: `Thread.ofVirtual()` + `Process.waitFor(timeout)`; stdout/stderr는 별도 virtual thread로 drain해 파이프 막힘 방지.
4. 타임아웃 테스트: `ProcessHandle.descendants()`까지 destroy 후 isAlive=false 확인.
5. D4 정정은 T5(Registry)의 회수 실측과 함께 판단 — 이 태스크에서는 스폰 측 서술만.

의존: AO-1 — promote가 draft ID 의존성을 갱신하지 않아(backlog 1.51.0 실측) promote 후 AO ID로 재설정함.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
권한 모드는 비대화형에서 승인 프롬프트로 멈추지 않도록 지정해야 한다 — 어떤 모드를 쓸지는 E2E(T8)에서 확인하고 기본값은 설정으로 뺀다. stdout을 drain하지 않으면 큰 출력에서 프로세스가 블록된다.

## 구현 결과 (2026-10-03)
- 권한 모드 기본값 `auto` + `--permission-prompts none` (claude 2.1.285 `--help` 기준 선택지: acceptEdits, auto, bypassPermissions, manual, dontAsk, plan). auto는 분류기가 승인을 대신해 헤드리스에서 멈추지 않으면서 bypassPermissions처럼 검사를 끄지 않는다. acceptEdits는 편집만 자동 승인이라 `./gradlew test`·`git commit`이 거부될 수 있다. 생성자 인자로 바꿀 수 있고 최종 선택은 AO-8에서 실제 claude로 확인.
- API: `start(role, cwd, prompt, timeout)` → `SpawnedAgent`(role, pid, process()=ProcessHandle, startedAt, await()). `spawn(...)` = `start(...).await()`. AO-5 Session은 `process()`·`startedAt()`을 그대로 쓰면 된다.
- 타임아웃: 부모를 죽이기 전에 `descendants()` 스냅샷 → destroy → 500ms 유예 → destroyForcibly. descendants 처리를 빼면 자손 테스트가 실패함을 확인(뮤테이션).
- stdin을 즉시 닫는다(`claude -p`가 파이프 stdin을 기다리지 않게). drain 스레드는 프로세스 종료 후 최대 2초만 기다린다 — 정상 종료 시 백그라운드 자손이 파이프를 물고 있으면 부분 출력만 담고, 그 자손은 스포너가 정리하지 않는다(정상 종료 경로의 자손 정리는 AO-5 retire 몫).
- `./gradlew test` 45개 PASSED, BUILD SUCCESSFUL. AgentSpawnerTest 11개, 가장 긴 것 0.85s.
- DoD #4 미체크: D4는 회수 측(AO-5)이 남아 의도적으로 🔍(46d3e10)로만 갱신했다 — ✅/❌ 조건을 문자 그대로 만족하지 않으므로 AO-5 완료 시 ✅로 바꾸며 함께 닫는다.
- 범위 밖 발견: RFC 결론 1번(619행 부근)·12절 walking skeleton(2735행 부근)에도 "→ /clear → 회수", 13절 표 context-hub 행에 "/clear의 빈틈" 서술이 남아 있다 — AO-5 회수 측 정정 때 함께 볼 것.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
`AgentSpawner` 구현: `command`가 `claude -p <prompt> --permission-mode <mode(기본 auto)> --permission-prompts none`을 셸 없이 조립(`--bare` 금지 테스트), `start`/`spawn`이 virtual thread에서 대기·stdout/stderr 동시 drain 후 `AgentResult(exitCode, stdout, stderr, pid, timedOut, duration)` 반환, 타임아웃 시 프로세스와 자손을 destroy→destroyForcibly로 종료. 테스트는 @TempDir 가짜 실행 스크립트로만(실제 claude 미실행), 전체 45개 통과. 팩토리 46d3e10에 RFC 04절·fig.5 `/clear` 서술을 "헤드리스 프로세스 종료 = 컨텍스트 폐기"로 정정(스폰 측), doc-1 D4는 🔍 — 회수 측은 AO-5.
<!-- SECTION:FINAL_SUMMARY:END -->
