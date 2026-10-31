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


    static final Pattern EXCLUDE_ENTRY =
            Pattern.compile("^\\s*\"([^\"]+\\.class)\",\\s*//\\s*(DRAFT-\\d+)\\b.*$", Pattern.MULTILINE);
    static final Pattern PENDING_STUB =
            Pattern.compile("throw new UnsupportedOperationException\\(\"pending: (DRAFT-\\d+)\"\\);");
    static final Pattern NESTED_TYPE = Pattern.compile("\\b(class|interface|record|enum)\\s+\\w+");
    static final Pattern METHOD_HEADER =
            Pattern.compile("(\\w+)\\s*\\([^()]*\\)\\s*(throws\\s+[\\w.,\\s]+)?$");

    /** 제외 항목: 클래스 파일 경로(Ant 패턴, {@code $}는 이스케이프를 푼 값)와 주석의 draft ID. */
    record Exclusion(String classFile, String draftId) {

        /** {@code com/x/A$B.class} → {@code src/main/java/com/x/A.java}. */
        Path sourceFile() {
            String outer = classFile.replaceFirst("\\$.*", "").replaceFirst("\\.class$", "");
            return Path.of("src/main/java", outer + ".java");
        }

        /** {@code com/x/A$B.class} → {@code B}. */
        String simpleName() {
            String name = classFile.replaceFirst("\\.class$", "");
            return name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('$')) + 1);
        }
    }

    static List<Exclusion> exclusions() throws IOException {
        Matcher list = Pattern.compile("val pendingStubClasses = listOf<String>\\((.*?)\n\\)", Pattern.DOTALL)
                .matcher(build());
        assertTrue(list.find(), "pendingStubClasses 목록이 있어야 한다");
        List<Exclusion> out = new ArrayList<>();
        Matcher m = EXCLUDE_ENTRY.matcher(list.group(1).replace("\\$", "$"));
        while (m.find()) {
            out.add(new Exclusion(m.group(1), m.group(2)));
        }
        long entries = list.group(1).lines().filter(l -> l.strip().startsWith("\"")).count();
        assertEquals(entries, out.size(), "모든 항목이 '\"경로\", // DRAFT-n' 형식이어야 한다");
        return out;
    }

    /** 주석을 지운 소스. 문자열 · 문자 리터럴은 그대로 둔다. */
    static String withoutComments(String src) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < src.length()) {
            char c = src.charAt(i);
            if (c == '"' || c == '\'') {
                int end = literalEnd(src, i);
                out.append(src, i, end);
                i = end;
            } else if (src.startsWith("//", i)) {
                i = src.indexOf('\n', i);
                i = i < 0 ? src.length() : i;
            } else if (src.startsWith("/*", i)) {
                i = src.indexOf("*/", i + 2) + 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** {@code start}의 따옴표로 시작하는 리터럴 바로 뒤 위치. */
    static int literalEnd(String src, int start) {
        char quote = src.charAt(start);
        int i = start + 1;
        while (src.charAt(i) != quote) {
            i += src.charAt(i) == '\\' ? 2 : 1;
        }
        return i + 1;
    }

    /** {@code open} 위치의 여는 중괄호와 짝인 닫는 중괄호 위치. 리터럴 안의 중괄호는 세지 않는다. */
    static int matchingBrace(String src, int open) {
        int depth = 0;
        int i = open;
        while (true) {
            char c = src.charAt(i);
            if (c == '"' || c == '\'') {
                i = literalEnd(src, i);
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
            i++;
        }
    }

    /** 타입 {@code name}의 본문(주석 제거 후). */
    static String typeBody(String src, String name) {
        Matcher m = Pattern.compile("\\b(class|interface|record|enum)\\s+" + name + "\\b").matcher(src);
        assertTrue(m.find(), "타입 선언이 있어야 한다: " + name);
        int open = src.indexOf('{', m.end());
        return src.substring(open + 1, matchingBrace(src, open));
    }

    /** 본문 바로 아래(중첩 타입 제외)의 코드 블록: 헤더 → 블록 내용. */
    static List<String[]> memberBlocks(String body) {
        List<String[]> out = new ArrayList<>();
        int segment = 0;
        int i = 0;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == '"' || c == '\'') {
                i = literalEnd(body, i);
            } else if (c == ';') {
                segment = ++i;
            } else if (c == '{') {
                int close = matchingBrace(body, i);
                String header = body.substring(segment, i).strip();
                if (!NESTED_TYPE.matcher(header).find()) {
                    out.add(new String[] {header, body.substring(i + 1, close).strip()});
                }
                segment = i = close + 1;
            } else {
                i++;
            }
        }
        return out;
    }

    /** 제외된 클래스의 메서드(생성자 제외) 본문 각각에서 찾은 pending draft ID. 스텁이 아니면 실패. */
    static List<String> stubDraftIds(Exclusion exclusion) throws IOException {
        String body = typeBody(withoutComments(Files.readString(exclusion.sourceFile())), exclusion.simpleName());
        List<String> ids = new ArrayList<>();
        for (String[] block : memberBlocks(body)) {
            Matcher header = METHOD_HEADER.matcher(block[0]);
            assertTrue(header.find(), exclusion.classFile() + ": 메서드 · 생성자가 아닌 코드 블록: " + block[0]);
            if (header.group(1).equals(exclusion.simpleName())) {
                continue; // 생성자는 필드 대입뿐이라 스텁 판정에서 뺀다.
            }
            Matcher stub = PENDING_STUB.matcher(block[1]);
            assertTrue(stub.matches(), exclusion.classFile() + "." + header.group(1)
                    + "이 pending 스텁이 아니다 — 구현했으면 제외 항목을 지운다(AO-32 B1): " + block[1]);
            ids.add(stub.group(1));
        }
        assertTrue(!ids.isEmpty(), exclusion.classFile() + ": 스텁 메서드가 하나도 없다");
        return ids;
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

    @Test
    @DisplayName("AO-32#2 제외 항목은 메서드가 전부 pending 스텁인 클래스 파일뿐이고 리포트 · 검증이 같은 필터를 쓴다")
    void excludesOnlyAllStubClassFiles() throws IOException {
        List<Exclusion> exclusions = exclusions();
        assertTrue(!exclusions.isEmpty(), "제외 항목이 있어야 한다");
        for (Exclusion exclusion : exclusions) {
            stubDraftIds(exclusion);
        }
        String build = build();
        assertTrue(block(build, "tasks.jacocoTestReport").contains("classDirectories.setFrom(measuredClasses)"));
        assertTrue(block(build, "tasks.jacocoTestCoverageVerification")
                .contains("classDirectories.setFrom(measuredClasses)"));
        assertTrue(build.contains("exclude(pendingStubClasses)"), "measuredClasses가 제외 목록으로 걸러야 한다");
    }

    @Test
    @DisplayName("AO-32#B1 항목마다 그 스텁을 구현할 draft ID를 주석으로 단다(스텁의 pending ID와 같다)")
    void eachExclusionNamesTheImplementingDraft() throws IOException {
        List<Exclusion> exclusions = exclusions();
        assertTrue(!exclusions.isEmpty(), "제외 항목이 있어야 한다");
        for (Exclusion exclusion : exclusions) {
            for (String id : stubDraftIds(exclusion)) {
                assertEquals(exclusion.draftId(), id,
                        exclusion.classFile() + "의 주석 draft ID가 스텁의 pending ID와 달라 구현 카드가 지울 항목을 찾지 못한다");
            }
        }
    }

    @Test
    @DisplayName("AO-32#4 프로젝트 설정 coverageCommand가 테스트 · 리포트 · 검증을 차례로 돈다")
    void coverageCommandRunsTestReportAndVerification() throws IOException {
        String config = Files.readString(Path.of(".claude-code-agile-hooks.json"));
        Matcher m = Pattern.compile("\"coverageCommand\"\\s*:\\s*\"([^\"]*)\"").matcher(config);
        assertTrue(m.find(), "coverageCommand가 있어야 한다");
        assertEquals("./gradlew test jacocoTestReport jacocoTestCoverageVerification", m.group(1));
    }

    // == Coverage ==
}
