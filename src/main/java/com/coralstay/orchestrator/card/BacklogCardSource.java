package com.coralstay.orchestrator.card;

import com.coralstay.orchestrator.backlog.BacklogClient.ProcessResult;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * backlog.md CLI로 구현한 {@link TaskCardSource}(DRAFT-2).
 *
 * <ul>
 *   <li>{@link #read}: {@code backlog task view <id> --plain}(BacklogClient 파서 재사용) + Description의
 *       {@code ### Token Budget} 소절 파싱(decision-6).
 *   <li>{@link #recordAttempt}·{@link #recordActual}: 태스크 워크트리에서 Description 전체를 읽어 Token Budget 소절만
 *       바꾼 뒤 {@code backlog task edit <id> -d <description>}으로 다시 쓰고 카드 파일을 커밋한다.
 * </ul>
 *
 * <p>카드를 In Progress로 바꾸는 착수는 에이전트 세션이 태스크 브랜치에서 한다 — 오케스트레이터는 main의 카드를 고치지
 * 않는다(decision-14).
 *
 * <p>오케스트레이터가 직접 하는 커밋(실제치 기록뿐)은 {@link #STRIPPED_ENV}를 뺀 환경으로 실행한다(decision-5) — 그렇지
 * 않으면 부모 Claude 세션의 커밋으로 표시된다. coding-agent-git-commit-tool GF-130이 {@code CLAUDE_CODE_SESSION_ID}·
 * {@code CLAUDE_PID}로 에이전트를 판별할 예정이라 {@code CLAUDE_PID}도 지운다. 커밋 메시지는 git-trail 형식
 * {@code [type][subsystem] 설명}(첫 줄 50자 이하)이다.
 *
 * <p>시그니처 스텁(decision-11): 동작은 DRAFT-2에서 구현한다.
 */
public final class BacklogCardSource implements TaskCardSource {

    /** 오케스트레이터 커밋 환경에서 지우는 AI 도구 환경변수(decision-5). */
    public static final List<String> STRIPPED_ENV = List.of("AI_AGENT", "CLAUDE_CODE_SESSION_ID", "CLAUDE_PID");

    /**
     * 외부 명령 실행기. {@code env}는 자식 프로세스의 <b>전체</b> 환경이다(부모 환경을 상속하지 않고 이 맵으로 대체).
     * 테스트는 인자·작업 디렉토리·환경을 캡처하는 가짜를, 운영은 {@link #processRunner()}를 넘긴다.
     */
    @FunctionalInterface
    public interface CommandRunner {
        ProcessResult run(List<String> argv, Path workingDir, Map<String, String> env);
    }

    private final CommandRunner runner;
    private final Path repoRoot;
    private final Function<String, Path> worktreeOf;
    private final Map<String, String> parentEnv;

    /**
     * @param runner 명령 실행기
     * @param repoRoot main 작업 트리(read가 실행되는 곳)
     * @param worktreeOf 카드 ID → 태스크 워크트리 경로(recordAttempt·recordActual이 실행되는 곳, 보통
     *     {@code WorktreeManager::worktreePath})
     * @param parentEnv 오케스트레이터 프로세스의 환경(보통 {@code System.getenv()}). 자식 환경은 여기서 만든다.
     */
    public BacklogCardSource(
            CommandRunner runner, Path repoRoot, Function<String, Path> worktreeOf, Map<String, String> parentEnv) {
        this.runner = Objects.requireNonNull(runner);
        this.repoRoot = Objects.requireNonNull(repoRoot);
        this.worktreeOf = Objects.requireNonNull(worktreeOf);
        this.parentEnv = Map.copyOf(parentEnv);
    }

    /** ProcessBuilder로 실행하는 운영용 실행기. 환경을 {@code env}로 통째로 바꾼다. */
    public static CommandRunner processRunner() {
        throw new UnsupportedOperationException("pending: DRAFT-2");
    }

    @Override
    public Card read(String id) {
        throw new UnsupportedOperationException("pending: DRAFT-2");
    }

    @Override
    public void recordAttempt(String id, Attempt attempt) {
        throw new UnsupportedOperationException("pending: DRAFT-2");
    }

    @Override
    public void recordActual(String id, Actual actual) {
        throw new UnsupportedOperationException("pending: DRAFT-2");
    }
}
