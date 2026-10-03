import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A raw Messages API client over an injected transport. See ../../statement.md for the contract. */
final class RawClient {
    static final String URL = "https://api.anthropic.com/v1/messages";

    private RawClient() {}

    private static boolean present(String s) {
        return s != null && !s.isBlank();
    }

    static Request buildRequest(String apiKey, String model, List<Map<String, Object>> messages, int maxTokens, String system) {
        if (!present(apiKey)) throw new IllegalArgumentException("apiKey is required");
        if (messages == null || messages.isEmpty()) throw new IllegalArgumentException("messages must not be empty");
        if (maxTokens < 1) throw new IllegalArgumentException("maxTokens must be at least 1");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", (long) maxTokens);
        body.put("messages", messages);
        if (present(system)) body.put("system", system);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", "2023-06-01");
        headers.put("content-type", "application/json");
        return new Request("POST", URL, headers, Json.stringify(body));
    }

    private static ApiError errorFrom(Response response, String apiKey) {
        String requestId = response.headers().get("request-id");
        String kind = "unknown";
        String message = response.body().strip();
        if (message.length() > 200) message = message.substring(0, 200);
        try {
            if (Json.parse(response.body()) instanceof Map<?, ?> parsed && parsed.get("error") instanceof Map<?, ?> error) {
                kind = error.get("type") == null ? "unknown" : String.valueOf(error.get("type"));
                message = error.get("message") == null ? "" : String.valueOf(error.get("message"));
                if (requestId == null && parsed.get("request_id") != null) requestId = String.valueOf(parsed.get("request_id"));
            }
        } catch (IllegalArgumentException notJson) {
            // keep the raw text
        }
        return new ApiError(response.status(), kind, message.replace(apiKey, "[redacted]"), requestId);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> sendMessages(Transport transport, String apiKey, String model, List<Map<String, Object>> messages,
                                            int maxTokens, String system) {
        Request request = buildRequest(apiKey, model, messages, maxTokens, system);
        Response response = transport.send(request);
        if (response.status() >= 200 && response.status() < 300) return (Map<String, Object>) Json.parse(response.body());
        throw errorFrom(response, apiKey);
    }

    static String textOf(Map<String, Object> message) {
        StringBuilder out = new StringBuilder();
        if (message.get("content") instanceof List<?> blocks) {
            for (Object b : blocks) {
                if (b instanceof Map<?, ?> block && "text".equals(block.get("type"))) out.append(block.get("text"));
            }
        }
        return out.toString();
    }
}
