package com.coralstay.orchestrator.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.registry.SessionRegistry.LiveSession;
import com.coralstay.orchestrator.registry.SessionRegistry.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 가짜 프로세스는 {@code sh -c} 스크립트로 띄운다 — 실제 claude는 띄우지 않는다. */
class SessionRegistryTest {

    /** 블록돼야 하는 호출이 "아직 안 끝났다"고 판정하기까지 기다리는 시간. */
    static final long BLOCK_MS = 300;

    /** 끝나야 하는 호출을 기다리는 상한. */
    static final long DONE_S = 5;

    final List<Process> started = new ArrayList<>();

    @AfterEach
    void killLeftovers() {
        waiters.forEach(Thread::interrupt);
        for (Process p : started) {
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
        }
    }

    Process sh(String script) throws IOException {
        Process p = new ProcessBuilder("sh", "-c", script).start();
        started.add(p);
        return p;
    }

    /** virtual thread에서 돌린 admit. 테스트가 끝날 때 아직 대기 중이면 인터럽트한다. */
    record PendingAdmit(Thread thread, CompletableFuture<Session> result) {}

    final List<Thread> waiters = new ArrayList<>();

    PendingAdmit admitAsync(SessionRegistry registry, String taskId) {
        var result = new CompletableFuture<Session>();
        Thread thread = Thread.ofVirtual().start(() -> {
            try {
                result.complete(registry.admit(taskId, "implementer"));
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        waiters.add(thread);
        return new PendingAdmit(thread, result);
    }

    // --- admit ---

    @Test
    void admitRegistersSessionAndTakesPermit() throws Exception {
        var registry = new SessionRegistry(2);

        Session session = registry.admit("AO-1", "implementer");

        assertEquals("AO-1", session.taskId());
        assertEquals("implementer", session.role());
        assertTrue(session.process().isEmpty(), "attach 전에는 프로세스가 없다");
        assertEquals(1, registry.availablePermits());
    }

    @Test
    void defaultCapacityIsOne() throws Exception {
        var registry = new SessionRegistry();

        registry.admit("AO-1", "implementer");

        assertEquals(0, registry.availablePermits());
    }

    @Test
    void admitBlocksWhenFull() throws Exception {
        var registry = new SessionRegistry(1);
        registry.admit("AO-1", "implementer");

        PendingAdmit second = admitAsync(registry, "AO-2");

        assertThrows(TimeoutException.class, () -> second.result().get(BLOCK_MS, TimeUnit.MILLISECONDS),
                "용량 1에서 두 번째 admit은 허가가 날 때까지 기다려야 한다");

        second.thread().interrupt();
        var e = assertThrows(java.util.concurrent.ExecutionException.class,
                () -> second.result().get(DONE_S, TimeUnit.SECONDS));
        assertTrue(e.getCause() instanceof InterruptedException, e.toString());
        assertEquals(0, registry.availablePermits(), "인터럽트된 대기자는 허가를 얻지 않는다");
    }

    @Test
    void admitRejectsDuplicateTaskAndReturnsPermit() throws Exception {
        var registry = new SessionRegistry(2);
        registry.admit("AO-1", "implementer");

        assertThrows(IllegalStateException.class, () -> registry.admit("AO-1", "implementer"));
        assertEquals(1, registry.availablePermits(), "중복 거부 시 얻은 허가를 돌려줘야 한다");
    }

    @Test
    void constructorAndAdmitRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SessionRegistry(0));
        var registry = new SessionRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.admit(" ", "implementer"));
        assertThrows(IllegalArgumentException.class, () -> registry.admit("AO-1", ""));
        assertEquals(1, registry.availablePermits());
    }

    @Test
    void attachBindsProcessOnce() throws Exception {
        var registry = new SessionRegistry();
        Session session = registry.admit("AO-1", "implementer");
        Process p = sh("sleep 30");

        registry.attach(session, p.toHandle());

        assertEquals(p.pid(), session.process().orElseThrow().pid());
        assertThrows(IllegalStateException.class, () -> registry.attach(session, p.toHandle()));
        assertFalse(session.process().isEmpty());
    }

    // --- retire ---

    /** sh가 띄운 백그라운드 자식의 PID — 스크립트가 첫 줄에 {@code $!}를 찍는다. */
    static long firstLinePid(Process p) throws IOException {
        return Long.parseLong(firstLineOf(p).strip());
    }

    static boolean alive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    @Test
    void retireReleasesPermitAndUnblocksWaitingAdmit() throws Exception {
        var registry = new SessionRegistry(1);
        Session first = registry.admit("AO-1", "implementer");
        PendingAdmit second = admitAsync(registry, "AO-2");
        assertThrows(TimeoutException.class, () -> second.result().get(BLOCK_MS, TimeUnit.MILLISECONDS));

        assertTrue(registry.retire(first));

        Session admitted = second.result().get(DONE_S, TimeUnit.SECONDS);
        assertEquals("AO-2", admitted.taskId());
        assertEquals(0, registry.availablePermits());
    }

    @Test
    void retireTwiceReleasesPermitOnlyOnce() throws Exception {
        var registry = new SessionRegistry(1);
        Session session = registry.admit("AO-1", "implementer");

        assertTrue(registry.retire(session));
        assertFalse(registry.retire(session), "두 번째 retire는 아무것도 반납하지 않는다");

        assertEquals(1, registry.availablePermits(), "이중 반납은 용량을 몰래 늘린다");
    }

    @Test
    void retireOfStaleSessionDoesNotRemoveNewSessionOfSameTask() throws Exception {
        var registry = new SessionRegistry(2);
        Session old = registry.admit("AO-1", "implementer");
        registry.retire(old);
        Session current = registry.admit("AO-1", "implementer");

        assertFalse(registry.retire(old));

        assertEquals(1, registry.availablePermits());
        assertThrows(IllegalStateException.class, () -> registry.admit("AO-1", "implementer"),
                "새 세션은 여전히 등록돼 있어야 한다");
        assertTrue(registry.retire(current));
    }

    @Test
    void retireWithoutProcessReleasesPermitWhenSpawnFailed() throws Exception {
        var registry = new SessionRegistry(1);
        Session session = registry.admit("AO-1", "implementer");

        assertTrue(registry.retire(session));

        assertEquals(1, registry.availablePermits());
        assertThrows(IllegalStateException.class, () -> registry.attach(session, ProcessHandle.current()),
                "회수된 세션에는 프로세스를 붙일 수 없다");
    }

    @Test
    void retireKillsRunningProcessAndLingeringChild() throws Exception {
        var registry = new SessionRegistry(1);
        Session session = registry.admit("AO-1", "implementer");
        Process p = sh("sleep 30 & echo $!; wait");
        long child = firstLinePid(p);
        registry.attach(session, p.toHandle());
        assertTrue(alive(child));

        assertTrue(registry.retire(session));

        assertFalse(p.isAlive(), "root가 남으면 안 된다");
        assertFalse(alive(child), "백그라운드 자식 sleep(pid " + child + ")이 남으면 안 된다");
        assertEquals(1, registry.availablePermits());
    }

    @Test
    void retireForcesProcessThatIgnoresSigterm() throws Exception {
        var registry = new SessionRegistry(1);
        Session session = registry.admit("AO-1", "implementer");
        Process p = sh("trap '' TERM; echo ready; while :; do sleep 0.05; done");
        firstLineOf(p);
        registry.attach(session, p.toHandle());

        registry.retire(session);

        assertFalse(p.isAlive(), "SIGTERM을 무시해도 유예 후 SIGKILL로 끝나야 한다");
    }

    @Test
    void retireAfterNormalExitJustRemovesSession() throws Exception {
        var registry = new SessionRegistry(1);
        Session session = registry.admit("AO-1", "implementer");
        Process p = sh("exit 0");
        registry.attach(session, p.toHandle());
        assertTrue(p.waitFor(DONE_S, TimeUnit.SECONDS));

        assertTrue(registry.retire(session));

        assertEquals(1, registry.availablePermits());
    }

    static String firstLineOf(Process p) throws IOException {
        return new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)).readLine();
    }

    // --- live ---

    @Test
    void liveReturnsSnapshotOfRegisteredSessions() throws Exception {
        var registry = new SessionRegistry(2);
        Session spawned = registry.admit("AO-1", "implementer");
        Process p = sh("sleep 30");
        registry.attach(spawned, p.toHandle());
        Session pending = registry.admit("AO-2", "verifier");

        List<LiveSession> live = registry.live();

        assertEquals(List.of(
                new LiveSession("AO-1", "implementer", OptionalLong.of(p.pid()), spawned.startedAt()),
                new LiveSession("AO-2", "verifier", OptionalLong.empty(), pending.startedAt())), live);
    }

    @Test
    void liveSnapshotIsImmutableAndUnaffectedByLaterChanges() throws Exception {
        var registry = new SessionRegistry(2);
        Session first = registry.admit("AO-1", "implementer");
        List<LiveSession> before = registry.live();

        registry.admit("AO-2", "implementer");
        registry.retire(first);

        assertEquals(1, before.size());
        assertEquals("AO-1", before.getFirst().taskId());
        assertThrows(UnsupportedOperationException.class, () -> before.add(before.getFirst()));
        assertThrows(UnsupportedOperationException.class, before::clear);
        assertEquals(List.of("AO-2"), registry.live().stream().map(LiveSession::taskId).toList());
    }

    @Test
    void liveIsEmptyAfterAllRetired() throws Exception {
        var registry = new SessionRegistry();
        registry.retire(registry.admit("AO-1", "implementer"));

        assertTrue(registry.live().isEmpty());
    }
}
