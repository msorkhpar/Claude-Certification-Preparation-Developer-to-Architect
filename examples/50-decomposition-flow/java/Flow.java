import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Two decompositions with scripted model replies: a per-file pass followed by one cross-file pass, and an adaptive loop that plans each step from the last.
 *
 * <p>The "model" is a set of hand-written functions, so the output shows what each pass was given and what the control flow did with the answers, and nothing about
 * how a real model would review the code. The files, summaries and findings are illustrative.
 */
public final class Flow {
    static final Map<String, String> CHANGE = new LinkedHashMap<>();
    static final Map<String, String> SUMMARIES = new LinkedHashMap<>();

    static {
        CHANGE.put("api.py", "def get_user(id):\n    return db.find(id)\n");
        CHANGE.put("db.py", "def find(name):\n    return rows.get(name)\n");
        CHANGE.put("ui.py", "def show(user):\n    print(user['name'])\n");
        SUMMARIES.put("api.py", "get_user passes an id to db.find");
        SUMMARIES.put("db.py", "find looks a row up by name");
        SUMMARIES.put("ui.py", "show prints the name field");
    }

    /** A file's path and the summary of it. */
    record Summary(String path, String text) {}

    /** One step of the adaptive run: the subtask and what the worker answered. */
    record Step(String subtask, String result) {}

    /** What the planner answers: done with a summary, or the next subtask. */
    record Plan(boolean done, String next, String summary) {}

    /** The end of an adaptive run. */
    record Run(String status, List<Step> steps, String summary) {}

    /** One call per file: it is given this file and nothing else. */
    static String filePass(String path, String text) {
        System.out.println("  file pass " + path + ": " + text.lines().count() + " lines, no other file");
        return SUMMARIES.get(path);
    }

    /** One call over the summaries: the relations between files, never the text. */
    static List<String> crossPass(List<Summary> summaries) {
        int withheld = CHANGE.values().stream().mapToInt(String::length).sum();
        System.out.println("  cross pass: " + summaries.size() + " summaries, " + withheld + " characters of source withheld");
        Map<String, String> names = new LinkedHashMap<>();
        for (Summary s : summaries) names.put(s.path(), s.text());
        if (names.get("api.py").contains("id") && names.get("db.py").contains("name")) return List.of("api.py passes an id but db.py looks up by name");
        return List.of();
    }

    /** A scripted planner: what it answers depends on what the steps so far found. */
    static Plan plan(String goal, List<Step> steps) {
        List<String> done = steps.stream().map(Step::subtask).toList();
        if (steps.isEmpty()) return new Plan(false, "list the test files", null);
        if (done.contains("list the test files") && !done.contains("run the failing test")) return new Plan(false, "run the failing test", null);
        if (done.contains("read the module under test")) return new Plan(true, null, "the failure is in parse()");
        if (steps.get(steps.size() - 1).result().startsWith("1 failure")) return new Plan(false, "read the module under test", null);
        return new Plan(true, null, "nothing failed");
    }

    static String work(String subtask) {
        return Map.of("list the test files", "3 files", "run the failing test", "1 failure in test_parse", "read the module under test", "parse() drops the last field").get(subtask);
    }

    static Run runAdaptive(String goal, int maxSteps) {
        List<Step> steps = new ArrayList<>();
        while (true) {
            Plan reply = plan(goal, new ArrayList<>(steps));
            if (reply.done()) return new Run("done", steps, reply.summary());
            if (steps.size() >= maxSteps) return new Run("step_limit", steps, "");
            steps.add(new Step(reply.next(), work(reply.next())));
            System.out.println("  step " + steps.size() + ": " + reply.next() + " -> " + steps.get(steps.size() - 1).result());
        }
    }

    static Run runAdaptive(String goal) {
        return runAdaptive(goal, 5);
    }

    public static void main(String[] args) {
        System.out.println("per file, then across files:");
        List<Summary> summaries = new ArrayList<>();
        CHANGE.forEach((path, text) -> summaries.add(new Summary(path, filePass(path, text))));
        for (String finding : crossPass(summaries)) System.out.println("  finding: " + finding);
        System.out.println("\nadaptive, each step planned from the last:");
        Run run = runAdaptive("find why the parser test fails");
        System.out.println("  status: " + run.status() + " after " + run.steps().size() + " steps; " + run.summary());
    }
}
