import java.util.List;
import java.util.Map;

/** One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md. */
final class Platforms {
    private Platforms() {}

    static Map<String, Object> buildRequest(String platform, String model, Map<String, Object> body, Map<String, Object> config) {
        // TODO: return a map {method, url, headers, body} for the platform, or throw PlatformError.
        return null;
    }

    static List<String> unsupportedFeatures(String platform, List<String> features) {
        // TODO: return the features of the list that the platform lacks, in the order given.
        return null;
    }
}
