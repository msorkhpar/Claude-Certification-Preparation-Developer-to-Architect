import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
public final class ReviewGate {
    private ReviewGate() {}

    static final List<String> SEVERITIES = List.of("low", "medium", "high");

    /** The prompt of one review run. prior: maps with file, line, category and issue; existingTests: test names. */
    public static String reviewPrompt(String diff, List<Map<String, Object>> prior, List<String> existingTests) {
        // TODO: instructions, the findings already reported, the tests that exist, and the diff last.
        return null;
    }

    /** policy: min_severity, disabled_categories and fail_on. Returns a map with exit, comments and problems. */
    public static Map<String, Object> gate(String stdout, int exitCode, JsonNode schema, Map<String, Object> policy) {
        // TODO: decide the job from the exit status and the JSON the run printed: {"exit", "comments", "problems"}. SchemaCheck.schemaCheck is provided.
        return null;
    }
}
