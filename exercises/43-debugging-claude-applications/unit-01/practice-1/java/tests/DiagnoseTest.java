import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnoseTest {
    private static final String REQ = "{\"kind\":\"request\",\"model\":\"claude-sonnet-5-5\",\"max_tokens\":1024,\"tools\":[\"get_weather\"],\"last_user_blocks\":[\"text\"]}";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> diag(String... events) {
        return Diagnose.diagnose((List<Map<String, Object>>) Json.parse("[" + String.join(",", events) + "]"));
    }

    private static String label(Map<String, Object> r) {
        return r.get("type") + "|" + r.get("origin") + "|" + r.get("recovery");
    }

    /** The diagnosis of a trace that starts with the usual request, as "type|origin|recovery". */
    private static String one(String... events) {
        String[] all = new String[events.length + 1];
        all[0] = REQ;
        System.arraycopy(events, 0, all, 1, events.length);
        return label(diag(all));
    }

    private static String error(int status, String extra) {
        return "{\"kind\":\"error\",\"status\":" + status + ",\"error_type\":\"x_error\",\"message\":\"m\"" + extra + "}";
    }

    private static String error(int status) {
        return error(status, "");
    }

    private static String reply(String stopReason, String content) {
        return "{\"kind\":\"response\",\"status\":200,\"stop_reason\":\"" + stopReason + "\",\"content\":" + content + "}";
    }

    private static String reply(String stopReason) {
        return reply(stopReason, "[{\"type\":\"text\",\"text\":\"hi\"}]");
    }

    @Test
    void m1_eachDocumentedHttpErrorMapsToATypeAnOriginAndARecovery() {
        assertEquals("invalid_request|integration|fix_request", one(error(400)));
        assertEquals("authentication|account|fix_credentials", one(error(401)));
        assertEquals("billing|account|fix_billing", one(error(402)));
        assertEquals("permission|account|fix_access", one(error(403)));
        assertEquals("not_found|integration|fix_request", one(error(404)));
        assertEquals("conflict|integration|resolve_then_retry", one(error(409)));
        assertEquals("request_too_large|integration|shrink_request", one(error(413)));
        assertEquals("server_error|service|retry_backoff", one(error(500)));
        assertEquals("timeout|service|stream_or_batch", one(error(504)));
        assertEquals("overloaded|service|retry_backoff", one(error(529)));
    }

    @Test
    void e1_a429IsARateLimitOrASpendCapAndOtherStatusesFallBackByClass() {
        assertEquals("rate_limit|service|wait_retry_after", one(error(429, ",\"headers\":{\"retry-after\":\"12\"}")));
        assertEquals("spend_cap|account|wait_for_reset", one(error(429, ",\"error_code\":\"enforced_spend_limit_reached\"")));
        assertEquals("rate_limit|service|wait_retry_after", one(error(429, ",\"headers\":{\"retry-after\":\"3\"},\"error_code\":\"enforced_spend_limit_reached\"")));
        assertEquals("rate_limit|service|retry_backoff", one(error(429)));
        assertEquals("spend_limit|account|raise_limit", one("{\"kind\":\"error\",\"status\":400,\"message\":\"You have reached your workspace Spend Limit\"}"));
        assertEquals("invalid_request|integration|fix_request", one(error(418)));
        assertEquals("server_error|service|retry_backoff", one(error(502)));
    }

    @Test
    void e2_aSuccessfulResponseCanStillFailByItsStopReason() {
        assertEquals("truncated|integration|raise_max_tokens", one(reply("max_tokens")));
        assertEquals("context_exceeded|integration|trim_context", one(reply("model_context_window_exceeded")));
        assertEquals("refusal|model|fallback_model", one(reply("refusal")));
        assertEquals("paused|integration|continue_turn", one(reply("pause_turn")));
        for (String fine : List.of("end_turn", "stop_sequence", "tool_use")) assertEquals("ok|none|none", one(reply(fine)), fine);
    }

    private static String emptyAfter(String blocks) {
        Map<String, Object> r = diag("{\"kind\":\"request\",\"tools\":[],\"last_user_blocks\":" + blocks + "}", reply("end_turn", "[]"));
        return r.get("index") + "|" + label(r);
    }

    @Test
    void e3_anEmptyEndTurnIsTheIntegrationWhenTextFollowedTheToolResultAndTheModelOtherwise() {
        assertEquals("1|empty_response|integration|remove_text_after_tool_result", emptyAfter("[\"tool_result\",\"text\"]"));
        assertEquals("1|empty_response|model|add_continue_prompt", emptyAfter("[\"tool_result\"]"));
        assertEquals("1|empty_response|model|add_continue_prompt", emptyAfter("[\"text\"]"));
        assertEquals("1|empty_response|model|add_continue_prompt", emptyAfter("[\"text\",\"tool_result\"]"));
        assertEquals("1|empty_response|integration|remove_text_after_tool_result", emptyAfter("[\"tool_result\",\"tool_result\",\"text\"]"));
        assertEquals("ok|none|none", one(reply("end_turn")));
    }

    private static String parse(String text) {
        return "{\"kind\":\"parse\",\"ok\":false,\"text\":" + Json.stringify(text) + "}";
    }

    @Test
    void e4_aParseFailureIsTheIntegrationWhenAJsonObjectIsInTheTextAndTheModelWhenNot() {
        assertEquals("parse_failure|integration|extract_json", one(parse("Here you go: {\"label\": \"spam\"} hope it helps")));
        assertEquals("parse_failure|integration|extract_json", one(parse("```json\n{\"a\": 1}\n```")));
        assertEquals("parse_failure|model|validate_and_retry", one(parse("I cannot decide")));
        assertEquals("parse_failure|model|validate_and_retry", one(parse("{\"label\": \"spam\"")));
        assertEquals("parse_failure|model|validate_and_retry", one(parse("see [1, 2] and {nope}")));
        assertEquals("ok|none|none", one("{\"kind\":\"parse\",\"ok\":true,\"text\":\"{}\"}"));
    }

    @Test
    void e5_toolFailuresSplitIntoAModelThatCalledAMissingToolAndOurToolThatRaised() {
        assertEquals("ok|none|none", one("{\"kind\":\"tool_call\",\"name\":\"get_weather\",\"input\":{}}"));
        assertEquals("unknown_tool|model|return_error_result", one("{\"kind\":\"tool_call\",\"name\":\"get_wether\",\"input\":{}}"));
        assertEquals("ok|none|none", one("{\"kind\":\"tool_result\",\"name\":\"get_weather\",\"is_error\":false}"));
        assertEquals("tool_exception|integration|fix_tool_code", one("{\"kind\":\"tool_result\",\"name\":\"get_weather\",\"is_error\":true,\"exception\":\"KeyError: city\"}"));
        assertEquals("ok|none|none", one("{\"kind\":\"tool_result\",\"name\":\"get_weather\",\"is_error\":true}"));
        assertEquals("ok", diag("{\"kind\":\"tool_call\",\"name\":\"anything\",\"input\":{}}").get("type"));
    }

    @Test
    void e6_theFirstFailureNamesTheCauseAndALaterGoodResponseMarksItRecovered() {
        Map<String, Object> r = diag(REQ, error(529), REQ, reply("max_tokens"), REQ, reply("end_turn"));
        assertEquals("1|overloaded|true", r.get("index") + "|" + r.get("type") + "|" + r.get("recovered"));
        r = diag(REQ, error(529), REQ, error(529));
        assertEquals("1|false", r.get("index") + "|" + r.get("recovered"));
        r = diag(REQ, error(401), REQ, reply("end_turn", "[]"));
        assertEquals("1|authentication|false", r.get("index") + "|" + r.get("type") + "|" + r.get("recovered"));
        String clean = "{index=-1, type=ok, origin=none, recovery=none, recovered=false}";
        assertEquals(clean, diag(REQ, reply("end_turn")).toString());
        assertEquals(clean, diag().toString());
    }

    @Test
    void e7_aDroppedConnectionHasNoStatusAndBelongsToTheServiceSide() {
        Map<String, Object> r = diag(REQ, reply("end_turn"), REQ, "{\"kind\":\"network_error\",\"message\":\"connection reset\"}");
        assertEquals("3|network|service|retry_backoff|false", r.get("index") + "|" + label(r) + "|" + r.get("recovered"));
        r = diag(REQ, "{\"kind\":\"network_error\",\"message\":\"timed out\"}", REQ, reply("end_turn"));
        assertEquals("1|network|true", r.get("index") + "|" + r.get("type") + "|" + r.get("recovered"));
    }
}
