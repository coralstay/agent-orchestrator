---
id: AO-32
title: JaCoCo를 도입하고 커버리지 100%(line + branch)를 검증으로 건다
status: In Progress
assignee: []
created_date: '2026-10-30T16:50:00Z'
updated_date: '2026-10-30T17:14:00Z'
labels:
  - build
  - test
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
claude-code-agile-hooks의 pre_push_coverage_check는 Java 프로젝트에 coverageCommand(커버리지 100%, 리포트 build/reports/jacoco/)를 요구한다. 이 저장소에는 커버리지 도구가 없어 Claude 세션에서 어떤 브랜치도 push할 수 없다(2026-10-31, 로컬 main 6커밋 대기). 유저 결정: 긴급, JaCoCo + 100%, 메서드가 전부 pending 스텁인 클래스 파일만 측정에서 뺀다(스텁 파일 안의 실제 코드는 테스트로 덮는다).

## 비즈니스 규칙
- B1 (AC #2) 스텁을 구현하는 카드는 그 클래스의 JaCoCo 제외 항목을 함께 지운다 — 제외는 '아직 구현 없음'의 표시이지 커버리지 면제가 아니다.
- B2 (AC #1) 검증 규칙은 LINE과 BRANCH 각각 COVEREDRATIO minimum 1.0이고, 미달이면 빌드가 실패한다.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 build.gradle.kts에 jacoco 플러그인(Java 25 지원 toolVersion 명시), jacocoTestReport(XML + HTML, test 뒤), jacocoTestCoverageVerification(LINE · BRANCH COVEREDRATIO 1.0)을 두고 check가 검증에 의존한다
- [x] #2 메서드가 전부 pending 스텁인 클래스 파일만 측정에서 빼고, 항목마다 구현 draft ID를 주석으로 단다
- [x] #3 측정 대상 전부 LINE · BRANCH 100%를 테스트로 채운다
- [x] #4 프로젝트 설정 coverageCommand가 ./gradlew test jacocoTestReport jacocoTestCoverageVerification이고 push 커버리지 게이트를 통과한다 (수동)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 검증 명령 통과 로그와 LINE · BRANCH 수치를 final summary에 남긴다 (수동)
- [x] #2 파일 하나에 커밋 하나, push는 PR로 (수동)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Subjective
유저(2026-10-31): 긴급 반영. agent-orchestrator에 JaCoCo를 달고 커버리지 100%를 건다. draft 검토 없이 바로 승격 · 진행. 스텁 처리는 '전부 스텁인 클래스 파일만 제외'.

### Objective
- build.gradle.kts에 커버리지 플러그인 없음(이력에도 jacoco 없음). Gradle 9.4.1, Java 25 toolchain, tasks.test는 pending 태그 제외.
- 동작 클래스: Main · BacklogClient · SessionRegistry · AgentSpawner · WorktreeManager. 스텁(pending: DRAFT-n): TaskCardSource(DRAFT-1) · BacklogCardSource(DRAFT-2) · AgentRunner/ClaudeCodeRunner(DRAFT-3) · ModelPolicy(AO-25/DRAFT-4).
- hooks README 'Java (JaCoCo, Gradle)': 리포트 build/reports/jacoco/, jacocoTestCoverageVerification LINE · BRANCH COVEREDRATIO 1.0, 명령 ./gradlew test jacocoTestReport jacocoTestCoverageVerification.

### Assessment
게이트는 종료 코드와 리포트 생성만 보므로 100%는 JaCoCo 검증 규칙으로 강제한다. 제외는 클래스 파일 단위로, 메서드가 전부 스텁인 바깥 클래스만 — 중첩 레코드 등 실제 로직은 측정 대상으로 남긴다. toolVersion은 Java 25 클래스 파일을 읽는 버전으로 명시한다.

### Plan
1. AC1: 플러그인 · 리포트 · 검증 규칙(B2) → 실행해 현재 수치 측정.
2. AC2: 측정 결과로 제외 목록 확정(B1 주석).
3. AC3: 미달 클래스마다 테스트 먼저(ID#3 표식) → 100%.
4. AC4: 유저가 coverageCommand 추가 → push 게이트 통과 확인.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## 구현 노트 (AO-32)

### AC #1 — JaCoCo 플러그인 · 리포트 · 검증 (B2)
- build.gradle.kts: jacoco 플러그인, toolVersion 0.8.16, jacocoTestReport(XML + HTML, dependsOn test), jacocoTestCoverageVerification(LINE · BRANCH COVEREDRATIO minimum 1.0, failOnViolation 기본값 true), check → 검증 의존. pendingTest · test의 excludeTags("pending")는 그대로.
- 버전: Maven Central 최신이 0.8.16(2026-10-26 확인). Java 25 클래스 파일 정식 지원은 0.8.14부터라 테스트가 0.8.14 이상을 요구한다. 실제 Gradle 9.4.1 + Java 25 toolchain에서 리포트 · 검증이 돈다.
- 커밋: 568d51e(빌드), 4a237c4(테스트 CoverageBuildConfigTest: AO-32#1 · AO-32#B2)

### AC #2 — pending 스텁 클래스 파일만 제외 (B1)
- build.gradle.kts의 pendingStubClasses 7개, 리포트 · 검증 둘 다 measuredClasses 한 필터를 쓴다:
  - card/TaskCardSource.class — DRAFT-1 (default validate만 있음)
  - card/TaskCardSource$Card.class — DRAFT-1 (withTokenBudget 스텁)
  - card/TaskCardSource$TokenBudget.class — DRAFT-1 (parse · render · with*)
  - card/TaskCardSource$Tokens.class — DRAFT-1 (parse · toString)
  - card/BacklogCardSource.class — DRAFT-2 (read · record* · processRunner; 생성자는 필드 대입뿐)
  - agent/AgentRunner$ClaudeCodeRunner.class — DRAFT-3 (command · parseSession · start; 생성자는 필드 대입뿐)
  - policy/ModelPolicy.class — DRAFT-4 (AO-25로 AllocationPolicy가 대체)
- 측정에 남긴 것: TaskCardSource$Attempt · $Actual(검증 로직) · $CardCheck, AgentRunner(기본 run은 실제 위임) · $Tokens · $Session · $AgentRun, ModelPolicy$Choice · $Source · $PolicyFormatException.
- 테스트(CoverageBuildConfigTest): AO-32#2 — 항목마다 소스에서 그 타입의 메서드(생성자 제외)가 전부 throw new UnsupportedOperationException("pending: DRAFT-n")인지 확인(구현되면 실패해 항목을 지우게 함), 두 태스크가 같은 필터를 쓰는지 확인. AO-32#B1 — 항목 주석의 draft ID가 스텁의 pending ID와 같은지 확인.
- 커밋: a9374ff(빌드), da78bdf(테스트)

### AC #3 — 측정 대상 LINE · BRANCH 100%
- 테스트 파일(전부 AO-32#3): MainCoverageTest(bad8373), AgentRunnerValuesTest(078852d), ModelPolicyValuesTest(1bb108b), CardValuesTest(8971d71), SessionRegistryArgsTest(2f90626), WorktreeManagerArgsTest(79382ac), BacklogClientEdgeTest(646ea12), AgentSpawnerEdgeTest(cec865a).
- 테스트 가능하게 바꾼 소스:
  - BacklogClient: stderr Future 대기를 package-private awaitStderr로 분리(3e1a27d), 동작 동일.
  - AgentSpawner: 대기 스레드 처리를 settle(result, process, Callable)로 분리, awaitExit를 CompletableFuture.allOf 한 번의 대기로, SpawnedAgent 생성자 · drain을 package-private으로(92c230f). settle은 검사 예외도 실패로 완료한다.
  - Main: System.exit은 돌아오지 않아 그 줄이 JaCoCo에서 영영 미커버로 남는다. 비정상 종료를 별도 non-daemon 스레드의 System.exit(code)로 맡기고 main은 정상 반환(cba8597). 종료코드는 자식 JVM(-javaagent 전달)으로 확인한다.
- 수치(./gradlew clean test jacocoTestReport jacocoTestCoverageVerification, exit 0, 2회 연속): LINE covered 358 / missed 0, BRANCH covered 135 / missed 0.

### AC #4 — coverageCommand
- .claude-code-agile-hooks.json은 유저가 설정(e588479), 수정하지 않음. 테스트 AO-32#4(CoverageBuildConfigTest, 0f58ea6)가 값 ./gradlew test jacocoTestReport jacocoTestCoverageVerification을 확인. push 게이트 통과는 수동 확인 대상.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
agent-orchestrator에 JaCoCo 0.8.16(Java 25 지원)을 달고 LINE · BRANCH 100%를 빌드 검증으로 걸었다(B2). check가 jacocoTestCoverageVerification에 의존한다.

- 제외(B1, 항목마다 draft ID 주석, AO-32#2 · #B1 테스트가 '전부 pending 스텁'과 ID 일치를 소스에서 확인): TaskCardSource · $Card · $TokenBudget · $Tokens(DRAFT-1), BacklogCardSource(DRAFT-2), AgentRunner$ClaudeCodeRunner(DRAFT-3), ModelPolicy(DRAFT-4/AO-25). 실제 로직이 있는 레코드 · 기본 run은 측정에 남겨 덮었다.
- 테스트 파일 8개(헤더 → AC/DoD → Coverage). 테스트를 위한 소스 정리: BacklogClient.awaitStderr, AgentSpawner.settle(동작 동일 — 대기 코드의 검사 예외는 InterruptedException뿐), Main 비정상 종료를 별도 스레드의 System.exit으로(종료 코드 0 · 2는 자식 JVM 테스트로 확인).
- coverageCommand = ./gradlew test jacocoTestReport jacocoTestCoverageVerification(유저 설정, AO-32#4 테스트가 확인).

검증(메인 세션 재실행): ./gradlew clean test jacocoTestReport jacocoTestCoverageVerification exit 0, jacocoTestReport.xml LINE 358/0 · BRANCH 135/0.
검토 남김: Main 종료 방식은 커버리지 때문에 바꾼 모양이라 유저 판단 대상.
<!-- SECTION:FINAL_SUMMARY:END -->
