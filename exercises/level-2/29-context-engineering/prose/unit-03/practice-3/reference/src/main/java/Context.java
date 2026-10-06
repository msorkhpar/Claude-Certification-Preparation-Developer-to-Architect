import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md. Messages and blocks are JSON-like maps. */
final class Context {
    private static final System.Logger LOG = System.getLogger(Context.class.getName());
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

    @SuppressWarnings("unchecked")
    private static <T> T copy(T value) {
        return (T) Json.parse(Json.stringify(value));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> blocksOf(Map<String, Object> message) {
        Object content = message.get("content");
        if (content instanceof String s) return new ArrayList<>(List.of(map("type", "text", "text", s)));
        return copy((List<Map<String, Object>>) content);
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** A user message that is not made only of tool results begins a turn. */
    @SuppressWarnings("unchecked")
    private static boolean startsTurn(Map<String, Object> message) {
        if (!"user".equals(message.get("role"))) return false;
        Object content = message.get("content");
        if (content instanceof String) return true;
        for (Map<String, Object> block : (List<Map<String, Object>>) content) if (!"tool_result".equals(block.get("type"))) return true;
        return false;
    }

    /** The messages as a list of turns: each turn is a user message that is not a tool result, and everything up to the next one. */
    static List<List<Map<String, Object>>> splitTurns(List<Map<String, Object>> messages) {
        LOG.log(System.Logger.Level.DEBUG, "splitTurns input: {0}", messages);
        List<List<Map<String, Object>>> turns = new ArrayList<>();
        for (Map<String, Object> message : messages) {
            if (startsTurn(message) || turns.isEmpty()) turns.add(new ArrayList<>());
            turns.get(turns.size() - 1).add(message);
        }
        return turns;
    }

    private static List<Map<String, Object>> flatten(List<List<Map<String, Object>>> turns) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (List<Map<String, Object>> turn : turns) out.addAll(turn);
        return out;
    }

    /** Replace the content of every result but the newest keep with the placeholder. */
    private static void clearOldest(List<Map<String, Object>> results, int keep, String placeholder) {
        for (int i = 0; i < Math.max(results.size() - keep, 0); i++) results.get(i).put("content", placeholder);
    }

    static List<Map<String, Object>> clearToolResults(List<Map<String, Object>> messages) {
        return clearToolResults(messages, 2, List.of(), "[cleared]");
    }

    /** Copy of the conversation in which every tool result but the newest keep has its content replaced by the placeholder. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> clearToolResults(List<Map<String, Object>> messages, int keep, List<String> exclude, String placeholder) {
        List<Map<String, Object>> out = copy(messages);
        Map<String, String> names = new LinkedHashMap<>();
        for (Map<String, Object> m : out) {
            if (m.get("content") instanceof List<?> blocks) {
                for (Object o : blocks) {
                    Map<String, Object> b = (Map<String, Object>) o;
                    if ("tool_use".equals(b.get("type"))) names.put((String) b.get("id"), (String) b.get("name"));
                }
            }
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> m : out) {
            if (m.get("content") instanceof List<?> blocks) {
                for (Object o : blocks) {
                    Map<String, Object> b = (Map<String, Object>) o;
                    if ("tool_result".equals(b.get("type")) && !exclude.contains(names.get((String) b.get("tool_use_id")))) results.add(b);
                }
            }
        }
        clearOldest(results, keep, placeholder);
        return out;
    }

    /** The turns of rest that remain: the oldest are dropped while the conversation is over budget, the newest always stays. */
    private static List<List<Map<String, Object>>> trim(List<List<Map<String, Object>>> pinned, List<List<Map<String, Object>>> rest, int budget) {
        while (rest.size() > 1) {
            List<List<Map<String, Object>>> all = new ArrayList<>(pinned);
            all.addAll(rest);
            if (countTokens(flatten(all)) <= budget) break;
            rest.remove(0);
        }
        return rest;
    }

    static List<Map<String, Object>> window(List<Map<String, Object>> messages, int budget) {
        return window(messages, budget, false);
    }

    /** Drop the oldest whole turns until the conversation fits; the newest turn always stays. With pin, the first turn stays too. */
    static List<Map<String, Object>> window(List<Map<String, Object>> messages, int budget, boolean pin) {
        List<List<Map<String, Object>>> turns = splitTurns(messages);
        List<List<Map<String, Object>>> pinned = pin ? new ArrayList<>(turns.subList(0, Math.min(1, turns.size()))) : new ArrayList<>();
        List<List<Map<String, Object>>> rest = trim(pinned, new ArrayList<>(pin ? turns.subList(Math.min(1, turns.size()), turns.size()) : turns), budget);
        List<List<Map<String, Object>>> all = new ArrayList<>(pinned);
        all.addAll(rest);
        return flatten(all);
    }

    /** Whether compaction has nothing to do: the conversation fits, or there are no more turns than keepTurns. */
    private static boolean leaveAlone(List<Map<String, Object>> messages, List<List<Map<String, Object>>> turns, int budget, int keepTurns) {
        return countTokens(messages) <= budget || turns.size() <= keepTurns;
    }

    /** The first kept message with the summary block placed before its own blocks. */
    private static Map<String, Object> withSummary(List<Map<String, Object>> kept, String summary) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(map("type", "text", "text", SUMMARY_OPEN + summary + SUMMARY_CLOSE));
        content.addAll(blocksOf(kept.get(0)));
        return map("role", kept.get(0).get("role"), "content", content);
    }

    static List<Map<String, Object>> compact(List<Map<String, Object>> messages, int budget, Function<List<Map<String, Object>>, String> summarise) {
        return compact(messages, budget, summarise, 1);
    }

    /** When the conversation is over budget, replace everything before the newest keepTurns turns by one summary. */
    static List<Map<String, Object>> compact(List<Map<String, Object>> messages, int budget, Function<List<Map<String, Object>>, String> summarise, int keepTurns) {
        List<List<Map<String, Object>>> turns = splitTurns(messages);
        if (leaveAlone(messages, turns, budget, keepTurns)) return new ArrayList<>(messages);
        List<Map<String, Object>> older = flatten(turns.subList(0, turns.size() - keepTurns));
        List<Map<String, Object>> kept = flatten(turns.subList(turns.size() - keepTurns, turns.size()));
        String summary = summarise.apply(older);
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(withSummary(kept, summary));
        out.addAll(kept.subList(1, kept.size()));
        return out;
    }

    /** The problem of a citation whose document exists, or null when it can be trusted. */
    private static String spanProblem(String text, Map<String, Object> cite) {
        int start = ((Number) cite.get("start_char_index")).intValue();
        int end = ((Number) cite.get("end_char_index")).intValue();
        if (start < 0 || end <= start || end > text.length()) return "bad_range";
        if (!text.substring(start, end).equals(cite.get("cited_text"))) return "text_mismatch";
        return null;
    }

    /** One {block, citation, problem} per citation that cannot be trusted, in order. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> verifyCitations(List<Map<String, Object>> blocks, List<Map<String, Object>> documents) {
        List<Map<String, Object>> problems = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            List<Map<String, Object>> citations = (List<Map<String, Object>>) blocks.get(i).get("citations");
            if (citations == null) continue;
            for (int j = 0; j < citations.size(); j++) {
                Map<String, Object> cite = citations.get(j);
                String problem = null;
                if (!"char_location".equals(cite.get("type"))) {
                    problem = "unsupported_type";
                } else {
                    int doc = ((Number) cite.get("document_index")).intValue();
                    if (doc < 0 || doc >= documents.size()) problem = "unknown_document";
                    else problem = spanProblem((String) documents.get(doc).get("text"), cite);
                }
                if (problem != null) problems.add(map("block", i, "citation", j, "problem", problem));
            }
        }
        return problems;
    }

    /** Give a new key the next number; true when the key was new. */
    private static boolean numberFor(Map<String, Integer> numbers, String key) {
        if (!numbers.containsKey(key)) {
            numbers.put(key, numbers.size() + 1);
            return true;
        }
        return false;
    }

    /** The answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order. */
    @SuppressWarnings("unchecked")
    static String footnotes(List<Map<String, Object>> blocks, List<Map<String, Object>> documents) {
        Map<String, Integer> numbers = new LinkedHashMap<>();
        List<String> sources = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        for (Map<String, Object> block : blocks) {
            out.append(block.get("text"));
            List<Map<String, Object>> citations = (List<Map<String, Object>>) block.get("citations");
            if (citations == null) continue;
            for (Map<String, Object> cite : citations) {
                String key = cite.get("document_index") + ":" + cite.get("start_char_index") + ":" + cite.get("end_char_index");
                if (numberFor(numbers, key)) {
                    int doc = ((Number) cite.get("document_index")).intValue();
                    sources.add("[" + numbers.get(key) + "] " + documents.get(doc).get("title") + ": \"" + cite.get("cited_text") + "\"");
                }
                out.append("[").append(numbers.get(key)).append("]");
            }
        }
        return out + (sources.isEmpty() ? "" : "\n\nSources:\n" + String.join("\n", sources));
    }
}
