package com.coralstay.orchestrator.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
    void createAddsWorktreeOnTaskBranchNextToRepo() throws IOException {
        Path path = manager.create("AO-3");

        assertEquals(repo.getParent().resolve("app-wt").resolve("AO-3"), path);
        assertTrue(Files.isRegularFile(path.resolve("README")));
        assertEquals("task/AO-3", git(path, "rev-parse", "--abbrev-ref", "HEAD").strip());
        assertTrue(git(repo, "worktree", "list", "--porcelain").contains("worktree " + path));
    }

    // --- helpers ---

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
