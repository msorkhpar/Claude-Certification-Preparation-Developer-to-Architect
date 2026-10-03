import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        return Math.ceilDiv(width, 28) * Math.ceilDiv(height, 28);
    }

    private static boolean fits(int w, int h, int maxEdge, int maxTokens) {
        return Math.ceilDiv(w, 28) * 28 <= maxEdge && Math.ceilDiv(h, 28) * 28 <= maxEdge && visualTokens(w, h) <= maxTokens;
    }

    /** The {width, height} the model sees: the largest aspect-preserving size within the tier's edge and token limits. */
    static int[] resizedSize(int width, int height, String tier) {
        int maxEdge = TIERS.get(tier)[0], maxTokens = TIERS.get(tier)[1];
        if (fits(width, height, maxEdge, maxTokens)) return new int[] {width, height};
        if (height > width) {
            int[] r = resizedSize(height, width, tier);
            return new int[] {r[1], r[0]};
        }
        double ratio = (double) width / height;
        for (int longEdge = width - 1; longEdge > 0; longEdge--) {
            int shortEdge = Math.max((int) Math.rint(longEdge / ratio), 1); // half to even, like the API
            if (fits(longEdge, shortEdge, maxEdge, maxTokens)) return new int[] {longEdge, shortEdge};
        }
        return new int[] {1, 1};
    }

    static double imageCostUsd(String model, int tokens) {
        double price = (Double) MODELS.get(model)[2];
        return Math.rint(tokens * price / 1_000_000 * 1e6) / 1e6;
    }

    static double[] toOriginalCoordinates(double x, double y, int width, int height, String model) {
        int[] r = resizedSize(width, height, (String) MODELS.get(model)[0]);
        double cx = Math.min(Math.max(x, 0), r[0]);
        double cy = Math.min(Math.max(y, 0), r[1]);
        return new double[] {cx / r[0] * width, cy / r[1] * height};
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> sourceOf(Map<String, Object> item) {
        String kind = (String) item.get("source");
        Object value = item.get("value");
        if (kind.equals("base64")) return map("type", "base64", "media_type", item.get("media_type"), "data", value);
        return kind.equals("url") ? map("type", "url", "url", value) : map("type", "file", "file_id", value);
    }

    private static int num(Map<String, Object> item, String key) {
        return ((Number) item.get(key)).intValue();
    }

    static Map<String, Object> planRequest(String model, List<Map<String, Object>> items, String question) {
        return planRequest(model, items, question, false, "api");
    }

    static Map<String, Object> planRequest(String model, List<Map<String, Object>> items, String question, boolean exact, String platform) {
        if (!MODELS.containsKey(model)) throw new RequestError("model", "unknown model " + model);
        String tier = (String) MODELS.get(model)[0];
        int context = (Integer) MODELS.get(model)[1];
        if (question == null || question.isBlank()) throw new RequestError("question", "the question must be a non-empty string");
        boolean cloud = platform.equals("bedrock") || platform.equals("vertex");
        int images = 0, pdfs = 0, pages = 0;
        long total = 0;
        for (Map<String, Object> it : items) {
            if (it.get("kind").equals("image")) images++;
            else {
                pdfs++;
                pages += num(it, "pages");
            }
            total += num(it, "size");
        }
        int limit = context < 1_000_000 ? 100 : 600;
        if (images > 600) throw new RequestError("items", "too many images for " + model);
        if (pages > limit) throw new RequestError("items", "too many PDF pages for " + model);
        if (total > 32L * MIB) throw new RequestError("items", "the request would be larger than 32 MiB");
        boolean many = images + (cloud ? pdfs : 0) > 20;
        int tokens = 0;
        List<Object> resized = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> it = items.get(i);
            if (cloud && !it.get("source").equals("base64")) throw new RequestError("items[" + i + "].source", platform + " accepts base64 sources only");
            if (it.get("kind").equals("pdf")) {
                if (!"application/pdf".equals(it.get("media_type"))) throw new RequestError("items[" + i + "].media_type", "a PDF must be application/pdf");
                continue;
            }
            int w = num(it, "width"), h = num(it, "height");
            if (!IMAGE_TYPES.contains(it.get("media_type"))) throw new RequestError("items[" + i + "].media_type", it.get("media_type") + " is not a supported image format");
            if (w > 8000 || h > 8000) throw new RequestError("items[" + i + "].dimensions", "an image may not exceed 8000 x 8000 pixels");
            if (num(it, "size") > (cloud ? 5 : 10) * MIB) throw new RequestError("items[" + i + "].size", "the image is too large");
            if (many && Math.max(w, h) > 2000) throw new RequestError("items[" + i + "].dimensions", "with more than 20 images, no side may exceed 2000 pixels");
            int[] seen = resizedSize(w, h, tier);
            if (seen[0] != w || seen[1] != h) {
                if (exact) throw new RequestError("items[" + i + "].dimensions", "would be resized to " + seen[0] + "x" + seen[1]);
                resized.add(it.get("name"));
            }
            tokens += visualTokens(seen[0], seen[1]);
        }
        List<Object> content = new ArrayList<>();
        int n = 0;
        for (Map<String, Object> it : items) {
            if (it.get("kind").equals("image")) {
                n++;
                if (images > 1) content.add(map("type", "text", "text", "Image " + n + ":"));
                Map<String, Object> block = map("type", "image", "source", sourceOf(it));
                if (exact) block.put("transformations", map("oversized_image", "error"));
                content.add(block);
            } else {
                content.add(map("type", "document", "source", sourceOf(it)));
            }
        }
        content.add(map("type", "text", "text", question));
        return map("content", content, "image_tokens", tokens, "resized", resized, "pdf_pages", pages);
    }
}
