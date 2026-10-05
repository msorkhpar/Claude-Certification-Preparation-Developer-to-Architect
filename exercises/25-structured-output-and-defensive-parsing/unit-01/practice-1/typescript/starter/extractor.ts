// Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("extractor");
export type Problem = { path: string; message: string };
export type Reply = { content: Array<{ type: string; text?: string }>; stop_reason?: string };
export type Message = { role: "user" | "assistant"; content: string };
export type Ask = (messages: Message[]) => Reply;
export type Result = { status: "ok" | "failed" | "refused" | "truncated"; value: any; attempts: number; errors: Problem[] };

/** No JSON object could be read from the model's text. */
export class ParseError extends Error {}

/** The index of the first { and of the last } in the text; -1 for a brace that is not there. */
function objectSpan(body: string): [number, number] {
  // TODO 1 of 8 (finish this to pass m1, e1 and e3): where the JSON object sits in the text.
  // Receives the text. Returns [first, last]: the index of the first `{` and of the last `}`, or -1 for a brace that is not there.
  // Example: objectSpan('Sure! {"a": 1} Done.') -> [6, 13]
  return [-1, -1];
}

/** The JSON value in a model reply: the body of a code fence, else the span from the first { to the last }. */
export function parseJson(text: string): any {
  log.debug("parseJson input", text);
  let body = text;
  const fence = text.indexOf("```");
  if (fence !== -1) {
    const start = text.indexOf("\n", fence);
    const end = text.indexOf("```", start !== -1 ? start : fence + 3);
    if (start !== -1 && end !== -1) body = text.slice(start + 1, end);
  }
  const [first, last] = objectSpan(body);
  if (first === -1 || last < first) throw new ParseError("no JSON object found in the reply");
  try {
    return JSON.parse(body.slice(first, last + 1));
  } catch (err) {
    throw new ParseError(`invalid JSON: ${(err as Error).message}`);
  }
}

/** Is this value an integer for the schema? A number with no fraction; never a boolean. */
function isInteger(v: any): boolean {
  // TODO 2 of 8 (finish this to pass e6): is this value an integer for the schema?
  // Receives any value. Returns true for a number with no fraction (2 and 2.0), false for 2.5, for a boolean and for a string.
  // Example: isInteger(2.5) -> false, isInteger(true) -> false
  return false;
}

const TYPES: Record<string, (v: any) => boolean> = {
  string: (v) => typeof v === "string",
  integer: isInteger,
  number: (v) => typeof v === "number" && Number.isFinite(v),
  boolean: (v) => typeof v === "boolean",
  array: (v) => Array.isArray(v),
  object: (v) => typeof v === "object" && v !== null && !Array.isArray(v),
  null: (v) => v === null,
};

/** The problems of a number outside minimum and maximum. */
function rangeErrors(schema: any, value: number, path: string): Problem[] {
  // TODO 3 of 8 (finish this to pass e2): the problems of a number outside minimum and maximum.
  // Receives the schema, a number and its path. Returns { path, message } problems: "must be at least <minimum>" first, then
  // "must be at most <maximum>", each only when the schema has that key and the value breaks it.
  // Example: rangeErrors({ minimum: 0 }, -1, "$.total") -> [{ path: "$.total", message: "must be at least 0" }]
  return [];
}

/** The problems of missing required keys. */
function requiredErrors(schema: any, value: any, path: string): Problem[] {
  // TODO 4 of 8 (finish this to pass e2): the problems of missing required keys.
  // Receives the schema, an object and its path. Returns one { path: "<path>.<key>", message: "is required" } per key of the schema's
  // `required` list that the object lacks, in the schema's order.
  // Example: requiredErrors({ required: ["a"] }, {}, "$") -> [{ path: "$.a", message: "is required" }]
  return [];
}

/** The problems of keys the schema does not list, when additionalProperties is false. */
function extraErrors(schema: any, value: any, path: string): Problem[] {
  // TODO 5 of 8 (finish this to pass e2): the problems of keys the schema does not list.
  // Receives the schema, an object and its path. When `additionalProperties` is false, returns one
  // { path: "<path>.<key>", message: "is not allowed" } per key that is not in `properties`; otherwise an empty list.
  // Example: extraErrors({ additionalProperties: false, properties: {} }, { x: 1 }, "$") -> [{ path: "$.x", message: "is not allowed" }]
  return [];
}

/** A list of problems, one per way value breaks schema; empty when it conforms. */
export function validate(schema: any, value: any, path = "$"): Problem[] {
  const errors: Problem[] = [];
  if (schema.type !== undefined && !TYPES[schema.type](value)) return [{ path, message: `must be of type ${schema.type}` }];
  if (schema.enum && !schema.enum.includes(value)) errors.push({ path, message: `must be one of ${JSON.stringify(schema.enum)}` });
  if (typeof value === "number") errors.push(...rangeErrors(schema, value, path));
  if (TYPES.object(value)) {
    errors.push(...requiredErrors(schema, value, path));
    const properties = schema.properties ?? {};
    for (const [key, sub] of Object.entries(properties)) if (key in value) errors.push(...validate(sub, value[key], `${path}.${key}`));
    errors.push(...extraErrors(schema, value, path));
  }
  if (Array.isArray(value) && schema.items) value.forEach((item, i) => errors.push(...validate(schema.items, item, `${path}[${i}]`)));
  return errors;
}

const textOf = (reply: Reply) => reply.content.filter((b) => b.type === "text").map((b) => b.text).join("");

function prompt(document: string, schema: unknown): string {
  return "Extract the data from the document as one JSON object that follows this JSON Schema. Reply with the JSON only.\n" +
    `<schema>${JSON.stringify(schema)}</schema>\n<document>\n${document}\n</document>`;
}

/** The message that sends the problems back to the model. */
function feedback(errors: Problem[]): string {
  // TODO 6 of 8 (finish this to pass e2): the message that sends the problems back to the model.
  // Receives the list of problems. Returns "Your reply was rejected:", then one "- <path>: <message>" line per problem, then
  // "Return the corrected JSON only.", each on its own line.
  // Example: [{ path: "$.total", message: "must be of type number" }] -> "Your reply was rejected:\n- $.total: must be of type number\nReturn the corrected JSON only."
  return "";
}

/** The quotes the document does not contain. */
function groundingErrors(value: any, document: string, evidenceFields: string[]): Problem[] {
  // TODO 7 of 8 (finish this to pass e5): the quotes the document does not contain.
  // Receives the parsed value, the document text and the names of the evidence fields. For each name whose value in the object is a
  // string that does not occur in the document, returns { path: "$.<name>", message: "is not found in the document" }.
  // Example: groundingErrors({ quote: "x" }, "abc", ["quote"]) -> [{ path: "$.quote", message: "is not found in the document" }]
  return [];
}

/** "refused" or "truncated" for a reply that must not be retried, else null. */
function earlyStatus(reply: Reply): "refused" | "truncated" | null {
  // TODO 8 of 8 (finish this to pass e4): a reply that must not be retried.
  // Receives a reply. Returns "refused" when its stop_reason is "refusal", "truncated" when it is "max_tokens", else null.
  // Example: earlyStatus({ content: [], stop_reason: "max_tokens" }) -> "truncated"
  // The starter says "truncated" for every reply, so it stops after one call; write the real rule.
  return "truncated";
}

/** Ask, parse, validate and, on a problem, re-prompt with the errors, at most maxAttempts calls. */
export function extract(ask: Ask, document: string, schema: any, maxAttempts = 3, evidenceFields: string[] = []): Result {
  let messages: Message[] = [{ role: "user", content: prompt(document, schema) }];
  let errors: Problem[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const reply = ask(messages);
    const early = earlyStatus(reply);
    if (early) return { status: early, value: null, attempts: attempt, errors: [] };
    const text = textOf(reply);
    let value: any = null;
    try {
      value = parseJson(text);
      errors = validate(schema, value);
      errors.push(...groundingErrors(value, document, evidenceFields));
    } catch (err) {
      if (!(err instanceof ParseError)) throw err;
      value = null;
      errors = [{ path: "$", message: err.message }];
    }
    if (errors.length === 0) return { status: "ok", value, attempts: attempt, errors: [] };
    if (attempt < maxAttempts) messages = [...messages, { role: "assistant", content: text }, { role: "user", content: feedback(errors) }];
  }
  return { status: "failed", value: null, attempts: maxAttempts, errors };
}
