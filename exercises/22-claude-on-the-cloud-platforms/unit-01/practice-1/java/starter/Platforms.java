import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md. */
final class Platforms {
    private static final System.Logger LOG = System.getLogger(Platforms.class.getName());
    private Platforms() {}

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String VERTEX_VERSION = "vertex-2023-10-16";
    private static final String HAIKU = "claude-haiku-4-5";
    private static final String HAIKU_VERTEX = "claude-haiku-4-5@20251001";
    private static final Set<String> BEDROCK_MODELS = Set.of("claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", HAIKU);
    private static final Set<String> VERTEX_MODELS = Set.of("claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", HAIKU, "claude-sonnet-4-6");
    private static final Set<String> REGIONAL_VERTEX_MODELS = Set.of("claude-sonnet-4-6"); // specific regions serve Sonnet 4.6 and earlier only
    private static final Map<String, Set<String>> MISSING = Map.of(
            "anthropic", Set.of(),
            "bedrock", Set.of("batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector", "structured_outputs", "skills", "managed_agents"),
            "vertex", Set.of("batches", "fast_mode", "files_api", "web_fetch", "code_execution", "mcp_connector", "skills", "managed_agents"));
    private static final Set<String> FEATURES = Set.of("batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector",
            "structured_outputs", "skills", "managed_agents", "prompt_caching", "thinking", "tool_use", "citations");

    private static String family(String model) {
        // TODO 1 of 7 (finish this to pass e1): the model name without its date.
        // Receives a model id. Returns HAIKU when the id starts with HAIKU (so a dated id such as claude-haiku-4-5-20251001 and the plain
        // name are one family), the id itself otherwise. Example: "claude-haiku-4-5-20251001" -> "claude-haiku-4-5"
        return model;
    }

    private static void checkBedrock(String model, String family, Map<String, Object> config) {
        // TODO 2 of 7 (finish this to pass e4 and e6): refuse what Bedrock cannot take.
        // Receives the model id, its family and the config map. Throws PlatformError("model", reason) when the family is not in
        // BEDROCK_MODELS, and PlatformError("config", reason) when config has no region or an empty one (blank() tells).
        // Example: claude-sonnet-4-6 with a region throws with field "model"
    }

    private static String bedrockModelId(String family) {
        // TODO 3 of 7 (finish this to pass m1 and e1): the model id in a Bedrock body.
        // Receives the family. Returns "anthropic." followed by it. Example: "claude-opus-5-5" -> "anthropic.claude-opus-5-5"
        return family;
    }

    private static String vertexModelId(String family) {
        // TODO 4 of 7 (finish this to pass e1): the model id in a Vertex URL.
        // Receives the family. Returns HAIKU_VERTEX for HAIKU and the family itself for the others.
        // Example: "claude-haiku-4-5" -> "claude-haiku-4-5@20251001"
        return family;
    }

    private static String vertexHost(String endpoint, String family, String model) {
        // TODO 5 of 7 (finish this to pass m1 and e3): the host of a Vertex endpoint.
        // Receives the endpoint, the model's family and the model id. Returns aiplatform.googleapis.com for "global",
        // aiplatform.<endpoint>.rep.googleapis.com for "us" and "eu", and <endpoint>-aiplatform.googleapis.com for a specific region,
        // which throws PlatformError("endpoint", reason) unless the family is in REGIONAL_VERTEX_MODELS.
        // Example: ("eu", "claude-opus-5-5", "claude-opus-5-5") -> "aiplatform.eu.rep.googleapis.com"
        return "";
    }

    private static Map<String, Object> vertexBody(Map<String, Object> body) {
        // TODO 6 of 7 (finish this to pass e2): the body Vertex takes.
        // Receives the caller's body. Returns a deep copy (copyBody) without the "model" key (the model is in the URL) and with
        // "anthropic_version" set to VERTEX_VERSION; the caller's own body is never changed.
        // Example: {model: x, max_tokens: 5} -> {max_tokens: 5, anthropic_version: ...}
        return copyBody(body);
    }

    private static List<String> lacking(String platform, List<String> features) {
        // TODO 7 of 7 (finish this to pass e5): the features this platform lacks.
        // Receives a known platform and a list of known feature names. Returns the names that are in MISSING.get(platform), in the order given.
        // Example: ("vertex", ["batches", "thinking"]) -> ["batches"]
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static Object copy(Object value) {
        if (value instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) m).entrySet()) out.put(e.getKey(), copy(e.getValue()));
            return out;
        }
        if (value instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object x : l) out.add(copy(x));
            return out;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyBody(Map<String, Object> body) {
        return (Map<String, Object>) copy(body);
    }

    private static Map<String, Object> request(String url, Map<String, Object> headers, Map<String, Object> body) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("method", "POST");
        r.put("url", url);
        r.put("headers", headers);
        r.put("body", body);
        return r;
    }

    private static Map<String, Object> versionHeaders() {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("anthropic-version", ANTHROPIC_VERSION);
        h.put("content-type", "application/json");
        return h;
    }

    private static boolean blank(Object s) {
        return s == null || ((String) s).isEmpty();
    }

    static Map<String, Object> buildRequest(String platform, String model, Map<String, Object> body, Map<String, Object> config) {
        LOG.log(System.Logger.Level.DEBUG, "buildRequest input: {0} {1} {2} {3}", platform, model, body, config);
        String family = family(model);
        switch (platform) {
            case "anthropic": {
                Map<String, Object> sent = copyBody(body);
                sent.put("model", model);
                return request("https://api.anthropic.com/v1/messages", versionHeaders(), sent);
            }
            case "bedrock": {
                checkBedrock(model, family, config);
                Map<String, Object> sent = copyBody(body);
                sent.put("model", bedrockModelId(family));
                return request("https://bedrock-mantle." + config.get("region") + ".api.aws/anthropic/v1/messages", versionHeaders(), sent);
            }
            case "vertex": {
                if (!VERTEX_MODELS.contains(family)) throw new PlatformError("model", model + " is not served on Google Vertex AI");
                if (blank(config.get("project"))) throw new PlatformError("config", "a Vertex request needs a project");
                String endpoint = config.get("endpoint") == null ? "global" : (String) config.get("endpoint");
                String path = "/v1/projects/" + config.get("project") + "/locations/" + endpoint + "/publishers/anthropic/models/" + vertexModelId(family) + ":rawPredict";
                String host = vertexHost(endpoint, family, model);
                Map<String, Object> headers = new LinkedHashMap<>();
                headers.put("content-type", "application/json");
                return request("https://" + host + path, headers, vertexBody(body));
            }
            default:
                throw new PlatformError("platform", "unknown platform " + platform);
        }
    }

    static List<String> unsupportedFeatures(String platform, List<String> features) {
        Set<String> missing = MISSING.get(platform);
        if (missing == null) throw new PlatformError("platform", "unknown platform " + platform);
        for (String name : features) if (!FEATURES.contains(name)) throw new PlatformError("feature", "unknown feature " + name);
        return lacking(platform, features);
    }
}
