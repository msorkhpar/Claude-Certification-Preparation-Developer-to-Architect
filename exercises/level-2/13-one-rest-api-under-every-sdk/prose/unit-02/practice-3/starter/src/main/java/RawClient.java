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
        // TODO 1 of 8 (finish this to pass e2): refuse bad input before anything is sent.
        // Receives the key, the messages and maxTokens. Throws IllegalArgumentException when the key is blank (use present), the
        // messages are null or empty or maxTokens is below 1; otherwise returns nothing.
        // Example: validate("  ", List.of(), 8) -> IllegalArgumentException
    }

    private static Map<String, String> headersFor(String apiKey) {
        // TODO 2 of 8 (finish this to pass m1): the three request headers.
        // Receives the API key. Returns a map with exactly x-api-key (the key), anthropic-version (2023-06-01) and content-type
        // (application/json). Example: headersFor("k").get("anthropic-version") -> "2023-06-01"
        return new LinkedHashMap<>();
    }

    private static Map<String, Object> bodyFor(String model, List<Map<String, Object>> messages, int maxTokens, String system) {
        // TODO 3 of 8 (finish this to pass m1 and e1): the request body as a map.
        // Receives the model, the messages, maxTokens (store it as a long) and the system text (may be null). Returns a map with
        // model, max_tokens and messages, plus system only when it is present and not blank (use present).
        // Example: bodyFor("m", List.of(), 8, "  ") -> {model=m, max_tokens=8, messages=[]}
        return new LinkedHashMap<>();
    }

    static Request buildRequest(String apiKey, String model, List<Map<String, Object>> messages, int maxTokens, String system) {
        LOG.log(System.Logger.Level.DEBUG, "buildRequest input: {0} {1} {2} {3}", model, messages, maxTokens, system);
        validate(apiKey, messages, maxTokens);
        return new Request("POST", URL, headersFor(apiKey), Json.stringify(bodyFor(model, messages, maxTokens, system)));
    }

    private static String[] errorParts(Object parsed, String text) {
        // TODO 4 of 8 (finish this to pass e4 and e5): the error type and message of an error reply.
        // Receives the parsed JSON body (null when it is not JSON) and the raw body text. For a body shaped like
        // {"error": {"type": ..., "message": ...}} returns {type, message} as text; for anything else returns {"unknown", the first
        // 200 characters of the text, trimmed}.
        // Example: errorParts(Map.of("error", Map.of("type", "x", "message", "m")), "") -> {"x", "m"}, errorParts(null, " <html> ") -> {"unknown", "<html>"}
        return new String[] {"", ""};
    }

    private static String pickRequestId(String headerId, String bodyId) {
        // TODO 5 of 8 (finish this to pass e4): which request id the error carries.
        // Receives the id from the request-id header and the id from the body, either may be null. Returns the header's when there is
        // one, else the body's, else null. Example: pickRequestId("h", "b") -> "h", pickRequestId(null, "b") -> "b"
        return null;
    }

    private static String redact(String message, String apiKey) {
        // TODO 6 of 8 (finish this to pass e6): keep the key out of the error.
        // Receives the message and the API key. Returns the message with every occurrence of the key replaced by [redacted].
        // Example: redact("bad key sk-1", "sk-1") -> "bad key [redacted]"
        return message;
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
        // TODO 7 of 8 (finish this to pass e3): the message of a good reply.
        // Receives the response body text. Returns the parsed JSON object (Json.parse gives a Map).
        // Example: parseMessage("{\"stop_reason\": \"end_turn\"}") -> {stop_reason=end_turn}
        return new LinkedHashMap<>();
    }

    static Map<String, Object> sendMessages(Transport transport, String apiKey, String model, List<Map<String, Object>> messages,
                                            int maxTokens, String system) {
        Request request = buildRequest(apiKey, model, messages, maxTokens, system);
        Response response = transport.send(request);
        if (response.status() >= 200 && response.status() < 300) return parseMessage(response.body());
        throw errorFrom(response, apiKey);
    }

    static String textOf(Map<String, Object> message) {
        // TODO 8 of 8 (finish this to pass e3): the text of a message.
        // Receives a message map with a content list of blocks. Returns the text of the blocks whose type is "text", joined with
        // nothing between them; every other block type is ignored.
        // Example: a message with blocks text "a", tool_use, text "b" -> "ab"
        return "";
    }
}
