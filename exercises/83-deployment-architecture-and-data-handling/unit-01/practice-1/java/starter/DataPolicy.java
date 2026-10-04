import java.util.List;
import java.util.Map;

/** A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md. Configurations and requirements are JSON-like maps. */
final class DataPolicy {
    private DataPolicy() {}

    static final List<String> SAFE_INPUT = List.of("tokenise", "redact"); // pii_handling values that keep identifiers out of the prompt
    static final Map<String, String> WANT = Map.of("us", "us", "eu", "eu", "other", "global"); // the residency of the deployment that may serve a user region

    static List<String> checkDeployment(Map<String, Object> config, Map<String, Object> req) {
        // TODO: the sorted list of finding ids for a deployment configuration against the requirements.
        return null;
    }

    static Map<String, Object> retentionActions(List<Map<String, Object>> entries, int maxDays, String today) {
        // TODO: a map with purge and keep, both lists sorted by id.
        return null;
    }

    static String pickDeployment(String userRegion, List<Map<String, Object>> deployments) {
        // TODO: the name of the deployment that may serve the user's region, or null.
        return null;
    }
}
