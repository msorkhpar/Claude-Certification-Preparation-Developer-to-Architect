import static harness.Scripted.message;
import static harness.Scripted.text;

import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.FileDocumentSource;
import com.anthropic.models.messages.FileImageSource;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.UrlImageSource;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.UrlPdfSource;
import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Image and document blocks: three kinds of source, a labelled comparison, the resize rule, the cost and the way back.
 *
 * <p>The model reply is illustrative, a hand-written body in the shape of the Messages API (claude-sonnet-5-5), not a capture. The resize
 * rule is the reference implementation of the "Coordinates and bounding boxes" page of the Claude documentation, checked on 2026-10-03.
 */
public final class ImageBlocks {
    static final String MODEL = "claude-sonnet-5-5";
    /** tier to {longest edge in pixels, visual token budget} */
    static final Map<String, int[]> TIERS = Map.of("standard", new int[] {1568, 1568}, "high", new int[] {2576, 4784});
    static final double PRICE = 2.0; // dollars per million input tokens for claude-sonnet-5-5 (docs/VERSIONS.md)

    static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    static int visualTokens(int width, int height) {
        return ceilDiv(width, 28) * ceilDiv(height, 28);
    }

    /** Claude pads every image up to the next multiple of 28 on the bottom and the right; the padding holds no content. */
    static int[] padded(int width, int height) {
        return new int[] {ceilDiv(width, 28) * 28, ceilDiv(height, 28) * 28};
    }

    private static boolean fits(int w, int h, int maxEdge, int maxTokens) {
        int[] p = padded(w, h);
        return Math.max(p[0], p[1]) <= maxEdge && visualTokens(w, h) <= maxTokens;
    }

    /** The size Claude sees: the largest aspect-preserving size within the tier's edge limit and token budget. */
    static int[] resizedSize(int width, int height, String tier) {
        int maxEdge = TIERS.get(tier)[0], maxTokens = TIERS.get(tier)[1];
        if (fits(width, height, maxEdge, maxTokens)) return new int[] {width, height};
        if (height > width) {
            int[] swapped = resizedSize(height, width, tier);
            return new int[] {swapped[1], swapped[0]};
        }
        double ratio = (double) width / height;
        for (int longEdge = width - 1; longEdge > 0; longEdge--) {
            int shortEdge = Math.max((int) Math.rint(longEdge / ratio), 1); // rint rounds halves to even, as Python's round does
            if (fits(longEdge, shortEdge, maxEdge, maxTokens)) return new int[] {longEdge, shortEdge};
        }
        return new int[] {1, 1};
    }

    /** An image block with one of the three sources of the Messages API: base64, url or file (a Files API id). */
    static ContentBlockParam imageBlock(String kind, String value, String mediaType) {
        ImageBlockParam.Source source = switch (kind) {
            case "base64" -> ImageBlockParam.Source.ofBase64(Base64ImageSource.builder().data(value).mediaType(Base64ImageSource.MediaType.of(mediaType)).build());
            case "url" -> ImageBlockParam.Source.ofUrl(UrlImageSource.builder().url(value).build());
            case "file" -> ImageBlockParam.Source.ofFile(FileImageSource.builder().fileId(value).build());
            default -> throw new IllegalArgumentException(kind);
        };
        return ContentBlockParam.ofImage(ImageBlockParam.builder().source(source).build());
    }

    static ContentBlockParam imageBlock(String kind, String value) {
        return imageBlock(kind, value, "image/png");
    }

    static ContentBlockParam documentBlock(String kind, String value, String title) {
        DocumentBlockParam.Source source = switch (kind) {
            case "base64" -> DocumentBlockParam.Source.ofBase64(Base64PdfSource.builder().data(value).build());
            case "url" -> DocumentBlockParam.Source.ofUrl(UrlPdfSource.builder().url(value).build());
            case "file" -> DocumentBlockParam.Source.ofFile(FileDocumentSource.builder().fileId(value).build());
            default -> throw new IllegalArgumentException(kind);
        };
        DocumentBlockParam.Builder block = DocumentBlockParam.builder().source(source);
        if (title != null) block.title(title);
        return ContentBlockParam.ofDocument(block.build());
    }

    /** Several images are each introduced by a label, and the question comes last. */
    static List<ContentBlockParam> comparison(List<ContentBlockParam> images, String question) {
        List<ContentBlockParam> content = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            content.add(ContentBlockParam.ofText("Image " + (i + 1) + ":"));
            content.add(images.get(i));
        }
        content.add(ContentBlockParam.ofText(question));
        return content;
    }

    /** A point on the picture Claude saw, as a point on the original: divide by the resized size, never by the padded one. */
    static double[] toOriginal(int x, int y, int width, int height, String tier) {
        int[] seen = resizedSize(width, height, tier);
        return new double[] {(double) x / seen[0] * width, (double) y / seen[1] * height};
    }

    static String shape(ContentBlockParam block) {
        if (block.isText()) return "text";
        if (block.isImage()) {
            ImageBlockParam.Source s = block.asImage().source();
            return "image/" + (s.isBase64() ? "base64" : s.isUrl() ? "url" : "file");
        }
        DocumentBlockParam.Source s = block.asDocument().source();
        return "document/" + (s.isBase64() ? "base64" : s.isUrl() ? "url" : s.isFile() ? "file" : "other");
    }

    private static String size(int[] wh) {
        return wh[0] + "x" + wh[1];
    }

    /** The SDK's JSON mapper sorts keys alphabetically; the block is shown with the keys in the order the API documents them: type first. */
    static String inWireOrder(JsonNode node) {
        List<String> order = List.of("type", "source", "media_type", "data");
        if (!node.isObject()) return node.toString();
        List<String> keys = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        keys.sort(java.util.Comparator.comparingInt((String k) -> order.indexOf(k) < 0 ? order.size() : order.indexOf(k)).thenComparing(k -> k));
        return keys.stream().map(k -> "\"" + k + "\":" + inWireOrder(node.get(k))).collect(Collectors.joining(",", "{", "}"));
    }

    public static void main(String[] args) {
        Scripted.Rig rig = Scripted.client(message(List.of(text("Image 1 has the larger bars; image 2 matches the table in the PDF."))));
        List<ContentBlockParam> content = comparison(List.of(imageBlock("base64", "iVBORw0KGgo="), imageBlock("url", "https://example.invalid/chart.png"),
            imageBlock("file", "file_011CNha8iCJcU1wXNR6q4V8w")), "Compare the images with the table in the document.");
        content.add(content.size() - 1, documentBlock("file", "file_011CPMxVD3fHLUhvTqtsQA5w", "Quarterly table"));
        Message reply = rig.client().messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).addUserMessageOfBlockParams(content).build());
        JsonNode sent = rig.http().requests.get(0).at("/messages/0/content");
        System.out.println("content order: " + content.stream().map(ImageBlocks::shape).collect(Collectors.joining(", ")));
        System.out.println("labels: " + content.stream().filter(ContentBlockParam::isText).map(b -> b.asText().text()).filter(t -> t.startsWith("Image")).collect(Collectors.joining(" ")));
        System.out.println("one image block: " + inWireOrder(sent.get(1)));
        System.out.println("reply: " + reply.content().get(0).asText().text());
        System.out.println();
        System.out.println("size         tier       seen        padded      tokens  dollars per 1000 images");
        for (int[] wh : new int[][] {{200, 200}, {1920, 1080}, {3840, 2160}, {1075, 1520}}) {
            for (String tier : List.of("standard", "high")) {
                int[] seen = resizedSize(wh[0], wh[1], tier);
                int tokens = visualTokens(seen[0], seen[1]);
                System.out.println(String.format(Locale.ROOT, "%-12s %-10s %-11s %-11s %6d  %.2f", wh[0] + "x" + wh[1], tier, size(seen), size(padded(seen[0], seen[1])), tokens, tokens * PRICE / 1000));
            }
        }
        System.out.println();
        double[] xy = toOriginal(462, 654, 1075, 1520, "standard");
        int[] p = padded(resizedSize(1075, 1520, "standard")[0], resizedSize(1075, 1520, "standard")[1]);
        double wrongX = 462.0 / p[0] * 1075, wrongY = 654.0 / p[1] * 1520;
        System.out.println(String.format(Locale.ROOT, "point (462, 654) on the picture Claude saw of a 1075x1520 scan: (%.1f, %.1f) on the original", xy[0], xy[1]));
        System.out.println(String.format(Locale.ROOT, "dividing by the padded size instead gives (%.1f, %.1f), %.1f pixels off", wrongX, wrongY, Math.abs(xy[1] - wrongY)));
    }
}
