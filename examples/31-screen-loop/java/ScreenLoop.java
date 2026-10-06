import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ComputerToolset20260801;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.ToolUseBlock;
import harness.Scripted;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The loop around the computer use toolset, on a toy screen: scaling both ways, a batch, a halt after a failure and a confirmation.
 *
 * <p>There is no desktop and no model: the screen is a few rectangles in memory and the replies are hand-written bodies in the shape of the
 * Messages API (claude-sonnet-5-5), illustrative and not captures. The tool entry, the batch rule and the halt text are those of the
 * "Computer use tool" page of the Claude documentation, checked on 2026-10-03.
 */
public final class ScreenLoop {
    private static final System.Logger LOG = System.getLogger(ScreenLoop.class.getName());
    static final String MODEL = "claude-sonnet-5-5";
    static final String HALT = "Not executed: an earlier computer action in this turn failed.";

    /** A rectangle on the toy screen; `risk` is "none" or what a click on it could cost. */
    record Element(String id, int x, int y, int w, int h, String risk) {}

    /** The toy screen: its size, what was typed, a log of the actions performed and the elements on it. */
    static final class Screen {
        final int width, height;
        final StringBuilder typed = new StringBuilder();
        final List<List<String>> log = new ArrayList<>();
        final List<Element> elements = List.of(new Element("search", 800, 100, 900, 60, "none"), new Element("buy", 2000, 1200, 300, 80, "payment"));

        Screen(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    static Screen newScreen() {
        return new Screen(2560, 1440);
    }

    /** The documentation's example limits (1568 px on the long edge, about 1.15 megapixels): small enough for every model. */
    static double scaleFor(int width, int height) {
        return Math.min(1.0, Math.min(1568.0 / Math.max(width, height), Math.sqrt(1_150_000.0 / ((double) width * height))));
    }

    static int[] toScreen(double x, double y, double scale, Screen screen) {
        return new int[] {(int) Math.min(Math.max(Math.rint(x / scale), 0), screen.width - 1), (int) Math.min(Math.max(Math.rint(y / scale), 0), screen.height - 1)};
    }

    static Element elementAt(Screen screen, int x, int y) {
        Element hit = null;
        for (Element e : screen.elements) if (e.x() <= x && x < e.x() + e.w() && e.y() <= y && y < e.y() + e.h()) hit = e;
        return hit;
    }

    static ToolResultBlockParam.Content screenshot(Screen screen, double scale) {
        String size = (int) (screen.width * scale) + "x" + (int) (screen.height * scale);
        String data = Base64.getEncoder().encodeToString((size + ":" + screen.log.size()).getBytes(StandardCharsets.UTF_8));
        ImageBlockParam image = ImageBlockParam.builder().source(Base64ImageSource.builder().data(data).mediaType(Base64ImageSource.MediaType.IMAGE_PNG).build()).build();
        return ToolResultBlockParam.Content.ofBlocks(List.of(ToolResultBlockParam.Content.Block.ofImage(image)));
    }

    /** What one action gave back: text or an image, and whether it failed. */
    record Outcome(ToolResultBlockParam.Content content, boolean isError) {
        static Outcome text(String s, boolean isError) {
            return new Outcome(ToolResultBlockParam.Content.ofString(s), isError);
        }
    }

    /** Run one member of the toolset on the toy screen. */
    static Outcome perform(Screen screen, String name, Map<String, Object> args, double scale, Predicate<Map<String, String>> confirm) {
        switch (name) {
            case "screenshot":
                return new Outcome(screenshot(screen, scale), false);
            case "left_click": {
                List<?> coordinate = (List<?>) args.get("coordinate");
                int[] at = toScreen(((Number) coordinate.get(0)).doubleValue(), ((Number) coordinate.get(1)).doubleValue(), scale, screen);
                Element target = elementAt(screen, at[0], at[1]);
                if (target != null && !target.risk().equals("none") && !(confirm != null && confirm.test(Map.of("action", name, "element", target.id(), "risk", target.risk())))) {
                    return Outcome.text("Declined: " + target.id() + " needs a person's confirmation (" + target.risk() + ")", true);
                }
                screen.log.add(java.util.Arrays.asList("click", target == null ? null : target.id()));
                return Outcome.text("Clicked " + (target == null ? "nothing" : target.id()) + " at " + at[0] + "," + at[1] + " on the screen", false);
            }
            case "type":
                screen.typed.append(args.get("text"));
                screen.log.add(List.of("type", (String) args.get("text")));
                return Outcome.text("Typed " + ((String) args.get("text")).length() + " characters", false);
            case "key":
                screen.log.add(List.of("key", (String) args.get("text")));
                return Outcome.text("Pressed " + args.get("text"), false);
            default:
                return Outcome.text("Unknown action: " + name, true);
        }
    }

    /** How a loop ended. */
    record Result(String status, int turns, String answer) {}

    @SuppressWarnings("unchecked")
    static Result runLoop(AnthropicClient client, Screen screen, Predicate<Map<String, String>> confirm, int maxTurns) {
        double scale = scaleFor(screen.width, screen.height);
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder().role(MessageParam.Role.USER).content("Search for a kettle and buy the first one.").build());
        ToolUnion toolset = ToolUnion.ofComputerToolset20260801(ComputerToolset20260801.builder().build());
        for (int turn = 1; turn <= maxTurns; turn++) {
            Message reply = client.messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(4096).addTool(toolset).messages(messages).build());
            messages.add(reply.toParam());
            if (!reply.stopReason().get().asString().equals("tool_use")) {
                return new Result("done", turn, reply.content().get(reply.content().size() - 1).asText().text());
            }
            List<ContentBlockParam> results = new ArrayList<>();
            boolean failed = false;
            for (ContentBlock b : reply.content()) {
                if (!b.isToolUse()) continue;
                ToolUseBlock block = b.asToolUse();
                Map<String, Object> input = ObjectMappers.jsonMapper().convertValue(block._input(), Map.class);
                Outcome outcome = failed ? Outcome.text(HALT, true) : perform(screen, block.name(), input, scale, confirm);
                failed = outcome.isError();
                String shown = outcome.content().string().orElse("image");
                System.out.println("turn " + turn + ": " + block.name() + " " + json(block._input()) + " -> " + (outcome.isError() ? "ERROR " : "") + shown);
                ToolResultBlockParam.Builder result = ToolResultBlockParam.builder().toolUseId(block.id()).content(outcome.content())
                    .toolsetName(block.toolsetName().orElseThrow());
                if (outcome.isError()) result.isError(true);
                results.add(ContentBlockParam.ofToolResult(result.build()));
            }
            messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(results).build());
        }
        return new Result("max_turns", maxTurns, null);
    }

    private static String json(Object value) {
        try {
            return ObjectMappers.jsonMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Map<String, Object> call(String id, String name, Map<String, Object> input) {
        return map("type", "tool_use", "id", id, "name", name, "toolset_name", "computer", "input", input);
    }

    public static void main(String[] args) {
        Screen screen = newScreen();
        double scale = scaleFor(screen.width, screen.height);
        Element search = screen.elements.get(0), buy = screen.elements.get(1);
        List<Integer> searchAt = List.of((int) Math.rint((search.x() + search.w() / 2.0) * scale), (int) Math.rint((search.y() + search.h() / 2.0) * scale));
        List<Integer> buyAt = List.of((int) Math.rint((buy.x() + buy.w() / 2.0) * scale), (int) Math.rint((buy.y() + buy.h() / 2.0) * scale));
        Scripted.Rig rig = Scripted.client(
            message(List.of(call("toolu_1", "screenshot", map()), call("toolu_2", "left_click", map("coordinate", searchAt)), call("toolu_3", "type", map("text", "kettle")), call("toolu_4", "key", map("text", "Return"))), "tool_use"),
            message(List.of(call("toolu_5", "left_click", map("coordinate", buyAt)), call("toolu_6", "screenshot", map())), "tool_use"),
            message(List.of(text("I did not buy it: the payment button needs your confirmation."))));
        System.out.println(String.format(Locale.ROOT, "screen %dx%d, scale %.4f, the model sees %dx%d", screen.width, screen.height, scale, (int) (screen.width * scale), (int) (screen.height * scale)));
        Result result = runLoop(rig.client(), screen, action -> false, 6);
        System.out.println("result: " + result.status() + " after " + result.turns() + " turns: " + result.answer());
        System.out.println("screen log: " + json(screen.log) + " typed: " + screen.typed);
        System.out.println("tools sent: " + rig.http().requests.get(0).get("tools") + " beta header sent: " + (rig.http().headers.get(0).containsKey("anthropic-beta") ? "True" : "False"));
        List<String> second = new ArrayList<>();
        rig.http().requests.get(2).get("messages").get(rig.http().requests.get(2).get("messages").size() - 1).get("content").forEach(
            r -> second.add("('" + r.get("tool_use_id").asText() + "', '" + r.get("toolset_name").asText() + "', " + (r.path("is_error").asBoolean(false) ? "True" : "False") + ")"));
        System.out.println("second result message: " + second);
    }
}
