private val log = System.getLogger("tool_review")

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

/** The findings, in order: bad_name, short_description, timeout, memory. */
fun findingsOf(proposal: Proposal, policy: Policy): List<String> {
    val findings = mutableListOf<String>()
    if (!Regex("[a-z][a-z0-9_]{2,63}").matches(proposal.name)) findings += "bad_name"
    if (proposal.description.trim().split(Regex("\\s+")).size < policy.minWords) findings += "short_description"
    if (proposal.timeoutS > policy.maxTimeout) findings += "timeout"
    if (proposal.memoryMb > policy.maxMemory) findings += "memory"
    return findings
}

/** The forbidden tokens the code contains, in alphabetical order. */
fun forbiddenCalls(code: String): List<String> = FORBIDDEN.filter { it in code }.sorted()

/** The permissions the code text shows, in alphabetical order. */
fun permissionsUsed(code: String): List<String> = MARKERS.filter { (_, markers) -> markers.any { it in code } }.keys.sorted()

/** forbidden:<token>, then undeclared:<permission>, then denied:<permission>, each group in alphabetical order. */
fun refusalsOf(forbidden: List<String>, used: List<String>, declared: List<String>, denied: List<String>): List<String> {
    val refusals = mutableListOf<String>()
    refusals += forbidden.map { "forbidden:$it" }
    refusals += used.filter { it !in declared }.map { "undeclared:$it" }
    refusals += declared.toSortedSet().filter { it in denied }.map { "denied:$it" }
    return refusals
}

/** True when any declared permission needs a person's approval. */
fun isGated(declared: List<String>, approval: List<String>): Boolean = declared.any { it in approval }

/** refuse beats revise, revise beats approve_with_gate, otherwise approve. */
fun decide(refusals: List<String>, findings: List<String>, gated: Boolean): String = when {
    refusals.isNotEmpty() -> "refuse"
    findings.isNotEmpty() -> "revise"
    gated -> "approve_with_gate"
    else -> "approve"
}

/** Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision. */
fun review(proposal: Proposal, policy: Policy): Report {
    log.log(System.Logger.Level.DEBUG, "review input: {0}", proposal)
    val declared = proposal.permissions
    val findings = findingsOf(proposal, policy)
    val used = permissionsUsed(proposal.code)
    val refusals = refusalsOf(forbiddenCalls(proposal.code), used, declared, policy.denied)
    val decision = decide(refusals, findings, isGated(declared, policy.approval))
    return Report(proposal.name, decision, refusals, findings, used, "${proposal.name}: $decision")
}
