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
        return agent.holds().stream().filter(t -> !agent.needs().contains(t)).toList();
    }

    private static List<String> risky(List<String> remove, Map<String, Tool> catalog) {
        return remove.stream().filter(t -> RISKY.contains(catalog.get(t).access())).toList();
    }

    private static List<String> dormant(Agent agent) {
        return agent.holds().stream().filter(t -> agent.needs().contains(t) && agent.used().getOrDefault(t, 0) == 0).toList();
    }

    static AuditResult audit(Agent agent, Map<String, Tool> catalog) {
        LOG.log(System.Logger.Level.DEBUG, "audit input: {0}", agent);
        List<String> remove = remove(agent);
        return new AuditResult(remove, risky(remove, catalog), agent.needs().stream().filter(t -> !agent.holds().contains(t)).toList(), dormant(agent));
    }

    private static int clamp(int keep) {
        return Math.max(3, Math.min(5, keep));
    }

    private static boolean defers(Map<String, Integer> tools) {
        return tools.size() >= 10 || tools.values().stream().mapToInt(Integer::intValue).sum() > 10000;
    }

    private static List<String> ranked(Map<String, Integer> tools, Map<String, Integer> usage, int keep) {
        List<String> sorted = new ArrayList<>(tools.keySet());
        sorted.sort(Comparator.comparing((String t) -> -usage.getOrDefault(t, 0)).thenComparing(t -> t));
        return new ArrayList<>(sorted.subList(0, Math.min(keep, sorted.size())));
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

    private static List<String> verdict(String team, Request request, Policy policy) {
        if (team == null) return List.of("deny", "unauthenticated");
        if (!policy.models().get(team).contains(request.model())) return List.of("deny", "model not allowed");
        if (request.tool() != null && !policy.tools().get(team).contains(request.tool())) return List.of("deny", "tool not allowed");
        if (request.recent() >= policy.limits().get(team)) return List.of("deny", "rate limited");
        return List.of("allow", "routed to " + policy.routes().getOrDefault(request.model(), request.model()));
    }

    private static Map<String, String> record(String team, Request request, String decision) {
        return Map.of("team", team == null ? "unknown" : team, "model", request.model(), "tool", request.tool() == null ? "none" : request.tool(), "decision", decision);
    }

    static Outcome gateway(Request request, Policy policy) {
        LOG.log(System.Logger.Level.DEBUG, "gateway input: {0}", request);
        String team = request.credential() == null ? null : policy.credentials().get(request.credential());
        List<String> verdict = verdict(team, request, policy);
        return new Outcome(verdict.get(0), verdict.get(1), record(team, request, verdict.get(0)));
    }
}
