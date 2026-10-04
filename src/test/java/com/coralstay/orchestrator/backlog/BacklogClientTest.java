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
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BacklogClientTest {

    // --- parse: 실제 backlog 1.51.0 `task view <id> --plain` 출력 픽스처 ---

    @DisplayName("AO-2#2 parse: 직접 의존 5개인 To Do 태스크를 파싱한다")
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

    @DisplayName("AO-2#2 parse: 의존 없는 Done 태스크와 체크된 AC를 파싱한다")
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

    @DisplayName("AO-2#2 parse: 의존 1개인 In Progress 태스크를 파싱한다")
    @Test
    void parsesInProgressTaskWithOneDependency() {
        Task task = BacklogClient.parse(fixture("ao-2-plain.txt"));

        assertEquals("AO-2", task.id());
        assertEquals("In Progress", task.status());
        assertEquals(List.of("AO-1"), task.dependencies());
        assertEquals(4, task.acceptanceCriteria().size());
        assertFalse(task.acceptanceCriteria().get(3).checked());
    }

    @DisplayName("AO-2 보조: DoD 항목은 AC로 파싱하지 않는다")
    @Test
    void definitionOfDoneIsNotParsedAsAcceptanceCriteria() {
        Task task = BacklogClient.parse(fixture("ao-1-plain.txt"));

        assertTrue(task.acceptanceCriteria().stream()
                .noneMatch(c -> c.text().contains("통과 로그를 확인했다")));
    }

    // --- view: 실행기 주입 ---

    @DisplayName("AO-2#1 view: backlog task view <id> --plain을 실행하고 stdout을 파싱한다")
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

    @DisplayName("AO-2#1 view: 종료코드≠0이면 stderr를 담은 예외를 던진다")
    @Test
    void viewThrowsWithStderrOnNonZeroExit() {
        var client = new BacklogClient(command -> new ProcessResult(1, "", "Task AO-999 not found.\n"));

        var e = assertThrows(BacklogException.class, () -> client.view("AO-999"));

        assertEquals(1, e.exitCode());
        assertEquals("Task AO-999 not found.\n", e.stderr());
        assertTrue(e.getMessage().contains("Task AO-999 not found."));
    }

    // --- isRunnable: 상태 × 의존 완료 여부 ---

    @DisplayName("AO-2#3 isRunnable: To Do이고 의존이 모두 Done일 때만 true")
    @ParameterizedTest(name = "{0}, deps={1},{2} -> {3}")
    @CsvSource({
        "To Do,       Done,        Done,        true",
        "To Do,       Done,        To Do,       false",
        "To Do,       Done,        In Progress, false",
        "In Progress, Done,        Done,        false",
        "In Progress, To Do,       Done,        false",
        "Done,        Done,        Done,        false",
        "Done,        To Do,       To Do,       false",
    })
    void runnableOnlyWhenToDoAndAllDependenciesDone(
            String status, String dep1Status, String dep2Status, boolean expected) {
        Task task = task("AO-9", status, List.of("AO-1", "AO-2"));
        Map<String, Task> board = Map.of(
                "AO-1", task("AO-1", dep1Status, List.of()),
                "AO-2", task("AO-2", dep2Status, List.of()));

        assertEquals(expected, BacklogClient.isRunnable(task, board::get));
    }

    @DisplayName("AO-2 보조: 의존 없는 To Do는 lookup 없이 runnable")
    @Test
    void toDoWithoutDependenciesIsRunnableWithoutLookup() {
        Task task = task("AO-9", "To Do", List.of());

        assertTrue(BacklogClient.isRunnable(task, id -> {
            throw new AssertionError("lookup should not be called");
        }));
    }

    @DisplayName("AO-2#3 isRunnable: 실출력 픽스처의 In Progress·미완 의존 To Do는 false")
    @Test
    void realFixturesInProgressAndBlockedToDoAreNotRunnable() {
        Map<String, Task> board = Map.of(
                "AO-1", BacklogClient.parse(fixture("ao-1-plain.txt")),
                "AO-2", BacklogClient.parse(fixture("ao-2-plain.txt")));

        // AO-2: 의존 AO-1은 Done이지만 In Progress(사람 선점) → false
        assertFalse(BacklogClient.isRunnable(board.get("AO-2"), board::get));
        // AO-7: To Do지만 AO-2가 미완 → 첫 미완 의존에서 false
        assertFalse(BacklogClient.isRunnable(
                BacklogClient.parse(fixture("ao-7-plain.txt")),
                id -> board.getOrDefault(id, task(id, "To Do", List.of()))));
    }

    private static Task task(String id, String status, List<String> dependencies) {
        return new Task(id, id + " title", status, List.of(), dependencies);
    }

    // --- 실제 backlog CLI (이 저장소의 backlog/, PATH에 backlog가 없으면 건너뜀) ---

    @DisplayName("AO-2#1 view: 실제 backlog CLI로 Done 태스크(AO-1)를 조회한다")
    @Test
    void realCliViewsDoneTask() {
        var client = new BacklogClient(BacklogClient.processExecutor(Path.of("").toAbsolutePath()));
        assumeTrue(backlogOnPath(), "backlog CLI not on PATH");

        Task task = client.view("AO-1");

        assertEquals("AO-1", task.id());
        assertEquals("Done", task.status());
        assertEquals(List.of(), task.dependencies());
    }

    @DisplayName("AO-2#1 view: 실제 backlog CLI에서 없는 태스크는 비0 종료와 stderr")
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
