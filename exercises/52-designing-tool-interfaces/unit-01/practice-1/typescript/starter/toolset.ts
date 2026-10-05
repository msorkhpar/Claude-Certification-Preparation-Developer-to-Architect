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

function nameRules(name: string): Set<string> {
  // TODO 1 of 8 (finish this to pass e1): the rule ids a tool name breaks.
  // Receives the tool's name. Returns a set with "bad-name" when the name does not match NAME in full, and "vague-name" when the name
  // in lower case is in VAGUE. Example: nameRules("Helper") -> Set {"vague-name"}, nameRules("my tool") -> Set {"bad-name"}
  const found = new Set<string>();
  return found;
}

function descriptionRules(description: string): Set<string> {
  // TODO 2 of 8 (finish this to pass e2): the rule ids a description breaks.
  // Receives the description text. Returns a set with "short-description" when it has fewer than 3 sentences (a `.`, `!` or `?` followed
  // by white space or the end) and "no-boundary" when the lower-cased text has none of "do not use", "not for", "instead of".
  // Example: descriptionRules("Gets stuff.") -> Set {"short-description", "no-boundary"}
  const found = new Set<string>();
  return found;
}

function parameterRules(properties: Obj, required: string[]): Set<string> {
  // TODO 3 of 8 (finish this to pass e3): the rule ids the parameters break.
  // Receives the schema's `properties` (name -> spec) and the `required` list. Returns a set with "param-undescribed" when a property has
  // no description or a blank one, and "required-unknown" when `required` names something that is not a property.
  // Example: parameterRules({ q: { type: "string" } }, ["limit"]) -> Set {"param-undescribed", "required-unknown"}
  const found = new Set<string>();
  return found;
}

function listAndHintRules(name: string, properties: Obj, hints: Obj): Set<string> {
  // TODO 4 of 8 (finish this to pass e4): the rule ids a list tool and its annotations break.
  // Receives the name, the `properties` and the tool's `annotations` object. Returns a set with "list-unbounded" when the name starts with
  // a READ_PREFIXES entry and `properties` lacks "limit" or "cursor", and "hint-contradicts-name" when readOnlyHint is true and the name
  // starts with a WRITE_PREFIXES entry, or destructiveHint is false and it starts with a DELETE_PREFIXES entry.
  // Example: listAndHintRules("list_users", { limit: {} }, {}) -> Set {"list-unbounded"}
  const found = new Set<string>();
  return found;
}

/** The rules one tool breaks, sorted and without repeats. */
export function lintTool(tool: Obj): string[] {
  const found = new Set<string>();
  const name = String(tool.name ?? "");
  const description = String(tool.description ?? "");
  const schema = tool.input_schema ?? {};
  const properties: Obj = schema.properties ?? {};
  const required: string[] = schema.required ?? [];
  const specs: Obj[] = Object.values(properties).map((s) => s ?? {});
  nameRules(name).forEach((rule) => found.add(rule));
  descriptionRules(description).forEach((rule) => found.add(rule));
  if (!description.toLowerCase().includes("use when")) found.add("no-use-when");
  parameterRules(properties, required).forEach((rule) => found.add(rule));
  if (specs.some((spec) => spec.type === "string" && !("enum" in spec) && /one of|either/.test(String(spec.description ?? "").toLowerCase()))) found.add("open-set");
  if (Object.entries(properties).some(([key, spec]) => /reasoning|thinking/.test(key.toLowerCase() + " " + String((spec ?? {}).description ?? "").toLowerCase()))) found.add("reasoning-param");
  if ((tool.input_examples ?? []).some((example: any) => !exampleOk(example, properties, required))) found.add("bad-example");
  listAndHintRules(name, properties, tool.annotations ?? {}).forEach((rule) => found.add(rule));
  return [...found].sort();
}

const words = (text: unknown) => new Set(String(text ?? "").toLowerCase().match(/[a-z]{3,}/g) ?? []);

function similar(wa: Set<string>, wb: Set<string>): boolean {
  // TODO 5 of 8 (finish this to pass e5): do two descriptions overlap?
  // Receives two sets of words. Returns true when the share of common words over all the words is at least OVERLAP (0.6), and false when
  // both sets are empty. Example: similar(new Set(["a", "b", "c"]), new Set(["a", "b", "c", "d"])) -> true (3 of 4),
  // similar(new Set(["a", "b"]), new Set(["c", "d"])) -> false
  return false;
}

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
      if (a.name !== b.name && similar(wa, wb)) {
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

function checkLimit(limit: number): number {
  // TODO 6 of 8 (finish this to pass e6): validate and clamp a page limit.
  // Receives the requested limit. Throws an Error unless it is a whole number of at least 1; otherwise returns it cut to MAX_LIMIT.
  // Example: checkLimit(500) -> 50, checkLimit(0) throws
  return limit;
}

function overCap(page: string[], used: number, item: string, maxChars: number): boolean {
  // TODO 7 of 8 (finish this to pass e7): would this item push the page over the size cap?
  // Receives the items already in the page, their total length `used`, the next item and `maxChars`. Returns true when the page is not
  // empty and adding the item would make the total longer than maxChars; the first item is always taken, however long.
  // Example: overCap(["aaaaa"], 5, "bbbbbb", 10) -> true, overCap([], 0, "b".repeat(99), 10) -> false
  return false;
}

/** One page of a long list: an opaque cursor, a limit that is clamped, a size cap, and a note that tells the model how to go on. */
export function pageResults(items: string[], cursor: string | null = null, limit = 10, maxChars = 2000): { items: string[]; next_cursor: string | null; truncated: boolean; note: string | null } {
  limit = checkLimit(limit);
  const offset = cursor === null ? 0 : decode(cursor, items.length);
  const page: string[] = [];
  let used = 0;
  for (const item of items.slice(offset, offset + limit)) {
    if (overCap(page, used, item, maxChars)) break;
    page.push(item);
    used += item.length;
  }
  const taken = offset + page.length;
  const nextCursor = taken < items.length ? encode(taken) : null;
  const note = nextCursor ? `Showing ${page.length} of ${items.length} results; pass next_cursor to continue, or narrow the query with a filter.` : null;
  return { items: page, next_cursor: nextCursor, truncated: page.length < Math.min(limit, items.length - offset), note };
}

/** The four annotation hints a client acts on: the tool's own only when its server is trusted, the defaults otherwise. */
export function effectiveHints(tool: Obj, trustedServer: boolean): { [key: string]: boolean } {
  const hints = { ...DEFAULT_HINTS };
  // TODO 8 of 8 (finish this to pass e8): the four annotation hints a client acts on.
  // `hints` holds the defaults. When the server is trusted, the tool's own boolean value in tool.annotations replaces the default for each
  // of the four keys (values that are not booleans are ignored); an untrusted server keeps the defaults.
  // Example: a tool with readOnlyHint true -> readOnlyHint stays false when untrusted, becomes true when trusted.
  return hints;
}

/** Names of the tools that may run beside other read-only tools: a read-only hint from a trusted server. */
export function parallelSafe(tools: Obj[], trustedServers: Set<string>): string[] {
  return tools.filter((tool) => effectiveHints(tool, trustedServers.has(tool.server)).readOnlyHint).map((tool) => tool.name);
}
