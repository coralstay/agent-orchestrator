package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.Main.Command;
import com.coralstay.orchestrator.Main.UsageError;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MainTest {

    @Test
    void parsesRunWithTaskId() {
        var result = Main.parse(new String[] {"run", "AO-1"});
        assertEquals(new Command("run", "AO-1"), result);
    }

    @Test
    void runWithoutTaskIdIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"run"}));
    }

    @Test
    void runWithExtraArgsIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"run", "AO-1", "AO-2"}));
    }

    @Test
    void blankTaskIdIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"run", "  "}));
    }

    @Test
    void noArgsIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {}));
    }

    @Test
    void unknownCommandIsUsageError() {
        assertInstanceOf(UsageError.class, Main.parse(new String[] {"watch"}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "watch AO-1", "run"})
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
