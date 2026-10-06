import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlatformsTest {
    private static final String OPUS = "claude-opus-5-5", SONNET5 = "claude-sonnet-5-5", SONNET46 = "claude-sonnet-4-6", HAIKU = "claude-haiku-4-5-20251001", FABLE = "claude-fable-5-1";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> body() {
        return map("model", "ignored-by-the-builder", "max_tokens", 256, "messages", new ArrayList<>(List.of(map("role", "user", "content", "Hello, Claude"))));
    }

    private static Map<String, Object> built(String platform, String model, Object... config) {
        Map<String, Object> r = Platforms.buildRequest(platform, model, body(), map(config));
        return r == null ? map() : r;
    }

    @SuppressWarnings("unchecked")
    private static Object g(Map<String, Object> request, String... path) {
        Object cur = request;
        for (String step : path) cur = cur instanceof Map<?, ?> m ? ((Map<String, Object>) m).get(step) : null;
        return cur;
    }

    private static String fieldOf(String platform, String model, Object... config) {
        try {
            Platforms.buildRequest(platform, model, body(), map(config));
        } catch (PlatformError e) {
            return e.field();
        } catch (RuntimeException e) {
            return "crash: " + e;
        }
        return null;
    }

    private static boolean endsWith(Object url, String suffix) {
        return url instanceof String s && s.endsWith(suffix);
    }

    @Test
    void m1_theSameMessageTakesThreeShapes() {
        Map<String, Object> direct = built("anthropic", OPUS);
        assertEquals("https://api.anthropic.com/v1/messages", g(direct, "url"));
        assertEquals(map("anthropic-version", "2023-06-01", "content-type", "application/json"), g(direct, "headers"));
        assertEquals(OPUS, g(direct, "body", "model"));
        Map<String, Object> bedrock = built("bedrock", OPUS, "region", "us-east-1");
        assertEquals("https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages", g(bedrock, "url"));
        assertEquals("2023-06-01", g(bedrock, "headers", "anthropic-version"));
        assertEquals("anthropic.claude-opus-5-5", g(bedrock, "body", "model"));
        Map<String, Object> vertex = built("vertex", OPUS, "project", "my-project");
        assertEquals("https://aiplatform.googleapis.com/v1/projects/my-project/locations/global/publishers/anthropic/models/claude-opus-5-5:rawPredict", g(vertex, "url"));
        Object headers = g(vertex, "headers");
        assertTrue(headers instanceof Map<?, ?> h && !h.containsKey("anthropic-version"));
        assertEquals("POST", g(vertex, "method"));
    }

    @Test
    void e1_modelIdsChangeWithThePlatform() {
        assertEquals(HAIKU, g(built("anthropic", HAIKU), "body", "model"));
        assertEquals("anthropic.claude-haiku-4-5", g(built("bedrock", HAIKU, "region", "eu-west-1"), "body", "model"));
        assertEquals("anthropic.claude-haiku-4-5", g(built("bedrock", "claude-haiku-4-5", "region", "eu-west-1"), "body", "model"));
        assertEquals("anthropic.claude-fable-5-1", g(built("bedrock", FABLE, "region", "us-east-1"), "body", "model"));
        assertTrue(endsWith(g(built("vertex", HAIKU, "project", "p"), "url"), "/models/claude-haiku-4-5@20251001:rawPredict"));
        assertTrue(endsWith(g(built("vertex", SONNET5, "project", "p"), "url"), "/models/claude-sonnet-5-5:rawPredict"));
    }

    @Test
    void e2_vertexMovesTheModelIntoTheUrlAndTheVersionIntoTheBody() {
        Map<String, Object> body = body();
        Map<String, Object> sent = Platforms.buildRequest("vertex", OPUS, body, map("project", "p"));
        Object vertexBody = g(sent == null ? map() : sent, "body");
        assertTrue(vertexBody instanceof Map<?, ?> m && !m.containsKey("model") && "vertex-2023-10-16".equals(m.get("anthropic_version"))
                && Integer.valueOf(256).equals(m.get("max_tokens")) && body().get("messages").equals(m.get("messages")));
        assertEquals(body(), body); // the caller's body is left as it was
        Map<String, Object> other = Platforms.buildRequest("bedrock", OPUS, body, map("region", "us-east-1"));
        Object bedrockBody = g(other == null ? map() : other, "body");
        assertTrue(bedrockBody instanceof Map<?, ?> m && !m.containsKey("anthropic_version") && Integer.valueOf(256).equals(m.get("max_tokens")));
        assertEquals(body(), body);
    }

    @Test
    void e3_vertexEndpointsGlobalMultiRegionAndRegional() {
        assertEquals("https://aiplatform.us.rep.googleapis.com/v1/projects/p/locations/us/publishers/anthropic/models/claude-opus-5-5:rawPredict",
                g(built("vertex", OPUS, "project", "p", "endpoint", "us"), "url"));
        Object eu = g(built("vertex", SONNET5, "project", "p", "endpoint", "eu"), "url");
        assertTrue(eu instanceof String s && s.startsWith("https://aiplatform.eu.rep.googleapis.com/v1/projects/p/locations/eu/"));
        assertEquals("https://europe-west1-aiplatform.googleapis.com/v1/projects/p/locations/europe-west1/publishers/anthropic/models/claude-sonnet-4-6:rawPredict",
                g(built("vertex", SONNET46, "project", "p", "endpoint", "europe-west1"), "url"));
        assertEquals("endpoint", fieldOf("vertex", OPUS, "project", "p", "endpoint", "europe-west1"));
        assertEquals("endpoint", fieldOf("vertex", HAIKU, "project", "p", "endpoint", "us-east5"));
    }

    @Test
    void e4_aPlatformServesOnlyItsOwnModels() {
        assertEquals("model", fieldOf("bedrock", SONNET46, "region", "us-east-1"));
        assertEquals("model", fieldOf("bedrock", "claude-nonexistent-9", "region", "us-east-1"));
        assertEquals("model", fieldOf("vertex", "claude-nonexistent-9", "project", "p"));
        assertNull(fieldOf("vertex", SONNET46, "project", "p"));
        assertEquals("platform", fieldOf("mystery", OPUS));
    }

    @Test
    void e5_eachPlatformLacksItsOwnFeatures() {
        List<String> names = List.of("batches", "fast_mode", "prompt_caching", "thinking", "web_search", "structured_outputs", "files_api", "tool_use");
        assertEquals(List.of(), Platforms.unsupportedFeatures("anthropic", names));
        assertEquals(List.of("batches", "fast_mode", "web_search", "structured_outputs", "files_api"), Platforms.unsupportedFeatures("bedrock", names));
        assertEquals(List.of("batches", "fast_mode", "files_api"), Platforms.unsupportedFeatures("vertex", names));
        assertEquals(List.of("web_fetch", "mcp_connector"), Platforms.unsupportedFeatures("vertex", List.of("web_fetch", "mcp_connector", "citations")));
    }

    @Test
    void e6_aRequestNeedsThePlaceItIsSentTo() {
        assertEquals("config", fieldOf("bedrock", OPUS));
        assertEquals("config", fieldOf("vertex", OPUS));
        assertEquals("config", fieldOf("bedrock", OPUS, "region", ""));
        assertNull(fieldOf("anthropic", OPUS));
    }
}
