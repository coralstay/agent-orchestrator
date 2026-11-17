// ---
// title: MainCoverageTest
// description: Main의 도움말·null 인자·main 종료 경로 커버리지 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-17'
// ---
// AO-32: Main의 도움말 · 빈 인자 · 프로세스 종료코드를 덮는다(커버리지 100%).
// AO-33: main이 run <taskId>를 현재 디렉토리 대상 RunCommand로 연결하는지 별도 JVM으로 확인한다.
package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MainCoverageTest {

    record Exit(int code, String stdout, String stderr) {}

    /**
     * Main을 별도 JVM으로 띄워 실제 종료코드를 본다(System.exit은 테스트 JVM에서 부를 수 없다). 테스트 JVM의
     * {@code -javaagent}(JaCoCo, append=true)를 그대로 넘겨 자식 JVM의 실행도 커버리지에 합친다.
     */
    static Exit runMain(String... args) throws IOException, InterruptedException {
        return runMain(null, Map.of(), args);
    }

    /**
     * JaCoCo 에이전트 인자의 {@code destfile}이 상대 경로면 테스트 JVM 기준 절대 경로로 바꾼다. 자식 JVM을 다른 작업
     * 디렉토리에서 띄우면 상대 경로는 그 디렉토리 아래로 풀려 커버리지가 합쳐지지 않는다.
     */
    static String absoluteDestfile(String agentArg) {  // ---- 1
        return Pattern.compile("(destfile=)([^,]+)").matcher(agentArg).replaceFirst(m ->
                Matcher.quoteReplacement(m.group(1) + Path.of(m.group(2)).toAbsolutePath()));
    }

    /** dir(null이면 현재 디렉토리)에서 env를 더해 Main을 띄운다. */
    static Exit runMain(Path dir, Map<String, String> env, String... args)  // ---- 2
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(arg -> arg.startsWith("-javaagent:"))  // ---- 3
                .map(MainCoverageTest::absoluteDestfile)
                .forEach(command::add);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        command.addAll(List.of(args));
        var builder = new ProcessBuilder(command);
        if (dir != null) {
            builder.directory(dir.toFile());
        }
        builder.environment().putAll(env);
        Process process = builder.start();
        process.getOutputStream().close();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Main이 30초 안에 끝나야 한다");  // ---- 4
        return new Exit(process.exitValue(), stdout, stderr);
    }

    // == AC/DoD ==

    @ParameterizedTest
    @ValueSource(strings = {"-h", "--help", "help"})
    @DisplayName("AO-32#3 -h · --help · help는 usage를 stdout에 찍고 0을 돌려준다")
    void helpPrintsUsageAndSucceeds(String flag) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Main.run(new String[] {flag}, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        assertEquals(0, code);
        String stdout = out.toString(StandardCharsets.UTF_8);
        assertTrue(stdout.startsWith(Main.USAGE), stdout);
        assertTrue(stdout.contains("-h, --help"), stdout);
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("AO-32#3 인자 배열이 null이어도 예외 없이 UsageError")
    void nullArgsIsUsageError() {
        assertInstanceOf(Main.UsageError.class, Main.parse(null));
    }

    @Test
    @DisplayName("AO-32#3 main: 정상 종료(도움말)는 프로세스를 0으로 끝낸다")
    void mainExitsZeroOnSuccess() throws Exception {
        Exit exit = runMain("--help");

        assertEquals(0, exit.code(), exit.stderr());
        assertTrue(exit.stdout().startsWith(Main.USAGE), exit.stdout());
    }

    @Test
    @DisplayName("AO-33#5 main: run <taskId>는 현재 디렉토리를 대상 저장소로 RunCommand를 돌린다 — git 저장소가 아니면 스폰 없이 exit 3")
    void mainRunsRunCommandInWorkingDirectory(@TempDir Path dir) throws Exception {  // ---- 5
        Exit exit = runMain(dir, Map.of(), "run", "NOPE-1");

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, exit.code(), exit.stdout() + exit.stderr());
        assertTrue(exit.stderr().contains("실행 불가"), exit.stderr());
        assertFalse(Files.exists(dir.resolveSibling(dir.getFileName() + "-wt")), "워크트리를 만들면 안 된다");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORCHESTRATOR_MAX_BUDGET_USD", "ORCHESTRATOR_TIMEOUT_MINUTES"})
    @DisplayName("AO-33#5 main: 비용 상한 · 타임아웃 환경 변수가 잘못되면 실행하지 않고 exit 2")
    void mainRejectsInvalidEnvironment(String name, @TempDir Path dir) throws Exception {
        Exit exit = runMain(dir, Map.of(name, "abc"), "run", "NOPE-1");

        assertEquals(Main.EXIT_USAGE, exit.code(), exit.stdout() + exit.stderr());
        assertTrue(exit.stderr().contains(name), exit.stderr());
    }

    @Test
    @DisplayName("AO-32#3 main: usage 오류는 프로세스를 EXIT_USAGE로 끝낸다")
    void mainExitsWithUsageCodeOnError() throws Exception {
        Exit exit = runMain("deploy");

        assertEquals(Main.EXIT_USAGE, exit.code());
        assertTrue(exit.stderr().contains(Main.USAGE), exit.stderr());
    }

    // == Coverage ==
}

// ======== 노트 ========
// 1 destfile은 절대 경로로
//   설명: 자식 JVM을 임시 디렉토리에서 띄우면 에이전트 인자의 상대 destfile이 그 디렉토리 아래로 풀려 커버리지가 사라진다. 테스트 JVM 기준 절대 경로로 바꿔 넘긴다.
//   관련: AO-33, AO-32
//   개념: JaCoCo 에이전트
// 2 Main을 별도 JVM으로 띄운다
//   설명: System.exit은 테스트 JVM에서 부를 수 없어 자식 JVM의 실제 종료코드를 본다.
//   관련: Main.java
//   개념: 종료 코드
// 3 JaCoCo 에이전트를 자식 JVM에도 넘긴다
//   설명: 테스트 JVM의 -javaagent(JaCoCo, append=true)를 그대로 넘겨 자식 JVM의 실행도 커버리지에 합친다. 안 넘기면 main의 종료 분기가 100% 게이트에서 빠진다.
//   관련: AO-32, build.gradle.kts
//   개념: JaCoCo 에이전트
// 4 30초 안에 끝나야 한다
//   설명: 자식 JVM이 종료 스레드 때문에 멈추지 않는지 확인하는 안전 장치다.
//   관련: Main.java
// 5 실제 RunCommand 연결은 임시 디렉토리에서
//   설명: 자식 JVM의 작업 디렉토리를 git 저장소가 아닌 임시 디렉토리로 두어, 테스트를 어느 브랜치에서 돌리든 실제 claude를 띄우지 않고 HEAD 확인에서 exit 3으로 끝나게 한다. 환경 변수 오류는 실행 전에 exit 2다.
//   관련: AO-33, RunCommand.forRepo
