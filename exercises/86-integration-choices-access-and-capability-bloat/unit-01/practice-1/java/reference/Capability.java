import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md. */
final class Capability {
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

    static AuditResult audit(Agent agent, Map<String, Tool> catalog) {
        List<String> remove = agent.holds().stream().filter(t -> !agent.needs().contains(t)).toList();
        return new AuditResult(remove, remove.stream().filter(t -> RISKY.contains(catalog.get(t).access())).toList(), agent.needs().stream().filter(t -> !agent.holds().contains(t)).toList(),
            agent.holds().stream().filter(t -> agent.needs().contains(t) && agent.used().getOrDefault(t, 0) == 0).toList());
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage) {
        return planLoading(tools, usage, 4, 350);
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage, int keep, int searchTokens) {
        keep = Math.max(3, Math.min(5, keep));
        List<String> names = new ArrayList<>(tools.keySet());
        int total = tools.values().stream().mapToInt(Integer::intValue).sum();
        if (names.size() < 10 && total <= 10000) return new Plan(false, names, List.of(), total);
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(Comparator.comparing((String t) -> -usage.getOrDefault(t, 0)).thenComparing(t -> t));
        List<String> ranked = new ArrayList<>(sorted.subList(0, Math.min(keep, sorted.size())));
        return new Plan(true, ranked, names.stream().filter(t -> !ranked.contains(t)).toList(), ranked.stream().mapToInt(tools::get).sum() + searchTokens);
    }

    static String chooseMechanism(int consumers, String counterpart, String path) {
        if (counterpart.equals("agent")) return "agent-to-agent";
        if (path.equals("fixed")) return "direct call in code";
        return consumers > 1 ? "MCP server" : "custom tool";
    }

    static String authorize(String tool, Set<String> userScopes, Set<String> agentScopes, Map<String, String> required) {
        if (!required.containsKey(tool)) return "deny: unknown tool";
        String scope = required.get(tool);
        if (!userScopes.contains(scope)) return "deny: user lacks " + scope;
        if (!agentScopes.contains(scope)) return "deny: agent lacks " + scope;
        return "allow";
    }

    static Outcome gateway(Request request, Policy policy) {
        String team = request.credential() == null ? null : policy.credentials().get(request.credential());
        String decision = "deny";
        String reason;
        if (team == null) reason = "unauthenticated";
        else if (!policy.models().get(team).contains(request.model())) reason = "model not allowed";
        else if (request.tool() != null && !policy.tools().get(team).contains(request.tool())) reason = "tool not allowed";
        else if (request.recent() >= policy.limits().get(team)) reason = "rate limited";
        else {
            decision = "allow";
            reason = "routed to " + policy.routes().getOrDefault(request.model(), request.model());
        }
        return new Outcome(decision, reason, Map.of("team", team == null ? "unknown" : team, "model", request.model(), "tool", request.tool() == null ? "none" : request.tool(), "decision", decision));
    }
}
