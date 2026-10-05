import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A conversation client that keeps the state the API does not. See ../../statement.md for the contract. */
final class Conversation {
    private static final System.Logger LOG = System.getLogger(Conversation.class.getName());
    private final Send send;
    private final String model;
    private final int maxTokens;
    private final String system;
    private final List<String> stopSequences;
    private List<Map<String, Object>> history = new ArrayList<>();
    private long inputTokens;
    private long outputTokens;

    Conversation(Send send, String model, int maxTokens, String system, List<String> stopSequences) {
        this.send = send;
        this.model = model;
        this.maxTokens = maxTokens;
        this.system = system;
        this.stopSequences = stopSequences;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> copy(List<Map<String, Object>> turns) {
        return (List<Map<String, Object>>) Json.parse(Json.stringify(turns));
    }

    private void checkText(String text) {
        // TODO 1 of 8 (finish this to pass e6): refuse a blank turn before anything is sent.
        // Receives the text of the turn. Throws IllegalArgumentException when it is null, empty or only whitespace; otherwise returns
        // nothing. Example: checkText("   ") -> IllegalArgumentException, checkText("hi") -> nothing
    }

    private Map<String, Object> requestBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", (long) maxTokens);
        // TODO 2 of 8 (finish this to pass m1 and e5): the request body, a snapshot.
        // Receives nothing (it reads the fields). Puts into body, next to model and max_tokens, "messages": copy(history), a deep copy
        // of the whole history so far (the new user turn is already in it), so a later turn cannot change a request already sent.
        // Example: after say("a"), the first body has messages [{role=user, content=a}]
        body.put("messages", new ArrayList<>());
        return body;
    }

    private void optionalFields(Map<String, Object> body) {
        // TODO 3 of 8 (finish this to pass e4): the top-level fields that are only sometimes there.
        // Receives the body and adds to it: "system" (a top-level field, never a message) when system is not blank, and
        // "stop_sequences" (a copy of the list) when stopSequences is given and not empty. Returns nothing.
        // Example: with system "Be brief." the body gains system=Be brief.; with system "  " it gains nothing
    }

    private Map<String, Object> sendOrRollBack(Map<String, Object> body) {
        // TODO 4 of 8 (finish this to pass e2): send the body, and leave no dangling user turn when the call fails.
        // Receives the body. Returns send.send(body). When send throws a RuntimeException, removes the user turn that say added to
        // history and throws the same exception again, so roles keep alternating on the next call.
        return send.send(body);
    }

    private Map<String, Object> assistantTurn(Map<String, Object> response) {
        Map<String, Object> assistant = new LinkedHashMap<>();
        assistant.put("role", "assistant");
        // TODO 5 of 8 (finish this to pass m1): the turn to store for the reply.
        // Receives the response. Fills assistant with "role" = "assistant" and "content" = the response's content list, as received.
        // Example: content [{type=text, text=Paris.}] -> {role=assistant, content=[that same list]}
        assistant.put("role", "assistant");
        assistant.put("content", new ArrayList<>());
        return assistant;
    }

    private void addUsage(Object usageValue) {
        // TODO 6 of 8 (finish this to pass e1): keep the running totals.
        // Receives the response's usage (a Map, or null; a key may be missing). Adds its input_tokens and output_tokens to the
        // inputTokens and outputTokens fields. Returns nothing.
        // Example: inputTokens 12 plus usage {input_tokens=30, output_tokens=9} -> inputTokens 42
    }

    private Reply makeReply(Map<String, Object> response) {
        // TODO 7 of 8 (finish this to pass m1 and e3): what say returns.
        // Receives the response. Returns a Reply: the text of the content blocks whose type is "text" joined with nothing between
        // them, the response's stop_reason, and truncated, true only when the stop reason is "max_tokens".
        // Example: stop_reason "max_tokens" -> truncated true; "end_turn" -> truncated false
        return new Reply("", "", false);
    }

    Reply say(String text) {
        LOG.log(System.Logger.Level.DEBUG, "say input: {0}", text);
        checkText(text);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", text);
        history.add(user);
        Map<String, Object> body = requestBody();
        optionalFields(body);
        Map<String, Object> response = sendOrRollBack(body);
        history.add(assistantTurn(response));
        addUsage(response.get("usage"));
        return makeReply(response);
    }

    List<Map<String, Object>> history() {
        // TODO 8 of 8 (finish this to pass e5): the turns so far, as a copy.
        // Returns copy(history), so changing what the caller gets changes nothing here.
        // Example: chat.history().add(x) leaves chat.history().size() unchanged
        return new ArrayList<>();
    }

    Map<String, Long> totals() {
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put("input_tokens", inputTokens);
        totals.put("output_tokens", outputTokens);
        return totals;
    }

    void reset() {
        history = new ArrayList<>();
        inputTokens = 0;
        outputTokens = 0;
    }
}
