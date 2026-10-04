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
        // TODO: the tools to remove (held, not needed), the risky ones among them, the needed tools the agent lacks and the held, needed tools nobody used.
        return null;
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage) {
        return planLoading(tools, usage, 4, 350);
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage, int keep, int searchTokens) {
        // TODO: load everything for a small set, otherwise keep the most used tools loaded and defer the rest behind a search tool.
        return null;
    }

    static String chooseMechanism(int consumers, String counterpart, String path) {
        // TODO: agent-to-agent, direct call in code, MCP server or custom tool.
        return null;
    }

    static String authorize(String tool, Set<String> userScopes, Set<String> agentScopes, Map<String, String> required) {
        // TODO: allow, or deny with the reason, when the user's rights and the agent's rights do not both cover the tool.
        return null;
    }

    static Outcome gateway(Request request, Policy policy) {
        // TODO: authenticate, check the model, the tool and the rate in that order, route an allowed request and keep a record of every decision.
        return null;
    }
}
