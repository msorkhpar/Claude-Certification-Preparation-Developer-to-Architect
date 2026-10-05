# Images: sources, limits, cost and the picture Claude actually sees

**Level:** Developer · **Module 30:** Vision and documents · **Page 1 of 2**
**Exams:** DV1

**After this page you can** send an image to Claude from base64, a URL or the Files API, keep a request inside the documented limits, estimate what an image costs, and map a coordinate that Claude returns back onto your original picture.

Checked against the Claude API documentation (Vision, and Coordinates and bounding boxes) on 2026-10-03, with the example and the practice run offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The replies in the example are hand-written bodies in the shape of the Messages API, labelled illustrative. Prices are the ones in `docs/VERSIONS.md` for Claude Sonnet 5.5, read on 2026-10-02.

## Why it matters

Images arrive in applications as receipts, screenshots, charts, scanned forms and photos of a whiteboard. The model reading them is the easy part. The work is in the request: how the image is sent, how many a request may carry, what each one costs, and what size the model really saw, because every box and point it returns is in that size and not in yours. The exam asks about limits and costs, and about the errors that come from getting them wrong.

## The idea

### Three ways to send an image

On the API, an image is an `image` content block with one of three source types: a base64 string in the request body, a URL, or a `file_id` from the Files API. On Amazon Bedrock and Google Cloud, "only base64-encoded sources are currently available." Claude supports JPEG, PNG, GIF and WebP; "Animations are unsupported, and only the first frame is used."

Put the images before the text. The documentation says Claude "works best when images come before text", and it still performs well when they come after. When a request has several images, introduce each with a short label such as `Image 1:` so that the question and later turns can refer to them by name. In a multi-turn conversation Claude keeps every image from earlier turns, so a later turn needs no copy of the earlier ones.

Base64 has a cost that grows with the conversation: "each request resends the full conversation history", so every base64 image travels again on every turn. Upload an image once to the Files API and refer to it by id, and the payload stays small "regardless of how many images accumulate in the conversation history." One caution from the Files API page, repeated on the next page: files are visible to the whole workspace, so a multi-tenant application needs a workspace per tenant.

<!-- example: m30-image-blocks tabs: python,typescript,java,kotlin -->
```python
"""Image and document blocks: three kinds of source, a labelled comparison, the resize rule, the cost and the way back.

The model reply is illustrative, a hand-written body in the shape of the Messages API (claude-sonnet-5-5), not a capture. The resize
rule is the reference implementation of the "Coordinates and bounding boxes" page of the Claude documentation, checked on 2026-10-03.
"""
import logging
import json
import math

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
TIERS = {"standard": (1568, 1568), "high": (2576, 4784)}  # tier -> (longest edge in pixels, visual token budget)
PRICE = 2.0  # dollars per million input tokens for claude-sonnet-5-5 (docs/VERSIONS.md)


def visual_tokens(width, height):
    return math.ceil(width / 28) * math.ceil(height / 28)


def padded(width, height):
    """Claude pads every image up to the next multiple of 28 on the bottom and the right; the padding holds no content."""
    return (math.ceil(width / 28) * 28, math.ceil(height / 28) * 28)


def resized_size(width, height, tier):
    """The size Claude sees: the largest aspect-preserving size within the tier's edge limit and token budget."""
    max_edge, max_tokens = TIERS[tier]

    def fits(w, h):
        return max(padded(w, h)) <= max_edge and visual_tokens(w, h) <= max_tokens

    if fits(width, height):
        return (width, height)
    if height > width:
        h, w = resized_size(height, width, tier)
        return (w, h)
    ratio = width / height
    for long_edge in range(width - 1, 0, -1):
        short = max(round(long_edge / ratio), 1)
        if fits(long_edge, short):
            return (long_edge, short)
    return (1, 1)


def image_block(kind, value, media_type="image/png"):
    """An image block with one of the three sources of the Messages API: base64, url or file (a Files API id)."""
    source = {"base64": {"type": "base64", "media_type": media_type, "data": value}, "url": {"type": "url", "url": value}, "file": {"type": "file", "file_id": value}}[kind]
    return {"type": "image", "source": source}


def document_block(kind, value, title=None):
    source = {"base64": {"type": "base64", "media_type": "application/pdf", "data": value}, "url": {"type": "url", "url": value}, "file": {"type": "file", "file_id": value}}[kind]
    return {"type": "document", "source": source, **({"title": title} if title else {})}


def comparison(images, question):
    """Several images are each introduced by a label, and the question comes last."""
    content = []
    for number, image in enumerate(images, 1):
        content += [{"type": "text", "text": f"Image {number}:"}, image]
    return content + [{"type": "text", "text": question}]


def to_original(x, y, width, height, tier):
    """A point on the picture Claude saw, as a point on the original: divide by the resized size, never by the padded one."""
    rw, rh = resized_size(width, height, tier)
    return (x / rw * width, y / rh * height)


def shape(block):
    if block["type"] == "text":
        return "text"
    return f'{block["type"]}/{block["source"]["type"]}'


def main():
    client, transport = scripted_client(message([text("Image 1 has the larger bars; image 2 matches the table in the PDF.")], model=MODEL))
    content = comparison([image_block("base64", "iVBORw0KGgo="), image_block("url", "https://example.invalid/chart.png"), image_block("file", "file_011CNha8iCJcU1wXNR6q4V8w")],
                         "Compare the images with the table in the document.")
    content.insert(len(content) - 1, document_block("file", "file_011CPMxVD3fHLUhvTqtsQA5w", "Quarterly table"))
    reply = client.messages.create(model=MODEL, max_tokens=300, messages=[{"role": "user", "content": content}])
    sent = transport.requests[0]["messages"][0]["content"]
    print("content order:", ", ".join(shape(b) for b in sent))
    print("labels:", " ".join(b["text"] for b in sent if b["type"] == "text" and b["text"].startswith("Image")))
    print("one image block:", json.dumps(sent[1], separators=(",", ":")))
    print("reply:", reply.content[0].text)
    print()
    print("size         tier       seen        padded      tokens  dollars per 1000 images")
    for width, height in [(200, 200), (1920, 1080), (3840, 2160), (1075, 1520)]:
        for tier in ("standard", "high"):
            seen = resized_size(width, height, tier)
            tokens = visual_tokens(*seen)
            print(f"{f'{width}x{height}':<12} {tier:<10} {f'{seen[0]}x{seen[1]}':<11} {'x'.join(map(str, padded(*seen))):<11} {tokens:>6}  {tokens * PRICE / 1000:.2f}")
    print()
    x, y = to_original(462, 654, 1075, 1520, "standard")
    wrong_x, wrong_y = 462 / padded(*resized_size(1075, 1520, "standard"))[0] * 1075, 654 / padded(*resized_size(1075, 1520, "standard"))[1] * 1520
    print(f"point (462, 654) on the picture Claude saw of a 1075x1520 scan: ({x:.1f}, {y:.1f}) on the original")
    print(f"dividing by the padded size instead gives ({wrong_x:.1f}, {wrong_y:.1f}), {abs(y - wrong_y):.1f} pixels off")


if __name__ == "__main__":
    main()
```
```text
content order: text, image/base64, text, image/url, text, image/file, document/file, text
labels: Image 1: Image 2: Image 3:
one image block: {"type":"image","source":{"type":"base64","media_type":"image/png","data":"iVBORw0KGgo="}}
reply: Image 1 has the larger bars; image 2 matches the table in the PDF.

size         tier       seen        padded      tokens  dollars per 1000 images
200x200      standard   200x200     224x224         64  0.13
200x200      high       200x200     224x224         64  0.13
1920x1080    standard   1456x819    1456x840      1560  3.12
1920x1080    high       1920x1080   1932x1092     2691  5.38
3840x2160    standard   1456x819    1456x840      1560  3.12
3840x2160    high       2576x1449   2576x1456     4784  9.57
1075x1520    standard   924x1307    924x1316      1551  3.10
1075x1520    high       1075x1520   1092x1540     2145  4.29

point (462, 654) on the picture Claude saw of a 1075x1520 scan: (537.5, 760.6) on the original
dividing by the padded size instead gives (537.5, 755.4), 5.2 pixels off
```
```typescript
// Image and document blocks: three kinds of source, a labelled comparison, the resize rule, the cost and the way back.
// The model reply is illustrative, a hand-written body in the shape of the Messages API (claude-sonnet-5-5), not a capture. The resize
// rule is the reference implementation of the "Coordinates and bounding boxes" page of the Claude documentation, checked on 2026-10-03.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("image_blocks");

export const MODEL = "claude-sonnet-5-5";
const TIERS: Record<string, [number, number]> = { standard: [1568, 1568], high: [2576, 4784] }; // tier -> [longest edge in pixels, visual token budget]
const PRICE = 2.0; // dollars per million input tokens for claude-sonnet-5-5 (docs/VERSIONS.md)

export const visualTokens = (width: number, height: number) => Math.ceil(width / 28) * Math.ceil(height / 28);

/** Claude pads every image up to the next multiple of 28 on the bottom and the right; the padding holds no content. */
export const padded = (width: number, height: number): [number, number] => [Math.ceil(width / 28) * 28, Math.ceil(height / 28) * 28];

function roundHalfEven(x: number): number {
  const floor = Math.floor(x);
  if (x - floor !== 0.5) return Math.round(x);
  return floor % 2 === 0 ? floor : floor + 1;
}

/** The size Claude sees: the largest aspect-preserving size within the tier's edge limit and token budget. */
export function resizedSize(width: number, height: number, tier: string): [number, number] {
  const [maxEdge, maxTokens] = TIERS[tier];
  const fits = (w: number, h: number) => Math.max(...padded(w, h)) <= maxEdge && visualTokens(w, h) <= maxTokens;
  if (fits(width, height)) return [width, height];
  if (height > width) {
    const [h, w] = resizedSize(height, width, tier);
    return [w, h];
  }
  const ratio = width / height;
  for (let longEdge = width - 1; longEdge > 0; longEdge--) {
    const short = Math.max(roundHalfEven(longEdge / ratio), 1);
    if (fits(longEdge, short)) return [longEdge, short];
  }
  return [1, 1];
}

/** An image block with one of the three sources of the Messages API: base64, url or file (a Files API id). */
export function imageBlock(kind: "base64" | "url" | "file", value: string, mediaType = "image/png") {
  const source = { base64: { type: "base64", media_type: mediaType, data: value }, url: { type: "url", url: value }, file: { type: "file", file_id: value } }[kind];
  return { type: "image", source };
}

export function documentBlock(kind: "base64" | "url" | "file", value: string, title?: string) {
  const source = { base64: { type: "base64", media_type: "application/pdf", data: value }, url: { type: "url", url: value }, file: { type: "file", file_id: value } }[kind];
  return { type: "document", source, ...(title ? { title } : {}) };
}

/** Several images are each introduced by a label, and the question comes last. */
export function comparison(images: any[], question: string): any[] {
  const content: any[] = [];
  images.forEach((image, i) => content.push({ type: "text", text: `Image ${i + 1}:` }, image));
  return [...content, { type: "text", text: question }];
}

/** A point on the picture Claude saw, as a point on the original: divide by the resized size, never by the padded one. */
export function toOriginal(x: number, y: number, width: number, height: number, tier: string): [number, number] {
  const [rw, rh] = resizedSize(width, height, tier);
  return [(x / rw) * width, (y / rh) * height];
}

export const shape = (block: any) => (block.type === "text" ? "text" : `${block.type}/${block.source.type}`);

async function main() {
  const fake = scriptedFetch([{ body: message([text("Image 1 has the larger bars; image 2 matches the table in the PDF.")], "end_turn", { input_tokens: 1, output_tokens: 1 }, MODEL) }]);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  const content = comparison([imageBlock("base64", "iVBORw0KGgo="), imageBlock("url", "https://example.invalid/chart.png"), imageBlock("file", "file_011CNha8iCJcU1wXNR6q4V8w")],
    "Compare the images with the table in the document.");
  content.splice(content.length - 1, 0, documentBlock("file", "file_011CPMxVD3fHLUhvTqtsQA5w", "Quarterly table"));
  const reply = await client.messages.create({ model: MODEL, max_tokens: 300, messages: [{ role: "user", content }] });
  const sent = fake.seen[0].body.messages[0].content;
  console.log("content order:", sent.map(shape).join(", "));
  console.log("labels:", sent.filter((b: any) => b.type === "text" && b.text.startsWith("Image")).map((b: any) => b.text).join(" "));
  console.log("one image block:", JSON.stringify(sent[1]));
  console.log("reply:", (reply.content[0] as any).text);
  console.log();
  console.log("size         tier       seen        padded      tokens  dollars per 1000 images");
  for (const [width, height] of [[200, 200], [1920, 1080], [3840, 2160], [1075, 1520]]) {
    for (const tier of ["standard", "high"]) {
      const seen = resizedSize(width, height, tier);
      const tokens = visualTokens(...seen);
      console.log(`${`${width}x${height}`.padEnd(12)} ${tier.padEnd(10)} ${`${seen[0]}x${seen[1]}`.padEnd(11)} ${padded(...seen).join("x").padEnd(11)} ${String(tokens).padStart(6)}  ${((tokens * PRICE) / 1000).toFixed(2)}`);
    }
  }
  console.log();
  const [x, y] = toOriginal(462, 654, 1075, 1520, "standard");
  const [pw, ph] = padded(...resizedSize(1075, 1520, "standard"));
  const [wx, wy] = [(462 / pw) * 1075, (654 / ph) * 1520];
  console.log(`point (462, 654) on the picture Claude saw of a 1075x1520 scan: (${x.toFixed(1)}, ${y.toFixed(1)}) on the original`);
  console.log(`dividing by the padded size instead gives (${wx.toFixed(1)}, ${wy.toFixed(1)}), ${Math.abs(y - wy).toFixed(1)} pixels off`);
}

if (import.meta.main) await main();
```
```text
content order: text, image/base64, text, image/url, text, image/file, document/file, text
labels: Image 1: Image 2: Image 3:
one image block: {"type":"image","source":{"type":"base64","media_type":"image/png","data":"iVBORw0KGgo="}}
reply: Image 1 has the larger bars; image 2 matches the table in the PDF.

size         tier       seen        padded      tokens  dollars per 1000 images
200x200      standard   200x200     224x224         64  0.13
200x200      high       200x200     224x224         64  0.13
1920x1080    standard   1456x819    1456x840      1560  3.12
1920x1080    high       1920x1080   1932x1092     2691  5.38
3840x2160    standard   1456x819    1456x840      1560  3.12
3840x2160    high       2576x1449   2576x1456     4784  9.57
1075x1520    standard   924x1307    924x1316      1551  3.10
1075x1520    high       1075x1520   1092x1540     2145  4.29

point (462, 654) on the picture Claude saw of a 1075x1520 scan: (537.5, 760.6) on the original
dividing by the padded size instead gives (537.5, 755.4), 5.2 pixels off
```
```java
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
    private static final System.Logger LOG = System.getLogger(ImageBlocks.class.getName());
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
```
```text
content order: text, image/base64, text, image/url, text, image/file, document/file, text
labels: Image 1: Image 2: Image 3:
one image block: {"type":"image","source":{"type":"base64","media_type":"image/png","data":"iVBORw0KGgo="}}
reply: Image 1 has the larger bars; image 2 matches the table in the PDF.

size         tier       seen        padded      tokens  dollars per 1000 images
200x200      standard   200x200     224x224         64  0.13
200x200      high       200x200     224x224         64  0.13
1920x1080    standard   1456x819    1456x840      1560  3.12
1920x1080    high       1920x1080   1932x1092     2691  5.38
3840x2160    standard   1456x819    1456x840      1560  3.12
3840x2160    high       2576x1449   2576x1456     4784  9.57
1075x1520    standard   924x1307    924x1316      1551  3.10
1075x1520    high       1075x1520   1092x1540     2145  4.29

point (462, 654) on the picture Claude saw of a 1075x1520 scan: (537.5, 760.6) on the original
dividing by the padded size instead gives (537.5, 755.4), 5.2 pixels off
```
```kotlin
import com.fasterxml.jackson.databind.JsonNode
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.Base64PdfSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.DocumentBlockParam
import com.anthropic.models.messages.FileDocumentSource
import com.anthropic.models.messages.FileImageSource
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.UrlImageSource
import com.anthropic.models.messages.UrlPdfSource
import harness.Scripted
import harness.Scripted.message
import harness.Scripted.text
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val log = System.getLogger("image_blocks")

/**
 * Image and document blocks: three kinds of source, a labelled comparison, the resize rule, the cost and the way back.
 *
 * The model reply is illustrative, a hand-written body in the shape of the Messages API (claude-sonnet-5-5), not a capture. The resize
 * rule is the reference implementation of the "Coordinates and bounding boxes" page of the Claude documentation, checked on 2026-10-03.
 */
const val MODEL = "claude-sonnet-5-5"

/** tier to (longest edge in pixels, visual token budget) */
val TIERS = mapOf("standard" to (1568 to 1568), "high" to (2576 to 4784))
const val PRICE = 2.0 // dollars per million input tokens for claude-sonnet-5-5 (docs/VERSIONS.md)

private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

fun visualTokens(width: Int, height: Int) = ceilDiv(width, 28) * ceilDiv(height, 28)

/** Claude pads every image up to the next multiple of 28 on the bottom and the right; the padding holds no content. */
fun padded(width: Int, height: Int) = ceilDiv(width, 28) * 28 to ceilDiv(height, 28) * 28

/** The size Claude sees: the largest aspect-preserving size within the tier's edge limit and token budget. */
fun resizedSize(width: Int, height: Int, tier: String): Pair<Int, Int> {
    val (maxEdge, maxTokens) = TIERS.getValue(tier)
    fun fits(w: Int, h: Int) = padded(w, h).toList().max() <= maxEdge && visualTokens(w, h) <= maxTokens
    if (fits(width, height)) return width to height
    if (height > width) return resizedSize(height, width, tier).let { (h, w) -> w to h }
    val ratio = width.toDouble() / height
    for (longEdge in width - 1 downTo 1) {
        val short = max(Math.rint(longEdge / ratio).toInt(), 1) // rint rounds halves to even, as Python's round does
        if (fits(longEdge, short)) return longEdge to short
    }
    return 1 to 1
}

/** An image block with one of the three sources of the Messages API: base64, url or file (a Files API id). */
fun imageBlock(kind: String, value: String, mediaType: String = "image/png"): ContentBlockParam {
    val source = when (kind) {
        "base64" -> ImageBlockParam.Source.ofBase64(Base64ImageSource.builder().data(value).mediaType(Base64ImageSource.MediaType.of(mediaType)).build())
        "url" -> ImageBlockParam.Source.ofUrl(UrlImageSource.builder().url(value).build())
        "file" -> ImageBlockParam.Source.ofFile(FileImageSource.builder().fileId(value).build())
        else -> throw IllegalArgumentException(kind)
    }
    return ContentBlockParam.ofImage(ImageBlockParam.builder().source(source).build())
}

fun documentBlock(kind: String, value: String, title: String? = null): ContentBlockParam {
    val source = when (kind) {
        "base64" -> DocumentBlockParam.Source.ofBase64(Base64PdfSource.builder().data(value).build())
        "url" -> DocumentBlockParam.Source.ofUrl(UrlPdfSource.builder().url(value).build())
        "file" -> DocumentBlockParam.Source.ofFile(FileDocumentSource.builder().fileId(value).build())
        else -> throw IllegalArgumentException(kind)
    }
    return ContentBlockParam.ofDocument(DocumentBlockParam.builder().source(source).apply { title?.let { title(it) } }.build())
}

/** Several images are each introduced by a label, and the question comes last. */
fun comparison(images: List<ContentBlockParam>, question: String): MutableList<ContentBlockParam> {
    val content = mutableListOf<ContentBlockParam>()
    for ((i, image) in images.withIndex()) content += listOf(ContentBlockParam.ofText("Image ${i + 1}:"), image)
    content += ContentBlockParam.ofText(question)
    return content
}

/** A point on the picture Claude saw, as a point on the original: divide by the resized size, never by the padded one. */
fun toOriginal(x: Int, y: Int, width: Int, height: Int, tier: String): Pair<Double, Double> {
    val (rw, rh) = resizedSize(width, height, tier)
    return x.toDouble() / rw * width to y.toDouble() / rh * height
}

fun shape(block: ContentBlockParam): String = when {
    block.isText() -> "text"
    block.isImage() -> block.asImage().source().let { "image/" + if (it.isBase64()) "base64" else if (it.isUrl()) "url" else "file" }
    else -> block.asDocument().source().let { "document/" + if (it.isBase64()) "base64" else if (it.isUrl()) "url" else if (it.isFile()) "file" else "other" }
}

private fun size(wh: Pair<Int, Int>) = "${wh.first}x${wh.second}"

/** The SDK's JSON mapper sorts keys alphabetically; the block is shown with the keys in the order the API documents them: type first. */
fun inWireOrder(node: JsonNode): String {
    val order = listOf("type", "source", "media_type", "data")
    if (!node.isObject) return node.toString()
    val keys = node.fieldNames().asSequence().sortedWith(compareBy({ order.indexOf(it).let { i -> if (i < 0) order.size else i } }, { it }))
    return keys.joinToString(",", "{", "}") { "\"$it\":${inWireOrder(node[it])}" }
}

fun main() {
    val rig = Scripted.client(message(listOf(text("Image 1 has the larger bars; image 2 matches the table in the PDF."))))
    val content = comparison(
        listOf(imageBlock("base64", "iVBORw0KGgo="), imageBlock("url", "https://example.invalid/chart.png"), imageBlock("file", "file_011CNha8iCJcU1wXNR6q4V8w")),
        "Compare the images with the table in the document.",
    )
    content.add(content.size - 1, documentBlock("file", "file_011CPMxVD3fHLUhvTqtsQA5w", "Quarterly table"))
    val reply = rig.client().messages().create(MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(300).addUserMessageOfBlockParams(content).build())
    val sent = rig.http().requests[0].at("/messages/0/content")
    println("content order: ${content.joinToString(", ") { shape(it) }}")
    println("labels: ${content.filter { it.isText() }.map { it.asText().text() }.filter { it.startsWith("Image") }.joinToString(" ")}")
    println("one image block: ${inWireOrder(sent[1])}")
    println("reply: ${reply.content()[0].asText().text()}")
    println()
    println("size         tier       seen        padded      tokens  dollars per 1000 images")
    for ((width, height) in listOf(200 to 200, 1920 to 1080, 3840 to 2160, 1075 to 1520)) {
        for (tier in listOf("standard", "high")) {
            val seen = resizedSize(width, height, tier)
            val tokens = visualTokens(seen.first, seen.second)
            println("%-12s %-10s %-11s %-11s %6d  %.2f".format("${width}x$height", tier, size(seen), size(padded(seen.first, seen.second)), tokens, tokens * PRICE / 1000))
        }
    }
    println()
    val (x, y) = toOriginal(462, 654, 1075, 1520, "standard")
    val (pw, ph) = padded(resizedSize(1075, 1520, "standard").first, resizedSize(1075, 1520, "standard").second)
    val wrongX = 462.0 / pw * 1075
    val wrongY = 654.0 / ph * 1520
    println("point (462, 654) on the picture Claude saw of a 1075x1520 scan: (${"%.1f".format(x)}, ${"%.1f".format(y)}) on the original")
    println("dividing by the padded size instead gives (${"%.1f".format(wrongX)}, ${"%.1f".format(wrongY)}), ${"%.1f".format(abs(y - wrongY))} pixels off")
}
```
```text
content order: text, image/base64, text, image/url, text, image/file, document/file, text
labels: Image 1: Image 2: Image 3:
one image block: {"type":"image","source":{"type":"base64","media_type":"image/png","data":"iVBORw0KGgo="}}
reply: Image 1 has the larger bars; image 2 matches the table in the PDF.

size         tier       seen        padded      tokens  dollars per 1000 images
200x200      standard   200x200     224x224         64  0.13
200x200      high       200x200     224x224         64  0.13
1920x1080    standard   1456x819    1456x840      1560  3.12
1920x1080    high       1920x1080   1932x1092     2691  5.38
3840x2160    standard   1456x819    1456x840      1560  3.12
3840x2160    high       2576x1449   2576x1456     4784  9.57
1075x1520    standard   924x1307    924x1316      1551  3.10
1075x1520    high       1075x1520   1092x1540     2145  4.29

point (462, 654) on the picture Claude saw of a 1075x1520 scan: (537.5, 760.6) on the original
dividing by the padded size instead gives (537.5, 755.4), 5.2 pixels off
```
<!-- /example -->

A reader of Java or Kotlin builds the same blocks with the builders of the Java SDK (the arithmetic in the example is plain integer math); the practice at the end of the module is in all four languages.

### Limits that end in an error

| Limit | Value |
|---|---|
| Images per request, models with a 200k-token context window | 100 |
| Images per request, all other models | 600 |
| Dimensions of one image | 8000 by 8000 pixels |
| Size of one image, API (base64) | 10 MB |
| Size of one image, Amazon Bedrock and Google Cloud (base64) | 5 MB |
| Request size, standard endpoints | 32 MB, lower on some platforms |

The count of 600 can be out of reach: the request size limit "can be reached first", so for many images use the Files API and shrink the images before uploading.

One limit surprises people. When a single request holds more than 20 images, "a stricter per-image dimension limit applies to every image in that request." Every `image` block counts, including images from earlier turns that you resend and images nested in a `tool_result` (the screenshots of the computer use tool, next module). An image over the stricter limit is rejected with an `invalid_request_error` that mentions "many-image requests". The documented way out is to resize each image "so that neither dimension exceeds 2000 px, or keep the request to 20 or fewer image and document blocks."

### Visual tokens and what an image costs

Claude views an image in patches of 28 by 28 pixels, each one a visual token, so an image costs `⌈width / 28⌉ × ⌈height / 28⌉` of them. Each model has a native maximum resolution, a long-edge limit and a visual-token limit, and a larger image is downscaled before processing.

| Resolution tier | Models | Longest edge | Visual tokens |
|---|---|---|---|
| High-resolution | Claude 4.7 and later models | 2576 px | 4784 |
| Standard | All other models | 1568 px | 1568 |

"High-resolution support is automatic on the listed models and requires no beta header or client-side opt-in." The price of an image is its token count times the model's input price: "High-resolution images can use up to roughly three times more visual tokens than the same image on a standard-tier model", so downsample when you do not need the extra fidelity. The example prints the table for a few sizes on both tiers. Two numbers from the documentation's own table are worth remembering: a 1920 by 1080 screenshot costs 1560 tokens on the standard tier and 2691 on the high-resolution tier.

Token counting estimates an image's cost from its size, but "a successful count doesn't mean the image is within the Messages API's request limits". An image can count fine and still be rejected when you send it.

### The size Claude saw, and why coordinates depend on it

Claude finds the largest aspect-preserving size that satisfies both limits: neither side over the edge limit, and the token cost within the budget. For nearly all photos and screenshots "the visual token limit is what determines the final size." The documentation's warning: compute the size with its reference implementation, not by scaling to the edge by hand, because "a 1920×1080 screenshot resizes to 1456×819, not 1568×882."

The token limit can resize an image whose sides are both under the edge limit. An A4 page scanned at 130 DPI is 1075 by 1520 pixels, costs `39 × 55 = 2145` visual tokens, and on the standard tier is resized to 924 by 1307. Claude then pads every image on the bottom and right up to a multiple of 28 (924 by 1316 here). The padding holds no content, and the rule is blunt: "Always normalize or rescale by the resized dimensions, not the padded dimensions."

Three practical consequences follow. Ask for pixel coordinates in your prompt: "Claude does not work well when you ask for normalized coordinates", so normalize in your own code. Pre-resize the image yourself when you can, so that the picture you hold is the picture Claude sees and the coordinates need no conversion. And when you cannot, map a returned point back by dividing by the resized size and multiplying by the original size, after clamping the point into the resized picture, because padding touches only the bottom and right and the origin does not move. A per-image field protects a pipeline against silent drift: setting `"transformations": {"oversized_image": "error"}` on an image block makes the API reject an image that would be resized, with a `400`, instead of resizing it. The practice's `exact` flag models it.

### What Claude reads badly

The limitations list is short and testable. Claude "cannot be used to name people in images and refuses to do so." It may hallucinate on low-quality, rotated or very small images under 200 pixels. Its coordinate and localization outputs "are approximate", and so are its counts of many small objects. It cannot tell whether an image is AI-generated, it does not generate or edit images, and it is "not designed to interpret complex diagnostic scans such as CTs or MRIs." Lossy compression can add artifacts that hurt text; check the images you actually send. Claude does not read image metadata, and uploaded images are not stored beyond the request.

## Traps

1. **Counting images as if the limit were 600 everywhere.** Models with a 200k-token window allow 100, and above 20 images a stricter per-image size applies to the whole request, including screenshots returned by tools.
2. **Dividing a coordinate by the padded size.** The padding sits on the bottom and right and holds nothing; divide by the resized size.
3. **Assuming an image under the edge limit is not resized.** The token budget resizes it too, and on the standard tier that moves every coordinate.
4. **Resending base64 on every turn.** The payload grows with each turn; upload once and refer to the file id, and keep workspace scope in mind.

## Quiz

1. A review service sends thirty scanned pages, each 3000 pixels wide, in a single call, and the API refuses it with an invalid_request_error that cites a stricter limit for crowded requests. Which repair does the page document?
   - **a**: Send the pages through Amazon Bedrock, which has no per-image limit at all
   - **b**: Raise the output limit so that the reply has room to describe all thirty of them
   - **c**: Convert each page to GIF so that the stricter size limit stops counting it
   - **d**: Downscale every file until neither side exceeds 2000 px

2. A scanned A4 page is 1075 by 1520 pixels, both sides under the 1568 edge limit. On a standard-tier model, Claude points at (462, 654) for the signature line, yet the click lands above and to the left of the line on the original. What explains the miss?
   - **a**: The picture it viewed was 924 by 1307, so every returned value needs rescaling by that size
   - **b**: The reply was normalized to a 0 to 1000 grid and was then used as plain pixels
   - **c**: Claude pads every image on the top and left, which moved the origin downward
   - **d**: Pictures under the edge limit are returned untouched, so the model misjudged the line

3. A support agent keeps ten screenshots in a long chat and attaches all of them as base64 on every turn. Latency climbs with each turn. What does the page recommend?
   - **a**: Re-encode them as heavily compressed JPEG until the payload shrinks to a few hundred kilobytes
   - **b**: Move to a model with a larger window, which makes the repeated bytes cost nothing
   - **c**: Upload each one once and refer to it by id, so the payload stays small
   - **d**: Place the screenshots after the text, because that order shortens the transfer

<details>
<summary>Answer key</summary>

1. **d**. The page says to resize each image "so that neither dimension exceeds 2000 px, or keep the request to 20 or fewer image and document blocks." *b* is ruled out because the stricter limit is "a stricter per-image dimension limit", which concerns the size of each file and not the reply. *c* is ruled out because "Animations are unsupported, and only the first frame is used", which changes nothing about the dimension limit. *a* is ruled out because on Amazon Bedrock "only base64-encoded sources are currently available", and the limit still applies to every image.
2. **a**. The page works the example: 1075 by 1520 costs 2145 tokens, so on the standard tier it "is resized to 924 by 1307" and the point is in that picture. *b* is ruled out because "Claude does not work well when you ask for normalized coordinates", and it returns pixel positions. *c* is ruled out because the padding sits "on the bottom and right", so the origin does not move. *d* is ruled out because "The token limit can resize an image whose sides are both under the edge limit."
3. **c**. The page says that an uploaded file keeps the payload small "regardless of how many images accumulate in the conversation history." *a* is ruled out because "Lossy compression can add artifacts that hurt text". *b* is ruled out because the problem is that "each request resends the full conversation history", whatever the window size. *d* is ruled out because the placement advice is only that Claude "works best when images come before text".

</details>
