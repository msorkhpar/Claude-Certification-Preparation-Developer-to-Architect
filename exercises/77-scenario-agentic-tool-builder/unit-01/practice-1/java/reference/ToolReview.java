import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision. */
public final class ToolReview {
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

    public static Report review(Proposal proposal, Policy policy) {
        String name = proposal.name();
        String code = proposal.code();
        List<String> declared = proposal.permissions();
        List<String> findings = new ArrayList<>();
        if (!Pattern.matches("[a-z][a-z0-9_]{2,63}", name)) findings.add("bad_name");
        if (proposal.description().trim().split("\\s+").length < policy.minWords()) findings.add("short_description");
        if (proposal.timeoutS() > policy.maxTimeout()) findings.add("timeout");
        if (proposal.memoryMb() > policy.maxMemory()) findings.add("memory");
        List<String> forbidden = new TreeSet<>(FORBIDDEN.stream().filter(code::contains).toList()).stream().toList();
        List<String> used = new TreeSet<>(MARKERS.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(code::contains)).map(Map.Entry::getKey).toList()).stream().toList();
        List<String> refusals = new ArrayList<>();
        forbidden.forEach(t -> refusals.add("forbidden:" + t));
        used.stream().filter(p -> !declared.contains(p)).forEach(p -> refusals.add("undeclared:" + p));
        new TreeSet<>(declared).stream().filter(policy.denied()::contains).forEach(p -> refusals.add("denied:" + p));
        boolean gated = declared.stream().anyMatch(policy.approval()::contains);
        String decision;
        if (!refusals.isEmpty()) decision = "refuse";
        else if (!findings.isEmpty()) decision = "revise";
        else if (gated) decision = "approve_with_gate";
        else decision = "approve";
        return new Report(name, decision, refusals, findings, used, name + ": " + decision);
    }
}
