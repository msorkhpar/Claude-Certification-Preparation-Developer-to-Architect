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

private fun family(model: String) = if (model.startsWith(HAIKU)) HAIKU else model

@Suppress("UNCHECKED_CAST")
private fun copy(value: Any?): Any? = when (value) {
    is Map<*, *> -> LinkedHashMap<String, Any?>().also { out -> (value as Map<String, Any?>).forEach { (k, v) -> out[k] = copy(v) } }
    is List<*> -> value.map { copy(it) }
    else -> value
}

@Suppress("UNCHECKED_CAST")
private fun copyBody(body: Map<String, Any?>): LinkedHashMap<String, Any?> = copy(body) as LinkedHashMap<String, Any?>

private fun versionHeaders(): Map<String, Any?> = linkedMapOf("anthropic-version" to ANTHROPIC_VERSION, "content-type" to "application/json")

private fun request(url: String, headers: Map<String, Any?>, body: Map<String, Any?>): Map<String, Any?> =
    linkedMapOf("method" to "POST", "url" to url, "headers" to headers, "body" to body)

fun buildRequest(platform: String, model: String, body: Map<String, Any?>, config: Map<String, Any?>): Map<String, Any?> {
    val family = family(model)
    when (platform) {
        "anthropic" -> {
            val sent = copyBody(body)
            sent["model"] = model
            return request("https://api.anthropic.com/v1/messages", versionHeaders(), sent)
        }
        "bedrock" -> {
            if (family !in BEDROCK_MODELS) throw PlatformError("model", "$model is not served by Claude in Amazon Bedrock")
            val region = config["region"] as String?
            if (region.isNullOrEmpty()) throw PlatformError("config", "a Bedrock request needs a region")
            val sent = copyBody(body)
            sent["model"] = model
            return request("https://bedrock-mantle.$region.api.aws/anthropic/v1/messages", versionHeaders(), sent)
        }
        "vertex" -> {
            if (family !in VERTEX_MODELS) throw PlatformError("model", "$model is not served on Google Vertex AI")
            val project = config["project"] as String?
            if (project.isNullOrEmpty()) throw PlatformError("config", "a Vertex request needs a project")
            val endpoint = (config["endpoint"] as String?) ?: "global"
            val modelId = if (family == HAIKU) HAIKU_VERTEX else family
            val path = "/v1/projects/$project/locations/$endpoint/publishers/anthropic/models/$modelId:rawPredict"
            val host = when (endpoint) {
                "global" -> "aiplatform.googleapis.com"
                "us", "eu" -> "aiplatform.$endpoint.rep.googleapis.com"
                else -> {
                    if (family !in REGIONAL_VERTEX_MODELS) throw PlatformError("endpoint", "$model is served on the global and multi-region endpoints, not on a specific region")
                    "$endpoint-aiplatform.googleapis.com"
                }
            }
            val sent = copyBody(body)
            sent.remove("model")
            sent["anthropic_version"] = VERTEX_VERSION
            return request("https://$host$path", linkedMapOf("content-type" to "application/json"), sent)
        }
        else -> throw PlatformError("platform", "unknown platform $platform")
    }
}

fun unsupportedFeatures(platform: String, features: List<String>): List<String> {
    val missing = MISSING[platform] ?: throw PlatformError("platform", "unknown platform $platform")
    features.firstOrNull { it !in FEATURES }?.let { throw PlatformError("feature", "unknown feature $it") }
    return features.filter { it in missing }
}
