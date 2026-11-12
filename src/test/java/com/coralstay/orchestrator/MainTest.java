// ---
// title: MainTest
// description: Main.parse의 run <taskId> 파싱과 usage 오류 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-06'
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

    @Test
    @DisplayName("AO-1 보조: run은 taskId를 stdout에 출력하고 exit 0")
    void runPrintsTaskIdAndExits0() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int code = Main.run(new String[] {"run", "AO-1"}, print(out), print(err));

        assertEquals(0, code);
        assertEquals("AO-1", out.toString(StandardCharsets.UTF_8).strip());
        assertEquals("", err.toString(StandardCharsets.UTF_8));
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
