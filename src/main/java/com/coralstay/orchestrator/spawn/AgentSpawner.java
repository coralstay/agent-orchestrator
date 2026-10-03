package com.coralstay.orchestrator.spawn;

import java.util.List;
import java.util.Objects;

/**
 * {@code claude -p} 헤드리스 프로세스를 띄우고 기다리는 스포너 (RFC 06절 Agent Spawner).
 *
 * <p>{@code --bare}는 절대 쓰지 않는다(RFC 11절①, decision-5) — bare 모드는 훅을 건너뛰므로 git-logbook 트레일러와
 * claude-rails 게이트가 함께 꺼진다.
 *
 * <p>BacklogClient의 실행기({@code Function<List<String>, ProcessResult>})는 끝난 결과만 돌려주므로 쓰지 않는다. 스포너는
 * 살아 있는 Process가 필요하다(PID 노출, 타임아웃, 자손 종료). 대신 실행 파일 경로를 주입받아 테스트는 셸 스크립트로 대체한다.
 */
public final class AgentSpawner {

    public static final String DEFAULT_EXECUTABLE = "claude";

    /**
     * 기본 권한 모드 {@code auto}.
     *
     * <p>헤드리스 세션은 승인 프롬프트에 답할 사람이 없으므로 묻지 않고 진행할 모드가 필요하다. {@code auto}는 분류기가
     * 승인을 대신해 멈추지 않으면서도 {@code bypassPermissions}(= {@code --dangerously-skip-permissions})처럼 검사를
     * 통째로 끄지 않는다. {@code acceptEdits}는 파일 편집만 자동 승인하므로 Implementer가 돌려야 하는
     * {@code ./gradlew test}·{@code git commit}이 거부될 수 있다. 최종 선택은 실제 claude로 돌리는 E2E(AO-8)에서 확인한다.
     */
    public static final String DEFAULT_PERMISSION_MODE = "auto";

    private final String executable;
    private final String permissionMode;

    /** {@code claude}(PATH 탐색) + {@link #DEFAULT_PERMISSION_MODE}. */
    public AgentSpawner() {
        this(DEFAULT_EXECUTABLE, DEFAULT_PERMISSION_MODE);
    }

    /**
     * @param executable claude 실행 파일 경로 또는 PATH상 이름 (테스트는 가짜 스크립트)
     * @param permissionMode {@code --permission-mode} 값 (claude 2.1.x 선택지: acceptEdits, auto, bypassPermissions,
     *     manual, dontAsk, plan)
     */
    public AgentSpawner(String executable, String permissionMode) {
        this.executable = requireText(executable, "executable");
        this.permissionMode = requireText(permissionMode, "permissionMode");
    }

    /**
     * {@code [executable, -p, <prompt>, --permission-mode, <mode>, --permission-prompts, none]}.
     *
     * <p>프롬프트는 셸을 거치지 않고 argv 원소 하나로 넘긴다 — 따옴표·개행·{@code $}가 그대로 전달된다.
     * {@code --permission-prompts none}은 권한 모드가 허용하지 않은 동작을 묻지 않고 거부하게 해, 헤드리스 세션이 답할 사람
     * 없는 프롬프트에서 멈추지 않게 한다.
     *
     * @param role 에이전트 역할 (스켈레톤은 Implementer 단독이라 아직 argv에 반영하지 않는다)
     */
    public List<String> command(String role, String prompt) {
        requireText(role, "role");
        Objects.requireNonNull(prompt, "prompt");
        return List.of(
                executable,
                "-p", prompt,
                "--permission-mode", permissionMode,
                "--permission-prompts", "none");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "가 비어 있습니다");
        }
        return value;
    }
}
