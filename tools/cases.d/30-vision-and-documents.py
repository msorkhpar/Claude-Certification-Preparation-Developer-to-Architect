# Case lists of module 30-vision-and-documents: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/30-vision-and-documents/unit-01/practice-1"] = {
    "name": "vision", "suite": "VisionTest", "langs": ["python", "typescript", "java", "kotlin"], "pyfile": "test_vision.py", "tsfile": "vision.test.ts",
    "cases": [
        ("m1", "main", "images come first with labels and the question last"),
        ("e1", "edge", "the token cost follows the models resolution tier"),
        ("e2", "edge", "formats dimensions sizes and counts are checked before any call"),
        ("e3", "edge", "more than twenty images lower the side limit to 2000 pixels"),
        ("e4", "edge", "an image whose size matters is rejected instead of resized"),
        ("e5", "edge", "pdfs become document blocks in order and are limited by pages"),
        ("e6", "edge", "bedrock and vertex take only base64 sources and smaller images"),
        ("e7", "edge", "coordinates map back to the original and cost follows the price"),
    ],
    "plants": {
        "wrong-edge-only-resize": (["e1"], "sizes an image by the edge limit alone and ignores the visual token budget"),
        "wrong-text-first": (["m1"], "puts the question before the images"),
        "wrong-no-labels": (["m1"], "does not label the images when there are several"),
        "wrong-padded-coordinates": (["e7"], "divides a returned coordinate by the padded height instead of the resized height"),
        "wrong-same-limit-all-models": (["e2"], "allows 600 images for every model, including the 200k-context one"),
        "wrong-many-image-rule": (["e3"], "ignores the 2000 pixel limit that applies above 20 images"),
        "wrong-exact-silent": (["e4"], "lets an oversized image through even when its exact size matters"),
        "wrong-pdf-pages-ignored": (["e5"], "does not limit a request by the number of PDF pages"),
        "wrong-pdfs-after-images": (["e5"], "puts every PDF after the images instead of keeping the order of the items"),
        "wrong-cloud-url-allowed": (["e6"], "accepts url and file sources on bedrock and vertex"),
        "wrong-cloud-image-size": (["e6"], "allows a 10 MiB image on bedrock and vertex, where the limit is 5 MiB"),
        "wrong-no-clamp": (["e7"], "does not clamp a returned point into the resized image"),
    },
}
