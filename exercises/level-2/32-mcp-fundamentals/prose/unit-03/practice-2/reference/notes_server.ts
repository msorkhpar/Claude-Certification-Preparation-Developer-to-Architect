// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import { McpServer, ResourceTemplate } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { ErrorCode, McpError } from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";
import { logger } from "./logger.ts";
const log = logger("notes_server");

const MAX_TEXT = 500;
const server = new McpServer({ name: "notes", version: "1.0.0" });
const notes: { title: string; text: string }[] = []; // the id of a note is its position, counting from 1

const fail = (message: string) => ({ isError: true, content: [{ type: "text" as const, text: message }] });
const ok = (text: string) => ({ content: [{ type: "text" as const, text }] });

/** Refuse a bad note: the error message, or null when the (stripped) title and text are fine. */
function noteError(title: string, text: string): string | null {
  if (!title) return "title is required";
  if (!text) return "text is required";
  if (text.length > MAX_TEXT) return `text is too long (max ${MAX_TEXT})`;
  return null;
}

/** Refuse a bad search: the error message, or null when the (stripped) query and the limit are fine. */
function searchError(query: string, limit: number): string | null {
  if (!query) return "query is required";
  if (limit < 1 || limit > 20) return "limit must be between 1 and 20";
  return null;
}

/** The lines `{id}. {title}` of the notes whose title or text contains the query, in any letter case, in id order. */
function findHits(query: string): string[] {
  const needle = query.toLowerCase();
  return notes.flatMap((n, i) => (n.title.toLowerCase().includes(needle) || n.text.toLowerCase().includes(needle) ? [`${i + 1}. ${n.title}`] : []));
}

/** The answer of a search: at most `limit` hit lines joined by newlines, or the no-match sentence. */
function formatHits(hits: string[], limit: number, query: string): string {
  return hits.length ? hits.slice(0, limit).join("\n") : `No notes match "${query}"`;
}

/** The text of the count resource: 0 notes, 1 note, 2 notes. */
function countText(count: number): string {
  return `${count} note${count === 1 ? "" : "s"}`;
}

/** The text of one note, or an McpError "No note {id}" when the id is not a whole number of an existing note. */
function noteText(id: string): string {
  if (!/^\d+$/.test(id) || Number(id) < 1 || Number(id) > notes.length) throw new McpError(ErrorCode.InvalidParams, `No note ${id}`);
  const n = notes[Number(id) - 1];
  return `${n.title}\n\n${n.text}`;
}

/** The text of the review prompt. */
function reviewText(tone: string): string {
  if (!notes.length) return "There are no notes to review.";
  return `Review these notes in a ${tone} tone:\n` + notes.map((n) => `- ${n.title}`).join("\n");
}

/** How many hits a search returns when the caller gives no limit. */
function defaultLimit(): number {
  return 5;
}

/** The annotations that tell a client search_notes only reads. */
function searchAnnotations(): { readOnlyHint: boolean } | undefined {
  return { readOnlyHint: true };
}

server.registerTool(
  "add_note",
  { description: "Save a note with a title and a text.", inputSchema: { title: z.string(), text: z.string() }, annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false } },
  async ({ title, text }) => {
    log.debug("add_note input", { title, text });
    title = title.trim();
    text = text.trim();
    const error = noteError(title, text);
    if (error) return fail(error);
    notes.push({ title, text });
    return ok(`Saved note ${notes.length}: ${title}`);
  },
);

server.registerTool(
  "search_notes",
  { description: "Find notes whose title or text contains the query.", inputSchema: { query: z.string(), limit: z.number().int().default(defaultLimit()) }, annotations: searchAnnotations() },
  async ({ query, limit }) => {
    const error = searchError(query.trim(), limit);
    if (error) return fail(error);
    return ok(formatHits(findHits(query.trim()), limit, query.trim()));
  },
);

server.registerResource("count", "notes://count", { description: "How many notes there are.", mimeType: "text/plain" }, async (uri) => ({
  contents: [{ uri: uri.href, mimeType: "text/plain", text: countText(notes.length) }],
}));

server.registerResource("note", new ResourceTemplate("notes://note/{id}", { list: undefined }), { description: "One note by id.", mimeType: "text/plain" }, async (uri, { id }) => ({
  contents: [{ uri: uri.href, mimeType: "text/plain", text: noteText(String(id)) }],
}));

server.registerPrompt("review_notes", { description: "Ask for a review of the notes.", argsSchema: { tone: z.string().optional() } }, ({ tone }) => ({
  messages: [{ role: "user" as const, content: { type: "text" as const, text: reviewText(tone ?? "brief") } }],
}));

await server.connect(new StdioServerTransport());
