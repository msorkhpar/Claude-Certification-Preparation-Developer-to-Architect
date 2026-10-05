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
        for (Finding f : findings) if (f.area().equals(area) && f.fact().equals(fact)) return out;
        out.add(new Finding(area, fact, location));
        return out;
    }

    static String renderScratchpad(List<Finding> findings) {
        List<String> areas = new ArrayList<>();
        for (Finding f : findings) if (!areas.contains(f.area())) areas.add(f.area());
        List<String> blocks = new ArrayList<>();
        for (String area : areas) {
            List<String> lines = new ArrayList<>();
            for (Finding f : findings) if (f.area().equals(area)) lines.add("- " + f.fact() + " (" + f.location() + ")");
            blocks.add("## " + area + "\n" + String.join("\n", lines));
        }
        return String.join("\n\n", blocks);
    }

    static Manifest buildManifest(List<AgentEntry> agents) {
        LOG.log(System.Logger.Level.DEBUG, "buildManifest input: {0}", agents);
        Set<String> names = new HashSet<>();
        for (AgentEntry a : agents) if (!names.add(a.name())) throw new IllegalArgumentException("duplicate agent name");
        for (AgentEntry a : agents) if (!STATUSES.contains(a.status())) throw new IllegalArgumentException("unknown status " + a.status());
        List<AgentEntry> sorted = new ArrayList<>(agents);
        sorted.sort(Comparator.comparing(AgentEntry::name));
        return new Manifest(1, sorted);
    }

    static List<Action> resumePlan(Manifest manifest, Set<String> existingFiles) {
        List<Action> plan = new ArrayList<>();
        for (AgentEntry a : manifest.agents()) {
            String action;
            if (!existingFiles.contains(a.stateFile())) action = "restart";
            else if (a.status().equals("done")) action = "reuse";
            else action = "resume";
            plan.add(new Action(a.name(), action));
        }
        return plan;
    }

    static String resumePrompt(String task, List<String> stateLines) {
        if (stateLines.isEmpty()) return task;
        List<String> bullets = new ArrayList<>();
        for (String line : stateLines) bullets.add("- " + line);
        return task + "\n\nState from the last run:\n" + String.join("\n", bullets) + "\nContinue from the first unfinished step.";
    }

    static String compactCommand(List<String> keep) {
        return keep.isEmpty() ? "/compact" : "/compact Focus on " + String.join(", ", keep);
    }
}
