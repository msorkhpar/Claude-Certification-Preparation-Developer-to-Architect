import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.
 *
 * <p>The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
 * restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a map
 * stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
 */
public final class StateManifest {
    private static final System.Logger LOG = System.getLogger(StateManifest.class.getName());
    static final String MANIFEST = "state/manifest.txt";

    record Entry(String status, String path) {}

    record Finding(String fact, String where) {}

    record Step(String agent, String action) {}

    static int tokens(int chars) {
        return (chars + 3) / 4;
    }

    static int tokens(String text) {
        return tokens(text.length());
    }

    static Map<String, Entry> readManifest(Map<String, String> fs) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (String line : fs.getOrDefault(MANIFEST, "").split("\n")) {
            if (line.isEmpty()) continue;
            String[] p = line.split("\\|");
            entries.put(p[0], new Entry(p[1], p[2]));
        }
        return entries;
    }

    private static void writeManifest(Map<String, String> fs, Map<String, Entry> entries) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Entry> e : entries.entrySet()) lines.add(e.getKey() + "|" + e.getValue().status() + "|" + e.getValue().path());
        fs.put(MANIFEST, String.join("\n", lines));
    }

    /** The manifest is written when the agent starts, so that a crash leaves a trace. */
    static void start(Map<String, String> fs, String agent) {
        Map<String, Entry> entries = readManifest(fs);
        entries.put(agent, new Entry("running", "state/" + agent + ".md"));
        writeManifest(fs, entries);
    }

    /** The state file is written when the agent has something to keep, and the manifest then says done. */
    static void finish(Map<String, String> fs, String agent, List<Finding> findings) {
        Map<String, Entry> entries = readManifest(fs);
        String path = entries.get(agent).path();
        List<String> lines = new ArrayList<>();
        for (Finding f : findings) lines.add("- " + f.fact() + " (" + f.where() + ")");
        fs.put(path, String.join("\n", lines));
        entries.put(agent, new Entry("done", path));
        writeManifest(fs, entries);
    }

    static List<Step> recoveryPlan(Map<String, String> fs, List<String> planned) {
        Map<String, Entry> entries = readManifest(fs);
        List<Step> plan = new ArrayList<>();
        for (String agent : planned) {
            Entry e = entries.getOrDefault(agent, new Entry("running", "state/" + agent + ".md"));
            plan.add(new Step(agent, !fs.containsKey(e.path()) ? "restart" : e.status().equals("done") ? "reuse" : "resume"));
        }
        return plan;
    }

    /** What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again. */
    static String injectedState(Map<String, String> fs, List<Step> plan) {
        Map<String, Entry> entries = readManifest(fs);
        List<String> parts = new ArrayList<>();
        for (Step s : plan) if (s.action().equals("reuse") || s.action().equals("resume")) parts.add(s.agent() + ":\n" + fs.get(entries.get(s.agent()).path()));
        return String.join("\n", parts);
    }

    public static void main(String[] args) {
        Map<String, String> fs = new LinkedHashMap<>();
        Map<String, List<Finding>> work = new LinkedHashMap<>();
        work.put("auth", List.of(new Finding("sessions expire after 30 minutes", "auth/Session.java:18"), new Finding("tokens are signed in TokenSigner", "auth/TokenSigner.java:12"), new Finding("the login route is POST /login", "auth/Routes.java:7")));
        work.put("billing", List.of(new Finding("amounts are integer cents", "billing/Money.java:5"), new Finding("refunds go through RefundService", "billing/RefundService.java:41")));
        int total = 0;
        for (Map.Entry<String, List<Finding>> w : work.entrySet()) {
            start(fs, w.getKey());
            finish(fs, w.getKey(), w.getValue());
            total += w.getValue().size();
            System.out.println(w.getKey() + ": exported " + readManifest(fs).get(w.getKey()).path() + " (" + w.getValue().size() + " findings), manifest says " + readManifest(fs).get(w.getKey()).status());
        }
        start(fs, "search");
        System.out.println("search: manifest says running, state file never written (crash)");
        List<Step> plan = recoveryPlan(fs, List.of("auth", "billing", "search"));
        List<String> parts = new ArrayList<>();
        for (Step s : plan) parts.add(s.agent() + " " + s.action());
        System.out.println("recovery plan: " + String.join(", ", parts));
        String state = injectedState(fs, plan);
        int replay = tokens(3200) + tokens(2400) + tokens(1600);
        System.out.println("injected into the next phase: " + total + " findings from " + work.size() + " agents, about " + tokens(state) + " tokens");
        System.out.println("replaying the three transcripts instead: about " + replay + " tokens");
    }
}
