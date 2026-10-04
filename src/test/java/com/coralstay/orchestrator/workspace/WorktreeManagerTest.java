package com.coralstay.orchestrator.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.workspace.WorktreeManager.WorktreeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 임시 디렉토리에 만든 실제 git 저장소로 검증한다.
 *
 * <p>레이아웃: {@code <tmp>/app}(저장소), {@code <tmp>/app-wt/<id>}(워크트리). 전역/템플릿 훅이 끼어들지 않도록
 * 저장소 로컬 {@code core.hooksPath}를 빈 디렉토리로 두고 user.name/email도 로컬에 설정한다.
 */
class WorktreeManagerTest {

    @TempDir
    Path tmp;

    Path repo;
    WorktreeManager manager;

    @BeforeEach
    void initRepo() throws IOException {
        Path root = tmp.toRealPath(); // macOS: /var → /private/var. git이 돌려주는 경로와 비교하려고 실경로 사용
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
        manager = WorktreeManager.forRepo(repo);
    }

    // --- create ---

    @Test
    @DisplayName("AO-3#1 create는 형제 디렉토리에 task/<id> 브랜치 worktree를 만들고 경로를 돌려준다")
    void createAddsWorktreeOnTaskBranchNextToRepo() throws IOException {
        Path path = manager.create("AO-3");

        assertEquals(repo.getParent().resolve("app-wt").resolve("AO-3"), path);
        assertTrue(Files.isRegularFile(path.resolve("README")));
        assertEquals("task/AO-3", git(path, "rev-parse", "--abbrev-ref", "HEAD").strip());
        assertTrue(git(repo, "worktree", "list", "--porcelain").contains("worktree " + path));
    }

    @Test
    @DisplayName("AO-3#2 create는 경로가 이미 있으면(빈 디렉토리여도) 예외를 던진다")
    void createRefusesExistingPathEvenIfEmpty() throws IOException {
        // git은 빈 디렉토리면 그대로 worktree add를 해버리므로 직접 막아야 한다
        Path path = Files.createDirectories(manager.worktreePath("AO-3"));

        var e = assertThrows(WorktreeException.class, () -> manager.create("AO-3"));

        assertTrue(e.getMessage().contains(path.toString()), e.getMessage());
        assertFalse(branchExists("task/AO-3"), "브랜치를 만들면 안 된다");
        assertFalse(Files.exists(path.resolve("README")));
    }

    @Test
    @DisplayName("AO-3#2 create는 브랜치가 이미 있으면 예외를 던지고 브랜치를 보존한다")
    void createRefusesExistingBranchAndKeepsIt() throws IOException {
        git(repo, "branch", "task/AO-3");
        String before = git(repo, "rev-parse", "task/AO-3");

        var e = assertThrows(WorktreeException.class, () -> manager.create("AO-3"));

        assertTrue(e.getMessage().contains("task/AO-3"), e.getMessage());
        assertFalse(Files.exists(manager.worktreePath("AO-3")), "경로를 만들면 안 된다");
        assertEquals(before, git(repo, "rev-parse", "task/AO-3"));
    }

    @Test
    @DisplayName("AO-3 보조: worktree 디렉토리를 벗어나는 태스크 ID는 거부한다")
    void rejectsTaskIdThatEscapesWorktreeDirectory() {
        assertThrows(IllegalArgumentException.class, () -> manager.create("../evil"));
    }

    // --- remove ---

    @Test
    @DisplayName("AO-3#3 remove는 worktree를 지우고 커밋이 있는 브랜치는 남긴다")
    void removeDeletesWorktreeAndKeepsBranchWithCommits() throws IOException {
        Path path = manager.create("AO-3");
        Files.writeString(path.resolve("work.txt"), "done\n");
        git(path, "add", "work.txt");
        git(path, "commit", "-q", "-m", "work");
        String head = git(path, "rev-parse", "HEAD");

        manager.remove("AO-3");

        assertFalse(Files.exists(path));
        assertFalse(git(repo, "worktree", "list", "--porcelain").contains(path.toString()));
        assertEquals(head, git(repo, "rev-parse", "task/AO-3"), "브랜치와 커밋은 남아야 한다");
        // 같은 ID로 다시 만들 수 있는지는 브랜치가 남아 있으므로 거부되어야 한다
        assertThrows(WorktreeException.class, () -> manager.create("AO-3"));
    }

    @Test
    @DisplayName("AO-3#3 remove는 손으로 지운 worktree의 메타데이터를 prune한다")
    void removePrunesWorktreeWhoseDirectoryWasDeletedByHand() throws IOException {
        Path path = manager.create("AO-3");
        deleteRecursively(path);

        // 디렉토리가 이미 없으면 remove는 건너뛰고 prune으로 남은 메타데이터만 정리한다
        manager.remove("AO-3");

        assertFalse(git(repo, "worktree", "list", "--porcelain").contains(path.toString()));
    }

    @Test
    @DisplayName("AO-3 보조: 커밋하지 않은 변경이 있는 worktree는 remove를 거부한다")
    void removeRefusesWorktreeWithUncommittedChanges() throws IOException {
        Path path = manager.create("AO-3");
        Files.writeString(path.resolve("README"), "dirty\n");

        var e = assertThrows(WorktreeException.class, () -> manager.remove("AO-3"));

        assertTrue(e.getMessage().contains("worktree remove"), e.getMessage());
        assertEquals("dirty\n", Files.readString(path.resolve("README")), "변경을 지우면 안 된다");
        assertTrue(git(repo, "worktree", "list", "--porcelain").contains(path.toString()));
    }

    // --- helpers ---

    static void deleteRecursively(Path dir) throws IOException {
        try (var paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    boolean branchExists(String branch) {
        try {
            git(repo, "rev-parse", "--verify", "--quiet", "refs/heads/" + branch);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }


    static String git(Path dir, String... args) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        try {
            Process p = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
            p.getOutputStream().close();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = p.waitFor();
            if (exit != 0) {
                throw new IllegalStateException(command + " failed(" + exit + "): " + out);
            }
            return out;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
