import static harness.Show.py;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Three decisions of a Claude Code session on a code-generation task: plan mode or direct execution, one message or several for a list of problems, and what a failing test run must say.
 *
 * <p>The rules are the ones the exam guide states for tasks 3.4 and 3.5 and the best-practices page confirms (read on 2026-10-03): plan when the change is large, architectural,
 * touches many files or has more than one valid approach; execute directly when you could describe the diff in one sentence; send interacting problems in one message and
 * independent problems one after another; and give the model the failing tests, with input and expected output, as the target. No model is called.
 */
public final class Refinement {
    /** What decides the mode of a task. */
    record Task(boolean diffInOneSentence, int files, boolean architectural, int approaches) {}

    /** A problem found in review and the ids of the problems it interacts with. */
    record Issue(String id, List<String> interactsWith) {
        Issue(String id) {
            this(id, List.of());
        }
    }

    /** One test run: the test's name, its input, the expected output and the actual one. */
    record Result(String name, Object input, Object expected, Object actual) {}

    /** Phases of the work: `plan` first when the change is large, architectural, spread over files or has several valid approaches. */
    static List<String> chooseMode(Task task) {
        boolean small = task.diffInOneSentence() && task.files() <= 1 && !task.architectural();
        if (small) return List.of("implement");
        if (task.architectural() || task.approaches() > 1 || task.files() > 1) return List.of("explore", "plan", "implement");
        return List.of("implement");
    }

    private static String find(Map<String, String> parent, String x) {
        while (!parent.get(x).equals(x)) {
            parent.put(x, parent.get(parent.get(x)));
            x = parent.get(x);
        }
        return x;
    }

    /** Messages to send, in order: problems that interact travel together, independent ones go one at a time. */
    static List<List<String>> groupFeedback(List<Issue> issues) {
        Map<String, String> parent = new HashMap<>();
        for (Issue i : issues) parent.put(i.id(), i.id());
        for (Issue issue : issues) {
            for (String other : issue.interactsWith()) {
                if (parent.containsKey(other)) parent.put(find(parent, issue.id()), find(parent, other));
            }
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (Issue issue : issues) groups.computeIfAbsent(find(parent, issue.id()), k -> new ArrayList<>()).add(issue.id());
        return new ArrayList<>(groups.values());
    }

    /** The message that closes the loop: each failing test with its input, the expected output and the actual one; nothing about the passing tests. */
    static String failureReport(List<Result> results) {
        List<Result> failing = results.stream().filter(r -> !Objects.equals(r.actual(), r.expected())).toList();
        if (failing.isEmpty()) return "All tests pass.";
        List<String> lines = new ArrayList<>();
        lines.add(failing.size() + " of " + results.size() + " tests fail:");
        for (Result r : failing) lines.add("- " + r.name() + ": input " + py(r.input()) + ", expected " + py(r.expected()) + ", got " + py(r.actual()));
        return String.join("\n", lines);
    }

    public static void main(String[] args) {
        Map<String, Task> tasks = new LinkedHashMap<>();
        tasks.put("rename a variable in one function", new Task(true, 1, false, 1));
        tasks.put("add a date check to one handler", new Task(true, 1, false, 1));
        tasks.put("split a monolith into services", new Task(false, 60, true, 3));
        tasks.put("migrate a library used in 45 files", new Task(false, 45, false, 1));
        tasks.forEach((name, task) -> System.out.println(name + ": " + String.join(" > ", chooseMode(task))));
        List<Issue> issues = List.of(new Issue("sort-order", List.of("pagination")), new Issue("pagination", List.of("sort-order")), new Issue("typo-in-label"), new Issue("null-date"));
        System.out.println("messages: " + py(groupFeedback(issues)));
        List<Result> results = List.of(
            new Result("keeps order", List.of(3, 1, 2), List.of(1, 2, 3), List.of(1, 2, 3)),
            new Result("empty list", List.of(), List.of(), null),
            new Result("null entry", Arrays.asList(2, null), List.of(2), Arrays.asList(2, null)));
        System.out.println(failureReport(results));
    }
}
