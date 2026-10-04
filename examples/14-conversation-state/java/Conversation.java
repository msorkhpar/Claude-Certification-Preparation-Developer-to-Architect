import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A conversation the client keeps: the API is stateless, so every request carries the whole history.
 *
 * <p>Three turns through the real SDK against a scripted transport. The replies are illustrative,
 * hand-written Messages responses (claude-sonnet-5-5), not captures.
 */
public final class Conversation {
    static final String MODEL = "claude-sonnet-5-5";
    static final String SYSTEM = "You answer in one short sentence.";

    static final List<Map<String, Object>> REPLIES = List.of(
        message(List.of(text("Paris.")), "end_turn", MODEL, map("input_tokens", 18, "output_tokens", 4), null),
        message(List.of(text("It has been the capital since")), "max_tokens", MODEL, map("input_tokens", 30, "output_tokens", 6), null),
        message(List.of(text("Seine")), "stop_sequence", MODEL, map("input_tokens", 41, "output_tokens", 2), "END"));
    static final List<String> QUESTIONS = List.of("Capital of France?", "Since when?", "Name its river. End with END.");

    /** One turn: the reply and the token totals so far. */
    record Turn(Message reply, long input, long output) {}

    /** Keep the history in the request builder and send all of it every time. */
    static List<Turn> run(AnthropicClient client, List<String> questions) {
        MessageCreateParams.Builder history = MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(16).system(SYSTEM).stopSequences(List.of("END"));
        long input = 0, output = 0;
        List<Turn> turns = new ArrayList<>();
        for (String question : questions) {
            history.addUserMessage(question);
            Message reply = client.messages().create(history.build());
            history.addMessage(reply); // the assistant turn goes back exactly as it was received
            input += reply.usage().inputTokens();
            output += reply.usage().outputTokens();
            turns.add(new Turn(reply, input, output));
        }
        return turns;
    }

    public static void main(String[] args) {
        ScriptedHttp transport = Scripted.http(REPLIES.toArray());
        List<Turn> turns = run(Scripted.clientOn(transport, 0), QUESTIONS);
        for (int number = 1; number <= turns.size(); number++) {
            Message reply = turns.get(number - 1).reply();
            JsonNode sent = transport.requests.get(number - 1).get("messages");
            List<String> roles = new ArrayList<>();
            sent.forEach(m -> roles.add(m.get("role").asText()));
            String stop = reply.stopReason().get().asString();
            String sequence = reply.stopSequence().map(s -> " '" + s + "'").orElse("");
            System.out.println("turn " + number + ": sent " + sent.size() + " message(s) [" + String.join(", ", roles) + "] -> " + stop + sequence + ", '" + reply.content().get(0).asText().text() + "'");
        }
        Turn last = turns.get(turns.size() - 1);
        System.out.println("totals: {'input': " + last.input() + ", 'output': " + last.output() + "}");
        boolean systemTopLevel = transport.requests.stream().allMatch(r -> SYSTEM.equals(r.path("system").asText()));
        TreeSet<String> used = new TreeSet<>();
        transport.requests.forEach(r -> r.get("messages").forEach(m -> used.add(m.get("role").asText())));
        System.out.println("system is a top-level field: " + (systemTopLevel ? "True" : "False") + " | roles ever used in messages: "
            + used.stream().map(r -> "'" + r + "'").collect(Collectors.joining(", ", "[", "]")));
    }
}
