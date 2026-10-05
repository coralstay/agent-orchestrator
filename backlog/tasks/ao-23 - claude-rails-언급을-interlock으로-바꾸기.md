---
id: AO-23
title: claude-rails 언급을 interlock으로 바꾸기
status: Done
assignee: []
created_date: '2026-10-05 07:49'
updated_date: '2026-10-05 08:06'
labels:
  - rename
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
claude-rails 저장소가 interlock(coralstay/interlock)으로 이름이 바뀐다. 계획: ~/.claude/plans/typed-chasing-conway.md

수용 기준(승격 시 AC로 등록):
1. .claude-rails.json을 .interlock.json으로 바꾼다 (interlock이 옛 이름도 읽으므로 순서 무관)
2. README·레퍼런스 문서의 claude-rails 이름과 링크를 interlock / coralstay/interlock으로 바꾼다. 완료 태스크·decision 기록은 그대로 둔다
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 .claude-rails.json을 .interlock.json으로 바꾼다
- [x] #2 README·레퍼런스 문서의 claude-rails 이름과 링크를 interlock / coralstay/interlock으로 바꾼다. 완료 태스크·decision 기록은 그대로
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
.claude-rails.json을 .interlock.json으로 옮겼다(git mv, 내용 testCommand: ./gradlew test 그대로; interlock은 옛 이름도 fallback으로 읽음). AgentRunner·AgentSpawner·WorktreeManager 주석의 claude-rails를 interlock으로 바꿨다(ac1a952). README에는 claude-rails 언급이 없었고 .gitignore에도 .claude-rails/ 항목이 없어 바꿀 것이 없었다. 남긴 것: backlog decision·doc-1(검증 로그)·draft·다른 태스크 기록과, 그 태스크 출력 스냅샷인 테스트 픽스처 src/test/resources/backlog/ao-1-plain.txt·ao-7-plain.txt(역사 기록이며 BacklogClientTest 입력). 검증: ./gradlew test --rerun BUILD SUCCESSFUL, 62 tests 0 failures 0 errors 0 skipped.
<!-- SECTION:FINAL_SUMMARY:END -->
