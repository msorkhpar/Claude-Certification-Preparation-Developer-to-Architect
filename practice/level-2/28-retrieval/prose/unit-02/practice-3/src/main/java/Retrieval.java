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
        // TODO 1 of 7 (finish this to pass e1): the windows over an array of words.
        // Receives the words, size and overlap (already checked). Returns the windows as strings of the words joined by one space: each holds
        // size words, the next starts size - overlap words later, and the loop stops at the first window that reaches the last word (so the
        // last window ends at the last word and none is repeated). No words gives an empty list.
        // Example: windows(new String[] {"a", "b", "c", "d", "e"}, 3, 1) -> ["a b c", "c d e"]
        return new ArrayList<>();
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
        // TODO 2 of 7 (finish this to pass e2): the BM25 score one query term gives one chunk.
        // Receives the term's count in the chunk (tf, at least 1), the number of chunks that hold the term (df), the number of chunks (n), the
        // chunk's length in tokens, the average length, k1 and b. Returns idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * length / average)) with
        // idf = ln(1 + (n - df + 0.5) / (df + 0.5)). A rare term has a large idf; a short chunk gets a larger score than a long one.
        // Example: termScore(1, 1, 4, 5, 5.0, 1.5, 0.75) is about 1.2 times ln(1 + 3.5 / 1.5)
        return 0.0;
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
        // TODO 3 of 7 (finish this to pass m1): the dot product of two vectors of the same length.
        // Receives two arrays of numbers. Returns the sum of the products of the numbers in the same place (the vectors of embed() have length 1,
        // so this is their cosine). Example: dot(new double[] {1, 2}, new double[] {3, 4}) -> 11.0
        return 0.0;
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
        // TODO 4 of 7 (finish this to pass e3): reciprocal rank fusion of the lists in rankings.
        // Returns the ids best first: an id scores the sum of 1 / (k + rank) over the lists that hold it (rank starts at 1, a repeat in one list is
        // ignored); ordered(scores) sorts a Map of scores best first, ties by id, and leaves out scores of 0 or less.
        // Example: fuse(List.of(List.of("a", "b"), List.of("b", "c")), 60) -> ["b", "a", "c"]
        return new ArrayList<>();
    }

    static List<String> rerank(String query, List<String> ids, Map<String, String> texts, BiFunction<String, String, Double> scorer, Integer topN) {
        // TODO 5 of 7 (finish this to pass e4): the ids ordered by scorer.apply(query, texts.get(id)) high to low.
        // Equal scores keep their input order (List.sort is stable); cut to topN when it is not null. Only the given ids take part.
        // Example: rerank("q", List.of("a", "b"), Map.of("a", "x", "b", "xx"), (q, t) -> (double) t.length(), null) -> ["b", "a"]
        return new ArrayList<>(ids);
    }

    /** The share of the relevant documents that appear among the documents of the first k chunk ids. */
    static double recallAtK(List<String> ids, Map<String, String> docOf, List<String> relevant, int k) {
        if (relevant.isEmpty()) throw new IllegalArgumentException("relevant must not be empty");
        // TODO 6 of 7 (finish this to pass e5): the share of the relevant documents found among the documents of the first k chunk ids.
        // A document counts once however many of its chunks are there; relevant is not empty here. Returns a number from 0 to 1.
        // Example: recallAtK(List.of("a#0", "a#1", "b#0"), Map.of("a#0", "a", "a#1", "a", "b#0", "b"), List.of("a", "c"), 3) -> 0.5
        return 0.0;
    }

    private static Map<String, String> indexedText(List<Chunk> chunks, Map<String, String> contexts) {
        // TODO 7 of 7 (finish this to pass e6): the text each chunk is indexed under when it has a context sentence.
        // Receives the chunks and contexts (null or a map from chunk id to a sentence). Returns a map from chunk id to the sentence, a space
        // and the chunk's text, for the chunks that have a sentence only; the chunks themselves are not changed.
        // Example: indexedText(List.of(new Chunk("a#0", "a", "x")), Map.of("a#0", "About a.")) -> {a#0=About a. x}
        return new LinkedHashMap<>();
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
