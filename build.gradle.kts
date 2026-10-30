plugins {
    application
    jacoco
}

group = "com.coralstay"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "com.coralstay.orchestrator.Main"
}

tasks.test {
    useJUnitPlatform {
        excludeTags("pending")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// decision-11: 아직 구현이 없는 @Tag("pending") 테스트만 따로 돌린다.
val pendingTest by tasks.registering(Test::class) {
    description = "Runs only tests tagged 'pending' (not yet implemented)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("pending")
    }
    failOnNoDiscoveredTests = false
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// AO-32: JaCoCo 커버리지. 0.8.14가 Java 25 클래스 파일을 정식 지원한 첫 릴리스이고, 0.8.16은 그 뒤 최신이다.
jacoco {
    toolVersion = "0.8.16"
}

// AO-32 B1: 메서드가 전부 pending 스텁인 클래스 파일만 측정에서 뺀다. 주석은 그 스텁을 구현할 카드다 —
// 구현 카드는 자기 항목을 지운다(제외는 '아직 구현 없음'의 표시이지 커버리지 면제가 아니다).
val pendingStubClasses = listOf<String>(
    "com/coralstay/orchestrator/card/TaskCardSource.class", // DRAFT-1 validate
    "com/coralstay/orchestrator/card/TaskCardSource\$Card.class", // DRAFT-1 withTokenBudget
    "com/coralstay/orchestrator/card/TaskCardSource\$TokenBudget.class", // DRAFT-1 parse · render · with*
    "com/coralstay/orchestrator/card/TaskCardSource\$Tokens.class", // DRAFT-1 parse · toString
    "com/coralstay/orchestrator/card/BacklogCardSource.class", // DRAFT-2 read · record* · processRunner
    "com/coralstay/orchestrator/agent/AgentRunner\$ClaudeCodeRunner.class", // DRAFT-3 command · parseSession · start
    "com/coralstay/orchestrator/policy/ModelPolicy.class", // DRAFT-4 AllocationPolicy로 대체(AO-25)
)

// 리포트와 검증이 같은 대상을 재도록 한 곳에서 거른다.
val measuredClasses = files(sourceSets.main.map { main ->
    main.output.classesDirs.asFileTree.matching { exclude(pendingStubClasses) }
})

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(measuredClasses)
    reports {
        xml.required = true
        html.required = true
    }
}

// AO-32 B2: LINE · BRANCH 각각 COVEREDRATIO 1.0 미달이면 빌드가 실패한다.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(measuredClasses)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
