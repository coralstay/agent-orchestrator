package com.coralstay.orchestrator.backlog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * backlog.md CLI({@code backlog task view <id> --plain})를 감싸는 클라이언트.
 *
 * <p>파서는 backlog 1.51.0 출력 기준이며, 포맷 변화에 덜 깨지도록 "제목 줄 + 대시 밑줄" 섹션 헤더 단위로 느슨하게 읽는다.
 */
public final class BacklogClient {

    /** 수용 기준 한 항목. {@code - [x] #1 본문}. */
    public record Criterion(int index, boolean checked, String text) {}

    /** {@code task view --plain}에서 뽑은 값 객체. dependencies는 직접 의존 태스크 ID만 담는다. */
    public record Task(
            String id,
            String title,
            String status,
            List<Criterion> acceptanceCriteria,
            List<String> dependencies) {

        public Task {
            acceptanceCriteria = List.copyOf(acceptanceCriteria);
            dependencies = List.copyOf(dependencies);
        }
    }

    /** 외부 프로세스 실행 결과. */
    public record ProcessResult(int exitCode, String stdout, String stderr) {}

    /** {@code backlog}가 0이 아닌 종료코드로 끝났을 때. stderr를 그대로 담는다. */
    public static final class BacklogException extends RuntimeException {
        private final int exitCode;
        private final String stderr;

        public BacklogException(List<String> command, int exitCode, String stderr) {
            super(String.join(" ", command) + " 실패(exit " + exitCode + "): " + stderr.strip());
            this.exitCode = exitCode;
            this.stderr = stderr;
        }

        public int exitCode() {
            return exitCode;
        }

        public String stderr() {
            return stderr;
        }
    }

    private final Function<List<String>, ProcessResult> executor;

    /** 명령 실행기를 주입받는다. 테스트는 가짜 실행기를, 운영은 {@link #processExecutor(Path)}를 넘긴다. */
    public BacklogClient(Function<List<String>, ProcessResult> executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    /** {@code backlog task view <id> --plain}을 실행해 Task로 해석한다. 종료코드≠0이면 {@link BacklogException}. */
    public Task view(String id) {
        List<String> command = List.of("backlog", "task", "view", id, "--plain");
        ProcessResult result = executor.apply(command);
        if (result.exitCode() != 0) {
            throw new BacklogException(command, result.exitCode(), result.stderr());
        }
        return parse(result.stdout());
    }

    /** workingDir에서 ProcessBuilder로 명령을 실행하는 실행기. stdout·stderr를 동시에 읽어 파이프 교착을 피한다. */
    public static Function<List<String>, ProcessResult> processExecutor(Path workingDir) {
        return command -> {
            try {
                Process process = new ProcessBuilder(command).directory(workingDir.toFile()).start();
                process.getOutputStream().close();
                try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                    Future<String> stderr = pool.submit(() -> read(process.getErrorStream()));
                    String stdout = read(process.getInputStream());
                    int exitCode = process.waitFor();
                    return new ProcessResult(exitCode, stdout, stderr.get());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted: " + command, e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("stderr read failed: " + command, e.getCause());
            }
        };
    }

    private static String read(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static final Pattern TITLE = Pattern.compile("^Task (\\S+) - (.+)$");
    /** {@code Status: ○ To Do} — 앞의 상태 글리프(○ ◒ ✔ 등)는 버린다. */
    private static final Pattern STATUS =
            Pattern.compile("^Status:\\s*(?:[^\\p{L}\\p{N}\\s]+\\s+)?(.+?)\\s*$");
    private static final Pattern SECTION_RULE = Pattern.compile("^-{3,}$");
    private static final Pattern CRITERION = Pattern.compile("^- \\[([ xX])\\] #(\\d+) (.*)$");
    /** 의존성 트리의 1단계(직접 의존) 줄: {@code ├─ AO-2 - 제목 [상태]}. 들여쓴 줄은 전이 의존. */
    private static final Pattern DIRECT_DEPENDENCY = Pattern.compile("^[├└]─ (\\S+) - ");

    static final String SECTION_DEPENDENCIES = "Dependency Graph:";
    static final String SECTION_ACCEPTANCE = "Acceptance Criteria:";

    /** {@code --plain} 출력 전체를 Task로 해석한다. 부수효과 없는 순수 함수. */
    public static Task parse(String plainText) {
        List<String> lines = plainText.lines().toList();
        String id = null;
        String title = null;
        String status = null;
        for (String line : lines) {
            Matcher m;
            if (id == null && (m = TITLE.matcher(line)).matches()) {
                id = m.group(1);
                title = m.group(2).strip();
            } else if (status == null && (m = STATUS.matcher(line)).matches()) {
                status = m.group(1);
            }
        }
        if (id == null || status == null) {
            throw new IllegalArgumentException("backlog --plain 출력에서 Task/Status 줄을 찾지 못했습니다");
        }
        Map<String, List<String>> sections = sections(lines);
        return new Task(
                id,
                title,
                status,
                criteria(sections.getOrDefault(SECTION_ACCEPTANCE, List.of())),
                directDependencies(sections.getOrDefault(SECTION_DEPENDENCIES, List.of())));
    }

    /** "헤더 줄 + 대시 밑줄"로 시작하는 섹션들을 헤더 → 본문 줄로 나눈다. */
    static Map<String, List<String>> sections(List<String> lines) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        List<String> current = null;
        for (int i = 0; i < lines.size(); i++) {
            boolean isHeader = i + 1 < lines.size() && SECTION_RULE.matcher(lines.get(i + 1)).matches();
            if (isHeader) {
                current = new ArrayList<>();
                sections.put(lines.get(i).strip(), current);
                i++; // 밑줄 건너뜀
            } else if (current != null) {
                current.add(lines.get(i));
            }
        }
        return sections;
    }

    private static List<Criterion> criteria(List<String> body) {
        List<Criterion> result = new ArrayList<>();
        for (String line : body) {
            Matcher m = CRITERION.matcher(line.strip());
            if (m.matches()) {
                result.add(new Criterion(
                        Integer.parseInt(m.group(2)), !m.group(1).isBlank(), m.group(3).strip()));
            }
        }
        return result;
    }

    /** "Depends on" 블록에서 1단계 줄만 읽는다. "Dependents" 블록(역방향)은 무시한다. */
    private static List<String> directDependencies(List<String> body) {
        List<String> result = new ArrayList<>();
        boolean inDependsOn = false;
        for (String line : body) {
            if (line.startsWith("Depends on")) {
                inDependsOn = true;
                continue;
            }
            if (inDependsOn) {
                if (line.isBlank() || line.startsWith("Dependents")) {
                    break;
                }
                Matcher m = DIRECT_DEPENDENCY.matcher(line);
                if (m.find()) {
                    result.add(m.group(1));
                }
            }
        }
        return result;
    }
}
