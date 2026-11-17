// ---
// title: RunCommandDefinitionOfDoneTest
// description: AO-33 완료 조건(DoD) 중 저장소 안에서 확인할 수 있는 부분을 빌드 설정과 테스트 소스로 검사하는 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-17'
// updated_date: '2026-11-17'
// task: AO-33
// ---
// AO-33: DoD 네 항목 중 빌드 설정 · 테스트 소스로 확인할 수 있는 부분을 검사한다(로그 확인 · 실제 커밋은 사람이 본다).
package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Gradle은 프로젝트 디렉토리를 작업 디렉토리로 테스트를 돌린다. */
class RunCommandDefinitionOfDoneTest {

    static final String TASK = "AO-33";
    static final int CRITERIA = 6;
    static final int RULES = 5;
    static final Pattern DISPLAY_NAME = Pattern.compile("@DisplayName\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)");

    /** src/test/java 아래 모든 @DisplayName 문자열. */
    static List<String> displayNames() {  // ---- 1
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src", "test", "java"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = DISPLAY_NAME.matcher(Files.readString(file));
                while (m.find()) {
                    names.add(m.group(1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return names;
    }

    /** name에 든 이 태스크의 표식(접미 n · Bk · Dn). 뒤에 숫자가 더 붙은 것(#10 대 #1)은 따로 센다. */
    static Set<String> markers(String name) {
        Set<String> found = new TreeSet<>();
        Matcher m = Pattern.compile("(?<![A-Za-z0-9-])" + TASK + "#([BD]?\\d+)").matcher(name);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    // == AC/DoD ==

    @Test
    @DisplayName("AO-33#D1 새 클래스(RunCommand · Main · AgentSpawner)는 check의 line+branch 100% 게이트에서 빠지지 않는다")
    void newClassesAreMeasuredByCoverageGate() throws IOException {  // ---- 2
        String build = Files.readString(Path.of("build.gradle.kts"));
        String stubs = build.substring(build.indexOf("val pendingStubClasses"), build.indexOf("val measuredClasses"));

        for (String cls : List.of("RunCommand", "Main", "spawn/AgentSpawner")) {
            assertFalse(stubs.contains("orchestrator/" + cls + "."), cls + "는 측정 대상이어야 한다");
            assertFalse(stubs.contains("orchestrator/" + cls + "$"), cls + "의 내부 클래스도 측정 대상이어야 한다");
        }
        assertTrue(build.replaceAll("\\s+", " ")
                .contains("tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }"), "check가 게이트를 돈다");
    }

    @Test
    @DisplayName("AO-33#D2 AC #1~#6 · 규칙 B1~B5 표식이 모두 테스트 @DisplayName에 있다")
    void everyCriterionAndRuleHasDisplayNameMarker() {
        Set<String> found = new TreeSet<>();
        displayNames().forEach(name -> found.addAll(markers(name)));

        for (int n = 1; n <= CRITERIA; n++) {
            assertTrue(found.contains(String.valueOf(n)), TASK + " AC " + n + " 표식이 없다: " + found);
        }
        for (int k = 1; k <= RULES; k++) {
            assertTrue(found.contains("B" + k), TASK + " 규칙 B" + k + " 표식이 없다: " + found);
        }
    }

    @Test
    @DisplayName("AO-33#D3 installDist가 ~/githubs/bin/agent-orchestrator 링크 대상(build/install/agent-orchestrator/bin/agent-orchestrator)을 만들도록 설정돼 있다")
    void installDistProducesLinkedLauncher() throws IOException {
        String settings = Files.readString(Path.of("settings.gradle.kts"));
        String build = Files.readString(Path.of("build.gradle.kts"));

        assertTrue(settings.contains("rootProject.name = \"agent-orchestrator\""), settings);
        assertTrue(build.contains("application"), "application 플러그인이 installDist를 준다");
        assertTrue(build.contains("mainClass = \"" + Main.class.getName() + "\""), "실행 스크립트가 Main을 띄운다");
    }

    @Test
    @DisplayName("AO-33#D4 (구조 검사) 테스트 하나가 AC 둘 이상을 섞지 않아 AC마다 테스트 · 구현을 따로 커밋할 수 있다")
    void testsDoNotMixCriteriaSoEachCanBeCommittedAlone() {
        for (String name : displayNames()) {
            long criteria = markers(name).stream().filter(m -> Character.isDigit(m.charAt(0))).count();
            assertTrue(criteria <= 1, "AC 표식이 둘 이상인 테스트: " + name);
        }
        assertEquals(CRITERIA, displayNames().stream()
                .flatMap(name -> markers(name).stream())
                .filter(m -> Character.isDigit(m.charAt(0)))
                .distinct()
                .count());
    }

    // == Coverage ==
}

// ======== 노트 ========
// 1 표식은 @DisplayName에서 찾는다
//   설명: 표식 문자열은 이 파일 안에서 조립해 쓴다 — 리터럴로 쓰면 이 테스트 자체가 표식을 채워 버린다. 뒤에 숫자가 더 붙은 표식(#10)은 #1로 세지 않는다.
//   관련: claude-code-agile-hooks require_test_first
// 2 DoD를 저장소 안에서 확인할 수 있는 만큼만
//   설명: D1은 게이트에서 빠지지 않음, D2는 표식 존재, D3은 installDist 결과 경로를 결정하는 설정을 본다. D4(AC마다 커밋)는 커밋 이력이 테스트 시점에 확정되지 않아 구조 검사(테스트가 AC를 섞지 않음)로만 둔다. 실제 로그 · 커밋은 사람이 확인한다.
//   관련: AO-33 DoD
