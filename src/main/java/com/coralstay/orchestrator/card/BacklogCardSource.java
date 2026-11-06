// ---
// title: BacklogCardSource
// description: backlog.md CLI로 구현한 카드 접근 포트
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-06'
// ---
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
    public static final List<String> STRIPPED_ENV = List.of("AI_AGENT", "CLAUDE_CODE_SESSION_ID", "CLAUDE_PID");  // ---- 1

    /**
     * 외부 명령 실행기. {@code env}는 자식 프로세스의 <b>전체</b> 환경이다(부모 환경을 상속하지 않고 이 맵으로 대체).
     * 테스트는 인자·작업 디렉토리·환경을 캡처하는 가짜를, 운영은 {@link #processRunner()}를 넘긴다.
     */
    @FunctionalInterface
    public interface CommandRunner {
        ProcessResult run(List<String> argv, Path workingDir, Map<String, String> env);  // ---- 2
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
        this.parentEnv = Map.copyOf(parentEnv);  // ---- 3
    }

    /** ProcessBuilder로 실행하는 운영용 실행기. 환경을 {@code env}로 통째로 바꾼다. */
    public static CommandRunner processRunner() {  // ---- 4
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

// ======== 노트 ========
// 1 커밋 환경에서 AI 도구 변수를 지운다
//   설명: 오케스트레이터가 직접 하는 커밋(실제치 기록뿐)은 이 변수를 뺀 환경으로 실행한다. 안 지우면 부모 Claude 세션의 커밋으로 표시된다. coding-agent-git-commit-tool GF-130이 CLAUDE_CODE_SESSION_ID · CLAUDE_PID로 에이전트를 판별할 예정이라 CLAUDE_PID도 지운다.
//   관련: decision-5, coding-agent-git-commit-tool GF-130
//   개념: 환경 변수 상속
// 2 env는 자식의 전체 환경
//   설명: 부모 환경을 상속하지 않고 이 맵으로 통째로 대체한다. 그래야 STRIPPED_ENV가 확실히 빠진다. 테스트는 인자 · 작업 디렉토리 · 환경을 캡처하는 가짜를 넣는다.
//   관련: BacklogCardSourceTest
// 3 부모 환경은 방어적 복사
//   설명: 생성 뒤 바깥에서 맵을 바꿔도 자식 환경이 달라지지 않게 불변 사본을 둔다.
//   개념: 방어적 복사
// 4 아직 구현 없는 스텁 (DRAFT-2)
//   설명: read · record* 와 함께 pending 스텁이다. build.gradle.kts pendingStubClasses에 올라 커버리지 측정에서 빠지며, 구현 카드가 그 항목을 지운다.
//   관련: build.gradle.kts, DRAFT-2
//   개념: 시그니처 스텁
