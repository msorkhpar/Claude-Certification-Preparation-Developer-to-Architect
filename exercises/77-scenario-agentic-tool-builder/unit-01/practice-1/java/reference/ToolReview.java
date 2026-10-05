import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision. */
public final class ToolReview {
    private static final System.Logger LOG = System.getLogger(ToolReview.class.getName());

    public record Proposal(String name, String description, List<String> permissions, int timeoutS, int memoryMb, String code) {}

    public record Policy(int minWords, int maxTimeout, int maxMemory, List<String> denied, List<String> approval) {}

    public record Report(String name, String decision, List<String> refusals, List<String> findings, List<String> used, String audit) {}

    static final List<String> FORBIDDEN = List.of("os.system", "subprocess", "eval(", "exec(", "__import__");
    // a deliberately crude scan of the code text: which permission each marker shows (the order of this table is not the order of the report)
    static final Map<String, List<String>> MARKERS = new LinkedHashMap<>();

    static {
        MARKERS.put("network", List.of("requests.", "urllib"));
        MARKERS.put("write_files", List.of(".write(", "shutil."));
        MARKERS.put("read_files", List.of("open(", ".read("));
        MARKERS.put("run_process", List.of("subprocess"));
    }

    /** The findings, in order: bad_name, short_description, timeout, memory. */
    static List<String> findingsOf(Proposal proposal, Policy policy) {
        List<String> findings = new ArrayList<>();
        if (!Pattern.matches("[a-z][a-z0-9_]{2,63}", proposal.name())) findings.add("bad_name");
        if (proposal.description().trim().split("\\s+").length < policy.minWords()) findings.add("short_description");
        if (proposal.timeoutS() > policy.maxTimeout()) findings.add("timeout");
        if (proposal.memoryMb() > policy.maxMemory()) findings.add("memory");
        return findings;
    }

    /** The forbidden tokens the code contains, in alphabetical order. */
    static List<String> forbiddenCalls(String code) {
        return new TreeSet<>(FORBIDDEN.stream().filter(code::contains).toList()).stream().toList();
    }

    /** The permissions the code text shows, in alphabetical order. */
    static List<String> permissionsUsed(String code) {
        return new TreeSet<>(MARKERS.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(code::contains)).map(Map.Entry::getKey).toList()).stream().toList();
    }

    /** forbidden:<token>, then undeclared:<permission>, then denied:<permission>, each group in alphabetical order. */
    static List<String> refusalsOf(List<String> forbidden, List<String> used, List<String> declared, List<String> denied) {
        List<String> refusals = new ArrayList<>();
        forbidden.forEach(t -> refusals.add("forbidden:" + t));
        used.stream().filter(p -> !declared.contains(p)).forEach(p -> refusals.add("undeclared:" + p));
        new TreeSet<>(declared).stream().filter(denied::contains).forEach(p -> refusals.add("denied:" + p));
        return refusals;
    }

    /** True when any declared permission needs a person's approval. */
    static boolean isGated(List<String> declared, List<String> approval) {
        return declared.stream().anyMatch(approval::contains);
    }

    /** refuse beats revise, revise beats approve_with_gate, otherwise approve. */
    static String decide(List<String> refusals, List<String> findings, boolean gated) {
        if (!refusals.isEmpty()) return "refuse";
        if (!findings.isEmpty()) return "revise";
        if (gated) return "approve_with_gate";
        return "approve";
    }

    public static Report review(Proposal proposal, Policy policy) {
        LOG.log(System.Logger.Level.DEBUG, "review input: {0}", proposal);
        String name = proposal.name();
        List<String> declared = proposal.permissions();
        List<String> findings = findingsOf(proposal, policy);
        List<String> used = permissionsUsed(proposal.code());
        List<String> refusals = refusalsOf(forbiddenCalls(proposal.code()), used, declared, policy.denied());
        String decision = decide(refusals, findings, isGated(declared, policy.approval()));
        return new Report(name, decision, refusals, findings, used, name + ": " + decision);
    }
}
