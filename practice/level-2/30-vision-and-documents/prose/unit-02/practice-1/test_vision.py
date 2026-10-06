import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from vision import MIB, RequestError, image_cost_usd, plan_request, resized_size, to_original_coordinates, visual_tokens

OPUS, HAIKU = "claude-opus-5-5", "claude-haiku-4-5"


def img(name, w, h, media_type="image/png", size=1000, source="base64", value="AAAA"):
    return {"kind": "image", "name": name, "media_type": media_type, "width": w, "height": h, "size": size, "source": source, "value": value}


def pdf(name, pages, size=1000, source="base64", value="JVBER"):
    return {"kind": "pdf", "name": name, "media_type": "application/pdf", "pages": pages, "size": size, "source": source, "value": value}


def plan(*args, **kwargs):
    return plan_request(*args, **kwargs) or {}


def failure_of(fn):
    """The RequestError field fn raises, 'crash' for another exception, None when it returns."""
    try:
        fn()
    except RequestError as err:
        return err.field
    except Exception:  # noqa: BLE001
        return "crash"
    return None


def types(content):
    return [b["type"] for b in content]


def test_m1_images_come_first_with_labels_and_the_question_last():
    result = plan(OPUS, [img("chart", 1000, 1000), img("photo", 200, 200, "image/jpeg", source="url", value="https://example.invalid/p.jpg")], "What changed?")
    assert result.get("content") == [
        {"type": "text", "text": "Image 1:"}, {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": "AAAA"}},
        {"type": "text", "text": "Image 2:"}, {"type": "image", "source": {"type": "url", "url": "https://example.invalid/p.jpg"}},
        {"type": "text", "text": "What changed?"}]
    assert (result.get("image_tokens"), result.get("resized"), result.get("pdf_pages")) == (1296 + 64, [], 0)
    one = plan(OPUS, [img("only", 200, 200, source="file", value="file_abc")], "Describe it.")
    assert one.get("content") == [{"type": "image", "source": {"type": "file", "file_id": "file_abc"}}, {"type": "text", "text": "Describe it."}]


def test_e1_the_token_cost_follows_the_models_resolution_tier():
    assert visual_tokens(1000, 1000) == 1296 and visual_tokens(200, 200) == 64
    assert resized_size(1920, 1080, "standard") == (1456, 819)
    assert resized_size(1080, 1920, "standard") == (819, 1456)
    assert resized_size(1075, 1520, "standard") == (924, 1307)
    assert resized_size(3840, 2160, "high") == (2576, 1449) and resized_size(1920, 1080, "high") == (1920, 1080)
    full = plan(HAIKU, [img("shot", 1920, 1080)], "q")
    assert (full.get("image_tokens"), full.get("resized")) == (1560, ["shot"])
    wide = plan(OPUS, [img("shot", 1920, 1080)], "q")
    assert (wide.get("image_tokens"), wide.get("resized")) == (2691, [])
    assert plan(HAIKU, [img("a", 3840, 2160)], "q").get("image_tokens") == 1560
    assert plan(OPUS, [img("a", 3840, 2160)], "q").get("image_tokens") == 4784
    assert plan(HAIKU, [img("scan", 1075, 1520)], "q").get("image_tokens") == 1551
    assert plan(OPUS, [img("scan", 1075, 1520)], "q").get("image_tokens") == 2145


def test_e2_formats_dimensions_sizes_and_counts_are_checked_before_any_call():
    ok = img("ok", 100, 100)
    assert failure_of(lambda: plan_request(OPUS, [ok, img("b", 100, 100, "image/bmp")], "q")) == "items[1].media_type"
    assert failure_of(lambda: plan_request(OPUS, [img("a", 8001, 100)], "q")) == "items[0].dimensions"
    assert failure_of(lambda: plan_request(OPUS, [img("a", 8000, 8000)], "q")) is None
    assert failure_of(lambda: plan_request(OPUS, [img("a", 100, 100, size=10 * MIB + 1)], "q")) == "items[0].size"
    assert failure_of(lambda: plan_request(OPUS, [img("a", 100, 100, size=10 * MIB)], "q")) is None
    many = [img(f"i{n}", 100, 100) for n in range(101)]
    assert failure_of(lambda: plan_request(HAIKU, many, "q")) == "items"
    assert failure_of(lambda: plan_request(OPUS, many, "q")) is None
    assert failure_of(lambda: plan_request(OPUS, [img(f"i{n}", 100, 100) for n in range(601)], "q")) == "items"
    assert failure_of(lambda: plan_request("claude-unknown-1", [ok], "q")) == "model"
    assert failure_of(lambda: plan_request(OPUS, [ok], "  ")) == "question"


def test_e3_more_than_twenty_images_lower_the_side_limit_to_2000_pixels():
    twenty = [img(f"i{n}", 100, 100) for n in range(20)]
    big = img("big", 2500, 100)
    assert failure_of(lambda: plan_request(OPUS, twenty[:19] + [big], "q")) is None
    four = twenty[:4] + [big] + twenty[5:] + [img("extra", 100, 100)]
    assert failure_of(lambda: plan_request(OPUS, four, "q")) == "items[4].dimensions"
    assert failure_of(lambda: plan_request(OPUS, twenty[:19] + [img("edge", 2000, 100), img("edge2", 100, 2000)], "q")) is None
    with_pdf = twenty[:19] + [big, pdf("doc", 3)]
    assert failure_of(lambda: plan_request(OPUS, with_pdf, "q")) is None
    assert failure_of(lambda: plan_request(OPUS, [pdf("doc", 3)] + twenty[:19] + [img("w", 2500, 100)], "q", platform="bedrock")) == "items[20].dimensions"


def test_e4_an_image_whose_size_matters_is_rejected_instead_of_resized():
    exact = plan(OPUS, [img("shot", 1000, 1000)], "Where is the button?", exact=True)
    assert exact.get("content", [{}])[0].get("transformations") == {"oversized_image": "error"}
    assert "transformations" not in plan(OPUS, [img("shot", 1000, 1000)], "q").get("content", [{}])[0]
    assert failure_of(lambda: plan_request(HAIKU, [img("a", 100, 100), img("shot", 1920, 1080)], "q", exact=True)) == "items[1].dimensions"
    assert failure_of(lambda: plan_request(OPUS, [img("shot", 1920, 1080)], "q", exact=True)) is None
    quiet = plan(HAIKU, [img("shot", 1920, 1080)], "q")
    assert quiet.get("resized") == ["shot"]


def test_e5_pdfs_become_document_blocks_in_order_and_are_limited_by_pages():
    result = plan(OPUS, [pdf("report", 12), img("logo", 200, 200)], "Summarise.")
    assert types(result.get("content", [])) == ["document", "image", "text"]
    assert result["content"][0] == {"type": "document", "source": {"type": "base64", "media_type": "application/pdf", "data": "JVBER"}}
    assert result.get("pdf_pages") == 12
    link = plan(OPUS, [pdf("report", 3, source="url", value="https://example.invalid/r.pdf")], "q")
    assert link.get("content", [{}])[0] == {"type": "document", "source": {"type": "url", "url": "https://example.invalid/r.pdf"}}
    assert failure_of(lambda: plan_request(OPUS, [pdf("a", 300), pdf("b", 301)], "q")) == "items"
    assert failure_of(lambda: plan_request(OPUS, [pdf("a", 300), pdf("b", 300)], "q")) is None
    assert failure_of(lambda: plan_request(HAIKU, [pdf("a", 101)], "q")) == "items"
    assert failure_of(lambda: plan_request(HAIKU, [pdf("a", 100)], "q")) is None
    bad = pdf("a", 3)
    bad["media_type"] = "text/plain"
    assert failure_of(lambda: plan_request(OPUS, [pdf("ok", 1), bad], "q")) == "items[1].media_type"
    assert failure_of(lambda: plan_request(OPUS, [pdf("a", 5, size=20 * MIB), pdf("b", 5, size=13 * MIB)], "q")) == "items"


def test_e6_bedrock_and_vertex_take_only_base64_sources_and_smaller_images():
    link = img("a", 100, 100, source="url", value="https://example.invalid/a.png")
    assert failure_of(lambda: plan_request(OPUS, [link], "q")) is None
    assert failure_of(lambda: plan_request(OPUS, [img("ok", 100, 100), link], "q", platform="bedrock")) == "items[1].source"
    assert failure_of(lambda: plan_request(OPUS, [img("f", 100, 100, source="file", value="file_1")], "q", platform="vertex")) == "items[0].source"
    six = img("six", 100, 100, size=6 * MIB)
    assert failure_of(lambda: plan_request(OPUS, [img("ok", 100, 100), six], "q")) is None
    assert failure_of(lambda: plan_request(OPUS, [img("ok", 100, 100), six], "q", platform="bedrock")) == "items[1].size"
    assert failure_of(lambda: plan_request(OPUS, [img("five", 100, 100, size=5 * MIB)], "q", platform="vertex")) is None


def test_e7_coordinates_map_back_to_the_original_and_cost_follows_the_price():
    assert to_original_coordinates(462, 653.5, 1075, 1520, HAIKU) == (537.5, 760.0)
    assert to_original_coordinates(462, 653.5, 1075, 1520, OPUS) == (462.0, 653.5)
    assert to_original_coordinates(2000, -5, 1075, 1520, HAIKU) == (1075.0, 0.0)
    assert to_original_coordinates(500, 400, 1000, 1000, "claude-haiku-4-5-20251001") == (500.0, 400.0)
    assert image_cost_usd(OPUS, 1296) == 0.005184 and image_cost_usd(HAIKU, 1296) == 0.001296
    assert image_cost_usd("claude-fable-5-1", 1000) == 0.01 and image_cost_usd("claude-sonnet-5-5", 4784) == 0.009568
