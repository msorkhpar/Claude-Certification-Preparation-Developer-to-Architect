import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
 * are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.
 *
 * <p>The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
 * (a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
 */
public final class ExtractionRun {
    record Doc(String kind, String text) {}

    record Rec(String vendor, List<Integer> lines, Integer total, boolean conflict) {}

    record Err(String kind, String field) {}

    record Extracted(String id, int attempts, Rec record, List<Err> errors, List<String> retried, String status) {}

    static final Map<String, Doc> DOCS = new LinkedHashMap<>();
    static final Map<String, Rec> LABELS = new LinkedHashMap<>();
    static final Map<String, List<Rec>> REPLIES = new LinkedHashMap<>();

    static Rec rec(String vendor, List<Integer> lines, Integer total) {
        return new Rec(vendor, lines, total, false);
    }

    static {
        DOCS.put("d1", new Doc("typed", "Vendor: Acme Ltd. Lines: 10.00 20.00. Total: 30.00"));
        DOCS.put("d2", new Doc("typed", "Vendor: Borealis Co. Lines: 40.00 5.00. Total: 45.00"));
        DOCS.put("d3", new Doc("scanned", "Lines: 8.00 2.00. Total: 10.00"));
        DOCS.put("d4", new Doc("scanned", "Vendor: Corvid Inc. Lines: 12.00 8.00."));
        DOCS.put("d5", new Doc("handwritten", "Vendor: Dunmore. Lines: 6.00 6.00. Total: 12.50"));
        DOCS.put("d6", new Doc("handwritten", "Lines: 3.00. Total: 3.00"));
        LABELS.put("d1", rec("Acme Ltd", List.of(), 3000));
        LABELS.put("d2", rec("Borealis Co", List.of(), 4500));
        LABELS.put("d3", rec(null, List.of(), 1000));
        LABELS.put("d4", rec("Corvid Inc", List.of(), 2000));
        LABELS.put("d5", rec("Dunmore", List.of(), 1200));
        LABELS.put("d6", rec(null, List.of(), 300));
        // what the scripted model returns on the first and on the second attempt (a missing second reply repeats the first)
        REPLIES.put("d1", List.of(rec("Acme Ltd", List.of(1000, 2000), 3000)));
        REPLIES.put("d2", List.of(rec("Borealis Co", List.of(4000, 500), 5400), rec("Borealis Co", List.of(4000, 500), 4500)));
        REPLIES.put("d3", List.of(rec("Globex", List.of(800, 200), 1000), rec(null, List.of(800, 200), 1000)));
        REPLIES.put("d4", List.of(rec("Corvid Inc", List.of(1200, 800), null)));
        REPLIES.put("d5", List.of(new Rec("Dunmore", List.of(600, 600), 1250, true)));
        REPLIES.put("d6", List.of(rec("Hollis", List.of(300), 300)));
    }

    /** What a schema cannot check: a vendor the document never names, totals that disagree, a required total that is missing. */
    static List<Err> validate(Rec record, String text) {
        List<Err> errors = new ArrayList<>();
        if (record.vendor() != null && !text.contains(record.vendor())) errors.add(new Err("ungrounded", "vendor"));
        if (record.total() == null) errors.add(new Err("absent", "total"));
        else if (record.total() != record.lines().stream().mapToInt(Integer::intValue).sum() && !record.conflict()) errors.add(new Err("semantic", "total"));
        return errors;
    }

    /** One attempt, then one retry that carries the errors, and only when a second look can fix one. An absent value is never retried. */
    static Extracted extract(String docId, String text) {
        List<String> retried = List.of();
        List<Rec> replies = REPLIES.get(docId);
        Rec record = replies.get(0);
        List<Err> errors = List.of();
        for (int attempt = 1; attempt <= 2; attempt++) {
            record = replies.get(Math.min(attempt, replies.size()) - 1);
            errors = validate(record, text);
            List<Err> fixable = errors.stream().filter(e -> !e.kind().equals("absent")).toList();
            if (fixable.isEmpty()) return new Extracted(docId, attempt, record, errors, retried, !errors.isEmpty() || record.conflict() ? "needs_review" : "valid");
            retried = new TreeSet<>(fixable.stream().map(Err::kind).toList()).stream().toList();
        }
        return new Extracted(docId, 2, record, errors, retried, "failed");
    }

    static int percent(int correct, int total) {
        return total > 0 ? (200 * correct + total) / (2 * total) : 0;
    }

    public static void main(String[] args) {
        List<Extracted> results = new ArrayList<>();
        List<String> kinds = new ArrayList<>();
        List<Boolean> correct = new ArrayList<>();
        for (var entry : DOCS.entrySet()) {
            Extracted r = extract(entry.getKey(), entry.getValue().text());
            Rec label = LABELS.get(entry.getKey());
            results.add(r);
            kinds.add(entry.getValue().kind());
            correct.add(r.status().equals("valid") && java.util.Objects.equals(r.record().vendor(), label.vendor()) && r.record().total().equals(label.total()));
            String note = r.status().equals("valid") && !r.retried().isEmpty() ? " (retried: " + String.join(", ", r.retried()) + ")" : "";
            if (r.status().equals("needs_review")) note = r.record().conflict() ? " (conflict flagged)" : " (" + r.errors().get(0).kind() + ": " + r.errors().get(0).field() + ", not retried)";
            if (r.status().equals("failed")) note = " (" + String.join(", ", r.retried()) + ")";
            System.out.println(r.id() + " " + entry.getValue().kind() + ": " + r.status() + " after " + r.attempts() + " attempt" + (r.attempts() > 1 ? "s" : "") + note);
        }
        int valid = 0, rightValid = 0, right = 0;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).status().equals("valid")) {
                valid++;
                if (correct.get(i)) rightValid++;
            }
            if (correct.get(i)) right++;
        }
        System.out.println("accuracy: validated only " + rightValid + " of " + valid + " (" + percent(rightValid, valid) + "%), all documents " + right + " of " + results.size() + " (" + percent(right, results.size()) + "%)");
        List<String> order = new ArrayList<>(new LinkedHashSet<>(kinds));
        List<String> parts = new ArrayList<>();
        List<String> ready = new ArrayList<>();
        for (String k : order) {
            int n = 0, ok = 0;
            for (int i = 0; i < kinds.size(); i++) {
                if (kinds.get(i).equals(k)) {
                    n++;
                    if (correct.get(i)) ok++;
                }
            }
            parts.add(k + " " + ok + "/" + n);
            if (n >= 2 && ok == n) ready.add(k);
        }
        System.out.println("by kind: " + String.join(", ", parts));
        System.out.println("automate: " + (ready.isEmpty() ? "none" : ready.stream().collect(Collectors.joining(", "))));
    }
}
