import com.anthropic.client.AnthropicClient
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.batches.BatchCreateParams
import com.anthropic.models.messages.batches.MessageBatch
import com.anthropic.models.messages.batches.MessageBatchResult
import harness.Reply
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Show.py

/**
 * A Message Batch from submission to results, against a scripted server.
 *
 * The replies are illustrative, hand-written bodies in the shapes of the batch processing page (claude-haiku-4-5), not
 * captures. The results arrive out of order, as the page warns they may, and one request of each non-success kind is in
 * them. Waiting between polls is recorded, not slept. The Java SDK (used from Kotlin) asks for `/results` directly, so the
 * script has no batch look-up before the results (the Python SDK makes one).
 */
const val MODEL = "claude-haiku-4-5-20251001"
val TICKETS = linkedMapOf("t-1" to "My parcel never arrived.", "t-2" to "How do I change my address?", "t-3" to "Charge me twice? Refund please.", "t-4" to "x".repeat(10))

fun batch(status: String, counts: Map<String, Any>, resultsUrl: String? = null) = map(
    "id", "msgbatch_illustrative", "type", "message_batch", "processing_status", status, "request_counts", counts,
    "ended_at", if (status == "ended") "2026-10-02T10:40:00Z" else null, "created_at", "2026-10-02T10:00:00Z",
    "expires_at", "2026-10-03T10:00:00Z", "cancel_initiated_at", null, "results_url", resultsUrl,
)

fun counts(processing: Int = 0, succeeded: Int = 0, errored: Int = 0, canceled: Int = 0, expired: Int = 0) =
    map("processing", processing, "succeeded", succeeded, "errored", errored, "canceled", canceled, "expired", expired)

fun resultLine(customId: String, result: Map<String, Any?>): String = jsonMapper().writeValueAsString(map("custom_id", customId, "result", result))

fun succeeded(label: String) = map("type", "succeeded", "message", message(listOf(text(label)), "end_turn", MODEL, map("input_tokens", 30, "output_tokens", 3), null))

val RESULTS = listOf(
    resultLine("t-3", succeeded("billing")),
    resultLine("t-1", succeeded("shipping")),
    resultLine("t-4", map("type", "errored", "error", map("type", "error", "error", map("type", "invalid_request_error", "message", "messages: at least one message is required")))),
    resultLine("t-2", map("type", "expired")),
).joinToString("\n") + "\n"

const val RESULTS_URL = "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results"

fun script(): List<Any> = listOf(
    Reply.json(200, batch("in_progress", counts(processing = 4))),
    Reply.json(200, batch("in_progress", counts(processing = 2, succeeded = 2))),
    Reply.json(200, batch("ended", counts(succeeded = 2, errored = 1, expired = 1), RESULTS_URL)),
    Reply.text(200, "application/x-jsonl", RESULTS),
)

fun requests(): BatchCreateParams {
    val builder = BatchCreateParams.builder()
    TICKETS.forEach { (cid, body) ->
        builder.addRequest(
            BatchCreateParams.Request.builder().customId(cid)
                .params(BatchCreateParams.Request.Params.builder().model(MODEL).maxTokens(50).addUserMessage("Label this ticket: $body").build()).build(),
        )
    }
    return builder.build()
}

/** What came back for one request: its kind (succeeded, errored, expired, canceled) and a detail. */
data class Outcome(val kind: String, val detail: String)

fun outcomeOf(result: MessageBatchResult): Outcome = when {
    result.succeeded().isPresent -> Outcome("succeeded", result.succeeded().get().message().content()[0].asText().text())
    result.errored().isPresent -> Outcome("errored", jsonMapper().valueToTree<com.fasterxml.jackson.databind.JsonNode>(result.errored().get().error()).at("/error/type").asText())
    result.canceled().isPresent -> Outcome("canceled", "")
    else -> Outcome("expired", "")
}

/** The results in the order they arrived. */
fun results(client: AnthropicClient, batchId: String): Map<String, Outcome> {
    val outcomes = linkedMapOf<String, Outcome>()
    client.messages().batches().resultsStreaming(batchId).use { stream -> stream.stream().forEach { outcomes[it.customId()] = outcomeOf(it.result()) } }
    return outcomes
}

private fun counts(status: MessageBatch): String = status.requestCounts().let {
    "{'canceled': ${it.canceled()}, 'errored': ${it.errored()}, 'expired': ${it.expired()}, 'processing': ${it.processing()}, 'succeeded': ${it.succeeded()}}"
}

fun main() {
    val transport = Scripted.http(*script().toTypedArray())
    val client = Scripted.clientOn(transport, 0)
    val created = client.messages().batches().create(requests())
    println("created: ${created.id()} ${created.processingStatus().asString()} | request ids sent: ${py(transport.requests[0]["requests"].map { it["custom_id"].asText() })}")
    val waits = mutableListOf<Int>()
    var status = created
    while (status.processingStatus().asString() != "ended") {
        status = client.messages().batches().retrieve(created.id())
        waits += 60
        println("poll: ${status.processingStatus().asString()} ${counts(status)}")
    }
    println("waited between polls (recorded, not slept): $waits seconds")
    val outcomes = results(client, created.id())
    for ((cid, o) in outcomes) println("result: $cid ${o.kind} ${o.detail}")
    println("in request order: ${TICKETS.keys.joinToString(", ", "[", "]") { "('$it', '${outcomes.getValue(it).kind}')" }}")
    val fix = outcomes.filter { it.value.kind == "errored" && it.value.detail == "invalid_request_error" }.keys.toList()
    val retry = outcomes.filter { it.value.kind in listOf("expired", "canceled") || (it.value.kind == "errored" && it.key !in fix) }.keys.toList()
    println("fix before resubmitting: ${py(fix)} | resubmit unchanged: ${py(retry)}")
}
