"""A small reader for the YAML subset that Claude Code files and GitHub workflows use: nested maps, lists, flow lists, quoted
strings, block scalars (| and >) and comments. It is not a general YAML parser: no anchors, no multi-document streams, no flow maps
beyond an empty one. It exists so the examples and tests need nothing outside the standard library."""
import re

KEY = re.compile(r"""^("[^"]*"|'[^']*'|[^\s:#'"\-][^:]*?|-[^\s:][^:]*?):(?:\s+(.*))?$""")


def _scalar(text):
    text = re.sub(r"\s+#.*$", "", text.strip()) if not text.strip().startswith(("'", '"')) else text.strip()
    if text == "":
        return None
    if text[0] in "\"'" and text[-1] == text[0] and len(text) > 1:
        return text[1:-1]
    if text == "[]":
        return []
    if text == "{}":
        return {}
    if text[0] == "[" and text[-1] == "]":
        return [_scalar(part) for part in re.findall(r"""("[^"]*"|'[^']*'|[^,]+)""", text[1:-1])]
    if text in ("true", "false"):
        return text == "true"
    if text in ("null", "~"):
        return None
    if re.fullmatch(r"-?\d+", text):
        return int(text)
    return text


def parse_yaml(text):
    raw = text.split("\n")
    lines = []  # (indent, content, raw index)
    for i, line in enumerate(raw):
        stripped = line.strip()
        if stripped and not stripped.startswith("#"):
            lines.append([len(line) - len(line.lstrip()), stripped, i])
    pos = 0

    def block_scalar(start_raw, parent_indent, folded):
        nonlocal pos
        body, j = [], start_raw + 1
        while j < len(raw) and (not raw[j].strip() or len(raw[j]) - len(raw[j].lstrip()) > parent_indent):
            body.append(raw[j])
            j += 1
        while pos < len(lines) and lines[pos][2] < j:
            pos += 1
        indents = [len(b) - len(b.lstrip()) for b in body if b.strip()]
        cut = min(indents) if indents else 0
        parts = [b[cut:] if b.strip() else "" for b in body]
        while parts and parts[-1] == "":
            parts.pop()
        return (" ".join(parts) if folded else "\n".join(parts)) + "\n"

    def value_after(rest, indent, raw_index):
        nonlocal pos
        if rest in ("|", ">", "|-", ">-"):
            out = block_scalar(raw_index, indent, rest[0] == ">")
            return out.rstrip("\n") if rest.endswith("-") else out
        if rest:
            return _scalar(rest)
        if pos < len(lines) and (lines[pos][0] > indent or (lines[pos][0] == indent and lines[pos][1].startswith("- "))):
            return parse_block(lines[pos][0])
        return None

    def parse_block(indent):
        nonlocal pos
        if lines[pos][1].startswith("- ") or lines[pos][1] == "-":
            items = []
            while pos < len(lines) and lines[pos][0] == indent and (lines[pos][1].startswith("- ") or lines[pos][1] == "-"):
                rest = lines[pos][1][1:].strip()
                raw_index = lines[pos][2]
                if rest == "":
                    pos += 1
                    items.append(parse_block(lines[pos][0]) if pos < len(lines) and lines[pos][0] > indent else None)
                elif KEY.match(rest) and not rest.startswith(("'", '"')):
                    lines[pos] = [indent + 2, rest, raw_index]
                    items.append(parse_block(indent + 2))
                else:
                    pos += 1
                    items.append(_scalar(rest))
            return items
        mapping = {}
        while pos < len(lines) and lines[pos][0] == indent and not lines[pos][1].startswith("- "):
            m = KEY.match(lines[pos][1])
            if not m:
                raise ValueError(f"line {lines[pos][2] + 1}: cannot read {lines[pos][1]!r}")
            key = m.group(1).strip("\"'")
            raw_index, rest = lines[pos][2], (m.group(2) or "").strip()
            pos += 1
            mapping[key] = value_after(rest, indent, raw_index)
        return mapping

    return parse_block(lines[0][0]) if lines else {}


def split_frontmatter(text):
    """(frontmatter mapping, body) of a Markdown file that starts with a --- block; ({}, text) when it has none."""
    m = re.match(r"^---\n(.*?)\n---\n?(.*)$", text, re.S)
    return (parse_yaml(m.group(1)), m.group(2)) if m else ({}, text)
