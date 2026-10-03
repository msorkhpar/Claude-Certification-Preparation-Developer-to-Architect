from image_blocks import comparison, document_block, image_block, padded, resized_size, shape, to_original, visual_tokens


def test_the_documentation_table_for_two_sizes_on_both_tiers():
    assert resized_size(1920, 1080, "standard") == (1456, 819) and visual_tokens(1456, 819) == 1560
    assert resized_size(1920, 1080, "high") == (1920, 1080) and visual_tokens(1920, 1080) == 2691
    assert resized_size(3840, 2160, "high") == (2576, 1449) and visual_tokens(2576, 1449) == 4784
    assert resized_size(200, 200, "standard") == (200, 200) and visual_tokens(200, 200) == 64


def test_a_scan_under_the_edge_limit_is_still_resized_by_the_token_budget():
    assert visual_tokens(1075, 1520) == 2145
    assert resized_size(1075, 1520, "standard") == (924, 1307) and padded(924, 1307) == (924, 1316)
    assert resized_size(1075, 1520, "high") == (1075, 1520)


def test_the_three_sources_and_the_label_order():
    blocks = [image_block("base64", "AAAA", "image/jpeg"), image_block("url", "https://example.invalid/a.png"), image_block("file", "file_1")]
    content = comparison(blocks, "Which is larger?")
    assert [shape(b) for b in content] == ["text", "image/base64", "text", "image/url", "text", "image/file", "text"]
    assert [b["text"] for b in content if b["type"] == "text"] == ["Image 1:", "Image 2:", "Image 3:", "Which is larger?"]
    assert document_block("file", "file_2", "T") == {"type": "document", "source": {"type": "file", "file_id": "file_2"}, "title": "T"}


def test_a_coordinate_maps_back_by_the_resized_size_and_not_the_padded_one():
    x, y = to_original(462, 654, 1075, 1520, "standard")
    assert (round(x, 1), round(y, 1)) == (537.5, 760.6)
    assert to_original(10, 20, 100, 100, "standard") == (10, 20)
