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
        return title + " > " + name + ". " + part;
    }

    private static List<String> splitSection(String body, int maxWords) {
        List<String> pieces = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String sentence : body.split("(?<=\\.) ")) {
            if (!current.isEmpty() && (String.join(" ", current) + " " + sentence).split("\\s+").length > maxWords) {
                pieces.add(String.join(" ", current));
                current = new ArrayList<>();
            }
            current.add(sentence);
        }
        pieces.add(String.join(" ", current));
        return pieces;
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
        return wanted.stream().filter(have::contains).mapToInt(t -> t.chars().anyMatch(Character::isDigit) ? 3 : 1).sum();
    }

    private static boolean visible(Chunk chunk, Set<String> allowedDocs) {
        return allowedDocs == null || allowedDocs.contains(chunk.doc());
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
        if (corpusTokens < 200000) return "cached prompt";
        if (shape.equals("table")) return "structured query";
        if (pattern.equals("multi-hop")) return "agentic search";
        return switch (pattern) {
            case "identifier" -> "keyword index";
            case "paraphrase" -> "embedding index";
            default -> "hybrid index";
        };
    }

    private static String status(List<Chunk> oldChunks, String text) {
        if (oldChunks.isEmpty()) return "added";
        return oldChunks.get(0).version() == docVersion(text) ? "kept" : "replaced";
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
        return chunks.stream().filter(c -> !docs.containsKey(c.doc()) || c.version() != docVersion(docs.get(c.doc()))).map(Chunk::id).toList();
    }

    static Double recallAtK(Map<String, List<String>> results, Map<String, String> relevant, int k) {
        if (relevant.isEmpty()) return 0.0;
        int hits = 0;
        for (Map.Entry<String, String> e : relevant.entrySet()) {
            List<String> got = results.getOrDefault(e.getKey(), List.of());
            if (got.subList(0, Math.min(k, got.size())).contains(e.getValue())) hits++;
        }
        return Math.round(hits * 100.0 / relevant.size()) / 100.0;
    }
}
