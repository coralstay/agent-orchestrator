// AO-32: Main의 도움말 · 빈 인자 · 프로세스 종료코드를 덮는다(커버리지 100%).
package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MainCoverageTest {

    record Exit(int code, String stdout, String stderr) {}

    /**
     * Main을 별도 JVM으로 띄워 실제 종료코드를 본다(System.exit은 테스트 JVM에서 부를 수 없다). 테스트 JVM의
     * {@code -javaagent}(JaCoCo, append=true)를 그대로 넘겨 자식 JVM의 실행도 커버리지에 합친다.
     */
    static Exit runMain(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(arg -> arg.startsWith("-javaagent:"))
                .forEach(command::add);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();
        process.getOutputStream().close();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Main이 30초 안에 끝나야 한다");
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
    @DisplayName("AO-32#3 main: 정상 명령은 프로세스를 0으로 끝내고 taskId를 찍는다")
    void mainExitsZeroOnSuccess() throws Exception {
        Exit exit = runMain("run", "AO-1");

        assertEquals(0, exit.code(), exit.stderr());
        assertEquals("AO-1", exit.stdout().strip());
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
