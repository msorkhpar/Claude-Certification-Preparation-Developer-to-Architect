import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String[] parts = text.split("\n## ");
        String title = parts[0].startsWith("# ") ? parts[0].substring(2) : parts[0];
        int version = docVersion(text);
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            int cut = parts[i].indexOf('\n');
            String name = parts[i].substring(0, cut);
            List<String> pieces = new ArrayList<>();
            List<String> current = new ArrayList<>();
            for (String sentence : parts[i].substring(cut + 1).split("(?<=\\.) ")) {
                if (!current.isEmpty() && (String.join(" ", current) + " " + sentence).split("\\s+").length > maxWords) {
                    pieces.add(String.join(" ", current));
                    current = new ArrayList<>();
                }
                current.add(sentence);
            }
            pieces.add(String.join(" ", current));
            for (int n = 0; n < pieces.size(); n++) {
                String suffix = pieces.size() == 1 ? "" : "#" + (n + 1);
                chunks.add(new Chunk(docId + "/" + name + suffix, docId, version, title + " > " + name + ". " + pieces.get(n)));
            }
        }
        return chunks;
    }

    static List<String> search(List<Chunk> chunks, String query) {
        return search(chunks, query, 3, null);
    }

    static List<String> search(List<Chunk> chunks, String query, int k) {
        return search(chunks, query, k, null);
    }

    static List<String> search(List<Chunk> chunks, String query, int k, Set<String> allowedDocs) {
        Set<String> wanted = new HashSet<>(tokens(query));
        List<int[]> scored = new ArrayList<>();
        for (int n = 0; n < chunks.size(); n++) {
            if (allowedDocs != null && !allowedDocs.contains(chunks.get(n).doc())) continue;
            Set<String> have = new HashSet<>(tokens(chunks.get(n).text()));
            int score = 0;
            for (String t : wanted) if (have.contains(t)) score += t.chars().anyMatch(Character::isDigit) ? 3 : 1;
            if (score > 0) scored.add(new int[] {-score, n});
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

    static Reindexed reindex(List<Chunk> chunks, Map<String, String> docs) {
        Map<String, List<Chunk>> old = new LinkedHashMap<>();
        for (Chunk chunk : chunks) old.computeIfAbsent(chunk.doc(), d -> new ArrayList<>()).add(chunk);
        Map<String, List<String>> report = new LinkedHashMap<>();
        for (String key : List.of("added", "replaced", "removed", "kept")) report.put(key, new ArrayList<>());
        for (String doc : old.keySet()) if (!docs.containsKey(doc)) report.get("removed").add(doc);
        List<Chunk> result = new ArrayList<>();
        for (Map.Entry<String, String> e : docs.entrySet()) {
            List<Chunk> had = old.get(e.getKey());
            if (had != null && had.get(0).version() == docVersion(e.getValue())) {
                report.get("kept").add(e.getKey());
                result.addAll(had);
            } else {
                report.get(had != null ? "replaced" : "added").add(e.getKey());
                result.addAll(chunkSections(e.getKey(), e.getValue()));
            }
        }
        return new Reindexed(result, report);
    }

    static List<String> stale(List<Chunk> chunks, Map<String, String> docs) {
        List<String> out = new ArrayList<>();
        for (Chunk c : chunks) if (!docs.containsKey(c.doc()) || c.version() != docVersion(docs.get(c.doc()))) out.add(c.id());
        return out;
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
