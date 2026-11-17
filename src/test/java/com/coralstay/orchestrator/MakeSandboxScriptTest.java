// ---
// title: MakeSandboxScriptTest
// description: scripts/make-sandbox.sh가 실제 run을 돌릴 샌드박스 저장소(카드 SB-1)를 만드는지 임시 디렉토리에서 검증하는 테스트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-17'
// updated_date: '2026-11-17'
// task: AO-33
// ---
// AO-33: 실제 1회 실행에 쓸 샌드박스(git · backlog init, To Do 카드 SB-1, AC 1개)를 make-sandbox.sh가 만드는지 본다.
package com.coralstay.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.coralstay.orchestrator.backlog.BacklogClient;
import com.coralstay.orchestrator.backlog.BacklogClient.ProcessResult;
import com.coralstay.orchestrator.backlog.BacklogClient.Task;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 스크립트를 자식 프로세스로 실제로 돌린다(backlog CLI가 PATH에 없으면 건너뛴다). 사용자 git 설정의 서명 · 훅이 끼지 않도록
 * {@code GIT_CONFIG_*} 환경 변수로 {@code commit.gpgsign=false}, 빈 {@code core.hooksPath}를 덮어쓴다 — 스크립트 자체는 사용자
 * git 설정 그대로 커밋한다.
 */
class MakeSandboxScriptTest {

    static final Path SCRIPT = Path.of("scripts", "make-sandbox.sh").toAbsolutePath();

    record Run(int code, String stdout, String stderr) {}

    @TempDir
    Path tmp;

    Path root;

    @BeforeEach
    void requireBacklog() throws IOException {
        assumeTrue(onPath("backlog"), "backlog CLI not on PATH");
        root = tmp.toRealPath();
    }

    static boolean onPath(String name) {
        return Stream.of(System.getenv().getOrDefault("PATH", "").split(":"))
                .anyMatch(dir -> !dir.isEmpty() && Files.isExecutable(Path.of(dir, name)));
    }

    Run sandbox(String... args) throws IOException, InterruptedException {  // ---- 1
        Path emptyHooks = Files.createDirectories(root.resolve("no-hooks"));
        List<String> command = new ArrayList<>(List.of("sh", SCRIPT.toString()));
        command.addAll(List.of(args));
        var builder = new ProcessBuilder(command).directory(root.toFile());
        var env = builder.environment();
        env.put("GIT_CONFIG_COUNT", "2");
        env.put("GIT_CONFIG_KEY_0", "commit.gpgsign");
        env.put("GIT_CONFIG_VALUE_0", "false");
        env.put("GIT_CONFIG_KEY_1", "core.hooksPath");
        env.put("GIT_CONFIG_VALUE_1", emptyHooks.toString());
        Process process = builder.start();
        process.getOutputStream().close();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(120, TimeUnit.SECONDS), "스크립트가 끝나야 한다");
        return new Run(process.exitValue(), stdout, stderr);
    }

    static String git(Path dir, String... args) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        ProcessResult result = BacklogClient.processExecutor(dir).apply(command);
        assertEquals(0, result.exitCode(), command + ": " + result.stderr());
        return result.stdout();
    }

    // == AC/DoD ==

    @Test
    @DisplayName("AO-33#6 make-sandbox.sh는 main 하나에 커밋된 샌드박스를 만들고, 그 안의 SB-1은 AC 1개 · DoD · SOAP 계획을 갖춘 실행 가능한 To Do 카드다")
    void createsRunnableSandboxWithOneCard() throws Exception {  // ---- 2
        Path dir = root.resolve("ao-sandbox");

        Run run = sandbox(dir.toString());

        assertEquals(0, run.code(), run.stdout() + run.stderr());
        assertTrue(run.stdout().contains("SB-1"), run.stdout());
        assertTrue(run.stdout().contains("git commit --allow-empty -S -m \"Plan-Approved: SB-1\""), run.stdout());
        assertEquals("main", git(dir, "rev-parse", "--abbrev-ref", "HEAD").strip());
        assertEquals("1", git(dir, "rev-list", "--count", "HEAD").strip());
        assertEquals("", git(dir, "status", "--porcelain"), "전부 커밋돼 있어야 한다");

        var backlog = new BacklogClient(BacklogClient.processExecutor(dir));
        Task card = backlog.view("SB-1");
        assertEquals("To Do", card.status());
        assertEquals(1, card.acceptanceCriteria().size());
        assertFalse(card.acceptanceCriteria().getFirst().checked());
        assertTrue(card.acceptanceCriteria().getFirst().text().contains("hello.txt"), card.toString());
        assertTrue(BacklogClient.isRunnable(card, backlog::view, backlog::hasUnmergedTaskBranch));

        String plain = BacklogClient.processExecutor(dir)
                .apply(List.of("backlog", "task", "view", "SB-1", "--plain")).stdout();
        for (String heading : List.of("### Subjective", "### Objective", "### Assessment", "### Plan")) {
            assertTrue(plain.contains(heading), heading + " 이 있어야 한다:\n" + plain);
        }
        assertTrue(plain.contains("Definition of Done"), plain);
        assertFalse(Files.exists(dir.resolve(".claude-code-agile-hooks.json")),
                "humanSigners 설정은 더는 쓰이지 않아 만들지 않는다");
    }

    // == Coverage ==

    @Test
    @DisplayName("보조: 경로가 이미 있으면 아무것도 바꾸지 않고 실패한다")
    void refusesExistingPath() throws Exception {
        Path dir = Files.createDirectory(root.resolve("taken"));

        Run run = sandbox(dir.toString());

        assertNotEquals(0, run.code());
        assertTrue(run.stderr().contains(dir.toString()), run.stderr());
        try (Stream<Path> children = Files.list(dir)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    @DisplayName("보조: 경로 인자가 없으면 usage와 exit 2")
    void requiresPathArgument() throws Exception {
        Run run = sandbox();

        assertEquals(2, run.code());
        assertTrue(run.stderr().contains("usage"), run.stderr());
    }
}

// ======== 노트 ========
// 1 스크립트를 실제로 돌린다
//   설명: 자식 프로세스로 sh scripts/make-sandbox.sh를 실행한다. GIT_CONFIG_* 환경 변수로 서명과 훅만 끄고, 나머지(user.name 등)는 사용자 전역 설정을 쓴다.
//   관련: scripts/make-sandbox.sh
//   개념: GIT_CONFIG_COUNT
// 2 실행 가능한 To Do 카드
//   설명: 오케스트레이터가 실제로 돌 수 있는지(isRunnable)와 에이전트 세션의 착수 검사가 요구하는 계획(SOAP) · AC · DoD가 갖춰졌는지를 본다. 사람 서명(Plan-Approved)은 스크립트가 하지 않고 명령만 안내한다.
//   관련: AC #6, require_implementation_plan
