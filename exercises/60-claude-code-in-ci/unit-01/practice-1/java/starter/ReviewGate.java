import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
public final class ReviewGate {
    private static final System.Logger LOG = System.getLogger(ReviewGate.class.getName());
    private ReviewGate() {}

    static final List<String> SEVERITIES = List.of("low", "medium", "high");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The prompt of one review run. prior: maps with file, line, category and issue; existingTests: test names. */
    public static String reviewPrompt(String diff, List<Map<String, Object>> prior, List<String> existingTests) {
        // TODO 1 of 9 (finish this to pass e4): the first instructions of the prompt. Add the sentence "Report only
        //   findings that are new or still unaddressed." as the third line of the instructions, after the line that points
        //   to the project criteria. Example: the prompt starts with <instructions>, the review line, then this sentence.
        List<String> lines = new ArrayList<>(List.of("<instructions>", "Review the change in <diff> against the criteria in the project instructions."));
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
        LOG.log(System.Logger.Level.DEBUG, "gate input: {0}", stdout);
        List<String> problems = new ArrayList<>();
        // TODO 2 of 9 (finish this to pass e1): the exit status. When claude exited with a status other than 0, add the
        //   problem "claude exited with status N" (the job fails whatever the output looks like). Example: exit 2 and a
        //   perfect answer -> exit 1 and that problem.
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
        // TODO 3 of 9 (finish this to pass e2): the schema check. Check the structured output against the schema with
        //   the provided helper and add one problem per error, as "schema " followed by the error (which names the path).
        //   Example: severity "critical" -> "schema $.findings[0].severity: ...".
        if (!problems.isEmpty()) return decision(1, List.of(), problems);
        int floor = SEVERITIES.indexOf((String) policy.get("min_severity"));
        List<String> disabled = (List<String>) policy.get("disabled_categories");
        List<String> failOn = (List<String>) policy.get("fail_on");
        List<Map<String, Object>> comments = new ArrayList<>();
        // TODO 4 of 9 (finish this to pass m1): the comments of a valid answer. Keep each finding whose severity is at
        //   or above policy min_severity (the order is low, medium, high) and whose category is not in
        //   disabled_categories. Return one comment {file, line, severity, body} per kept finding, in order, with the body
        //   "ISSUE Suggested fix: FIX". Example: a medium bug at api.py:12 with floor medium -> one comment.
        // TODO 5 of 9 (finish this to pass e3): the decision. The job fails (exit 1) when at least one posted comment
        //   has a severity listed in policy fail_on; otherwise it only comments (exit 0). Example: fail_on [high], one
        //   posted medium comment -> exit 0.
        boolean blocked = false;
        return decision(blocked ? 1 : 0, comments, List.of());
    }
}
