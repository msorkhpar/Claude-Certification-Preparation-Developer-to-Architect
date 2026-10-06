import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A retrieval pipeline: chunking, lexical and embedding search, fusion, reranking and recall. See ../../statement.md. */
final class Retrieval {
    private static final System.Logger LOG = System.getLogger(Retrieval.class.getName());
    private Retrieval() {}

    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");

    /** Lower-case words and numbers: the runs of [a-z0-9] in the text. */
    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = WORD.matcher(text.toLowerCase());
        while (m.find()) out.add(m.group());
        return out;
    }

    // ---- given: a deterministic toy embedding (not a real model) ---------------------------------------------------
    static final int DIMS = 64;

    private static long fnv1a(String text) {
        long h = 2166136261L;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) h = ((h ^ (b & 0xff)) * 16777619L) & 0xFFFFFFFFL;
        return h;
    }

    /** 64 numbers: letter-trigram counts of the tokenised text hashed into buckets, scaled to length 1.
     *  It sees spelling, not meaning: "delete" and "deleting" are close, "delete" and "remove" are not. */
    static double[] embed(String text) {
        String padded = " " + String.join(" ", tokenize(text)) + " ";
        double[] vector = new double[DIMS];
        for (int i = 0; i < padded.length() - 2; i++) vector[(int) (fnv1a(padded.substring(i, i + 3)) % DIMS)] += 1.0;
        double sum = 0;
        for (double x : vector) sum += x * x;
        double length = Math.sqrt(sum);
        if (length != 0) for (int i = 0; i < DIMS; i++) vector[i] /= length;
        return vector;
    }
    // ----------------------------------------------------------------------------------------------------------------

    /** Windows of size words that start size - overlap words apart; the last window ends at the last word. */
    static List<String> chunk(String text, int size, int overlap) {
        if (size < 1 || overlap < 0 || overlap >= size) throw new IllegalArgumentException("size must be at least 1 and overlap must be in 0 .. size - 1");
        String[] words = text.isBlank() ? new String[0] : text.trim().split("\\s+");
        return windows(words, size, overlap);
    }

    private static List<String> windows(String[] words, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < words.length; start += size - overlap) {
            chunks.add(String.join(" ", java.util.Arrays.copyOfRange(words, start, Math.min(start + size, words.length))));
            if (start + size >= words.length) break;
        }
        return chunks;
    }

    /** A Chunk with id "doc#n" for every window of every document, in order. */
    static List<Chunk> buildChunks(List<Doc> corpus, int size, int overlap) {
        List<Chunk> out = new ArrayList<>();
        for (Doc doc : corpus) {
            List<String> pieces = chunk(doc.text(), size, overlap);
            for (int n = 0; n < pieces.size(); n++) out.add(new Chunk(doc.id() + "#" + n, doc.id(), pieces.get(n)));
        }
        return out;
    }

    private static Map<String, String> texts(List<Chunk> chunks, Map<String, String> indexText) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Chunk c : chunks) out.put(c.id(), indexText != null && indexText.containsKey(c.id()) ? indexText.get(c.id()) : c.text());
        return out;
    }

    private static List<String> ordered(Map<String, Double> scores) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>(scores.entrySet());
        entries.sort(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()));
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : entries) if (e.getValue() > 0) out.add(e.getKey());
        return out;
    }

    private static double termScore(int tf, int df, int n, int length, double average, double k1, double b) {
        double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
        return idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * length / average));
    }

    static List<String> bm25Rank(List<Chunk> chunks, String query) {
        return bm25Rank(chunks, query, null);
    }

    /** Chunk ids by BM25 score (k1 = 1.5, b = 0.75), best first, ties by id, chunks with no word in common with the query left out. */
    static List<String> bm25Rank(List<Chunk> chunks, String query, Map<String, String> indexText) {
        double k1 = 1.5, b = 0.75;
        Map<String, List<String>> docs = new LinkedHashMap<>();
        texts(chunks, indexText).forEach((id, text) -> docs.put(id, tokenize(text)));
        int n = docs.size();
        double total = 0;
        for (List<String> d : docs.values()) total += d.size();
        double average = n == 0 ? 0 : total / n;
        Map<String, Integer> df = new LinkedHashMap<>();
        for (List<String> tokens : docs.values()) for (String term : new LinkedHashSet<>(tokens)) df.merge(term, 1, Integer::sum);
        Set<String> terms = new LinkedHashSet<>(tokenize(query));
        Map<String, Double> scores = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : docs.entrySet()) {
            List<String> tokens = e.getValue();
            double score = 0;
            for (String term : terms) {
                int tf = java.util.Collections.frequency(tokens, term);
                if (tf == 0) continue;
                score += termScore(tf, df.get(term), n, tokens.size(), average, k1, b);
            }
            scores.put(e.getKey(), score);
        }
        return ordered(scores);
    }

    private static double dot(double[] a, double[] b) {
        double total = 0;
        for (int i = 0; i < a.length; i++) total += a[i] * b[i];
        return total;
    }

    static List<String> embeddingRank(List<Chunk> chunks, String query) {
        return embeddingRank(chunks, query, null);
    }

    /** Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out. */
    static List<String> embeddingRank(List<Chunk> chunks, String query, Map<String, String> indexText) {
        double[] q = embed(query);
        Map<String, Double> scores = new LinkedHashMap<>();
        texts(chunks, indexText).forEach((id, text) -> {
            scores.put(id, dot(q, embed(text)));
        });
        return ordered(scores);
    }

    static List<String> fuse(List<List<String>> rankings) {
        return fuse(rankings, 60);
    }

    /** Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat in
     *  one list ignored); best first, ties by id. */
    static List<String> fuse(List<List<String>> rankings, int k) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<String> ranking : rankings) {
            int rank = 1;
            for (String id : new LinkedHashSet<>(ranking)) scores.merge(id, 1.0 / (k + rank++), Double::sum);
        }
        return ordered(scores);
    }

    static List<String> rerank(String query, List<String> ids, Map<String, String> texts, BiFunction<String, String, Double> scorer, Integer topN) {
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(Comparator.comparingDouble((String id) -> -scorer.apply(query, texts.get(id)))); // List.sort is stable
        return topN == null ? sorted : new ArrayList<>(sorted.subList(0, Math.min(topN, sorted.size())));
    }

    /** The share of the relevant documents that appear among the documents of the first k chunk ids. */
    static double recallAtK(List<String> ids, Map<String, String> docOf, List<String> relevant, int k) {
        if (relevant.isEmpty()) throw new IllegalArgumentException("relevant must not be empty");
        Set<String> found = new LinkedHashSet<>();
        for (String id : ids.subList(0, Math.min(k, ids.size()))) found.add(docOf.get(id));
        Set<String> wanted = new LinkedHashSet<>(relevant);
        int hits = 0;
        for (String d : wanted) if (found.contains(d)) hits++;
        return (double) hits / wanted.size();
    }

    private static Map<String, String> indexedText(List<Chunk> chunks, Map<String, String> contexts) {
        Map<String, String> indexText = new LinkedHashMap<>();
        for (Chunk c : chunks) if (contexts != null && contexts.containsKey(c.id())) indexText.put(c.id(), contexts.get(c.id()) + " " + c.text());
        return indexText;
    }

    static List<String> retrieve(List<Chunk> chunks, String query, String mode, int k) {
        return retrieve(chunks, query, mode, k, 10, null, null);
    }

    /** The ids of the best k chunks. mode is bm25, embedding or hybrid (fusion of both). contexts maps chunk ids to a sentence
     *  that is indexed in front of the chunk's text; scorer reranks the first pool ids of the ranking. */
    static List<String> retrieve(List<Chunk> chunks, String query, String mode, int k, int pool, Map<String, String> contexts, BiFunction<String, String, Double> scorer) {
        LOG.log(System.Logger.Level.DEBUG, "retrieve input: mode={0} k={1} query={2}", mode, k, query);
        Map<String, String> indexText = indexedText(chunks, contexts);
        List<String> lexical = bm25Rank(chunks, query, indexText);
        List<String> semantic = embeddingRank(chunks, query, indexText);
        List<String> ranked = mode.equals("bm25") ? lexical : mode.equals("embedding") ? semantic : fuse(List.of(lexical, semantic));
        if (scorer == null) return new ArrayList<>(ranked.subList(0, Math.min(k, ranked.size())));
        Map<String, String> plain = new LinkedHashMap<>();
        for (Chunk c : chunks) plain.put(c.id(), c.text());
        return rerank(query, ranked.subList(0, Math.min(pool, ranked.size())), plain, scorer, k);
    }

    static double evaluate(List<Chunk> chunks, List<Query> queries, String mode, int k) {
        return evaluate(chunks, queries, mode, k, 10, null, null);
    }

    /** The mean recall@k over the queries. */
    static double evaluate(List<Chunk> chunks, List<Query> queries, String mode, int k, int pool, Map<String, String> contexts, BiFunction<String, String, Double> scorer) {
        Map<String, String> docOf = new LinkedHashMap<>();
        for (Chunk c : chunks) docOf.put(c.id(), c.doc());
        double sum = 0;
        for (Query q : queries) sum += recallAtK(retrieve(chunks, q.query(), mode, k, pool, contexts, scorer), docOf, q.relevant(), k);
        return sum / queries.size();
    }
}
