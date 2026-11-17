// ---
// title: MainTest
// description: Main.parse의 run <taskId> 파싱과 usage 오류 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-17'
// ---
package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.Main.Command;
import com.coralstay.orchestrator.Main.UsageError;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MainTest {

    @Test
    @DisplayName("AO-1#1 run <taskId>를 파싱해 taskId를 꺼낸다")
    void parsesRunWithTaskId() {
        var result = Main.parse(new String[] {"run", "AO-1"});  // ---- 1
        assertEquals(new Command("run", "AO-1"), result);
    }

    @Test
    @DisplayName("AO-1 보조: run 뒤 taskId가 없으면 UsageError")
    void runWithoutTaskIdIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"run"}));
    }

    @Test
    @DisplayName("AO-1 보조: run 뒤 인자가 둘 이상이면 UsageError")
    void runWithExtraArgsIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"run", "AO-1", "AO-2"}));
    }

    @Test
    @DisplayName("AO-1 보조: 공백 taskId는 UsageError")
    void blankTaskIdIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"run", "  "}));
    }

    @Test
    @DisplayName("AO-1#2 인자가 없으면 UsageError")
    void noArgsIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {}));
    }

    @Test
    @DisplayName("AO-1#2 알 수 없는 명령이면 UsageError")
    void unknownCommandIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"watch"}));
    }

    @ParameterizedTest(name = "{displayName} [{index}] args=\"{0}\"")
    @DisplayName("AO-1#2 잘못된 인자면 usage를 stderr에 출력하고 exit 2")
    @ValueSource(strings = {"", "watch AO-1", "run"})  // ---- 2
    void badArgsPrintUsageToStderrAndExit2(String line) {
        String[] args = line.isEmpty() ? new String[0] : line.split(" ");
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Main.run(args, print(out), print(err));

        assertEquals(2, code);
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains(Main.USAGE));
    }

    @ParameterizedTest(name = "{displayName} [{index}] exit={0}")
    @DisplayName("AO-33#5 run <taskId>는 RunCommand 실행기에 taskId를 넘기고 그 종료 코드를 그대로 돌려준다")
    @ValueSource(ints = {0, 1, 3})
    void runDelegatesToTaskRunner(int exit) {  // ---- 3
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        List<String> seen = new ArrayList<>();

        int code = Main.run(new String[] {"run", " AO-1 "}, print(out), print(err), (taskId, o, e) -> {
            seen.add(taskId);
            o.println("ran " + taskId);
            return exit;
        });

        assertEquals(exit, code);
        assertEquals(List.of("AO-1"), seen);
        assertEquals("ran AO-1", out.toString(StandardCharsets.UTF_8).strip());
    }

    @ParameterizedTest(name = "{displayName} [{index}] args=\"{0}\"")
    @DisplayName("AO-33#5 usage 오류는 실행기를 부르지 않고 그대로 exit 2")
    @ValueSource(strings = {"", "watch AO-1", "run", "run AO-1 AO-2"})
    void usageErrorNeverRunsTask(String line) {
        String[] args = line.isEmpty() ? new String[0] : line.split(" ");
        var err = new ByteArrayOutputStream();

        int code = Main.run(args, print(new ByteArrayOutputStream()), print(err), (taskId, o, e) -> {
            throw new AssertionError("실행기를 부르면 안 된다: " + taskId);
        });

        assertEquals(Main.EXIT_USAGE, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains(Main.USAGE));
    }

    private static PrintStream print(ByteArrayOutputStream sink) {
        return new PrintStream(sink, true, StandardCharsets.UTF_8);
    }
}

// ======== 노트 ========
// 1 AC #1: run <taskId> 파싱
//   설명: 진입점의 기본 계약: run 뒤 taskId 하나를 Command로 읽는다.
//   관련: AO-1#1
// 2 AC #2: 잘못된 인자는 stderr + exit 2
//   설명: usage 오류는 stdout에 아무것도 쓰지 않고 stderr에 usage를 쓰며 종료 코드 2다.
//   관련: AO-1#2
// 3 run은 RunCommand로 연결된다
//   설명: 테스트는 실행기를 가짜로 넘겨 taskId 전달과 종료 코드(0 · 1 · 3) 전파만 본다. 실제 RunCommand 연결은 MainCoverageTest가 별도 JVM으로 확인한다.
//   관련: AO-33, RunCommand
