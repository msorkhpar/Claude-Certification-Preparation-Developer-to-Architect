import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lint a GitHub Actions workflow that runs Claude Code, against what the Claude Code GitHub Actions documentation recommends.
 *
 * <p>The checks are the ones the documentation states (read on 2026-10-03): keep keys in GitHub Secrets, grant only the permissions
 * the job needs, use the v1 action, cap the work of each run with --max-turns, a timeout and concurrency control, start runners only on
 * comments that mention @claude, and check the repository out before a skill from it can run. Reading the workflow is plain data work:
 * nothing here needs a runner, a key or a network. The workflow is YAML, read with Jackson.
 */
public final class WorkflowLint {
    static final String CLAUDE_ACTION = "anthropics/claude-code-action";

    /** A finding: the job (or "-"), a rule id and a message. */
    record Finding(String job, String rule, String message) {}

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseYaml(String text) throws IOException {
        return new ObjectMapper(new YAMLFactory()).readValue(text, LinkedHashMap.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> steps(Map<String, Object> job) {
        return job.get("steps") instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    static List<Map<String, Object>> claudeSteps(Map<String, Object> job) {
        return steps(job).stream().filter(s -> str(s.get("uses")).startsWith(CLAUDE_ACTION)).toList();
    }

    /** Findings in the order they are found. */
    static List<Finding> lint(String text) throws IOException {
        Map<String, Object> wf = parseYaml(text);
        List<Finding> findings = new ArrayList<>();
        if (Pattern.compile("sk-ant-[A-Za-z0-9_-]{8,}").matcher(text).find()) findings.add(new Finding("-", "literal-key", "an API key is written into the file: use ${{ secrets.NAME }}"));
        if (!wf.containsKey("concurrency")) findings.add(new Finding("-", "no-concurrency", "no concurrency group: parallel runs are not limited"));
        Object triggers = wf.get("on");
        List<Object> names = triggers instanceof Map<?, ?> m ? new ArrayList<>(m.keySet()) : triggers instanceof List<?> l ? new ArrayList<>(l) : java.util.Collections.singletonList(triggers);
        for (Map.Entry<String, Object> entry : map(wf.get("jobs")).entrySet()) {
            String name = entry.getKey();
            Map<String, Object> job = map(entry.getValue());
            List<Map<String, Object>> claude = claudeSteps(job);
            if (claude.isEmpty()) continue;
            for (Map<String, Object> step : claude) {
                if (!str(step.get("uses")).endsWith("@v1")) findings.add(new Finding(name, "action-version", step.get("uses") + " is not the v1 action: move @beta workflows to @v1"));
                Map<String, Object> with = map(step.get("with"));
                for (String field : List.of("anthropic_api_key", "claude_code_oauth_token")) {
                    if (with.containsKey(field) && !str(with.get(field)).startsWith("${{ secrets.")) findings.add(new Finding(name, "literal-key", field + " is not read from the secrets context"));
                }
                if (!str(with.get("claude_args")).contains("--max-turns")) findings.add(new Finding(name, "no-max-turns", "claude_args has no --max-turns: a run has no turn limit"));
                if (str(with.get("prompt")).startsWith("/")) {
                    List<Map<String, Object>> before = steps(job).subList(0, steps(job).indexOf(step));
                    if (before.stream().noneMatch(s -> str(s.get("uses")).startsWith("actions/checkout"))) {
                        findings.add(new Finding(name, "no-checkout", "a skill from the repository needs actions/checkout before the Claude step"));
                    }
                }
            }
            if (!job.containsKey("permissions") || job.get("permissions") == null) {
                findings.add(new Finding(name, "no-permissions", "no permissions block: grant only what the job needs"));
            } else if (name.toLowerCase().contains("review") && "write".equals(map(job.get("permissions")).get("contents"))) {
                findings.add(new Finding(name, "review-writes", "a review job has contents: write; reading is enough to review"));
            }
            if (!job.containsKey("timeout-minutes")) findings.add(new Finding(name, "no-timeout", "no timeout-minutes: a stuck run keeps the runner"));
            if (names.contains("issue_comment") && !str(job.get("if")).contains("@claude")) {
                findings.add(new Finding(name, "unguarded-trigger", "the job has no if: contains(..., '@claude'): a runner starts on every comment"));
            }
        }
        return findings;
    }

    static final String BAD = """
        name: Claude
        on:
          issue_comment:
            types: [created]
        jobs:
          review:
            runs-on: ubuntu-latest
            permissions:
              contents: write
            steps:
              - uses: anthropics/claude-code-action@beta
                with:
                  anthropic_api_key: sk-ant-api03-EXAMPLEEXAMPLE
                  prompt: "/code-review"
        """;

    static final String GOOD = """
        name: Claude review
        on:
          pull_request:
            types: [opened, synchronize]
        concurrency:
          group: claude-${{ github.event.pull_request.number }}
          cancel-in-progress: true
        jobs:
          review:
            runs-on: ubuntu-latest
            timeout-minutes: 15
            permissions:
              contents: read
              pull-requests: write
              id-token: write
            steps:
              - uses: actions/checkout@v6
                with:
                  fetch-depth: 1
              - uses: anthropics/claude-code-action@v1
                with:
                  anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
                  prompt: "/code-review"
                  claude_args: --max-turns 8
        """;

    public static void main(String[] args) throws IOException {
        for (Map.Entry<String, String> e : List.of(Map.entry("bad workflow", BAD), Map.entry("good workflow", GOOD))) {
            List<Finding> found = lint(e.getValue());
            System.out.println(e.getKey() + ": " + found.size() + " finding(s)");
            for (Finding f : found) System.out.println("  [" + f.rule() + "] " + f.job() + ": " + f.message());
        }
    }
}
