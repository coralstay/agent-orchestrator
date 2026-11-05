---
id: AO-1
title: Gradle 골격과 CLI 진입점(Main)을 만든다
status: Done
assignee:
  - '@claude'
created_date: '2026-10-03 03:18'
updated_date: '2026-10-03 03:27'
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
- [x] #1 `main`: `run <taskId>` 인자를 파싱해 taskId를 꺼낸다 (`parse` 함수, 단위 테스트로 확인)
- [x] #2 `main`: 인자가 없거나 알 수 없는 명령이면 usage를 stderr에 출력하고 exit 2로 끝난다
- [x] #3 Gradle wrapper + Java 25 toolchain + JUnit 5 설정으로 `./gradlew test`가 통과하고 `.claude-rails.json`에 testCommand가 설정돼 있다
- [x] #4 팩토리 문서 D1 반영: 아키텍처.md 3절과 README 그림의 "Java 22+"를 Java 25로 정정했다
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

실행 기록 (2026-10-03):
- 288c637 Gradle 9.4.1 wrapper + Kotlin DSL, toolchain 25, JUnit 5(BOM 5.13.4, 테스트 전용), application mainClass, .claude-rails.json testCommand
- 9771578 Main.parse → sealed ParseResult(Command | UsageError) 순수 함수 + 단위 테스트
- 2ee116a Main.run(args,out,err)이 exit code 반환, main만 System.exit. usage는 stderr, exit 2 (installDist 바이너리로 실제 exit 2 확인)
- 팩토리 8931057 (docs/orchestrator-skeleton): 아키텍처.md 3절·README 그림 Java 22+ → Java 25, 박스 폭 74칸 유지. 8절 청사진에는 버전 표기 없음
- 8266542 doc-1 D1 ✅ 8931057
- 증거: ./gradlew test --rerun-tasks → MainTest 10건 PASSED, BUILD SUCCESSFUL
- 참고: gradlew.bat CRLF 경고(.gitattributes 없음) — 범위 밖이라 미처리
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Java 25 Gradle(Kotlin DSL) 골격과 wrapper, JUnit 5 테스트 게이트(.claude-rails.json)를 만들고, Main.parse(순수 함수)로 run <taskId>를 파싱해 taskId를 출력하며 잘못된 인자는 usage를 stderr로 내고 exit 2로 끝내게 했다. 팩토리 docs/orchestrator-skeleton 브랜치(8931057)에 Java 25 정정을 반영하고 doc-1 D1을 ✅로 갱신했다. ./gradlew test 10건 통과.
<!-- SECTION:FINAL_SUMMARY:END -->
