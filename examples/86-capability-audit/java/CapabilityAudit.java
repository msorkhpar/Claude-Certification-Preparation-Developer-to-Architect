import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.
 *
 * <p>The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
 * "Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
 */
public final class CapabilityAudit {
    private static final System.Logger LOG = System.getLogger(CapabilityAudit.class.getName());
    private CapabilityAudit() {}

    /** A tool's access class and the size of its definition in tokens. */
    record Tool(String access, int tokens) {}

    /** The outcome of an audit: what to remove, which of that is risky and what the role lacks. */
    record Audit(List<String> remove, List<String> risky, List<String> missing) {}

    /** How tool definitions load: with a search tool or not, which tools now and which later, and the tokens up front. */
    record Plan(boolean search, List<String> loadNow, List<String> deferred, int tokens) {}

    /** The gateway's rules: which credential belongs to which team, the models and requests a minute each team may use, and where a model name is routed. */
    record Policy(Map<String, String> credentials, Map<String, Set<String>> models, Map<String, Integer> limits, Map<String, String> routes) {}

    /** The gateway's decision, its reason and the record kept of it. */
    record Outcome(String decision, String reason, Map<String, String> audit) {}

    static final Map<String, Tool> CATALOG = new LinkedHashMap<>();
    static final Map<String, Integer> TOOLS = new LinkedHashMap<>();
    static final Map<String, Integer> USAGE = Map.of("github_create_issue", 90, "github_search_code", 70, "slack_post_message", 60, "github_get_pr", 50, "sentry_list_issues", 20, "grafana_query", 10);
    static final Policy POLICY = new Policy(Map.of("key-a", "support", "key-b", "research"), Map.of("support", Set.of("standard"), "research", Set.of("standard", "deep")), Map.of("support", 30, "research", 10),
        Map.of("standard", "claude-sonnet-5-5", "deep", "claude-opus-5-5"));
    private static final Set<String> RISKY = Set.of("money", "destroy");

    static {
        CATALOG.put("read_ticket", new Tool("read", 160));
        CATALOG.put("draft_reply", new Tool("draft", 220));
        CATALOG.put("issue_refund", new Tool("money", 240));
        CATALOG.put("delete_account", new Tool("destroy", 210));
        Map<String, List<String>> servers = new LinkedHashMap<>();
        servers.put("github", List.of("create_issue", "search_code", "get_pr", "list_prs", "merge_pr", "comment", "list_repos", "get_file"));
        servers.put("slack", List.of("post_message", "search", "list_channels", "get_thread", "react", "upload"));
        servers.put("sentry", List.of("list_issues", "get_event", "resolve", "assign", "search"));
        servers.put("grafana", List.of("query", "list_dashboards", "get_panel", "create_alert", "list_alerts"));
        Map<String, Integer> size = Map.of("github", 520, "slack", 410, "sentry", 480, "grafana", 620);
        servers.forEach((server, names) -> names.forEach(name -> TOOLS.put(server + "_" + name, size.get(server))));
    }

    /** Least privilege: a tool the role does not need is removed from its configuration, not logged or put behind a confirmation. */
    static Audit audit(List<String> holds, List<String> needs, Map<String, Tool> catalog) {
        LOG.log(System.Logger.Level.DEBUG, "audit input: {0}", holds);
        List<String> remove = holds.stream().filter(t -> !needs.contains(t)).toList();
        return new Audit(remove, remove.stream().filter(t -> RISKY.contains(catalog.get(t).access())).toList(), needs.stream().filter(t -> !holds.contains(t)).toList());
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage) {
        return planLoading(tools, usage, 4, 350);
    }

    /** With 10 or more tools, or definitions over 10,000 tokens, the 3 to 5 most used tools stay loaded and the rest are found through a search tool. */
    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage, int keep, int searchTokens) {
        keep = Math.max(3, Math.min(5, keep));
        List<String> names = new ArrayList<>(tools.keySet());
        int total = tools.values().stream().mapToInt(Integer::intValue).sum();
        if (names.size() < 10 && total <= 10000) return new Plan(false, names, List.of(), total);
        List<String> sorted = new ArrayList<>(names);
        sorted.sort((a, b) -> usage.getOrDefault(a, 0).equals(usage.getOrDefault(b, 0)) ? a.compareTo(b) : Integer.compare(usage.getOrDefault(b, 0), usage.getOrDefault(a, 0)));
        List<String> ranked = new ArrayList<>(sorted.subList(0, Math.min(keep, sorted.size())));
        return new Plan(true, List.copyOf(ranked), names.stream().filter(t -> !ranked.contains(t)).toList(), ranked.stream().mapToInt(tools::get).sum() + searchTokens);
    }

    /** Another agent is reached agent-to-agent; a step with a known path is a call in code; a capability several clients share is an MCP server; otherwise it is a tool of the one application. */
    static String chooseMechanism(int consumers, String counterpart, String path) {
        if (counterpart.equals("agent")) return "agent-to-agent";
        if (path.equals("fixed")) return "direct call in code";
        return consumers > 1 ? "MCP server" : "custom tool";
    }

    /** A call is allowed only when the user holds the scope the tool needs and the agent does too; the agent's own rights are a ceiling, not a licence. */
    static String authorize(String tool, Set<String> userScopes, Set<String> agentScopes, Map<String, String> required) {
        if (!required.containsKey(tool)) return "deny: unknown tool";
        String scope = required.get(tool);
        if (!userScopes.contains(scope)) return "deny: user lacks " + scope;
        if (!agentScopes.contains(scope)) return "deny: agent lacks " + scope;
        return "allow";
    }

    /** One place decides who is calling, which models that team may use and how many requests a minute it may send, and keeps a record of every decision. */
    static Outcome gateway(String credential, String model, int recent, Policy policy) {
        String team = credential == null ? null : policy.credentials().get(credential);
        String decision = "deny";
        String reason;
        if (team == null) reason = "unauthenticated";
        else if (!policy.models().get(team).contains(model)) reason = "model not allowed";
        else if (recent >= policy.limits().get(team)) reason = "rate limited";
        else {
            decision = "allow";
            reason = "routed to " + policy.routes().getOrDefault(model, model);
        }
        return new Outcome(decision, reason, Map.of("team", team == null ? "unknown" : team, "model", model, "decision", decision));
    }

    private static int percent(int part, int whole) {
        return (200 * part + whole) / (2 * whole);
    }

    public static void main(String[] args) {
        List<String> holds = new ArrayList<>(CATALOG.keySet());
        List<String> needs = List.of("read_ticket", "draft_reply");
        Audit result = audit(holds, needs, CATALOG);
        int heldTokens = holds.stream().mapToInt(t -> CATALOG.get(t).tokens()).sum();
        int neededTokens = needs.stream().mapToInt(t -> CATALOG.get(t).tokens()).sum();
        System.out.println("support agent holds " + holds.size() + " tools (" + heldTokens + " tokens) and needs " + needs.size());
        System.out.println("  remove: " + String.join(", ", result.remove()) + "; risky among them: " + String.join(", ", result.risky()));
        System.out.println("  after removal: " + (holds.size() - result.remove().size()) + " tools, " + neededTokens + " tokens");
        Map<String, Integer> three = new LinkedHashMap<>();
        CATALOG.entrySet().stream().limit(3).forEach(e -> three.put(e.getKey(), e.getValue().tokens()));
        Plan plan = null;
        Map<String, Map<String, Integer>> sets = new LinkedHashMap<>();
        sets.put("three tools", three);
        sets.put("four servers", TOOLS);
        for (Map.Entry<String, Map<String, Integer>> e : sets.entrySet()) {
            plan = planLoading(e.getValue(), USAGE);
            int total = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            String saved = plan.search() ? ", " + percent(total - plan.tokens(), total) + "% fewer" : "";
            System.out.println(e.getKey() + ": " + e.getValue().size() + " tools, " + total + " tokens of definitions -> search tool " + (plan.search() ? "yes" : "no") + ", " + plan.loadNow().size() + " loaded now, "
                + plan.deferred().size() + " deferred, " + plan.tokens() + " tokens up front" + saved);
        }
        System.out.println("  loaded now: " + String.join(", ", plan.loadNow()));
        Object[][] rows = {{1, "tool", "model-chosen"}, {4, "tool", "model-chosen"}, {1, "tool", "fixed"}, {1, "agent", "model-chosen"}};
        for (Object[] r : rows) {
            System.out.println(String.format("  %d application(s), counterpart %-5s, path %-12s -> %s", (int) r[0], r[1], r[2], chooseMechanism((int) r[0], (String) r[1], (String) r[2])));
        }
        Map<String, String> required = Map.of("read_ticket", "tickets:read", "issue_refund", "refunds:write");
        Set<String> user = Set.of("tickets:read");
        Set<String> agent = Set.of("tickets:read", "refunds:write");
        System.out.println("refund asked by a user who may only read: agent's rights alone -> " + (agent.contains(required.get("issue_refund")) ? "allow" : "deny") + "; user's and agent's rights -> "
            + authorize("issue_refund", user, agent, required));
        List<Map<String, String>> log = new ArrayList<>();
        Object[][] requests = {{null, "standard", 0}, {"key-a", "deep", 0}, {"key-a", "standard", 30}, {"key-b", "deep", 3}};
        for (Object[] r : requests) {
            Outcome outcome = gateway((String) r[0], (String) r[1], (int) r[2], POLICY);
            log.add(outcome.audit());
            System.out.println(String.format("  gateway: credential %-5s model %-8s recent %2d -> %s: %s", r[0] == null ? "none" : r[0], r[1], (int) r[2], outcome.decision(), outcome.reason()));
        }
        System.out.println("  records kept: " + log.size() + ", denials among them: " + log.stream().filter(r -> r.get("decision").equals("deny")).count());
    }
}
