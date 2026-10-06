import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A toy byte-pair tokenizer. It is not Claude's tokenizer: it shows why tokens are not words. */
public final class Bpe {
    private static final System.Logger LOG = System.getLogger(Bpe.class.getName());
    record Rule(String left, String right) {}

    /** Learn merge rules: repeatedly join the most frequent adjacent pair (ties: first seen). */
    static List<Rule> train(String corpus, int merges) {
        List<List<String>> words = new ArrayList<>();
        for (String w : corpus.trim().split("\\s+")) {
            List<String> letters = new ArrayList<>();
            for (char c : w.toCharArray()) letters.add(String.valueOf(c));
            words.add(letters);
        }
        List<Rule> rules = new ArrayList<>();
        for (int round = 0; round < merges; round++) {
            Map<Rule, Integer> pairs = new LinkedHashMap<>();
            for (List<String> w : words) {
                for (int i = 0; i + 1 < w.size(); i++) pairs.merge(new Rule(w.get(i), w.get(i + 1)), 1, Integer::sum);
            }
            if (pairs.isEmpty()) break;
            Rule best = null;
            for (Map.Entry<Rule, Integer> e : pairs.entrySet()) {
                if (best == null || e.getValue() > pairs.get(best)) best = e.getKey();
            }
            rules.add(best);
            List<List<String>> merged = new ArrayList<>();
            for (List<String> w : words) merged.add(merge(w, best));
            words = merged;
        }
        return rules;
    }

    static List<String> merge(List<String> word, Rule pair) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < word.size()) {
            if (i + 1 < word.size() && word.get(i).equals(pair.left()) && word.get(i + 1).equals(pair.right())) {
                out.add(word.get(i) + word.get(i + 1));
                i += 2;
            } else {
                out.add(word.get(i));
                i += 1;
            }
        }
        return out;
    }

    static List<String> encode(String word, List<Rule> rules) {
        List<String> pieces = new ArrayList<>();
        for (char c : word.toCharArray()) pieces.add(String.valueOf(c));
        for (Rule rule : rules) pieces = merge(pieces, rule);
        return pieces;
    }

    public static void main(String[] args) {
        String corpus = "low low low lower lower lowest newest newest widest widest";
        List<Rule> rules = train(corpus, 6);
        System.out.println("merges: " + String.join(" ", rules.stream().map(r -> r.left() + "+" + r.right()).toList()));
        for (String word : List.of("low", "lowest", "newer", "widest", "lowish")) {
            System.out.println(String.format("%-7s", word) + " -> " + String.join(" | ", encode(word, rules)));
        }
    }
}
