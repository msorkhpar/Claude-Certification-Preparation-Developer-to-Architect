import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md. Messages and blocks are JSON-like maps. */
final class Context {
    private Context() {}

    static final String SUMMARY_OPEN = "<summary>\n";
    static final String SUMMARY_CLOSE = "\n</summary>";

    private static int tokens(String text) {
        return (text.length() + 3) / 4;
    }

    /** Given: a rough size of a conversation. 4 per message, plus 1 per 4 characters of text and of tool result text, plus 10 per tool call. */
    @SuppressWarnings("unchecked")
    static int countTokens(List<Map<String, Object>> messages) {
        int total = 0;
        for (Map<String, Object> message : messages) {
            total += 4;
            Object content = message.get("content");
            if (content instanceof String s) {
                total += tokens(s);
                continue;
            }
            for (Map<String, Object> block : (List<Map<String, Object>>) content) {
                switch ((String) block.get("type")) {
                    case "text" -> total += tokens((String) block.get("text"));
                    case "tool_result" -> total += tokens(block.get("content") == null ? "" : (String) block.get("content"));
                    case "tool_use" -> total += 10;
                    default -> { }
                }
            }
        }
        return total;
    }

    static List<List<Map<String, Object>>> splitTurns(List<Map<String, Object>> messages) {
        // TODO: the messages as a list of turns: a turn is a user message that is not only tool results, and everything up to the next one.
        return null;
    }

    static List<Map<String, Object>> clearToolResults(List<Map<String, Object>> messages) {
        return clearToolResults(messages, 2, List.of(), "[cleared]");
    }

    static List<Map<String, Object>> clearToolResults(List<Map<String, Object>> messages, int keep, List<String> exclude, String placeholder) {
        // TODO: a copy in which every tool result but the newest keep has its content replaced by the placeholder.
        return null;
    }

    static List<Map<String, Object>> window(List<Map<String, Object>> messages, int budget) {
        return window(messages, budget, false);
    }

    static List<Map<String, Object>> window(List<Map<String, Object>> messages, int budget, boolean pin) {
        // TODO: drop the oldest whole turns until the conversation fits; the newest turn always stays; with pin the first stays too.
        return null;
    }

    static List<Map<String, Object>> compact(List<Map<String, Object>> messages, int budget, Function<List<Map<String, Object>>, String> summarise) {
        return compact(messages, budget, summarise, 1);
    }

    static List<Map<String, Object>> compact(List<Map<String, Object>> messages, int budget, Function<List<Map<String, Object>>, String> summarise, int keepTurns) {
        // TODO: over budget, replace everything before the newest keepTurns turns by one summary block.
        return null;
    }

    static List<Map<String, Object>> verifyCitations(List<Map<String, Object>> blocks, List<Map<String, Object>> documents) {
        // TODO: one {block, citation, problem} per citation that cannot be trusted, in order.
        return null;
    }

    static String footnotes(List<Map<String, Object>> blocks, List<Map<String, Object>> documents) {
        // TODO: the answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order.
        return null;
    }
}
