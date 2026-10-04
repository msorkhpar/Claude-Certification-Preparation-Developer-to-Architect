import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a long support conversation should keep, and where it should sit.
 *
 * <p>The exam guide (task 5.1) names four risks: a progressive summary turns numbers, dates and the customer's stated expectations into vague prose; models attend well to the start and the end of a long input and may miss the middle;
 * tool results pile up in the context out of proportion to their use (40 fields in an order lookup, five of them wanted); and the whole history must be sent on each request. The Claude documentation on long-context prompts
 * (read 2026-10-04) says to put long documents at the top and the question at the end, which can improve quality in tests by up to 30 percent, and to structure documents with tags. The methods below show the bookkeeping;
 * nothing here calls a model, and the numbers come from the sample data, not from a measurement.
 */
public final class CaseFacts {
    static final Map<String, List<String>> TOOL_FIELDS = Map.of(
        "lookup_order", List.of("order_id", "purchase_date", "items", "return_window", "refund_amount"),
        "lookup_customer", List.of("customer_id", "tier"));

    record Fact(String name, String value, int day) {}

    record Document(String title, String text) {}

    static int tokens(String text) {
        return (text.length() + 3) / 4;
    }

    static String render(Map<String, String> record) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : record.entrySet()) parts.add(e.getKey() + "=" + e.getValue());
        return String.join(";", parts);
    }

    /** Keep what the next decision needs from a tool result, with its exact values. */
    static Map<String, String> shrink(String tool, Map<String, String> result) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String k : TOOL_FIELDS.get(tool)) if (result.containsKey(k)) out.put(k, result.get(k));
        return out;
    }

    /** Facts the conversation must not lose, as a block that goes into every request outside the summarised history. */
    static String caseFactsBlock(List<Fact> facts) {
        List<String> lines = new ArrayList<>();
        for (Fact f : facts) lines.add(f.name() + ": " + f.value() + " (as of day " + f.day() + ")");
        return "## Case facts\n" + String.join("\n", lines);
    }

    /** Key facts first, a short findings summary, the long documents under headers, the question last. */
    static String assemble(String block, List<String> findings, List<Document> documents, String question) {
        List<String> bullets = new ArrayList<>();
        for (String f : findings) bullets.add("- " + f);
        List<String> docs = new ArrayList<>();
        for (Document d : documents) docs.add("### " + d.title() + "\n" + d.text());
        return String.join("\n\n", block, "## Key findings\n" + String.join("\n", bullets), "## Documents\n" + String.join("\n", docs), "## Question\n" + question);
    }

    /** A value read days ago is re-read before it is acted on. */
    static boolean stale(int seenDay, int today, int maxAgeDays) {
        return today - seenDay > maxAgeDays;
    }

    public static void main(String[] args) {
        Map<String, String> order = new LinkedHashMap<>();
        order.put("order_id", "A-1042");
        order.put("purchase_date", "2026-09-02");
        order.put("items", "2 x kettle");
        order.put("return_window", "30 days");
        order.put("refund_amount", "$129.50");
        for (int n = 1; n < 36; n++) order.put(String.format("internal_%02d", n), String.format("backend-value-%02d", n));
        Map<String, String> small = shrink("lookup_order", order);
        System.out.println("lookup_order result: " + order.size() + " fields, " + render(order).length() + " characters, about " + tokens(render(order)) + " tokens");
        System.out.println("after shrinking: " + small.size() + " fields, " + render(small).length() + " characters, about " + tokens(render(small)) + " tokens");
        System.out.println("twenty lookups kept whole: " + 20 * tokens(render(order)) + " tokens; shrunk: " + 20 * tokens(render(small)) + " tokens");
        String block = caseFactsBlock(List.of(new Fact("refund_amount", "$129.50", 118), new Fact("return_deadline", "2026-09-30", 118)));
        System.out.println(block);
        String prompt = assemble(block, List.of("The order qualifies for a refund", "The deadline is the binding fact"), List.of(new Document("Policy", "..."), new Document("Order history", "...")), "What should the customer be told?");
        List<String> headings = new ArrayList<>();
        for (String line : prompt.split("\n")) if (line.startsWith("#")) headings.add(line);
        System.out.println("section order: " + String.join(" | ", headings));
        for (int seen : new int[] {118, 124}) System.out.println("refund_amount seen on day " + seen + ", today day 125, limit 3 days: " + (stale(seen, 125, 3) ? "read it again before acting" : "still fresh"));
    }
}
