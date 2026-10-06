import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */
final class ContextBuilder {
    private static final System.Logger LOG = System.getLogger(ContextBuilder.class.getName());
    private ContextBuilder() {}

    record Fact(String value, String asOf, List<String> superseded) {}

    record FactEntry(String customer, String name, String value, String asOf) {}

    record Message(String role, String kind, String id, String text) {}

    static int estimateTokens(String text) {
        return (text.length() + 3) / 4;
    }

    static Map<String, String> trimRecord(Map<String, String> record, List<String> keep) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String k : keep) if (record.containsKey(k)) out.put(k, record.get(k));
        return out;
    }

    static Map<String, Fact> updateFacts(Map<String, Fact> facts, String name, String value, String asOf) {
        Map<String, Fact> next = new LinkedHashMap<>();
        for (Map.Entry<String, Fact> e : facts.entrySet()) next.put(e.getKey(), new Fact(e.getValue().value(), e.getValue().asOf(), new ArrayList<>(e.getValue().superseded())));
        Fact current = next.get(name);
        if (current == null) {
            next.put(name, new Fact(value, asOf, new ArrayList<>()));
        } else if (asOf.compareTo(current.asOf()) >= 0) {
            List<String> history = new ArrayList<>(current.superseded());
            history.add(current.value() + "@" + current.asOf());
            next.put(name, new Fact(value, asOf, history));
        } else {
            List<String> history = new ArrayList<>(current.superseded());
            history.add(value + "@" + asOf);
            next.put(name, new Fact(current.value(), current.asOf(), history));
        }
        return next;
    }

    static String buildContext(String customer, List<FactEntry> facts, String summary, List<Message> recent) {
        LOG.log(System.Logger.Level.DEBUG, "buildContext input: {0}", customer);
        List<String> parts = new ArrayList<>();
        List<String> mine = new ArrayList<>();
        for (FactEntry f : facts) if (f.customer().equals(customer)) mine.add(f.name() + ": " + f.value() + " (as of " + f.asOf() + ")");
        if (!mine.isEmpty()) parts.add("## Case facts\n" + String.join("\n", mine));
        parts.add("## Summary so far\n" + summary);
        List<String> lines = new ArrayList<>();
        for (Message m : recent) lines.add(m.role() + ": " + m.text());
        parts.add("## Recent messages\n" + String.join("\n", lines));
        return String.join("\n\n", parts);
    }

    static List<String> missingFromSummary(String summary, List<FactEntry> facts) {
        List<String> missing = new ArrayList<>();
        for (FactEntry f : facts) if (!summary.contains(f.value())) missing.add(f.name());
        return missing;
    }

    static List<Message> window(List<Message> messages, int budget) {
        List<List<Message>> units = new ArrayList<>();
        int i = 0;
        while (i < messages.size()) {
            Message m = messages.get(i);
            if (m.kind().equals("tool_use") && i + 1 < messages.size() && messages.get(i + 1).kind().equals("tool_result") && messages.get(i + 1).id().equals(m.id())) {
                units.add(List.of(m, messages.get(i + 1)));
                i += 2;
            } else {
                units.add(List.of(m));
                i += 1;
            }
        }
        List<List<Message>> kept = new ArrayList<>();
        int used = 0;
        for (int u = units.size() - 1; u >= 0; u--) {
            int cost = 0;
            for (Message x : units.get(u)) cost += estimateTokens(x.text());
            if (used + cost > budget) break;
            kept.add(0, units.get(u));
            used += cost;
        }
        List<Message> out = new ArrayList<>();
        for (List<Message> unit : kept) out.addAll(unit);
        return out;
    }
}
