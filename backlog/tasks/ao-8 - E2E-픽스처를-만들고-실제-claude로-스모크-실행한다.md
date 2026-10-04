---
id: AO-8
title: E2E 픽스처로 예상 토큰부터 실제치 병기까지 실제 claude로 검증한다
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 13:10'
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
  - decision-6
  - decision-8
  - decision-9
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
실제 claude로 재설계 흐름 전체를 한 번 돌려 검증한다. 픽스처 대상 저장소를 만들고 `orchestrator run`을 실행해, 카드의 예상 토큰부터 실제치 병기까지 확인한다.

## 맥락
2026-10-03 스파이크로 헤드리스 `claude -p` 세션의 git-trail 실측(`Tokens-Used: in/out`)과 peak RSS 240~290MB(사소한 태스크)를 확인했다. 스파이크 저장소에는 backlog가 없어 claude-rails 태스크 규칙(task view 필독·In Progress·Edit/Write 차단)이 검증되지 않았다 — 이 E2E가 그 검증이다. 실제 API 토큰을 쓴다.

## 대상 파일
- `scripts/make-fixture.sh` — 대상 저장소 생성
- `scripts/smoke.sh` — run 실행과 확인 항목 PASS/FAIL 출력
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `make-fixture.sh`: git·backlog init, git-trail 설치, `.claude-rails.json`, 실패하는 테스트 1개, `### Token Budget`(estimated)을 가진 카드 1개(AC 1개 = 함수 1개)로 대상 저장소를 만든다
- [ ] #2 `smoke.sh`: run 후 (a) main 카드 claim(In Progress·@claude) (b) 워크트리·브랜치 (c) 잔존 claude 프로세스 없음 (d) AC 커밋의 git-trail 트레일러 (e) 브랜치 카드 Done + Token Budget actual 병기 (f) runs.jsonl 값 = 트레일러 합·세션 총량을 PASS/FAIL로 출력한다
- [ ] #3 헤드리스 Claude가 claude-rails 태스크 규칙을 통과하는지와 peak RSS를 기록하고 doc-1 D5·D9와 팩토리 README 검증 상태 표·RFC 09절에 반영했다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 팩토리 `docs/orchestrator-skeleton` 브랜치에 문서 반영 커밋이 있다
- [ ] #3 doc-1 해당 행을 ✅(팩토리 커밋 해시) 또는 ❌로 갱신했다
- [ ] #4 AC마다 커밋 1개 이상이며 git-trail 형식 검사를 통과했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. 픽스처: `Calculator.add` 하나를 구현하면 통과하는 테스트, 카드 1개(Token Budget estimated 포함), gitformat.taskPrefix를 픽스처 prefix에 맞춤.
2. smoke: 실제 claude 실행은 비용이 드는 수동 스크립트(게이트에 넣지 않음).
3. 실패 경로 1회: 아주 짧은 타임아웃으로 attempt 기록 확인.
4. 결과 문서 반영.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
실행 전 유저 확인(API 비용). 스파이크 기준 사소한 태스크 1회 약 $0.12~0.13. 프롬프트에 '승인·승격된 태스크' 문구가 없으면 전역 CLAUDE.md 승인 절차에서 멈출 수 있다.
<!-- SECTION:NOTES:END -->
