package com.coralstay.orchestrator.registry;

import com.coralstay.orchestrator.spawn.AgentSpawner;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 살아 있는 에이전트 세션을 추적하고 동시 실행 수를 Semaphore로 제한한다 (RFC 06절 Session Registry, "virtual thread +
 * Semaphore").
 *
 * <p>호출 순서는 {@code admit → attach → retire}다. 허가는 admit에서 얻고 retire에서만 반납하므로, 스폰이 예외를 던져도
 * 허가가 새지 않게 호출자는 admit 직후부터 {@code try/finally}로 retire를 보장한다:
 *
 * <pre>{@code
 * Session session = registry.admit(taskId, role);
 * try {
 *     SpawnedAgent agent = spawner.start(role, cwd, prompt, timeout);
 *     registry.attach(session, agent.process());
 *     ... agent.await() ...
 * } finally {
 *     registry.retire(session);
 * }
 * }</pre>
 *
 * <p>상한은 RFC상 min(그래프 폭, 자원 상한)이지만 스켈레톤은 기본 1이고 생성자로만 바꾼다(그래프 폭 계측은 범위 밖).
 */
public final class SessionRegistry {

    /** 기본 동시 실행 상한. 스켈레톤은 Implementer 1개만 띄운다. */
    public static final int DEFAULT_CAPACITY = 1;

    /**
     * admit으로 허가를 얻은 세션. 프로세스는 스폰 후 {@link #attach}로 붙는다 — 그 전에 retire해도(스폰 실패) 허가는
     * 정상 반납된다.
     */
    public static final class Session {
        private final String taskId;
        private final String role;
        private final Instant startedAt;
        private final AtomicReference<ProcessHandle> process = new AtomicReference<>();

        private Session(String taskId, String role, Instant startedAt) {
            this.taskId = taskId;
            this.role = role;
            this.startedAt = startedAt;
        }

        public String taskId() {
            return taskId;
        }

        public String role() {
            return role;
        }

        /** admit(허가 획득) 시각. */
        public Instant startedAt() {
            return startedAt;
        }

        /** attach된 프로세스. 스폰 전이면 비어 있다. */
        public Optional<ProcessHandle> process() {
            return Optional.ofNullable(process.get());
        }
    }

    private final Semaphore permits;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** {@link #DEFAULT_CAPACITY}. */
    public SessionRegistry() {
        this(DEFAULT_CAPACITY);
    }

    /** @param capacity 동시에 살아 있을 수 있는 세션 수 (1 이상) */
    public SessionRegistry(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity는 1 이상이어야 합니다: " + capacity);
        }
        this.permits = new Semaphore(capacity, true);
    }

    /**
     * 허가를 얻은 뒤 세션을 등록한다. 용량이 차 있으면 다른 세션이 retire될 때까지 기다린다.
     *
     * <p>같은 taskId가 이미 살아 있으면 얻은 허가를 돌려주고 {@link IllegalStateException} — 태스크 하나에 worktree
     * 하나(decision-3)이므로 중복 실행은 호출자 버그다.
     *
     * @throws InterruptedException 허가를 기다리다 인터럽트됨 (허가는 얻지 않은 상태)
     */
    public Session admit(String taskId, String role) throws InterruptedException {
        requireText(taskId, "taskId");
        requireText(role, "role");
        permits.acquire();
        var session = new Session(taskId, role, Instant.now());
        if (sessions.putIfAbsent(taskId, session) != null) {
            permits.release();
            throw new IllegalStateException("이미 실행 중인 태스크입니다: " + taskId);
        }
        return session;
    }

    /**
     * 스폰된 프로세스를 세션에 붙인다. retire는 이 프로세스와 자손을 종료 대상으로 삼는다.
     *
     * @throws IllegalStateException 이미 프로세스가 붙어 있거나, 이 레지스트리에 살아 있는 세션이 아님
     */
    public void attach(Session session, ProcessHandle process) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(process, "process");
        if (sessions.get(session.taskId) != session) {
            throw new IllegalStateException("등록된 세션이 아닙니다(이미 retire됨?): " + session.taskId);
        }
        if (!session.process.compareAndSet(null, process)) {
            throw new IllegalStateException("이미 프로세스가 붙은 세션입니다: " + session.taskId);
        }
    }

    /**
     * 세션을 회수한다: attach된 프로세스와 자손을 종료({@link AgentSpawner#terminate}) → 목록에서 제거 → 허가 반납.
     *
     * <p>회수의 정의는 "프로세스(자손 포함) 종료 + 레지스트리 제거 + 허가 반납"이다. {@code claude -p} 헤드리스 세션은
     * 프로세스가 끝나면 컨텍스트도 함께 사라지므로 {@code /clear} 같은 별도 단계가 없다(doc-1 D4).
     *
     * <p>멱등이다 — 허가는 {@code ConcurrentHashMap.remove(key, value)}가 성공한 호출에서만 반납하므로 두 번 불러도
     * Semaphore 용량이 몰래 늘지 않는다. 종료는 매번 시도한다(이미 끝났으면 아무 일도 없다). 종료 중 예외가 나도 제거와
     * 반납은 수행한다.
     *
     * <p><b>한계</b>: 자손은 root가 살아 있을 때만 찾을 수 있다. root가 이미 정상 종료한 뒤라면 그 뒤에 남은 백그라운드
     * 자손은 init으로 재부모화되어 {@link ProcessHandle#descendants()}로 보이지 않으므로 여기서 죽이지 못한다. 실행
     * 중에 자손을 주기적으로 기록해 두는 방식은 스켈레톤 범위 밖이다.
     *
     * @return 이 호출이 세션을 제거하고 허가를 반납했는가 (이미 회수된 세션이면 false)
     */
    public boolean retire(Session session) {
        Objects.requireNonNull(session, "session");
        boolean removed;
        try {
            session.process().ifPresent(AgentSpawner::terminate);
        } finally {
            removed = sessions.remove(session.taskId, session);
            if (removed) {
                permits.release();
            }
        }
        return removed;
    }

    /** 지금 얻을 수 있는 허가 수 (테스트·진단용). */
    public int availablePermits() {
        return permits.availablePermits();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "가 비어 있습니다");
        }
        return value;
    }
}
