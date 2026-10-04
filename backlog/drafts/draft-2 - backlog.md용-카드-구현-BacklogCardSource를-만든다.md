---
id: DRAFT-2
title: backlog.md용 카드 구현 BacklogCardSource를 만든다
status: Draft
assignee:
  - '@claude'
created_date: '2026-10-03 13:09'
updated_date: '2026-10-04 01:38'
labels:
  - skeleton
  - card
  - backlog
  - docs-sync
milestone: m-0
dependencies:
  - DRAFT-1
  - AO-2
  - DRAFT-17
references:
  - decision-6
  - decision-9
  - decision-5
documentation:
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
  - ../coralstay-software-factory/에이전트_레일_파이프라인.html
  - ../coralstay-software-factory/아키텍처.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/card/BacklogCardSource.java
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
TaskCardSource를 backlog.md CLI로 구현한다. Token Budget 소절을 읽고 쓰며, claim·시도·실제치 기록을 카드 커밋으로 남긴다.

## 맥락
- backlog.md 1.51.0: 사용자 frontmatter 키는 편집 시 지워지고(#918), 라벨은 분류용이라 Description 소절을 쓴다(decision-6).
- claim은 main에서, 이후 카드 변경은 태스크 브랜치에서만(decision-9).
- 오케스트레이터 커밋은 `AI_AGENT`·`CLAUDE_CODE_SESSION_ID`를 지우고 실행해야 부모 Claude 세션 커밋으로 표시되지 않는다(스파이크로 확인).
- 사람 락은 파일 단위: 같은 대상 파일(modified_files)을 가진 다른 카드가 사람 담당 In Progress면 실행하지 않는다.

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/card/BacklogCardSource.java` (BacklogClient 재사용)
- `src/test/java/com/coralstay/orchestrator/card/BacklogCardSourceTest.java`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `read(id)`: BacklogClient로 카드를 읽고 Description의 `### Token Budget` 소절(estimated / attempt n / actual)을 파싱한다
- [ ] #2 `claim(id, agent)`: `backlog task edit -s "In Progress" -a @claude` 후 카드 파일만 커밋한다(AI 도구 환경변수 제거)
- [ ] #3 `recordAttempt`·`recordActual`: Description 전체를 읽어 Token Budget 소절만 바꿔 `-d`로 다시 쓰고 카드 커밋한다(워크트리 기준 경로)
- [ ] #4 `isLocked(card, all)`: 같은 대상 파일을 가진 카드 중 사람 담당 In Progress가 있으면 락, `@claude` 담당 In Progress는 에이전트 실행분으로 구분한다
- [ ] #5 팩토리 문서 D10 반영: 카드 Token Budget 스키마와 수명 주기를 RFC·아키텍처.md에 기록했다
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 `./gradlew test` 통과 로그를 확인했다
- [ ] #2 AC마다 커밋 1개 이상이며 git-trail 형식 검사를 통과했다
- [ ] #3 팩토리 `docs/orchestrator-skeleton` 브랜치에 문서 반영 커밋이 있다
- [ ] #4 doc-1 해당 행을 ✅(팩토리 커밋 해시) 또는 ❌로 갱신했다
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Token Budget 파서/렌더러(소절 교체가 나머지 Description을 건드리지 않는지 테스트).
2. claim/record는 실행기 주입으로 명령 인자·환경 정리를 테스트.
3. 락 판정 테스트(사람/`@claude`/다른 카드 같은 파일).
4. 팩토리 D10.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
`backlog task edit -d`는 Description 전체를 교체한다 — 소절 바깥 텍스트가 바뀌지 않음을 반드시 테스트. 카드 커밋 메시지도 git-trail 형식, 브랜치는 claim 때 main(예외 브랜치), 이후 task/<id>.
<!-- SECTION:NOTES:END -->
