import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.
 *
 * <p>No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
 * neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
 * page of the Claude API documentation and the "Writing tools for agents" article.
 */
public final class ToolLint {
    static final double OVERLAP = 0.6;

    /** A tool as the lint sees it: a name, a description and the description of each parameter. */
    record Tool(String name, String description, Map<String, String> params) {}

    /** One page of a long result: the rows, the opaque cursor of the next page (null on the last) and a note for the model. */
    record Page(List<String> rows, String cursor, String note) {}

    static List<String> lint(Tool tool) {
        String text = tool.description().toLowerCase();
        List<String> found = new ArrayList<>();
        if (!(text.contains("do not use") || text.contains("not for") || text.contains("instead of"))) found.add("no-boundary");
        if (!text.contains("use when")) found.add("no-use-when");
        if (tool.params().values().stream().anyMatch(d -> d.strip().isEmpty())) found.add("param-undescribed");
        Matcher m = Pattern.compile("[.!?](?:\\s|$)").matcher(tool.description());
        int sentences = 0;
        while (m.find()) sentences++;
        if (sentences < 3) found.add("short-description");
        return found;
    }

    private static Set<String> words(Tool t) {
        Set<String> out = new HashSet<>();
        Matcher m = Pattern.compile("[a-z]{3,}").matcher(t.description().toLowerCase());
        while (m.find()) out.add(m.group());
        return out;
    }

    static double overlap(Tool a, Tool b) {
        Set<String> wa = words(a), wb = words(b);
        Set<String> both = new HashSet<>(wa);
        both.retainAll(wb);
        Set<String> either = new HashSet<>(wa);
        either.addAll(wb);
        return (double) both.size() / either.size();
    }

    static void report(String title, List<Tool> tools) {
        System.out.println(title);
        for (Tool tool : tools) {
            List<String> found = lint(tool);
            System.out.println("  " + tool.name() + ": " + (found.isEmpty() ? "clean" : String.join(", ", found)));
        }
        for (int i = 0; i < tools.size(); i++) {
            for (Tool b : tools.subList(i + 1, tools.size())) {
                double score = overlap(tools.get(i), b);
                if (score >= OVERLAP) System.out.println("  overlap: " + tools.get(i).name() + " and " + b.name() + " (" + String.format(Locale.ROOT, "%.2f", score) + ")");
            }
        }
    }

    static Page page(List<String> items, String cursor, int limit) {
        int offset = cursor == null ? 0 : Integer.parseInt(new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8).split(":")[1]);
        List<String> chunk = items.subList(Math.min(offset, items.size()), Math.min(offset + limit, items.size()));
        boolean more = offset + chunk.size() < items.size();
        String token = more ? Base64.getEncoder().encodeToString(("offset:" + (offset + chunk.size())).getBytes(StandardCharsets.UTF_8)) : null;
        String note = more ? "Showing " + chunk.size() + " of " + items.size() + " results; pass next_cursor to continue, or narrow the query with a filter." : null;
        return new Page(chunk, token, note);
    }

    static Map<String, String> params(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    static final List<Tool> POOR = List.of(
        new Tool("analyze_content", "Analyzes content and returns the result.", params("content", "The content.")),
        new Tool("analyze_document", "Analyzes a document and returns the result.", params("document", "")));

    static final List<Tool> SPLIT = List.of(
        new Tool("extract_web_results",
            "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those.",
            params("url", "The page address.")),
        new Tool("extract_data_points",
            "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those.",
            params("document_id", "The id of an uploaded document.")),
        new Tool("summarize_content",
            "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that.",
            params("text", "The text to shorten.", "max_words", "The longest summary, in words.")),
        new Tool("verify_claim_against_source",
            "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that.",
            params("claim", "One sentence to test.", "source_id", "The id of the source to test it against.")));

    public static void main(String[] args) {
        report("the set as first written", POOR);
        System.out.println();
        report("the same job, one contract per tool", SPLIT);
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 25; i++) rows.add(String.format("row-%02d", i));
        System.out.println("\na long result, paged four rows at a time");
        String cursor = null;
        for (int number = 1; number <= 2; number++) {
            Page p = page(rows, cursor, 4);
            cursor = p.cursor();
            System.out.println("  page " + number + ": " + String.join(" ", p.rows()));
            System.out.println("    note: " + (p.note() == null ? "None" : p.note()));
        }
        System.out.println("  the cursor is opaque: " + (cursor == null ? "None" : cursor));
    }
}
