import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
 * one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.
 *
 * <p>The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
 * shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
 */
public final class ResearchRun {
    static final List<String> REQUIRED = List.of("visual arts", "music", "writing", "film");

    record Finding(String claim, String value, String source, String date) {}

    record Task(String scope, String query) {}

    record SearchError(String type, String query, List<Finding> partial, List<String> alternatives, List<String> tried) {}

    /** A result: status "ok" with findings, or "error" with its context; recoveredFrom is set when an alternative query succeeded. */
    record Result(String status, List<Finding> findings, SearchError error, String recoveredFrom) {}

    record Done(Task task, Result result) {}

    record Report(String status, int covered, int findings, int errors, List<String> notes) {}

    /** What the web search subagent finds for a query. */
    static final Map<String, List<Finding>> SOURCES = new LinkedHashMap<>();
    static final Map<String, List<String>> ALTERNATIVES = Map.of("AI in film", List.of("AI in film production"));
    static final Map<String, String> PUBLISHED = Map.of("survey-a", "2025-02-01", "report-d", "2025-01-15"); // what the synthesis agent can check here

    static {
        SOURCES.put("AI in digital art", List.of(new Finding("studios using generative tools", "60%", "survey-a", "2025-02-01")));
        SOURCES.put("AI in graphic design", List.of(new Finding("designers using generative tools weekly", "48%", "survey-b", "2025-03-10")));
        SOURCES.put("AI in photography", List.of(new Finding("agencies labelling generated images", "yes", "policy-c", "2024-11-20")));
        SOURCES.put("AI in music", List.of(new Finding("labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15")));
        SOURCES.put("AI in writing", List.of(new Finding("publishers with an AI policy", "70%", "survey-e", "2025-04-02")));
        SOURCES.put("AI in film production", List.of(new Finding("studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12")));
    }

    /** The search subagent. A failure is returned with its type, the query, the partial results and what to try instead. */
    static Result search(String query, List<String> down) {
        if (down.contains(query) || !SOURCES.containsKey(query)) {
            return new Result("error", null, new SearchError("timeout", query, List.of(), ALTERNATIVES.getOrDefault(query, List.of()), List.of(query)), null);
        }
        return new Result("ok", SOURCES.get(query), null, null);
    }

    static Result search(String query) {
        return search(query, List.of());
    }

    /** The covered scopes and the gaps, in the order of the required scopes. */
    static List<List<String>> coverage(List<Task> plan) {
        List<String> covered = REQUIRED.stream().filter(s -> plan.stream().anyMatch(t -> t.scope().equals(s))).toList();
        return List.of(covered, REQUIRED.stream().filter(s -> !covered.contains(s)).toList());
    }

    /** The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out. */
    static List<Task> replan(List<Task> plan) {
        List<Task> out = new ArrayList<>(plan);
        for (String s : coverage(plan).get(1)) out.add(new Task(s, "AI in " + s));
        return out;
    }

    /** One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries. */
    static Result recover(Result result, List<String> down) {
        SearchError error = result.error();
        if (result.status().equals("ok") || error.alternatives().isEmpty()) return result;
        String alternative = error.alternatives().get(0);
        Result again = search(alternative, down);
        if (again.status().equals("ok")) return new Result("ok", again.findings(), null, error.query());
        List<String> tried = new ArrayList<>(error.tried());
        tried.add(alternative);
        return new Result("error", null, new SearchError(error.type(), error.query(), error.partial(), error.alternatives(), tried), null);
    }

    static List<Done> research(List<Task> plan, List<String> down) {
        return plan.stream().map(t -> new Done(t, recover(search(t.query(), down), down))).toList();
    }

    /** The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator. */
    static String verifyFact(String kind, String source, String value) {
        if (kind.equals("date")) return value.equals(PUBLISHED.get(source)) ? "confirmed" : "mismatch";
        return "needs_search";
    }

    static Report report(List<Done> results) {
        List<String> covered = REQUIRED.stream().filter(s -> results.stream().anyMatch(r -> r.task().scope().equals(s) && r.result().status().equals("ok"))).toList();
        List<String> notes = results.stream().filter(r -> r.result().status().equals("error"))
            .map(r -> r.task().scope() + " not covered: timeout on " + r.result().error().tried().stream().map(q -> "'" + q + "'").collect(Collectors.joining(" and on "))).toList();
        int findings = results.stream().filter(r -> r.result().status().equals("ok")).mapToInt(r -> r.result().findings().size()).sum();
        return new Report(covered.size() == REQUIRED.size() ? "complete" : "partial", covered.size(), findings, notes.size(), notes.isEmpty() ? List.of("nothing left uncovered") : notes);
    }

    public static void main(String[] args) {
        List<Task> narrow = List.of("AI in digital art", "AI in graphic design", "AI in photography").stream().map(q -> new Task("visual arts", q)).toList();
        List<List<String>> c = coverage(narrow);
        System.out.println("plan 1: " + narrow.size() + " subtasks, scopes covered " + c.get(0).size() + " of " + REQUIRED.size() + ", gaps: " + String.join(", ", c.get(1)));
        List<Task> plan = replan(narrow);
        c = coverage(plan);
        System.out.println("plan 2: " + plan.size() + " subtasks, scopes covered " + c.get(0).size() + " of " + REQUIRED.size() + ", gaps: " + (c.get(1).isEmpty() ? "none" : String.join(", ", c.get(1))));
        Result first = search("AI in film");
        SearchError e = first.error();
        System.out.println("search '" + e.query() + "': " + first.status() + " " + e.type() + ", " + e.partial().size() + " partial, alternative '" + e.alternatives().get(0) + "'");
        Done recovered = research(plan, List.of()).stream().filter(r -> r.result().recoveredFrom() != null).findFirst().orElseThrow();
        System.out.println("recovered: '" + recovered.result().recoveredFrom() + "' -> '" + ALTERNATIVES.get(recovered.result().recoveredFrom()).get(0) + "' scope " + recovered.task().scope() + ", " + recovered.result().findings().size() + " finding");
        List<String> verdicts = List.of(verifyFact("date", "survey-a", "2025-02-01"), verifyFact("date", "report-d", "2025-01-15"), verifyFact("statistic", "survey-a", "60%"));
        System.out.println("verify_fact: " + verdicts.stream().filter(v -> v.equals("confirmed")).count() + " confirmed here, " + verdicts.stream().filter(v -> v.equals("needs_search")).count() + " sent back to the coordinator");
        Map<String, List<String>> runs = new LinkedHashMap<>();
        runs.put("all sources up", List.of());
        runs.put("film search down for good", List.of("AI in film", "AI in film production"));
        runs.forEach((label, down) -> {
            Report r = report(research(plan, down));
            System.out.println("report (" + label + "): status=" + r.status() + ", covered=" + r.covered() + "/" + REQUIRED.size() + ", findings=" + r.findings() + ", errors=" + r.errors());
            System.out.println("  note: " + String.join("; ", r.notes()));
        });
    }
}
