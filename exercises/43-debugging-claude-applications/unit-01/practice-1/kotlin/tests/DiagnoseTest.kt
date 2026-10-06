import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiagnoseTest {
    private val req = """{"kind":"request","model":"claude-sonnet-5-5","max_tokens":1024,"tools":["get_weather"],"last_user_blocks":["text"]}"""

    @Suppress("UNCHECKED_CAST")
    private fun diag(vararg events: String): Map<String, Any?> =
        Diagnose.diagnose(Json.parse("[" + events.joinToString(",") + "]") as List<Map<String, Any?>>)

    private fun label(r: Map<String, Any?>) = "${r["type"]}|${r["origin"]}|${r["recovery"]}"

    /** The diagnosis of a trace that starts with the usual request, as "type|origin|recovery". */
    private fun one(vararg events: String) = label(diag(req, *events))

    private fun error(status: Int, extra: String = "") = """{"kind":"error","status":$status,"error_type":"x_error","message":"m"$extra}"""

    private fun reply(stopReason: String, content: String = """[{"type":"text","text":"hi"}]""") =
        """{"kind":"response","status":200,"stop_reason":"$stopReason","content":$content}"""

    @Test
    fun m1_eachDocumentedHttpErrorMapsToATypeAnOriginAndARecovery() {
        assertEquals("invalid_request|integration|fix_request", one(error(400)))
        assertEquals("authentication|account|fix_credentials", one(error(401)))
        assertEquals("billing|account|fix_billing", one(error(402)))
        assertEquals("permission|account|fix_access", one(error(403)))
        assertEquals("not_found|integration|fix_request", one(error(404)))
        assertEquals("conflict|integration|resolve_then_retry", one(error(409)))
        assertEquals("request_too_large|integration|shrink_request", one(error(413)))
        assertEquals("server_error|service|retry_backoff", one(error(500)))
        assertEquals("timeout|service|stream_or_batch", one(error(504)))
        assertEquals("overloaded|service|retry_backoff", one(error(529)))
    }

    @Test
    fun e1_a429IsARateLimitOrASpendCapAndOtherStatusesFallBackByClass() {
        assertEquals("rate_limit|service|wait_retry_after", one(error(429, ""","headers":{"retry-after":"12"}""")))
        assertEquals("spend_cap|account|wait_for_reset", one(error(429, ""","error_code":"enforced_spend_limit_reached"""")))
        assertEquals("rate_limit|service|wait_retry_after", one(error(429, ""","headers":{"retry-after":"3"},"error_code":"enforced_spend_limit_reached"""")))
        assertEquals("rate_limit|service|retry_backoff", one(error(429)))
        assertEquals("spend_limit|account|raise_limit", one("""{"kind":"error","status":400,"message":"You have reached your workspace Spend Limit"}"""))
        assertEquals("invalid_request|integration|fix_request", one(error(418)))
        assertEquals("server_error|service|retry_backoff", one(error(502)))
    }

    @Test
    fun e2_aSuccessfulResponseCanStillFailByItsStopReason() {
        assertEquals("truncated|integration|raise_max_tokens", one(reply("max_tokens")))
        assertEquals("context_exceeded|integration|trim_context", one(reply("model_context_window_exceeded")))
        assertEquals("refusal|model|fallback_model", one(reply("refusal")))
        assertEquals("paused|integration|continue_turn", one(reply("pause_turn")))
        for (fine in listOf("end_turn", "stop_sequence", "tool_use")) assertEquals("ok|none|none", one(reply(fine)), fine)
    }

    private fun emptyAfter(blocks: String): String {
        val r = diag("""{"kind":"request","tools":[],"last_user_blocks":$blocks}""", reply("end_turn", "[]"))
        return "${r["index"]}|${label(r)}"
    }

    @Test
    fun e3_anEmptyEndTurnIsTheIntegrationWhenTextFollowedTheToolResultAndTheModelOtherwise() {
        assertEquals("1|empty_response|integration|remove_text_after_tool_result", emptyAfter("""["tool_result","text"]"""))
        assertEquals("1|empty_response|model|add_continue_prompt", emptyAfter("""["tool_result"]"""))
        assertEquals("1|empty_response|model|add_continue_prompt", emptyAfter("""["text"]"""))
        assertEquals("1|empty_response|model|add_continue_prompt", emptyAfter("""["text","tool_result"]"""))
        assertEquals("1|empty_response|integration|remove_text_after_tool_result", emptyAfter("""["tool_result","tool_result","text"]"""))
        assertEquals("ok|none|none", one(reply("end_turn")))
    }

    private fun parse(text: String) = """{"kind":"parse","ok":false,"text":${Json.stringify(text)}}"""

    @Test
    fun e4_aParseFailureIsTheIntegrationWhenAJsonObjectIsInTheTextAndTheModelWhenNot() {
        assertEquals("parse_failure|integration|extract_json", one(parse("Here you go: {\"label\": \"spam\"} hope it helps")))
        assertEquals("parse_failure|integration|extract_json", one(parse("```json\n{\"a\": 1}\n```")))
        assertEquals("parse_failure|model|validate_and_retry", one(parse("I cannot decide")))
        assertEquals("parse_failure|model|validate_and_retry", one(parse("{\"label\": \"spam\"")))
        assertEquals("parse_failure|model|validate_and_retry", one(parse("see [1, 2] and {nope}")))
        assertEquals("ok|none|none", one("""{"kind":"parse","ok":true,"text":"{}"}"""))
    }

    @Test
    fun e5_toolFailuresSplitIntoAModelThatCalledAMissingToolAndOurToolThatRaised() {
        assertEquals("ok|none|none", one("""{"kind":"tool_call","name":"get_weather","input":{}}"""))
        assertEquals("unknown_tool|model|return_error_result", one("""{"kind":"tool_call","name":"get_wether","input":{}}"""))
        assertEquals("ok|none|none", one("""{"kind":"tool_result","name":"get_weather","is_error":false}"""))
        assertEquals("tool_exception|integration|fix_tool_code", one("""{"kind":"tool_result","name":"get_weather","is_error":true,"exception":"KeyError: city"}"""))
        assertEquals("ok|none|none", one("""{"kind":"tool_result","name":"get_weather","is_error":true}"""))
        assertEquals("ok", diag("""{"kind":"tool_call","name":"anything","input":{}}""")["type"])
    }

    @Test
    fun e6_theFirstFailureNamesTheCauseAndALaterGoodResponseMarksItRecovered() {
        var r = diag(req, error(529), req, reply("max_tokens"), req, reply("end_turn"))
        assertEquals("1|overloaded|true", "${r["index"]}|${r["type"]}|${r["recovered"]}")
        r = diag(req, error(529), req, error(529))
        assertEquals("1|false", "${r["index"]}|${r["recovered"]}")
        r = diag(req, error(401), req, reply("end_turn", "[]"))
        assertEquals("1|authentication|false", "${r["index"]}|${r["type"]}|${r["recovered"]}")
        val clean = "{index=-1, type=ok, origin=none, recovery=none, recovered=false}"
        assertEquals(clean, diag(req, reply("end_turn")).toString())
        assertEquals(clean, diag().toString())
    }

    @Test
    fun e7_aDroppedConnectionHasNoStatusAndBelongsToTheServiceSide() {
        var r = diag(req, reply("end_turn"), req, """{"kind":"network_error","message":"connection reset"}""")
        assertEquals("3|network|service|retry_backoff|false", "${r["index"]}|${label(r)}|${r["recovered"]}")
        r = diag(req, """{"kind":"network_error","message":"timed out"}""", req, reply("end_turn"))
        assertEquals("1|network|true", "${r["index"]}|${r["type"]}|${r["recovered"]}")
    }
}
