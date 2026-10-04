import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

/** A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md. */
final class Pipeline {
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

    static List<Chunk> chunkSections(String docId, String text) {
        return chunkSections(docId, text, 30);
    }

    static List<Chunk> chunkSections(String docId, String text, int maxWords) {
        // TODO: one chunk per section, its text starting with "<title> > <section>. ", split at sentence ends when a section is longer than maxWords.
        return null;
    }

    static List<String> search(List<Chunk> chunks, String query) {
        return search(chunks, query, 3, null);
    }

    static List<String> search(List<Chunk> chunks, String query, int k) {
        return search(chunks, query, k, null);
    }

    static List<String> search(List<Chunk> chunks, String query, int k, Set<String> allowedDocs) {
        // TODO: the ids of the k best chunks for the query among the documents the caller may read.
        return null;
    }

    static String chooseRetrieval(int corpusTokens, String shape, String pattern) {
        // TODO: the retrieval mechanism for a corpus of this size, this data shape ("text" or "table") and this query pattern.
        return null;
    }

    static Reindexed reindex(List<Chunk> chunks, Map<String, String> docs) {
        // TODO: bring the chunks in line with the documents and report what was kept, replaced, added and removed.
        return null;
    }

    static List<String> stale(List<Chunk> chunks, Map<String, String> docs) {
        // TODO: the ids of the chunks that no longer match their source.
        return null;
    }

    static Double recallAtK(Map<String, List<String>> results, Map<String, String> relevant, int k) {
        // TODO: the share of all labelled questions whose relevant chunk is among the first k results.
        return null;
    }
}
