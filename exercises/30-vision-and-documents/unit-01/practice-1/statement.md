# Practice: planning a request with images and PDFs

An image or a PDF is cheap to attach and easy to get wrong: a format the API refuses, a screenshot that is silently shrunk so that
every coordinate Claude returns is off, a request with more images than the model accepts, a PDF placed after the question. Write the
planner that checks all of it before a request is sent, and builds the user content in the order Claude reads best. Pick your language
folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. The limits and the sizing rule come from
the Claude documentation on vision, coordinates and bounding boxes, and PDF support, read on 2026-10-03; the lesson pages explain
them. Nothing here touches the network: the planner only builds and checks a request.

## The given parts

| Name | Meaning |
|---|---|
| `MODELS` | model id to its resolution tier, its context window and its input price in dollars per million tokens |
| `TIERS` | tier to the longest edge in pixels and the visual token budget |
| `RequestError` | what your code throws, before any call, for a request the API would reject; `field` names the part |
| item | `{"kind": "image" or "pdf", "name", "media_type", "size", "source": "base64", "url" or "file", "value"}`; an image also has `width` and `height` in pixels, a PDF has `pages`. `size` is the payload size in bytes. `value` is the base64 text, the URL or the file id |

## What is already written, and what you write

The starter is a working planner with eight gaps cut out of it. Everything that is plumbing is written and correct: the tables, the
`RequestError`, `resized_size` with its search, and the whole of `plan_request` with its checks in order. Each gap is a small function
with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a
neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of
the file (`log.debug(...)`); a run shows the lines under the failing case. Write them in this order (the other languages use the
camel-case forms, and TypeScript, Java and Kotlin keep the helpers private to the file):

1. `visual_tokens` unlocks `e1` and `e7`: one token per 28 x 28 pixel patch.
2. `image_cost_usd` unlocks `e7`: tokens times the model's price per million, rounded.
3. `to_original_coordinates` unlocks `e7`: clamp into the resized size, then scale onto the original.
4. `_max_count` unlocks `e2` and `e5`: 100 or 600 images or PDF pages, by the context window.
5. `_max_image_size` unlocks `e2` and `e6`: 10 MiB, or 5 MiB on bedrock and vertex.
6. `_many` unlocks `e3`: more than twenty image blocks, PDFs counted on bedrock and vertex.
7. `_image_blocks` unlocks `m1` and `e4`: the `Image n:` label and the image block, with the `transformations` entry when `exact`.
8. `_source` unlocks `m1`, `e5` and `e6`: the base64, url or file source of an item.

About ten lines in all. The list below describes the whole planner; the parts you do not write are there so you can see how your
functions are used.

## What to write

- `visual_tokens(width, height)`: one visual token per 28 by 28 pixel patch, so `ceil(width / 28) * ceil(height / 28)`.
- `resized_size(width, height, tier)`: the size the model sees. An image that fits is unchanged. It fits when each side rounded up to a
  multiple of 28 is at most the tier's edge limit **and** its visual tokens are at most the tier's budget. Otherwise it is scaled,
  keeping its aspect ratio, to the largest size that fits (the long edge is searched downward; the short edge is `long edge / aspect
  ratio` rounded half to even, and at least 1). A portrait image is handled as its landscape twin with the sides swapped back.
- `image_cost_usd(model, tokens)`: `tokens * price / 1,000,000`, rounded to 6 decimals.
- `to_original_coordinates(x, y, width, height, model)`: Claude returned a point for the image it saw, which is the resized image
  (never the padded one). Clamp the point into the resized size, then scale it onto the original `width` by `height` image.
- `plan_request(model, items, question, exact=False, platform="api")` returns `{"content", "image_tokens", "resized", "pdf_pages"}`.
  Check in this order and throw `RequestError` with the field named here, at the first problem:
  1. `model` is not in `MODELS`: field `model`. 2. `question` is empty or only spaces: field `question`.
  3. More images than the model accepts, 100 for a model whose context window is under 1,000,000 tokens and 600 otherwise: field
     `items`. 4. More PDF pages in total than the same limit: field `items`. 5. The `size` of all items together is more than 32 MiB
     (32 * 1,048,576 bytes): field `items`.
  6. Then each item in order, at its index `i`: on `platform` `bedrock` or `vertex` a source other than `base64`: `items[i].source`.
     A PDF whose `media_type` is not `application/pdf`: `items[i].media_type`. An image whose `media_type` is not one of the four in
     `IMAGE_TYPES`: `items[i].media_type`. A side above 8000 pixels: `items[i].dimensions`. A `size` above 10 MiB, or 5 MiB on
     `bedrock` and `vertex`: `items[i].size`. When the request holds **more than 20** image blocks (on `bedrock` and `vertex` the PDFs
     count as well), a side above 2000 pixels: `items[i].dimensions`. When `exact` is true and the image would be resized for this
     model's tier: `items[i].dimensions`.
  - `image_tokens` is the sum of the visual tokens of every image at the size the model sees. `resized` lists the names of the images
    that would be resized (an empty list when none). `pdf_pages` is the total number of PDF pages.
  - `content` keeps the order of `items`, one block per item, and ends with the question as a text block. An image is
    `{"type": "image", "source": ...}` and a PDF is `{"type": "document", "source": ...}`; the source is `{"type": "base64",
    "media_type", "data"}`, `{"type": "url", "url"}` or `{"type": "file", "file_id"}`. When there are two or more images, each one is
    preceded by a text block `Image n:` (n counts the images from 1; PDFs get no label). When `exact` is true each image block also
    carries `"transformations": {"oversized_image": "error"}`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Images come first with labels, and the question comes last |
| `e1` | The token cost follows the model's resolution tier |
| `e2` | Formats, dimensions, sizes and counts are checked before any call |
| `e3` | More than twenty images lower the side limit to 2000 pixels |
| `e4` | An image whose size matters is rejected instead of resized |
| `e5` | PDFs become document blocks in order and are limited by pages |
| `e6` | Bedrock and Vertex take only base64 sources and smaller images |
| `e7` | Coordinates map back to the original, and cost follows the price |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
