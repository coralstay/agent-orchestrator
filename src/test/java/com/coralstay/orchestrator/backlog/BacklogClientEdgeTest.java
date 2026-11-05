// AO-32: BacklogClient의 파싱 실패 · 의존성 블록 경계 · 하위 브랜치 · 실행기 오류 경로를 덮는다.
package com.coralstay.orchestrator.backlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BacklogClientEdgeTest {

    static final String HEADER = "Task AO-9 - 제목\n==================================================\n\n";

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 parse: Task 줄이 없으면 IllegalArgumentException")
    void parseWithoutTaskLineFails() {
        assertThrows(IllegalArgumentException.class, () -> BacklogClient.parse("Status: ○ To Do\n"));
    }

    @Test
    @DisplayName("AO-32#3 parse: Status 줄이 없으면 IllegalArgumentException")
    void parseWithoutStatusLineFails() {
        assertThrows(IllegalArgumentException.class, () -> BacklogClient.parse(HEADER + "Priority: High\n"));
    }

    @Test
    @DisplayName("AO-32#3 parse: 빈 줄 없이 바로 이어지는 Dependents 블록은 직접 의존으로 읽지 않는다")
    void dependentsRightAfterDependsOnAreIgnored() {
        String text = HEADER + "Status: ○ To Do\n\n"
                + "Dependency Graph:\n--------------------------------------------------\n"
                + "Depends on (1 direct, 1 total):\n"
                + "└─ AO-1 - 골격 [Done]\n"
                + "Dependents (1 direct, 1 total):\n"
                + "└─ AO-10 - 다음 [To Do]\n";

        assertEquals(List.of("AO-1"), BacklogClient.parse(text).dependencies());
    }

    @Test
    @DisplayName("AO-32#3 hasUnmergedTaskBranch: task/<ID>/하위 브랜치는 그 태스크 브랜치로 보지 않는다")
    void nestedLocalBranchIsNotTheTaskBranch() {
        List<List<String>> calls = new ArrayList<>();
        var client = new BacklogClient(
                BacklogClientTest.git(calls, "refs/heads/task/AO-9/spike\n", Map.of("refs/heads/task/AO-9/spike", "+ abc\n")));

        assertFalse(client.hasUnmergedTaskBranch("AO-9"));
        assertEquals(1, calls.size(), "cherry를 부르면 안 된다: " + calls);
    }

    @Test
    @DisplayName("AO-32#3 processExecutor: 실행 파일을 띄우지 못하면 UncheckedIOException")
    void executorWrapsStartFailure(@TempDir Path dir) {
        var executor = BacklogClient.processExecutor(dir);

        assertThrows(UncheckedIOException.class, () -> executor.apply(List.of(dir.resolve("no-such-cli").toString())));
    }

    @Test
    @DisplayName("AO-32#3 processExecutor: 기다리는 중 인터럽트되면 IllegalStateException을 던지고 인터럽트 상태를 남긴다")
    void executorKeepsInterruptStatus(@TempDir Path dir) {
        var executor = BacklogClient.processExecutor(dir);
        // 출력 스트림을 닫고 잠깐 살아 있는 자식: 출력 읽기는 바로 끝나고 waitFor에서 인터럽트를 만난다.
        List<String> command = List.of("sh", "-c", "exec >&- 2>&-; sleep 1");

        Thread.currentThread().interrupt();
        try {
            var e = assertThrows(IllegalStateException.class, () -> executor.apply(command));
            assertTrue(e.getMessage().startsWith("interrupted: "), e.getMessage());
            assertTrue(Thread.currentThread().isInterrupted(), "인터럽트 상태가 남아 있어야 한다");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("AO-32#3 stderr 읽기가 실패하면 원인을 담은 IllegalStateException")
    void stderrReadFailureKeepsCause() throws InterruptedException {
        IOException cause = new IOException("pipe closed");

        var e = assertThrows(IllegalStateException.class,
                () -> BacklogClient.awaitStderr(CompletableFuture.failedFuture(cause), List.of("backlog", "x")));

        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("[backlog, x]"), e.getMessage());
        assertEquals("err", BacklogClient.awaitStderr(CompletableFuture.completedFuture("err"), List.of()));
    }

    // == Coverage ==
}
