// Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision.
// Read statement.md for the fields of a proposal, of the policy and of the report, then replace the body of review().
data class Proposal(val name: String, val description: String, val permissions: List<String>, val timeoutS: Int, val memoryMb: Int, val code: String)

data class Policy(val minWords: Int, val maxTimeout: Int, val maxMemory: Int, val denied: List<String>, val approval: List<String>)

data class Report(val name: String, val decision: String, val refusals: List<String>, val findings: List<String>, val used: List<String>, val audit: String)

fun review(proposal: Proposal, policy: Policy): Report = Report("", "", listOf(), listOf(), listOf(), "")
