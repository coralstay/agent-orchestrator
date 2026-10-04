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
 *   <li>{@link #claim}: 저장소 루트(main)에서 {@code backlog task edit <id> -s "In Progress" -a <agent>} 후 카드 파일만
 *       {@code git add} + {@code git commit}(decision-9).
 *   <li>{@link #recordAttempt}·{@link #recordActual}: 태스크 워크트리에서 Description 전체를 읽어 Token Budget 소절만
 *       바꾼 뒤 {@code backlog task edit <id> -d <description>}으로 다시 쓰고 카드 파일을 커밋한다.
 * </ul>
 *
 * <p>오케스트레이터가 직접 하는 커밋은 {@link #STRIPPED_ENV}를 뺀 환경으로 실행한다(decision-5) — 그렇지 않으면 부모 Claude
 * 세션의 커밋으로 표시되고 {@code unavailable (transcript-not-found)} 트레일러가 붙는다. 커밋 메시지는 git-trail 형식
 * {@code [type][subsystem] 설명}(첫 줄 50자 이하)이다.
 *
 * <p>시그니처 스텁(decision-11): 동작은 DRAFT-2에서 구현한다.
 */
public final class BacklogCardSource implements TaskCardSource {

    /** 오케스트레이터 실행분을 나타내는 담당자(decision-9). */
    public static final String AGENT = "@claude";

    /** 오케스트레이터 커밋 환경에서 지우는 AI 도구 환경변수(decision-5). */
    public static final List<String> STRIPPED_ENV = List.of("AI_AGENT", "CLAUDE_CODE_SESSION_ID");

    /**
     * 외부 명령 실행기. {@code env}는 자식 프로세스의 <b>전체</b> 환경이다(부모 환경을 상속하지 않고 이 맵으로 대체).
     * 테스트는 인자·작업 디렉토리·환경을 캡처하는 가짜를, 운영은 {@link #processRunner()}를 넘긴다.
     */
    @FunctionalInterface
    public interface CommandRunner {
        ProcessResult run(List<String> argv, Path workingDir, Map<String, String> env);
    }

    /** 락 판정 종류. */
    public enum LockKind {
        /** 실행해도 된다. */
        FREE,
        /** 같은 대상 파일을 가진 카드(자기 자신 포함)가 사람 담당 In Progress — 실행하지 않는다. */
        HUMAN_LOCK,
        /** 같은 대상 파일을 가진 카드가 {@code @claude} 담당 In Progress — 에이전트 실행분(비정상 종료면 사람에게 보고). */
        AGENT_OWNED
    }

    /**
     * {@link #isLocked} 결과. {@code cardIds}는 판정 근거가 된 카드, {@code files}는 겹친 대상 파일이다. FREE면 둘 다 빈다.
     * 사람 락과 에이전트 실행분이 함께 있으면 HUMAN_LOCK이 우선한다.
     */
    public record LockVerdict(LockKind kind, List<String> cardIds, List<String> files) {

        public LockVerdict {
            Objects.requireNonNull(kind);
            cardIds = List.copyOf(cardIds);
            files = List.copyOf(files);
        }

        public boolean locked() {
            return kind == LockKind.HUMAN_LOCK;
        }
    }

    private final CommandRunner runner;
    private final Path repoRoot;
    private final Function<String, Path> worktreeOf;
    private final Map<String, String> parentEnv;

    /**
     * @param runner 명령 실행기
     * @param repoRoot main 작업 트리(read·claim이 실행되는 곳)
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
    public void claim(String id, String agent) {
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

    /**
     * 파일 단위 사람 락 판정(decision-9). {@code card} 자신과, {@code all} 중 {@code card}와 대상 파일이 하나라도 겹치는
     * 카드를 본다: In Progress + 사람 담당(담당자 없음 포함) → HUMAN_LOCK, In Progress + {@link #AGENT} → AGENT_OWNED,
     * 그 밖 → FREE.
     */
    public LockVerdict isLocked(Card card, List<Card> all) {
        throw new UnsupportedOperationException("pending: DRAFT-2");
    }
}
