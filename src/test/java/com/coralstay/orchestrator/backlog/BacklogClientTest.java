package com.coralstay.orchestrator.backlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.coralstay.orchestrator.backlog.BacklogClient.BacklogException;
import com.coralstay.orchestrator.backlog.BacklogClient.Criterion;
import com.coralstay.orchestrator.backlog.BacklogClient.ProcessResult;
import com.coralstay.orchestrator.backlog.BacklogClient.Task;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BacklogClientTest {

    // --- parse: 실제 backlog 1.51.0 `task view <id> --plain` 출력 픽스처 ---

    @Test
    void parsesTaskWithFiveDirectDependencies() {
        Task task = BacklogClient.parse(fixture("ao-7-plain.txt"));

        assertEquals("AO-7", task.id());
        assertEquals("run 흐름을 배선하고 실행 결과를 runs.jsonl에 기록한다", task.title());
        assertEquals("To Do", task.status());
        // 직접 의존만 — 트리의 전이 의존(AO-1)과 Dependents(AO-8)는 제외
        assertEquals(List.of("AO-2", "AO-3", "AO-4", "AO-5", "AO-6"), task.dependencies());
        assertEquals(5, task.acceptanceCriteria().size());
        assertEquals(
                new Criterion(1, false,
                        "`execute(taskId)`: isRunnable이 false면 스폰하지 않고 사유와 함께 exit 3으로 끝난다"),
                task.acceptanceCriteria().getFirst());
    }

    @Test
    void parsesDoneTaskWithoutDependencies() {
        Task task = BacklogClient.parse(fixture("ao-1-plain.txt"));

        assertEquals("AO-1", task.id());
        assertEquals("Gradle 골격과 CLI 진입점(Main)을 만든다", task.title());
        assertEquals("Done", task.status());
        assertEquals(List.of(), task.dependencies());
        assertEquals(List.of(1, 2, 3, 4),
                task.acceptanceCriteria().stream().map(Criterion::index).toList());
        assertTrue(task.acceptanceCriteria().stream().allMatch(Criterion::checked));
    }

    @Test
    void parsesInProgressTaskWithOneDependency() {
        Task task = BacklogClient.parse(fixture("ao-2-plain.txt"));

        assertEquals("AO-2", task.id());
        assertEquals("In Progress", task.status());
        assertEquals(List.of("AO-1"), task.dependencies());
        assertEquals(4, task.acceptanceCriteria().size());
        assertFalse(task.acceptanceCriteria().get(3).checked());
    }

    @Test
    void definitionOfDoneIsNotParsedAsAcceptanceCriteria() {
        Task task = BacklogClient.parse(fixture("ao-1-plain.txt"));

        assertTrue(task.acceptanceCriteria().stream()
                .noneMatch(c -> c.text().contains("통과 로그를 확인했다")));
    }

    // --- view: 실행기 주입 ---

    @Test
    void viewRunsBacklogTaskViewPlainAndParsesStdout() {
        List<List<String>> calls = new ArrayList<>();
        var client = new BacklogClient(command -> {
            calls.add(command);
            return new ProcessResult(0, fixture("ao-7-plain.txt"), "");
        });

        Task task = client.view("AO-7");

        assertEquals(List.of(List.of("backlog", "task", "view", "AO-7", "--plain")), calls);
        assertEquals("AO-7", task.id());
    }

    @Test
    void viewThrowsWithStderrOnNonZeroExit() {
        var client = new BacklogClient(command -> new ProcessResult(1, "", "Task AO-999 not found.\n"));

        var e = assertThrows(BacklogException.class, () -> client.view("AO-999"));

        assertEquals(1, e.exitCode());
        assertEquals("Task AO-999 not found.\n", e.stderr());
        assertTrue(e.getMessage().contains("Task AO-999 not found."));
    }

    // --- 실제 backlog CLI (이 저장소의 backlog/, PATH에 backlog가 없으면 건너뜀) ---

    @Test
    void realCliViewsDoneTask() {
        var client = new BacklogClient(BacklogClient.processExecutor(Path.of("").toAbsolutePath()));
        assumeTrue(backlogOnPath(), "backlog CLI not on PATH");

        Task task = client.view("AO-1");

        assertEquals("AO-1", task.id());
        assertEquals("Done", task.status());
        assertEquals(List.of(), task.dependencies());
    }

    @Test
    void realCliMissingTaskExitsNonZeroWithStderr() {
        var client = new BacklogClient(BacklogClient.processExecutor(Path.of("").toAbsolutePath()));
        assumeTrue(backlogOnPath(), "backlog CLI not on PATH");

        var e = assertThrows(BacklogException.class, () -> client.view("AO-999999"));

        assertEquals(1, e.exitCode());
        assertTrue(e.stderr().contains("not found"), e.stderr());
    }

    private static boolean backlogOnPath() {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (Files.isExecutable(Path.of(dir, "backlog"))) {
                return true;
            }
        }
        return false;
    }

    static String fixture(String name) {
        try (InputStream in = BacklogClientTest.class.getResourceAsStream("/backlog/" + name)) {
            if (in == null) {
                throw new IllegalStateException("fixture not found: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
