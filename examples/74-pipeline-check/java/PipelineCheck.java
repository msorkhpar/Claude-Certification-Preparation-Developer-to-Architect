import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.
 *
 * <p>The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
 * course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
 * without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
 * better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
 * Claude.
 */
public final class PipelineCheck {
    static final Path HERE = Path.of("..").toAbsolutePath().normalize();
    private static final List<String> WRITERS = List.of("Bash", "Edit", "Write");
    private static final ObjectMapper JSON = new ObjectMapper();

    static JsonNode load(Path root) {
        try {
            return JSON.readTree(Files.readString(root.resolve("ci/pipeline.json"))).get("jobs");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Split a command line the way a shell does for the quoting this pipeline uses: spaces separate words, double quotes group them and a backslash escapes a quote. */
    static List<String> words(String command) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        boolean quoted = false, started = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '\\' && i + 1 < command.length()) { word.append(command.charAt(++i)); started = true; }
            else if (c == '"') { quoted = !quoted; started = true; }
            else if (c == ' ' && !quoted) { if (started) out.add(word.toString()); word.setLength(0); started = false; }
            else { word.append(c); started = true; }
        }
        if (started) out.add(word.toString());
        return out;
    }

    private static List<String> list(JsonNode node) {
        List<String> out = new ArrayList<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    static List<String> audit(Path root) {
        List<String> found = new ArrayList<>();
        for (JsonNode job : load(root)) {
            String name = job.get("name").asText(), kind = job.get("kind").asText();
            List<String> tokens = words(job.get("command").asText());
            String audience = job.get("audience").asText(), api = job.get("api").asText();
            if (tokens.get(0).equals("claude") && !tokens.contains("-p") && !tokens.contains("--print")) found.add("no-print-flag: " + name);
            if (audience.equals("waiting") && api.equals("batch")) found.add("blocking-batch: " + name);
            if (audience.equals("scheduled") && api.equals("realtime")) found.add("batchable: " + name);
            if (kind.equals("review") && !String.join(",", list(job.get("passes"))).equals("per-file,integration")) found.add("single-pass-review: " + name);
            if (kind.equals("review") && !job.get("session").asText().equals("fresh")) found.add("shared-session: " + name);
            if (kind.equals("review") && !list(job.get("context")).contains("prior_findings")) found.add("no-prior-findings: " + name);
            if (kind.equals("testgen") && !list(job.get("context")).contains("existing_tests")) found.add("no-existing-tests: " + name);
            if (kind.equals("review") && list(job.get("tools")).stream().anyMatch(WRITERS::contains)) found.add("writes: " + name);
        }
        return found;
    }

    public static void main(String[] args) {
        for (String name : List.of("project-before", "project-after")) {
            JsonNode jobs = load(HERE.resolve(name));
            int batch = 0;
            for (JsonNode j : jobs) if (j.get("api").asText().equals("batch")) batch++;
            System.out.println(name + ": " + jobs.size() + " jobs (" + (jobs.size() - batch) + " real-time, " + batch + " batch)");
            List<String> found = audit(HERE.resolve(name));
            found.forEach(f -> System.out.println("  finding: " + f));
            if (found.isEmpty()) {
                System.out.println("  no findings");
                for (JsonNode j : jobs) {
                    String passes = String.join("+", list(j.get("passes")));
                    System.out.println("  " + j.get("name").asText() + ": " + j.get("api").asText() + ", passes " + (passes.isEmpty() ? "none" : passes));
                }
            }
        }
    }
}
