data class Proposal(val name: String, val description: String, val permissions: List<String>, val timeoutS: Int, val memoryMb: Int, val code: String)

data class Policy(val minWords: Int, val maxTimeout: Int, val maxMemory: Int, val denied: List<String>, val approval: List<String>)

data class Report(val name: String, val decision: String, val refusals: List<String>, val findings: List<String>, val used: List<String>, val audit: String)

private val FORBIDDEN = listOf("os.system", "subprocess", "eval(", "exec(", "__import__")

// a deliberately crude scan of the code text: which permission each marker shows (the order of this table is not the order of the report)
private val MARKERS = linkedMapOf(
    "network" to listOf("requests.", "urllib"),
    "write_files" to listOf(".write(", "shutil."),
    "read_files" to listOf("open(", ".read("),
    "run_process" to listOf("subprocess"),
)

/** Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision. */
fun review(proposal: Proposal, policy: Policy): Report {
    val name = proposal.name
    val code = proposal.code
    val declared = proposal.permissions
    val findings = mutableListOf<String>()
    if (!Regex("[a-z][a-z0-9_]{2,63}").matches(name)) findings += "bad_name"
    if (proposal.description.trim().split(Regex("\\s+")).size < policy.minWords) findings += "short_description"
    if (proposal.timeoutS > policy.maxTimeout) findings += "timeout"
    if (proposal.memoryMb > policy.maxMemory) findings += "memory"
    val forbidden = FORBIDDEN.filter { it in code }.sorted()
    val used = MARKERS.filter { (_, markers) -> markers.any { it in code } }.keys.sorted()
    val refusals = mutableListOf<String>()
    refusals += forbidden.map { "forbidden:$it" }
    refusals += used.filter { it !in declared }.map { "undeclared:$it" }
    refusals += declared.toSortedSet().filter { it in policy.denied }.map { "denied:$it" }
    val gated = declared.any { it in policy.approval }
    val decision = when {
        refusals.isNotEmpty() -> "refuse"
        findings.isNotEmpty() -> "revise"
        gated -> "approve_with_gate"
        else -> "approve"
    }
    return Report(name, decision, refusals, findings, used, "$name: $decision")
}
