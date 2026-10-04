import java.util.List;

/**
 * Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision.
 * Read statement.md for the fields of a proposal, of the policy and of the report, then replace the body of review().
 */
public final class ToolReview {
    public record Proposal(String name, String description, List<String> permissions, int timeoutS, int memoryMb, String code) {}

    public record Policy(int minWords, int maxTimeout, int maxMemory, List<String> denied, List<String> approval) {}

    public record Report(String name, String decision, List<String> refusals, List<String> findings, List<String> used, String audit) {}

    public static Report review(Proposal proposal, Policy policy) {
        return new Report("", "", List.of(), List.of(), List.of(), "");
    }
}
