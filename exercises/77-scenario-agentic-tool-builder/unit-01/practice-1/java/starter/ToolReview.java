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

    /**
     * TODO 1 of 6 (unlocks e1, e2 and e3): the findings about the name, the description and the limits.
     * Receives the proposal and the policy. Returns a list, in this order, of {@code bad_name} (the name does not match {@code [a-z][a-z0-9_]{2,63}}),
     * {@code short_description} (fewer than {@code minWords} words), {@code timeout} ({@code timeoutS} above {@code maxTimeout}) and {@code memory} ({@code memoryMb} above {@code maxMemory}).
     * Example: a 3-word description with a policy minimum of 12 -> ["short_description"]
     */
    static List<String> findingsOf(Proposal proposal, Policy policy) {
        return new ArrayList<>();
    }

    /**
     * TODO 2 of 6 (unlocks e4): the forbidden tokens in the code.
     * Receives the code text. Returns the tokens of {@code FORBIDDEN} that it contains, in alphabetical order.
     * Example: "eval(text)\nos.system('ls')" -> ["eval(", "os.system"]
     */
    static List<String> forbiddenCalls(String code) {
        return List.of();
    }

    /**
     * TODO 3 of 6 (unlocks e5, e6 and e8): the permissions the code shows.
     * Receives the code text. Returns the permissions of {@code MARKERS} that have a marker in the code, in alphabetical order (the table is not in that order).
     * Example: "shutil.copy(a, b)\nopen(a).read()" -> ["read_files", "write_files"]
     */
    static List<String> permissionsUsed(String code) {
        return List.of();
    }

    /**
     * TODO 4 of 6 (unlocks e4 and e5): the refusals, in order.
     * Receives the forbidden tokens, the permissions used, the permissions declared and the permissions the policy denies. Returns
     * {@code forbidden:<token>} for each forbidden token, then {@code undeclared:<permission>} for each used permission that was not declared, then
     * {@code denied:<permission>} for each declared permission that is denied (alphabetical within each group).
     * Example: refusalsOf(List.of(), List.of("network"), List.of("read_files"), List.of("network")) -> ["undeclared:network"]
     */
    static List<String> refusalsOf(List<String> forbidden, List<String> used, List<String> declared, List<String> denied) {
        return new ArrayList<>();
    }

    /**
     * TODO 5 of 6 (unlocks e6): does a declared permission need approval?
     * Receives the declared permissions and the permissions that need approval. Returns true when any declared permission is among them.
     * Example: isGated(List.of("read_files", "write_files"), List.of("write_files")) -> true
     */
    static boolean isGated(List<String> declared, List<String> approval) {
        return false;
    }

    /**
     * TODO 6 of 6 (unlocks m1, e6 and e7): the decision.
     * Receives the refusals, the findings and {@code gated}. Returns {@code refuse} when there are refusals, otherwise {@code revise} when there are findings,
     * otherwise {@code approve_with_gate} when gated, otherwise {@code approve}.
     * Example: decide(List.of(), List.of("timeout"), true) -> "revise"
     */
    static String decide(List<String> refusals, List<String> findings, boolean gated) {
        return "";
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
