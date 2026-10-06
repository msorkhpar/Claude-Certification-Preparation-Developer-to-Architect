import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ManagedAgentTest {
    @Test
    fun anOmittedNetworkingFieldIsAFindingAndLimitedNeedsThePackageFlag() {
        assertTrue("unrestricted" in checkEnvironment(obj("""{"type": "cloud"}"""))[0])
        val limited = obj("""{"type": "cloud", "packages": {"pip": ["x"]}, "networking": {"type": "limited"}}""")
        assertTrue(checkEnvironment(limited).any { "allow_package_managers" in it })
        val allowed = obj("""{"type": "cloud", "packages": {"pip": ["x"]}, "networking": {"type": "limited", "allow_package_managers": true}}""")
        assertEquals(emptyList<String>(), checkEnvironment(allowed))
    }

    @Test
    fun hostsAreBareAndMcpHostsMustBeReachable() {
        val env = obj("""{"type": "cloud", "networking": {"type": "limited", "allowed_hosts": ["https://a.example.com", "b.example.com:443", "c.example.com"]}}""")
        val found = checkEnvironment(env, listOf("m.example.com", "c.example.com"))
        assertEquals(2, found.count { "bare hostname" in it })
        assertEquals(1, found.count { "MCP host" in it })
        val open = obj("""{"type": "cloud", "networking": {"type": "limited", "allow_mcp_servers": true}}""")
        assertTrue(checkEnvironment(open, listOf("m.example.com")).none { "MCP host" in it })
    }

    @Test
    fun defaultPoliciesFlagBashOnAnOpenNetworkAndUnreviewedMcpTools() {
        val agent = obj("""{"tools": [{"type": "agent_toolset_20260401"}, {"type": "mcp_toolset", "mcp_server_name": "github", "default_config": {"permission_policy": {"type": "always_allow"}}}]}""")
        assertEquals(2, checkAgent(agent, obj("""{"type": "cloud"}""")).size)
        assertEquals(1, checkAgent(agent, obj("""{"type": "cloud", "networking": {"type": "limited"}}""")).size)
        val asked = obj("""{"tools": [{"type": "agent_toolset_20260401", "configs": [{"name": "bash", "permission_policy": {"type": "always_ask"}}]}]}""")
        assertEquals(emptyList<String>(), checkAgent(asked, obj("""{"type": "cloud"}""")))
    }

    @Test
    fun selfHostedSandboxesAcceptOnlyMemoryStores() {
        val resources = listOf(obj("""{"type": "file"}"""), obj("""{"type": "github_repository"}"""), obj("""{"type": "memory_store"}"""))
        assertEquals(
            listOf("self-hosted sandboxes reject file resources (400)", "self-hosted sandboxes reject github_repository resources (400)"),
            checkSessionResources(obj("""{"type": "self_hosted"}"""), resources),
        )
        assertEquals(emptyList<String>(), checkSessionResources(obj("""{"type": "cloud"}"""), listOf(obj("""{"type": "file"}"""))))
    }

    @Test
    fun listCostAddsTokensSearchesAndRunningTime() {
        assertEquals(804, listCostCents("claude-opus-5-5", 1_200_000, 150_000, 8, 7200))
        assertEquals(414, listCostCents("claude-sonnet-5-5", 1_200_000, 150_000, 8, 7200))
        assertEquals(8, listCostCents("claude-opus-5-5", 0, 0, 0, 3600))
    }

    @Test
    fun aBudgetIsWholeCentsAsAStringAndStopsNewWorkAtTheCap() {
        assertEquals("ok", checkBudget("125"))
        for (bad in listOf("25.00", "050", "0")) assertEquals("amount '$bad' is rejected: write whole cents as a string with no leading zeros", checkBudget(bad))
        assertEquals("budget_reached", budgetState(804, "800"))
        assertEquals("budget_reached", budgetState(800, "800"))
        assertEquals("running, 196 cents left", budgetState(804, "1000"))
    }
}
