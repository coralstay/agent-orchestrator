package com.coralstay.orchestrator.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    // --- timeout ---

    @Test
    void spawnTimeoutKillsProcessAndAllDescendants() throws IOException {
        // 백그라운드 자식과 전경 자식을 하나씩 두고, 둘 다 30초 동안 버티게 한다
        Path pids = tmp.resolve("pids");
        Path exe = fake("""
                sleep 30 &
                echo $! > %s
                echo started
                sleep 30""".formatted(pids));

        AgentResult result = spawner(exe).spawn("implementer", tmp, "p", Duration.ofMillis(300));

        assertTrue(result.timedOut());
        assertNotEquals(0, result.exitCode());
        assertEquals("started\n", result.stdout(), "죽이기 전까지의 출력은 남는다");
        assertTrue(result.duration().compareTo(Duration.ofSeconds(2)) < 0, result.duration().toString());
        assertFalse(ProcessHandle.of(result.pid()).map(ProcessHandle::isAlive).orElse(false));
        long child = Long.parseLong(Files.readString(pids).strip());
        assertFalse(ProcessHandle.of(child).map(ProcessHandle::isAlive).orElse(false),
                "백그라운드 자식 sleep(pid " + child + ")이 남으면 안 된다");
    }

    @Test
    void terminateForcesProcessThatIgnoresSigterm() throws IOException {
        Path exe = fake("""
                trap '' TERM
                echo ready
                while :; do sleep 0.05; done""");

        AgentResult result = spawner(exe).spawn("implementer", tmp, "p", Duration.ofMillis(300));

        assertTrue(result.timedOut());
        assertFalse(ProcessHandle.of(result.pid()).map(ProcessHandle::isAlive).orElse(false),
                "SIGTERM을 무시해도 유예 후 SIGKILL로 끝나야 한다");
    }
}
