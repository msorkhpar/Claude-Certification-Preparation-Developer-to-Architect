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

private fun family(model: String): String {
    // TODO 1 of 7 (finish this to pass e1): the model name without its date.
    // Receives a model id. Returns HAIKU when the id starts with HAIKU (so a dated id such as claude-haiku-4-5-20251001 and the plain
    // name are one family), the id itself otherwise. Example: "claude-haiku-4-5-20251001" -> "claude-haiku-4-5"
    return model
}

private fun checkBedrock(model: String, family: String, config: Map<String, Any?>) {
    // TODO 2 of 7 (finish this to pass e4 and e6): refuse what Bedrock cannot take.
    // Receives the model id, its family and the config map. Throws PlatformError("model", reason) when the family is not in
    // BEDROCK_MODELS, and PlatformError("config", reason) when config has no region or an empty one.
    // Example: claude-sonnet-4-6 with a region throws with field "model"
}

private fun bedrockModelId(family: String): String {
    // TODO 3 of 7 (finish this to pass m1 and e1): the model id in a Bedrock body.
    // Receives the family. Returns "anthropic." followed by it. Example: "claude-opus-5-5" -> "anthropic.claude-opus-5-5"
    return family
}

private fun vertexModelId(family: String): String {
    // TODO 4 of 7 (finish this to pass e1): the model id in a Vertex URL.
    // Receives the family. Returns HAIKU_VERTEX for HAIKU and the family itself for the others.
    // Example: "claude-haiku-4-5" -> "claude-haiku-4-5@20251001"
    return family
}

private fun vertexHost(endpoint: String, family: String, model: String): String {
    // TODO 5 of 7 (finish this to pass m1 and e3): the host of a Vertex endpoint.
    // Receives the endpoint, the model's family and the model id. Returns aiplatform.googleapis.com for "global",
    // aiplatform.<endpoint>.rep.googleapis.com for "us" and "eu", and <endpoint>-aiplatform.googleapis.com for a specific region,
    // which throws PlatformError("endpoint", reason) unless the family is in REGIONAL_VERTEX_MODELS.
    // Example: ("eu", "claude-opus-5-5", "claude-opus-5-5") -> "aiplatform.eu.rep.googleapis.com"
    return ""
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
    // TODO 6 of 7 (finish this to pass e2): the body Vertex takes.
    // Receives the caller's body. Returns a deep copy (copyBody) without the "model" key (the model is in the URL) and with
    // "anthropic_version" set to VERTEX_VERSION; the caller's own body is never changed.
    // Example: {model: x, max_tokens: 5} -> {max_tokens: 5, anthropic_version: ...}
    return copyBody(body)
}

private fun lacking(platform: String, features: List<String>): List<String> {
    // TODO 7 of 7 (finish this to pass e5): the features this platform lacks.
    // Receives a known platform and a list of known feature names. Returns the names that are in MISSING[platform], in the order given.
    // Example: ("vertex", ["batches", "thinking"]) -> ["batches"]
    return emptyList()
}

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
