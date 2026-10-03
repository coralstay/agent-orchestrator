package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.coralstay.orchestrator.Main.Command;
import com.coralstay.orchestrator.Main.UsageError;
import org.junit.jupiter.api.Test;

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
}
