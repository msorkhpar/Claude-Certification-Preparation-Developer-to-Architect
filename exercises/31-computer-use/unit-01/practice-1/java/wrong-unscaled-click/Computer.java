import java.util.ArrayList;
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
        return Math.min(1.0, Math.min(1568.0 / Math.max(width, height), Math.sqrt(1_150_000.0 / ((double) width * height))));
    }

    static int[] scaledSize(int width, int height) {
        double scale = scaleFor(width, height);
        return new int[] {(int) (width * scale), (int) (height * scale)};
    }

    /** A point on the (scaled) screenshot as a point on the real screen, clamped into the screen. */
    static int[] toScreen(double x, double y, double scale, Map<String, Object> screen) {
        int sx = (int) Math.min(Math.max(Math.rint(x), 0), num(screen.get("width")) - 1);
        int sy = (int) Math.min(Math.max(Math.rint(y), 0), num(screen.get("height")) - 1);
        return new int[] {sx, sy};
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> elementAt(Map<String, Object> screen, int x, int y) {
        Map<String, Object> hit = null;
        for (Map<String, Object> e : (List<Map<String, Object>>) screen.get("elements")) {
            if (num(e.get("x")) <= x && x < num(e.get("x")) + num(e.get("w")) && num(e.get("y")) <= y && y < num(e.get("y")) + num(e.get("h"))) hit = e;
        }
        return hit;
    }

    private static boolean inside(int[] shot, Object point) {
        return point instanceof List<?> p && p.size() == 2 && p.get(0) instanceof Number a && p.get(1) instanceof Number b
                && a.doubleValue() >= 0 && a.doubleValue() < shot[0] && b.doubleValue() >= 0 && b.doubleValue() < shot[1];
    }

    private static Outcome error(String message) {
        return new Outcome(message, true);
    }

    private static Outcome image(String data) {
        return new Outcome(List.of(map("type", "image", "source", map("type", "base64", "media_type", "image/png", "data", data))), false);
    }

    private static boolean isInt(Object o) {
        return o instanceof Integer || o instanceof Long;
    }

    /** Run one action. Returns the content for the result and whether it failed. */
    @SuppressWarnings("unchecked")
    static Outcome perform(Map<String, Object> screen, String name, Map<String, Object> args, double scale, Function<Map<String, Object>, Boolean> confirm) {
        int[] shot = scaledSize(num(screen.get("width")), num(screen.get("height")));
        if (name.equals("screenshot")) return image(render(screen, shot[0], shot[1]));
        if (name.equals("zoom")) {
            Object r = args.get("region");
            if (!(r instanceof List<?> l && l.size() == 4 && inside(shot, l.subList(0, 2)) && l.get(2) instanceof Number x1 && l.get(3) instanceof Number y1
                    && x1.doubleValue() > 0 && x1.doubleValue() <= shot[0] && y1.doubleValue() > 0 && y1.doubleValue() <= shot[1]
                    && ((Number) l.get(0)).doubleValue() < x1.doubleValue() && ((Number) l.get(1)).doubleValue() < y1.doubleValue())) return error("Invalid zoom region: " + r);
            int[] a = toScreen(((Number) l.get(0)).doubleValue(), ((Number) l.get(1)).doubleValue(), scale, screen);
            int[] b = toScreen(((Number) l.get(2)).doubleValue(), ((Number) l.get(3)).doubleValue(), scale, screen);
            return image(render(screen, b[0] - a[0], b[1] - a[1]));
        }
        if (CLICKS.contains(name)) {
            Object point = args.get("coordinate");
            int sx, sy;
            if (point == null) {
                List<Object> cursor = (List<Object>) screen.get("cursor");
                sx = num(cursor.get(0));
                sy = num(cursor.get(1));
            } else if (!inside(shot, point)) {
                return error("Coordinate " + point + " is outside the screenshot");
            } else {
                List<?> p = (List<?>) point;
                int[] s = toScreen(((Number) p.get(0)).doubleValue(), ((Number) p.get(1)).doubleValue(), scale, screen);
                sx = s[0];
                sy = s[1];
            }
            Map<String, Object> element = elementAt(screen, sx, sy);
            if (element != null && !"none".equals(element.getOrDefault("risk", "none"))) {
                if (confirm == null || !Boolean.TRUE.equals(confirm.apply(map("action", name, "element", element.get("id"), "risk", element.get("risk")))))
                    return error("Declined: " + element.get("id") + " needs a person's confirmation (" + element.get("risk") + ")");
            }
            List<Object> cursor = (List<Object>) screen.get("cursor");
            cursor.set(0, sx);
            cursor.set(1, sy);
            Object id = element == null ? null : element.get("id");
            log(screen).add(java.util.Arrays.asList(name, id));
            return new Outcome("Clicked " + (id == null ? "nothing" : id), false);
        }
        switch (name) {
            case "type" -> {
                if (!(args.get("text") instanceof String text)) return error("type needs a text");
                screen.put("typed", screen.get("typed") + text);
                log(screen).add(java.util.Arrays.asList("type", text));
                return new Outcome("Typed " + text.codePointCount(0, text.length()) + " characters", false);
            }
            case "key" -> {
                Object repeat = args.getOrDefault("repeat", 1);
                Object text = args.get("text");
                if (!(text instanceof String s) || s.isEmpty() || !isInt(repeat) || num(repeat) < 1 || num(repeat) > 100) return error("key needs a text and a repeat from 1 to 100");
                log(screen).add(java.util.Arrays.asList("key", s));
                return new Outcome("Pressed " + s, false);
            }
            case "wait" -> {
                Object d = args.get("duration");
                if (!(d instanceof Number n) || n.doubleValue() < 0 || n.doubleValue() > 300) return error("wait needs a duration from 0 to 300 seconds");
                return new Outcome("Waited " + (isInt(d) ? String.valueOf(n.longValue()) : String.valueOf(n.doubleValue())) + "s", false);
            }
            case "scroll" -> {
                Object direction = args.get("scroll_direction"), amount = args.get("scroll_amount");
                if (!List.of("up", "down", "left", "right").contains(direction) || !isInt(amount) || num(amount) < 1) return error("scroll needs a direction and a positive amount");
                log(screen).add(java.util.Arrays.asList("scroll", direction));
                return new Outcome("Scrolled " + direction + " " + amount, false);
            }
            case "mouse_move" -> {
                if (!inside(shot, args.get("coordinate"))) return error("Coordinate " + args.get("coordinate") + " is outside the screenshot");
                List<?> p = (List<?>) args.get("coordinate");
                int[] s = toScreen(((Number) p.get(0)).doubleValue(), ((Number) p.get(1)).doubleValue(), scale, screen);
                List<Object> cursor = (List<Object>) screen.get("cursor");
                cursor.set(0, s[0]);
                cursor.set(1, s[1]);
                return new Outcome("Moved", false);
            }
            case "cursor_position" -> {
                List<Object> cursor = (List<Object>) screen.get("cursor");
                return new Outcome("X=" + (long) Math.rint(num(cursor.get(0)) * scale) + ",Y=" + (long) Math.rint(num(cursor.get(1)) * scale), false);
            }
            default -> {
                return error("Unknown action: " + name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Object deepCopy(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) out.put((String) e.getKey(), deepCopy(e.getValue()));
            return out;
        }
        if (v instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object o : l) out.add(deepCopy(o));
            return out;
        }
        return v;
    }

    /** A copy of the conversation in which every screenshot except the newest keep is replaced by a text note. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> pruneScreenshots(List<Map<String, Object>> messages, int keep) {
        List<Map<String, Object>> out = (List<Map<String, Object>>) deepCopy(messages);
        List<Object[]> images = new ArrayList<>();
        for (Map<String, Object> m : out) {
            if (!(m.get("content") instanceof List<?> blocks)) continue;
            for (Object b : blocks) {
                Map<String, Object> block = (Map<String, Object>) b;
                if ("tool_result".equals(block.get("type")) && block.get("content") instanceof List<?> inner) {
                    for (int i = 0; i < inner.size(); i++) if ("image".equals(((Map<String, Object>) inner.get(i)).get("type"))) images.add(new Object[] {inner, i});
                }
            }
        }
        int upTo = keep > 0 ? Math.max(images.size() - keep, 0) : images.size();
        for (int k = 0; k < upTo; k++) ((List<Object>) images.get(k)[0]).set((Integer) images.get(k)[1], map("type", "text", "text", "[screenshot removed]"));
        return out;
    }

    static Map<String, Object> runComputerLoop(Function<Map<String, Object>, Map<String, Object>> ask, Map<String, Object> screen) {
        return runComputerLoop(ask, screen, "claude-sonnet-5-5", 10, null);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> runComputerLoop(Function<Map<String, Object>, Map<String, Object>> ask, Map<String, Object> screen, String model, int maxTurns,
            Function<Map<String, Object>, Boolean> confirm) {
        double scale = scaleFor(num(screen.get("width")), num(screen.get("height")));
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(map("role", "user", "content", "Do the task on the screen."));
        for (int turn = 1; turn <= maxTurns; turn++) {
            Map<String, Object> reply = ask.apply(map("model", model, "max_tokens", 4096, "tools", List.of(map("type", TOOLSET)), "messages", deepCopy(messages)));
            List<Map<String, Object>> content = (List<Map<String, Object>>) reply.get("content");
            messages.add(map("role", "assistant", "content", content));
            Object stop = reply.get("stop_reason");
            if ("tool_use".equals(stop)) {
                List<Object> results = new ArrayList<>();
                boolean failed = false;
                for (Map<String, Object> block : content) {
                    if (!"tool_use".equals(block.get("type"))) continue;
                    Map<String, Object> result = map("type", "tool_result", "tool_use_id", block.get("id"));
                    if (block.containsKey("toolset_name")) result.put("toolset_name", block.get("toolset_name"));
                    if (failed) {
                        result.put("content", NOT_EXECUTED);
                        result.put("is_error", true);
                    } else {
                        Outcome o = perform(screen, (String) block.get("name"), (Map<String, Object>) block.get("input"), scale, confirm);
                        result.put("content", o.content());
                        if (o.isError()) {
                            result.put("is_error", true);
                            failed = true;
                        }
                    }
                    results.add(result);
                }
                messages.add(map("role", "user", "content", results));
            } else if ("refusal".equals(stop)) {
                return map("status", "refused", "turns", turn, "messages", messages);
            } else if (!"pause_turn".equals(stop)) {
                return map("status", "end_turn".equals(stop) || "stop_sequence".equals(stop) ? "done" : "truncated", "turns", turn, "messages", messages);
            }
        }
        return map("status", "max_turns", "turns", maxTurns, "messages", messages);
    }
}
