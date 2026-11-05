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
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    /** 미병합 task 브랜치가 없는 저장소. */
    static final Predicate<String> NO_BRANCH = id -> false;

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

        assertEquals(expected, BacklogClient.isRunnable(task, board::get, NO_BRANCH));
    }

    @DisplayName("AO-2 보조: 의존 없는 To Do는 lookup 없이 runnable")
    @Test
    void toDoWithoutDependenciesIsRunnableWithoutLookup() {
        Task task = task("AO-9", "To Do", List.of());

        assertTrue(BacklogClient.isRunnable(task, id -> {
            throw new AssertionError("lookup should not be called");
        }, NO_BRANCH));
    }

    @DisplayName("AO-2#3 isRunnable: 실출력 픽스처의 In Progress·미완 의존 To Do는 false")
    @Test
    void realFixturesInProgressAndBlockedToDoAreNotRunnable() {
        Map<String, Task> board = Map.of(
                "AO-1", BacklogClient.parse(fixture("ao-1-plain.txt")),
                "AO-2", BacklogClient.parse(fixture("ao-2-plain.txt")));

        // AO-2: 의존 AO-1은 Done이지만 In Progress(이미 착수) → false
        assertFalse(BacklogClient.isRunnable(board.get("AO-2"), board::get, NO_BRANCH));
        // AO-7: To Do지만 AO-2가 미완 → 첫 미완 의존에서 false
        assertFalse(BacklogClient.isRunnable(
                BacklogClient.parse(fixture("ao-7-plain.txt")),
                id -> board.getOrDefault(id, task(id, "To Do", List.of())),
                NO_BRANCH));
    }

    // --- AO-30#3 isRunnable: 미병합 task/<ID> 브랜치가 있으면 배정하지 않는다(decision-14, ORC-005) ---

    @DisplayName("AO-30#3 AO-30#B1 isRunnable: To Do · 의존 Done이어도 미병합 task/<ID> 브랜치가 있으면 false")
    @Test
    void notRunnableWhenUnmergedTaskBranchExists() {
        Task task = task("AO-9", "To Do", List.of("AO-1"));
        Map<String, Task> board = Map.of("AO-1", task("AO-1", "Done", List.of()));
        List<String> asked = new ArrayList<>();

        assertFalse(BacklogClient.isRunnable(task, board::get, id -> asked.add(id)));
        assertEquals(List.of("AO-9"), asked);
        assertTrue(BacklogClient.isRunnable(task, board::get, NO_BRANCH));
    }

    /** {@code git for-each-ref} · {@code git cherry} 출력을 돌려주는 가짜 실행기. 호출을 기록한다. */
    static Function<List<String>, ProcessResult> git(List<List<String>> calls, String refs, Map<String, String> cherry) {
        return command -> {
            calls.add(command);
            if (command.size() >= 2 && command.get(1).equals("for-each-ref")) {
                return new ProcessResult(0, refs, "");
            }
            if (command.size() == 4 && command.get(1).equals("cherry")) {
                return new ProcessResult(0, cherry.getOrDefault(command.get(3), ""), "");
            }
            throw new AssertionError("unexpected command: " + command);
        };
    }

    @DisplayName("AO-30#3 AO-30#B1 hasUnmergedTaskBranch: 로컬 task/<ID>에 main에 없는 커밋(+)이 있으면 true")
    @Test
    void localBranchWithCommitsNotInMainIsUnmerged() {
        List<List<String>> calls = new ArrayList<>();
        var client = new BacklogClient(git(calls, "refs/heads/task/AO-9\n",
                Map.of("refs/heads/task/AO-9", "- 1111111\n+ 2222222\n")));

        assertTrue(client.hasUnmergedTaskBranch("AO-9"));
        assertEquals(
                List.of("git", "for-each-ref", "--format=%(refname)", "refs/heads/task/AO-9", "refs/remotes"),
                calls.getFirst());
        assertEquals(List.of("git", "cherry", "main", "refs/heads/task/AO-9"), calls.get(1));
    }

    @DisplayName("AO-30#3 AO-30#B1 hasUnmergedTaskBranch: 원격에만 있는 미병합 task/<ID>도 true")
    @Test
    void remoteOnlyBranchWithCommitsNotInMainIsUnmerged() {
        List<List<String>> calls = new ArrayList<>();
        var client = new BacklogClient(git(calls,
                "refs/remotes/origin/HEAD\nrefs/remotes/origin/main\nrefs/remotes/origin/task/AO-9\n"
                        + "refs/remotes/origin/task/AO-90\n",
                Map.of("refs/remotes/origin/task/AO-9", "+ 2222222\n")));

        assertTrue(client.hasUnmergedTaskBranch("AO-9"));
        assertEquals(List.of("git", "cherry", "main", "refs/remotes/origin/task/AO-9"), calls.getLast(),
                "다른 ID(AO-90) 브랜치는 보지 않는다");
    }

    @DisplayName("AO-30#3 AO-30#B1 hasUnmergedTaskBranch: 커밋이 전부 main에 있으면(- 또는 없음) 병합된 것으로 false")
    @Test
    void branchWhoseCommitsAreAllInMainIsMerged() {
        List<List<String>> calls = new ArrayList<>();
        var client = new BacklogClient(git(calls,
                "refs/heads/task/AO-9\nrefs/remotes/origin/task/AO-9\n",
                Map.of("refs/heads/task/AO-9", "- 1111111\n- 2222222\n", "refs/remotes/origin/task/AO-9", "")));

        assertFalse(client.hasUnmergedTaskBranch("AO-9"));
        assertEquals(3, calls.size(), "로컬 · 원격 브랜치를 모두 git cherry로 본다: " + calls);
    }

    @DisplayName("AO-30#3 AO-30#B1 hasUnmergedTaskBranch: task/<ID> 브랜치가 없으면 false")
    @Test
    void noTaskBranchIsNotStarted() {
        List<List<String>> calls = new ArrayList<>();
        var client = new BacklogClient(git(calls, "refs/remotes/origin/main\n", Map.of()));

        assertFalse(client.hasUnmergedTaskBranch("AO-9"));
        assertEquals(1, calls.size(), "브랜치가 없으면 git cherry를 부르지 않는다");
    }

    @DisplayName("AO-30#3 hasUnmergedTaskBranch: git이 실패하면 stderr를 담아 예외를 던진다")
    @Test
    void gitFailureThrows() {
        var client = new BacklogClient(command -> new ProcessResult(128, "", "fatal: not a git repository\n"));

        var e = assertThrows(IllegalStateException.class, () -> client.hasUnmergedTaskBranch("AO-9"));

        assertTrue(e.getMessage().contains("not a git repository"), e.getMessage());
    }

    @DisplayName("AO-30#3 AO-30#B1 hasUnmergedTaskBranch: 실제 git — 로컬 · 원격 미병합은 true, 병합 · cherry-pick 뒤는 false")
    @Test
    void realGitDetectsUnmergedLocalAndRemoteBranches(@TempDir Path tmp) throws IOException {
        Path root = tmp.toRealPath();
        Path hooks = Files.createDirectory(root.resolve("no-hooks"));
        Path origin = Files.createDirectory(root.resolve("origin"));
        initRepo(origin, hooks);
        commitFile(origin, "README", "hello\n");
        runGit(root, "clone", "-q", origin.toString(), "app");
        Path app = root.resolve("app");
        initRepo(app, hooks);
        var client = new BacklogClient(BacklogClient.processExecutor(app));

        assertFalse(client.hasUnmergedTaskBranch("AO-9"), "브랜치 없음");

        // 원격에만 있는 미병합 브랜치
        runGit(origin, "switch", "-q", "-c", "task/AO-9");
        commitFile(origin, "work.txt", "remote work\n");
        runGit(origin, "switch", "-q", "main");
        runGit(app, "fetch", "-q", "origin");
        assertTrue(client.hasUnmergedTaskBranch("AO-9"), "원격 미병합");

        // 원격 브랜치를 main에 병합하면 미병합이 아니다
        runGit(app, "merge", "-q", "--ff-only", "origin/task/AO-9");
        assertFalse(client.hasUnmergedTaskBranch("AO-9"), "원격 병합 뒤");

        // 로컬 미병합 브랜치, 그다음 cherry-pick(같은 패치)으로 main에 반영
        runGit(app, "switch", "-q", "-c", "task/AO-9");
        commitFile(app, "local.txt", "local work\n");
        runGit(app, "switch", "-q", "main");
        assertTrue(client.hasUnmergedTaskBranch("AO-9"), "로컬 미병합");
        runGit(app, "cherry-pick", "task/AO-9");
        assertFalse(client.hasUnmergedTaskBranch("AO-9"), "git cherry는 같은 패치를 병합된 것으로 본다");
    }

    private static void initRepo(Path dir, Path hooks) {
        if (!Files.isDirectory(dir.resolve(".git"))) {
            runGit(dir, "init", "-q", "-b", "main");
        }
        runGit(dir, "config", "core.hooksPath", hooks.toString());
        runGit(dir, "config", "user.name", "test");
        runGit(dir, "config", "user.email", "test@example.com");
        runGit(dir, "config", "commit.gpgsign", "false");
    }

    private static void commitFile(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content);
        runGit(dir, "add", name);
        runGit(dir, "commit", "-q", "-m", name);
    }

    private static String runGit(Path dir, String... args) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        ProcessResult result = BacklogClient.processExecutor(dir).apply(command);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(command + " failed(" + result.exitCode() + "): " + result.stderr());
        }
        return result.stdout();
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
