// AO-32: JaCoCo 커버리지 빌드 설정(build.gradle.kts)과 프로젝트 설정의 coverageCommand를 검증한다.
package com.coralstay.orchestrator.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Gradle은 프로젝트 디렉토리를 작업 디렉토리로 테스트를 돌린다. */
class CoverageBuildConfigTest {

    static final Path BUILD_FILE = Path.of("build.gradle.kts");

    static String build() throws IOException {
        return Files.readString(BUILD_FILE);
    }

    /** {@code name {} 블록의 본문(중괄호 짝 맞춤). 없으면 실패. */
    static String block(String text, String header) {
        int start = text.indexOf(header);
        assertTrue(start >= 0, "블록이 있어야 한다: " + header);
        int open = text.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return text.substring(open + 1, i);
            }
        }
        throw new AssertionError("닫히지 않은 블록: " + header);
    }

    /** {@code 0.8.14} → {@code [0, 8, 14]}. */
    static int[] version(String v) {
        String[] parts = v.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i]);
        }
        return out;
    }

    static int compare(int[] a, int[] b) {
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#1 jacoco 플러그인과 Java 25를 읽는 toolVersion(0.8.14 이상)을 명시한다")
    void jacocoPluginWithJava25ToolVersion() throws IOException {
        String build = build();
        assertTrue(block(build, "plugins").lines().anyMatch(l -> l.strip().equals("jacoco")),
                "plugins에 jacoco가 있어야 한다");

        Matcher m = Pattern.compile("toolVersion\\s*=\\s*\"([0-9.]+)\"")
                .matcher(block(build, "jacoco {"));
        assertTrue(m.find(), "jacoco { toolVersion = \"x.y.z\" }가 있어야 한다");
        // JaCoCo 0.8.14가 Java 25 클래스 파일(major 69)을 정식 지원한 첫 릴리스다.
        assertTrue(compare(version(m.group(1)), version("0.8.14")) >= 0,
                "toolVersion은 0.8.14 이상이어야 한다: " + m.group(1));
    }

    @Test
    @DisplayName("AO-32#1 jacocoTestReport는 test 뒤에 XML과 HTML을 낸다")
    void reportAfterTestWithXmlAndHtml() throws IOException {
        String report = block(build(), "tasks.jacocoTestReport");
        assertTrue(report.contains("dependsOn(tasks.test)"), "리포트는 test에 의존해야 한다");
        assertTrue(report.contains("xml.required = true"), "XML 리포트가 켜져 있어야 한다");
        assertTrue(report.contains("html.required = true"), "HTML 리포트가 켜져 있어야 한다");
    }

    @Test
    @DisplayName("AO-32#1 check가 jacocoTestCoverageVerification에 의존하고 test의 pending 제외는 그대로다")
    void checkDependsOnVerification() throws IOException {
        String build = build();
        assertTrue(block(build, "tasks.check").contains("dependsOn(tasks.jacocoTestCoverageVerification)"),
                "check가 커버리지 검증에 의존해야 한다");
        assertTrue(block(build, "tasks.jacocoTestCoverageVerification").contains("dependsOn(tasks.test)"),
                "검증은 test 뒤에 돌아야 한다");
        assertTrue(block(build, "tasks.test").contains("excludeTags(\"pending\")"),
                "test는 pending 태그를 계속 빼야 한다");
        assertTrue(build.contains("val pendingTest by tasks.registering(Test::class)"),
                "pendingTest 태스크가 남아 있어야 한다");
    }

    @Test
    @DisplayName("AO-32#B2 검증 규칙은 LINE과 BRANCH 각각 COVEREDRATIO minimum 1.0이다")
    void lineAndBranchMustBeFullyCovered() throws IOException {
        String rules = block(block(build(), "tasks.jacocoTestCoverageVerification"), "violationRules");
        List<String> limits = new ArrayList<>();
        Matcher m = Pattern.compile("limit\\s*\\{([^}]*)}").matcher(rules);
        while (m.find()) {
            String body = m.group(1).replaceAll("\\s+", " ").strip();
            limits.add(body);
        }
        assertEquals(2, limits.size(), "limit은 LINE · BRANCH 두 개여야 한다: " + limits);
        for (String counter : List.of("LINE", "BRANCH")) {
            assertTrue(limits.stream().anyMatch(l -> l.contains("counter = \"" + counter + "\"")
                            && l.contains("value = \"COVEREDRATIO\"")
                            && l.contains("minimum = \"1.0\".toBigDecimal()")),
                    counter + " COVEREDRATIO minimum 1.0 규칙이 있어야 한다: " + limits);
        }
        // 규칙 미달이 경고가 아니라 실패가 되도록 failOnViolation을 끄지 않는다(기본값 true).
        assertTrue(!rules.contains("failOnViolation = false"), "failOnViolation을 끄면 안 된다");
    }

    // == Coverage ==
}
