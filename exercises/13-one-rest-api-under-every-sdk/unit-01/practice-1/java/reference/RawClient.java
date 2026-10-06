import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A raw Messages API client over an injected transport. See ../../statement.md for the contract. */
final class RawClient {
    private static final System.Logger LOG = System.getLogger(RawClient.class.getName());
    static final String URL = "https://api.anthropic.com/v1/messages";

    private RawClient() {}

    private static boolean present(String s) {
        return s != null && !s.isBlank();
    }

    private static void validate(String apiKey, List<Map<String, Object>> messages, int maxTokens) {
        if (!present(apiKey)) throw new IllegalArgumentException("apiKey is required");
        if (messages == null || messages.isEmpty()) throw new IllegalArgumentException("messages must not be empty");
        if (maxTokens < 1) throw new IllegalArgumentException("maxTokens must be at least 1");
    }

    private static Map<String, String> headersFor(String apiKey) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", "2023-06-01");
        headers.put("content-type", "application/json");
        return headers;
    }

    private static Map<String, Object> bodyFor(String model, List<Map<String, Object>> messages, int maxTokens, String system) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", (long) maxTokens);
        body.put("messages", messages);
        if (present(system)) body.put("system", system);
        return body;
    }

    static Request buildRequest(String apiKey, String model, List<Map<String, Object>> messages, int maxTokens, String system) {
        LOG.log(System.Logger.Level.DEBUG, "buildRequest input: {0} {1} {2} {3}", model, messages, maxTokens, system);
        validate(apiKey, messages, maxTokens);
        return new Request("POST", URL, headersFor(apiKey), Json.stringify(bodyFor(model, messages, maxTokens, system)));
    }

    private static String[] errorParts(Object parsed, String text) {
        if (parsed instanceof Map<?, ?> map && map.get("error") instanceof Map<?, ?> error) {
            return new String[] {error.get("type") == null ? "unknown" : String.valueOf(error.get("type")),
                error.get("message") == null ? "" : String.valueOf(error.get("message"))};
        }
        String trimmed = text.strip();
        return new String[] {"unknown", trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed};
    }

    private static String pickRequestId(String headerId, String bodyId) {
        return headerId != null ? headerId : bodyId;
    }

    private static String redact(String message, String apiKey) {
        return message.replace(apiKey, "[redacted]");
    }

    private static ApiError errorFrom(Response response, String apiKey) {
        Object parsed = null;
        try {
            parsed = Json.parse(response.body());
        } catch (IllegalArgumentException notJson) {
            // keep the raw text
        }
        String[] parts = errorParts(parsed, response.body());
        String bodyId = parsed instanceof Map<?, ?> map && map.get("request_id") != null ? String.valueOf(map.get("request_id")) : null;
        return new ApiError(response.status(), parts[0], redact(parts[1], apiKey), pickRequestId(response.headers().get("request-id"), bodyId));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseMessage(String text) {
        return (Map<String, Object>) Json.parse(text);
    }

    static Map<String, Object> sendMessages(Transport transport, String apiKey, String model, List<Map<String, Object>> messages,
                                            int maxTokens, String system) {
        Request request = buildRequest(apiKey, model, messages, maxTokens, system);
        Response response = transport.send(request);
        if (response.status() >= 200 && response.status() < 300) return parseMessage(response.body());
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
