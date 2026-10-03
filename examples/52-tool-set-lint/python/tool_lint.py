"""Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.

No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
page of the Claude API documentation and the "Writing tools for agents" article.
"""
import base64
import re

OVERLAP = 0.6


def lint(tool):
    text = tool["description"].lower()
    found = []
    if not any(p in text for p in ("do not use", "not for", "instead of")):
        found.append("no-boundary")
    if "use when" not in text:
        found.append("no-use-when")
    if any(not d.strip() for d in tool["params"].values()):
        found.append("param-undescribed")
    if len(re.findall(r"[.!?](?:\s|$)", tool["description"])) < 3:
        found.append("short-description")
    return found


def overlap(a, b):
    wa, wb = set(re.findall(r"[a-z]{3,}", a["description"].lower())), set(re.findall(r"[a-z]{3,}", b["description"].lower()))
    return len(wa & wb) / len(wa | wb)


def report(title, tools):
    print(title)
    for tool in tools:
        print(f"  {tool['name']}: {', '.join(lint(tool)) or 'clean'}")
    for i, a in enumerate(tools):
        for b in tools[i + 1:]:
            score = overlap(a, b)
            if score >= OVERLAP:
                print(f"  overlap: {a['name']} and {b['name']} ({score:.2f})")


def page(items, cursor=None, limit=4):
    offset = 0 if cursor is None else int(base64.b64decode(cursor).decode().split(":")[1])
    chunk = items[offset:offset + limit]
    more = offset + len(chunk) < len(items)
    token = base64.b64encode(f"offset:{offset + len(chunk)}".encode()).decode() if more else None
    note = f"Showing {len(chunk)} of {len(items)} results; pass next_cursor to continue, or narrow the query with a filter." if more else None
    return chunk, token, note


POOR = [
    {"name": "analyze_content", "description": "Analyzes content and returns the result.", "params": {"content": "The content."}},
    {"name": "analyze_document", "description": "Analyzes a document and returns the result.", "params": {"document": ""}},
]

SPLIT = [
    {"name": "extract_web_results", "params": {"url": "The page address."},
     "description": "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those."},
    {"name": "extract_data_points", "params": {"document_id": "The id of an uploaded document."},
     "description": "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those."},
    {"name": "summarize_content", "params": {"text": "The text to shorten.", "max_words": "The longest summary, in words."},
     "description": "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that."},
    {"name": "verify_claim_against_source", "params": {"claim": "One sentence to test.", "source_id": "The id of the source to test it against."},
     "description": "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that."},
]


def main():
    report("the set as first written", POOR)
    print()
    report("the same job, one contract per tool", SPLIT)
    rows = [f"row-{i:02d}" for i in range(25)]
    print("\na long result, paged four rows at a time")
    cursor = None
    for number in (1, 2):
        chunk, cursor, note = page(rows, cursor)
        print(f"  page {number}: {' '.join(chunk)}")
        print(f"    note: {note}")
    print(f"  the cursor is opaque: {cursor}")


if __name__ == "__main__":
    main()
