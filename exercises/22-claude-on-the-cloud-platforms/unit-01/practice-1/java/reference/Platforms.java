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
        return model.startsWith(HAIKU) ? HAIKU : model;
    }

    private static void checkBedrock(String model, String family, Map<String, Object> config) {
        if (!BEDROCK_MODELS.contains(family)) throw new PlatformError("model", model + " is not served by Claude in Amazon Bedrock");
        if (blank(config.get("region"))) throw new PlatformError("config", "a Bedrock request needs a region");
    }

    private static String bedrockModelId(String family) {
        return "anthropic." + family;
    }

    private static String vertexModelId(String family) {
        return family.equals(HAIKU) ? HAIKU_VERTEX : family;
    }

    private static String vertexHost(String endpoint, String family, String model) {
        if (endpoint.equals("global")) return "aiplatform.googleapis.com";
        if (endpoint.equals("us") || endpoint.equals("eu")) return "aiplatform." + endpoint + ".rep.googleapis.com";
        if (!REGIONAL_VERTEX_MODELS.contains(family)) {
            throw new PlatformError("endpoint", model + " is served on the global and multi-region endpoints, not on a specific region");
        }
        return endpoint + "-aiplatform.googleapis.com";
    }

    private static Map<String, Object> vertexBody(Map<String, Object> body) {
        Map<String, Object> sent = copyBody(body);
        sent.remove("model");
        sent.put("anthropic_version", VERTEX_VERSION);
        return sent;
    }

    private static List<String> lacking(String platform, List<String> features) {
        List<String> out = new ArrayList<>();
        for (String name : features) if (MISSING.get(platform).contains(name)) out.add(name);
        return out;
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
