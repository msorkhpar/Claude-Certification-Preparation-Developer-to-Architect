import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A claims assistant on one page of code: identifiers swapped for tokens before anything is sent, retrieval that filters by the reader's rights first and refuses stale evidence, a source check, a route to a person, a trace that holds no content, and a release gate that protects the costly segment.
 *
 * The documents, requests, answers and cases are invented, and the model is a scripted answer, so nothing here calls a model. The thresholds (a confidence of 95) are design values.
 */
public class ClaimsAssistant {
    record Chunk(String id, String doc, int version, String text) {}

    record Request(String id, String text, Set<String> allowed, String consequence, String quote, int confidence) {}

    record Case(String id, String segment, boolean oldOk, boolean newOk) {}

    record Trace(String request, String chunk, String outcome, int chars) {}

    record Handled(String sent, Trace trace) {}

    record Tokenised(String sent, Map<String, String> vault) {}

    static final Map<String, Integer> CURRENT = Map.of("policy", 3, "contracts", 1);
    static final List<Chunk> INDEX = List.of(
        new Chunk("policy-1", "policy", 3, "Claims must be reported within 30 days of the loss."),
        new Chunk("policy-2", "policy", 3, "Water damage is covered up to 5,000 per claim."),
        new Chunk("contract-9", "contracts", 1, "Partner commission is 12 percent of premiums."));
    static final List<Chunk> STALE_INDEX = List.of(INDEX.get(0), new Chunk("policy-2-old", "policy", 2, "Water damage is covered up to 3,000 per claim."), INDEX.get(2));

    /** Identifiers become tokens before the text leaves the caller; the map from token to value stays here. */
    static Tokenised tokenise(String text) {
        Map<String, String> vault = new LinkedHashMap<>();
        Matcher m = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+").matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = m.group();
            String token = null;
            for (Map.Entry<String, String> e : vault.entrySet()) if (e.getValue().equals(value)) token = e.getKey();
            if (token == null) {
                token = "<EMAIL_" + (vault.size() + 1) + ">";
                vault.put(token, value);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(token));
        }
        m.appendTail(out);
        return new Tokenised(out.toString(), vault);
    }

    static Set<String> words(String text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("[a-z]+").matcher(text.toLowerCase());
        while (m.find()) if (m.group().length() > 3) out.add(m.group());
        return out;
    }

    /** The reader's rights are applied before ranking; the best match wins, a tie goes to the smaller id, and no overlap is no evidence. */
    static Chunk retrieve(String question, Set<String> allowed, List<Chunk> index) {
        Set<String> mine = words(question);
        Chunk best = null;
        int bestOverlap = 0;
        List<Chunk> candidates = new ArrayList<>(index.stream().filter(c -> allowed.contains(c.doc())).toList());
        candidates.sort(Comparator.comparing(Chunk::id));
        for (Chunk c : candidates) {
            Set<String> shared = new LinkedHashSet<>(words(c.text()));
            shared.retainAll(mine);
            if (shared.size() > bestOverlap) {
                bestOverlap = shared.size();
                best = c;
            }
        }
        return best;
    }

    /** One request through the chain; the outcome says why a request was held. */
    static Handled handle(Request request, List<Chunk> index) {
        String sent = tokenise(request.text()).sent();
        Chunk chunk = retrieve(sent, request.allowed(), index);
        String outcome;
        if (chunk == null) outcome = "hold: no evidence";
        else if (chunk.version() != CURRENT.get(chunk.doc())) outcome = "hold: stale evidence (" + chunk.id() + " v" + chunk.version() + ", current v" + CURRENT.get(chunk.doc()) + ")";
        else if (!chunk.text().contains(request.quote())) outcome = "hold: unsupported";
        else if (request.consequence().equals("high")) outcome = "human";
        else outcome = request.confidence() >= 95 ? "auto" : "review";
        return new Handled(sent, new Trace(request.id(), chunk == null ? "none" : chunk.id() + "@v" + chunk.version(), outcome, sent.length()));
    }

    /** Tail-based: a trace that was held or reached a person is kept, the rest are sampled elsewhere. */
    static boolean keep(Trace trace) {
        return trace.outcome().startsWith("hold") || trace.outcome().equals("human");
    }

    /** A change ships only when no protected segment loses an answer and the losses do not outnumber the gains. */
    static String release(List<Case> cases, Set<String> protectedSegments) {
        int lost = 0;
        int gained = 0;
        TreeSet<String> hit = new TreeSet<>();
        for (Case c : cases) {
            if (c.oldOk() && !c.newOk()) {
                lost++;
                if (protectedSegments.contains(c.segment())) hit.add(c.segment());
            }
            if (c.newOk() && !c.oldOk()) gained++;
        }
        if (!hit.isEmpty()) return "no-go: protected segment lost answers: " + String.join(", ", hit);
        if (lost > gained) return "no-go: net loss: lost " + lost + ", gained " + gained;
        return "go: lost " + lost + ", gained " + gained;
    }

    public static void main(String[] args) {
        String water = "Water damage is covered up to 5,000 per claim.";
        String late = "Claims must be reported within 30 days of the loss.";
        Set<String> policy = Set.of("policy");
        List<Request> requests = List.of(
            new Request("r1", "How much does the policy cover for water damage?", policy, "low", water, 97),
            new Request("r2", "How much does the policy cover for water damage?", policy, "low", water, 97),
            new Request("r3", "Can I get a refund of 400 for water damage?", policy, "high", water, 99),
            new Request("r4", "What is the partner commission?", policy, "low", water, 99),
            new Request("r5", "I reported my claim from jo@example.com, how many days do I have?", policy, "low", late, 96));
        List<Trace> traces = new ArrayList<>();
        for (Request request : requests) {
            Handled h = handle(request, request.id().equals("r2") ? STALE_INDEX : INDEX);
            traces.add(h.trace());
            System.out.println(request.id() + ": sent='" + h.sent() + "'; evidence=" + h.trace().chunk() + "; outcome=" + h.trace().outcome());
        }
        System.out.println("traces kept: " + traces.stream().filter(ClaimsAssistant::keep).map(Trace::request).collect(Collectors.joining(", ")));
        List<Case> cases = new ArrayList<>();
        for (int i = 1; i <= 6; i++) cases.add(new Case("s" + i, "status", i > 2, true));
        for (int i = 1; i <= 4; i++) cases.add(new Case("f" + i, "refund", true, i != 4));
        cases.add(new Case("c1", "complaint", false, true));
        cases.add(new Case("c2", "complaint", true, true));
        System.out.println("release with refunds protected: " + release(cases, Set.of("refund")));
        List<Case> fixed = new ArrayList<>();
        for (Case c : cases) fixed.add(c.id().equals("f4") ? new Case(c.id(), c.segment(), c.oldOk(), true) : c);
        System.out.println("release after the refund fix: " + release(fixed, Set.of("refund")));
    }
}
