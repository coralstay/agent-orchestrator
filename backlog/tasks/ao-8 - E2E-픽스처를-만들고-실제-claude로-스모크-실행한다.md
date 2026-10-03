---
id: AO-8
title: E2E 픽스처를 만들고 실제 claude로 스모크 실행한다
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:25'
labels:
  - skeleton
  - e2e
  - measurement
  - docs-sync
milestone: m-0
dependencies:
  - AO-7
references:
  - decision-3
  - decision-5
documentation:
  - ../coralstay-software-factory/README.md
  - ../coralstay-software-factory/에이전트_레일_파이프라인.html
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - scripts/make-fixture.sh
  - scripts/smoke.sh
priority: medium
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
스크래치에 대상 저장소(git/backlog init + git-logbook·claude-rails 설정 + 함수 1개짜리 태스크 1개 + Gradle 테스트)를
만드는 스크립트와, 그 픽스처에 대해 실제 `claude`로 `run`을 돌려 결과를 검증하는 스모크 절차.

## 맥락
마일스톤 완료 조건 (2)의 증거. 실측으로만 답할 수 있는 가설을 여기서 확인한다: 헤드리스 세션의 git-logbook 트레일러
실측 여부(D5), 회수 후 잔존 프로세스 없음(D4), `claude` 프로세스 peak RSS(D9 — Semaphore 상한 근거).
결과는 팩토리 README "출처와 검증 상태" 표와 RFC 09절에 기록한다.

## 대상 파일
- `scripts/make-fixture.sh`
- `scripts/smoke.sh` (픽스처 생성 → run → 검증 항목 출력)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `make-fixture.sh`: 인자로 받은 경로에 git·backlog init, git-logbook 설치, 함수 1개짜리 태스크와 실패하는 테스트를 가진 Gradle 대상 저장소를 만든다
- [ ] #2 `smoke.sh`: 픽스처에서 `run`을 실행하고 (a) worktree·브랜치 생성 (b) 종료 후 잔존 claude 프로세스 없음 (c) 커밋 트레일러 존재 (d) runs.jsonl 합계 = 트레일러 합계를 각각 PASS/FAIL로 출력한다
- [ ] #3 `/usr/bin/time -l`로 측정한 claude peak RSS와 트레일러 실측/unavailable 결과를 doc-1과 팩토리 README 검증 상태 표·RFC 09절에 기록했다
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
1. 픽스처: `Calculator.add` 하나를 구현하면 통과하는 테스트, backlog 태스크 1개(AC 1개 = 함수 1개).
2. 픽스처 저장소에 `gitformat.taskPrefix`를 픽스처 backlog prefix에 맞춘다.
3. smoke: 실제 claude 실행은 비용이 드므로 수동 실행 스크립트로 두고 CI/게이트에는 넣지 않는다.
4. 결과 4항목 PASS/FAIL 출력 + peak RSS 수집.
5. 팩토리 문서 반영, doc-1 D4·D5·D9 마무리.

의존: AO-7 — promote가 draft ID 의존성을 갱신하지 않아(backlog 1.51.0 실측) promote 후 AO ID로 재설정함.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
이 태스크는 실제 API 토큰을 쓴다 — 실행 전 유저 확인. claude-rails 훅은 전역(~/.claude/settings.json)이라 픽스처에서도 적용되며, 픽스처 태스크가 In Progress 전환·브랜치 규칙을 따라야 커밋이 통과한다.
<!-- SECTION:NOTES:END -->
