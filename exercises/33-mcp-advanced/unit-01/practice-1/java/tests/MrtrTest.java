import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MrtrTest {
    private static final String SECRET = "s3cret";
    private static final String VERSION = "2026-07-28";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static final Map<String, Object> BOTH = map("elicitation", map(), "sampling", map());
    private static final Map<String, Object> ARGS = map("service", "api", "env", "production");
    private static final Map<String, Object> CONFIRM = map("confirm", map("method", "elicitation/create", "params", map("mode", "form", "message", "Deploy api to production?", "requestedSchema",
            map("type", "object", "properties", map("confirm", map("type", "boolean", "title", "Confirm the deployment")), "required", List.of("confirm")))));
    private static final Map<String, Object> NOTES = map("notes", map("method", "sampling/createMessage", "params", map("messages",
            List.of(map("role", "user", "content", map("type", "text", "text", "Write one sentence of release notes for api."))), "maxTokens", 100)));
    private static final Map<String, Object> YES = map("confirm", map("action", "accept", "content", map("confirm", true)));

    private static Map<String, Object> request(String name, Map<String, Object> args, Map<String, Object> caps, String version, Object... extra) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (version != null) meta.put("io.modelcontextprotocol/protocolVersion", version);
        meta.put("io.modelcontextprotocol/clientCapabilities", caps == null ? BOTH : caps);
        Map<String, Object> r = map("name", name, "arguments", args == null ? ARGS : args, "_meta", meta);
        for (int i = 0; i < extra.length; i += 2) r.put((String) extra[i], extra[i + 1]);
        return r;
    }

    private static Map<String, Object> request() {
        return request("deploy", null, null, VERSION);
    }

    private static Map<String, Object> call(Map<String, Object> req, long now, String principal, String secret) {
        Map<String, Object> r = Mrtr.callTool(req, secret, principal, now);
        return r == null ? new LinkedHashMap<>() : r;
    }

    private static Map<String, Object> call(Map<String, Object> req, long now) {
        return call(req, now, "alice", SECRET);
    }

    private static Map<String, Object> call(Map<String, Object> req) {
        return call(req, 1000);
    }

    private static Map<String, Object> retry(Map<String, Object> first, Object responses, Map<String, Object> caps) {
        return request("deploy", null, caps, VERSION, "inputResponses", responses, "requestState", first.get("requestState"));
    }

    private static Map<String, Object> retry(Map<String, Object> first, Object responses) {
        return retry(first, responses, null);
    }

    private static Map<String, Object> complete(String text, boolean isError) {
        return map("resultType", "complete", "content", List.of(map("type", "text", "text", text)), "isError", isError);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> errorOf(Map<String, Object> result) {
        Object e = result.get("error");
        Map<String, Object> m = e instanceof Map<?, ?> ? (Map<String, Object>) e : new LinkedHashMap<>();
        return Arrays.asList(m.get("code"), m.get("message"));
    }

    private static Map<String, Object> state(Map<String, Object> over) {
        Map<String, Object> payload = map("v", 1, "tool", "deploy", "digest", Mrtr.argsDigest(ARGS), "sub", "alice", "exp", 999, "step", "confirm");
        payload.putAll(over);
        return payload;
    }

    @Test
    void m1_aProductionDeployTakesThreeRoundTripsAndKeepsNoState() {
        Map<String, Object> first = call(request());
        assertEquals("input_required", first.get("resultType"));
        assertEquals(CONFIRM, first.get("inputRequests"));
        assertTrue(first.get("requestState") instanceof String && !first.containsKey("content"));
        Map<String, Object> second = call(retry(first, YES), 1010);
        assertEquals("input_required", second.get("resultType"));
        assertEquals(NOTES, second.get("inputRequests"));
        assertTrue(second.get("requestState") instanceof String && !second.get("requestState").equals(first.get("requestState")));
        Map<String, Object> notes = map("notes", map("role", "assistant", "content", map("type", "text", "text", "Faster checkout."), "model", "m", "stopReason", "endTurn"));
        assertEquals(complete("Deployed api to production. Release notes: Faster checkout.", false), call(retry(second, notes), 1020));
    }

    @Test
    @SuppressWarnings("unchecked")
    void e1_aStagingDeployAndAStatusCallFinishAtOnceAndTheToolListIsCacheable() {
        assertEquals(complete("Deployed api to staging", false), call(request("deploy", map("service", "api", "env", "staging"), map(), VERSION)));
        assertEquals(complete("api: running", false), call(request("status", map("service", "api"), map(), VERSION)));
        Map<String, Object> listing = Mrtr.listTools(map("_meta", map("io.modelcontextprotocol/protocolVersion", VERSION)));
        listing = listing == null ? new LinkedHashMap<>() : listing;
        assertEquals(Arrays.asList("complete", 300000, "public"), Arrays.asList(listing.get("resultType"), listing.get("ttlMs"), listing.get("cacheScope")));
        List<Map<String, Object>> tools = listing.get("tools") == null ? new ArrayList<>() : (List<Map<String, Object>>) listing.get("tools");
        assertEquals(List.of("deploy", "status"), tools.stream().map(t -> t.get("name")).toList());
        assertEquals(List.of(List.of("service", "env"), List.of("service")), tools.stream().map(t -> ((Map<String, Object>) t.get("inputSchema")).get("required")).toList());
    }

    @Test
    void e2_theServerOnlyAsksWhatTheClientDeclaredItCanAnswer() {
        String refusal = "Deploying to production needs confirmation, and this client cannot be asked.";
        assertEquals(complete(refusal, true), call(request("deploy", null, map(), VERSION)));
        assertEquals(complete(refusal, true), call(request("deploy", null, map("sampling", map()), VERSION)));
        assertEquals(complete(refusal, true), call(request("deploy", null, map("elicitation", map("url", map())), VERSION)));
        Map<String, Object> formOnly = map("elicitation", map("form", map()));
        Map<String, Object> first = call(request("deploy", null, formOnly, VERSION));
        assertEquals(CONFIRM, first.get("inputRequests"));
        assertEquals(complete("Deployed api to production", false), call(retry(first, YES, formOnly), 1010));
    }

    @Test
    void e3_aNoOrAMissingAnswerIsHandledWithoutAnError() {
        Map<String, Object> first = call(request());
        List<Object> answers = List.of(map("action", "decline"), map("action", "cancel"), map("action", "accept", "content", map("confirm", false)), map("action", "accept"),
                map("action", "decline", "content", map("confirm", true)));
        for (Object answer : answers) assertEquals(complete("Deployment cancelled", false), call(retry(first, map("confirm", answer)), 1010));
        for (Object responses : List.of(map(), map("other", 1), map("confirm", map("action", "maybe")), map("confirm", "yes"))) {
            Map<String, Object> again = call(retry(first, responses), 1100);
            assertEquals("input_required", again.get("resultType"));
            assertEquals(CONFIRM, again.get("inputRequests"));
            assertTrue(again.get("requestState") != null && !again.get("requestState").equals(first.get("requestState")));
        }
        Map<String, Object> second = call(retry(first, YES), 1010);
        assertEquals(NOTES, call(retry(second, map()), 1020).get("inputRequests"));
        assertEquals(NOTES, call(retry(second, map("notes", map("role", "assistant", "content", map("type", "image")))), 1020).get("inputRequests"));
    }

    @Test
    void e4_aStateTheServerDidNotSignIsRefused() {
        Map<String, Object> first = call(request());
        String token = first.get("requestState") instanceof String s ? s : "x.y";
        String flipped = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
        Object foreign = call(request(), 1000, "alice", "another secret").get("requestState");
        for (Object bad : Arrays.asList(flipped, "abc", "", foreign)) {
            assertEquals(Arrays.asList(-32602, "Invalid requestState"), errorOf(call(request("deploy", null, null, VERSION, "inputResponses", YES, "requestState", bad), 1010)));
        }
    }

    private static Map<String, Object> go(String token, Map<String, Object> args) {
        return request("deploy", args, null, VERSION, "inputResponses", YES, "requestState", token);
    }

    @Test
    void e5_aStateWorksOnlyForTheSameUserTheSameCallAndBeforeItExpires() {
        String same = "requestState does not match this request";
        String token = Mrtr.mintState(SECRET, state(map()));
        assertEquals(NOTES, call(go(token, null), 999).get("inputRequests"));
        assertEquals(Arrays.asList(-32602, "Expired requestState"), errorOf(call(go(token, null), 1000)));
        assertEquals(Arrays.asList(-32602, same), errorOf(call(go(token, null), 999, "bob", SECRET)));
        assertEquals(Arrays.asList(-32602, same), errorOf(call(go(token, map("service", "billing", "env", "production")), 999)));
        assertEquals(Arrays.asList(-32602, same), errorOf(call(go(Mrtr.mintState(SECRET, state(map("tool", "status"))), null), 999)));
        assertEquals(NOTES, call(go(Mrtr.mintState(SECRET, state(map("step", "notes"))), null), 999).get("inputRequests"));
    }

    @Test
    void e6_aRequestTheServerCannotServeIsAProtocolErrorWithACode() {
        Map<String, Object> old = call(request("deploy", null, null, "2025-11-25"));
        assertEquals(Arrays.asList(-32022, "Unsupported protocol version"), errorOf(old));
        assertEquals(map("supported", List.of(VERSION)), old.get("error") instanceof Map<?, ?> e ? e.get("data") : null);
        assertEquals(-32022, errorOf(call(request("deploy", null, null, null))).get(0));
        assertEquals(Arrays.asList(-32602, "Unknown tool: rollback"), errorOf(call(request("rollback", null, null, VERSION))));
        assertEquals(Arrays.asList(-32602, "Invalid params: service is required"), errorOf(call(request("deploy", map("env", "staging"), null, VERSION))));
        assertEquals(Arrays.asList(-32602, "Invalid params: service is required"), errorOf(call(request("deploy", map("service", "  ", "env", "staging"), null, VERSION))));
        assertEquals(Arrays.asList(-32602, "Invalid params: env must be staging or production"), errorOf(call(request("deploy", map("service", "api", "env", "dev"), null, VERSION))));
        assertEquals("Invalid params: service is required", errorOf(call(request("status", map(), null, VERSION))).get(1));
        Map<String, Object> listed = Mrtr.listTools(map("_meta", map("io.modelcontextprotocol/protocolVersion", "2024-11-05")));
        assertEquals(-32022, errorOf(listed == null ? new LinkedHashMap<>() : listed).get(0));
    }

    @Test
    void e7_theStateCarriesTheWholeContextAndAnAnswerAloneNeverSkipsAStep() {
        Map<String, Object> first = call(request(), 1000);
        Map<String, Object> again = call(request(), 1000);
        assertEquals(first.get("inputRequests"), again.get("inputRequests"));
        Map<String, Object> payload = first.get("requestState") instanceof String t ? Mrtr.readState(SECRET, t) : null;
        payload = payload == null ? new LinkedHashMap<>() : payload;
        assertEquals(Arrays.asList(1L, "deploy", Mrtr.argsDigest(ARGS), "alice", 1300L, "confirm"),
                Arrays.asList(num(payload.get("v")), payload.get("tool"), payload.get("digest"), payload.get("sub"), num(payload.get("exp")), payload.get("step")));
        assertEquals(Mrtr.argsDigest(ARGS), Mrtr.argsDigest(map("env", "production", "service", "api")));
        Map<String, Object> alone = call(request("deploy", null, null, VERSION, "inputResponses", YES));
        assertEquals("input_required", alone.get("resultType"));
        assertEquals(CONFIRM, alone.get("inputRequests"));
        Map<String, Object> noisy = new LinkedHashMap<>(YES);
        noisy.put("junk", map("action", "accept"));
        assertEquals(NOTES, call(retry(first, noisy), 1010).get("inputRequests"));
        Map<String, Object> second = call(retry(first, YES), 1010);
        Map<String, Object> secondState = second.get("requestState") instanceof String t ? Mrtr.readState(SECRET, t) : null;
        assertEquals("notes", secondState == null ? "none" : secondState.get("step"));
        Map<String, Object> before = new LinkedHashMap<>(first);
        call(retry(first, YES), 1010);
        assertEquals(before, first);
    }

    private static Object num(Object o) {
        return o instanceof Number n ? (Object) n.longValue() : o;
    }
}
