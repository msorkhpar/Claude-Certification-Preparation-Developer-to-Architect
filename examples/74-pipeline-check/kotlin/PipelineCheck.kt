import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.
 *
 * The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
 * course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
 * without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
 * better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
 * Claude.
 */
val HERE: Path = Path.of("..").toAbsolutePath().normalize()
private val WRITERS = listOf("Bash", "Edit", "Write")
private val JSON = ObjectMapper()

fun load(root: Path): JsonNode = JSON.readTree(Files.readString(root.resolve("ci/pipeline.json"))).get("jobs")

/** Split a command line the way a shell does for the quoting this pipeline uses: spaces separate words, double quotes group them and a backslash escapes a quote. */
fun words(command: String): List<String> {
    val out = mutableListOf<String>()
    val word = StringBuilder()
    var quoted = false
    var started = false
    var i = 0
    while (i < command.length) {
        val c = command[i]
        when {
            c == '\\' && i + 1 < command.length -> { i++; word.append(command[i]); started = true }
            c == '"' -> { quoted = !quoted; started = true }
            c == ' ' && !quoted -> { if (started) out += word.toString(); word.setLength(0); started = false }
            else -> { word.append(c); started = true }
        }
        i++
    }
    if (started) out += word.toString()
    return out
}

private fun list(node: JsonNode): List<String> = node.map { it.asText() }

fun audit(root: Path): List<String> {
    val found = mutableListOf<String>()
    for (job in load(root)) {
        val name = job.get("name").asText()
        val kind = job.get("kind").asText()
        val tokens = words(job.get("command").asText())
        val audience = job.get("audience").asText()
        val api = job.get("api").asText()
        if (tokens[0] == "claude" && "-p" !in tokens && "--print" !in tokens) found += "no-print-flag: $name"
        if (audience == "waiting" && api == "batch") found += "blocking-batch: $name"
        if (audience == "scheduled" && api == "realtime") found += "batchable: $name"
        if (kind == "review" && list(job.get("passes")).joinToString(",") != "per-file,integration") found += "single-pass-review: $name"
        if (kind == "review" && job.get("session").asText() != "fresh") found += "shared-session: $name"
        if (kind == "review" && "prior_findings" !in list(job.get("context"))) found += "no-prior-findings: $name"
        if (kind == "testgen" && "existing_tests" !in list(job.get("context"))) found += "no-existing-tests: $name"
        if (kind == "review" && list(job.get("tools")).any { it in WRITERS }) found += "writes: $name"
    }
    return found
}

fun main() {
    for (name in listOf("project-before", "project-after")) {
        val jobs = load(HERE.resolve(name))
        val batch = jobs.count { it.get("api").asText() == "batch" }
        println("$name: ${jobs.size()} jobs (${jobs.size() - batch} real-time, $batch batch)")
        val found = audit(HERE.resolve(name))
        for (finding in found) println("  finding: $finding")
        if (found.isEmpty()) {
            println("  no findings")
            for (j in jobs) println("  ${j.get("name").asText()}: ${j.get("api").asText()}, passes ${list(j.get("passes")).joinToString("+").ifEmpty { "none" }}")
        }
    }
}
