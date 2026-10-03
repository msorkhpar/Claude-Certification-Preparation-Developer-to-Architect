import java.io.File
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlatformConfigTest {
    // config.dir is the folder that holds the three config files: starter, reference or a planted wrong solution.
    private val dir = File(System.getProperty("config.dir", "starter"))

    private val invoke = setOf("bedrock-mantle:CreateInference", "bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream")
    // the AWS regions listed for Claude in Amazon Bedrock on 2026-10-02
    private val regions = setOf("af-south-1", "ap-northeast-1", "ap-northeast-2", "ap-northeast-3", "ap-south-1", "ap-south-2", "ap-southeast-1", "ap-southeast-2",
        "ap-southeast-3", "ap-southeast-4", "ca-central-1", "ca-west-1", "eu-central-1", "eu-central-2", "eu-north-1", "eu-south-1",
        "eu-south-2", "eu-west-1", "eu-west-2", "eu-west-3", "il-central-1", "me-central-1", "sa-east-1", "us-east-1", "us-east-2",
        "us-west-1", "us-west-2")
    private val vertexModels = setOf("claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-4-5@20251001", "claude-sonnet-4-6")
    private val wantedArn = "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"

    @Suppress("UNCHECKED_CAST")
    private fun load(name: String): Map<String, Any?> = Json.parse(File(dir, name).readText()) as Map<String, Any?>

    private fun asList(value: Any?): List<Any?> = when (value) {
        null -> emptyList()
        is List<*> -> value
        else -> listOf(value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun statements(): List<Map<String, Any?>> = asList(load("bedrock-policy.json")["Statement"]).map { it as Map<String, Any?> }

    private fun collect(key: String): List<String> = statements().flatMap { s -> asList(s[key]).map { it as String } }

    @Suppress("UNCHECKED_CAST")
    private fun map(value: Any?): Map<String, Any?> = (value as? Map<String, Any?>) ?: emptyMap()

    @Test
    fun m1_thePolicyAllowsInvokingOneModelInOneRegionAndNothingElse() {
        val stmts = statements()
        assertEquals(1, stmts.size)
        assertEquals("Allow", stmts[0]["Effect"])
        assertFalse(collect("Action").isEmpty())
        assertTrue(invoke.containsAll(collect("Action")), collect("Action").toString())
        assertEquals(listOf(wantedArn), collect("Resource"))
    }

    @Test
    fun e1_noActionIsAWildcardAndEveryActionIsAnInvokeAction() {
        assertFalse(collect("Action").isEmpty(), "the policy has no action")
        for (action in collect("Action")) {
            assertFalse(action.contains("*"), action)
            assertTrue(action in invoke, action)
        }
    }

    @Test
    fun e2_everyResourceARNNamesOneDocumentedRegionAndOneModel() {
        assertFalse(collect("Resource").isEmpty(), "the policy has no resource")
        for (arn in collect("Resource")) {
            assertFalse(arn.contains("*"), arn)
            val parts = arn.split(":")
            assertTrue(parts.size == 6 && parts[0] == "arn" && parts[1] == "aws" && parts[2] == "bedrock", arn)
            assertTrue(parts[3] in regions, arn)
            assertTrue(parts[5].startsWith("foundation-model/anthropic.claude-"), arn)
        }
    }

    @Test
    fun e3_everyStatementAllowsAndThePolicyUsesTheCurrentVersion() {
        assertEquals("2012-10-17", load("bedrock-policy.json")["Version"])
        assertFalse(statements().isEmpty())
        for (s in statements()) {
            assertEquals("Allow", s["Effect"])
            assertFalse(s.containsKey("NotAction") || s.containsKey("NotResource"))
        }
    }

    @Test
    fun e4_theVertexRoleIsACustomRoleThatCanOnlyPredict() {
        val role = map(load("vertex.json")["role"])
        val first = role["id"].toString().split("/")[0]
        assertTrue(first == "projects" || first == "organizations", role["id"].toString())
        assertEquals(listOf("aiplatform.endpoints.predict"), role["permissions"])
    }

    @Test
    fun e5_theVertexEndpointKeepsTheDataWhereResidencySaysAndServesTheModel() {
        val cfg = load("vertex.json")
        val endpoint = cfg["endpoint"].toString()
        val residency = cfg["residency"].toString()
        val model = cfg["model"].toString()
        assertTrue(model in vertexModels, model)
        if (residency == "eu") assertTrue(endpoint == "eu" || endpoint.startsWith("europe-"), endpoint)
        if (residency == "us") assertTrue(endpoint == "us" || endpoint.startsWith("us-"), endpoint)
        if (endpoint !in setOf("global", "us", "eu")) assertEquals("claude-sonnet-4-6", model, "$model is not served on a specific region")
    }

    @Test
    fun e6_modelIdsUseEachPlatformsOwnForm() {
        for (arn in collect("Resource")) assertTrue(arn.substringAfterLast('/').startsWith("anthropic.claude-"), arn)
        val model = load("vertex.json")["model"].toString()
        assertFalse(model.startsWith("anthropic."), model)
        if (model.startsWith("claude-haiku-4-5")) assertEquals("claude-haiku-4-5@20251001", model)
    }

    @Test
    fun e7_theQuotaRequestStaysUnderTheSelfServiceCeiling() {
        val quota = load("quotas.json")
        val input = (quota["input_tpm"] as Number).toLong()
        val output = (quota["output_tpm"] as Number).toLong()
        assertTrue(input > 0 && output > 0)
        if (quota["anthropic_approval"] != true) {
            assertTrue(input <= 5_000_000L, input.toString())
            assertTrue(output <= 500_000L, output.toString())
        }
    }
}
