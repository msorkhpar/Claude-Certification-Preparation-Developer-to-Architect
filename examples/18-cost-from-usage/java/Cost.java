import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Usage;
import harness.Scripted;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Count tokens before sending, then price the reply from its usage object.
 *
 * <p>Both calls go through a scripted transport, so nothing leaves the container. The replies are illustrative,
 * hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
 * tokens, read from the Claude pricing page on 2026-10-02.
 */
public final class Cost {
    static final String MODEL = "claude-sonnet-5-5";
    static final String SYSTEM = "You answer from the policy document.";
    static final String QUESTION = "Summarise the refund policy in two sentences.";

    /** input, output, cache-read multiplier */
    static final Map<String, double[]> PRICES = new LinkedHashMap<>();

    static {
        PRICES.put("claude-haiku-4-5-20251001", new double[] {1.0, 5.0, 0.1});
        PRICES.put("claude-sonnet-5-5", new double[] {2.0, 10.0, 0.1});
        PRICES.put("claude-opus-5-5", new double[] {4.0, 20.0, 0.05});
        PRICES.put("claude-fable-5-1", new double[] {10.0, 50.0, 0.025});
    }

    static final Map<String, Object> USAGE = map("input_tokens", 120, "output_tokens", 340, "cache_read_input_tokens", 0, "cache_creation_input_tokens", 4000,
        "cache_creation", map("ephemeral_5m_input_tokens", 4000, "ephemeral_1h_input_tokens", 0));

    static MessageCreateParams params() {
        return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).system(SYSTEM).addUserMessage(QUESTION).build();
    }

    static double cost(String model, Usage usage) {
        return cost(model, usage, false);
    }

    /** Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction. */
    static double cost(String model, Usage usage, boolean batch) {
        double[] price = PRICES.get(model);
        double priceIn = price[0], priceOut = price[1], read = price[2];
        double perToken = usage.inputTokens() * priceIn
            + usage.cacheCreation().get().ephemeral5mInputTokens() * priceIn * 1.25
            + usage.cacheCreation().get().ephemeral1hInputTokens() * priceIn * 2.0
            + usage.cacheReadInputTokens().get() * priceIn * read
            + usage.outputTokens() * priceOut;
        return perToken / 1_000_000 * (batch ? 0.5 : 1.0);
    }

    private static String dollars(double v) {
        return String.format(Locale.ROOT, "$%.6f", v);
    }

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(map("input_tokens", 4821), message(List.of(text("Refunds take 14 days. Opened items are excluded.")), "end_turn", MODEL, USAGE, null));
        long counted = rig.client().messages().countTokens(MessageCountTokensParams.builder().model(MODEL).system(SYSTEM).addUserMessage(QUESTION).build()).inputTokens();
        System.out.println("count_tokens -> " + counted + " input tokens (POST " + rig.http().urls.get(0).split("\\.com")[1] + ")");
        double priceIn = PRICES.get(MODEL)[0];
        System.out.println("estimate before sending, input only: " + dollars(counted * priceIn / 1_000_000) + " on " + MODEL);
        Usage u = rig.client().messages().create(params()).usage();
        System.out.println("usage: input " + u.inputTokens() + ", cache write " + u.cacheCreationInputTokens().get() + ", cache read " + u.cacheReadInputTokens().get() + ", output " + u.outputTokens());
        System.out.println("cost of this request: " + dollars(cost(MODEL, u)) + "  (batch: " + dollars(cost(MODEL, u, true)) + ")");
        System.out.println("the same usage on each model:");
        for (String model : PRICES.keySet()) System.out.println(String.format("  %-28s ", model) + dollars(cost(model, u)));
    }
}
