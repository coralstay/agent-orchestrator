---
id: AO-32
title: JaCoCo를 도입하고 커버리지 100%(line + branch)를 검증으로 건다
status: In Progress
assignee: []
created_date: '2026-10-30T16:50:00Z'
updated_date: '2026-10-30T16:52:00Z'
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
- [ ] #1 build.gradle.kts에 jacoco 플러그인(Java 25 지원 toolVersion 명시), jacocoTestReport(XML + HTML, test 뒤), jacocoTestCoverageVerification(LINE · BRANCH COVEREDRATIO 1.0)을 두고 check가 검증에 의존한다
- [ ] #2 메서드가 전부 pending 스텁인 클래스 파일만 측정에서 빼고, 항목마다 구현 draft ID를 주석으로 단다
- [ ] #3 측정 대상 전부 LINE · BRANCH 100%를 테스트로 채운다
- [ ] #4 프로젝트 설정 coverageCommand가 ./gradlew test jacocoTestReport jacocoTestCoverageVerification이고 push 커버리지 게이트를 통과한다 (수동)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 검증 명령 통과 로그와 LINE · BRANCH 수치를 final summary에 남긴다 (수동)
- [ ] #2 파일 하나에 커밋 하나, push는 PR로 (수동)
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
