---
id: AO-6
title: 태스크 브랜치의 git-logbook 트레일러를 읽는 CommitLedger를 만든다
status: To Do
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:25'
labels:
  - skeleton
  - ledger
  - git-logbook
  - docs-sync
milestone: m-0
dependencies:
  - AO-1
references:
  - decision-5
documentation:
  - ../coralstay-software-factory/에이전트_레일_파이프라인.html
  - ../coralstay-software-factory/README.md
  - ../coralstay-software-factory/diagrams/07-task-sequence.mmd
  - ../git-format/README.md
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/ledger/CommitLedger.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
태스크 브랜치에서 base 이후 커밋들을 읽어 git-logbook 트레일러(`Task-Id`/`Tokens-Used`/`Tool-Calls`/`AI-Model`)를
파싱하고, 토큰·도구 호출 합계와 `unavailable (사유)` 커밋 수를 그대로 돌려주는 판독기.

## 맥락
decision-5: 토큰 측정은 git-logbook(구 git-format)이 한다. 오케스트레이터는 측정하지 않고 커밋 트레일러를 읽기만
하며, `unavailable`이면 재측정·추정하지 않는다. RFC 06·07절의 "Logger는 값을 커밋 자체에서 읽는다"와 같은 방향이다.
커밋 수 자체는 AC 수와 비교해 자가 치유 재시도 배수의 첫 표본이 된다(T7에서 기록). 팩토리 문서의 git-format 명칭과
측정 주체 서술을 정정한다(doc-1 D5).

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/ledger/CommitLedger.java`
- `src/test/java/com/coralstay/orchestrator/ledger/CommitLedgerTest.java` (임시 저장소에 트레일러 커밋을 직접 만들어 검증)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `read(branch, base)`: `git log base..branch`를 트레일러 포함 포맷으로 실행해 커밋 해시 목록을 얻는다
- [ ] #2 `parseTrailers(raw)`: Task-Id·Tokens-Used·Tool-Calls·AI-Model을 추출하고 `unavailable (사유)` 값은 숫자가 아닌 사유로 보존한다
- [ ] #3 `summarize(commits)`: 커밋 수, 토큰·도구 호출 합계(실측 커밋만), unavailable 커밋 수와 사유별 개수를 돌려준다
- [ ] #4 팩토리 문서 D5 반영: RFC·README·`07-task-sequence.mmd`의 git-format 명칭을 git-logbook으로 바꾸고 토큰 측정 주체를 트레일러로 명시했다
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
1. git-logbook README "각 필드" 표에서 트레일러 키·사유 슬러그 목록 확인(no-session-id, transcript-not-found, transcript-unreadable, transcript-parse-failed, no-usage-channel).
2. 테스트: @TempDir 저장소에 `git commit --trailer`로 실측/unavailable/트레일러 없음 커밋을 섞어 만든다.
3. 포맷: `%H%x1f%(trailers:key=Tokens-Used,valueonly,separator=%x1e)...%x1d` 형태로 구분자 충돌 없이 파싱.
4. summarize 합계 테스트(사람 커밋은 트레일러 없음 → 합계 제외, 커밋 수에는 포함).
5. 팩토리 D5 반영 — 다이어그램 수정 시 build-diagrams/build-pdf.

의존: AO-1 — promote가 draft ID 의존성을 갱신하지 않아(backlog 1.51.0 실측) promote 후 AO ID로 재설정함.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Tokens-Used는 '직전 커밋 이후 세션 델타'라 커밋 간 합산이 곧 태스크 총량의 근사다 — 이 한계는 git-logbook이 스스로 밝히고 있으므로 runs.jsonl에도 근사치임을 필드명으로 드러낸다. 헤드리스 세션에서 트레일러가 실측되는지는 T8에서 확인.
<!-- SECTION:NOTES:END -->
