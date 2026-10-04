import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The pattern ladder: which rung a task needs, and whether its value pays for the rung.
 *
 * <p>Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
 * complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
 * interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
 * the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
 * a measurement of your workload, and the rule is the course's own teaching model.
 */
public final class PatternLadder {
    record Task(String name, boolean oneStep, int steps, boolean needsExternal, boolean stepsKnown, boolean independentParts, double value, double chatCost) {}

    static final Map<String, Integer> MULTIPLIER = Map.of("plain call", 1, "augmented call", 1, "agent", 4, "multi-agent", 15); // a workflow costs one chat per step

    static final List<Task> TASKS = List.of(
        new Task("classify ticket", true, 1, false, true, false, 0.05, 0.02),
        new Task("answer from policy", true, 1, true, true, false, 0.40, 0.02),
        new Task("claims intake", false, 4, true, true, false, 6.00, 0.02),
        new Task("investigate outage", false, 0, true, false, false, 40.00, 0.02),
        new Task("market research brief", false, 0, true, false, true, 25.00, 0.02),
        new Task("trivia round-up", false, 0, true, false, true, 0.05, 0.02));

    /** The lowest rung that fits: a call, an augmented call, a workflow, an agent, and a team only when its value covers the team's cost. */
    static String choosePattern(Task task) {
        if (task.oneStep()) return task.needsExternal() ? "augmented call" : "plain call";
        if (task.stepsKnown()) return "workflow";
        if (task.independentParts() && task.value() >= MULTIPLIER.get("multi-agent") * task.chatCost()) return "multi-agent";
        return "agent";
    }

    static double cost(Task task, String pattern) {
        int multiplier = pattern.equals("workflow") ? task.steps() : MULTIPLIER.get(pattern);
        return multiplier * task.chatCost();
    }

    public static void main(String[] args) {
        for (Task task : TASKS) {
            String pattern = choosePattern(task);
            double price = cost(task, pattern);
            System.out.println(String.format(Locale.ROOT, "%s: %s, cost %.2f, value %.2f, pays: %s", task.name(), pattern, price, task.value(), task.value() >= price ? "True" : "False"));
        }
    }
}
