import java.lang.System.Logger.Level

private val log = System.getLogger("platforms")

/** One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md. */

/** The request cannot be built for this platform. [field] names the offending part. */
class PlatformError(val field: String, reason: String) : RuntimeException("$field: $reason")

private const val ANTHROPIC_VERSION = "2023-06-01"
private const val VERTEX_VERSION = "vertex-2023-10-16"
private const val HAIKU = "claude-haiku-4-5"
private const val HAIKU_VERTEX = "claude-haiku-4-5@20251001"
private val BEDROCK_MODELS = setOf("claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", HAIKU)
private val VERTEX_MODELS = BEDROCK_MODELS + "claude-sonnet-4-6"
private val REGIONAL_VERTEX_MODELS = setOf("claude-sonnet-4-6") // specific regions serve Sonnet 4.6 and earlier only
private val MISSING = mapOf(
    "anthropic" to emptySet<String>(),
    "bedrock" to setOf("batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector", "structured_outputs", "skills", "managed_agents"),
    "vertex" to setOf("batches", "fast_mode", "files_api", "web_fetch", "code_execution", "mcp_connector", "skills", "managed_agents"),
)
private val FEATURES = setOf("batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector",
    "structured_outputs", "skills", "managed_agents", "prompt_caching", "thinking", "tool_use", "citations")

private fun family(model: String): String = if (model.startsWith(HAIKU)) HAIKU else model

private fun checkBedrock(model: String, family: String, config: Map<String, Any?>) {
    if (family !in BEDROCK_MODELS) throw PlatformError("model", "$model is not served by Claude in Amazon Bedrock")
    if ((config["region"] as String?).isNullOrEmpty()) throw PlatformError("config", "a Bedrock request needs a region")
}

private fun bedrockModelId(family: String): String = "anthropic.$family"

private fun vertexModelId(family: String): String = if (family == HAIKU) HAIKU_VERTEX else family

private fun vertexHost(endpoint: String, family: String, model: String): String = when (endpoint) {
    "global" -> "aiplatform.googleapis.com"
    "us", "eu" -> "aiplatform.$endpoint.rep.googleapis.com"
    else -> {
        if (family !in REGIONAL_VERTEX_MODELS) throw PlatformError("endpoint", "$model is served on the global and multi-region endpoints, not on a specific region")
        "$endpoint-aiplatform.googleapis.com"
    }
}

@Suppress("UNCHECKED_CAST")
private fun copy(value: Any?): Any? = when (value) {
    is Map<*, *> -> LinkedHashMap<String, Any?>().also { out -> (value as Map<String, Any?>).forEach { (k, v) -> out[k] = copy(v) } }
    is List<*> -> value.map { copy(it) }
    else -> value
}

@Suppress("UNCHECKED_CAST")
private fun copyBody(body: Map<String, Any?>): LinkedHashMap<String, Any?> = copy(body) as LinkedHashMap<String, Any?>

private fun vertexBody(body: Map<String, Any?>): Map<String, Any?> {
    val sent = copyBody(body)
    sent.remove("model")
    sent["anthropic_version"] = VERTEX_VERSION
    return sent
}

private fun lacking(platform: String, features: List<String>): List<String> = features.filter { it in MISSING.getValue(platform) }

private fun versionHeaders(): Map<String, Any?> = linkedMapOf("anthropic-version" to ANTHROPIC_VERSION, "content-type" to "application/json")

private fun request(url: String, headers: Map<String, Any?>, body: Map<String, Any?>): Map<String, Any?> =
    linkedMapOf("method" to "POST", "url" to url, "headers" to headers, "body" to body)

fun buildRequest(platform: String, model: String, body: Map<String, Any?>, config: Map<String, Any?>): Map<String, Any?> {
    log.log(Level.DEBUG, "buildRequest input: {0} {1} {2} {3}", platform, model, body, config)
    val family = family(model)
    when (platform) {
        "anthropic" -> {
            val sent = copyBody(body)
            sent["model"] = model
            return request("https://api.anthropic.com/v1/messages", versionHeaders(), sent)
        }
        "bedrock" -> {
            checkBedrock(model, family, config)
            val region = config["region"] as String?
            val sent = copyBody(body)
            sent["model"] = bedrockModelId(family)
            return request("https://bedrock-mantle.$region.api.aws/anthropic/v1/messages", versionHeaders(), sent)
        }
        "vertex" -> {
            if (family !in VERTEX_MODELS) throw PlatformError("model", "$model is not served on Google Vertex AI")
            val project = config["project"] as String?
            if (project.isNullOrEmpty()) throw PlatformError("config", "a Vertex request needs a project")
            val endpoint = (config["endpoint"] as String?) ?: "global"
            val path = "/v1/projects/$project/locations/$endpoint/publishers/anthropic/models/${vertexModelId(family)}:rawPredict"
            val host = vertexHost(endpoint, family, model)
            return request("https://$host$path", linkedMapOf("content-type" to "application/json"), vertexBody(body))
        }
        else -> throw PlatformError("platform", "unknown platform $platform")
    }
}

fun unsupportedFeatures(platform: String, features: List<String>): List<String> {
    val missing = MISSING[platform] ?: throw PlatformError("platform", "unknown platform $platform")
    features.firstOrNull { it !in FEATURES }?.let { throw PlatformError("feature", "unknown feature $it") }
    return lacking(platform, features)
}
