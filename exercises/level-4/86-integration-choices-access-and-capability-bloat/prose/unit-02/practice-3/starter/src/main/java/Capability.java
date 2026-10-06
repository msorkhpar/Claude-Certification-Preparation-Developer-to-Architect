import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md. */
final class Capability {
    private static final System.Logger LOG = System.getLogger(Capability.class.getName());
    private Capability() {}

    /** A tool's access class and the size of its definition in tokens. */
    record Tool(String access, int tokens) {}

    /** What an agent holds, what its role needs and how often each tool was called. */
    record Agent(List<String> holds, List<String> needs, Map<String, Integer> used) {}

    /** The tools to remove, the risky ones among them, the needed tools the agent lacks and the held, needed tools nobody used. */
    record AuditResult(List<String> remove, List<String> risky, List<String> missing, List<String> dormant) {}

    /** Whether a search tool is used, the tools loaded now and deferred, and the tokens loaded up front. */
    record Plan(boolean search, List<String> loadNow, List<String> deferred, int tokens) {}

    /** A request to the gateway: who calls (null for nobody), which model, which tool (null for none) and how many requests the team sent this minute. */
    record Request(String credential, String model, String tool, int recent) {}

    /** The gateway's rules: credential to team, the models and tools each team may use, requests a minute and where each model name is routed. */
    record Policy(Map<String, String> credentials, Map<String, Set<String>> models, Map<String, Set<String>> tools, Map<String, Integer> limits, Map<String, String> routes) {}

    /** The gateway's decision, its reason and the record kept of it. */
    record Outcome(String decision, String reason, Map<String, String> audit) {}

    private static final Set<String> RISKY = Set.of("money", "destroy");

    private static List<String> remove(Agent agent) {
        // TODO 1 of 10 (unlocks m1): the tools to take away.
        // Receives the agent (`holds`, `needs`, `used`). Returns the held tools that the role does not need, in the order they are held.
        // Example: holds [read, refund], needs [read] -> [refund]
        return List.of();
    }

    private static List<String> risky(List<String> remove, Map<String, Tool> catalog) {
        // TODO 2 of 10 (unlocks m1): the risky tools among those to remove.
        // Receives the tools to remove and the catalog (tool to its access class and tokens). Returns those whose access class is in RISKY, in the same order.
        // Example: remove [refund, export], refund has access "money" and export has "read" -> [refund]
        return List.of();
    }

    private static List<String> dormant(Agent agent) {
        // TODO 3 of 10 (unlocks e1): the tools that are kept but never used.
        // Receives the agent. Returns the held tools that the role needs and that have no calls in `used` (a tool missing from `used` has none), in the order held. They are
        // reported and never removed.
        // Example: holds [a, b], needs [a, b], used {a: 5, b: 0} -> [b]
        return List.of();
    }

    static AuditResult audit(Agent agent, Map<String, Tool> catalog) {
        LOG.log(System.Logger.Level.DEBUG, "audit input: {0}", agent);
        List<String> remove = remove(agent);
        return new AuditResult(remove, risky(remove, catalog), agent.needs().stream().filter(t -> !agent.holds().contains(t)).toList(), dormant(agent));
    }

    private static int clamp(int keep) {
        // TODO 4 of 10 (unlocks e3): how many tools to keep loaded.
        // Receives the number asked for. Returns it limited to the range from 3 to 5.
        // Example: clamp(1) -> 3, clamp(8) -> 5, clamp(4) -> 4
        return keep;
    }

    private static boolean defers(Map<String, Integer> tools) {
        // TODO 5 of 10 (unlocks e2): must the definitions be deferred?
        // Receives the tools (name to the tokens of its definition). Returns true when there are 10 tools or more, or the definitions together are over 10000 tokens (10000 itself is fine).
        // Example: 9 tools of 100 tokens -> false; 10 tools -> true; two tools of 5000 and 5001 -> true
        return false;
    }

    private static List<String> ranked(Map<String, Integer> tools, Map<String, Integer> usage, int keep) {
        // TODO 6 of 10 (unlocks e3): the tools to load first.
        // Receives the tools, the usage counts (a tool missing from `usage` has 0) and how many to keep. Returns that many tool names, the most used first and by name among equals.
        // Example: usage {b: 9, a: 9, c: 1}, keep 2 -> [a, b]
        return new ArrayList<>(tools.keySet()).subList(0, Math.min(keep, tools.size()));
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage) {
        return planLoading(tools, usage, 4, 350);
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage, int keep, int searchTokens) {
        LOG.log(System.Logger.Level.DEBUG, "planLoading input: {0}", tools);
        List<String> names = new ArrayList<>(tools.keySet());
        if (!defers(tools)) return new Plan(false, names, List.of(), tools.values().stream().mapToInt(Integer::intValue).sum());
        List<String> first = ranked(tools, usage, clamp(keep));
        return new Plan(true, first, names.stream().filter(t -> !first.contains(t)).toList(), first.stream().mapToInt(tools::get).sum() + searchTokens);
    }

    static String chooseMechanism(int consumers, String counterpart, String path) {
        // TODO 7 of 10 (unlocks e4): how a capability is connected.
        // Receives the number of clients that will use it, the counterpart ("agent" or "tool") and the path ("fixed" or "model-chosen"). Decide in this order:
        // a counterpart that is an agent gives "agent-to-agent"; a fixed path gives "direct call in code"; otherwise "MCP server" for more than one client and "custom tool" for one.
        // Example: chooseMechanism(4, "tool", "model-chosen") -> "MCP server"
        return null;
    }

    static String authorize(String tool, Set<String> userScopes, Set<String> agentScopes, Map<String, String> required) {
        // TODO 8 of 10 (unlocks e5): whose rights a tool call uses.
        // Receives the tool name, the scopes of the user, the scopes of the agent and `required` (tool to the scope it needs). Returns "deny: unknown tool" for a tool
        // that is not in `required`; then "deny: user lacks <scope>"; then "deny: agent lacks <scope>"; otherwise "allow". Both the user and the agent must hold the scope.
        // Example: scope "refunds:write" held by the agent only -> "deny: user lacks refunds:write"
        return null;
    }

    private static List<String> verdict(String team, Request request, Policy policy) {
        // TODO 9 of 10 (unlocks e6): the decision and its reason.
        // Receives the team (null when the credential is unknown), the request (`model`, `tool` or null, `recent`) and the policy (`models`, `tools` and `limits` per team,
        // `routes`). Checks in this order and stops at the first that fails, returning List.of("deny", reason): "unauthenticated" when there is no team; "model not allowed";
        // "tool not allowed" (only when a tool is named); "rate limited" when `recent` is at or over the team's limit. Otherwise List.of("allow", "routed to <name>"), where the name is
        // `policy.routes()` for the model, or the model itself when it has no route.
        // Example: team "support", model "standard", no tool, recent 1, limit 2 -> [allow, routed to claude-sonnet-5-5]
        return List.of("allow", "");
    }

    private static Map<String, String> record(String team, Request request, String decision) {
        // TODO 10 of 10 (unlocks e7): the record kept of a decision.
        // Receives the team (null when unknown), the request and the decision. Returns a map with `team`, `model`, `tool` and `decision`: the team or "unknown", the model, the tool or
        // "none", and the decision. Nothing else: no prompt and no credential.
        // Example: team null, model "deep", tool null, "deny" -> {team: unknown, model: deep, tool: none, decision: deny}
        return Map.of();
    }

    static Outcome gateway(Request request, Policy policy) {
        LOG.log(System.Logger.Level.DEBUG, "gateway input: {0}", request);
        String team = request.credential() == null ? null : policy.credentials().get(request.credential());
        List<String> verdict = verdict(team, request, policy);
        return new Outcome(verdict.get(0), verdict.get(1), record(team, request, verdict.get(0)));
    }
}
