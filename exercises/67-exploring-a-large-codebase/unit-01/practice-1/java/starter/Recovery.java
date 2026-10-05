import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */
final class Recovery {
    private static final System.Logger LOG = System.getLogger(Recovery.class.getName());
    private Recovery() {}

    static final List<String> STATUSES = List.of("done", "running", "failed");

    record Finding(String area, String fact, String location) {}

    record AgentEntry(String name, String stateFile, String status) {}

    record Manifest(int version, List<AgentEntry> agents) {}

    record Action(String name, String action) {}

    static List<Finding> addFinding(List<Finding> findings, String area, String fact, String location) {
        List<Finding> out = new ArrayList<>(findings);
        // TODO 1 of 6 (finish this to pass m1): the scratchpad's duplicate rule. Receives the findings so far and a new
        //   one (area, fact, location). When a finding with the same area and the same fact already exists, return the
        //   findings unchanged; otherwise return them with the new one added last. Example: adding (api, "uses JWT") twice
        //   -> one entry.
        out.add(new Finding(area, fact, location));
        return out;
    }

    static String renderScratchpad(List<Finding> findings) {
        List<String> areas = new ArrayList<>();
        for (Finding f : findings) if (!areas.contains(f.area())) areas.add(f.area());
        List<String> blocks = new ArrayList<>();
        for (String area : areas) {
            List<String> lines = new ArrayList<>();
            // TODO 2 of 6 (finish this to pass e1): the lines of one area. Receives all the findings and one area.
            //   Return one line "- fact (location)" for each finding of that area only, in order. Example: findings of api
            //   and db, area api -> the api lines only.
            for (Finding f : findings) lines.add("- " + f.fact() + " (" + f.location() + ")");
            blocks.add("## " + area + "\n" + String.join("\n", lines));
        }
        return String.join("\n\n", blocks);
    }

    static Manifest buildManifest(List<AgentEntry> agents) {
        LOG.log(System.Logger.Level.DEBUG, "buildManifest input: {0}", agents);
        Set<String> names = new HashSet<>();
        // TODO 3 of 6 (finish this to pass e2): the checks of the manifest. Refuse with an error when two agents have
        //   the same name, and when an agent's status is not one of STATUSES. Example: two agents named a -> refused;
        //   status "paused" -> refused.
        List<AgentEntry> sorted = new ArrayList<>(agents);
        sorted.sort(Comparator.comparing(AgentEntry::name));
        return new Manifest(1, sorted);
    }

    static List<Action> resumePlan(Manifest manifest, Set<String> existingFiles) {
        List<Action> plan = new ArrayList<>();
        for (AgentEntry a : manifest.agents()) {
            String action;
            // TODO 4 of 6 (finish this to pass e3, e4, e5): the action for one agent. When its state file does not
            //   exist, restart; otherwise when its status is done, reuse it; otherwise resume it from the state file.
            //   Example: done with its file -> reuse; failed with its file -> resume; any status without its file ->
            //   restart.
            action = "skip";
            plan.add(new Action(a.name(), action));
        }
        return plan;
    }

    static String resumePrompt(String task, List<String> stateLines) {
        // TODO 5 of 6 (finish this to pass e6): the resume prompt. Receives the task and the state lines. With no state
        //   lines, return the task alone; otherwise the task, a blank line, "State from the last run:", one "- line" per
        //   state line, and "Continue from the first unfinished step.". Example: [] -> the task.
        return task;
    }

    static String compactCommand(List<String> keep) {
        // TODO 6 of 6 (finish this to pass e7): the compact command. Receives the things to keep. Return /compact when
        //   there are none, otherwise "/compact Focus on " followed by them joined with ", ". Example: [auth flow, schema]
        //   -> /compact Focus on auth flow, schema.
        return "/compact";
    }
}
