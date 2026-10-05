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
        // TODO 1 of 6 (finish this to pass m1, e1): the trim. Receives a tool's record and the list of fields to keep.
        //   Return a new record with only those fields, in the order of the list, with their exact values, skipping a
        //   field that the record does not have. Example: record {id, status, notes}, keep [status, id] -> {status, id}.
        return new LinkedHashMap<>(record);
    }

    static Map<String, Fact> updateFacts(Map<String, Fact> facts, String name, String value, String asOf) {
        Map<String, Fact> next = new LinkedHashMap<>();
        for (Map.Entry<String, Fact> e : facts.entrySet()) next.put(e.getKey(), new Fact(e.getValue().value(), e.getValue().asOf(), new ArrayList<>(e.getValue().superseded())));
        Fact current = next.get(name);
        if (current == null) {
            next.put(name, new Fact(value, asOf, new ArrayList<>()));
        } else {
            // TODO 2 of 6 (finish this to pass e2, e3): the update of a known fact. When the new date is the same as or
            //   later than the stored one, replace the value and the date and append "oldvalue@olddate" to the history; when
            //   it is earlier, keep the current value and append "newvalue@newdate" to the history. Example: stored
            //   5@2026-01-02, new 7@2026-01-05 -> value 7, history [5@2026-01-02].
            next.put(name, new Fact(value, asOf, new ArrayList<>(current.superseded())));
        }
        return next;
    }

    static String buildContext(String customer, List<FactEntry> facts, String summary, List<Message> recent) {
        LOG.log(System.Logger.Level.DEBUG, "buildContext input: {0}", customer);
        List<String> parts = new ArrayList<>();
        List<String> mine = new ArrayList<>();
        // TODO 3 of 6 (finish this to pass e4): the facts of one customer. Receives the customer and all the fact
        //   entries. Keep only the entries whose customer is that customer. Example: facts of C1 and C2, customer C1 ->
        //   the C1 entries only.
        for (FactEntry f : facts) mine.add(f.name() + ": " + f.value() + " (as of " + f.asOf() + ")");
        // TODO 4 of 6 (finish this to pass e5): the case facts section. When there are facts for the customer, add first
        //   a section titled "## Case facts" with one line per fact, "name: value (as of date)". Example: one fact -> "##
        //   Case facts\norder: A-7 (as of 2026-01-02)", before the summary.
        parts.add("## Summary so far\n" + summary);
        List<String> lines = new ArrayList<>();
        for (Message m : recent) lines.add(m.role() + ": " + m.text());
        parts.add("## Recent messages\n" + String.join("\n", lines));
        return String.join("\n\n", parts);
    }

    static List<String> missingFromSummary(String summary, List<FactEntry> facts) {
        // TODO 5 of 6 (finish this to pass e6): the check of a summary. Receives the summary and the fact entries.
        //   Return the names of the facts whose exact value does not appear in the summary text. Example: fact order =
        //   A-7, summary "customer wants a refund" -> [order].
        return new ArrayList<>();
    }

    static List<Message> window(List<Message> messages, int budget) {
        List<List<Message>> units = new ArrayList<>();
        int i = 0;
        while (i < messages.size()) {
            Message m = messages.get(i);
            // TODO 6 of 6 (finish this to pass e7): the units of the window. Walk the messages in order: a tool_use
            //   message followed by the tool_result with the same id forms one unit of two, which is never split; any
            //   other message is a unit of one. Example: [user, tool_use t1, tool_result t1] -> two units.
            units.add(List.of(m));
            i += 1;
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
