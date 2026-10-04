import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Usage;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Prompt caching seen through the usage object, against a scripted server that applies the documented prefix rule.
 *
 * <p>The server (CacheSim) is an illustrative, hand-written stand-in, not a capture: it counts a token as four characters,
 * caches the prefix up to a block that carries cache_control when that prefix reaches the minimum size, keeps it for five
 * minutes from its last use, and reports `cache_creation_input_tokens`, `cache_read_input_tokens` and `input_tokens` (the
 * tokens after the last breakpoint) as the prompt caching page describes them (claude-sonnet-5-5, minimum 512 tokens).
 */
public final class CacheHits {
    static final String MODEL = "claude-sonnet-5-5";
    static final String POLICY = "Refund policy clause: items may be returned within 14 days. ".repeat(40); // about 600 tokens

    static int tokens(String piece) {
        return (piece.length() + 3) / 4; // ceil(length / 4)
    }

    /** One block of the prompt with the text it holds and whether it carries a cache_control breakpoint. */
    record Block(String text, boolean mark) {}

    /** The request flattened in prefix order: tools, then system, then messages, each with its cache_control. */
    static List<Block> blocksOf(JsonNode body) {
        List<Block> out = new ArrayList<>();
        for (JsonNode tool : body.path("tools")) out.add(new Block(tool.toString(), tool.has("cache_control")));
        JsonNode system = body.path("system");
        if (system.isTextual()) out.add(new Block(system.asText(), false));
        else for (JsonNode b : system) out.add(new Block(b.get("text").asText(), b.has("cache_control")));
        for (JsonNode m : body.get("messages")) {
            String role = m.get("role").asText();
            if (m.get("content").isTextual()) out.add(new Block(role + ": " + m.get("content").asText(), false));
            else for (JsonNode b : m.get("content")) out.add(new Block(role + ": " + b.get("text").asText(), b.has("cache_control")));
        }
        return out;
    }

    /** The scripted server: a reply function of the request body, with a clock the program moves. */
    static final class CacheSim implements Function<JsonNode, Object> {
        final int minimum, ttl;
        int clock = 0;
        final Map<String, Integer> entries = new HashMap<>();

        CacheSim() {
            this(512, 300);
        }

        CacheSim(int minimum, int ttl) {
            this.minimum = minimum;
            this.ttl = ttl;
        }

        private static String key(List<Block> blocks, int i) {
            StringBuilder k = new StringBuilder();
            for (int j = 0; j <= i; j++) k.append(j == 0 ? "" : "\u0000").append(blocks.get(j).text());
            return k.toString();
        }

        private static int size(List<Block> blocks, int i) {
            int total = 0;
            for (int j = 0; j <= i; j++) total += tokens(blocks.get(j).text());
            return total;
        }

        @Override
        public Object apply(JsonNode body) {
            List<Block> blocks = blocksOf(body);
            List<Integer> marks = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) if (blocks.get(i).mark()) marks.add(i);
            int read = 0, written = 0;
            Integer hit = null;
            for (int n = marks.size() - 1; n >= 0; n--) {
                int i = marks.get(n);
                if (entries.getOrDefault(key(blocks, i), -1) > clock) {
                    read = size(blocks, i);
                    hit = i;
                    entries.put(key(blocks, i), clock + ttl); // a hit refreshes the entry
                    break;
                }
            }
            if (!marks.isEmpty()) {
                int last = marks.get(marks.size() - 1);
                if (!Integer.valueOf(last).equals(hit) && size(blocks, last) >= minimum) {
                    written = size(blocks, last) - read;
                    entries.put(key(blocks, last), clock + ttl);
                }
            }
            int fresh = size(blocks, blocks.size() - 1) - read - written;
            Map<String, Object> usage = map("input_tokens", fresh, "output_tokens", 20, "cache_read_input_tokens", read, "cache_creation_input_tokens", written,
                "cache_creation", map("ephemeral_5m_input_tokens", written, "ephemeral_1h_input_tokens", 0));
            return message(List.of(text("ok")), "end_turn", MODEL, usage, null);
        }
    }

    static MessageCreateParams request(List<Block> system, String question) {
        List<TextBlockParam> blocks = new ArrayList<>();
        for (Block b : system) {
            TextBlockParam.Builder block = TextBlockParam.builder().text(b.text());
            if (b.mark()) block.cacheControl(CacheControlEphemeral.builder().build());
            blocks.add(block.build());
        }
        return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(50).systemOfTextBlockParams(blocks).addUserMessage(question).build();
    }

    static MessageCreateParams stable(String question) {
        return request(List.of(new Block(POLICY, true)), question);
    }

    static MessageCreateParams stampFirst(String clockLabel, String question) {
        return request(List.of(new Block("Current time: " + clockLabel, false), new Block(POLICY, true)), question);
    }

    static MessageCreateParams stampLast(String clockLabel, String question) {
        return request(List.of(new Block(POLICY, true), new Block("Current time: " + clockLabel, false)), question);
    }

    /** Move the clock, send the request through the SDK to the simulated server and read the usage back. */
    static Usage send(CacheSim sim, MessageCreateParams body, int wait) {
        sim.clock += wait;
        return Scripted.client(sim).client().messages().create(body).usage();
    }

    static Usage run(CacheSim sim, String label, MessageCreateParams body, int wait) {
        Usage usage = send(sim, body, wait);
        System.out.println(String.format("%-34s write %4d  read %4d  fresh %4d", label, usage.cacheCreationInputTokens().get(), usage.cacheReadInputTokens().get(), usage.inputTokens()));
        return usage;
    }

    public static void main(String[] args) {
        CacheSim sim = new CacheSim();
        run(sim, "1 stable system, first call", stable("Can I return a lamp?"), 0);
        run(sim, "2 same system, new question", stable("Can I return a chair?"), 60);
        run(sim, "3 six minutes of silence", stable("Can I return a desk?"), 360);
        sim = new CacheSim();
        run(sim, "4 timestamp first, 10:01", stampFirst("10:01", "Can I return a lamp?"), 0);
        run(sim, "5 timestamp first, 10:02", stampFirst("10:02", "Can I return a lamp?"), 60);
        run(sim, "6 timestamp last, 10:03", stampLast("10:03", "Can I return a lamp?"), 60);
        run(sim, "7 timestamp last, 10:04", stampLast("10:04", "Can I return a lamp?"), 60);
    }
}
