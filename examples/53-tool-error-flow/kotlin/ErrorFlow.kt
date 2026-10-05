import harness.Show.py

private val log = System.getLogger("error_flow")

/**
 * What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.
 *
 * The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
 * answer. The refund service, the orders and the limits are illustrative.
 */

/** A failure a tool reports about itself: its kind, a message the model can use, an optional wait the service asked for and an explanation for the customer. */
class ToolError(val kind: String, message: String, val retryAfterMs: Int? = null, val explanation: String? = null) : RuntimeException(message)

val RETRYABLE = mapOf("transient" to "yes", "validation" to "no", "permission" to "no", "business" to "no", "outcome_unknown" to "no", "internal" to "no")
val ACTION = mapOf(
    "transient" to "retry later", "validation" to "repair the input", "permission" to "escalate", "business" to "explain to the customer",
    "outcome_unknown" to "check the state first", "internal" to "escalate",
)

/** What the loop hands the model: an error flag, the kind of failure, the text, the attempts made, and whether a success was empty. */
data class Result(val isError: Boolean, val kind: String?, val content: Any?, val attempts: Int, val empty: Boolean)

/** What a run may be told: an idempotency key, and whether the call only reads. */
data class Options(val key: String? = null, val readOnly: Boolean = false)

fun failure(kind: String, message: String, attempts: Int, explanation: String? = null): Result {
    var text = "$kind error (retryable: ${RETRYABLE[kind]}): $message"
    if (!explanation.isNullOrEmpty()) text += " Tell the customer: $explanation"
    return Result(true, kind, text, attempts, false)
}

/** Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits. */
fun run(tool: (Map<String, Any?>) -> Any?, args: Map<String, Any?>, options: Options = Options(), maxRetries: Int = 2, baseMs: Int = 100): Pair<Result, List<Int>> {
    val waits = mutableListOf<Int>()
    var attempts = 0
    while (true) {
        attempts++
        val call = if (options.key != null) args + ("idempotency_key" to options.key) else args
        try {
            val value = tool(call)
            return Result(false, null, value, attempts, value == null || value == "" || (value is List<*> && value.isEmpty())) to waits
        } catch (error: ToolError) {
            var kind = error.kind
            if (kind == "timeout") {
                if (options.key == null && !options.readOnly) {
                    return failure("outcome_unknown", "${error.message} The call may have taken effect: check the current state before trying again.", attempts) to waits
                }
                kind = "transient"
            }
            if (kind != "transient") return failure(kind, error.message!!, attempts, error.explanation) to waits
            if (attempts > maxRetries) return failure("transient", "${error.message} Gave up after $attempts attempts.", attempts) to waits
            waits += error.retryAfterMs ?: (baseMs * (1 shl (attempts - 1)))
        } catch (error: Exception) {
            return failure("internal", "unexpected failure in the tool: ${error.message}", attempts) to waits
        }
    }
}

fun nextStep(result: Result): String = if (!result.isError) (if (result.empty) "accept the empty result" else "continue") else ACTION.getValue(result.kind!!)

/** A tool that does what the script says, one entry per call; the last entry repeats. */
class Scripted(private vararg val steps: Any?) : (Map<String, Any?>) -> Any? {
    var n = 0
    val seen = mutableListOf<String?>()

    override fun invoke(args: Map<String, Any?>): Any? {
        seen += args["idempotency_key"] as String?
        val step = steps[minOf(n, steps.size - 1)]
        n++
        if (step is Exception) throw step
        return step
    }
}

data class Scenario(val title: String, val tool: Scripted, val options: Options = Options())

fun scenarios() = listOf(
    Scenario("get_order order=A-7", Scripted(ToolError("transient", "The order service is busy."), ToolError("transient", "The order service is busy."), "order A-7: 2 items")),
    Scenario("process_refund amount=-5", Scripted(ToolError("validation", "amount must be a positive whole number, for example 40"))),
    Scenario("process_refund amount=900", Scripted(ToolError("business", "Refunds above 500 need a person.", explanation = "A colleague will contact you about this refund."))),
    Scenario("process_refund amount=40, no key", Scripted(ToolError("timeout", "No answer from the refund service."))),
    Scenario("process_refund amount=40, key refund-A-7-1", Scripted(ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), Options(key = "refund-A-7-1")),
    Scenario("list_orders customer=C-9", Scripted(emptyList<String>()), Options(readOnly = true)),
    Scenario("process_refund amount=40, tool bug", Scripted(RuntimeException("the currency table is missing"))),
)

fun main() {
    for ((index, s) in scenarios().withIndex()) {
        val (result, waits) = run(s.tool, mapOf("order" to "A-7"), s.options)
        println("${index + 1}. ${s.title}")
        println("   attempts ${result.attempts}, waits ${py(waits)}, keys sent ${py(s.tool.seen)}")
        println("   tool_result is_error=${if (result.isError) "true" else "false"}: ${py(result.content)}")
        println("   next: ${nextStep(result)}")
    }
}
