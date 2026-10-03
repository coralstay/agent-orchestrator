---
id: AO-1
title: Gradle 골격과 CLI 진입점(Main)을 만든다
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:25'
labels:
  - skeleton
  - cli
  - build
  - docs-sync
milestone: m-0
dependencies: []
references:
  - decision-1
  - decision-2
documentation:
  - ../coralstay-software-factory/아키텍처.md
  - ../coralstay-software-factory/README.md
  - backlog/docs/doc-1 - 팩토리-문서-검증-로그.md
  - backlog/docs/doc-2 - 팩토리-문서-반영-구조.md
modified_files:
  - src/main/java/com/coralstay/orchestrator/Main.java
  - build.gradle.kts
  - settings.gradle.kts
  - .claude-rails.json
priority: high
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 결과
`./gradlew run --args="run AO-1"`이 인자를 파싱해 RunCommand로 넘길 준비가 된 Java 25 Gradle 프로젝트.

## 맥락
마일스톤 M1의 첫 노드. 이후 모든 태스크가 이 골격 위에 파일 하나씩 추가된다(태스크 = 파일).
decision-1(Java 25 + Gradle, 외부 런타임 의존성 없음), decision-2(`run <taskId>` 단발).

## 대상 파일
- `src/main/java/com/coralstay/orchestrator/Main.java` (주 파일)
- `settings.gradle.kts`, `build.gradle.kts`, `gradlew`, `gradle/wrapper/*` (골격)
- `src/test/java/com/coralstay/orchestrator/MainTest.java`
- `.claude-rails.json` (`testCommand: ./gradlew test`)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `main`: `run <taskId>` 인자를 파싱해 taskId를 꺼낸다 (`parse` 함수, 단위 테스트로 확인)
- [ ] #2 `main`: 인자가 없거나 알 수 없는 명령이면 usage를 stderr에 출력하고 exit 2로 끝난다
- [ ] #3 Gradle wrapper + Java 25 toolchain + JUnit 5 설정으로 `./gradlew test`가 통과하고 `.claude-rails.json`에 testCommand가 설정돼 있다
- [ ] #4 팩토리 문서 D1 반영: 아키텍처.md 3절과 README 그림의 "Java 22+"를 Java 25로 정정했다
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
1. `gradle init` 대신 최소 `settings.gradle.kts`/`build.gradle.kts`를 직접 쓰고 `gradle wrapper`로 wrapper 생성 (toolchain 25, application 플러그인, mainClass).
2. `Main.parse(String[])` → `Command` record(이름, taskId) 또는 usage 오류 — 테스트 먼저.
3. `Main.main`은 parse 결과에 따라 exit code 결정(성공 경로는 아직 RunCommand 자리표시 없이 taskId 출력만).
4. `.claude-rails.json` 작성 후 `./gradlew test`로 게이트 확인.
5. 팩토리 docs 브랜치에서 D1 정정 → doc-1 갱신.

의존: 없음 — promote가 draft ID 의존성을 갱신하지 않아(backlog 1.51.0 실측) promote 후 AO ID로 재설정함.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
RunCommand는 AO T7에서 생긴다 — 이 태스크에서는 파싱 결과를 출력만 하고 RunCommand 호출은 T7에서 연결한다. System.exit은 main에서만 부르고 parse는 순수 함수로 둬서 테스트 가능하게 한다.
<!-- SECTION:NOTES:END -->
