// ---
// title: Main
// description: CLI 진입점. run <taskId>를 RunCommand로 연결하고 잘못된 인자는 종료 코드 2
// authors: [coralstay]
// license: AGPL-3.0-only
// status: draft
// created_date: '2026-11-06'
// updated_date: '2026-11-17'
// ---
package com.coralstay.orchestrator;

import java.io.PrintStream;
import java.nio.file.Path;

/** CLI 진입점. 현재 지원 명령은 {@code run <taskId>} 하나다(decision-2). */
public final class Main {

    static final String USAGE = "usage: orchestrator run <taskId>";
    static final int EXIT_USAGE = 2;

    private Main() {}

    /** 파싱 결과: 성공한 명령, 도움말, 또는 usage 오류. */
    sealed interface ParseResult permits Command, Help, UsageError {}

    /** 파싱된 명령. 이름과 대상 taskId. */
    record Command(String name, String taskId) implements ParseResult {}

    /** 도움말 출력 요청. */
    record Help() implements ParseResult {}

    /** 인자가 잘못됐을 때의 사유. */
    record UsageError(String reason) implements ParseResult {}

    /** 인자 배열을 해석한다. 부수효과 없는 순수 함수. */
    static ParseResult parse(String[] args) {  // ---- 1
        if (args == null || args.length == 0) {
            return new UsageError("명령이 없습니다");
        }
        String name = args[0];
        if ("-h".equals(name) || "--help".equals(name) || "help".equals(name)) {
            return new Help();
        }
        if (!"run".equals(name)) {  // ---- 2
            return new UsageError("알 수 없는 명령: " + name);
        }
        if (args.length != 2) {
            return new UsageError("run에는 taskId 하나가 필요합니다");
        }
        String taskId = args[1].strip();
        if (taskId.isEmpty()) {
            return new UsageError("taskId가 비어 있습니다");
        }
        return new Command(name, taskId);
    }

    /** {@code run <taskId>}를 실제로 처리하는 쪽. 종료 코드를 돌려준다. */
    @FunctionalInterface
    interface TaskRunner {
        int run(String taskId, PrintStream out, PrintStream err);
    }

    /** parse 결과를 실행하고 exit code를 돌려준다. System.exit은 main에서만 부른다. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, out, err, Main::runTask);
    }

    /** runner를 주입받는 {@link #run(String[], PrintStream, PrintStream)}. 테스트는 가짜 runner를 넘긴다. */
    static int run(String[] args, PrintStream out, PrintStream err, TaskRunner runner) {  // ---- 3
        return switch (parse(args)) {
            case Command command -> runner.run(command.taskId(), out, err);
            case Help help -> {
                out.println(USAGE);
                out.println("  run <taskId>   지정한 백로그 태스크에 대한 역할형 에이전트 세션 실행");
                out.println("  -h, --help     이 도움말 출력");
                yield 0;
            }
            case UsageError error -> {
                err.println(error.reason());
                err.println(USAGE);
                yield EXIT_USAGE;
            }
        };
    }

    /**
     * 현재 디렉토리를 대상 저장소로 {@link RunCommand}를 돌린다(AO-33). 환경 변수 설정이 잘못됐으면 실행하지 않고
     * {@link #EXIT_USAGE}.
     */
    static int runTask(String taskId, PrintStream out, PrintStream err) {  // ---- 4
        RunCommand command;
        try {
            command = RunCommand.forRepo(Path.of("").toAbsolutePath(), System::getenv, out, err);
        } catch (IllegalArgumentException e) {
            err.println("설정 오류: " + e.getMessage());
            return EXIT_USAGE;
        }
        return command.execute(taskId);
    }

    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        if (code != 0) {
            // System.exit은 돌아오지 않아 그 줄의 커버리지 프로브가 끝내 실행되지 않는다(AO-32). 종료는 별도 스레드에
            // 맡기고 main은 정상 반환한다 — JVM은 그 non-daemon 스레드의 System.exit(code)로 끝난다.
            Thread.ofPlatform().name("exit-" + code).start(() -> System.exit(code));  // ---- 5
        }
    }
}

// ======== 노트 ========
// 1 parse는 순수 함수
//   설명: 인자 배열을 Command · Help · UsageError 중 하나로만 바꾼다. 출력과 종료는 하지 않아 단위 테스트가 쉽다.
//   관련: MainTest, decision-2
//   개념: 순수 함수
// 2 지원 명령은 run 하나
//   설명: 알 수 없는 명령은 전부 UsageError다. 새 명령을 넣으면 USAGE와 도움말도 함께 고친다.
//   관련: decision-2
// 3 System.exit은 main에서만
//   설명: run은 exit code를 돌려주기만 한다. 종료를 main 한 곳에 모아 run을 System.exit 없이 테스트한다. run <taskId>를 처리하는 쪽(TaskRunner)은 주입받아 단위 테스트가 실제 프로세스를 띄우지 않는다.
//   관련: MainTest
//   개념: 종료 코드
// 4 run <taskId> → RunCommand
//   설명: 현재 디렉토리를 대상 저장소로 RunCommand를 조립해 돌리고 그 종료 코드(0 성공 · 1 실패 · 3 실행 불가)를 그대로 돌려준다. 비용 상한 · 대기 한도 환경 변수가 잘못되면 실행 전에 exit 2다.
//   관련: RunCommand, AO-33, decision-2
// 5 종료를 별도 스레드에 맡긴다
//   설명: System.exit은 돌아오지 않아 그 줄의 JaCoCo 프로브가 끝내 실행되지 않는다. 그래서 non-daemon 스레드가 System.exit(code)를 부르고 main은 정상 반환한다. 이렇게 해야 line+branch 100% 게이트(AO-32)를 통과한다.
//   관련: AO-32, MainCoverageTest
//   개념: JaCoCo 프로브
