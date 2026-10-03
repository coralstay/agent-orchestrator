---
id: DRAFT-2
title: backlog CLI를 감싸는 BacklogClient를 만든다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:18'
labels:
  - skeleton
  - backlog
  - docs-sync
milestone: m-0
dependencies:
  - DRAFT-1
references:
  - decision-2
documentation:
  - ../coralstay-software-factory/에이전트_레일_파이프라인.html
  - ../coralstay-software-factory/README.md
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/backlog/BacklogClient.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
태스크 ID로 `backlog task view <id> --plain`을 실행해 제목·상태·AC 목록·의존성을 담은 값 객체를 돌려주고,
스폰해도 되는 태스크인지(`To Do` + 의존 태스크 전부 Done) 판정하는 클라이언트.

## 맥락
RFC 04-0절은 Board Watcher가 backlog.md의 readiness(isReady/isBlocked)를 그대로 읽는다고 가정한다. 이 가정과
README 로드맵 "실측 3종" 중 backlog.md 필드 지원 범위(doc-1 D7)를 backlog 1.51.0 실제 출력으로 확인한다.
사람이 In Progress로 옮긴 태스크(사람 선점 락)는 실행 대상이 아니다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/backlog/BacklogClient.java`
- `src/test/java/com/coralstay/orchestrator/backlog/BacklogClientTest.java` (실제 `--plain` 출력 샘플을 픽스처로 저장)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `view(id)`: `backlog task view <id> --plain`을 ProcessBuilder로 실행하고 종료코드≠0이면 stderr를 담은 예외를 던진다
- [ ] #2 `parse(plainText)`: 제목·상태·AC(번호, 체크 여부, 본문)·의존 태스크 ID를 추출한다 (실출력 샘플 테스트)
- [ ] #3 `isRunnable(task, lookup)`: 상태가 To Do이고 의존 태스크가 모두 Done일 때만 true — In Progress(사람 선점)는 false
- [ ] #4 팩토리 문서 D7 반영: backlog 1.51.0 `--plain` 출력이 제공하는/제공하지 않는 필드를 RFC 04-0·08절과 README 로드맵에 기록했다
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
1. 이 저장소 backlog에 실제 태스크로 `backlog task view AO-x --plain` 출력을 떠서 테스트 리소스로 저장(의존성 있는 것/없는 것).
2. `parse` 테스트 → 구현. 의존성 표기가 출력에 없으면 그 사실 자체를 D7 결과로 기록하고 대안(`--json` 유무, 파일 frontmatter `dependencies`) 확인.
3. `view`는 실행기(Function<List<String>, ProcessResult>)를 주입받아 테스트에서 가짜로 대체.
4. `isRunnable` 테스트(To Do/In Progress/Done × 의존 Done/미완).
5. 팩토리 문서 D7 반영.

의존(계획 키 기준): T1 — dependencies 필드에 draft ID로 설정됨. promote 후 AO ID로 바뀌었는지 확인하고 아니면 `--dep`로 재설정한다.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
RFC는 isReady가 backlog.md 내부 계산이라고 하지만 CLI 출력에 노출되는지는 미확인 — 노출되지 않으면 의존 태스크를 각각 view해서 직접 판정한다. 출력 포맷이 바뀌면 깨지므로 파서는 섹션 헤더 기반으로 느슨하게.
<!-- SECTION:NOTES:END -->
