---
id: AO-2
title: backlog CLI를 감싸는 BacklogClient를 만든다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:32'
labels:
  - skeleton
  - backlog
  - docs-sync
milestone: m-0
dependencies:
  - AO-1
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
- [x] #1 `view(id)`: `backlog task view <id> --plain`을 ProcessBuilder로 실행하고 종료코드≠0이면 stderr를 담은 예외를 던진다
- [x] #2 `parse(plainText)`: 제목·상태·AC(번호, 체크 여부, 본문)·의존 태스크 ID를 추출한다 (실출력 샘플 테스트)
- [x] #3 `isRunnable(task, lookup)`: 상태가 To Do이고 의존 태스크가 모두 Done일 때만 true — In Progress(사람 선점)는 false
- [x] #4 팩토리 문서 D7 반영: backlog 1.51.0 `--plain` 출력이 제공하는/제공하지 않는 필드를 RFC 04-0·08절과 README 로드맵에 기록했다
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
1. 이 저장소 backlog에 실제 태스크로 `backlog task view AO-x --plain` 출력을 떠서 테스트 리소스로 저장(의존성 있는 것/없는 것).
2. `parse` 테스트 → 구현. 의존성 표기가 출력에 없으면 그 사실 자체를 D7 결과로 기록하고 대안(`--json` 유무, 파일 frontmatter `dependencies`) 확인.
3. `view`는 실행기(Function<List<String>, ProcessResult>)를 주입받아 테스트에서 가짜로 대체.
4. `isRunnable` 테스트(To Do/In Progress/Done × 의존 Done/미완).
5. 팩토리 문서 D7 반영.

의존: AO-1 — promote가 draft ID 의존성을 갱신하지 않아(backlog 1.51.0 실측) promote 후 AO ID로 재설정함.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
RFC는 isReady가 backlog.md 내부 계산이라고 하지만 CLI 출력에 노출되는지는 미확인 — 노출되지 않으면 의존 태스크를 각각 view해서 직접 판정한다. 출력 포맷이 바뀌면 깨지므로 파서는 섹션 헤더 기반으로 느슨하게.

실측(2026-10-03): `backlog draft promote`는 다른 draft를 가리키는 dependencies(DRAFT-n)를 새 AO ID로 바꾸지 않는다 — D7 기록 대상.

실행 기록 (2026-10-03):
- 1dd9cac parse: --plain 섹션 헤더(제목+대시 밑줄) 기반. 제목·상태(글리프 제거)·AC·직접 의존 ID. 실출력 AO-1(Done, 의존 없음)/AO-2(In Progress, 1)/AO-7(To Do, 5)을 src/test/resources/backlog에 픽스처로 저장
- 89ccd8f view: 실행기 Function<List<String>, ProcessResult> 주입, exit≠0이면 stderr 담은 BacklogException. processExecutor(Path)는 ProcessBuilder + stdout/stderr 동시 읽기. 실제 CLI 테스트 2건(AO-1 Done, 없는 ID exit 1 + stderr; PATH에 backlog 없으면 skip)
- 2569512 isRunnable: To Do && 직접 의존 전부 Done. In Progress/Done → false (7케이스 파라미터 + 실픽스처)
- 팩토리 aaa0b0a (docs/orchestrator-skeleton): RFC 04-0절 실측 단락, 08절 단서, README 로드맵 2 → 출처와 검증 상태 표 행 추가, PDF 재생성
- a84a755 doc-1 D7 ✅ aaa0b0a
- 증거: ./gradlew test --rerun-tasks → 27건 PASSED(BacklogClientTest 17), BUILD SUCCESSFUL
D7 실측 (backlog 1.51.0):
- --plain: Status(글리프 ○/◒/✔ 접두), Milestone, AC 체크, Dependency Graph(Depends on 트리·전이 포함, Done은 [completed], Dependents 역방향 블록) 제공. isReady/readiness 없음
- --json(schemaVersion 1)이 task view/list 모두에 존재: dependencies, readiness{isReady,isBlocked,blockingDependencies,missingDependencies}, milestone, acceptanceCriteria 구조화
- isReady = Done 아님 && 의존 전부 Done → In Progress도 true(AO-2), Done은 false. task list --ready도 In Progress 포함. 할당 조건으로는 status==To Do 추가 필요
- 없는 ID: exit 1, 메시지는 stderr
- draft promote는 DRAFT-n 의존성을 새 ID로 바꾸지 않음
후속 제안(범위 밖): 파서를 --json 기반으로 바꾸면 포맷 취약성이 줄어든다
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
backlog task view <id> --plain을 실행(실행기 주입)해 제목·상태·AC·직접 의존 ID를 담은 Task로 파싱하는 BacklogClient와, To Do이면서 의존이 전부 Done일 때만 참인 isRunnable을 만들었다(In Progress 사람 선점은 거짓). 실출력 픽스처 3종과 실제 CLI 테스트 포함, ./gradlew test 27건 통과. backlog 1.51.0 실측(D7) — readiness/isReady는 --json에만 있고 In Progress도 isReady라 그대로 할당 조건으로 못 씀, draft promote는 DRAFT-n 의존성을 갱신하지 않음 — 을 팩토리 aaa0b0a에 반영하고 doc-1 D7을 ✅로 갱신했다.
<!-- SECTION:FINAL_SUMMARY:END -->
