import java.util.List;
import java.util.Map;

/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. Results are JSON-like maps. */
final class ReviewSpec {
    private ReviewSpec() {}

    /** Phrases that name no pattern. */
    static final List<String> VAGUE = List.of("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment");

    static String buildReviewPrompt(Map<String, Object> spec, String diff) {
        // TODO: refuse a specification that is vague or incomplete (IllegalArgumentException), then write the prompt: criteria, examples, diff last.
        return null;
    }

    static Map<String, Object> categoryReport(List<Map<String, Object>> findings) {
        return categoryReport(findings, 5, 0.5);
    }

    static Map<String, Object> categoryReport(List<Map<String, Object>> findings, int minReviewed, double minPrecision) {
        // TODO: per category the number reviewed, the precision, whether to disable it and its most dismissed patterns.
        return null;
    }

    static Map<String, Object> nextStep(Map<String, Object> request, List<String> required, Map<String, String> defaults, boolean attended) {
        // TODO: proceed, ask or stop for a request with missing fields, and state the assumptions made.
        return null;
    }
}
