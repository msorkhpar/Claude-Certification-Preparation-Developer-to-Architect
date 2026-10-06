import static harness.Show.py;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.
 *
 * <p>The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
 * nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
 * `auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
 */
public final class ExtractionChecks {
    private static final System.Logger LOG = System.getLogger(ExtractionChecks.class.getName());
    static final String DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR";
    static final Set<String> NO_FORCING = Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    /** What a reply holds: the item amounts, the total and the quotation that shows where the total comes from. */
    record Invoice(List<Double> items, double total, String evidence) {
        Invoice withEvidence(String other) {
            return new Invoice(items, total, other);
        }

        /** The reply as JSON text, in the spacing Python's json.dumps uses, so every language edition prints the same. */
        String toJson() {
            String list = items.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
            return "{\"items\": " + list + ", \"total\": " + total + ", \"evidence\": \"" + evidence.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
        }
    }

    /** The end of an extraction: its status, the attempts made and the feedback messages sent. */
    record Extraction(String status, int attempts, List<String> feedback) {}

    /** One document's outcome: its status and whether the record was right. */
    record Outcome(String status, boolean correct) {}

    /** What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null. */
    static String scriptedValue(String document, boolean nullable) {
        String marker = "PO ";
        if (document.contains(marker)) return document.split(Pattern.quote(marker), -1)[1].strip().split("\\s+")[0];
        return nullable ? null : "PO-0000";
    }

    /** Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document. */
    static List<String> check(Invoice record, String document) {
        LOG.log(System.Logger.Level.DEBUG, "check input: {0}", record);
        List<String> problems = new ArrayList<>();
        double sum = 0;
        for (double item : record.items()) sum += item;
        if (Math.abs(sum - record.total()) > 0.005) problems.add("total: the items add up to " + sum + ", not " + record.total());
        if (!document.contains(record.evidence())) problems.add("evidence: this quotation is not in the document");
        return problems;
    }

    /** Ask, check, and ask again with the document, the failed answer and the problems; give up after maxRetries. */
    static Extraction extract(String document, List<Invoice> replies, int maxRetries) {
        List<String> messages = new ArrayList<>();
        int attempt = 0;
        for (Invoice reply : replies.subList(0, Math.min(replies.size(), maxRetries + 1))) {
            attempt++;
            List<String> problems = check(reply, document);
            if (problems.isEmpty()) return new Extraction("valid", attempt, messages);
            messages.add("Document:\n" + document + "\nYour answer:\n" + reply.toJson() + "\nProblems:\n" + problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n")));
        }
        return new Extraction("failed", Math.min(replies.size(), maxRetries + 1), messages);
    }

    static Extraction extract(String document, List<Invoice> replies) {
        return extract(document, replies, 1);
    }

    /** outcomes: (status, correct) per document. The figure on validated records alone leaves out every document that failed. */
    static Map<String, Double> accuracy(List<Outcome> outcomes) {
        List<Boolean> valid = outcomes.stream().filter(o -> o.status().equals("valid")).map(Outcome::correct).toList();
        long right = valid.stream().filter(c -> c).count();
        Map<String, Double> out = new LinkedHashMap<>();
        out.put("validated_only", valid.isEmpty() ? 0.0 : Math.round((double) right / valid.size() * 100) / 100.0);
        out.put("all_documents", outcomes.isEmpty() ? 0.0 : Math.round((double) right / outcomes.size() * 100) / 100.0);
        return out;
    }

    /** The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected. */
    static Map<String, Object> requestChoice(String model, List<String> tools) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (NO_FORCING.contains(model)) {
            out.put("tool_choice", "auto");
            out.put("check_reply", true);
        } else {
            out.put("tool_choice", tools.size() > 1 ? "any" : "tool:" + tools.get(0));
            out.put("check_reply", false);
        }
        return out;
    }

    static List<Outcome> repeat(String status, boolean correct, int times) {
        List<Outcome> out = new ArrayList<>();
        for (int i = 0; i < times; i++) out.add(new Outcome(status, correct));
        return out;
    }

    public static void main(String[] args) {
        String noPo = "Invoice from Acme Tools. Total due: 130.00 EUR";
        String required = scriptedValue(noPo, false), nullable = scriptedValue(noPo, true);
        System.out.println("purchase order, document without one: required -> " + required + " | nullable -> " + (nullable == null ? "None" : nullable));
        Invoice wrong = new Invoice(List.of(100.0, 20.5), 130.0, "Total due: 130.00 EUR");
        Invoice right = new Invoice(List.of(100.0, 20.5, 9.5), 130.0, "Total due: 130.00 EUR");
        Invoice fabricated = right.withEvidence("Total due: 130.00 USD");
        Extraction result = extract(DOC, List.of(wrong, right));
        System.out.println("answer 1 wrong, answer 2 right: " + result.status() + " after " + result.attempts() + " attempts");
        System.out.println(result.feedback().get(0));
        System.out.println("two answers that stay wrong: " + extract(DOC, List.of(wrong, fabricated)).status());
        List<Outcome> outcomes = new ArrayList<>(repeat("valid", true, 5));
        outcomes.addAll(repeat("valid", false, 1));
        outcomes.addAll(repeat("failed", false, 4));
        System.out.println("accuracy of 10 documents (6 valid, 5 of them right): " + py(accuracy(outcomes)));
        for (String model : List.of("claude-haiku-4-5", "claude-sonnet-5-5")) {
            System.out.println(model + ", two extraction tools: " + py(requestChoice(model, List.of("extract_invoice", "extract_receipt"))));
        }
    }
}
