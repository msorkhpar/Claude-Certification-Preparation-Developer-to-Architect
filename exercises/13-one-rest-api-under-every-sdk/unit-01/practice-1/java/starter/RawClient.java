import java.util.List;
import java.util.Map;

/** A raw Messages API client over an injected transport. See ../../statement.md for the contract. */
final class RawClient {
    static final String URL = "https://api.anthropic.com/v1/messages";

    private RawClient() {}

    static Request buildRequest(String apiKey, String model, List<Map<String, Object>> messages, int maxTokens, String system) {
        // TODO: method, url, the three headers and the JSON body, as the statement says.
        return new Request("GET", "", Map.of(), "{}");
    }

    static Map<String, Object> sendMessages(Transport transport, String apiKey, String model, List<Map<String, Object>> messages,
                                            int maxTokens, String system) {
        // TODO: build the request, call transport.send(request), return the parsed message or throw ApiError.
        return Map.of();
    }

    static String textOf(Map<String, Object> message) {
        // TODO: the text of the text blocks, joined.
        return "";
    }
}
