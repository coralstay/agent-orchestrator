---
id: decision-2
title: CLI는 run <taskId> 단발 실행으로 시작한다
date: '2026-10-03 03:15'
status: accepted
---
## Context

RFC 06절 fig.7은 Board Watcher → Request Queue → Dispatcher → Spawner → Registry를 그리지만, 보드 감지 방식
(CLI 폴링 vs WatchService)은 09절에서 미결이다. 로드맵 1번 walking skeleton은 Implementer 하나의
스폰 → 작업 → 회수만 검증하면 된다.

## Decision

진입점은 `orchestrator run <taskId>` **단발 실행** 하나다. 태스크 하나를 처리하고 결과를 기록한 뒤 종료한다.
Watcher·Queue·폴링 루프는 이 마일스톤에서 만들지 않는다.

## Alternatives

- run + watch 동시 제공: 감지 방식이 미결인 상태에서 구현을 먼저 정하게 되고, 스켈레톤의 검증 대상(Spawner·Registry·Semaphore·토큰)과 무관한 복잡도가 붙는다.

## Consequences

- 아키텍처.md 1·4절의 WatchService 서술을 "미정"으로 정정한다(doc-1 D2).
- 다음 마일스톤에서 Watcher를 붙일 때 `RunCommand`를 그대로 재사용할 수 있게 흐름을 한 클래스에 모은다.
