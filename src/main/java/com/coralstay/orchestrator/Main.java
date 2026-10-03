package com.coralstay.orchestrator;

import java.io.PrintStream;

/** CLI 진입점. 현재 지원 명령은 {@code run <taskId>} 하나다(decision-2). */
public final class Main {

    static final String USAGE = "usage: orchestrator run <taskId>";
    static final int EXIT_USAGE = 2;

    private Main() {}

    /** 파싱 결과: 성공한 명령 또는 usage 오류. */
    sealed interface ParseResult permits Command, UsageError {}

    /** 파싱된 명령. 이름과 대상 taskId. */
    record Command(String name, String taskId) implements ParseResult {}

    /** 인자가 잘못됐을 때의 사유. */
    record UsageError(String reason) implements ParseResult {}

    /** 인자 배열을 해석한다. 부수효과 없는 순수 함수. */
    static ParseResult parse(String[] args) {
        if (args == null || args.length == 0) {
            return new UsageError("명령이 없습니다");
        }
        String name = args[0];
        if (!"run".equals(name)) {
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

    /** parse 결과를 실행하고 exit code를 돌려준다. System.exit은 main에서만 부른다. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        return switch (parse(args)) {
            case Command command -> {
                // RunCommand 연결은 AO-7에서 한다.
                out.println(command.taskId());
                yield 0;
            }
            case UsageError error -> {
                err.println(error.reason());
                err.println(USAGE);
                yield EXIT_USAGE;
            }
        };
    }

    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        if (code != 0) {
            System.exit(code);
        }
    }
}
