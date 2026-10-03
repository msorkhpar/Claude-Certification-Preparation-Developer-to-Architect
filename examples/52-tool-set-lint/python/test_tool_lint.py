from tool_lint import POOR, SPLIT, lint, overlap, page


def test_the_first_set_breaks_the_rules_and_overlaps():
    assert lint(POOR[0]) == ["no-boundary", "no-use-when", "short-description"]
    assert lint(POOR[1]) == ["no-boundary", "no-use-when", "param-undescribed", "short-description"]
    assert round(overlap(POOR[0], POOR[1]), 2) == 0.71


def test_the_split_set_is_clean_and_does_not_overlap():
    assert [lint(tool) for tool in SPLIT] == [[], [], [], []]
    assert max(overlap(a, b) for i, a in enumerate(SPLIT) for b in SPLIT[i + 1:]) < 0.6


def test_a_page_carries_a_cursor_until_the_last_row():
    rows = [str(i) for i in range(6)]
    first, cursor, note = page(rows, limit=4)
    assert first == ["0", "1", "2", "3"] and cursor and note.startswith("Showing 4 of 6")
    second, cursor, note = page(rows, cursor, 4)
    assert second == ["4", "5"] and cursor is None and note is None
