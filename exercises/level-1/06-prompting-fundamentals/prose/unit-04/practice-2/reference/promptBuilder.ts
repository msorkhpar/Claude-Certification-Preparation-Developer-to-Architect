// Build a structured prompt from a spec. See ../../statement.md for the exact format.
import { logger } from "./logger.ts";
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
  return parts.join("\n\n");
}

function present(value: string | null | undefined): value is string {
  return value !== null && value !== undefined && value.trim() !== "";
}

function lookup(name: string, variables: Record<string, string>): string {
  if (!(name in variables)) throw new Error(`missing variable: ${name}`);
  return String(variables[name]);
}

function escapeText(text: string): string {
  return text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

function renderDocument(index: number, doc: Doc): string {
  const name = escapeText(doc.name).replaceAll('"', "&quot;");
  return `<document index="${index}" name="${name}">\n${escapeText(doc.text)}\n</document>`;
}

function renderExample(index: number, ex: Example, variables: Record<string, string>): string {
  return (
    `<example index="${index}">\n` +
    `${block("input", fill(ex.input, variables))}\n` +
    `${block("output", fill(ex.output, variables))}\n` +
    "</example>"
  );
}

function constraintLines(constraints: string[], variables: Record<string, string>): string {
  return constraints.map((c) => "- " + fill(c, variables)).join("\n");
}

function checkTask(task: string | null | undefined): void {
  if (!present(task)) throw new Error("task is required");
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
