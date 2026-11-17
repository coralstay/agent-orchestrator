// ---
// title: RunCommandTest
// description: 임시 git 저장소 · 가짜 backlog · 가짜 claude 스크립트로 RunCommand의 실행 흐름을 검증하는 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-17'
// updated_date: '2026-11-17'
// task: AO-33
// ---
// AO-33: run <taskId> 최소 흐름(실행 가능 판정 → 워크트리 → admit → 스폰 → retire → 브랜치 상태 판정)을 검증한다.
package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.backlog.BacklogClient;
import com.coralstay.orchestrator.backlog.BacklogClient.ProcessResult;
import com.coralstay.orchestrator.registry.SessionRegistry;
import com.coralstay.orchestrator.spawn.AgentSpawner;
import com.coralstay.orchestrator.workspace.WorktreeManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 레이아웃: {@code <tmp>/app}(실제 git 저장소, main), {@code <tmp>/app-wt/<id>}(워크트리). git은 실제로 돌리고, backlog
 * 명령만 가짜로 답한다 — main 작업 트리의 카드는 {@link #cards}, 워크트리의 카드는 {@link #worktreeCard}. claude는
 * {@link #fakeClaude}로 쓴 셸 스크립트다(실제 claude는 띄우지 않는다).
 */
class RunCommandTest {

    static final String ID = "SB-1";
    static final Duration TIMEOUT = Duration.ofSeconds(20);

    @TempDir
    Path tmp;

    Path root;
    Path repo;
    Path spawnLog;
    Path wt;
    /** main 작업 트리에서 실행된 명령 전부(backlog · git). */
    final List<List<String>> mainCommands = Collections.synchronizedList(new ArrayList<>());
    /** 워크트리 카드를 읽으려고 실행된 명령 전부. */
    final List<List<String>> worktreeCommands = Collections.synchronizedList(new ArrayList<>());
    final List<Path> worktreeViews = Collections.synchronizedList(new ArrayList<>());
    final Map<String, String> cards = new HashMap<>();
    /** 이 git 하위 명령은 main 실행기에서 실패로 답한다. */
    final Set<String> failingGit = new HashSet<>();
    volatile String worktreeCard;
    SessionRegistry registry = new SessionRegistry();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    void initRepo() throws IOException {  // ---- 1
        root = tmp.toRealPath();
        repo = Files.createDirectory(root.resolve("app"));
        Path emptyHooks = Files.createDirectory(root.resolve("no-hooks"));
        git(repo, "init", "-q", "-b", "main");
        git(repo, "config", "core.hooksPath", emptyHooks.toString());
        git(repo, "config", "user.name", "test");
        git(repo, "config", "user.email", "test@example.com");
        git(repo, "config", "commit.gpgsign", "false");
        Files.writeString(repo.resolve("README"), "hello\n");
        git(repo, "add", "README");
        git(repo, "commit", "-q", "-m", "init");
        spawnLog = root.resolve("spawn.log");
        wt = root.resolve("app-wt").resolve(ID);
        cards.put(ID, plain(ID, "To Do", List.of(), false));
    }

    // --- 도우미 ---

    /** {@code backlog task view --plain} 모양의 텍스트. checks는 AC마다 체크 여부(개수 = AC 수). */
    static String plain(String id, String status, List<String> dependencies, boolean... checks) {
        var text = new StringBuilder();
        text.append("Task ").append(id).append(" - 제목\n");
        text.append("==================================================\n\n");
        text.append("Status: ○ ").append(status).append("\n\n");
        if (!dependencies.isEmpty()) {
            text.append("Dependency Graph:\n--------------------------------------------------\n");
            text.append("Depends on (").append(dependencies.size()).append(" direct):\n");
            for (String dep : dependencies) {
                text.append("└─ ").append(dep).append(" - 의존 [x]\n");
            }
            text.append("\n");
        }
        text.append("Acceptance Criteria:\n--------------------------------------------------\n");
        for (int i = 0; i < checks.length; i++) {
            text.append("- [").append(checks[i] ? "x" : " ").append("] #").append(i + 1).append(" 기준\n");
        }
        return text.toString();
    }

    static ProcessResult answer(Map<String, String> cards, List<String> command) {
        String card = cards.get(command.get(3));
        return card == null
                ? new ProcessResult(1, "", "Task " + command.get(3) + " not found.\n")
                : new ProcessResult(0, card, "");
    }

    static String git(Path dir, String... args) throws IOException {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        ProcessResult result = BacklogClient.processExecutor(dir).apply(command);
        if (result.exitCode() != 0) {
            throw new IOException(command + " 실패: " + result.stderr());
        }
        return result.stdout();
    }

    boolean branchExists(String branch) {
        return BacklogClient.processExecutor(repo)
                .apply(List.of("git", "rev-parse", "--verify", "--quiet", "refs/heads/" + branch))
                .exitCode() == 0;
    }

    /** 실행 기록({@link #spawnLog})을 남긴 뒤 body를 실행하는 가짜 claude. */
    Path fakeClaude(String body) throws IOException {
        Path script = root.resolve("fake-claude");
        Files.writeString(script, "#!/bin/sh\necho started >> '" + spawnLog + "'\n" + body + "\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        return script;
    }

    RunCommand command(Path claude, Duration timeout) {
        Function<List<String>, ProcessResult> real = BacklogClient.processExecutor(repo);
        Function<List<String>, ProcessResult> mainExecutor = cmd -> {
            mainCommands.add(cmd);
            if (failingGit.contains(cmd.get(1))) {
                return new ProcessResult(128, "", "boom");
            }
            return "backlog".equals(cmd.getFirst()) ? answer(cards, cmd) : real.apply(cmd);
        };
        Function<Path, BacklogClient> worktreeBacklog = path -> {
            worktreeViews.add(path);
            return new BacklogClient(cmd -> {
                worktreeCommands.add(cmd);
                return new ProcessResult(0, worktreeCard, "");
            });
        };
        return new RunCommand(
                mainExecutor,
                new BacklogClient(mainExecutor),
                worktreeBacklog,
                WorktreeManager.forRepo(repo),
                registry,
                new AgentSpawner(claude.toString(), "acceptEdits"),
                timeout,
                print(out),
                print(err));
    }

    RunCommand command(Path claude) {
        return command(claude, TIMEOUT);
    }

    static PrintStream print(ByteArrayOutputStream sink) {
        return new PrintStream(sink, true, StandardCharsets.UTF_8);
    }

    String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    /** B1: 워크트리 · 브랜치 · 프로세스 · 세션이 하나도 생기지 않았다. */
    void assertNothingStarted() {
        assertFalse(Files.exists(wt), "워크트리를 만들면 안 된다");
        assertFalse(Files.exists(spawnLog), "에이전트를 띄우면 안 된다");
        assertEquals(List.of(), registry.live());
        assertEquals(SessionRegistry.DEFAULT_CAPACITY, registry.availablePermits());
    }

    // == AC/DoD ==

    @Test
    @DisplayName("AO-33#1 AO-33#B1 HEAD가 main이 아니면 워크트리 · 프로세스 없이 사유와 exit 3")
    void headNotMainIsNotRunnable() throws IOException {  // ---- 2
        git(repo, "switch", "-q", "-c", "feature");
        Path claude = fakeClaude("exit 0");

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains("HEAD") && stderr().contains("feature"), stderr());
        assertNothingStarted();
        assertFalse(branchExists("task/" + ID));
    }

    @Test
    @DisplayName("AO-33#1 AO-33#B1 상태가 To Do가 아니면 워크트리 · 프로세스 없이 사유와 exit 3")
    void statusNotToDoIsNotRunnable() throws IOException {
        cards.put(ID, plain(ID, "In Progress", List.of(), false));
        Path claude = fakeClaude("exit 0");

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains("In Progress"), stderr());
        assertNothingStarted();
    }

    @Test
    @DisplayName("AO-33#1 AO-33#B1 의존 태스크가 Done이 아니면 워크트리 · 프로세스 없이 사유와 exit 3")
    void dependencyNotDoneIsNotRunnable() throws IOException {
        cards.put(ID, plain(ID, "To Do", List.of("SB-0", "SB-9"), false));
        cards.put("SB-0", plain("SB-0", "Done", List.of(), true));
        cards.put("SB-9", plain("SB-9", "In Progress", List.of(), false));
        Path claude = fakeClaude("exit 0");

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains("SB-9"), stderr());
        assertFalse(stderr().contains("SB-0"), "Done인 의존은 사유가 아니다: " + stderr());
        assertNothingStarted();
    }

    @Test
    @DisplayName("AO-33#1 AO-33#B1 미병합 task/<id> 브랜치가 있으면 워크트리 · 프로세스 없이 사유와 exit 3")
    void unmergedTaskBranchIsNotRunnable() throws IOException {
        git(repo, "branch", "task/" + ID);
        git(repo, "switch", "-q", "task/" + ID);
        git(repo, "commit", "-q", "--allow-empty", "-m", "착수");
        git(repo, "switch", "-q", "main");
        Path claude = fakeClaude("exit 0");

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains("task/" + ID), stderr());
        assertNothingStarted();
    }

    @Test
    @DisplayName("AO-33#2 워크트리를 만든 뒤 admit · 스폰하고, 대기 중에는 세션에 프로세스가 붙어 있다가 끝나면 retire된다")
    void createThenAdmitThenSpawnThenRetire() throws Exception {  // ---- 3
        worktreeCard = plain(ID, "To Do", List.of(), false);
        Path claude = fakeClaude("""
                git rev-parse --abbrev-ref HEAD >> '%s'
                pwd >> '%s'
                sleep 1""".formatted(spawnLog, spawnLog));
        RunCommand command = command(claude);

        var running = CompletableFuture.supplyAsync(() -> command.execute(ID));
        Optional<SessionRegistry.LiveSession> attached = Optional.empty();
        for (int i = 0; i < 200 && attached.isEmpty(); i++) {
            attached = registry.live().stream().filter(s -> s.pid().isPresent()).findFirst();
            Thread.sleep(25);
        }
        int code = running.get(30, TimeUnit.SECONDS);

        assertTrue(attached.isPresent(), "스폰 중에는 admit된 세션에 프로세스가 붙어 있어야 한다");
        assertEquals(ID, attached.get().taskId());
        assertEquals(RunCommand.ROLE, attached.get().role());
        assertEquals(List.of("started", "task/" + ID, wt.toString()), Files.readAllLines(spawnLog),
                "에이전트는 워크트리(task/<id> 브랜치) 안에서 떠야 한다");
        assertEquals(List.of(), registry.live(), "끝나면 retire된다");
        assertEquals(SessionRegistry.DEFAULT_CAPACITY, registry.availablePermits());
        assertEquals(RunCommand.EXIT_FAILURE, code);
    }

    @Test
    @DisplayName("AO-33#2 AO-33#B2 스폰이 예외를 던져도 retire된다(허가 반납) · 워크트리는 보존 · exit 1")
    void spawnExceptionStillRetires() {
        Path missing = root.resolve("no-such-claude");

        int code = command(missing).execute(ID);

        assertEquals(RunCommand.EXIT_FAILURE, code);
        assertTrue(stderr().contains(missing.toString()), stderr());
        assertEquals(List.of(), registry.live());
        assertEquals(SessionRegistry.DEFAULT_CAPACITY, registry.availablePermits());
        assertTrue(Files.isDirectory(wt), "실패면 워크트리를 보존한다");
    }

    @Test
    @DisplayName("AO-33#2 AO-33#B2 타임아웃이 나도 retire된다 · 프로세스는 죽고 exit 1")
    void timeoutStillRetires() throws IOException {
        worktreeCard = plain(ID, "To Do", List.of(), false);
        Path claude = fakeClaude("sleep 30");

        int code = command(claude, Duration.ofMillis(300)).execute(ID);

        assertEquals(RunCommand.EXIT_FAILURE, code);
        assertTrue(stdout().contains("timedOut: true"), stdout());
        assertEquals(List.of(), registry.live());
        assertEquals(SessionRegistry.DEFAULT_CAPACITY, registry.availablePermits());
        assertTrue(Files.isDirectory(wt));
    }

    @Test
    @DisplayName("AO-33#2 admit이 거부되면(같은 태스크가 이미 실행 중) 스폰하지 않고 exit 1 · 남의 세션은 건드리지 않는다")
    void rejectedAdmitDoesNotSpawn() throws Exception {
        registry = new SessionRegistry(2);
        SessionRegistry.Session other = registry.admit(ID, RunCommand.ROLE);
        Path claude = fakeClaude("exit 0");

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_FAILURE, code);
        assertFalse(Files.exists(spawnLog), "admit 전에는 스폰하지 않는다");
        assertTrue(Files.isDirectory(wt), "워크트리는 admit 전에 만든다");
        assertEquals(1, registry.live().size(), "이미 실행 중인 세션은 retire하면 안 된다");
        registry.retire(other);
    }

    /** n개의 빈 커밋을 cwd(워크트리)에 만들고 JSON 한 줄을 찍는 가짜 claude 본문. */
    static String commits(int n) {
        var body = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            body.append("git commit -q --allow-empty -m c").append(i).append('\n');
        }
        return body + "echo '{\"type\":\"result\",\"total_cost_usd\":0.01}'";
    }

    @Test
    @DisplayName("AO-33#3 AO-33#B5 워크트리 카드 Done · AC 전부 체크 · 커밋 수 ≥ AC 수면 성공 exit 0 · 워크트리 제거 · 브랜치 유지 · main 카드는 읽기만")
    void successRemovesWorktreeAndKeepsBranch() throws IOException {  // ---- 4
        worktreeCard = plain(ID, "Done", List.of(), true, true);
        Path claude = fakeClaude(commits(2));

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_SUCCESS, code, stdout() + stderr());
        assertTrue(stdout().contains("판정: 성공"), stdout());
        assertTrue(stdout().contains("커밋 수 / AC 수: 2 / 2"), stdout());
        assertTrue(stdout().contains("\"total_cost_usd\":0.01"), "에이전트 stdout(JSON)을 출력한다: " + stdout());
        assertTrue(stdout().contains(wt.toString()), stdout());
        assertFalse(Files.exists(wt), "성공이면 워크트리를 제거한다");
        assertTrue(branchExists("task/" + ID), "브랜치는 남긴다");
        assertEquals(List.of(wt), worktreeViews, "판정은 워크트리의 카드로 한다");
        assertEquals(List.of(List.of("backlog", "task", "view", ID, "--plain")), worktreeCommands);
        assertTrue(mainCommands.stream()
                        .filter(c -> "backlog".equals(c.getFirst()))
                        .allMatch(c -> c.equals(List.of("backlog", "task", "view", ID, "--plain"))),
                "main 카드는 읽기만 한다(B5): " + mainCommands);
    }

    @Test
    @DisplayName("AO-33#3 AO-33#B3 종료 코드 0이어도 카드가 Done이 아니면 실패 exit 1 · 워크트리 보존")
    void exitZeroAloneIsNotSuccess() throws IOException {
        worktreeCard = plain(ID, "In Progress", List.of(), true, true);
        Path claude = fakeClaude(commits(2));

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_FAILURE, code);
        assertTrue(stdout().contains("에이전트 종료 코드: 0"), stdout());
        assertTrue(stdout().contains("판정: 실패"), stdout());
        assertTrue(Files.isDirectory(wt), "실패면 워크트리를 보존한다");
    }

    @Test
    @DisplayName("AO-33#3 AO-33#B3 Done이어도 체크 안 된 AC가 있으면 실패 exit 1")
    void uncheckedCriterionIsFailure() throws IOException {
        worktreeCard = plain(ID, "Done", List.of(), true, false);
        Path claude = fakeClaude(commits(2));

        assertEquals(RunCommand.EXIT_FAILURE, command(claude).execute(ID));
        assertTrue(stdout().contains("판정: 실패"), stdout());
        assertTrue(Files.isDirectory(wt));
    }

    @Test
    @DisplayName("AO-33#3 AO-33#B3 커밋 수가 AC 수보다 적으면 실패 exit 1")
    void fewerCommitsThanCriteriaIsFailure() throws IOException {
        worktreeCard = plain(ID, "Done", List.of(), true, true);
        Path claude = fakeClaude(commits(1));

        assertEquals(RunCommand.EXIT_FAILURE, command(claude).execute(ID));
        assertTrue(stdout().contains("커밋 수 / AC 수: 1 / 2"), stdout());
        assertTrue(stdout().contains("판정: 실패"), stdout());
        assertTrue(Files.isDirectory(wt));
    }

    @Test
    @DisplayName("AO-33#3 워크트리 카드가 To Do 그대로면 '착수 안 됨' 실패 exit 1")
    void untouchedCardIsNotStarted() throws IOException {
        worktreeCard = plain(ID, "To Do", List.of(), false);
        Path claude = fakeClaude("echo hi; echo warn >&2");

        assertEquals(RunCommand.EXIT_FAILURE, command(claude).execute(ID));
        assertTrue(stdout().contains("판정: 착수 안 됨"), stdout());
        assertTrue(stderr().contains("warn"), "에이전트 stderr는 err로: " + stderr());
        assertTrue(Files.isDirectory(wt));
    }

    // == Coverage ==

    @Test
    @DisplayName("보조: 워크트리 카드를 읽지 못하면 실패 exit 1 · 워크트리 보존")
    void unreadableWorktreeCardIsFailure() throws IOException {
        worktreeCard = "카드 아님";
        Path claude = fakeClaude(commits(1));

        assertEquals(RunCommand.EXIT_FAILURE, command(claude).execute(ID));
        assertTrue(stdout().contains("판정: 실패"), stdout());
        assertTrue(stderr().contains("판정하지 못했습니다"), stderr());
        assertTrue(Files.isDirectory(wt));
    }

    @Test
    @DisplayName("보조: 타임아웃은 ORCHESTRATOR_TIMEOUT_MINUTES(분)이고, 없거나 비면 30분, 양의 정수가 아니면 IllegalArgumentException")
    void timeoutComesFromEnvironment() {
        assertEquals(Duration.ofMinutes(5), RunCommand.timeoutFrom(Map.of(RunCommand.TIMEOUT_ENV, " 5 ")::get));
        assertEquals(Duration.ofMinutes(30), RunCommand.timeoutFrom(name -> null));
        assertEquals(RunCommand.DEFAULT_TIMEOUT, RunCommand.timeoutFrom(name -> ""));
        for (String bad : List.of("0", "-1", "1.5", "abc", "99999999999")) {
            assertThrows(IllegalArgumentException.class,
                    () -> RunCommand.timeoutFrom(Map.of(RunCommand.TIMEOUT_ENV, bad)::get), bad);
        }
    }

    @Test
    @DisplayName("보조: forRepo는 실제 git · backlog로 조립하고, git 저장소가 아니면 exit 3")
    void forRepoWiresProductionCollaborators() throws IOException {
        Path plainDir = Files.createDirectory(root.resolve("plain"));

        int code = RunCommand.forRepo(plainDir, name -> null, print(out), print(err)).execute(ID);

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains("HEAD"), stderr());
        assertThrows(IllegalArgumentException.class,
                () -> RunCommand.forRepo(plainDir, Map.of(AgentSpawner.MAX_BUDGET_ENV, "x")::get, print(out), print(err)));
        assertThrows(IllegalArgumentException.class,
                () -> RunCommand.forRepo(plainDir, Map.of(RunCommand.TIMEOUT_ENV, "x")::get, print(out), print(err)));
    }

    @Test
    @DisplayName("보조: backlogAt은 그 디렉토리에서 backlog를 실행하는 클라이언트다")
    void backlogAtRunsInThatDirectory() throws IOException {
        Path plainDir = Files.createDirectory(root.resolve("plain"));

        var e = assertThrows(RuntimeException.class, () -> RunCommand.backlogAt(plainDir).view("SB-404"));

        assertTrue(e.getMessage() != null, "backlog가 없거나 백로그 프로젝트가 아니면 실패해야 한다");
    }

    @Test
    @DisplayName("보조: 프롬프트는 태스크 ID · 워크트리 · 브랜치를 담고 push 금지를 밝힌다")
    void promptNamesTaskWorktreeAndBranch() {
        String prompt = RunCommand.prompt(ID, wt);

        assertTrue(prompt.contains("승인 · 승격된 태스크 " + ID), prompt);
        assertTrue(prompt.contains(wt.toString()) && prompt.contains("task/" + ID), prompt);
        assertTrue(prompt.contains("git push 하지 않는다"), prompt);
    }

    @Test
    @DisplayName("보조: 커밋 수를 세지 못하면 실패 exit 1")
    void commitCountFailureIsFailure() throws IOException {
        worktreeCard = plain(ID, "Done", List.of(), true);
        failingGit.add("rev-list");
        Path claude = fakeClaude(commits(1));

        assertEquals(RunCommand.EXIT_FAILURE, command(claude).execute(ID));
        assertTrue(stderr().contains("rev-list") && stderr().contains("boom"), stderr());
        assertTrue(Files.isDirectory(wt));
    }

    @Test
    @DisplayName("보조: 성공인데 워크트리를 지우지 못하면(미커밋 파일) 경고만 하고 exit 0 · 워크트리는 남는다")
    void successWithDirtyWorktreeKeepsItAndWarns() throws IOException {
        worktreeCard = plain(ID, "Done", List.of(), true);
        Path claude = fakeClaude("echo x > untracked.txt\n" + commits(1));

        assertEquals(RunCommand.EXIT_SUCCESS, command(claude).execute(ID));
        assertTrue(stderr().contains("워크트리를 제거하지 못했습니다"), stderr());
        assertTrue(Files.isDirectory(wt));
    }

    @Test
    @DisplayName("보조: admit을 기다리다 인터럽트되면 스폰하지 않고 exit 1 · 인터럽트 상태를 남긴다")
    void interruptedAdmitDoesNotSpawn() throws Exception {
        SessionRegistry.Session busy = registry.admit("SB-2", RunCommand.ROLE);
        Path claude = fakeClaude("exit 0");
        RunCommand command = command(claude);
        int[] code = new int[1];
        boolean[] interrupted = new boolean[1];
        Thread runner = Thread.ofPlatform().start(() -> {
            code[0] = command.execute(ID);
            interrupted[0] = Thread.currentThread().isInterrupted();
        });
        for (int i = 0; i < 400 && runner.getState() != Thread.State.WAITING; i++) {
            Thread.sleep(25);
        }

        runner.interrupt();
        runner.join(10_000);

        assertEquals(RunCommand.EXIT_FAILURE, code[0]);
        assertTrue(interrupted[0], "인터럽트 상태를 되살려야 한다");
        assertTrue(stderr().contains("인터럽트"), stderr());
        assertFalse(Files.exists(spawnLog));
        registry.retire(busy);
    }

    @Test
    @DisplayName("보조: 카드를 읽지 못하면(없는 태스크) 워크트리 · 프로세스 없이 exit 3")
    void unreadableCardIsNotRunnable() throws IOException {
        Path claude = fakeClaude("exit 0");

        int code = command(claude).execute("SB-404");

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains("SB-404"), stderr());
        assertNothingStarted();
    }

    @Test
    @DisplayName("보조: git 저장소가 아니면(HEAD를 못 읽으면) exit 3")
    void notAGitRepositoryIsNotRunnable() throws IOException {
        Path claude = fakeClaude("exit 0");
        Path plainDir = Files.createDirectory(root.resolve("plain"));
        Function<List<String>, ProcessResult> executor = BacklogClient.processExecutor(plainDir);
        var command = new RunCommand(
                executor,
                new BacklogClient(executor),
                path -> new BacklogClient(executor),
                WorktreeManager.forRepo(plainDir),
                registry,
                new AgentSpawner(claude.toString(), "acceptEdits"),
                TIMEOUT,
                print(out),
                print(err));

        int code = command.execute(ID);

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains("HEAD"), stderr());
        assertFalse(Files.exists(spawnLog));
    }

    @Test
    @DisplayName("보조: 워크트리 경로가 이미 있으면 스폰 없이 exit 3")
    void existingWorktreePathIsNotRunnable() throws IOException {
        Files.createDirectories(wt);
        Path claude = fakeClaude("exit 0");

        int code = command(claude).execute(ID);

        assertEquals(RunCommand.EXIT_NOT_RUNNABLE, code);
        assertTrue(stderr().contains(wt.toString()), stderr());
        assertFalse(Files.exists(spawnLog));
        assertEquals(List.of(), registry.live());
    }
}

// ======== 노트 ========
// 1 실제 git 저장소 + 가짜 backlog
//   설명: 워크트리 · 브랜치 · 커밋 수는 임시 디렉토리의 실제 git으로 확인하고, backlog task view만 가짜로 답한다. 전역 훅이 끼지 않도록 저장소 로컬 core.hooksPath를 빈 디렉토리로 둔다.
//   관련: WorktreeManagerTest
// 2 B1: 실행 불가면 아무것도 만들지 않는다
//   설명: 사유마다 워크트리 디렉토리 · task 브랜치 · 에이전트 실행 기록 · 세션이 없고 허가가 그대로인지 본다.
//   관련: AC #1, 규칙 B1
// 3 순서: create → admit → start/await → retire
//   설명: 가짜 claude가 자기 cwd의 브랜치와 경로를 기록해 워크트리가 먼저 만들어졌음을 보이고, 실행 중 레지스트리에 프로세스가 붙은 세션이 보이는지, 끝난 뒤 세션이 없어지고 허가가 돌아왔는지 본다. 스폰 예외 · 타임아웃에서도 retire가 된다.
//   관련: AC #2, 규칙 B2, SessionRegistry
// 4 판정은 워크트리 카드와 커밋 수로
//   설명: 가짜 claude가 워크트리에 빈 커밋을 n개 만들고, 워크트리 카드는 가짜 backlog가 답한다. 종료 코드 0만으로는 성공이 아니고(B3), main 카드에는 view 말고 아무 명령도 실행하지 않는다(B5).
//   관련: AC #3, 규칙 B3 · B5, decision-14
