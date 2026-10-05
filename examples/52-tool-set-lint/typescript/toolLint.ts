// Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.
//
// No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
// neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
// page of the Claude API documentation and the "Writing tools for agents" article.
import { logger } from "./logger.ts";
const log = logger("tool_lint");
const OVERLAP = 0.6;

type Tool = { name: string; description: string; params: Record<string, string> };

export function lint(tool: Tool): string[] {
  const text = tool.description.toLowerCase();
  const found: string[] = [];
  if (!["do not use", "not for", "instead of"].some((p) => text.includes(p))) found.push("no-boundary");
  if (!text.includes("use when")) found.push("no-use-when");
  if (Object.values(tool.params).some((d) => !d.trim())) found.push("param-undescribed");
  if ((tool.description.match(/[.!?](?:\s|$)/g) ?? []).length < 3) found.push("short-description");
  return found;
}

export function overlap(a: Tool, b: Tool): number {
  const wa = new Set(a.description.toLowerCase().match(/[a-z]{3,}/g)), wb = new Set(b.description.toLowerCase().match(/[a-z]{3,}/g));
  const shared = [...wa].filter((w) => wb.has(w)).length;
  return shared / new Set([...wa, ...wb]).size;
}

function report(title: string, tools: Tool[]) {
  console.log(title);
  for (const tool of tools) console.log(`  ${tool.name}: ${lint(tool).join(", ") || "clean"}`);
  tools.forEach((a, i) => {
    for (const b of tools.slice(i + 1)) {
      const score = overlap(a, b);
      if (score >= OVERLAP) console.log(`  overlap: ${a.name} and ${b.name} (${score.toFixed(2)})`);
    }
  });
}

export function page(items: string[], cursor: string | null = null, limit = 4): [string[], string | null, string | null] {
  const offset = cursor === null ? 0 : Number(Buffer.from(cursor, "base64").toString().split(":")[1]);
  const chunk = items.slice(offset, offset + limit);
  const more = offset + chunk.length < items.length;
  const token = more ? Buffer.from(`offset:${offset + chunk.length}`).toString("base64") : null;
  const note = more ? `Showing ${chunk.length} of ${items.length} results; pass next_cursor to continue, or narrow the query with a filter.` : null;
  return [chunk, token, note];
}

export const POOR: Tool[] = [
  { name: "analyze_content", description: "Analyzes content and returns the result.", params: { content: "The content." } },
  { name: "analyze_document", description: "Analyzes a document and returns the result.", params: { document: "" } },
];

export const SPLIT: Tool[] = [
  { name: "extract_web_results", params: { url: "The page address." },
    description: "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those." },
  { name: "extract_data_points", params: { document_id: "The id of an uploaded document." },
    description: "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those." },
  { name: "summarize_content", params: { text: "The text to shorten.", max_words: "The longest summary, in words." },
    description: "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that." },
  { name: "verify_claim_against_source", params: { claim: "One sentence to test.", source_id: "The id of the source to test it against." },
    description: "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that." },
];

function main() {
  report("the set as first written", POOR);
  console.log();
  report("the same job, one contract per tool", SPLIT);
  const rows = Array.from({ length: 25 }, (_, i) => `row-${String(i).padStart(2, "0")}`);
  console.log("\na long result, paged four rows at a time");
  let cursor: string | null = null;
  for (const number of [1, 2]) {
    let note: string | null;
    let chunk: string[];
    [chunk, cursor, note] = page(rows, cursor);
    console.log(`  page ${number}: ${chunk.join(" ")}`);
    console.log(`    note: ${note}`);
  }
  console.log(`  the cursor is opaque: ${cursor}`);
}

if (import.meta.main) main();
