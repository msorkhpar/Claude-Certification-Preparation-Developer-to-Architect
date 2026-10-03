import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The loop around the computer use tool, against a toy screen. See ../../statement.md. Screens, messages and replies are JSON-like maps. */
final class Computer {
    private Computer() {}

    static final String TOOLSET = "computer_toolset_20260801";
    static final List<String> CLICKS = List.of("left_click", "right_click", "middle_click", "double_click", "triple_click");
    static final String NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed.";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static int num(Object o) {
        return ((Number) o).intValue();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> log(Map<String, Object> screen) {
        return (List<Object>) screen.get("log");
    }

    /** Given: the screenshot of the toy screen at width x height, as the base64 text of a picture. It changes after every logged action. */
    static String render(Map<String, Object> screen, int width, int height) {
        return "png:" + width + "x" + height + ":" + log(screen).size();
    }

    static double scaleFor(int width, int height) {
        // TODO: the factor that shrinks a screen to what the model may be sent: min(1, 1568 / longest side, sqrt(1,150,000 / pixels)).
        return 0;
    }

    static int[] scaledSize(int width, int height) {
        // TODO: {(int) (width * scale), (int) (height * scale)}.
        return null;
    }

    static int[] toScreen(double x, double y, double scale, Map<String, Object> screen) {
        // TODO: a point on the scaled screenshot as a point on the real screen: divide by the scale, round (half to even), clamp into the screen.
        return null;
    }

    static Outcome perform(Map<String, Object> screen, String name, Map<String, Object> args, double scale, Function<Map<String, Object>, Boolean> confirm) {
        // TODO: run one action on the toy screen and return its Outcome. See the statement for every action.
        return null;
    }

    static List<Map<String, Object>> pruneScreenshots(List<Map<String, Object>> messages, int keep) {
        // TODO: a copy of the conversation in which every screenshot except the newest keep is replaced by a text note.
        return null;
    }

    static Map<String, Object> runComputerLoop(Function<Map<String, Object>, Map<String, Object>> ask, Map<String, Object> screen) {
        return runComputerLoop(ask, screen, "claude-sonnet-5-5", 10, null);
    }

    static Map<String, Object> runComputerLoop(Function<Map<String, Object>, Map<String, Object>> ask, Map<String, Object> screen, String model, int maxTurns,
            Function<Map<String, Object>, Boolean> confirm) {
        // TODO: call the model with the computer toolset, run its actions, send the results back, until it ends or a limit is hit.
        return null;
    }
}
