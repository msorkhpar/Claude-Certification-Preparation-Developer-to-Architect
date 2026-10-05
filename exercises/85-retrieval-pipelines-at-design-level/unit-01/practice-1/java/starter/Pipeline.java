import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md. */
final class Pipeline {
    private static final System.Logger LOG = System.getLogger(Pipeline.class.getName());
    private Pipeline() {}

    /** A chunk of a document: its id, the document it came from, that document's version and its text. */
    record Chunk(String id, String doc, int version, String text) {}

    /** The chunks after a re-index and what happened to each document: the keys added, replaced, removed and kept. */
    record Reindexed(List<Chunk> chunks, Map<String, List<String>> report) {}

    static final Set<String> STOP = Set.of("a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at");
    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /** A cheap fingerprint of a document's text: when it changes, the document changed. */
    static int docVersion(String text) {
        long sum = 0;
        for (int i = 0; i < text.length(); i++) sum += text.charAt(i);
        return (int) (sum % 1000003);
    }

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(text.toLowerCase());
        while (m.find()) if (!STOP.contains(m.group())) out.add(m.group());
        return out;
    }

    private static String chunkText(String title, String name, String part) {
        // TODO 1 of 8 (unlocks m1): the text of one chunk.
        // Receives the document title, the section name and one part of the section's text. Returns the part with its context in front, as
        // "<title> > <name>. <part>", so a chunk still says where it came from.
        // Example: chunkText("Annual plan", "Cancellation", "You can cancel.") -> "Annual plan > Cancellation. You can cancel."
        return part;
    }

    private static List<String> splitSection(String body, int maxWords) {
        // TODO 2 of 8 (unlocks e1): the parts of a long section.
        // Receives the text of one section and the word limit. Splits the text after each sentence end (a full stop followed by a space) and fills parts
        // with whole sentences: a part is closed before the sentence that would push it over `maxWords` words, and a single sentence longer than the limit
        // stays whole. Returns the parts as a list of strings (a short section is one part).
        // Example: "Install it. Configure it. Restart it." with maxWords 4 -> ["Install it. Configure it.", "Restart it."]
        return List.of(body);
    }

    static List<Chunk> chunkSections(String docId, String text) {
        return chunkSections(docId, text, 30);
    }

    static List<Chunk> chunkSections(String docId, String text, int maxWords) {
        LOG.log(System.Logger.Level.DEBUG, "chunkSections input: {0}", text);
        String[] parts = text.split("\n## ");
        String title = parts[0].startsWith("# ") ? parts[0].substring(2) : parts[0];
        int version = docVersion(text);
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            int cut = parts[i].indexOf('\n');
            String name = parts[i].substring(0, cut);
            List<String> pieces = splitSection(parts[i].substring(cut + 1), maxWords);
            for (int n = 0; n < pieces.size(); n++) {
                String suffix = pieces.size() == 1 ? "" : "#" + (n + 1);
                chunks.add(new Chunk(docId + "/" + name + suffix, docId, version, chunkText(title, name, pieces.get(n))));
            }
        }
        return chunks;
    }

    private static int score(Set<String> wanted, Set<String> have) {
        // TODO 3 of 8 (unlocks e2): how well a chunk matches a query.
        // Receives the set of query words and the set of words of one chunk. Returns the sum, over the query words that the chunk has, of 3 for a code (a word that
        // contains a digit, such as E-7310) and 1 for any other word; 0 when the chunk has none of them.
        // Example: wanted {"cancel", "e-7310"}, have {"cancel", "e-7310", "card"} -> 4
        return 0;
    }

    private static boolean visible(Chunk chunk, Set<String> allowedDocs) {
        // TODO 4 of 8 (unlocks e3): may this reader see this chunk?
        // Receives a chunk and the set of document ids the reader may read, or null when the reader may read all of them. Returns true when the chunk's `doc` is allowed.
        // Example: visible(chunk of "annual", Set.of("monthly")) -> false, visible(chunk of "annual", null) -> true
        return true;
    }

    static List<String> search(List<Chunk> chunks, String query) {
        return search(chunks, query, 3, null);
    }

    static List<String> search(List<Chunk> chunks, String query, int k) {
        return search(chunks, query, k, null);
    }

    static List<String> search(List<Chunk> chunks, String query, int k, Set<String> allowedDocs) {
        LOG.log(System.Logger.Level.DEBUG, "search input: {0}", query);
        Set<String> wanted = new HashSet<>(tokens(query));
        List<int[]> scored = new ArrayList<>();
        for (int n = 0; n < chunks.size(); n++) {
            if (!visible(chunks.get(n), allowedDocs)) continue;
            int points = score(wanted, new HashSet<>(tokens(chunks.get(n).text())));
            if (points > 0) scored.add(new int[] {-points, n});
        }
        scored.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) out.add(chunks.get(scored.get(i)[1]).id());
        return out;
    }

    static String chooseRetrieval(int corpusTokens, String shape, String pattern) {
        // TODO 5 of 8 (unlocks e4): the retrieval mechanism.
        // Receives the corpus size in tokens, the data shape ("text" or "table") and the query pattern. Decide in this order: under 200000 tokens "cached prompt";
        // a table "structured query"; a "multi-hop" pattern "agentic search"; then "identifier" gives "keyword index", "paraphrase" gives "embedding index" and any other pattern "hybrid index".
        // Example: chooseRetrieval(2000000, "text", "identifier") -> "keyword index"
        return null;
    }

    private static String status(List<Chunk> oldChunks, String text) {
        // TODO 6 of 8 (unlocks e5): what a re-index does with one document.
        // Receives the chunks the index already holds for the document (an empty list when it has none) and the document's current text. Returns "added" when
        // there are no chunks, "kept" when the version of the first chunk equals `docVersion(text)`, and "replaced" when it differs.
        // Example: no chunks -> "added"; chunks made from the same text -> "kept"
        return "added";
    }

    static Reindexed reindex(List<Chunk> chunks, Map<String, String> docs) {
        Map<String, List<Chunk>> old = new LinkedHashMap<>();
        for (Chunk chunk : chunks) old.computeIfAbsent(chunk.doc(), d -> new ArrayList<>()).add(chunk);
        Map<String, List<String>> report = new LinkedHashMap<>();
        for (String key : List.of("added", "replaced", "removed", "kept")) report.put(key, new ArrayList<>());
        for (String doc : old.keySet()) if (!docs.containsKey(doc)) report.get("removed").add(doc);
        List<Chunk> result = new ArrayList<>();
        for (Map.Entry<String, String> e : docs.entrySet()) {
            String state = status(old.getOrDefault(e.getKey(), List.of()), e.getValue());
            report.get(state).add(e.getKey());
            result.addAll(state.equals("kept") ? old.get(e.getKey()) : chunkSections(e.getKey(), e.getValue()));
        }
        return new Reindexed(result, report);
    }

    static List<String> stale(List<Chunk> chunks, Map<String, String> docs) {
        // TODO 7 of 8 (unlocks e6): the chunks that no longer match their source.
        // Receives the chunks and a map of the current documents (id to text). Returns the ids of the chunks, in order, whose document is gone or whose
        // version differs from `docVersion` of the current text.
        // Example: a chunk of a document that is no longer in `docs` is stale
        return null;
    }

    static Double recallAtK(Map<String, List<String>> results, Map<String, String> relevant, int k) {
        // TODO 8 of 8 (unlocks e7): the share of questions answered in the first k results.
        // Receives `results` (question to the list of chunk ids returned) and `relevant` (every labelled question to the chunk id that answers it). Returns the
        // number of labelled questions whose relevant id is among the first k results, divided by the number of labelled questions, rounded to two decimals;
        // a question with no results counts as a miss, and 0.0 when there are no labelled questions.
        // Example: 2 of 3 questions hit -> 0.67
        return null;
    }
}
