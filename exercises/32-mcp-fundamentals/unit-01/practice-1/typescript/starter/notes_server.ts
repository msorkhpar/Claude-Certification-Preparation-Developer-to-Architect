// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import { McpServer, ResourceTemplate } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { ErrorCode, McpError } from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";
import { logger } from "../logger.ts";
const log = logger("notes_server");

const MAX_TEXT = 500;
const server = new McpServer({ name: "notes", version: "1.0.0" });
const notes: { title: string; text: string }[] = []; // the id of a note is its position, counting from 1

const fail = (message: string) => ({ isError: true, content: [{ type: "text" as const, text: message }] });
const ok = (text: string) => ({ content: [{ type: "text" as const, text }] });

// GAP 1 of 7 (unlocks e2 and e7): refuse a bad note.
// Receives the title and the text, both already stripped. Returns the error message, or null when they are fine: "title is required" for an empty title,
// "text is required" for an empty text and `text is too long (max ${MAX_TEXT})` for a text longer than MAX_TEXT, checked in that order.
// Example: noteError("", "x") -> "title is required"; noteError("T", "x") -> null
function noteError(title: string, text: string): string | null {
  return null;
}

// GAP 2 of 7 (unlocks e2): refuse a bad search.
// Receives the stripped query and the limit. Returns the error message, or null when they are fine: "query is required" for an empty query and
// "limit must be between 1 and 20" for a limit outside 1 to 20, in that order.
// Example: searchError("x", 21) -> "limit must be between 1 and 20"
function searchError(query: string, limit: number): string | null {
  return null;
}

// GAP 3 of 7 (unlocks m1 and e3): the notes that match a search.
// Receives the stripped query. Returns the lines `{id}. {title}` of the notes (ids count from 1) whose title or text contains the query in any
// letter case, in id order.
// Example: with notes ("Alpha", "x") and ("beta", "ALPHA again"), findHits("alpha") -> ["1. Alpha", "2. beta"]
function findHits(query: string): string[] {
  return [];
}

// GAP 4 of 7 (unlocks e3): the answer of a search.
// Receives the hit lines, the limit and the stripped query. Returns at most `limit` lines joined by newlines; with no hits the sentence
// No notes match "<query>".
// Example: formatHits(["1. A", "2. B"], 1, "a") -> "1. A"; formatHits([], 5, "zeta") -> 'No notes match "zeta"'
function formatHits(hits: string[], limit: number, query: string): string {
  return "";
}

// GAP 5 of 7 (unlocks e5): the text of the count resource.
// Receives the number of notes. Returns "0 notes", "1 note", "2 notes" and so on.
// Example: countText(1) -> "1 note"
function countText(count: number): string {
  return "";
}

// GAP 6 of 7 (unlocks m1 and e5): the text of one note.
// Receives the id from the URI as a string. Returns the title, an empty line, then the text. An id that is not a whole number of an existing note
// ("0", "3" of two notes, "abc") throws new McpError(ErrorCode.InvalidParams, `No note ${id}`).
// Example: with one note ("Plan", "ship it"), noteText("1") -> "Plan\n\nship it"
function noteText(id: string): string {
  return "";
}

// GAP 7 of 7 (unlocks e6): the text of the review prompt.
// With no notes it is "There are no notes to review."; otherwise "Review these notes in a <tone> tone:" and one line "- <title>" per note,
// each after a newline.
// Example: with one note titled "Plan", reviewText("brief") -> "Review these notes in a brief tone:\n- Plan"
function reviewText(tone: string): string {
  return "";
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
  { description: "Find notes whose title or text contains the query.", inputSchema: { query: z.string(), limit: z.number().int().default(5) }, annotations: { readOnlyHint: true } },
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
