// Build a structured prompt from a spec. Reference solution.
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
  return text.replace(/\{\{(\w+)\}\}/g, (_m, name: string) => {
    if (!(name in variables)) throw new Error(`missing variable: ${name}`);
    return String(variables[name]);
  });
}

function escapeText(text: string): string {
  return text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

function block(tag: string, body: string): string {
  return `<${tag}>\n${body}\n</${tag}>`;
}

function present(value: string | null | undefined): value is string {
  return value !== null && value !== undefined && value.trim() !== "";
}

export function buildPrompt(spec: Spec, variables: Record<string, string> = {}): string {
  if (!present(spec.task)) throw new Error("task is required");
  const parts: string[] = [];
  if (present(spec.role)) parts.push(block("role", fill(spec.role, variables)));
  const documents = spec.documents ?? [];
  if (documents.length > 0) {
    const rendered = documents.map((doc, i) => {
      const name = escapeText(doc.name).replaceAll('"', "&quot;");
      return `<document index="${i + 1}" name="${name}">\n${doc.text}\n</document>`;
    });
    parts.push(block("documents", rendered.join("\n")));
  }
  if (present(spec.context)) parts.push(block("context", fill(spec.context, variables)));
  const examples = spec.examples ?? [];
  if (examples.length > 0) {
    const rendered = examples.map(
      (ex, i) =>
        `<example index="${i + 1}">\n` +
        `${block("input", fill(ex.input, variables))}\n` +
        `${block("output", fill(ex.output, variables))}\n` +
        "</example>",
    );
    parts.push(block("examples", rendered.join("\n")));
  }
  const constraints = spec.constraints ?? [];
  if (constraints.length > 0) {
    parts.push(block("constraints", constraints.map((c) => "- " + fill(c, variables)).join("\n")));
  }
  if (present(spec.outputFormat)) parts.push(block("output_format", fill(spec.outputFormat, variables)));
  parts.push(block("task", fill(spec.task, variables)));
  return parts.join("\n\n");
}
