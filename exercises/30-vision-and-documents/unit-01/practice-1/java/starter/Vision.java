import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Planning a request that carries images and PDFs. See ../../statement.md. Items, blocks and results are JSON-like maps. */
final class Vision {
    private static final System.Logger LOG = System.getLogger(Vision.class.getName());
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
        // TODO 1 of 8 (unlocks e1 and e7): the visual tokens of an image of this size.
        // Receives the width and height in pixels. Returns one token per 28 x 28 pixel patch, each side rounded up to whole patches.
        // Example: visualTokens(29, 28) -> 2
        return 0;
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
        // TODO 2 of 8 (unlocks e7): the input cost in dollars of that many tokens for the model.
        // Receives a model id and a token count. Returns tokens * price / 1,000,000 rounded to 6 decimals (Math.rint); the price is
        // ((Double) MODELS.get(model)[2]). Example: imageCostUsd("claude-haiku-4-5", 1000) -> 0.001
        return 0.0;
    }

    static double[] toOriginalCoordinates(double x, double y, int width, int height, String model) {
        int[] r = resizedSize(width, height, (String) MODELS.get(model)[0]);
        // TODO 3 of 8 (unlocks e7): a point Claude returned for the image it saw, as a point on the original width x height image.
        // Clamp x and y into the resized size r[0] x r[1], then scale them onto the original size. Example: a 3000 x 2000 image on a
        // "high" model is seen as 2576 x 1717 (not padded), so the point (1288, 0) becomes about {1500.0, 0.0}.
        return new double[] {x, y};
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> sourceOf(Map<String, Object> item) {
        // TODO 8 of 8 (unlocks m1, e5 and e6): the "source" object of an item.
        // Returns {type: base64, media_type, data} for source "base64", {type: url, url} for "url" and {type: file, file_id} for
        // "file"; the text is item.get("value"). Build it with map(...).
        // Example: source "url", value "https://example.invalid/a.png" -> {type=url, url=https://example.invalid/a.png}
        return new LinkedHashMap<>();
    }

    private static int maxCount(int context) {
        // TODO 4 of 8 (unlocks e2 and e5): how many images, and how many PDF pages, a request may hold.
        // Receives the model's context window in tokens. Returns 100 when it is under 1,000,000 and 600 otherwise.
        // Example: maxCount(200_000) -> 100
        return Integer.MAX_VALUE;
    }

    private static int maxImageSize(boolean cloud) {
        // TODO 5 of 8 (unlocks e2 and e6): the largest image payload in bytes.
        // Receives true on bedrock and vertex. Returns 10 MiB, or 5 MiB when it is true.
        // Example: maxImageSize(true) -> 5 * MIB
        return Integer.MAX_VALUE;
    }

    private static boolean isMany(int images, int pdfs, boolean cloud) {
        // TODO 6 of 8 (unlocks e3): does the request hold more than 20 image blocks?
        // Receives the image count, the PDF count and the cloud flag; on bedrock and vertex the PDFs count as well.
        // Example: isMany(18, 3, true) -> true, isMany(18, 3, false) -> false
        return false;
    }

    private static List<Object> imageBlocks(Map<String, Object> item, int n, int imageCount, boolean exact) {
        // TODO 7 of 8 (unlocks m1 and e4): the content blocks of the n-th image (counting from 1).
        // Returns a text block "Image n:" first when there are two or more images, then the image block {type: image, source:
        // sourceOf(item)}, which also carries transformations: {oversized_image: error} when exact is true (use map(...)).
        // Example: imageBlocks(item, 2, 2, false) -> [{type=text, text=Image 2:}, {type=image, source={...}}]
        return new ArrayList<>();
    }

    private static int num(Map<String, Object> item, String key) {
        return ((Number) item.get(key)).intValue();
    }

    static Map<String, Object> planRequest(String model, List<Map<String, Object>> items, String question) {
        return planRequest(model, items, question, false, "api");
    }

    static Map<String, Object> planRequest(String model, List<Map<String, Object>> items, String question, boolean exact, String platform) {
        LOG.log(System.Logger.Level.DEBUG, "planRequest input: {0}", items);
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
        int limit = maxCount(context);
        if (images > limit) throw new RequestError("items", "too many images for " + model);
        if (pages > limit) throw new RequestError("items", "too many PDF pages for " + model);
        if (total > 32L * MIB) throw new RequestError("items", "the request would be larger than 32 MiB");
        boolean many = isMany(images, pdfs, cloud);
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
            if (num(it, "size") > maxImageSize(cloud)) throw new RequestError("items[" + i + "].size", "the image is too large");
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
                content.addAll(imageBlocks(it, n, images, exact));
            } else {
                content.add(map("type", "document", "source", sourceOf(it)));
            }
        }
        content.add(map("type", "text", "text", question));
        return map("content", content, "image_tokens", tokens, "resized", resized, "pdf_pages", pages);
    }
}
