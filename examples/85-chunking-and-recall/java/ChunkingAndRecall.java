import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.
 *
 * <p>The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
 * offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
 * documentation page "Embeddings". Nothing here calls a model.
 */
public final class ChunkingAndRecall {
    private static final System.Logger LOG = System.getLogger(ChunkingAndRecall.class.getName());
    private ChunkingAndRecall() {}

    /** A chunk: its id and its text. */
    record Chunk(String id, String text) {}

    static final Set<String> STOP = Set.of("a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at");
    static final Map<String, String> DOCS = new LinkedHashMap<>();

    static {
        DOCS.put("monthly", "# Monthly plan\n## Cancellation\nYou can cancel at any time and the current month is not refunded.");
        DOCS.put("annual", "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.");
        DOCS.put("refunds", "# Refund policy\n## Eligibility\nCustomers may return items within 30 days of delivery.\n## Exceptions\nItems marked final sale cannot be returned, except when they arrive damaged.\n## Process\nRefunds go back to the original payment method within 5 business days.");
        DOCS.put("errors", "# Error codes\n## E-7310\nThe warehouse could not reserve stock. Retry after the next stock sync.\n## E-4021\nThe payment gateway rejected the card. Ask for another card.");
    }

    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(text.toLowerCase());
        while (m.find()) if (!STOP.contains(m.group())) out.add(m.group());
        return out;
    }

    /** Cut every `size` words, whatever the words mean. */
    static List<Chunk> chunkFixed(String docId, String text, int size) {
        String[] words = text.trim().split("\\s+");
        List<Chunk> out = new ArrayList<>();
        for (int i = 0; i < words.length; i += size) out.add(new Chunk(docId + "#" + i / size, String.join(" ", Arrays.copyOfRange(words, i, Math.min(i + size, words.length)))));
        return out;
    }

    /** Cut at the headings; with context, each chunk starts with the document title and its section name, so it can be found and read alone. */
    static List<Chunk> chunkSections(String docId, String text, boolean context) {
        LOG.log(System.Logger.Level.DEBUG, "chunkSections input: {0}", text);
        String[] parts = text.split("\n## ");
        String title = parts[0].startsWith("# ") ? parts[0].substring(2) : parts[0];
        List<Chunk> out = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            int cut = parts[i].indexOf('\n');
            String name = parts[i].substring(0, cut);
            out.add(new Chunk(docId + "/" + name, (context ? title + " > " + name + ". " : "") + parts[i].substring(cut + 1)));
        }
        return out;
    }

    static List<Chunk> index(Map<String, String> docs, boolean context) {
        List<Chunk> out = new ArrayList<>();
        docs.forEach((docId, text) -> out.addAll(chunkSections(docId, text, context)));
        return out;
    }

    private static boolean hasDigit(String t) {
        return t.chars().anyMatch(Character::isDigit);
    }

    /** Words that appear in the chunk score one, a token with a digit (a code or an id) scores three; ties keep the index order. */
    static List<String> lexical(List<Chunk> chunks, String query, int k) {
        Set<String> wanted = new HashSet<>(tokens(query));
        List<int[]> scored = new ArrayList<>();
        for (int n = 0; n < chunks.size(); n++) {
            Set<String> have = new HashSet<>(tokens(chunks.get(n).text()));
            int score = 0;
            for (String t : wanted) if (have.contains(t)) score += hasDigit(t) ? 3 : 1;
            if (score > 0) scored.add(new int[] {-score, n});
        }
        scored.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) out.add(chunks.get(scored.get(i)[1]).id());
        return out;
    }

    static List<String> lexical(List<Chunk> chunks, String query) {
        return lexical(chunks, query, 3);
    }

    /** Reciprocal rank fusion, in integers so that every language ranks the same: each list adds 1000000 // (k + rank) to a chunk. */
    static List<String> fuse(List<List<String>> rankings, int k) {
        Map<String, Integer> score = new HashMap<>();
        for (List<String> ranking : rankings) for (int i = 0; i < ranking.size(); i++) score.merge(ranking.get(i), 1000000 / (k + i + 1), Integer::sum);
        List<String> ids = new ArrayList<>(score.keySet());
        ids.sort((a, b) -> score.get(a).equals(score.get(b)) ? a.compareTo(b) : Integer.compare(score.get(b), score.get(a)));
        return ids;
    }

    static List<String> fuse(List<List<String>> rankings) {
        return fuse(rankings, 60);
    }

    /** True when one retrieved chunk holds the whole answer sentence. */
    static boolean holds(List<Chunk> chunks, List<String> chunkIds, String answer) {
        Map<String, String> texts = new HashMap<>();
        for (Chunk c : chunks) texts.put(c.id(), c.text());
        return chunkIds.stream().anyMatch(id -> texts.get(id).contains(answer));
    }

    /** The shortcut: add the new chunks and leave the old ones where they are. */
    static List<Chunk> reindexAdditive(List<Chunk> chunks, String docId, String text) {
        List<Chunk> out = new ArrayList<>(chunks);
        out.addAll(chunkSections(docId, text, true));
        return out;
    }

    /** Drop every chunk of the document first, then add the new ones. */
    static List<Chunk> reindexReplace(List<Chunk> chunks, String docId, String text) {
        List<Chunk> out = new ArrayList<>();
        for (Chunk c : chunks) if (!c.id().startsWith(docId + "/")) out.add(c);
        out.addAll(chunkSections(docId, text, true));
        return out;
    }

    /** Chunks that no longer match what their source says now. */
    static List<String> stale(List<Chunk> chunks, Map<String, String> docs) {
        Set<Chunk> current = new HashSet<>(index(docs, true));
        List<String> out = new ArrayList<>();
        for (Chunk c : chunks) if (!current.contains(c)) out.add(c.id());
        return out;
    }

    /** The cheapest mechanism that fits: a corpus under 200,000 tokens fits a cached prompt; a table is queried; several hops need an agent that searches; otherwise the query pattern picks the index. */
    static String chooseRetrieval(int corpusTokens, String shape, String pattern) {
        if (corpusTokens < 200000) return "cached prompt";
        if (shape.equals("table")) return "structured query";
        if (pattern.equals("multi-hop")) return "agentic search";
        return switch (pattern) {
            case "identifier" -> "keyword index";
            case "paraphrase" -> "embedding index";
            default -> "hybrid index";
        };
    }

    /** Where a question went wrong, judged by retrieval and by generation separately; a right answer without its evidence is a risk of its own. */
    static String layer(boolean evidenceRetrieved, boolean answerCorrect) {
        if (answerCorrect) return evidenceRetrieved ? "ok" : "unsupported";
        return evidenceRetrieved ? "generation" : "retrieval";
    }

    private static String yes(boolean flag) {
        return flag ? "yes" : "no";
    }

    private static String names(List<String> items) {
        return items.isEmpty() ? "none" : String.join(", ", items);
    }

    private static List<String> ids(List<Chunk> chunks) {
        return chunks.stream().map(Chunk::id).toList();
    }

    public static void main(String[] args) {
        String answer = "Items marked final sale cannot be returned, except when they arrive damaged.";
        List<Chunk> fixed = chunkFixed("refunds", DOCS.get("refunds"), 12);
        List<Chunk> sections = chunkSections("refunds", DOCS.get("refunds"), false);
        System.out.println("cut every 12 words: " + fixed.size() + " chunks, the whole rule in one chunk: " + yes(holds(fixed, ids(fixed), answer)));
        System.out.println("cut at headings:    " + sections.size() + " chunks, the whole rule in one chunk: " + yes(holds(sections, ids(sections), answer)));
        for (boolean context : new boolean[] {false, true}) {
            System.out.println("query 'cancel the annual plan', chunks " + (context ? "with" : "without") + " context: top chunk " + lexical(index(DOCS, context), "cancel the annual plan", 1).get(0));
        }
        List<Chunk> chunks = index(DOCS, true);
        Map<String, List<String>> semantic = new LinkedHashMap<>();
        semantic.put("what does E-7310 mean", List.of("errors/E-4021", "errors/E-7310", "refunds/Process"));
        semantic.put("when will I be reimbursed", List.of("refunds/Process", "annual/Cancellation", "monthly/Cancellation"));
        Map<String, String> answers = Map.of("what does E-7310 mean", "The warehouse could not reserve stock.", "when will I be reimbursed", "Refunds go back to the original payment method within 5 business days.");
        System.out.println(String.format("%-28s%-9s%-10s%s", "query", "lexical", "semantic", "hybrid"));
        semantic.forEach((query, ranking) -> {
            List<String> lex = lexical(chunks, query);
            List<String> hybrid = fuse(List.of(lex, ranking));
            System.out.println(String.format("%-28s%-9s%-10s%s", query, yes(holds(chunks, lex.subList(0, Math.min(1, lex.size())), answers.get(query))),
                yes(holds(chunks, ranking.subList(0, 1), answers.get(query))), yes(holds(chunks, hybrid.subList(0, 1), answers.get(query)))));
        });
        System.out.println("mechanism by corpus size, data shape and query pattern:");
        Object[][] rows = {{50000, "text", "identifier"}, {5000000, "table", "paraphrase"}, {5000000, "text", "multi-hop"}, {5000000, "text", "identifier"}, {5000000, "text", "paraphrase"}, {5000000, "text", "mixed"}};
        for (Object[] r : rows) System.out.println(String.format("  %8d tokens  %-6s%-11s-> %s", (int) r[0], r[1], r[2], chooseRetrieval((int) r[0], (String) r[1], (String) r[2])));
        String edited = DOCS.get("refunds").replace("within 30 days", "within 60 days");
        Map<String, String> live = new LinkedHashMap<>(DOCS);
        live.put("refunds", edited);
        List<Chunk> additive = reindexAdditive(chunks, "refunds", edited);
        List<Chunk> replaced = reindexReplace(chunks, "refunds", edited);
        System.out.println("after the window changes from 30 to 60 days, add the new chunks only: " + additive.size() + " chunks, stale " + names(stale(additive, live)));
        System.out.println("after the window changes from 30 to 60 days, replace the document's chunks: " + replaced.size() + " chunks, stale " + names(stale(replaced, live)));
        boolean[][] outcomes = {{true, true}, {true, true}, {true, true}, {true, true}, {true, false}, {false, false}, {false, false}, {false, true}};
        Map<String, Integer> counts = new TreeMap<>();
        int retrieved = 0, correct = 0;
        for (boolean[] o : outcomes) {
            counts.merge(layer(o[0], o[1]), 1, Integer::sum);
            if (o[0]) retrieved++;
            if (o[1]) correct++;
        }
        StringBuilder byLayer = new StringBuilder();
        counts.forEach((k, v) -> byLayer.append(byLayer.length() == 0 ? "" : ", ").append(k).append(' ').append(v));
        System.out.println("8 questions: evidence retrieved for " + retrieved + ", answers correct " + correct + ", by layer " + byLayer);
    }
}
