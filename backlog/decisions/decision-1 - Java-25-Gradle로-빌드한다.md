---
id: decision-1
title: Java 25 + Gradle로 빌드한다
date: '2026-10-03 03:15'
status: accepted
---
## Context

coralstay-software-factory RFC 06절은 오케스트레이터를 "Java 25(JVM), virtual thread + Semaphore"로 확정했지만,
아키텍처.md 3절·README 그림은 "단일 JVM 프로세스 (Java 22+)"로 적혀 있어 서로 어긋난다(doc-1 D1).
이 머신에는 Temurin 25.0.2 LTS가 설치돼 있다. 대상 프로젝트 게이트(아키텍처.md 7절)는 `gradlew test`의 Exit Code를 전제한다.

## Decision

- 언어·런타임: **Java 25** (virtual thread, `ProcessBuilder`, `Semaphore`만으로 동시성 처리).
- 빌드: **Gradle (Kotlin DSL) + wrapper**, 테스트는 JUnit 5. 외부 런타임 의존성은 두지 않는다(CLI 파서도 직접 구현).
- `.claude-rails.json`의 `testCommand`는 `./gradlew test`.

## Alternatives

- Maven: 선언적이고 단순하지만, 대상 프로젝트 게이트가 `gradlew test`를 전제하므로 같은 도구로 맞추는 쪽이 검증 경로가 하나로 줄어든다.
- Java 21 LTS: virtual thread는 쓸 수 있으나 RFC가 25를 명시했고 설치본도 25다.

## Consequences

- 팩토리 아키텍처.md·README의 "Java 22+"를 25로 정정한다(AO T1의 문서 반영 AC).
- picocli 같은 라이브러리 없이 인자 파싱을 직접 한다 — 스켈레톤 범위(`run <taskId>` 한 개)에서는 충분하다.
