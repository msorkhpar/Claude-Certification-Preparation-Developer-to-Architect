import java.util.List;
import java.util.Set;

/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */
final class Recovery {
    private Recovery() {}

    static final List<String> STATUSES = List.of("done", "running", "failed");

    record Finding(String area, String fact, String location) {}

    record AgentEntry(String name, String stateFile, String status) {}

    record Manifest(int version, List<AgentEntry> agents) {}

    record Action(String name, String action) {}

    static List<Finding> addFinding(List<Finding> findings, String area, String fact, String location) {
        // TODO: a new list with the finding added unless the same fact is already recorded for the area.
        return null;
    }

    static String renderScratchpad(List<Finding> findings) {
        // TODO: Markdown text: one "## <area>" heading per area, in first-seen order, with one line per finding.
        return null;
    }

    static Manifest buildManifest(List<AgentEntry> agents) {
        // TODO: version 1 and the agents sorted by name; an error for a duplicate name or an unknown status.
        return null;
    }

    static List<Action> resumePlan(Manifest manifest, Set<String> existingFiles) {
        // TODO: an action for each agent of the manifest: reuse, resume or restart.
        return null;
    }

    static String resumePrompt(String task, List<String> stateLines) {
        // TODO: the prompt that continues a task from the state the last run exported.
        return null;
    }

    static String compactCommand(List<String> keep) {
        // TODO: the /compact command, telling it what to keep.
        return null;
    }
}
