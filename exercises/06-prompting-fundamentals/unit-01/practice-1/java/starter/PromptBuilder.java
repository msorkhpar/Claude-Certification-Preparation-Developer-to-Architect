import java.util.Map;

/** Build a structured prompt from a spec. See ../../statement.md for the exact format. */
final class PromptBuilder {
    private PromptBuilder() {}

    static String build(Spec spec, Map<String, String> variables) {
        // TODO: render the sections in the order the statement gives.
        return "";
    }
}
