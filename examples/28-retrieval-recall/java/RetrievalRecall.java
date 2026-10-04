import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A retrieval pipeline measured on recall: BM25, a toy embedding, rank fusion, a reranker and contextual indexing.
 *
 * <p>Everything is local and deterministic. The embedding is a TOY (hashed letter trigrams, no model), and the reranker is a
 * hand-written scoring function standing in for a reranking model. The knowledge base is invented. No API is called.
 */
public final class RetrievalRecall {
    record Chunk(String id, String doc, String text) {}

    record Query(String text, List<String> relevant) {}

    static final String[][] CORPUS = {
        new String[] {"refunds",
            "Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the original payment method within five business days. Digital goods are not refundable after download."},
        new String[] {"shipping",
            "Shipping times: standard delivery takes three to five business days. Express delivery arrives the next day. Orders over fifty dollars ship free to domestic addresses."},
        new String[] {"password",
            "To reset a forgotten password, open the sign in page and choose forgot password. A reset link is emailed to you and expires after one hour."},
        new String[] {"payment-errors",
            "Error E-4012 means the payment gateway rejected the card. Check the billing address and try again, or use another card. Error E-4013 means the card has expired."},
        new String[] {"warranty",
            "All hardware carries a two year warranty that covers manufacturing defects. Accidental damage is covered only with the protection plan."},
        new String[] {"deletion",
            "You can delete your account from the privacy settings. Deletion removes personal data within thirty days and cannot be undone."},
        new String[] {"api-limits",
            "The API allows sixty requests per minute for each key. Exceeding the limit returns status 429 with a retry-after header."},
        new String[] {"invoices",
            "Invoices are generated on the first day of each month and sent as PDF attachments. Past invoices can be downloaded from the billing page."},
        new String[] {"returns",
            "To return a physical item, print the prepaid label from your orders page and drop the parcel at any carrier point. Returns must arrive within fourteen days and items must be unused."},
        new String[] {"international",
            "International orders ship with a tracked carrier and take seven to twelve business days. Customs duties are paid by the recipient and are not included in the order total."},
        new String[] {"billing",
            "Your plan renews automatically each month on the billing date. You can switch plans or cancel renewal from the billing page before the date."},
        new String[] {"api-keys",
            "Create API keys in the developer console. Rotate a key by creating a new one and deleting the old one. Keys are shown only once, so store them safely."},
        new String[] {"warranty-claims",
            "Warranty claims. Hardware owners can open a claim from the support portal. Send the serial number and a photo of the damage within thirty days. We reply within two business days."}
    };

    static final List<Query> QUERIES = List.of(
        new Query("send back parcel", List.of("returns")),
        new Query("free shipping threshold", List.of("shipping")),
        new Query("tracked carrier", List.of("international")),
        new Query("deleting accounts", List.of("deletion")),
        new Query("resetting passwords", List.of("password")),
        new Query("monthly invoicing", List.of("invoices")),
        new Query("deliveries arrive", List.of("shipping")),
        new Query("refund", List.of("refunds")));

    static final int CHUNK_WORDS = 14;
    static final int OVERLAP = 4;

    /** Lower-case words and numbers: the runs of [a-z0-9] in the text. */
    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("[a-z0-9]+").matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) out.add(m.group());
        return out;
    }

    // ---- given: a deterministic toy embedding (not a real model) ---------------------------------------------------
    static final int DIMS = 64;

    static long fnv1a(String text) {
        long h = 2166136261L;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) h = ((h ^ (b & 0xFF)) * 16777619L) & 0xFFFFFFFFL;
        return h;
    }

    /**
     * 64 numbers: letter-trigram counts of the tokenised text hashed into buckets, scaled to length 1.
     * It sees spelling, not meaning: 'delete' and 'deleting' are close, 'delete' and 'remove' are not.
     */
    static double[] embed(String text) {
        String padded = " " + String.join(" ", tokenize(text)) + " ";
        double[] vector = new double[DIMS];
        for (int i = 0; i < padded.length() - 2; i++) vector[(int) (fnv1a(padded.substring(i, i + 3)) % DIMS)] += 1.0;
        double sum = 0;
        for (double x : vector) sum += x * x;
        double length = Math.sqrt(sum);
        if (length == 0) return vector;
        for (int i = 0; i < DIMS; i++) vector[i] /= length;
        return vector;
    }
    // -----------------------------------------------------------------------------------------------------------------

    /** Windows of `size` words that start `size - overlap` words apart; the last window ends at the last word. */
    static List<String> chunk(String text, int size, int overlap) {
        if (size < 1 || overlap < 0 || overlap >= size) throw new IllegalArgumentException("size must be at least 1 and overlap must be in 0 .. size - 1");
        String[] words = text.trim().isEmpty() ? new String[0] : text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < words.length) {
            chunks.add(String.join(" ", java.util.Arrays.copyOfRange(words, start, Math.min(start + size, words.length))));
            if (start + size >= words.length) break;
            start += size - overlap;
        }
        return chunks;
    }

    private static Map<String, String> texts(List<Chunk> chunks, Map<String, String> indexText) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Chunk c : chunks) out.put(c.id(), indexText == null ? c.text() : indexText.getOrDefault(c.id(), c.text()));
        return out;
    }

    private static List<String> ordered(Map<String, Double> scores) {
        return scores.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .sorted(Comparator.<Map.Entry<String, Double>>comparingDouble(e -> -e.getValue()).thenComparing(Map.Entry::getKey))
            .map(Map.Entry::getKey)
            .toList();
    }

    /** Chunk ids by BM25 score, best first, ties by id, chunks that share no word with the query left out. */
    static List<String> bm25Rank(List<Chunk> chunks, String query, Map<String, String> indexText) {
        double k1 = 1.5, b = 0.75;
        Map<String, List<String>> docs = new LinkedHashMap<>();
        texts(chunks, indexText).forEach((id, text) -> docs.put(id, tokenize(text)));
        int n = docs.size();
        double average = n == 0 ? 0.0 : docs.values().stream().mapToInt(List::size).sum() / (double) n;
        Map<String, Integer> df = new java.util.HashMap<>();
        for (List<String> tokens : docs.values()) for (String term : new HashSet<>(tokens)) df.merge(term, 1, Integer::sum);
        List<String> terms = new ArrayList<>(new LinkedHashSet<>(tokenize(query)));
        Map<String, Double> scores = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> d : docs.entrySet()) {
            List<String> tokens = d.getValue();
            double score = 0.0;
            for (String term : terms) {
                long tf = tokens.stream().filter(term::equals).count();
                if (tf == 0) continue;
                double idf = Math.log(1 + (n - df.get(term) + 0.5) / (df.get(term) + 0.5));
                score += idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * tokens.size() / average));
            }
            scores.put(d.getKey(), score);
        }
        return ordered(scores);
    }

    /** Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out. */
    static List<String> embeddingRank(List<Chunk> chunks, String query, Map<String, String> indexText) {
        double[] q = embed(query);
        Map<String, Double> scores = new LinkedHashMap<>();
        texts(chunks, indexText).forEach((id, text) -> {
            double total = 0.0;
            double[] v = embed(text);
            for (int i = 0; i < q.length; i++) total += q[i] * v[i];
            scores.put(id, total);
        });
        return ordered(scores);
    }

    /** Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat in one list ignored). */
    static List<String> fuse(List<List<String>> rankings, int k) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<String> ranking : rankings) {
            int rank = 1;
            for (String id : new LinkedHashSet<>(ranking)) scores.merge(id, 1.0 / (k + rank++), Double::sum);
        }
        return ordered(scores);
    }

    /** The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to topN. */
    static List<String> rerank(String query, List<String> ids, Map<String, String> texts, BiFunction<String, String, Double> scorer, Integer topN) {
        List<String> sorted = ids.stream().sorted(Comparator.comparingDouble(id -> -scorer.apply(query, texts.get(id)))).toList(); // a stable sort
        return topN == null ? sorted : sorted.subList(0, Math.min(topN, sorted.size()));
    }

    static Map<String, String> docOf(List<Chunk> chunks) {
        return chunks.stream().collect(Collectors.toMap(Chunk::id, Chunk::doc));
    }

    /** 1-based position of the first chunk of a relevant document, or null. */
    static Integer firstRank(List<String> ids, List<Chunk> chunks, List<String> relevant) {
        Map<String, String> docs = docOf(chunks);
        for (int i = 0; i < ids.size(); i++) if (relevant.contains(docs.get(ids.get(i)))) return i + 1;
        return null;
    }

    static double recall(List<String> ids, List<Chunk> chunks, List<String> relevant, int k) {
        Map<String, String> docs = docOf(chunks);
        Set<String> found = new HashSet<>();
        for (String id : ids.subList(0, Math.min(k, ids.size()))) found.add(docs.get(id));
        found.retainAll(relevant);
        return found.size() / (double) new HashSet<>(relevant).size();
    }

    static Map<String, List<String>> rankers(List<Chunk> chunks, String query) {
        List<String> lexical = bm25Rank(chunks, query, null);
        List<String> semantic = embeddingRank(chunks, query, null);
        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("bm25", lexical);
        out.put("embedding", semantic);
        out.put("hybrid", fuse(List.of(lexical, semantic), 60));
        return out;
    }

    static final Map<String, List<String>> SYNONYMS = Map.of("send", List.of("return", "returns"), "back", List.of("return", "returns"));

    /** Stands in for a reranking model: it reads the query with a tiny synonym list added and scores the share of those words that the chunk holds. */
    static double toyReranker(String query, String text) {
        Set<String> words = new LinkedHashSet<>();
        for (String w : tokenize(query)) {
            words.add(w);
            words.addAll(SYNONYMS.getOrDefault(w, List.of()));
        }
        Set<String> have = new HashSet<>(tokenize(text));
        return words.stream().filter(have::contains).count() / (double) words.size();
    }

    static List<Chunk> buildAll() {
        List<Chunk> chunks = new ArrayList<>();
        for (String[] doc : CORPUS) {
            List<String> pieces = chunk(doc[1], CHUNK_WORDS, OVERLAP);
            for (int n = 0; n < pieces.size(); n++) chunks.add(new Chunk(doc[0] + "#" + n, doc[0], pieces.get(n)));
        }
        return chunks;
    }

    private static String py(List<String> ids) {
        return ids.stream().map(i -> "'" + i + "'").collect(Collectors.joining(", ", "[", "]"));
    }

    private static String num(double v) {
        return v == Math.rint(v) ? String.format(Locale.ROOT, "%.1f", v) : String.valueOf(Math.round(v * 1000) / 1000.0);
    }

    public static void main(String[] args) {
        List<Chunk> chunks = buildAll();
        System.out.println(CORPUS.length + " documents, " + chunks.size() + " chunks of at most " + CHUNK_WORDS + " words, overlap " + OVERLAP);
        System.out.println(String.format("%-34s%6s%11s%8s", "rank of the first relevant chunk", "bm25", "embedding", "hybrid"));
        Map<String, Double> totals = new LinkedHashMap<>(Map.of());
        for (String mode : List.of("bm25", "embedding", "hybrid")) totals.put(mode, 0.0);
        for (Query q : QUERIES) {
            Map<String, List<String>> ranked = rankers(chunks, q.text());
            Map<String, String> cells = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : ranked.entrySet()) {
                Integer rank = firstRank(e.getValue(), chunks, q.relevant());
                cells.put(e.getKey(), rank == null ? "-" : String.valueOf(rank));
                totals.merge(e.getKey(), recall(e.getValue(), chunks, q.relevant(), 3) / QUERIES.size(), Double::sum);
            }
            System.out.println(String.format("%-34s%6s%11s%8s", q.text(), cells.get("bm25"), cells.get("embedding"), cells.get("hybrid")));
        }
        System.out.println("mean recall@3: " + totals.entrySet().stream().map(e -> "'" + e.getKey() + "': " + num(e.getValue())).collect(Collectors.joining(", ", "{", "}")));
        Map<String, String> texts = new LinkedHashMap<>();
        for (Chunk c : chunks) texts.put(c.id(), c.text());
        String query = "send back parcel";
        List<String> hybrid = rankers(chunks, query).get("hybrid");
        List<String> reranked = rerank(query, hybrid.subList(0, 10), texts, RetrievalRecall::toyReranker, 3);
        System.out.println("'" + query + "': hybrid top 3 " + py(hybrid.subList(0, 3)) + " -> reranked top 3 " + py(reranked));
        List<Chunk> bare = List.of(new Chunk("a#0", "a", "The limit is 30 days after delivery."), new Chunk("b#0", "b", "The limit is 5 users per workspace."));
        Map<String, String> context = Map.of("a#0", "Returns policy: the return window for physical orders. ");
        Map<String, String> indexed = new LinkedHashMap<>();
        for (Chunk c : bare) indexed.put(c.id(), context.getOrDefault(c.id(), "") + c.text());
        System.out.println("'return window' on the bare chunks: " + py(bm25Rank(bare, "return window", null))
            + " | with a context sentence indexed in front: " + py(bm25Rank(bare, "return window", indexed)));
    }
}
