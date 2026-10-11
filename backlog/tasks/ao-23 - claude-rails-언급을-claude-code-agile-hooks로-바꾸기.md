---
id: AO-23
title: claude-rails 언급을 claude-code-agile-hooks로 바꾸기
status: Done
assignee: []
created_date: '2026-10-05 07:49'
updated_date: '2026-10-05 11:42'
labels:
  - rename
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
claude-rails 저장소의 이름이 claude-code-agile-hooks(coralstay/claude-code-agile-hooks)로 바뀐다. 계획: ~/.claude/plans/typed-chasing-conway.md

수용 기준(승격 시 AC로 등록):
1. .claude-rails.json을 .claude-code-agile-hooks.json으로 바꾼다 (claude-code-agile-hooks가 이 파일을 먼저 읽고 옛 이름으로 fallback하므로 순서 무관)
2. README·레퍼런스 문서·주석의 claude-rails 이름과 링크를 claude-code-agile-hooks / coralstay/claude-code-agile-hooks로 바꾼다. 완료 태스크·decision 기록은 그대로 둔다
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 .claude-rails.json을 .claude-code-agile-hooks.json으로 바꾼다
- [x] #2 README·레퍼런스 문서·주석의 claude-rails 이름과 링크를 claude-code-agile-hooks / coralstay/claude-code-agile-hooks로 바꾼다. 완료 태스크·decision 기록은 그대로
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
claude-rails를 claude-code-agile-hooks(coralstay/claude-code-agile-hooks)로 바꿨다. 배포된 적 없는 중간 이름을 거쳤으나 그 흔적은 모두 지웠다. .claude-rails.json을 .claude-code-agile-hooks.json으로 옮겼다(git mv, 내용 testCommand: ./gradlew test 그대로; claude-code-agile-hooks는 이 파일을 먼저 읽고 없으면 옛 .claude-rails.json으로 fallback). AgentRunner·AgentSpawner·WorktreeManager 주석의 claude-rails를 claude-code-agile-hooks로 바꿨다(ac1a952, 36f4593). README에는 claude-rails 언급이 없었고 .gitignore에도 로그 디렉토리 항목이 없어 바꿀 것이 없었다. 남긴 것: backlog decision·doc-1(검증 로그)·draft·다른 태스크 기록과, 그 태스크 출력 스냅샷인 테스트 픽스처 src/test/resources/backlog/ao-1-plain.txt·ao-7-plain.txt(역사 기록이며 BacklogClientTest 입력). 검증: ./gradlew test --rerun BUILD SUCCESSFUL, 62 tests 0 failures 0 errors 0 skipped.
<!-- SECTION:FINAL_SUMMARY:END -->
