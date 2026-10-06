import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

private val log = System.getLogger("workflow_lint")

/**
 * Lint a GitHub Actions workflow that runs Claude Code, against what the Claude Code GitHub Actions documentation recommends.
 *
 * The checks are the ones the documentation states (read on 2026-10-03): keep keys in GitHub Secrets, grant only the permissions
 * the job needs, use the v1 action, cap the work of each run with --max-turns, a timeout and concurrency control, start runners only on
 * comments that mention @claude, and check the repository out before a skill from it can run. Reading the workflow is plain data work:
 * nothing here needs a runner, a key or a network. The workflow is YAML, read with Jackson.
 */
const val CLAUDE_ACTION = "anthropics/claude-code-action"

/** A finding: the job (or "-"), a rule id and a message. */
data class Finding(val job: String, val rule: String, val message: String)

typealias Node = Map<String, Any?>

@Suppress("UNCHECKED_CAST")
fun parseYaml(text: String): Node = ObjectMapper(YAMLFactory()).readValue(text, LinkedHashMap::class.java) as Node

@Suppress("UNCHECKED_CAST")
private fun node(o: Any?): Node = o as? Node ?: emptyMap()

@Suppress("UNCHECKED_CAST")
private fun steps(job: Node): List<Node> = job["steps"] as? List<Node> ?: emptyList()

private fun str(o: Any?) = o?.toString() ?: ""

fun claudeSteps(job: Node): List<Node> = steps(job).filter { str(it["uses"]).startsWith(CLAUDE_ACTION) }

/** Findings in the order they are found. */
fun lint(text: String): List<Finding> {
    val wf = parseYaml(text)
    val findings = mutableListOf<Finding>()
    if (Regex("sk-ant-[A-Za-z0-9_-]{8,}").containsMatchIn(text)) findings += Finding("-", "literal-key", "an API key is written into the file: use \${{ secrets.NAME }}")
    if ("concurrency" !in wf) findings += Finding("-", "no-concurrency", "no concurrency group: parallel runs are not limited")
    val triggers = wf["on"]
    val names: List<Any?> = when (triggers) {
        is Map<*, *> -> triggers.keys.toList()
        is List<*> -> triggers
        else -> listOf(triggers)
    }
    for ((name, jobValue) in node(wf["jobs"])) {
        val job = node(jobValue)
        val claude = claudeSteps(job)
        if (claude.isEmpty()) continue
        for (step in claude) {
            if (!str(step["uses"]).endsWith("@v1")) findings += Finding(name, "action-version", "${step["uses"]} is not the v1 action: move @beta workflows to @v1")
            val with = node(step["with"])
            for (field in listOf("anthropic_api_key", "claude_code_oauth_token")) {
                if (field in with && !str(with[field]).startsWith("\${{ secrets.")) findings += Finding(name, "literal-key", "$field is not read from the secrets context")
            }
            if ("--max-turns" !in str(with["claude_args"])) findings += Finding(name, "no-max-turns", "claude_args has no --max-turns: a run has no turn limit")
            if (str(with["prompt"]).startsWith("/")) {
                val before = steps(job).take(steps(job).indexOf(step))
                if (before.none { str(it["uses"]).startsWith("actions/checkout") }) {
                    findings += Finding(name, "no-checkout", "a skill from the repository needs actions/checkout before the Claude step")
                }
            }
        }
        val perms = job["permissions"]
        if (perms == null) {
            findings += Finding(name, "no-permissions", "no permissions block: grant only what the job needs")
        } else if ("review" in name.lowercase() && node(perms)["contents"] == "write") {
            findings += Finding(name, "review-writes", "a review job has contents: write; reading is enough to review")
        }
        if ("timeout-minutes" !in job) findings += Finding(name, "no-timeout", "no timeout-minutes: a stuck run keeps the runner")
        if ("issue_comment" in names && "@claude" !in str(job["if"])) {
            findings += Finding(name, "unguarded-trigger", "the job has no if: contains(..., '@claude'): a runner starts on every comment")
        }
    }
    return findings
}

val BAD = """name: Claude
on:
  issue_comment:
    types: [created]
jobs:
  review:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: anthropics/claude-code-action@beta
        with:
          anthropic_api_key: sk-ant-api03-EXAMPLEEXAMPLE
          prompt: "/code-review"
"""

val GOOD = """name: Claude review
on:
  pull_request:
    types: [opened, synchronize]
concurrency:
  group: claude-${"$"}{{ github.event.pull_request.number }}
  cancel-in-progress: true
jobs:
  review:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    permissions:
      contents: read
      pull-requests: write
      id-token: write
    steps:
      - uses: actions/checkout@v6
        with:
          fetch-depth: 1
      - uses: anthropics/claude-code-action@v1
        with:
          anthropic_api_key: ${"$"}{{ secrets.ANTHROPIC_API_KEY }}
          prompt: "/code-review"
          claude_args: --max-turns 8
"""

fun main() {
    for ((label, text) in listOf("bad workflow" to BAD, "good workflow" to GOOD)) {
        val found = lint(text)
        println("$label: ${found.size} finding(s)")
        for (f in found) println("  [${f.rule}] ${f.job}: ${f.message}")
    }
}
