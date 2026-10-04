import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManagedAgentTest {
    private static Map<String, Object> obj(String json) {
        return ManagedAgent.obj(json);
    }

    @Test
    void anOmittedNetworkingFieldIsAFindingAndLimitedNeedsThePackageFlag() {
        assertTrue(ManagedAgent.checkEnvironment(obj("{\"type\": \"cloud\"}"), List.of()).get(0).contains("unrestricted"));
        Map<String, Object> limited = obj("{\"type\": \"cloud\", \"packages\": {\"pip\": [\"x\"]}, \"networking\": {\"type\": \"limited\"}}");
        assertTrue(ManagedAgent.checkEnvironment(limited, List.of()).stream().anyMatch(f -> f.contains("allow_package_managers")));
        Map<String, Object> allowed = obj("{\"type\": \"cloud\", \"packages\": {\"pip\": [\"x\"]}, \"networking\": {\"type\": \"limited\", \"allow_package_managers\": true}}");
        assertEquals(List.of(), ManagedAgent.checkEnvironment(allowed, List.of()));
    }

    @Test
    void hostsAreBareAndMcpHostsMustBeReachable() {
        Map<String, Object> env = obj("{\"type\": \"cloud\", \"networking\": {\"type\": \"limited\", \"allowed_hosts\": [\"https://a.example.com\", \"b.example.com:443\", \"c.example.com\"]}}");
        List<String> found = ManagedAgent.checkEnvironment(env, List.of("m.example.com", "c.example.com"));
        assertEquals(2, found.stream().filter(f -> f.contains("bare hostname")).count());
        assertEquals(1, found.stream().filter(f -> f.contains("MCP host")).count());
        Map<String, Object> open = obj("{\"type\": \"cloud\", \"networking\": {\"type\": \"limited\", \"allow_mcp_servers\": true}}");
        assertTrue(ManagedAgent.checkEnvironment(open, List.of("m.example.com")).stream().noneMatch(f -> f.contains("MCP host")));
    }

    @Test
    void defaultPoliciesFlagBashOnAnOpenNetworkAndUnreviewedMcpTools() {
        Map<String, Object> agent = obj("{\"tools\": [{\"type\": \"agent_toolset_20260401\"}, {\"type\": \"mcp_toolset\", \"mcp_server_name\": \"github\", \"default_config\": {\"permission_policy\": {\"type\": \"always_allow\"}}}]}");
        assertEquals(2, ManagedAgent.checkAgent(agent, obj("{\"type\": \"cloud\"}")).size());
        assertEquals(1, ManagedAgent.checkAgent(agent, obj("{\"type\": \"cloud\", \"networking\": {\"type\": \"limited\"}}")).size());
        Map<String, Object> asked = obj("{\"tools\": [{\"type\": \"agent_toolset_20260401\", \"configs\": [{\"name\": \"bash\", \"permission_policy\": {\"type\": \"always_ask\"}}]}]}");
        assertEquals(List.of(), ManagedAgent.checkAgent(asked, obj("{\"type\": \"cloud\"}")));
    }

    @Test
    void selfHostedSandboxesAcceptOnlyMemoryStores() {
        List<Map<String, Object>> resources = List.of(obj("{\"type\": \"file\"}"), obj("{\"type\": \"github_repository\"}"), obj("{\"type\": \"memory_store\"}"));
        assertEquals(List.of("self-hosted sandboxes reject file resources (400)", "self-hosted sandboxes reject github_repository resources (400)"),
            ManagedAgent.checkSessionResources(obj("{\"type\": \"self_hosted\"}"), resources));
        assertEquals(List.of(), ManagedAgent.checkSessionResources(obj("{\"type\": \"cloud\"}"), List.of(obj("{\"type\": \"file\"}"))));
    }

    @Test
    void listCostAddsTokensSearchesAndRunningTime() {
        assertEquals(804, ManagedAgent.listCostCents("claude-opus-5-5", 1_200_000, 150_000, 8, 7200));
        assertEquals(414, ManagedAgent.listCostCents("claude-sonnet-5-5", 1_200_000, 150_000, 8, 7200));
        assertEquals(8, ManagedAgent.listCostCents("claude-opus-5-5", 0, 0, 0, 3600));
    }

    @Test
    void aBudgetIsWholeCentsAsAStringAndStopsNewWorkAtTheCap() {
        assertEquals("ok", ManagedAgent.checkBudget("125"));
        for (String bad : List.of("25.00", "050", "0")) assertEquals("amount '" + bad + "' is rejected: write whole cents as a string with no leading zeros", ManagedAgent.checkBudget(bad));
        assertEquals("budget_reached", ManagedAgent.budgetState(804, "800"));
        assertEquals("budget_reached", ManagedAgent.budgetState(800, "800"));
        assertEquals("running, 196 cents left", ManagedAgent.budgetState(804, "1000"));
    }
}
