import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.
 *
 * The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
 */
public class ControlChain {
    private static final System.Logger LOG = System.getLogger(ControlChain.class.getName());
    record Action(String name, String consequence) {}

    record Answer(String text, int confidence, String quote) {}

    record Case(String label, Action action, Answer answer, boolean screenUp) {}

    record Erased(Map<String, String> kept, int removed) {}

    static final int AUTO_CONFIDENCE = 95;

    /** Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed. */
    static String route(Action action, Answer answer, String source, boolean screenUp, int confidenceMin) {
        LOG.log(System.Logger.Level.DEBUG, "route input: {0}", action);
        if (!screenUp && action.consequence().equals("high")) return "hold: screen down";
        String flag = screenUp ? "" : " (unscreened)";
        if (!source.contains(answer.quote())) return "hold: unsupported" + flag;
        if (action.consequence().equals("high")) return "human" + flag;
        return (answer.confidence() >= confidenceMin ? "auto" : "review") + flag;
    }

    static String route(Action action, Answer answer, String source, boolean screenUp) {
        return route(action, answer, source, screenUp, AUTO_CONFIDENCE);
    }

    /** Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text. */
    static Map<String, Object> auditRecord(String requestId, Action action, String outcome, String text) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("request", requestId);
        record.put("action", action.name());
        record.put("consequence", action.consequence());
        record.put("outcome", outcome);
        record.put("chars", text.length());
        record.put("content_stored", false);
        return record;
    }

    /** Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone. */
    static Erased erase(Map<String, String> vault, String subject) {
        Map<String, String> kept = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : vault.entrySet()) if (!e.getValue().equals(subject)) kept.put(e.getKey(), e.getValue());
        return new Erased(kept, vault.size() - kept.size());
    }

    static String flag(Object value) {
        return value instanceof Boolean b ? (b ? "True" : "False") : String.valueOf(value);
    }

    public static void main(String[] args) {
        String source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded.";
        Action reply = new Action("draft_reply", "low");
        Action refund = new Action("issue_refund", "high");
        String supported = "Water damage is covered up to 5,000 per claim.";
        Answer good = new Answer("Water damage is covered up to 5,000.", 99, supported);
        Answer edge = new Answer("Water damage is covered up to 5,000.", 95, supported);
        Answer unsure = new Answer("Water damage is covered up to 5,000.", 94, supported);
        Answer wrong = new Answer("Water damage is covered up to 8,000.", 99, "Water damage is covered up to 8,000 per claim.");
        List<Case> cases = List.of(
            new Case("screen up, refund, supported", refund, good, true),
            new Case("screen up, reply, confidence 99", reply, good, true),
            new Case("screen up, reply, confidence 95", reply, edge, true),
            new Case("screen up, reply, confidence 94", reply, unsure, true),
            new Case("screen up, reply, confident but unsupported", reply, wrong, true),
            new Case("screen down, refund", refund, good, false),
            new Case("screen down, reply", reply, good, false));
        for (Case c : cases) System.out.println(c.label() + ": " + route(c.action(), c.answer(), source, c.screenUp()));
        Map<String, Object> record = auditRecord("r-1001", refund, "human", good.text());
        System.out.println("audit record: " + record.entrySet().stream().map(e -> e.getKey() + "=" + flag(e.getValue())).collect(Collectors.joining(", ")));
        Map<String, String> vault = new LinkedHashMap<>();
        vault.put("<EMAIL_1>", "person-a");
        vault.put("<EMAIL_2>", "person-b");
        vault.put("<MEMBER_1>", "person-a");
        Erased erased = erase(vault, "person-a");
        System.out.println("erasure removed " + erased.removed() + " of " + vault.size() + " mappings; the audit entries stay, with " + erased.kept().size() + " token still linkable");
    }
}
