import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A retrieval pipeline: chunking, lexical and embedding search, fusion, reranking and recall. See ../../statement.md. */
final class Retrieval {
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

    static List<String> chunk(String text, int size, int overlap) {
        // TODO: windows of size words starting size - overlap words apart; IllegalArgumentException for a size or overlap that cannot work.
        return null;
    }

    static List<Chunk> buildChunks(List<Doc> corpus, int size, int overlap) {
        // TODO: a Chunk with id "doc#n" for every window of every document, in order.
        return null;
    }

    static List<String> bm25Rank(List<Chunk> chunks, String query) {
        return bm25Rank(chunks, query, null);
    }

    static List<String> bm25Rank(List<Chunk> chunks, String query, Map<String, String> indexText) {
        // TODO: chunk ids by BM25 score, best first, ties by id; chunks with no word in common with the query are left out.
        return null;
    }

    static List<String> embeddingRank(List<Chunk> chunks, String query) {
        return embeddingRank(chunks, query, null);
    }

    static List<String> embeddingRank(List<Chunk> chunks, String query, Map<String, String> indexText) {
        // TODO: chunk ids by the dot product of embed() vectors, best first, ties by id; zero similarity is left out.
        return null;
    }

    static List<String> fuse(List<List<String>> rankings) {
        return fuse(rankings, 60);
    }

    static List<String> fuse(List<List<String>> rankings, int k) {
        // TODO: reciprocal rank fusion of the rankings; best first, ties by id.
        return null;
    }

    static List<String> rerank(String query, List<String> ids, Map<String, String> texts, BiFunction<String, String, Double> scorer, Integer topN) {
        // TODO: the given ids reordered by scorer(query, text), high to low, equal scores keeping their order, cut to topN (null keeps all).
        return null;
    }

    static double recallAtK(List<String> ids, Map<String, String> docOf, List<String> relevant, int k) {
        // TODO: the share of the relevant documents among the documents of the first k chunk ids; IllegalArgumentException when none is relevant.
        return Double.NaN;
    }

    static List<String> retrieve(List<Chunk> chunks, String query, String mode, int k) {
        return retrieve(chunks, query, mode, k, 10, null, null);
    }

    static List<String> retrieve(List<Chunk> chunks, String query, String mode, int k, int pool, Map<String, String> contexts, BiFunction<String, String, Double> scorer) {
        // TODO: the ids of the best k chunks for mode bm25, embedding or hybrid; contexts are indexed in front of a chunk's text;
        // a scorer reranks the first pool ids of the ranking.
        return null;
    }

    static double evaluate(List<Chunk> chunks, List<Query> queries, String mode, int k) {
        return evaluate(chunks, queries, mode, k, 10, null, null);
    }

    static double evaluate(List<Chunk> chunks, List<Query> queries, String mode, int k, int pool, Map<String, String> contexts, BiFunction<String, String, Double> scorer) {
        // TODO: the mean recall@k over the queries.
        return Double.NaN;
    }
}
