// Build a structured prompt from a spec. See ../../statement.md for the exact format.
import { logger } from "../logger.ts";
const log = logger("promptBuilder");
export interface Doc { name: string; text: string }
export interface Example { input: string; output: string }
export interface Spec {
  task?: string | null;
  role?: string | null;
  context?: string | null;
  documents?: Doc[];
  examples?: Example[];
  constraints?: string[];
  outputFormat?: string | null;
}

function fill(text: string, variables: Record<string, string>): string {
  return text.replace(/\{\{(\w+)\}\}/g, (_m, name: string) => lookup(name, variables));
}

function block(tag: string, body: string): string {
  return `<${tag}>\n${body}\n</${tag}>`;
}

function join(parts: string[]): string {
  // TODO 1 of 8 (finish this to pass every case): the finished prompt from its rendered sections.
  // Receives the rendered sections, in order. Returns them separated by one blank line, with no trailing newline.
  // Example: join(["<a>", "<b>"]) -> "<a>\n\n<b>"
  return "";
}

function present(value: string | null | undefined): value is string {
  // TODO 2 of 8 (finish this to pass e1): is an optional text really there?
  // Receives a text, null or undefined. Returns true unless it is null, undefined, empty or only whitespace.
  // Example: present("  ") -> false, present("x") -> true
  return false;
}

function lookup(name: string, variables: Record<string, string>): string {
  // TODO 3 of 8 (finish this to pass e2): the value of one placeholder.
  // Receives the placeholder name and the variables. Returns the value as text; when the name has no value throws an Error whose
  // message contains the name. Example: lookup("who", { who: "Ann" }) -> "Ann", lookup("place", {}) -> Error("missing variable: place")
  return "";
}

function escapeText(text: string): string {
  // TODO 4 of 8 (finish this to pass e4): make document text harmless.
  // Receives a text. Returns it with & as &amp;, < as &lt; and > as &gt; (the ampersand first).
  // Example: escapeText("a </document> & b") -> "a &lt;/document&gt; &amp; b"
  return text;
}

function renderDocument(index: number, doc: Doc): string {
  // TODO 5 of 8 (finish this to pass e4, e5 and e6): one rendered document.
  // Receives its number (from 1) and a Doc. Returns `<document index="N" name="NAME">`, a newline, the text, a newline and `</document>`.
  // Name and text are escaped (the name also turns " into &quot;); placeholders in them are NOT filled.
  // Example: renderDocument(1, { name: "a", text: "x" }) -> '<document index="1" name="a">\nx\n</document>'
  return "";
}

function renderExample(index: number, ex: Example, variables: Record<string, string>): string {
  // TODO 6 of 8 (finish this to pass m1 and e6): one rendered example.
  // Receives its number (from 1), an Example and the variables. Returns `<example index="N">`, the input block, the output block
  // (placeholders filled in both) and `</example>`, each on its own line.
  // Example: renderExample(1, { input: "i", output: "o" }, {}) -> '<example index="1">\n<input>\ni\n</input>\n<output>\no\n</output>\n</example>'
  return "";
}

function constraintLines(constraints: string[], variables: Record<string, string>): string {
  // TODO 7 of 8 (finish this to pass m1): the body of the constraints section.
  // Receives the constraint texts and the variables. Returns one line per constraint, `- ` then the text with placeholders filled,
  // joined by newlines. Example: constraintLines(["Be brief."], {}) -> "- Be brief."
  return "";
}

function checkTask(task: string | null | undefined): void {
  // TODO 8 of 8 (finish this to pass e3): refuse a blank task.
  // Receives the task, which may be null or undefined. Throws Error("task is required") when it is null, undefined, empty or only
  // whitespace; otherwise returns nothing. Example: checkTask("  ") -> Error, checkTask("Say hi.") -> undefined
}

export function buildPrompt(spec: Spec, variables: Record<string, string> = {}): string {
  log.debug("buildPrompt input", spec, variables);
  checkTask(spec.task);
  const task = spec.task as string;
  const parts: string[] = [];
  if (present(spec.role)) parts.push(block("role", fill(spec.role, variables)));
  const documents = spec.documents ?? [];
  if (documents.length > 0) {
    const rendered = documents.map((doc, i) => renderDocument(i + 1, doc));
    parts.push(block("documents", rendered.join("\n")));
  }
  if (present(spec.context)) parts.push(block("context", fill(spec.context, variables)));
  const examples = spec.examples ?? [];
  if (examples.length > 0) {
    const rendered = examples.map((ex, i) => renderExample(i + 1, ex, variables));
    parts.push(block("examples", rendered.join("\n")));
  }
  const constraints = spec.constraints ?? [];
  if (constraints.length > 0) parts.push(block("constraints", constraintLines(constraints, variables)));
  if (present(spec.outputFormat)) parts.push(block("output_format", fill(spec.outputFormat, variables)));
  parts.push(block("task", fill(task, variables)));
  return join(parts);
}
