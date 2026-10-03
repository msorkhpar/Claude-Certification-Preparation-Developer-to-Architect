// Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md.

const NAME = /^[A-Za-z0-9_-]{1,128}$/;
const VAGUE = new Set(["tool", "helper", "do", "run", "process", "handle", "data", "util", "utils", "query"]);
const READ_PREFIXES = ["list_", "search_", "find_"];
const WRITE_PREFIXES = ["create_", "update_", "delete_", "remove_", "send_", "write_"];
const DELETE_PREFIXES = ["delete_", "remove_"];
const OVERLAP = 0.6; // token overlap of two descriptions from which the tools count as overlapping
const MAX_LIMIT = 50;
const DEFAULT_HINTS: { [key: string]: boolean } = { readOnlyHint: false, destructiveHint: true, idempotentHint: false, openWorldHint: true };

type Obj = { [key: string]: any };

const startsWithAny = (text: string, prefixes: string[]) => prefixes.some((p) => text.startsWith(p));

function valid(value: any, schema: Obj): boolean {
  const kind = schema.type;
  if (kind === "string" && typeof value !== "string") return false;
  if (kind === "integer" && !Number.isInteger(value)) return false;
  if (kind === "number" && !(typeof value === "number" && Number.isFinite(value))) return false;
  if (kind === "boolean" && typeof value !== "boolean") return false;
  if (kind === "array" && !Array.isArray(value)) return false;
  if (kind === "object" && !(typeof value === "object" && value !== null && !Array.isArray(value))) return false;
  return !("enum" in schema) || schema.enum.includes(value);
}

function exampleOk(example: any, properties: Obj, required: string[]): boolean {
  if (typeof example !== "object" || example === null || Array.isArray(example) || required.some((name) => !(name in example))) return false;
  return Object.entries(example).every(([name, value]) => name in properties && valid(value, properties[name]));
}

/** The rules one tool breaks, sorted and without repeats. */
export function lintTool(tool: Obj): string[] {
  const found = new Set<string>();
  const name = String(tool.name ?? "");
  const description = String(tool.description ?? "");
  const schema = tool.input_schema ?? {};
  const properties: Obj = schema.properties ?? {};
  const required: string[] = schema.required ?? [];
  const low = description.toLowerCase();
  const specs: Obj[] = Object.values(properties).map((s) => s ?? {});
  if (!NAME.test(name)) found.add("bad-name");
  if (VAGUE.has(name.toLowerCase())) found.add("vague-name");
  if ((description.match(/[.!?](?:\s|$)/g) ?? []).length < 3) found.add("short-description");
  if (!low.includes("use when")) found.add("no-use-when");
  if (!["do not use", "not for", "instead of"].some((phrase) => low.includes(phrase))) found.add("no-boundary");
  if (specs.some((spec) => !String(spec.description ?? "").trim())) found.add("param-undescribed");
  if (required.some((item) => !(item in properties))) found.add("required-unknown");
  if (specs.some((spec) => spec.type === "string" && !("enum" in spec) && /one of|either/.test(String(spec.description ?? "").toLowerCase()))) found.add("open-set");
  if (Object.entries(properties).some(([key, spec]) => /reasoning|thinking/.test(key.toLowerCase() + " " + String((spec ?? {}).description ?? "").toLowerCase()))) found.add("reasoning-param");
  if ((tool.input_examples ?? []).some((example: any) => !exampleOk(example, properties, required))) found.add("bad-example");
  if (startsWithAny(name, READ_PREFIXES) && !("limit" in properties && "cursor" in properties)) found.add("list-unbounded");
  const hints: Obj = tool.annotations ?? {};
  if ((hints.readOnlyHint === true && startsWithAny(name, WRITE_PREFIXES)) || (hints.destructiveHint === false && startsWithAny(name, DELETE_PREFIXES))) found.add("hint-contradicts-name");
  return [...found].sort();
}

const words = (text: unknown) => new Set(String(text ?? "").toLowerCase().match(/[a-z]{3,}/g) ?? []);

/** [tool name, rule] pairs, sorted: each tool's own rules, duplicate names, overlapping descriptions and a set that is too large. */
export function lintToolSet(tools: Obj[], maxTools = 20): string[][] {
  const found = new Map<string, string[]>();
  const add = (name: string, rule: string) => found.set(JSON.stringify([name, rule]), [name, rule]);
  for (const tool of tools) for (const rule of lintTool(tool)) add(tool.name, rule);
  const names = tools.map((tool) => tool.name);
  for (const name of new Set(names)) if (names.filter((n) => n === name).length > 1) add(name, "duplicate-name");
  tools.forEach((a, i) => {
    for (const b of tools.slice(i + 1)) {
      const wa = words(a.description), wb = words(b.description);
      const union = new Set([...wa, ...wb]);
      const shared = [...wa].filter((w) => wb.has(w)).length;
      if (a.name !== b.name && union.size > 0 && shared / union.size >= OVERLAP) {
        add(a.name, `overlap:${b.name}`);
        add(b.name, `overlap:${a.name}`);
      }
    }
  });
  if (tools.length > maxTools) add("*", "too-many-tools");
  return [...found.values()].sort((x, y) => (x[0] < y[0] ? -1 : x[0] > y[0] ? 1 : x[1] < y[1] ? -1 : x[1] > y[1] ? 1 : 0));
}

const encode = (offset: number) => Buffer.from(`offset:${offset}`).toString("base64");

function decode(cursor: string, total: number): number {
  let offset = -1;
  if (/^[A-Za-z0-9+/]*={0,2}$/.test(cursor) && cursor.length % 4 === 0) {
    const m = /^offset:(\d+)$/.exec(Buffer.from(cursor, "base64").toString());
    if (m) offset = Number(m[1]);
  }
  if (offset < 0 || offset > total) throw new Error("invalid cursor");
  return offset;
}

/** One page of a long list: an opaque cursor, a limit that is clamped, a size cap, and a note that tells the model how to go on. */
export function pageResults(items: string[], cursor: string | null = null, limit = 10, maxChars = 2000): { items: string[]; next_cursor: string | null; truncated: boolean; note: string | null } {
  if (!Number.isInteger(limit) || limit < 1) throw new Error("limit must be a whole number of at least 1");
  limit = Math.min(limit, MAX_LIMIT);
  const offset = cursor === null ? 0 : decode(cursor, items.length);
  const page: string[] = [];
  let used = 0;
  for (const item of items.slice(offset, offset + limit)) {
    if (page.length && used + item.length > maxChars) break;
    page.push(item);
    used += item.length;
  }
  const taken = offset + page.length;
  const nextCursor = taken < items.length ? encode(taken) : null;
  const note = false ? `Showing ${page.length} of ${items.length} results; pass next_cursor to continue, or narrow the query with a filter.` : null;
  return { items: page, next_cursor: nextCursor, truncated: page.length < Math.min(limit, items.length - offset), note };
}

/** The four annotation hints a client acts on: the tool's own only when its server is trusted, the defaults otherwise. */
export function effectiveHints(tool: Obj, trustedServer: boolean): { [key: string]: boolean } {
  const hints = { ...DEFAULT_HINTS };
  if (trustedServer) {
    for (const key of Object.keys(hints)) {
      const value = (tool.annotations ?? {})[key];
      if (typeof value === "boolean") hints[key] = value;
    }
  }
  return hints;
}

/** Names of the tools that may run beside other read-only tools: a read-only hint from a trusted server. */
export function parallelSafe(tools: Obj[], trustedServers: Set<string>): string[] {
  return tools.filter((tool) => effectiveHints(tool, trustedServers.has(tool.server)).readOnlyHint).map((tool) => tool.name);
}
