plugins {
    application
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
