// Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md.
export type Problem = { path: string; message: string };
export type Reply = { content: Array<{ type: string; text?: string }>; stop_reason?: string };
export type Message = { role: "user" | "assistant"; content: string };
export type Ask = (messages: Message[]) => Reply;
export type Result = { status: "ok" | "failed" | "refused" | "truncated"; value: any; attempts: number; errors: Problem[] };

/** No JSON object could be read from the model's text. */
export class ParseError extends Error {}

/** The JSON value in a model reply: the body of a code fence, else the span from the first { to the last }. */
export function parseJson(text: string): any {
  let body = text;
  const fence = text.indexOf("```");
  if (fence !== -1) {
    const start = text.indexOf("\n", fence);
    const end = text.indexOf("```", start !== -1 ? start : fence + 3);
    if (start !== -1 && end !== -1) body = text.slice(start + 1, end);
  }
  const first = body.indexOf("{");
  const last = body.lastIndexOf("}");
  if (first === -1 || last < first) throw new ParseError("no JSON object found in the reply");
  try {
    return JSON.parse(body.slice(first, last + 1));
  } catch (err) {
    throw new ParseError(`invalid JSON: ${(err as Error).message}`);
  }
}

const TYPES: Record<string, (v: any) => boolean> = {
  string: (v) => typeof v === "string",
  integer: (v) => typeof v === "number" && Number.isInteger(v),
  number: (v) => typeof v === "number" && Number.isFinite(v),
  boolean: (v) => typeof v === "boolean",
  array: (v) => Array.isArray(v),
  object: (v) => typeof v === "object" && v !== null && !Array.isArray(v),
  null: (v) => v === null,
};

/** A list of problems, one per way value breaks schema; empty when it conforms. */
export function validate(schema: any, value: any, path = "$"): Problem[] {
  const errors: Problem[] = [];
  if (schema.type !== undefined && !TYPES[schema.type](value)) return [{ path, message: `must be of type ${schema.type}` }];
  if (schema.enum && !schema.enum.includes(value)) errors.push({ path, message: `must be one of ${JSON.stringify(schema.enum)}` });
  if (typeof value === "number") {
    if (schema.minimum !== undefined && value < schema.minimum) errors.push({ path, message: `must be at least ${schema.minimum}` });
    if (schema.maximum !== undefined && value > schema.maximum) errors.push({ path, message: `must be at most ${schema.maximum}` });
  }
  if (TYPES.object(value)) {
    for (const key of schema.required ?? []) if (!(key in value)) errors.push({ path: `${path}.${key}`, message: "is required" });
    const properties = schema.properties ?? {};
    for (const [key, sub] of Object.entries(properties)) if (key in value) errors.push(...validate(sub, value[key], `${path}.${key}`));
    if (schema.additionalProperties === false) for (const key of Object.keys(value)) if (!(key in properties)) errors.push({ path: `${path}.${key}`, message: "is not allowed" });
  }
  if (Array.isArray(value) && schema.items) value.forEach((item, i) => errors.push(...validate(schema.items, item, `${path}[${i}]`)));
  return errors;
}

const textOf = (reply: Reply) => reply.content.filter((b) => b.type === "text").map((b) => b.text).join("");

function prompt(document: string, schema: unknown): string {
  return "Extract the data from the document as one JSON object that follows this JSON Schema. Reply with the JSON only.\n" +
    `<schema>${JSON.stringify(schema)}</schema>\n<document>\n${document}\n</document>`;
}

function feedback(errors: Problem[]): string {
  return `Your reply was rejected:\n${errors.map((e) => `- ${e.path}: ${e.message}`).join("\n")}\nReturn the corrected JSON only.`;
}

/** Ask, parse, validate and, on a problem, re-prompt with the errors, at most maxAttempts calls. */
export function extract(ask: Ask, document: string, schema: any, maxAttempts = 3, evidenceFields: string[] = []): Result {
  let messages: Message[] = [{ role: "user", content: prompt(document, schema) }];
  let errors: Problem[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const reply = ask(messages);
    if (reply.stop_reason === "refusal") return { status: "refused", value: null, attempts: attempt, errors: [] };
    if (reply.stop_reason === "max_tokens") return { status: "truncated", value: null, attempts: attempt, errors: [] };
    const text = textOf(reply);
    let value: any = null;
    try {
      value = parseJson(text);
      errors = validate(schema, value);
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
