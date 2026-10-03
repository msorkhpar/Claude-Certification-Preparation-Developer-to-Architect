import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlatformsTest {
    private val opus = "claude-opus-5-5"
    private val sonnet5 = "claude-sonnet-5-5"
    private val sonnet46 = "claude-sonnet-4-6"
    private val haiku = "claude-haiku-4-5-20251001"
    private val fable = "claude-fable-5-1"

    private fun body(): Map<String, Any?> = linkedMapOf("model" to "ignored-by-the-builder", "max_tokens" to 256,
        "messages" to mutableListOf(mapOf("role" to "user", "content" to "Hello, Claude")))

    private fun built(platform: String, model: String, vararg config: Pair<String, Any?>): Map<String, Any?> =
        buildRequest(platform, model, body(), mapOf(*config)) ?: emptyMap()

    private fun g(request: Any?, vararg path: String): Any? {
        var cur: Any? = request
        for (step in path) cur = (cur as? Map<*, *>)?.get(step)
        return cur
    }

    private fun fieldOf(platform: String, model: String, vararg config: Pair<String, Any?>): String? = try {
        buildRequest(platform, model, body(), mapOf(*config))
        null
    } catch (e: PlatformError) {
        e.field
    } catch (e: RuntimeException) {
        "crash: $e"
    }

    private fun endsWith(url: Any?, suffix: String) = (url as? String)?.endsWith(suffix) == true

    @Test
    fun m1_theSameMessageTakesThreeShapes() {
        val direct = built("anthropic", opus)
        assertEquals("https://api.anthropic.com/v1/messages", g(direct, "url"))
        assertEquals(mapOf("anthropic-version" to "2023-06-01", "content-type" to "application/json"), g(direct, "headers"))
        assertEquals(opus, g(direct, "body", "model"))
        val bedrock = built("bedrock", opus, "region" to "us-east-1")
        assertEquals("https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages", g(bedrock, "url"))
        assertEquals("2023-06-01", g(bedrock, "headers", "anthropic-version"))
        assertEquals("anthropic.claude-opus-5-5", g(bedrock, "body", "model"))
        val vertex = built("vertex", opus, "project" to "my-project")
        assertEquals("https://aiplatform.googleapis.com/v1/projects/my-project/locations/global/publishers/anthropic/models/claude-opus-5-5:rawPredict", g(vertex, "url"))
        val headers = g(vertex, "headers") as? Map<*, *>
        assertTrue(headers != null && !headers.containsKey("anthropic-version"))
        assertEquals("POST", g(vertex, "method"))
    }

    @Test
    fun e1_modelIdsChangeWithThePlatform() {
        assertEquals(haiku, g(built("anthropic", haiku), "body", "model"))
        assertEquals("anthropic.claude-haiku-4-5", g(built("bedrock", haiku, "region" to "eu-west-1"), "body", "model"))
        assertEquals("anthropic.claude-haiku-4-5", g(built("bedrock", "claude-haiku-4-5", "region" to "eu-west-1"), "body", "model"))
        assertEquals("anthropic.claude-fable-5-1", g(built("bedrock", fable, "region" to "us-east-1"), "body", "model"))
        assertTrue(endsWith(g(built("vertex", haiku, "project" to "p"), "url"), "/models/claude-haiku-4-5@20251001:rawPredict"))
        assertTrue(endsWith(g(built("vertex", sonnet5, "project" to "p"), "url"), "/models/claude-sonnet-5-5:rawPredict"))
    }

    @Test
    fun e2_vertexMovesTheModelIntoTheUrlAndTheVersionIntoTheBody() {
        val body = body()
        val sent = g(buildRequest("vertex", opus, body, mapOf("project" to "p")), "body") as? Map<*, *>
        assertTrue(sent != null && !sent.containsKey("model") && sent["anthropic_version"] == "vertex-2023-10-16" && sent["max_tokens"] == 256 && sent["messages"] == body()["messages"])
        assertEquals(body(), body) // the caller's body is left as it was
        val other = g(buildRequest("bedrock", opus, body, mapOf("region" to "us-east-1")), "body") as? Map<*, *>
        assertTrue(other != null && !other.containsKey("anthropic_version") && other["max_tokens"] == 256)
        assertEquals(body(), body)
    }

    @Test
    fun e3_vertexEndpointsGlobalMultiRegionAndRegional() {
        assertEquals("https://aiplatform.us.rep.googleapis.com/v1/projects/p/locations/us/publishers/anthropic/models/claude-opus-5-5:rawPredict",
            g(built("vertex", opus, "project" to "p", "endpoint" to "us"), "url"))
        assertTrue((g(built("vertex", sonnet5, "project" to "p", "endpoint" to "eu"), "url") as? String)?.startsWith("https://aiplatform.eu.rep.googleapis.com/v1/projects/p/locations/eu/") == true)
        assertEquals("https://europe-west1-aiplatform.googleapis.com/v1/projects/p/locations/europe-west1/publishers/anthropic/models/claude-sonnet-4-6:rawPredict",
            g(built("vertex", sonnet46, "project" to "p", "endpoint" to "europe-west1"), "url"))
        assertEquals("endpoint", fieldOf("vertex", opus, "project" to "p", "endpoint" to "europe-west1"))
        assertEquals("endpoint", fieldOf("vertex", haiku, "project" to "p", "endpoint" to "us-east5"))
    }

    @Test
    fun e4_aPlatformServesOnlyItsOwnModels() {
        assertEquals("model", fieldOf("bedrock", sonnet46, "region" to "us-east-1"))
        assertEquals("model", fieldOf("bedrock", "claude-nonexistent-9", "region" to "us-east-1"))
        assertEquals("model", fieldOf("vertex", "claude-nonexistent-9", "project" to "p"))
        assertNull(fieldOf("vertex", sonnet46, "project" to "p"))
        assertEquals("platform", fieldOf("mystery", opus))
    }

    @Test
    fun e5_eachPlatformLacksItsOwnFeatures() {
        val names = listOf("batches", "fast_mode", "prompt_caching", "thinking", "web_search", "structured_outputs", "files_api", "tool_use")
        assertEquals(emptyList<String>(), unsupportedFeatures("anthropic", names))
        assertEquals(listOf("batches", "fast_mode", "web_search", "structured_outputs", "files_api"), unsupportedFeatures("bedrock", names))
        assertEquals(listOf("batches", "fast_mode", "files_api"), unsupportedFeatures("vertex", names))
        assertEquals(listOf("web_fetch", "mcp_connector"), unsupportedFeatures("vertex", listOf("web_fetch", "mcp_connector", "citations")))
    }

    @Test
    fun e6_aRequestNeedsThePlaceItIsSentTo() {
        assertEquals("config", fieldOf("bedrock", opus))
        assertEquals("config", fieldOf("vertex", opus))
        assertEquals("config", fieldOf("bedrock", opus, "region" to ""))
        assertNull(fieldOf("anthropic", opus))
    }
}
