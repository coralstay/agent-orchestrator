// AO-32: AgentSpawner의 오류 · 인터럽트 경로(시작 실패, 대기 실패, 스트림 오류, 인터럽트 중 종료)를 덮는다.
package com.coralstay.orchestrator.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.spawn.AgentSpawner.AgentResult;
import com.coralstay.orchestrator.spawn.AgentSpawner.SpawnedAgent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentSpawnerEdgeTest {

    @TempDir
    Path tmp;

    Process sleeper;

    /** 종료 대상으로 쓸 살아 있는 자식 프로세스. */
    ProcessHandle sleeper() throws IOException {
        sleeper = new ProcessBuilder("sleep", "30").start();
        return sleeper.toHandle();
    }

    @AfterEach
    void cleanUp() {
        Thread.interrupted();
        if (sleeper != null) {
            sleeper.destroyForcibly();
        }
    }

    static AgentResult result() {
        return new AgentResult(0, "", "", 1L, false, Duration.ZERO);
    }

    // == AC/DoD ==

    @Test
    @DisplayName("AO-32#3 start: 실행 파일을 띄우지 못하면 UncheckedIOException")
    void startWrapsLaunchFailure() {
        var spawner = new AgentSpawner(tmp.resolve("no-such-claude").toString(), "auto");

        var e = assertThrows(UncheckedIOException.class,
                () -> spawner.start("implementer", tmp, "p", Duration.ofSeconds(5)));
        assertTrue(e.getMessage().contains("no-such-claude"), e.getMessage());
    }

    @Test
    @DisplayName("AO-32#3 start: null · 음수 타임아웃과 null 실행 파일을 거부한다")
    void rejectsNullAndNegativeArguments() {
        var spawner = new AgentSpawner("/usr/bin/true", "auto");

        assertThrows(IllegalArgumentException.class, () -> spawner.start("implementer", tmp, "p", null));
        assertThrows(IllegalArgumentException.class,
                () -> spawner.start("implementer", tmp, "p", Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> new AgentSpawner(null, "auto"));
    }

    @Test
    @DisplayName("AO-32#3 startedAt은 start 호출 시각이다")
    void startedAtIsLaunchTime() {
        Instant before = Instant.now();
        SpawnedAgent agent = new AgentSpawner("/usr/bin/true", "auto").start("implementer", tmp, "p", Duration.ofSeconds(5));
        Instant after = Instant.now();

        assertFalse(agent.startedAt().isBefore(before));
        assertFalse(agent.startedAt().isAfter(after));
        assertEquals(0, agent.await().exitCode());
    }

    @Test
    @DisplayName("AO-32#3 settle: 대기가 끝나면 그 결과로 완료한다")
    void settleCompletesWithResult() throws IOException {
        var future = new CompletableFuture<AgentResult>();
        AgentResult expected = result();

        AgentSpawner.settle(future, sleeper(), () -> expected);

        assertSame(expected, future.join());
        assertTrue(sleeper.isAlive(), "정상 완료에서는 프로세스를 건드리지 않는다");
    }

    @Test
    @DisplayName("AO-32#3 settle: 대기 중 인터럽트되면 프로세스를 종료하고 인터럽트 상태를 남긴 채 실패로 완료한다")
    void settleOnInterruptTerminatesAndFails() throws Exception {
        var future = new CompletableFuture<AgentResult>();
        InterruptedException interrupt = new InterruptedException();

        AgentSpawner.settle(future, sleeper(), () -> {
            throw interrupt;
        });

        assertTrue(Thread.interrupted(), "인터럽트 상태가 남아 있어야 한다(확인하며 지운다)");
        var e = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertSame(interrupt, e.getCause().getCause());
        sleeper.onExit().get(5, TimeUnit.SECONDS);
        assertFalse(sleeper.isAlive());
    }

    @Test
    @DisplayName("AO-32#3 settle: 그 밖의 예외는 그대로 실패 원인이 되고 프로세스는 건드리지 않는다")
    void settlePassesOtherFailuresThrough() throws IOException {
        var unchecked = new CompletableFuture<AgentResult>();
        var checked = new CompletableFuture<AgentResult>();
        RuntimeException boom = new IllegalStateException("boom");
        IOException io = new IOException("io");

        AgentSpawner.settle(unchecked, sleeper(), () -> {
            throw boom;
        });
        AgentSpawner.settle(checked, sleeper.toHandle(), () -> {
            throw io;
        });

        assertSame(boom, assertThrows(CompletionException.class, unchecked::join).getCause());
        assertSame(io, assertThrows(CompletionException.class, checked::join).getCause());
        assertTrue(sleeper.isAlive());
    }

    @Test
    @DisplayName("AO-32#3 await: 실패 원인이 RuntimeException이면 그대로, 아니면 CompletionException으로 던진다")
    void awaitRethrowsFailure() {
        RuntimeException boom = new IllegalStateException("boom");
        IOException io = new IOException("io");
        var failedUnchecked = new SpawnedAgent("implementer", ProcessHandle.current(), Instant.EPOCH,
                CompletableFuture.failedFuture(boom));
        var failedChecked = new SpawnedAgent("implementer", ProcessHandle.current(), Instant.EPOCH,
                CompletableFuture.failedFuture(io));

        assertSame(boom, assertThrows(IllegalStateException.class, failedUnchecked::await));
        assertSame(io, assertThrows(CompletionException.class, failedChecked::await).getCause());
    }

    @Test
    @DisplayName("AO-32#3 drain: 스트림 오류가 나면 그때까지 읽은 출력을 남긴다")
    void drainKeepsOutputReadBeforeError() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream closed");
            }
        };
        InputStream in = new SequenceInputStream(
                new ByteArrayInputStream("부분 출력".getBytes(StandardCharsets.UTF_8)), failing);
        var sink = new ByteArrayOutputStream();

        AgentSpawner.drain(in, sink);

        assertEquals("부분 출력", sink.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("AO-32#3 terminate: 인터럽트된 스레드에서도 대상을 끝내고 인터럽트 상태를 남긴다")
    void terminateUnderInterruptStillKills() throws Exception {
        ProcessHandle target = sleeper();

        Thread.currentThread().interrupt();
        AgentSpawner.terminate(target);

        assertTrue(Thread.interrupted(), "인터럽트 상태가 남아 있어야 한다(확인하며 지운다)");
        target.onExit().get(5, TimeUnit.SECONDS);
        assertFalse(target.isAlive());
    }

    // == Coverage ==
}
