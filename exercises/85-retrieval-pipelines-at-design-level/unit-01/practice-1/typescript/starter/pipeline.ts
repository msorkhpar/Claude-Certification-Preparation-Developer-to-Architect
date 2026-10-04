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

export function chunkSections(docId: string, text: string, maxWords = 30): Chunk[] | null {
  // TODO: one chunk per section, its text starting with "<title> > <section>. ", split at sentence ends when a section is longer than maxWords.
  return null;
}

export function search(chunks: Chunk[], query: string, k = 3, allowedDocs: Set<string> | null = null): string[] | null {
  // TODO: the ids of the k best chunks for the query among the documents the caller may read.
  return null;
}

export function chooseRetrieval(corpusTokens: number, shape: string, pattern: string): string | null {
  // TODO: the retrieval mechanism for a corpus of this size, this data shape ("text" or "table") and this query pattern.
  return null;
}

export function reindex(chunks: Chunk[], docs: Record<string, string>): [Chunk[], Report] | null {
  // TODO: bring the chunks in line with the documents and report what was kept, replaced, added and removed.
  return null;
}

export function stale(chunks: Chunk[], docs: Record<string, string>): string[] | null {
  // TODO: the ids of the chunks that no longer match their source.
  return null;
}

export function recallAtK(results: Record<string, string[]>, relevant: Record<string, string>, k: number): number | null {
  // TODO: the share of all labelled questions whose relevant chunk is among the first k results.
  return null;
}
