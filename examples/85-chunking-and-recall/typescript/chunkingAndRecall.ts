/**
 * Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.
 *
 * The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
 * offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
 * documentation page "Embeddings". Nothing here calls a model.
 */
export type Chunk = [string, string];

const STOP = new Set(["a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at"]);
export const DOCS: Record<string, string> = {
  monthly: "# Monthly plan\n## Cancellation\nYou can cancel at any time and the current month is not refunded.",
  annual: "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.",
  refunds: "# Refund policy\n## Eligibility\nCustomers may return items within 30 days of delivery.\n## Exceptions\nItems marked final sale cannot be returned, except when they arrive damaged.\n## Process\nRefunds go back to the original payment method within 5 business days.",
  errors: "# Error codes\n## E-7310\nThe warehouse could not reserve stock. Retry after the next stock sync.\n## E-4021\nThe payment gateway rejected the card. Ask for another card.",
};

export function tokens(text: string): string[] {
  return (text.toLowerCase().match(/[a-z0-9]+(?:-[a-z0-9]+)*/g) ?? []).filter((t) => !STOP.has(t));
}

/** Cut every `size` words, whatever the words mean. */
export function chunkFixed(docId: string, text: string, size: number): Chunk[] {
  const words = text.split(/\s+/).filter((w) => w !== "");
  const out: Chunk[] = [];
  for (let i = 0; i < words.length; i += size) out.push([`${docId}#${Math.floor(i / size)}`, words.slice(i, i + size).join(" ")]);
  return out;
}

/** Cut at the headings; with context, each chunk starts with the document title and its section name, so it can be found and read alone. */
export function chunkSections(docId: string, text: string, context: boolean): Chunk[] {
  const [head, ...sections] = text.split("\n## ");
  const title = head.replace(/^# /, "");
  return sections.map((section) => {
    const cut = section.indexOf("\n");
    const name = section.slice(0, cut);
    return [`${docId}/${name}`, (context ? `${title} > ${name}. ` : "") + section.slice(cut + 1)] as Chunk;
  });
}

export function index(docs: Record<string, string>, context: boolean): Chunk[] {
  return Object.entries(docs).flatMap(([docId, text]) => chunkSections(docId, text, context));
}

/** Words that appear in the chunk score one, a token with a digit (a code or an id) scores three; ties keep the index order. */
export function lexical(chunks: Chunk[], query: string, k = 3): string[] {
  const wanted = new Set(tokens(query));
  const scored: Array<[number, number, string]> = [];
  chunks.forEach(([chunkId, text], n) => {
    const have = new Set(tokens(text));
    let score = 0;
    for (const t of wanted) if (have.has(t)) score += /[0-9]/.test(t) ? 3 : 1;
    if (score) scored.push([-score, n, chunkId]);
  });
  scored.sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  return scored.slice(0, k).map((s) => s[2]);
}

/** Reciprocal rank fusion, in integers so that every language ranks the same: each list adds 1000000 // (k + rank) to a chunk. */
export function fuse(rankings: string[][], k = 60): string[] {
  const score = new Map<string, number>();
  for (const ranking of rankings) ranking.forEach((chunkId, i) => score.set(chunkId, (score.get(chunkId) ?? 0) + Math.floor(1000000 / (k + i + 1))));
  return [...score.keys()].sort((a, b) => score.get(b)! - score.get(a)! || (a < b ? -1 : a > b ? 1 : 0));
}

/** True when one retrieved chunk holds the whole answer sentence. */
export function holds(chunks: Chunk[], chunkIds: string[], answer: string): boolean {
  const texts = new Map(chunks);
  return chunkIds.some((id) => texts.get(id)!.includes(answer));
}

/** The shortcut: add the new chunks and leave the old ones where they are. */
export function reindexAdditive(chunks: Chunk[], docId: string, text: string): Chunk[] {
  return [...chunks, ...chunkSections(docId, text, true)];
}

/** Drop every chunk of the document first, then add the new ones. */
export function reindexReplace(chunks: Chunk[], docId: string, text: string): Chunk[] {
  return [...chunks.filter((c) => !c[0].startsWith(docId + "/")), ...chunkSections(docId, text, true)];
}

/** Chunks that no longer match what their source says now. */
export function stale(chunks: Chunk[], docs: Record<string, string>): string[] {
  const current = new Set(index(docs, true).map((c) => JSON.stringify(c)));
  return chunks.filter((c) => !current.has(JSON.stringify(c))).map((c) => c[0]);
}

/** The cheapest mechanism that fits: a corpus under 200,000 tokens fits a cached prompt; a table is queried; several hops need an agent that searches; otherwise the query pattern picks the index. */
export function chooseRetrieval(corpusTokens: number, shape: string, pattern: string): string {
  if (corpusTokens < 200000) return "cached prompt";
  if (shape === "table") return "structured query";
  if (pattern === "multi-hop") return "agentic search";
  return ({ identifier: "keyword index", paraphrase: "embedding index" } as Record<string, string>)[pattern] ?? "hybrid index";
}

/** Where a question went wrong, judged by retrieval and by generation separately; a right answer without its evidence is a risk of its own. */
export function layer(evidenceRetrieved: boolean, answerCorrect: boolean): string {
  if (answerCorrect) return evidenceRetrieved ? "ok" : "unsupported";
  return evidenceRetrieved ? "generation" : "retrieval";
}

const yes = (flag: boolean) => (flag ? "yes" : "no");
const names = (items: string[]) => (items.length ? items.join(", ") : "none");

function main() {
  const answer = "Items marked final sale cannot be returned, except when they arrive damaged.";
  const fixed = chunkFixed("refunds", DOCS.refunds, 12);
  const sections = chunkSections("refunds", DOCS.refunds, false);
  console.log(`cut every 12 words: ${fixed.length} chunks, the whole rule in one chunk: ${yes(holds(fixed, fixed.map((c) => c[0]), answer))}`);
  console.log(`cut at headings:    ${sections.length} chunks, the whole rule in one chunk: ${yes(holds(sections, sections.map((c) => c[0]), answer))}`);
  for (const context of [false, true]) {
    console.log(`query 'cancel the annual plan', chunks ${context ? "with" : "without"} context: top chunk ${lexical(index(DOCS, context), "cancel the annual plan", 1)[0]}`);
  }
  const chunks = index(DOCS, true);
  const semantic: Record<string, string[]> = {
    "what does E-7310 mean": ["errors/E-4021", "errors/E-7310", "refunds/Process"],
    "when will I be reimbursed": ["refunds/Process", "annual/Cancellation", "monthly/Cancellation"],
  };
  const answers: Record<string, string> = { "what does E-7310 mean": "The warehouse could not reserve stock.", "when will I be reimbursed": "Refunds go back to the original payment method within 5 business days." };
  console.log(`${"query".padEnd(28)}${"lexical".padEnd(9)}${"semantic".padEnd(10)}hybrid`);
  for (const [query, ranking] of Object.entries(semantic)) {
    const lex = lexical(chunks, query);
    const tops = [lex.slice(0, 1), ranking.slice(0, 1), fuse([lex, ranking]).slice(0, 1)];
    console.log(query.padEnd(28) + tops.map((t, i) => yes(holds(chunks, t, answers[query])).padEnd([9, 10, 1][i])).join(""));
  }
  console.log("mechanism by corpus size, data shape and query pattern:");
  const rows: Array<[number, string, string]> = [[50000, "text", "identifier"], [5000000, "table", "paraphrase"], [5000000, "text", "multi-hop"], [5000000, "text", "identifier"], [5000000, "text", "paraphrase"], [5000000, "text", "mixed"]];
  for (const [size, shape, pattern] of rows) console.log(`  ${String(size).padStart(8)} tokens  ${shape.padEnd(6)}${pattern.padEnd(11)}-> ${chooseRetrieval(size, shape, pattern)}`);
  const edited = DOCS.refunds.replace("within 30 days", "within 60 days");
  const live = { ...DOCS, refunds: edited };
  const strategies: Array<[string, (c: Chunk[], d: string, t: string) => Chunk[]]> = [["add the new chunks only", reindexAdditive], ["replace the document's chunks", reindexReplace]];
  for (const [name, fn] of strategies) {
    const after = fn(chunks, "refunds", edited);
    console.log(`after the window changes from 30 to 60 days, ${name}: ${after.length} chunks, stale ${names(stale(after, live))}`);
  }
  const outcomes: Array<[boolean, boolean]> = [[true, true], [true, true], [true, true], [true, true], [true, false], [false, false], [false, false], [false, true]];
  const counts = new Map<string, number>();
  for (const [retrieved, correct] of outcomes) counts.set(layer(retrieved, correct), (counts.get(layer(retrieved, correct)) ?? 0) + 1);
  const byLayer = [...counts.entries()].sort((a, b) => (a[0] < b[0] ? -1 : 1)).map(([k, v]) => `${k} ${v}`).join(", ");
  console.log(`8 questions: evidence retrieved for ${outcomes.filter((o) => o[0]).length}, answers correct ${outcomes.filter((o) => o[1]).length}, by layer ${byLayer}`);
}

if (import.meta.main) main();
