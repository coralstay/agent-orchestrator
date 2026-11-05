---
id: AO-5
title: 생존 에이전트를 추적·회수하는 SessionRegistry를 만든다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:49'
labels:
  - skeleton
  - registry
  - docs-sync
milestone: m-0
dependencies:
  - AO-1
references:
  - decision-3
documentation:
  - ../coralstay-software-factory/에이전트_레일_파이프라인.html
  - ../coralstay-software-factory/README.md
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/registry/SessionRegistry.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
Semaphore로 동시 실행 수를 제한해 에이전트를 admit하고, 살아 있는 세션을 추적하며, 회수 시 자손 프로세스까지
종료됐는지 확인한 뒤 목록에서 지우는 레지스트리.

## 맥락
RFC 06절 Session Registry + "virtual thread + Semaphore". Semaphore 상한은 RFC상 min(그래프 폭, 자원 상한)이지만
스켈레톤은 기본 1이고 설정으로 바꿀 수 있게만 둔다(그래프 폭 계측은 범위 밖). 회수 = 프로세스 종료 + 레지스트리
제거라는 정의를 이 태스크에서 확정해 doc-1 D4를 마무리한다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/registry/SessionRegistry.java`
- `src/test/java/com/coralstay/orchestrator/registry/SessionRegistryTest.java`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `admit(taskId, role)`: Semaphore 허가를 얻은 뒤 세션을 등록하고, 용량이 차면 허가가 날 때까지 기다린다
- [x] #2 `retire(session)`: 프로세스와 자손이 모두 종료됐는지 확인(필요 시 destroy)한 뒤 목록에서 제거하고 허가를 반납한다
- [x] #3 `live()`: 현재 살아 있는 세션의 불변 스냅샷(taskId, role, pid, 시작 시각)을 돌려준다
- [x] #4 팩토리 문서 D4 반영(회수 측): RFC 04·07절과 README 로드맵 1의 "`/clear` → 회수"를 실측한 회수 정의로 정정했다
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
1. `Session` record(taskId, role, ProcessHandle, startedAt).
2. admit/retire 테스트: 용량 1에서 두 번째 admit이 첫 retire까지 블록되는지(virtual thread + 타임아웃 assert).
3. retire는 idempotent — 두 번 불러도 허가를 두 번 반납하지 않는다.
4. `live()` 스냅샷 불변성 테스트.
5. D4 회수 측 정정.

의존: AO-1 — promote가 draft ID 의존성을 갱신하지 않아(backlog 1.51.0 실측) promote 후 AO ID로 재설정함.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
허가 이중 반납은 Semaphore 용량을 몰래 늘리는 버그가 된다 — retire는 ConcurrentHashMap.remove 성공 시에만 release. JVM 종료 시 남은 세션을 정리하는 shutdown hook은 T7에서 연결.

구현: eb89902(admit/attach) · 615e721(retire, AgentSpawner.terminate public화+SIGKILL 후 종료 대기) · fade543(live). 팩토리 d71e2b7(RFC 04절 회수 정의·결론1·12절·도구표 chub 행·fig.5·README 로드맵1). doc-1 D4 ✅ 8e1a806. 테스트: ./gradlew test --rerun-tasks BUILD SUCCESSFUL, 61건(SessionRegistryTest 16) 실패 0.
API(AO-7용): Session s = registry.admit(taskId, role) (InterruptedException 선언) → try { agent = spawner.start(...); registry.attach(s, agent.process()); agent.await(); } finally { registry.retire(s); }. retire는 멱등(remove(key,value) 성공 시에만 release), 미attach 세션도 허가 반납. live()는 LiveSession(taskId, role, OptionalLong pid, startedAt=admit 시각).
한계: root가 정상 종료한 뒤 남은 백그라운드 자손은 재부모화되어 retire가 찾지 못함 — AO-7 흐름(await 후 retire)에서는 항상 이 경우라 정상 종료 잔여 자손은 회수되지 않는다. 실행 중 자손 주기 기록은 범위 밖. 팩토리 커밋 메시지에 도구표를 '10절'로 적었으나 실제는 12절(히스토리 재작성 금지라 그대로 둠). AC4의 '07절'에는 회수/clear 서술이 없어 변경 없음(fig.5 파일명 07-task-sequence를 가리킨 것으로 판단). 아키텍처.md 6절의 spawn/·registry/ '(예정)' 표기는 미갱신(범위 밖).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Semaphore(기본 1) 기반 SessionRegistry 구현: admit(블록·중복 거부)·attach·retire(프로세스+자손 종료→제거→허가 반납, 멱등)·live(불변 스냅샷). AgentSpawner.terminate를 공용(public, 종료 확인 boolean)으로 열어 단일 구현 유지. 팩토리 문서 D4 회수 측을 d71e2b7로 반영해 doc-1 D4 종료. 정상 종료 후 남은 자손은 회수 불가라는 한계를 코드·RFC에 명시.
<!-- SECTION:FINAL_SUMMARY:END -->
