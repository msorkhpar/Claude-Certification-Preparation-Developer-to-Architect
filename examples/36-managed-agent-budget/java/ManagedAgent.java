import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.
 *
 * <p>Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
 * tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
 * API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
 * 2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
 * The payloads are JSON objects read into maps, as the API would receive them.
 */
public final class ManagedAgent {
    private static final System.Logger LOG = System.getLogger(ManagedAgent.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A JSON object as a map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(String json) {
        try {
            return JSON.readValue(json, LinkedHashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    /** Python's notion of an empty or missing value. */
    private static boolean truthy(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Map<?, ?> m) return !m.isEmpty();
        if (o instanceof List<?> l) return !l.isEmpty();
        return true;
    }

    // --- configuration checks -------------------------------------------------------------------------------------------

    /** Findings for an environment payload. An omitted networking field becomes `unrestricted` on the API, so it is a finding. */
    static List<String> checkEnvironment(Map<String, Object> env, List<String> agentMcpHosts) {
        List<String> findings = new ArrayList<>();
        if ("self_hosted".equals(env.get("type"))) return findings;
        if (env.get("networking") == null) {
            findings.add("networking omitted: a create request that omits it gets unrestricted");
            return findings;
        }
        Map<String, Object> net = map(env.get("networking"));
        if ("unrestricted".equals(net.get("type"))) {
            findings.add("unrestricted networking: any host except a safety blocklist; keep secrets out of the sandbox");
            return findings;
        }
        for (Object host : list(net.get("allowed_hosts"))) {
            String h = (String) host;
            if (h.contains("://") || h.contains("/") || h.contains(":")) findings.add("allowed_hosts entry " + py(h) + ": use a bare hostname, no scheme, port or path");
        }
        if (truthy(env.get("packages")) && !truthy(net.get("allow_package_managers"))) {
            findings.add("packages with limited networking need allow_package_managers: true, or the request is rejected (400)");
        }
        Set<Object> hosts = Set.copyOf(list(net.get("allowed_hosts")));
        for (String host : agentMcpHosts) {
            if (!hosts.contains(host) && !truthy(net.get("allow_mcp_servers"))) {
                findings.add("MCP host " + host + " is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)");
            }
        }
        return findings;
    }

    private static String policyType(Object config, String fallback) {
        Object policy = map(config).get("permission_policy");
        return policy == null ? fallback : (String) map(policy).get("type");
    }

    /** Findings for an agent payload read together with the environment it will run in. */
    static List<String> checkAgent(Map<String, Object> agent, Map<String, Object> env) {
        List<String> findings = new ArrayList<>();
        for (Object t : list(agent.get("tools"))) {
            Map<String, Object> tool = map(t);
            if ("agent_toolset_20260401".equals(tool.get("type"))) {
                String policy = policyType(tool.get("default_config"), "always_allow");
                Map<String, String> overrides = new LinkedHashMap<>();
                for (Object c : list(tool.get("configs"))) overrides.put((String) map(c).get("name"), policyType(c, null));
                String bash = overrides.containsKey("bash") ? overrides.get("bash") : policy;
                Object networking = env.get("networking");
                boolean reach = !"limited".equals(networking == null ? "unrestricted" : map(networking).get("type"));
                if ("always_allow".equals(bash) && reach) {
                    findings.add("bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking");
                }
                for (Object c : list(tool.get("configs"))) {
                    if (truthy(map(c).get("allowed_domains")) && truthy(map(c).get("blocked_domains"))) {
                        findings.add(map(c).get("name") + ": allowed_domains and blocked_domains cannot be combined");
                    }
                }
            }
            if ("mcp_toolset".equals(tool.get("type"))) {
                if ("always_allow".equals(policyType(tool.get("default_config"), "always_ask"))) {
                    findings.add("mcp toolset " + tool.get("mcp_server_name") + ": always_allow lets new tools of that server run unreviewed");
                }
            }
        }
        return findings;
    }

    /** A self-hosted sandbox accepts memory_store resources only. */
    static List<String> checkSessionResources(Map<String, Object> env, List<Map<String, Object>> resources) {
        if ("self_hosted".equals(env.get("type"))) {
            Set<String> bad = new TreeSet<>();
            for (Map<String, Object> r : resources) if (!"memory_store".equals(r.get("type"))) bad.add((String) r.get("type"));
            return bad.stream().map(t -> "self-hosted sandboxes reject " + t + " resources (400)").toList();
        }
        return List.of();
    }

    // --- list cost and budget -------------------------------------------------------------------------------------------

    /** Dollars per million input and output tokens. */
    static final Map<String, long[]> PRICES = Map.of("claude-opus-5-5", new long[] {4, 20}, "claude-sonnet-5-5", new long[] {2, 10});
    static final long WEB_SEARCH_MICRO = 10_000; // $10 per 1,000 searches, in millionths of a dollar per search
    static final long RUNNING_MICRO_PER_HOUR = 80_000; // $0.08 per hour of session running time

    /** The session's list cost in whole cents, rounded to the nearest cent, as the platform reports it. */
    static long listCostCents(String model, long inputTokens, long outputTokens, long searches, long activeSeconds) {
        long[] price = PRICES.get(model);
        long micro = inputTokens * price[0] + outputTokens * price[1] + searches * WEB_SEARCH_MICRO + activeSeconds * RUNNING_MICRO_PER_HOUR / 3600;
        return (micro + 5_000) / 10_000;
    }

    /** max_list_cost.amount is a whole number of cents as a string, no leading zeros, greater than zero. */
    static String checkBudget(String amount) {
        if (!(!amount.isEmpty() && amount.chars().allMatch(c -> c >= '0' && c <= '9') && !amount.startsWith("0"))) {
            return "amount " + py(amount) + " is rejected: write whole cents as a string with no leading zeros";
        }
        return "ok";
    }

    /** At or past the cap the session goes idle with stop_reason budget_reached; the request in flight still finishes. */
    static String budgetState(long costCents, String amount) {
        long cap = Long.parseLong(amount);
        return costCents >= cap ? "budget_reached" : "running, " + (cap - costCents) + " cents left";
    }

    /** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
    static String py(Object v) {
        if (v instanceof List<?> l) return l.stream().map(ManagedAgent::py).collect(Collectors.joining(", ", "[", "]"));
        String s = String.valueOf(v);
        String quote = s.contains("'") && !s.contains("\"") ? "\"" : "'";
        return quote + s.replace("\\", "\\\\").replace("\n", "\\n").replace(quote, "\\" + quote) + quote;
    }

    public static void main(String[] args) {
        Map<String, Object> env = obj("""
            {"type": "cloud", "packages": {"pip": ["sqlalchemy==2.0.30"]}, "networking": {"type": "limited", "allowed_hosts": ["https://api.example.com"]}}""");
        Map<String, Object> agent = obj("""
            {"tools": [{"type": "agent_toolset_20260401"}, {"type": "mcp_toolset", "mcp_server_name": "github", "default_config": {"permission_policy": {"type": "always_allow"}}}]}""");
        System.out.println("environment findings:");
        for (String line : checkEnvironment(env, List.of("mcp.example.com"))) System.out.println(" - " + line);
        System.out.println("omitted networking: " + checkEnvironment(obj("{\"type\": \"cloud\"}"), List.of()).get(0));
        System.out.println("agent findings, unrestricted sandbox:");
        for (String line : checkAgent(agent, obj("{\"type\": \"cloud\"}"))) System.out.println(" - " + line);
        System.out.println("agent findings, limited sandbox: " + py(checkAgent(agent, obj("{\"type\": \"cloud\", \"networking\": {\"type\": \"limited\"}}"))));
        System.out.println("self-hosted with a file resource: " + py(checkSessionResources(obj("{\"type\": \"self_hosted\"}"), List.of(obj("{\"type\": \"file\"}"), obj("{\"type\": \"memory_store\"}")))));
        System.out.println();
        for (String model : List.of("claude-opus-5-5", "claude-sonnet-5-5")) {
            long cents = listCostCents(model, 1_200_000, 150_000, 8, 7200);
            System.out.println(model + ": list cost " + cents + " cents; budget 800 -> " + budgetState(cents, "800") + "; budget 1000 -> " + budgetState(cents, "1000"));
        }
        System.out.println("budget '25.00': " + checkBudget("25.00") + " | budget '050': " + checkBudget("050") + " | budget '125': " + checkBudget("125"));
    }
}
