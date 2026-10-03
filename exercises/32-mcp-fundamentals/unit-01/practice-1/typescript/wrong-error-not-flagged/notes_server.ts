// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import { McpServer, ResourceTemplate } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { ErrorCode, McpError } from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";

const MAX_TEXT = 500;
const server = new McpServer({ name: "notes", version: "1.0.0" });
const notes: { title: string; text: string }[] = []; // the id of a note is its position, counting from 1

const fail = (message: string) => ({ isError: true, content: [{ type: "text" as const, text: message }] });
const ok = (text: string) => ({ content: [{ type: "text" as const, text }] });

server.registerTool(
  "add_note",
  { description: "Save a note with a title and a text.", inputSchema: { title: z.string(), text: z.string() }, annotations: { readOnlyHint: false, destructiveHint: false, idempotentHint: false } },
  async ({ title, text }) => {
    title = title.trim();
    text = text.trim();
    if (!title) return ok("title is required");
    if (!text) return fail("text is required");
    if (text.length > MAX_TEXT) return fail(`text is too long (max ${MAX_TEXT})`);
    notes.push({ title, text });
    return ok(`Saved note ${notes.length}: ${title}`);
  },
);

server.registerTool(
  "search_notes",
  { description: "Find notes whose title or text contains the query.", inputSchema: { query: z.string(), limit: z.number().int().default(5) }, annotations: { readOnlyHint: true } },
  async ({ query, limit }) => {
    if (!query.trim()) return fail("query is required");
    if (limit < 1 || limit > 20) return fail("limit must be between 1 and 20");
    const needle = query.trim().toLowerCase();
    const hits = notes.flatMap((n, i) => (n.title.toLowerCase().includes(needle) || n.text.toLowerCase().includes(needle) ? [`${i + 1}. ${n.title}`] : []));
    return ok(hits.length ? hits.slice(0, limit).join("\n") : `No notes match "${query.trim()}"`);
  },
);

server.registerResource("count", "notes://count", { description: "How many notes there are.", mimeType: "text/plain" }, async (uri) => ({
  contents: [{ uri: uri.href, mimeType: "text/plain", text: `${notes.length} note${notes.length === 1 ? "" : "s"}` }],
}));

server.registerResource("note", new ResourceTemplate("notes://note/{id}", { list: undefined }), { description: "One note by id.", mimeType: "text/plain" }, async (uri, { id }) => {
  const key = String(id);
  if (!/^\d+$/.test(key) || Number(key) < 1 || Number(key) > notes.length) throw new McpError(ErrorCode.InvalidParams, `No note ${key}`);
  const n = notes[Number(key) - 1];
  return { contents: [{ uri: uri.href, mimeType: "text/plain", text: `${n.title}\n\n${n.text}` }] };
});

server.registerPrompt("review_notes", { description: "Ask for a review of the notes.", argsSchema: { tone: z.string().optional() } }, ({ tone }) => {
  const text = notes.length ? `Review these notes in a ${tone ?? "brief"} tone:\n` + notes.map((n) => `- ${n.title}`).join("\n") : "There are no notes to review.";
  return { messages: [{ role: "user" as const, content: { type: "text" as const, text } }] };
});

await server.connect(new StdioServerTransport());
