import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        if (c.askedForPerson()) return new Decision("escalate", "customer asked for a person", false);
        if (c.matches() > 1) return new Decision("clarify", "ambiguous customer match", false);
        if (!c.policyCovers()) return new Decision("escalate", "policy does not cover the request", false);
        if (c.attemptsWithoutProgress() >= maxAttempts) return new Decision("escalate", "no progress", false);
        return new Decision("resolve", "within capability", !c.sentiment().equals("calm"));
    }

    static List<String> clarifyingFields(List<Map<String, String>> matches) {
        List<String> out = new ArrayList<>();
        if (matches.size() < 2) return out;
        for (String field : matches.get(0).keySet()) {
            if (field.equals("id")) continue;
            Set<String> values = new LinkedHashSet<>();
            for (Map<String, String> m : matches) values.add(m.get(field));
            if (values.size() > 1) out.add(field);
        }
        return out;
    }

    private static String orElse(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    static String handoffText(HandoffCase c) {
        if (c.customerId() == null || c.customerId().isEmpty() || c.issue() == null || c.issue().isEmpty()) throw new IllegalArgumentException("a hand-off needs a customer id and an issue");
        List<String> lines = List.of(
            "Customer: " + c.customerId(),
            "Issue: " + c.issue(),
            "Root cause: " + orElse(c.rootCause(), "unknown"),
            "Amount: " + orElse(c.amount(), "unknown"),
            "Actions taken: " + orElse(c.actions() == null ? "" : String.join("; ", c.actions()), "none"),
            "Recommended action: " + orElse(c.recommended(), "review the case"));
        return String.join("\n", lines);
    }
}
