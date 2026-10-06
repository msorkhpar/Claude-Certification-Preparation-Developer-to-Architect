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

/**
 * TODO 1 of 6 (unlocks e1, e2 and e3): the findings about the name, the description and the limits.
 * Receives the proposal and the policy. Returns a list, in this order, of `bad_name` (the name does not match `[a-z][a-z0-9_]{2,63}`),
 * `short_description` (fewer than `minWords` words), `timeout` (`timeoutS` above `maxTimeout`) and `memory` (`memoryMb` above `maxMemory`).
 * Example: a 3-word description with a policy minimum of 12 -> ["short_description"]
 */
fun findingsOf(proposal: Proposal, policy: Policy): List<String> = emptyList()

/**
 * TODO 2 of 6 (unlocks e4): the forbidden tokens in the code.
 * Receives the code text. Returns the tokens of `FORBIDDEN` that it contains, in alphabetical order.
 * Example: "eval(text)\nos.system('ls')" -> ["eval(", "os.system"]
 */
fun forbiddenCalls(code: String): List<String> = emptyList()

/**
 * TODO 3 of 6 (unlocks e5, e6 and e8): the permissions the code shows.
 * Receives the code text. Returns the permissions of `MARKERS` that have a marker in the code, in alphabetical order (the table is not in that order).
 * Example: "shutil.copy(a, b)\nopen(a).read()" -> ["read_files", "write_files"]
 */
fun permissionsUsed(code: String): List<String> = emptyList()

/**
 * TODO 4 of 6 (unlocks e4 and e5): the refusals, in order.
 * Receives the forbidden tokens, the permissions used, the permissions declared and the permissions the policy denies. Returns
 * `forbidden:<token>` for each forbidden token, then `undeclared:<permission>` for each used permission that was not declared, then
 * `denied:<permission>` for each declared permission that is denied (alphabetical within each group).
 * Example: refusalsOf(emptyList(), listOf("network"), listOf("read_files"), listOf("network")) -> ["undeclared:network"]
 */
fun refusalsOf(forbidden: List<String>, used: List<String>, declared: List<String>, denied: List<String>): List<String> = emptyList()

/**
 * TODO 5 of 6 (unlocks e6): does a declared permission need approval?
 * Receives the declared permissions and the permissions that need approval. Returns true when any declared permission is among them.
 * Example: isGated(listOf("read_files", "write_files"), listOf("write_files")) -> true
 */
fun isGated(declared: List<String>, approval: List<String>): Boolean = false

/**
 * TODO 6 of 6 (unlocks m1, e6 and e7): the decision.
 * Receives the refusals, the findings and `gated`. Returns `refuse` when there are refusals, otherwise `revise` when there are findings,
 * otherwise `approve_with_gate` when gated, otherwise `approve`.
 * Example: decide(emptyList(), listOf("timeout"), true) -> "revise"
 */
fun decide(refusals: List<String>, findings: List<String>, gated: Boolean): String = ""

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
