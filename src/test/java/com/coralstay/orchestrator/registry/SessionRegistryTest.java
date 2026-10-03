package com.coralstay.orchestrator.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coralstay.orchestrator.registry.SessionRegistry.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
}
