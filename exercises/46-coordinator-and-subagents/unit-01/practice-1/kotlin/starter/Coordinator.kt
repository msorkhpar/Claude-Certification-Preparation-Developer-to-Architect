/** A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md. */

typealias Planner = (String) -> Map<String, Any?>
typealias Spoke = (String) -> String?
typealias Reviewer = (String, List<Map<String, Any?>>) -> List<String>
typealias Synthesizer = (String, List<Map<String, Any?>>) -> String

fun coordinate(planner: Planner, subagent: Spoke, reviewer: Reviewer, synthesizer: Synthesizer, question: String, maxAgents: Int = 4, maxRounds: Int = 2): Map<String, Any?>? {
    // TODO: plan, delegate one brief per subagent, review the findings for gaps, delegate the gaps, then synthesize once.
    return null
}
