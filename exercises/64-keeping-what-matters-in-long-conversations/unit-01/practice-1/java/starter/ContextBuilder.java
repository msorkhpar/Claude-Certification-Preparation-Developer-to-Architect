import java.util.List;
import java.util.Map;

/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */
final class ContextBuilder {
    private ContextBuilder() {}

    record Fact(String value, String asOf, List<String> superseded) {}

    record FactEntry(String customer, String name, String value, String asOf) {}

    record Message(String role, String kind, String id, String text) {}

    static int estimateTokens(String text) {
        return (text.length() + 3) / 4;
    }

    static Map<String, String> trimRecord(Map<String, String> record, List<String> keep) {
        // TODO: the fields of the record named in keep, in that order, with their exact values.
        return null;
    }

    static Map<String, Fact> updateFacts(Map<String, Fact> facts, String name, String value, String asOf) {
        // TODO: a new facts map; the input is not changed.
        return null;
    }

    static String buildContext(String customer, List<FactEntry> facts, String summary, List<Message> recent) {
        // TODO: the text of the context: case facts of this customer, then the summary, then the recent messages, each under a heading.
        return null;
    }

    static List<String> missingFromSummary(String summary, List<FactEntry> facts) {
        // TODO: the names of the facts whose value the summary no longer holds.
        return null;
    }

    static List<Message> window(List<Message> messages, int budget) {
        // TODO: the newest messages that fit the token budget, keeping every tool call together with its result.
        return null;
    }
}
