package com.coralstay.orchestrator.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.backlog.BacklogClient.ProcessResult;
import com.coralstay.orchestrator.card.BacklogCardSource.LockKind;
import com.coralstay.orchestrator.card.BacklogCardSource.LockVerdict;
import com.coralstay.orchestrator.card.TaskCardSource.Actual;
import com.coralstay.orchestrator.card.TaskCardSource.Attempt;
import com.coralstay.orchestrator.card.TaskCardSource.Card;
import com.coralstay.orchestrator.card.TaskCardSource.TokenBudget;
import com.coralstay.orchestrator.card.TaskCardSource.Tokens;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * DRAFT-2 테스트(decision-11). 실행기 주입 람다로 명령 인자·작업 디렉토리·환경을 캡처하고, {@code backlog task view
 * --plain}에는 실제 출력 형식의 픽스처({@code src/test/resources/card/})를 돌려준다. 구현 전까지 전부 pending이다.
 */
@Tag("pending")
class BacklogCardSourceTest {

    static final Path REPO = Path.of("/repo/app");
    static final Path WORKTREE = Path.of("/repo/app-wt/AO-91");

    static final String CARD_90 = "backlog/tasks/ao-90 - 카드-소스-읽기-픽스처.md";
    static final String CARD_91 = "backlog/tasks/ao-91 - 카드-소스-기록-픽스처.md";

    /** 부모(Claude 세션 안에서 띄운 오케스트레이터) 환경: AI 도구 변수가 들어 있다. */
    static final Map<String, String> PARENT_ENV = Map.of(
            "PATH", "/usr/bin:/bin",
            "HOME", "/home/tester",
            "AI_AGENT", "claude-code/2.1.285",
            "CLAUDE_CODE_SESSION_ID", "11111111-2222-3333-4444-555555555555");

    /** git-trail 형식: {@code [type][subsystem] 설명}. */
    static final Pattern GIT_TRAIL_SUBJECT = Pattern.compile("^\\[[a-z]+\\]\\[[a-z0-9-]+\\] \\S.*$");

    record Call(List<String> argv, Path workingDir, Map<String, String> env) {

        boolean starts(String... prefix) {
            return argv.size() >= prefix.length && argv.subList(0, prefix.length).equals(List.of(prefix));
        }
    }

    /**
     * 호출을 기록하는 가짜 실행기. {@code backlog task view <id> --plain}에는 픽스처를 돌려주되 {@code File:} 줄을 작업
     * 디렉토리 기준 경로로 바꾼다(실제 backlog는 실행한 작업 트리의 카드 파일 경로를 출력한다). 나머지는 성공·빈 출력.
     */
    static final class FakeRunner implements BacklogCardSource.CommandRunner {

        final List<Call> calls = new ArrayList<>();
        private final Map<String, String> fixtures; // id → 픽스처 파일명
        private final Map<String, String> cardFiles; // id → 저장소 기준 카드 경로

        FakeRunner(Map<String, String> fixtures, Map<String, String> cardFiles) {
            this.fixtures = fixtures;
            this.cardFiles = cardFiles;
        }

        @Override
        public ProcessResult run(List<String> argv, Path workingDir, Map<String, String> env) {
            calls.add(new Call(List.copyOf(argv), workingDir, Map.copyOf(env)));
            if (argv.size() == 5
                    && argv.subList(0, 3).equals(List.of("backlog", "task", "view"))
                    && argv.get(4).equals("--plain")
                    && fixtures.containsKey(argv.get(3))) {
                String id = argv.get(3);
                String body = fixture(fixtures.get(id))
                        .replaceFirst("^File: [^\n]*", "File: " + workingDir.resolve(cardFiles.get(id)));
                return new ProcessResult(0, body, "");
            }
            return new ProcessResult(0, "", "");
        }

        List<Call> matching(String... prefix) {
            return calls.stream().filter(c -> c.starts(prefix)).toList();
        }

        Call only(String... prefix) {
            List<Call> found = matching(prefix);
            assertEquals(1, found.size(), () -> String.join(" ", prefix) + " 호출 수: " + calls);
            return found.getFirst();
        }

        int indexOf(Call call) {
            return calls.indexOf(call);
        }
    }

    static FakeRunner runner() {
        return new FakeRunner(
                Map.of("AO-90", "ao-90-plain.txt", "AO-91", "ao-91-plain.txt"),
                Map.of("AO-90", CARD_90, "AO-91", CARD_91));
    }

    static BacklogCardSource source(FakeRunner runner) {
        return new BacklogCardSource(runner, REPO, id -> WORKTREE, PARENT_ENV);
    }

    static String fixture(String name) {
        try (InputStream in = BacklogCardSourceTest.class.getResourceAsStream(name.startsWith("/") ? name : "/card/" + name)) {
            if (in == null) {
                throw new IllegalStateException("fixture not found: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 픽스처 {@code --plain} 출력의 Description 섹션 본문(밑줄 다음 줄부터 다음 섹션 헤더 앞까지, 끝 공백 제외). */
    static String description(String plain) {
        String marker = "Description:\n--------------------------------------------------\n";
        int start = plain.indexOf(marker) + marker.length();
        int end = plain.indexOf("\nAcceptance Criteria:\n", start);
        return plain.substring(start, end).stripTrailing();
    }

    static final Attempt TIMEOUT = new Attempt(
            new Tokens(120000, 3000), new Tokens(150000, 3500), "claude-sonnet-4-5", "timeout");

    // --- DRAFT-2#1 read ---

    @Test
    @DisplayName("DRAFT-2#1 read는 --plain 출력에서 카드 필드와 대상 파일을 읽는다")
    void readParsesCardFields() {
        var runner = runner();

        Card card = source(runner).read("AO-90");

        assertEquals(List.of("backlog", "task", "view", "AO-90", "--plain"), runner.calls.getFirst().argv());
        assertEquals(REPO, runner.calls.getFirst().workingDir());
        assertEquals("AO-90", card.id());
        assertEquals("카드 소스 읽기 픽스처", card.title());
        assertEquals("Done", card.status());
        assertEquals(List.of("@claude"), card.assignees());
        assertEquals(List.of("AO-17"), card.dependencies());
        assertEquals(List.of("Token Budget 소절을 파싱한다", "대상 파일을 읽는다"), card.acceptanceCriteria());
        assertEquals(
                List.of(
                        "src/main/java/com/coralstay/orchestrator/card/BacklogCardSource.java",
                        "src/main/java/com/coralstay/orchestrator/card/TaskCardSource.java"),
                card.targetFiles());
    }

    @Test
    @DisplayName("DRAFT-2#1 read는 Description의 ### Token Budget 소절(estimated/attempt/actual)을 파싱한다")
    void readParsesTokenBudgetFromDescription() {
        Card card = source(runner()).read("AO-90");

        TokenBudget budget = card.tokenBudget().orElseThrow();
        assertEquals(Optional.of(new Tokens(200000, 8000)), budget.estimated());
        assertEquals(List.of(TIMEOUT), budget.attempts());
        assertEquals(
                Optional.of(new Actual(new Tokens(300000, 9000), new Tokens(380000, 9800), 2)), budget.actual());
        assertEquals(List.of(), budget.unparsed(), "다음 ## 제목(대상 파일)은 소절에 포함되지 않는다");
    }

    @Test
    @DisplayName("DRAFT-2#1 read는 Token Budget 소절이 없는 카드에 빈 tokenBudget을 돌려준다")
    void readReturnsEmptyBudgetWithoutSection() {
        var runner = new FakeRunner(
                Map.of("AO-7", "/backlog/ao-7-plain.txt"), Map.of("AO-7", "backlog/tasks/ao-7.md"));

        Card card = source(runner).read("AO-7");

        assertEquals("AO-7", card.id());
        assertEquals(Optional.empty(), card.tokenBudget());
    }

    // --- DRAFT-2#2 claim ---

    @Test
    @DisplayName("DRAFT-2#2 claim은 main에서 task edit -s In Progress -a @claude 후 카드 파일만 add·commit한다")
    void claimEditsThenCommitsOnlyCardFile() {
        var runner = runner();

        source(runner).claim("AO-90", "@claude");

        Call edit = runner.only("backlog", "task", "edit");
        assertEquals(List.of("backlog", "task", "edit", "AO-90", "-s", "In Progress", "-a", "@claude"), edit.argv());
        Call add = runner.only("git", "add");
        assertEquals(List.of("git", "add", "--"), add.argv().subList(0, 3));
        assertEquals(1, add.argv().size() - 3, "카드 파일 하나만 add한다: " + add.argv());
        assertEquals(REPO.resolve(CARD_90), REPO.resolve(add.argv().get(3)).normalize());
        Call commit = runner.only("git", "commit");
        assertFalse(commit.argv().contains("-a") || commit.argv().contains("--all"), commit.argv().toString());
        assertTrue(runner.indexOf(edit) < runner.indexOf(add), "edit → add 순서");
        assertTrue(runner.indexOf(add) < runner.indexOf(commit), "add → commit 순서");
        for (Call call : List.of(edit, add, commit)) {
            assertEquals(REPO, call.workingDir(), "claim은 main 작업 트리에서 한다: " + call.argv());
        }
    }

    @Test
    @DisplayName("DRAFT-2#2 claim 커밋 메시지는 git-trail 형식([type][subsystem] 설명, 50자 이하)이고 카드 ID를 담는다")
    void claimCommitMessageFollowsGitTrail() {
        var runner = runner();

        source(runner).claim("AO-90", "@claude");

        assertGitTrailMessage(runner.only("git", "commit"), "AO-90");
    }

    @Test
    @DisplayName("DRAFT-2#2 claim 커밋 환경에는 부모에 있던 AI_AGENT·CLAUDE_CODE_SESSION_ID가 없다")
    void claimCommitEnvStripsAiToolVariables() {
        var runner = runner();

        source(runner).claim("AO-90", "@claude");

        assertAgentFreeEnv(runner.only("git", "commit"));
        assertAgentFreeEnv(runner.only("git", "add"));
    }

    // --- DRAFT-2#3 recordAttempt / recordActual ---

    @Test
    @DisplayName("DRAFT-2#3 recordAttempt는 Token Budget 소절만 바꾼 Description을 task edit -d로 쓰고 나머지는 그대로 둔다")
    void recordAttemptRewritesOnlyTokenBudgetSubsection() {
        var runner = runner();
        var second = new Attempt(new Tokens(90000, 2000), new Tokens(95000, 2100), "claude-opus-4-1", "tests-failed");

        source(runner).recordAttempt("AO-91", second);

        assertOnlySubsectionReplaced(runner, """
                ### Token Budget

                estimated: in=200000 out=8000
                attempt 1: commits in=120000 out=3000 · session in=150000 out=3500 · model=claude-sonnet-4-5 · timeout
                attempt 2: commits in=90000 out=2000 · session in=95000 out=2100 · model=claude-opus-4-1 · tests-failed
                """);
    }

    @Test
    @DisplayName("DRAFT-2#3 recordActual은 시도 기록을 두고 actual 줄을 더하며 소절 밖은 그대로 둔다")
    void recordActualRewritesOnlyTokenBudgetSubsection() {
        var runner = runner();
        var actual = new Actual(new Tokens(300000, 9000), new Tokens(380000, 9800), 2);

        source(runner).recordActual("AO-91", actual);

        assertOnlySubsectionReplaced(runner, """
                ### Token Budget

                estimated: in=200000 out=8000
                attempt 1: commits in=120000 out=3000 · session in=150000 out=3500 · model=claude-sonnet-4-5 · timeout
                actual: commits in=300000 out=9000 · session in=380000 out=9800 · attempts 2
                """);
    }

    @Test
    @DisplayName("DRAFT-2#3 recordAttempt는 모든 명령을 워크트리에서 실행하고 카드 파일을 커밋한다")
    void recordAttemptRunsInWorktreeAndCommitsCard() {
        var runner = runner();

        source(runner).recordAttempt("AO-91", TIMEOUT);

        assertFalse(runner.calls.isEmpty());
        for (Call call : runner.calls) {
            assertEquals(WORKTREE, call.workingDir(), "워크트리 기준이어야 한다: " + call.argv());
        }
        Call edit = runner.only("backlog", "task", "edit");
        Call add = runner.only("git", "add");
        Call commit = runner.only("git", "commit");
        assertEquals(List.of("git", "add", "--"), add.argv().subList(0, 3));
        assertEquals(1, add.argv().size() - 3, "카드 파일 하나만 add한다: " + add.argv());
        assertEquals(WORKTREE.resolve(CARD_91), WORKTREE.resolve(add.argv().get(3)).normalize());
        assertTrue(runner.indexOf(edit) < runner.indexOf(add), "edit → add 순서");
        assertTrue(runner.indexOf(add) < runner.indexOf(commit), "add → commit 순서");
        assertGitTrailMessage(commit, "AO-91");
        assertAgentFreeEnv(commit);
    }

    @Test
    @DisplayName("DRAFT-2#3 recordActual은 워크트리에서 카드를 커밋하고 커밋 환경에 AI 도구 변수가 없다")
    void recordActualRunsInWorktreeAndCommitsCard() {
        var runner = runner();

        source(runner).recordActual("AO-91", new Actual(new Tokens(1, 1), new Tokens(2, 2), 1));

        for (Call call : runner.calls) {
            assertEquals(WORKTREE, call.workingDir(), "워크트리 기준이어야 한다: " + call.argv());
        }
        Call commit = runner.only("git", "commit");
        assertEquals(WORKTREE.resolve(CARD_91), WORKTREE.resolve(runner.only("git", "add").argv().get(3)).normalize());
        assertGitTrailMessage(commit, "AO-91");
        assertAgentFreeEnv(commit);
    }

    // --- DRAFT-2#4 isLocked ---

    static final String SHARED = "src/main/java/com/coralstay/orchestrator/card/BacklogCardSource.java";
    static final String OTHER = "src/main/java/com/coralstay/orchestrator/Main.java";

    static Card lockCard(String id, String status, List<String> assignees, List<String> files) {
        return new Card(id, "락 판정 " + id, status, assignees, List.of(), List.of(), files, Optional.empty());
    }

    static final Card SELF = lockCard("AO-50", "To Do", List.of(), List.of(SHARED));

    @Test
    @DisplayName("DRAFT-2#4 같은 대상 파일을 가진 다른 카드가 사람 담당 In Progress면 락이다")
    void humanInProgressOnSameFileLocks() {
        Card human = lockCard("AO-51", "In Progress", List.of("@flynn"), List.of(OTHER, SHARED));

        LockVerdict verdict = source(runner()).isLocked(SELF, List.of(SELF, human));

        assertEquals(new LockVerdict(LockKind.HUMAN_LOCK, List.of("AO-51"), List.of(SHARED)), verdict);
        assertTrue(verdict.locked());
    }

    @Test
    @DisplayName("DRAFT-2#4 같은 대상 파일의 @claude 담당 In Progress는 사람 락이 아니라 에이전트 실행분으로 보고한다")
    void claudeInProgressOnSameFileIsAgentOwned() {
        Card agent = lockCard("AO-52", "In Progress", List.of("@claude"), List.of(SHARED));

        LockVerdict verdict = source(runner()).isLocked(SELF, List.of(SELF, agent));

        assertEquals(new LockVerdict(LockKind.AGENT_OWNED, List.of("AO-52"), List.of(SHARED)), verdict);
        assertFalse(verdict.locked());
    }

    @Test
    @DisplayName("DRAFT-2#4 사람 담당 In Progress라도 대상 파일이 다르면 free다")
    void humanInProgressOnDifferentFileIsFree() {
        Card human = lockCard("AO-53", "In Progress", List.of("@flynn"), List.of(OTHER));

        LockVerdict verdict = source(runner()).isLocked(SELF, List.of(SELF, human));

        assertEquals(new LockVerdict(LockKind.FREE, List.of(), List.of()), verdict);
    }

    @Test
    @DisplayName("DRAFT-2#4 이 카드가 To Do이고 같은 파일의 다른 카드도 In Progress가 아니면 free다")
    void toDoCardWithoutInProgressPeersIsFree() {
        Card peerTodo = lockCard("AO-54", "To Do", List.of("@flynn"), List.of(SHARED));
        Card peerDone = lockCard("AO-55", "Done", List.of("@flynn"), List.of(SHARED));

        LockVerdict verdict = source(runner()).isLocked(SELF, List.of(SELF, peerTodo, peerDone));

        assertEquals(new LockVerdict(LockKind.FREE, List.of(), List.of()), verdict);
    }

    @Test
    @DisplayName("DRAFT-2#4 이 카드 자신이 사람 담당 In Progress면 락이다")
    void selfHumanInProgressLocks() {
        Card self = lockCard("AO-50", "In Progress", List.of("@flynn"), List.of(SHARED));

        LockVerdict verdict = source(runner()).isLocked(self, List.of(self));

        assertEquals(LockKind.HUMAN_LOCK, verdict.kind());
        assertEquals(List.of("AO-50"), verdict.cardIds());
    }

    @Test
    @DisplayName("DRAFT-2#4 담당자 없는 In Progress는 사람 락으로 본다(보수적)")
    void unassignedInProgressCountsAsHuman() {
        Card nobody = lockCard("AO-56", "In Progress", List.of(), List.of(SHARED));

        LockVerdict verdict = source(runner()).isLocked(SELF, List.of(SELF, nobody));

        assertEquals(LockKind.HUMAN_LOCK, verdict.kind());
        assertEquals(List.of("AO-56"), verdict.cardIds());
    }

    @Test
    @DisplayName("DRAFT-2#4 사람 락과 에이전트 실행분이 함께 있으면 사람 락이 우선한다")
    void humanLockWinsOverAgentOwned() {
        Card agent = lockCard("AO-52", "In Progress", List.of("@claude"), List.of(SHARED));
        Card human = lockCard("AO-51", "In Progress", List.of("@flynn"), List.of(SHARED));

        LockVerdict verdict = source(runner()).isLocked(SELF, List.of(SELF, agent, human));

        assertEquals(LockKind.HUMAN_LOCK, verdict.kind());
        assertEquals(List.of("AO-51"), verdict.cardIds());
    }

    // --- 검증 헬퍼 ---

    static void assertAgentFreeEnv(Call call) {
        for (String key : BacklogCardSource.STRIPPED_ENV) {
            assertFalse(call.env().containsKey(key), key + "가 남아 있다: " + call.argv());
        }
        assertFalse(call.env().containsKey("AI_AGENT"));
        assertFalse(call.env().containsKey("CLAUDE_CODE_SESSION_ID"));
        assertEquals("/usr/bin:/bin", call.env().get("PATH"), "나머지 부모 환경은 유지한다");
        assertEquals("/home/tester", call.env().get("HOME"));
    }

    static void assertGitTrailMessage(Call commit, String id) {
        int m = commit.argv().indexOf("-m");
        assertTrue(m >= 0 && m + 1 < commit.argv().size(), "git commit -m <메시지>: " + commit.argv());
        String message = commit.argv().get(m + 1);
        String subject = message.lines().findFirst().orElse("");
        assertTrue(GIT_TRAIL_SUBJECT.matcher(subject).matches(), "git-trail 형식이 아니다: " + subject);
        assertTrue(subject.length() <= 50, "첫 줄 50자 초과: " + subject);
        assertTrue(subject.contains(id), "카드 ID가 없다: " + subject);
        message.lines().skip(1).forEach(line -> assertTrue(line.length() <= 72, "본문 72자 초과: " + line));
    }

    /**
     * {@code task edit <id> -d <새 Description>} 하나가 있고, 새 Description이 원본과 비교해 {@code ### Token Budget}
     * 소절 앞(prefix)과 다음 제목부터 끝(suffix)이 바이트 그대로이며, 가운데가 기대 소절과 같다.
     */
    static void assertOnlySubsectionReplaced(FakeRunner runner, String expectedSection) {
        Call edit = runner.only("backlog", "task", "edit");
        assertEquals(List.of("backlog", "task", "edit", "AO-91", "-d"), edit.argv().subList(0, 5));
        assertEquals(6, edit.argv().size(), "-d만 넘긴다: " + edit.argv());
        assertEquals(WORKTREE, edit.workingDir());
        String rewritten = edit.argv().get(5);

        String original = description(fixture("ao-91-plain.txt"));
        String prefix = original.substring(0, original.indexOf("### Token Budget"));
        String suffix = original.substring(original.indexOf("### 메모"));
        assertNotEquals(original, rewritten);
        assertTrue(rewritten.startsWith(prefix), "소절 앞 텍스트가 바뀌었다:\n" + rewritten);
        String trimmed = rewritten.stripTrailing();
        assertTrue(trimmed.endsWith(suffix), "소절 뒤 텍스트가 바뀌었다:\n" + rewritten);
        String middle = trimmed.substring(prefix.length(), trimmed.length() - suffix.length());
        assertEquals(expectedSection.strip(), middle.strip());
    }
}
