import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
public final class ReviewGate {
    private ReviewGate() {}

    static final List<String> SEVERITIES = List.of("low", "medium", "high");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The prompt of one review run. prior: maps with file, line, category and issue; existingTests: test names. */
    public static String reviewPrompt(String diff, List<Map<String, Object>> prior, List<String> existingTests) {
        List<String> lines = new ArrayList<>(List.of("<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed."));
        if (!prior.isEmpty()) lines.add("Do not repeat a finding listed in <already_reported>.");
        if (!existingTests.isEmpty()) lines.add("Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers.");
        lines.add("</instructions>");
        if (!prior.isEmpty()) {
            lines.add("<already_reported>");
            for (Map<String, Object> p : prior) lines.add("- " + p.get("file") + ":" + p.get("line") + " [" + p.get("category") + "] " + p.get("issue"));
            lines.add("</already_reported>");
        }
        if (!existingTests.isEmpty()) {
            lines.add("<existing_tests>");
            for (String t : existingTests) lines.add("- " + t);
            lines.add("</existing_tests>");
        }
        lines.addAll(List.of("<diff>", diff, "</diff>"));
        return String.join("\n", lines);
    }

    private static Map<String, Object> decision(int exit, List<Map<String, Object>> comments, List<String> problems) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exit", exit);
        out.put("comments", comments);
        out.put("problems", problems);
        return out;
    }

    /** policy: min_severity, disabled_categories and fail_on. Returns exit, comments and problems. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> gate(String stdout, int exitCode, JsonNode schema, Map<String, Object> policy) {
        List<String> problems = new ArrayList<>();
        if (exitCode != 0) problems.add("claude exited with status " + exitCode);
        JsonNode envelope;
        try {
            envelope = JSON.readTree(stdout);
        } catch (Exception e) {
            envelope = null;
        }
        if (envelope == null || !envelope.isObject()) {
            problems.add("the output is not a JSON object");
            return decision(1, List.of(), problems);
        }
        JsonNode subtype = envelope.get("subtype");
        if (envelope.path("is_error").asBoolean(false) || subtype == null || !subtype.isTextual() || !subtype.asText().equals("success")) {
            problems.add("the run ended with " + (subtype == null || subtype.isNull() ? "None" : subtype.asText()));
        }
        JsonNode output = envelope.get("structured_output");
        if (output == null || output.isNull()) problems.add("the result has no structured_output");
        else for (String e : SchemaCheck.schemaCheck(output, schema)) problems.add("schema " + e);
        if (!problems.isEmpty()) return decision(1, List.of(), problems);
        int floor = SEVERITIES.indexOf((String) policy.get("min_severity"));
        List<String> disabled = (List<String>) policy.get("disabled_categories");
        List<String> failOn = (List<String>) policy.get("fail_on");
        List<Map<String, Object>> comments = new ArrayList<>();
        for (JsonNode f : output.get("findings")) {
            if (disabled.contains(f.get("category").asText()) || SEVERITIES.indexOf(f.get("severity").asText()) < floor) continue;
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("file", f.get("file").asText());
            c.put("line", f.get("line").asInt());
            c.put("severity", f.get("severity").asText());
            c.put("body", f.get("issue").asText() + " Suggested fix: " + f.get("suggested_fix").asText());
            comments.add(c);
        }
        boolean blocked = comments.stream().anyMatch(c -> failOn.contains((String) c.get("severity")));
        return decision(blocked ? 1 : 0, comments, List.of());
    }
}
