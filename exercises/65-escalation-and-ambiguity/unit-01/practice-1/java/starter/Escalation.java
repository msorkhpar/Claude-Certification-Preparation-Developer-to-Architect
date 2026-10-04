import java.util.List;
import java.util.Map;

/** When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md. */
final class Escalation {
    private Escalation() {}

    record Case(boolean askedForPerson, int matches, boolean policyCovers, int attemptsWithoutProgress, String sentiment, int confidence) {}

    record Decision(String action, String reason, boolean acknowledge) {}

    record HandoffCase(String customerId, String issue, String rootCause, String amount, List<String> actions, String recommended, String transcript) {}

    static Decision decide(Case c) {
        return decide(c, 2);
    }

    static Decision decide(Case c, int maxAttempts) {
        // TODO: resolve, clarify or escalate, with the reason and whether the reply acknowledges the customer's frustration.
        return null;
    }

    static List<String> clarifyingFields(List<Map<String, String>> matches) {
        // TODO: the fields (never "id") on which the matching records differ, in the order of the first record.
        return null;
    }

    static String handoffText(HandoffCase c) {
        // TODO: the text of a hand-off: six labelled lines, no transcript; an error when the customer id or the issue is missing.
        return null;
    }
}
