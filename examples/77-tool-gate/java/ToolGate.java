import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
 * an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.
 *
 * <p>The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
 * The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
 */
public final class ToolGate {
    private static final System.Logger LOG = System.getLogger(ToolGate.class.getName());
    record Proposal(String name, List<String> permissions, Map<String, Object> result) {}

    record Decision(String decision, List<String> why) {}

    static final List<String> DENIED = List.of("network", "run_process");
    static final List<String> NEEDS_APPROVAL = List.of("write_files");
    static final int MAX_CHARS = 200;

    static final List<Proposal> PROPOSALS = List.of(
        new Proposal("read_report", List.of("read_files"), Map.of("headline", "Q3 up 4%", "rows", 12)),
        new Proposal("write_summary", List.of("read_files", "write_files"), Map.of("headline", "Q3 up 4%")),
        new Proposal("fetch_prices", List.of("network"), Map.of("headline", "x", "rows", 1)),
        new Proposal("tidy_up", List.of("write_files"), Map.of("headline", "x", "rows", 1)));
    static final Map<String, Boolean> REVIEWER = Map.of("write_summary", true, "tidy_up", false); // the person's answers, by tool name

    /** Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic. */
    static Decision decide(List<String> permissions) {
        LOG.log(System.Logger.Level.DEBUG, "decide input: {0}", permissions);
        List<String> denied = permissions.stream().filter(DENIED::contains).toList();
        if (!denied.isEmpty()) return new Decision("refused", denied);
        List<String> gated = permissions.stream().filter(NEEDS_APPROVAL::contains).toList();
        if (!gated.isEmpty()) return new Decision("needs_approval", gated);
        return new Decision("auto", List.of());
    }

    /** The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text. */
    static List<String> checkOutput(Map<String, Object> result) {
        List<String> problems = new ArrayList<>();
        if (!result.containsKey("headline")) problems.add("missing: headline");
        if (!result.containsKey("rows")) problems.add("missing: rows");
        if (result.containsKey("headline") && !(result.get("headline") instanceof String)) problems.add("type: headline");
        if (result.containsKey("rows") && !(result.get("rows") instanceof Integer)) problems.add("type: rows");
        int chars = result.values().stream().filter(v -> v instanceof String).mapToInt(v -> ((String) v).length()).sum();
        if (chars > MAX_CHARS) problems.add("too large");
        return problems;
    }

    public static void main(String[] args) {
        Map<String, String> log = new LinkedHashMap<>();
        for (Proposal proposal : PROPOSALS) {
            String name = proposal.name();
            Decision d = decide(proposal.permissions());
            if (d.decision().equals("refused")) {
                System.out.println(name + ": refused (" + String.join(", ", d.why()) + ")");
                log.put(name, "refused");
                continue;
            }
            if (d.decision().equals("needs_approval")) {
                boolean answer = REVIEWER.get(name);
                System.out.println(name + ": needs_approval -> " + (answer ? "approved" : "declined"));
                if (!answer) {
                    log.put(name, "declined");
                    continue;
                }
            } else {
                System.out.println(name + ": auto");
            }
            List<String> problems = checkOutput(proposal.result());
            log.put(name, problems.isEmpty() ? "ran" : "rejected");
            if (!problems.isEmpty()) System.out.println("  result of " + name + " rejected (" + String.join("; ", problems) + ")");
        }
        System.out.println("audit: " + PROPOSALS.size() + " proposals, " + log.values().stream().filter(s -> s.equals("ran")).count() + " ran, "
            + log.values().stream().filter(s -> s.equals("rejected")).count() + " rejected, " + log.values().stream().filter(s -> s.equals("refused")).count() + " refused, "
            + log.values().stream().filter(s -> s.equals("declined")).count() + " declined");
    }
}
