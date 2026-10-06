import static harness.Show.py;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.
 *
 * <p>No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
 */
public final class Distribution {
    private static final System.Logger LOG = System.getLogger(Distribution.class.getName());
    static final Map<String, List<String>> CATALOG = new LinkedHashMap<>();
    static final Set<String> IRREVERSIBLE = Set.of("send_report");
    static final Map<String, String> ROLES = new LinkedHashMap<>();
    static final Set<String> NO_FORCING = Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    static {
        CATALOG.put("web_search", List.of("web"));
        CATALOG.put("fetch_page", List.of("web"));
        CATALOG.put("verify_fact", List.of("web", "synthesis"));
        CATALOG.put("load_document", List.of("documents"));
        CATALOG.put("extract_data_points", List.of("documents"));
        CATALOG.put("summarize_content", List.of("synthesis"));
        CATALOG.put("write_report", List.of("reports"));
        CATALOG.put("send_report", List.of("reports"));
        ROLES.put("searcher", "web");
        ROLES.put("analyst", "documents");
        ROLES.put("synthesizer", "synthesis");
        ROLES.put("reporter", "reports");
    }

    /** The settings of one request: the tool_choice, the tools offered, and whether the reply must be checked for the call. */
    record Turn(String toolChoice, List<String> tools, boolean checkReply) {}

    /** One block of a reply: its type and the text or tool name it holds. */
    record Block(String type, String value) {}

    static List<String> toolsFor(String role) {
        return CATALOG.entrySet().stream().filter(e -> e.getValue().contains(ROLES.get(role)) && !IRREVERSIBLE.contains(e.getKey())).map(Map.Entry::getKey).toList();
    }

    /** The request settings for a turn whose first call must be `forced`. */
    static Turn turnFor(String model, String forced, List<String> tools) {
        if (NO_FORCING.contains(model)) return new Turn("auto", List.of(forced), true);
        return new Turn("tool:" + forced, tools, false);
    }

    /** What switching from one request to another costs in prompt caching. */
    static String cacheCost(Turn before, Turn after) {
        if (!before.tools().equals(after.tools())) return "everything (the tool definitions changed)";
        if (!before.toolChoice().equals(after.toolChoice())) return "the cached messages (tool_choice changed)";
        return "nothing";
    }

    static boolean madeTheCall(List<Block> reply, String forced) {
        List<Block> calls = reply.stream().filter(b -> b.type().equals("tool_use")).toList();
        return !calls.isEmpty() && calls.get(0).value().equals(forced);
    }

    static String allowed(String tool, int amount, boolean approved, int cap) {
        if (!Set.of("refund", "lookup").contains(tool)) return "refused: unknown tool";
        if (tool.equals("lookup")) return "run";
        if (amount > cap) return "refused: above the limit of " + cap + ", send to a person";
        return approved ? "run" : "wait: a person must approve";
    }

    static String allowed(String tool, int amount, boolean approved) {
        return allowed(tool, amount, approved, 200);
    }

    public static void main(String[] args) {
        System.out.println("catalog: " + CATALOG.size() + " tools");
        for (String role : ROLES.keySet()) System.out.println("  " + role + ": " + String.join(", ", toolsFor(role)));
        System.out.println("a synthesizer that may also check one fact has verify_fact, and nothing else from the web");
        System.out.println("first call must be extract_metadata:");
        List<String> both = List.of("extract_metadata", "enrich");
        for (String model : List.of("claude-opus-5", "claude-sonnet-5-5")) {
            Turn turn = turnFor(model, "extract_metadata", both);
            System.out.println("  " + model + ": tool_choice=" + turn.toolChoice() + ", tools=" + py(turn.tools()) + ", check the reply=" + py(turn.checkReply()));
            System.out.println("    cost against a turn with auto and both tools: " + cacheCost(new Turn("auto", both, false), turn));
        }
        System.out.println("a reply to the fallback turn:");
        Object[][] replies = {
            {"text only", List.of(new Block("text", "I will look at the metadata."))},
            {"the right call", List.of(new Block("tool_use", "extract_metadata"))},
            {"another tool", List.of(new Block("tool_use", "enrich"))}};
        for (Object[] r : replies) {
            @SuppressWarnings("unchecked")
            List<Block> reply = (List<Block>) r[1];
            System.out.println("  " + r[0] + ": " + (madeTheCall(reply, "extract_metadata") ? "accept" : "re-ask once, then escalate"));
        }
        System.out.println("refund decisions:");
        Object[][] decisions = {{"lookup", 0, false}, {"refund", 150, false}, {"refund", 150, true}, {"refund", 400, true}, {"delete_account", 0, true}};
        for (Object[] d : decisions) {
            System.out.println("  " + d[0] + " " + d[1] + ", approved=" + ((Boolean) d[2] ? "yes" : "no") + ": " + allowed((String) d[0], (Integer) d[1], (Boolean) d[2]));
        }
    }
}
