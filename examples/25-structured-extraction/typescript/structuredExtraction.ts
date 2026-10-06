// Structured outputs plus the checks a schema cannot make, against a scripted model.
// The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
// The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("structured_extraction");

export const MODEL = "claude-sonnet-5-5";
export const LOCAL_SCHEMA: any = {
  type: "object",
  properties: {
    vendor: { type: "string" },
    total: { type: "number", minimum: 0 },
    currency: { type: "string", enum: ["USD", "EUR", "GBP"] },
    evidence: { type: "string" },
  },
  required: ["vendor", "total", "currency", "evidence"],
  additionalProperties: false,
};
const UNSUPPORTED = ["minimum", "maximum", "multipleOf", "minLength", "maxLength"];

/** The schema without the constraints that structured outputs reject; they move to the field's description. */
export function forApi(schema: any): any {
  const out = structuredClone(schema);
  const walk = (node: any) => {
    const notes = UNSUPPORTED.filter((k) => k in node).map((k) => {
      const note = `${k} ${node[k]}`;
      delete node[k];
      return note;
    });
    if (notes.length) node.description = `${node.description ?? ""} (${notes.join(", ")})`.trim();
    for (const sub of Object.values(node.properties ?? {})) walk(sub);
  };
  walk(out);
  return out;
}

/** What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document. */
export function problems(value: any, document: string): string[] {
  const found: string[] = [];
  if (typeof value.total !== "number" || value.total < 0) found.push("$.total: must be a number of at least 0");
  if (!LOCAL_SCHEMA.properties.currency.enum.includes(value.currency)) found.push(`$.currency: must be one of USD, EUR, GBP, not ${JSON.stringify(value.currency)}`);
  if (!document.includes(value.evidence)) found.push("$.evidence: is not found in the document");
  return found;
}

/** Structured outputs may change the capital letters of an enum value; compare without them. */
export function normaliseEnum(value: any): any {
  const allowed = new Map<string, string>((LOCAL_SCHEMA.properties.currency.enum as string[]).map((e) => [e.toLowerCase(), e]));
  if (typeof value.currency === "string" && allowed.has(value.currency.toLowerCase())) return { ...value, currency: allowed.get(value.currency.toLowerCase()) };
  return value;
}

export async function extract(client: Anthropic, document: string, maxAttempts = 2): Promise<Record<string, unknown>> {
  const messages: Anthropic.MessageParam[] = [{ role: "user", content: `Extract the invoice data.\n<document>\n${document}\n</document>` }];
  let errors: string[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const reply = await client.messages.create({ model: MODEL, max_tokens: 300, messages, output_config: { format: { type: "json_schema", schema: forApi(LOCAL_SCHEMA) } } });
    if (reply.stop_reason === "refusal" || reply.stop_reason === "max_tokens") return { status: reply.stop_reason === "refusal" ? "refused" : "truncated", attempts: attempt };
    const raw = (reply.content[0] as { text: string }).text;
    const value = normaliseEnum(JSON.parse(raw));
    errors = problems(value, document);
    if (errors.length === 0) return { status: "ok", attempts: attempt, value };
    messages.push({ role: "assistant", content: raw }, { role: "user", content: `Rejected:\n${errors.join("\n")}\nReturn corrected JSON.` });
  }
  return { status: "failed", attempts: maxAttempts, errors };
}

export const DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";

export const body = (fields: Record<string, unknown> = {}) => JSON.stringify({ vendor: "Acme Tools", total: 120.5, currency: "EUR", evidence: "Total due: 120.50 EUR", ...fields });

const usage = { input_tokens: 1, output_tokens: 1 };
export const REPLIES = () => [
  { body: message([text(body({ currency: "Eur" }))], "end_turn", usage, MODEL) },
  { body: message([text(body({ evidence: "Total due: 999.00 USD" }))], "end_turn", usage, MODEL) },
  { body: message([text(body())], "end_turn", usage, MODEL) },
  { body: message([text("I can't help with that.")], "refusal", usage, MODEL) },
  { body: message([text('{"vendor": "Acme')], "max_tokens", usage, MODEL) },
];

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

/** Python's repr of a result dict, so that both languages print the same line. */
const show = (r: Record<string, unknown>) => `{${Object.entries(r).map(([k, v]) => `'${k}': ${typeof v === "object" ? pyRepr(v) : typeof v === "string" ? `'${v}'` : v}`).join(", ")}}`;
function pyRepr(v: any): string {
  if (Array.isArray(v)) return `[${v.map(pyRepr).join(", ")}]`;
  if (v && typeof v === "object") return `{${Object.entries(v).map(([k, x]) => `'${k}': ${pyRepr(x)}`).join(", ")}}`;
  return typeof v === "string" ? `'${v}'` : String(v);
}

async function main() {
  const { fake, client } = clientFor(REPLIES());
  console.log("schema sent to the API:", JSON.stringify(forApi(LOCAL_SCHEMA).properties.total));
  for (const n of [1, 2]) console.log(`document ${n}:`, show(await extract(client, DOC)));
  console.log("document 3:", show(await extract(client, DOC)));
  console.log("document 4:", show(await extract(client, DOC)));
  console.log("requests sent:", fake.seen.length, "| each carried output_config.format.type:", `{'${[...new Set(fake.seen.map((r) => r.body.output_config.format.type))].join("', '")}'}`);
  console.log("second call of document 2 sent:", pyRepr(fake.seen[2].body.messages.map((m: any) => m.role)), "| feedback:", fake.seen[2].body.messages.at(-1).content.split("\n")[1]);
}

if (import.meta.main) await main();
