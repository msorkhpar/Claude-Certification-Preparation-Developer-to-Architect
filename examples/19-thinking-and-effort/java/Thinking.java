import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Show.py;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.Usage;
import harness.Scripted;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.
 *
 * <p>The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
 * an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
 * `output_tokens_details.thinking_tokens` breakdown the thinking page documents.
 * (`py` is the harness's formatter: it prints a value the way the Python edition does, so the output of the editions matches.)
 */
public final class Thinking {
    private static final System.Logger LOG = System.getLogger(Thinking.class.getName());
    static final String MODEL = "claude-opus-5-5";
    static final double PRICE_OUT = 20.0; // dollars per million output tokens, pricing page 2026-10-02
    static final Map<String, Object> THINKING_BLOCK = map("type", "thinking", "thinking", "", "signature", "illustrative-signature");
    static final Map<String, Object> USAGE = map("input_tokens", 410, "output_tokens", 1900, "output_tokens_details", map("thinking_tokens", 1650));

    /** usage.output_tokens_details.thinking_tokens */
    static long thinkingTokens(Usage usage) {
        return usage.outputTokensDetails().get().thinkingTokens();
    }

    static Message request(AnthropicClient client, String effort) {
        return client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(8000)
            .thinking(ThinkingConfigAdaptive.builder().build()).outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(effort)).build())
            .addUserMessage("Which of these two schedules has no conflicts?").build());
    }

    static String kinds(Message reply) {
        return py(reply.content().stream().map(b -> b.isThinking() ? "thinking" : b.isText() ? "text" : "other").toList());
    }

    public static void main(String[] args) {
        Map<String, Object> replyBody = message(List.of(THINKING_BLOCK, text("Schedule B has no conflicts.")), "end_turn", MODEL, USAGE, null);
        Scripted.Rig rig = Scripted.client(replyBody, message(List.of(text("B.")), "end_turn", MODEL, map("input_tokens", 410, "output_tokens", 12), null));
        Message reply = request(rig.client(), "high");
        var sent = rig.http().requests.get(0);
        System.out.println("thinking sent: " + py(sent.get("thinking")) + " | effort sent: " + py(sent.get("output_config")));
        ContentBlock first = reply.content().get(0);
        System.out.println("blocks: " + kinds(reply) + " | thinking text shown: " + py(first.asThinking().thinking()));
        long thinking = thinkingTokens(reply.usage());
        System.out.println("output_tokens " + reply.usage().outputTokens() + " = thinking " + thinking + " + answer " + (reply.usage().outputTokens() - thinking));
        System.out.println(String.format(Locale.ROOT, "output cost: $%.4f (thinking is billed as output, shown or not)", reply.usage().outputTokens() * PRICE_OUT / 1_000_000));
        Message quick = request(rig.client(), "low");
        System.out.println("a low-effort turn may skip thinking: " + kinds(quick) + " | output_tokens " + quick.usage().outputTokens());
        System.out.println("effort differs between the two requests: " + py(!rig.http().requests.get(0).get("output_config").equals(rig.http().requests.get(1).get("output_config"))));
    }
}
