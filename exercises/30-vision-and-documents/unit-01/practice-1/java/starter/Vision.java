import java.util.List;
import java.util.Map;

/** Planning a request that carries images and PDFs. See ../../statement.md. Items, blocks and results are JSON-like maps. */
final class Vision {
    private Vision() {}

    static final int MIB = 1024 * 1024;
    static final List<String> IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    /** Given: model id to {tier, context window in tokens, input price in dollars per million tokens}. */
    static final Map<String, Object[]> MODELS = Map.of(
            "claude-fable-5-1", new Object[] {"high", 1_000_000, 10.0},
            "claude-opus-5-5", new Object[] {"high", 1_000_000, 4.0},
            "claude-sonnet-5-5", new Object[] {"high", 1_000_000, 2.0},
            "claude-haiku-4-5", new Object[] {"standard", 200_000, 1.0},
            "claude-haiku-4-5-20251001", new Object[] {"standard", 200_000, 1.0});
    /** Given: tier to {longest edge in pixels, visual token budget}. */
    static final Map<String, int[]> TIERS = Map.of("standard", new int[] {1568, 1568}, "high", new int[] {2576, 4784});

    static int visualTokens(int width, int height) {
        // TODO: the visual tokens of an image of this size (one per 28 x 28 pixel patch, edges rounded up).
        return 0;
    }

    /** The {width, height} the model sees: the largest aspect-preserving size within the tier's edge and token limits. */
    static int[] resizedSize(int width, int height, String tier) {
        // TODO
        return null;
    }

    static double imageCostUsd(String model, int tokens) {
        // TODO: the input cost in dollars of that many tokens, rounded to 6 decimals.
        return 0;
    }

    static double[] toOriginalCoordinates(double x, double y, int width, int height, String model) {
        // TODO: a point Claude returned for the image it saw, as a point on the original width x height image.
        return null;
    }

    static Map<String, Object> planRequest(String model, List<Map<String, Object>> items, String question) {
        return planRequest(model, items, question, false, "api");
    }

    static Map<String, Object> planRequest(String model, List<Map<String, Object>> items, String question, boolean exact, String platform) {
        // TODO: validate the items and build the user content. See the statement for the rules and their order.
        return null;
    }
}
