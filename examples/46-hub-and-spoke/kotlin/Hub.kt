import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.Tool
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import harness.Show.py

/**
 * Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.
 *
 * The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
 * request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
 */
const val MODEL = "claude-sonnet-5-5"
val PLAN_TOOL: Tool = Tool.builder().name("plan")
    .description("Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.")
    .inputSchema(
        Tool.InputSchema.builder()
            .properties(
                JsonValue.from(
                    map("subtasks", map("type", "array", "items", map("type", "object", "properties", map("scope", map("type", "string"), "brief", map("type", "string")), "required", listOf("scope", "brief")))),
                ),
            )
            .required(listOf("subtasks")).build(),
    ).build()
const val SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note."

data class Subtask(val scope: String, val brief: String)

val SUBTASKS = listOf(
    Subtask("chips", "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates."),
    Subtask("cars", "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates."),
    Subtask("rates", "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars."),
)
val REPORTS = listOf(
    "CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
    "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
    "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements.",
)

fun textOf(reply: Message): String = reply.content().filter { it.isText() }.joinToString("") { it.asText().text() }

fun plan(client: AnthropicClient, question: String): List<Subtask> {
    // tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
    val reply = client.messages().create(
        MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).addTool(PLAN_TOOL).addUserMessage("$question\nRecord your plan by calling the plan tool.").build(),
    )
    val use = reply.content().first { it.isToolUse() }.asToolUse()
    val input = jsonMapper().convertValue(use._input(), Map::class.java)
    return (input["subtasks"] as List<*>).map { s -> (s as Map<*, *>).let { Subtask(it["scope"] as String, it["brief"] as String) } }
}

/** A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed. */
fun runSubagent(client: AnthropicClient, brief: String): String =
    textOf(client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800).system(SUBAGENT_SYSTEM).addUserMessage(brief).build()))

fun synthesize(client: AnthropicClient, question: String, findings: List<Pair<String, String>>): String {
    val listing = findings.joinToString("\n") { (scope, report) -> "[$scope] $report" }
    return textOf(
        client.messages().create(
            MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(800)
                .addUserMessage("Question: $question\nFindings:\n$listing\nWrite one answer that uses every finding.").build(),
        ),
    )
}

fun tag(report: String): String = report.substringBefore(":")

fun main() {
    val question = "How did the 2024 supply picture change for chips, cars and interest rates?"
    val replies = mutableListOf<Any>(
        message(listOf(text("Three independent parts."), toolUse("toolu_01", "plan", map("subtasks", SUBTASKS.map { map("scope", it.scope, "brief", it.brief) }))), "tool_use"),
    )
    REPORTS.forEach { replies += message(listOf(text(it))) }
    replies += message(listOf(text("Chip supply recovered first, car output followed, and rates stayed high.")))
    val rig = Scripted.client(*replies.toTypedArray())
    val subtasks = plan(rig.client(), question)
    println("plan: ${subtasks.size} subtasks -> ${py(subtasks.map { it.scope })}")
    val findings = mutableListOf<Pair<String, String>>()
    subtasks.forEachIndexed { index, task ->
        val number = index + 1
        val report = runSubagent(rig.client(), task.brief)
        findings += task.scope to report
        val request = rig.http().requests[number]
        val body = request.toString()
        val others = REPORTS.any { it != report && tag(it) in body }
        println(
            "subagent $number request: ${request["messages"].size()} message, system prompt of the role: ${py(request["system"].asText() == SUBAGENT_SYSTEM)}, " +
                "holds the coordinator's question: ${py(question in body)}, holds another report: ${py(others)}",
        )
    }
    val answer = synthesize(rig.client(), question, findings)
    val synthesis = rig.http().requests[4].toString()
    println("synthesis request: reports included: ${REPORTS.count { tag(it) in synthesis }} of ${REPORTS.size}")
    println("model calls: ${rig.http().requests.size} | one plan, three subagents, one synthesis")
    println("answer: $answer")
}
