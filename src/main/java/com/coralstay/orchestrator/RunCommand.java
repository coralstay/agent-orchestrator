// ---
// title: RunCommand
// description: run <taskId> 최소 흐름. 실행 가능 판정 → 워크트리 → admit → 에이전트 1개 스폰 → retire → 브랜치 상태로 판정
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-17'
// updated_date: '2026-11-17'
// task: AO-33
// ---
package com.coralstay.orchestrator;

import com.coralstay.orchestrator.backlog.BacklogClient;
import com.coralstay.orchestrator.backlog.BacklogClient.ProcessResult;
import com.coralstay.orchestrator.backlog.BacklogClient.Task;
import com.coralstay.orchestrator.registry.SessionRegistry;
import com.coralstay.orchestrator.registry.SessionRegistry.Session;
import com.coralstay.orchestrator.spawn.AgentSpawner;
import com.coralstay.orchestrator.spawn.AgentSpawner.AgentResult;
import com.coralstay.orchestrator.spawn.AgentSpawner.SpawnedAgent;
import com.coralstay.orchestrator.workspace.WorktreeManager;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * {@code run <taskId>} 최소 흐름(AO-33, AO-7의 얇은 한 조각). 이미 구현된 부품만 잇는다.
 *
 * <ol>
 *   <li>대상 저장소 HEAD가 {@code main}이 아니면 실행 불가(exit 3)
 *   <li>main 작업 트리의 카드가 {@link BacklogClient#isRunnable}이 아니면 실행 불가(exit 3) — 워크트리 · 프로세스 없음
 *   <li>워크트리 생성 → admit → 스폰 · 대기 → retire(finally)
 *   <li>워크트리 카드 · 커밋 수로 판정 — 성공(exit 0)이면 워크트리 제거, 실패(exit 1)면 보존
 * </ol>
 *
 * <p>토큰 예산 · 모델 정책 · runs.jsonl 기록은 하지 않는다(AO-7). main의 카드는 고치지 않는다(decision-14).
 */
public final class RunCommand {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_FAILURE = 1;
    public static final int EXIT_NOT_RUNNABLE = 3;

    /** 스켈레톤은 Implementer 하나만 띄운다. */
    public static final String ROLE = "Implementer";

    /**
     * 고정 프롬프트. {@code %1$s} 태스크 ID, {@code %2$s} 워크트리 경로, {@code %3$s} 태스크 브랜치. "승인 · 승격된
     * 태스크"임을 밝혀야 헤드리스 세션이 승인 대기 없이 진행한다(decision-7).
     */
    static final String PROMPT = """
            승인 · 승격된 태스크 %1$s 를 처리한다. 계획은 사람이 검토하고 Plan-Approved 서명까지 했으므로 승인을 기다리지 말고 바로 진행한다.
            - 작업 위치: 이 디렉터리(%2$s)는 %1$s 전용 git worktree이고 브랜치는 %3$s 다. 다른 디렉터리 · 브랜치로 옮기지 않는다.
            - 시작: backlog task view %1$s --plain 으로 카드를 읽고, 이 브랜치에서 backlog task edit %1$s -s "In Progress" 로 착수한다.
            - 실행: claude-code-agile-hooks 절차를 따른다. 테스트 설정이 있으면 AC마다 테스트를 먼저 쓰고, AC마다 커밋한다.
            - 완료: 객관적 증거를 확인한 뒤 AC와 DoD를 체크하고(--check-ac · --check-dod), --final-summary 를 쓰고, -s Done 으로 옮긴 뒤 그 카드 변경도 커밋한다.
            - 금지: git push 하지 않는다. main 브랜치와 메인 작업 트리는 건드리지 않는다. 카드 범위 밖 작업은 하지 않는다.
            - 훅이 막으면 우회하지 말고 막힌 사유를 출력하고 끝낸다.
            """;  // ---- 1

    private final Function<List<String>, ProcessResult> git;
    private final BacklogClient backlog;
    private final Function<Path, BacklogClient> worktreeBacklog;
    private final WorktreeManager worktrees;
    private final SessionRegistry registry;
    private final AgentSpawner spawner;
    private final Duration timeout;
    private final PrintStream out;
    private final PrintStream err;

    /**
     * @param git 대상 저장소 루트에서 명령을 실행하는 실행기(HEAD 확인 · 커밋 수)
     * @param backlog main 작업 트리의 카드를 읽는 클라이언트(실행 가능 판정)
     * @param worktreeBacklog 워크트리 경로 → 그 워크트리의 카드를 읽는 클라이언트(판정)
     */
    public RunCommand(  // ---- 2
            Function<List<String>, ProcessResult> git,
            BacklogClient backlog,
            Function<Path, BacklogClient> worktreeBacklog,
            WorktreeManager worktrees,
            SessionRegistry registry,
            AgentSpawner spawner,
            Duration timeout,
            PrintStream out,
            PrintStream err) {
        this.git = Objects.requireNonNull(git, "git");
        this.backlog = Objects.requireNonNull(backlog, "backlog");
        this.worktreeBacklog = Objects.requireNonNull(worktreeBacklog, "worktreeBacklog");
        this.worktrees = Objects.requireNonNull(worktrees, "worktrees");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.spawner = Objects.requireNonNull(spawner, "spawner");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    /** 에이전트 대기 기본 한도. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);

    /** 대기 한도(분)를 바꾸는 환경 변수. */
    public static final String TIMEOUT_ENV = "ORCHESTRATOR_TIMEOUT_MINUTES";

    /**
     * repoRoot를 대상 저장소로 실제 git · backlog · claude를 쓰는 RunCommand. 세션 상한은 1, 비용 상한과 대기 한도는 env에서
     * 읽는다.
     *
     * @throws IllegalArgumentException 비용 상한 · 대기 한도 환경 변수가 잘못됐을 때(아무것도 실행하기 전)
     */
    public static RunCommand forRepo(  // ---- 3
            Path repoRoot, Function<String, String> env, PrintStream out, PrintStream err) {
        Function<List<String>, ProcessResult> executor = BacklogClient.processExecutor(repoRoot);
        return new RunCommand(
                executor,
                new BacklogClient(executor),
                RunCommand::backlogAt,
                WorktreeManager.forRepo(repoRoot),
                new SessionRegistry(),
                new AgentSpawner(
                        AgentSpawner.DEFAULT_EXECUTABLE,
                        AgentSpawner.DEFAULT_PERMISSION_MODE,
                        AgentSpawner.maxBudgetFrom(env)),
                timeoutFrom(env),
                out,
                err);
    }

    /** dir에서 backlog를 실행하는 클라이언트(워크트리 카드 판정용). */
    static BacklogClient backlogAt(Path dir) {
        return new BacklogClient(BacklogClient.processExecutor(dir));
    }

    /**
     * 환경 변수 {@link #TIMEOUT_ENV}(분)의 대기 한도. 없거나 비면 {@link #DEFAULT_TIMEOUT}, 양의 정수가 아니면
     * {@link IllegalArgumentException}.
     */
    static Duration timeoutFrom(Function<String, String> env) {
        String value = env.apply(TIMEOUT_ENV);
        if (value == null || value.isBlank()) {
            return DEFAULT_TIMEOUT;
        }
        try {
            int minutes = Integer.parseInt(value.strip());
            if (minutes > 0) {
                return Duration.ofMinutes(minutes);
            }
        } catch (NumberFormatException e) {
            // 아래에서 같은 사유로 거부한다
        }
        throw new IllegalArgumentException(TIMEOUT_ENV + "(대기 한도, 분)는 양의 정수여야 합니다: " + value);
    }

    /** 태스크 하나를 처리하고 종료 코드를 돌려준다: 0 성공 · 1 실패 · 3 실행 불가. */
    public int execute(String taskId) {
        String reason = notRunnableReason(taskId);
        if (reason != null) {
            err.println("실행 불가: " + reason);
            return EXIT_NOT_RUNNABLE;
        }
        Path worktree;
        try {
            worktree = worktrees.create(taskId);
        } catch (RuntimeException e) {
            err.println("실행 불가: 워크트리를 만들지 못했습니다: " + e.getMessage());
            return EXIT_NOT_RUNNABLE;
        }
        AgentResult result = runAgent(taskId, worktree);
        if (result == null) {
            err.println("워크트리 보존: " + worktree);
            return EXIT_FAILURE;
        }
        Judgement judgement = judge(taskId, worktree);
        boolean success = judgement.verdict() == Verdict.SUCCESS;
        String worktreeState = "보존";
        if (success) {
            try {
                worktrees.remove(taskId);  // ---- 4
                worktreeState = "제거됨";
            } catch (RuntimeException e) {
                err.println("경고: 워크트리를 제거하지 못했습니다(브랜치 판정은 성공): " + e.getMessage());
            }
        }
        out.println("--- 에이전트 stdout ---");
        out.println(result.stdout().strip());
        if (!result.stderr().isBlank()) {
            err.println("--- 에이전트 stderr ---");
            err.println(result.stderr().strip());
        }
        out.println("--- 결과 ---");
        out.println("판정: " + judgement.verdict().label());
        out.println("카드 상태: " + judgement.status());
        out.println("에이전트 종료 코드: " + result.exitCode());
        out.println("timedOut: " + result.timedOut());
        out.println("소요: " + result.duration());
        out.println("커밋 수 / AC 수: " + judgement.commits() + " / " + judgement.criteria());
        out.println("워크트리: " + worktree + " (" + worktreeState + ")");
        return success ? EXIT_SUCCESS : EXIT_FAILURE;
    }

    /** 판정 결과. */
    public enum Verdict {
        SUCCESS("성공"),
        FAILURE("실패"),
        NOT_STARTED("착수 안 됨");

        private final String label;

        Verdict(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 출력용 판정 근거. 판정하지 못했으면 상태 · 수는 "?"다. */
    private record Judgement(Verdict verdict, String status, String commits, String criteria) {}

    /**
     * 브랜치 상태로 판정한다(B3): 카드가 To Do 그대로면 착수 안 됨, Done이고 AC가 전부 체크됐고 커밋 수 ≥ AC 수면 성공,
     * 그 밖은 실패. 에이전트 종료 코드는 보지 않는다. 부수효과 없는 순수 함수.
     */
    static Verdict verdict(Task card, int commits) {  // ---- 5
        if (BacklogClient.TO_DO.equals(card.status())) {
            return Verdict.NOT_STARTED;
        }
        List<BacklogClient.Criterion> criteria = card.acceptanceCriteria();
        boolean done = BacklogClient.DONE.equals(card.status());
        boolean allChecked = criteria.stream().allMatch(BacklogClient.Criterion::checked);
        return done && allChecked && commits >= criteria.size() ? Verdict.SUCCESS : Verdict.FAILURE;
    }

    /** 워크트리의 카드(태스크 브랜치 위의 카드)와 {@code main..task/<id>} 커밋 수로 판정한다. main 카드는 읽지 않는다(B5). */
    private Judgement judge(String taskId, Path worktree) {
        try {
            Task card = worktreeBacklog.apply(worktree).view(taskId);  // ---- 6
            int commits = commitCount(taskId);
            return new Judgement(verdict(card, commits), card.status(), String.valueOf(commits),
                    String.valueOf(card.acceptanceCriteria().size()));
        } catch (RuntimeException e) {
            err.println("판정하지 못했습니다: " + e.getMessage());
            return new Judgement(Verdict.FAILURE, "?", "?", "?");
        }
    }

    /** {@code git rev-list --count main..task/<id>}. */
    private int commitCount(String taskId) {
        List<String> command = List.of(
                "git", "rev-list", "--count", BacklogClient.MAIN_BRANCH + ".." + WorktreeManager.branchName(taskId));
        ProcessResult result = git.apply(command);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " 실패: " + result.stderr().strip());
        }
        return Integer.parseInt(result.stdout().strip());
    }

    /**
     * admit → start → attach → await, retire는 finally(B2). admit이 거부되거나 스폰 · 대기가 실패하면 사유를 출력하고 null.
     */
    private AgentResult runAgent(String taskId, Path worktree) {  // ---- 7
        Session session;
        try {
            session = registry.admit(taskId, ROLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            err.println("실패: admit을 기다리다 인터럽트됐습니다");
            return null;
        } catch (RuntimeException e) {
            err.println("실패: admit이 거부됐습니다: " + e.getMessage());
            return null;
        }
        try {
            SpawnedAgent agent = spawner.start(ROLE, worktree, prompt(taskId, worktree), timeout);
            registry.attach(session, agent.process());
            return agent.await();
        } catch (RuntimeException e) {
            err.println("실패: 에이전트 실행 중 오류: " + e.getMessage());
            return null;
        } finally {
            registry.retire(session);
        }
    }

    /** 헤드리스 에이전트에게 주는 고정 프롬프트. */
    static String prompt(String taskId, Path worktree) {
        return PROMPT.formatted(taskId, worktree, WorktreeManager.branchName(taskId));
    }

    /** 실행 불가 사유. 실행할 수 있으면 null. 아무것도 만들거나 고치지 않는다(B1). */
    private String notRunnableReason(String taskId) {  // ---- 8
        ProcessResult head = git.apply(List.of("git", "rev-parse", "--abbrev-ref", "HEAD"));
        if (head.exitCode() != 0) {
            return "대상 저장소의 HEAD를 읽지 못했습니다: " + head.stderr().strip();
        }
        String branch = head.stdout().strip();
        if (!BacklogClient.MAIN_BRANCH.equals(branch)) {
            return "대상 저장소의 HEAD가 " + BacklogClient.MAIN_BRANCH + "가 아닙니다: " + branch;
        }
        try {
            Task task = backlog.view(taskId);
            if (!BacklogClient.TO_DO.equals(task.status())) {
                return taskId + " 상태가 " + BacklogClient.TO_DO + "가 아닙니다: " + task.status();
            }
            List<String> reasons = new ArrayList<>();
            boolean runnable = BacklogClient.isRunnable(  // ---- 9
                    task,
                    id -> {
                        Task dependency = backlog.view(id);
                        if (!BacklogClient.DONE.equals(dependency.status())) {
                            reasons.add("의존 태스크 " + id + "가 Done이 아닙니다: " + dependency.status());
                        }
                        return dependency;
                    },
                    id -> {
                        boolean unmerged = backlog.hasUnmergedTaskBranch(id);
                        if (unmerged) {
                            reasons.add("미병합 " + WorktreeManager.branchName(id) + " 브랜치가 있습니다(이미 착수됨)");
                        }
                        return unmerged;
                    });
            return runnable ? null : reasons.getFirst();
        } catch (RuntimeException e) {
            return taskId + " 카드를 확인하지 못했습니다: " + e.getMessage();
        }
    }
}

// ======== 노트 ========
// 1 프롬프트는 고정 문구
//   설명: 승인 · 승격된 태스크임을 밝혀야 헤드리스 세션이 승인 대기 없이 진행한다. 착수(In Progress)는 에이전트 세션이 태스크 브랜치에서 하고, push와 main 수정은 금지한다. push는 argv의 --disallowedTools로도 막는다.
//   관련: decision-7, decision-14, AgentSpawner.command
// 2 부품은 생성자로 받는다
//   설명: 실행기 · 백로그 클라이언트 · 워크트리 관리자 · 레지스트리 · 스포너를 주입받아 테스트는 임시 git 저장소 · 가짜 backlog · 가짜 claude 스크립트로 흐름 전체를 돌린다. decision-2대로 흐름을 이 한 클래스에 모아 나중에 Watcher가 그대로 재사용한다.
//   관련: RunCommandTest, decision-2
//   개념: 의존성 주입
// 3 운영 조립
//   설명: 대상 저장소는 호출한 디렉토리다. 비용 상한(ORCHESTRATOR_MAX_BUDGET_USD, 기본 1.00)과 대기 한도(ORCHESTRATOR_TIMEOUT_MINUTES, 기본 30분)는 환경 변수에서 읽고, 잘못된 값은 아무것도 실행하기 전에 IllegalArgumentException이다.
//   관련: Main.runTask, AgentSpawner.maxBudgetFrom
// 4 성공이면 워크트리 제거, 브랜치는 남긴다
//   설명: 실패면 부분 작업 상태를 사람이 판단하도록 워크트리를 보존한다(decision-3). 성공인데 미커밋 파일이 남아 제거가 실패하면 경고만 하고 판정은 그대로 성공이다(remove는 --force를 쓰지 않는다).
//   관련: WorktreeManager.remove, decision-3
// 5 종료 코드만으로 성공 처리하지 않는다 (B3)
//   설명: 성공은 카드 Done ∧ AC 전부 체크 ∧ 커밋 수 ≥ AC 수뿐이다. 카드가 To Do 그대로면 세션이 착수(In Progress 전환)조차 못 한 것이라 '착수 안 됨'으로 구분한다. 에이전트 종료 코드는 출력만 한다.
//   관련: decision-14, AO-7
//   개념: 브랜치 상태 판정
// 6 판정은 워크트리의 카드로 (B5)
//   설명: 카드 변경은 에이전트가 태스크 브랜치에서 한다. 오케스트레이터는 워크트리에서 카드를 읽기만 하고 main의 카드는 고치지 않는다.
//   관련: decision-14
// 7 retire는 finally (B2)
//   설명: admit에 성공한 뒤로는 스폰이 예외를 던지든 대기가 타임아웃이든 finally에서 retire해 프로세스(자손 포함)를 끝내고 허가를 돌려준다. admit이 거부되거나 인터럽트되면 스폰하지 않는다. 워크트리는 admit 전에 만든다.
//   관련: SessionRegistry, AO-33
//   개념: try/finally
// 8 실행 불가면 아무것도 만들지 않는다 (B1)
//   설명: HEAD가 main인지, 카드가 To Do인지, 의존이 Done인지, 미병합 task/<id> 브랜치가 없는지를 워크트리 · 프로세스를 만들기 전에 본다. 사유는 첫 번째 하나만 출력하고 exit 3이다. 카드를 읽지 못해도 실행 불가다.
//   관련: AO-33, decision-14
// 9 판정은 isRunnable 하나로
//   설명: 배정 조건의 기준은 BacklogClient.isRunnable이다. 의존 조회 · 브랜치 검사 함수를 감싸 거기서 걸린 사유를 모은다 — 조건을 여기서 다시 구현하지 않는다.
//   관련: BacklogClient.isRunnable
//   개념: 단일 기준
