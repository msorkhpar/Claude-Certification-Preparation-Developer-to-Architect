// A small reader for the YAML subset that Claude Code files and GitHub workflows use: nested maps, lists, flow lists, quoted
// strings, block scalars (| and >) and comments. It is not a general YAML parser: no anchors, no multi-document streams, no flow maps
// beyond an empty one. It exists so the examples and tests need nothing outside the standard library.
export type Yaml = string | number | boolean | null | Yaml[] | { [key: string]: Yaml };

const KEY = /^("[^"]*"|'[^']*'|[^\s:#'"\-][^:]*?|-[^\s:][^:]*?):(?:\s+(.*))?$/;

function scalar(input: string): Yaml {
  let text = input.trim();
  if (!text.startsWith("'") && !text.startsWith('"')) text = text.replace(/\s+#.*$/, "").trim();
  if (text === "") return null;
  if ((text[0] === '"' || text[0] === "'") && text[text.length - 1] === text[0] && text.length > 1) return text.slice(1, -1);
  if (text === "[]") return [];
  if (text === "{}") return {};
  if (text[0] === "[" && text[text.length - 1] === "]") return (text.slice(1, -1).match(/("[^"]*"|'[^']*'|[^,]+)/g) ?? []).map(scalar);
  if (text === "true" || text === "false") return text === "true";
  if (text === "null" || text === "~") return null;
  if (/^-?\d+$/.test(text)) return Number(text);
  return text;
}

export function parseYaml(text: string): Yaml {
  const raw = text.split("\n");
  const lines: Array<[number, string, number]> = [];
  raw.forEach((line, i) => {
    const stripped = line.trim();
    if (stripped && !stripped.startsWith("#")) lines.push([line.length - line.trimStart().length, stripped, i]);
  });
  let pos = 0;

  function blockScalar(startRaw: number, parentIndent: number, folded: boolean): string {
    const body: string[] = [];
    let j = startRaw + 1;
    while (j < raw.length && (!raw[j].trim() || raw[j].length - raw[j].trimStart().length > parentIndent)) body.push(raw[j++]);
    while (pos < lines.length && lines[pos][2] < j) pos++;
    const indents = body.filter((b) => b.trim()).map((b) => b.length - b.trimStart().length);
    const cut = indents.length ? Math.min(...indents) : 0;
    const parts = body.map((b) => (b.trim() ? b.slice(cut) : ""));
    while (parts.length && parts[parts.length - 1] === "") parts.pop();
    return (folded ? parts.join(" ") : parts.join("\n")) + "\n";
  }

  function valueAfter(rest: string, indent: number, rawIndex: number): Yaml {
    if (rest === "|" || rest === ">" || rest === "|-" || rest === ">-") {
      const out = blockScalar(rawIndex, indent, rest[0] === ">");
      return rest.endsWith("-") ? out.replace(/\n+$/, "") : out;
    }
    if (rest) return scalar(rest);
    if (pos < lines.length && (lines[pos][0] > indent || (lines[pos][0] === indent && lines[pos][1].startsWith("- ")))) return parseBlock(lines[pos][0]);
    return null;
  }

  function parseBlock(indent: number): Yaml {
    if (lines[pos][1].startsWith("- ") || lines[pos][1] === "-") {
      const items: Yaml[] = [];
      while (pos < lines.length && lines[pos][0] === indent && (lines[pos][1].startsWith("- ") || lines[pos][1] === "-")) {
        const rest = lines[pos][1].slice(1).trim();
        const rawIndex = lines[pos][2];
        if (rest === "") {
          pos++;
          items.push(pos < lines.length && lines[pos][0] > indent ? parseBlock(lines[pos][0]) : null);
        } else if (KEY.test(rest) && !rest.startsWith("'") && !rest.startsWith('"')) {
          lines[pos] = [indent + 2, rest, rawIndex];
          items.push(parseBlock(indent + 2));
        } else {
          pos++;
          items.push(scalar(rest));
        }
      }
      return items;
    }
    const mapping: { [key: string]: Yaml } = {};
    while (pos < lines.length && lines[pos][0] === indent && !lines[pos][1].startsWith("- ")) {
      const m = KEY.exec(lines[pos][1]);
      if (!m) throw new Error(`line ${lines[pos][2] + 1}: cannot read ${JSON.stringify(lines[pos][1])}`);
      const key = m[1].trim().replace(/^["']|["']$/g, "");
      const rawIndex = lines[pos][2];
      const rest = (m[2] ?? "").trim();
      pos++;
      mapping[key] = valueAfter(rest, indent, rawIndex);
    }
    return mapping;
  }

  return lines.length ? parseBlock(lines[0][0]) : {};
}

/** [frontmatter mapping, body] of a Markdown file that starts with a --- block; [{}, text] when it has none. */
export function splitFrontmatter(text: string): [{ [key: string]: Yaml }, string] {
  const m = /^---\n([\s\S]*?)\n---\n?([\s\S]*)$/.exec(text);
  return m ? [parseYaml(m[1]) as { [key: string]: Yaml }, m[2]] : [{}, text];
}
