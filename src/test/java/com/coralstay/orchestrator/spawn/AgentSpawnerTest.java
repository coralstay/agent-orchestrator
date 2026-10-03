package com.coralstay.orchestrator.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class AgentSpawnerTest {

    // --- command ---

    @Test
    void commandBuildsHeadlessInvocationWithPermissionMode() {
        var spawner = new AgentSpawner("/opt/claude", "acceptEdits");

        assertEquals(
                List.of("/opt/claude", "-p", "do it", "--permission-mode", "acceptEdits",
                        "--permission-prompts", "none"),
                spawner.command("implementer", "do it"));
    }

    @Test
    void commandDefaultsToClaudeWithAutoModeAndNeverBare() {
        List<String> command = new AgentSpawner().command("implementer", "x");

        assertEquals("claude", command.getFirst());
        assertEquals("auto", command.get(command.indexOf("--permission-mode") + 1));
        assertFalse(command.contains("--bare"), "bare는 훅을 끈다 (RFC 11절①)");
        assertFalse(command.contains("--dangerously-skip-permissions"));
    }

    @Test
    void commandPassesPromptAsSingleArgumentVerbatim() {
        String prompt = "line1\n\"quoted\" $HOME `id`; rm -rf / && echo";

        List<String> command = new AgentSpawner().command("implementer", prompt);

        assertEquals(prompt, command.get(command.indexOf("-p") + 1));
        assertEquals(7, command.size());
    }

    @Test
    void constructorRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new AgentSpawner(" ", "auto"));
        assertThrows(IllegalArgumentException.class, () -> new AgentSpawner("claude", ""));
    }
}
