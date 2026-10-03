package com.coralstay.orchestrator.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.spawn.AgentSpawner.AgentResult;
import com.coralstay.orchestrator.spawn.AgentSpawner.SpawnedAgent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** spawn 테스트는 claude 대신 @TempDir에 쓴 셸 스크립트를 실행 파일로 주입한다 — 실제 claude는 띄우지 않는다. */
class AgentSpawnerTest {

    static final Duration TIMEOUT = Duration.ofSeconds(5);

    @TempDir
    Path tmp;

    /** {@code #!/bin/sh} + body를 실행 가능 파일로 쓴다. */
    Path fake(String body) throws IOException {
        Path script = tmp.resolve("fake-claude");
        Files.writeString(script, "#!/bin/sh\n" + body + "\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        return script;
    }

    AgentSpawner spawner(Path executable) {
        return new AgentSpawner(executable.toString(), "acceptEdits");
    }

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

    // --- spawn ---

    @Test
    void spawnReturnsExitCodeOutputsAndPid() throws IOException {
        Path cwd = Files.createDirectory(tmp.resolve("work")).toRealPath();
        Path exe = fake("""
                printf '%s\\n' "$@"
                pwd
                echo oops >&2
                exit 7""");

        AgentResult result = spawner(exe).spawn("implementer", cwd, "hello world", TIMEOUT);

        assertEquals(7, result.exitCode());
        assertEquals(
                "-p\nhello world\n--permission-mode\nacceptEdits\n--permission-prompts\nnone\n" + cwd + "\n",
                result.stdout());
        assertEquals("oops\n", result.stderr());
        assertFalse(result.timedOut());
        assertTrue(result.pid() > 0);
        assertTrue(result.duration().compareTo(Duration.ZERO) > 0);
    }

    @Test
    void startExposesPidBeforeCompletion() throws IOException {
        Path exe = fake("sleep 0.3; echo done");

        SpawnedAgent agent = spawner(exe).start("implementer", tmp, "p", TIMEOUT);

        assertTrue(agent.process().isAlive(), "start 직후 프로세스는 살아 있어야 레지스트리가 추적할 수 있다");
        assertEquals("implementer", agent.role());
        AgentResult result = agent.await();
        assertEquals(agent.pid(), result.pid());
        assertEquals("done\n", result.stdout());
        assertFalse(agent.process().isAlive());
        assertEquals(result, agent.await());
    }

    @Test
    void spawnClosesStdinSoReadersSeeEof() throws IOException {
        Path exe = fake("cat; echo eof");

        AgentResult result = spawner(exe).spawn("implementer", tmp, "p", Duration.ofSeconds(2));

        assertFalse(result.timedOut(), "stdin이 열려 있으면 cat이 영원히 기다린다");
        assertEquals("eof\n", result.stdout());
    }

    @Test
    void spawnDrainsLargeOutputOnBothStreamsWithoutBlocking() throws IOException {
        // 파이프 버퍼(64KiB 안팎)를 훨씬 넘는 양을 stdout·stderr 양쪽에 쓴다
        Path exe = fake("head -c 1048576 /dev/zero | tr '\\0' a; head -c 1048576 /dev/zero | tr '\\0' b >&2");

        AgentResult result = spawner(exe).spawn("implementer", tmp, "p", TIMEOUT);

        assertFalse(result.timedOut());
        assertEquals(0, result.exitCode());
        assertEquals(1048576, result.stdout().length());
        assertEquals(1048576, result.stderr().length());
    }

    @Test
    void startRejectsNonPositiveTimeout() throws IOException {
        Path exe = fake("exit 0");

        assertThrows(IllegalArgumentException.class,
                () -> spawner(exe).start("implementer", tmp, "p", Duration.ZERO));
    }
}
