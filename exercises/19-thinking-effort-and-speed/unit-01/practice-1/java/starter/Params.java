import java.util.Map;

/** Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract. */
final class Params {
    private Params() {}

    static Map<String, Object> buildParams(String model, int maxTokens, Map<String, Object> options) {
        // TODO: return the request parameters for the model, or throw RejectedRequest for a request the API would refuse.
        return null;
    }
}
