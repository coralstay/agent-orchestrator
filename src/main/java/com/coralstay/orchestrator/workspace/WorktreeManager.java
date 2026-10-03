package com.coralstay.orchestrator.workspace;

import com.coralstay.orchestrator.backlog.BacklogClient;
import com.coralstay.orchestrator.backlog.BacklogClient.ProcessResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 태스크별 git worktree를 만들고 지운다 (decision-3).
 *
 * <p>태스크 {@code AO-3}이면 저장소 {@code <parent>/<repo>} 옆에 {@code <parent>/<repo>-wt/AO-3} 디렉토리와
 * {@code task/AO-3} 브랜치를 만든다. 브랜치명은 git-logbook {@code Task-Id} 추출 규칙과 claude-rails 브랜치 확인을
 * 함께 만족한다.
 */
public final class WorktreeManager {

    /** git 명령이 실패했거나 전제(경로·브랜치 부재)가 깨졌을 때. */
    public static final class WorktreeException extends RuntimeException {
        public WorktreeException(String message) {
            super(message);
        }
    }

    /** 경로 조작을 막으려고 태스크 ID는 영숫자·하이픈·밑줄만 허용한다. */
    private static final Pattern TASK_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*");

    private final Path repoRoot;
    private final Function<List<String>, ProcessResult> executor;

    /**
     * @param repoRoot 메인 워킹트리 루트
     * @param executor repoRoot에서 명령을 실행하는 실행기 (운영은 {@link #forRepo(Path)})
     */
    public WorktreeManager(Path repoRoot, Function<List<String>, ProcessResult> executor) {
        this.repoRoot = repoRoot.toAbsolutePath().normalize();
        this.executor = Objects.requireNonNull(executor);
    }

    /** repoRoot에서 실제 git을 실행하는 관리자. */
    public static WorktreeManager forRepo(Path repoRoot) {
        return new WorktreeManager(repoRoot, BacklogClient.processExecutor(repoRoot));
    }

    /** {@code <parent>/<repo>-wt/<taskId>}. */
    public Path worktreePath(String taskId) {
        requireValid(taskId);
        return repoRoot.resolveSibling(repoRoot.getFileName() + "-wt").resolve(taskId);
    }

    /** {@code task/<taskId>}. */
    public static String branchName(String taskId) {
        requireValid(taskId);
        return "task/" + taskId;
    }

    /** {@code git worktree add -b task/<id> <path>}를 실행하고 만든 경로를 돌려준다. */
    public Path create(String taskId) {
        Path path = worktreePath(taskId);
        git("worktree", "add", "-b", branchName(taskId), path.toString());
        return path;
    }

    private ProcessResult git(String... args) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        ProcessResult result = executor.apply(command);
        if (result.exitCode() != 0) {
            throw new WorktreeException(String.join(" ", command)
                    + " 실패(exit " + result.exitCode() + "): " + result.stderr().strip());
        }
        return result;
    }

    private static void requireValid(String taskId) {
        if (taskId == null || !TASK_ID.matcher(taskId).matches()) {
            throw new IllegalArgumentException("태스크 ID 형식이 아닙니다: " + taskId);
        }
    }
}
