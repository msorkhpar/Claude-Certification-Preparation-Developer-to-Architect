/** A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md. */

export type Chunk = { id: string; doc: string; version: number; text: string };
export type Report = { added: string[]; replaced: string[]; removed: string[]; kept: string[] };
const STOP = new Set(["a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at"]);

/** A cheap fingerprint of a document's text: when it changes, the document changed. */
export function docVersion(text: string): number {
  let sum = 0;
  for (let i = 0; i < text.length; i++) sum += text.charCodeAt(i);
  return sum % 1000003;
}

export function tokens(text: string): string[] {
  return (text.toLowerCase().match(/[a-z0-9]+(?:-[a-z0-9]+)*/g) ?? []).filter((t) => !STOP.has(t));
}

export function chunkSections(docId: string, text: string, maxWords = 30): Chunk[] {
  const [head, ...sections] = text.split("\n## ");
  const title = head.replace(/^# /, "");
  const version = docVersion(text);
  const chunks: Chunk[] = [];
  for (const section of sections) {
    const cut = section.indexOf("\n");
    const name = section.slice(0, cut);
    const parts: string[] = [];
    let current: string[] = [];
    for (const sentence of section.slice(cut + 1).split(/(?<=\.) /)) {
      if (current.length && [...current, sentence].join(" ").split(/\s+/).length > maxWords) {
        parts.push(current.join(" "));
        current = [];
      }
      current.push(sentence);
    }
    parts.push(current.join(" "));
    parts.forEach((part, i) => chunks.push({ id: `${docId}/${name}${parts.length === 1 ? "" : `#${i + 1}`}`, doc: docId, version, text: `${title} > ${name}. ${part}` }));
  }
  return chunks;
}

export function search(chunks: Chunk[], query: string, k = 3, allowedDocs: Set<string> | null = null): string[] {
  const wanted = new Set(tokens(query));
  const scored: Array<[number, number, string]> = [];
  chunks.forEach((chunk, n) => {
    if (allowedDocs !== null && !allowedDocs.has(chunk.doc)) return;
    const have = new Set(tokens(chunk.text));
    let score = 0;
    for (const t of wanted) if (have.has(t)) score += /[0-9]/.test(t) ? 3 : 1;
    if (score) scored.push([-score, n, chunk.id]);
  });
  scored.sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  return scored.slice(0, k).map((s) => s[2]);
}

export function chooseRetrieval(corpusTokens: number, shape: string, pattern: string): string {
  if (corpusTokens < 200000) return "cached prompt";
  if (shape === "table") return "structured query";
  if (pattern === "multi-hop") return "agentic search";
  return ({ identifier: "keyword index", paraphrase: "embedding index" } as Record<string, string>)[pattern] ?? "hybrid index";
}

export function reindex(chunks: Chunk[], docs: Record<string, string>): [Chunk[], Report] {
  const old = new Map<string, Chunk[]>();
  for (const chunk of chunks) old.set(chunk.doc, [...(old.get(chunk.doc) ?? []), chunk]);
  const report: Report = { added: [], replaced: [], removed: [...old.keys()].filter((d) => !(d in docs)), kept: [] };
  const result: Chunk[] = [];
  for (const [docId, text] of Object.entries(docs)) {
    const had = old.get(docId);
    if (had && had[0].version === docVersion(text)) {
      report.kept.push(docId);
      result.push(...had);
    } else {
      (had ? report.replaced : report.added).push(docId);
      result.push(...chunkSections(docId, text));
    }
  }
  return [result, report];
}

export function stale(chunks: Chunk[], docs: Record<string, string>): string[] {
  return chunks.filter((c) => !(c.doc in docs) || c.version !== docVersion(docs[c.doc])).map((c) => c.id);
}

export function recallAtK(results: Record<string, string[]>, relevant: Record<string, string>, k: number): number {
  const entries = Object.entries(relevant);
  if (entries.length === 0) return 0;
  const hits = entries.filter(([query, id]) => (results[query] ?? []).slice(0, k).includes(id)).length;
  return Math.round((hits / entries.length) * 100) / 100;
}
