/**
 * A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints.
 *
 * Facts checked on the Claude Code pages "Run Claude Code programmatically" and "CLI reference" and the Agent SDK pages on structured outputs and the result message
 * (read on 2026-10-03, Claude Code v2.1.286): `-p` runs without a prompt for input; `--output-format json` prints one JSON object whose `structured_output` field holds the
 * answer when `--json-schema` is given; a run can end with `is_error` true and a subtype such as `error_max_turns` or `error_max_structured_output_retries`; `--max-turns` exits with an error
 * when the limit is reached; `--bare` skips CLAUDE.md, hooks, skills, MCP servers and auto memory, so the context is passed with `--append-system-prompt-file`. The envelope below is
 * the shape the SDK documents for its result message; nothing here ran the real binary, needed a key or touched the network.
 */
export const SEVERITIES = ["low", "medium", "high"];
export type Schema = { [key: string]: any };
export const REVIEW_SCHEMA: Schema = {
  type: "object",
  properties: { findings: { type: "array", items: { type: "object", properties: {
    file: { type: "string" }, line: { type: "integer" }, category: { type: "string", enum: ["bug", "security", "style", "other"] },
    severity: { type: "string", enum: SEVERITIES }, issue: { type: "string" }, suggested_fix: { type: "string" }, detected_pattern: { type: "string" } },
    required: ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"], additionalProperties: false } } },
  required: ["findings"], additionalProperties: false,
};

/** The argument list of a review run: headless, one JSON object out, the answer checked against a schema, a turn limit, read-only tools, and the project context passed by hand. */
export function buildCommand(prompt: string, schema: Schema, maxTurns = 8, tools = ["Read", "Grep", "Glob", "Bash(git diff *)"], contextFile = "CLAUDE.md"): string[] {
  return ["claude", "--bare", "-p", prompt, "--append-system-prompt-file", contextFile, "--output-format", "json", "--json-schema", JSON.stringify(schema),
    "--max-turns", String(maxTurns), "--allowedTools", tools.join(",")];
}

/** Findings (rule ids) for the text of a shell step that runs `claude`: what a CI run needs and what it must not be allowed. */
export function lintCommand(raw: string): string[] {
  const text = raw.replace(/\\\n\s*/g, " ");
  if (!/\sclaude\b/.test(" " + text)) return ["no-claude-command"];
  const found: string[] = [];
  if (!/\s(-p|--print)\b/.test(text)) found.push("no-print");
  if (!/--output-format[ =]json\b/.test(text)) found.push("no-json");
  if (!/--json-schema\b/.test(text)) found.push("no-schema");
  const turns = /--max-turns[ =](\d+)/.exec(text);
  if (!turns || Number(turns[1]) > 20) found.push("no-turn-limit");
  const allowed = /--allowed[Tt]ools[ =]("[^"]*"|'[^']*'|\S+)/.exec(text);
  const names = allowed ? allowed[1].replace(/^["']|["']$/g, "").match(/[^\s,(]+(?:\([^)]*\))?/g) ?? [] : [];
  if (!allowed) found.push("no-tool-list");
  else if (names.some((n) => ["Bash", "Edit", "Write", "MultiEdit", "NotebookEdit"].includes(n))) found.push("wide-tools");
  if (!text.includes("--bare")) found.push("no-bare");
  else if (!/--append-system-prompt(-file)?\b/.test(text)) found.push("bare-without-context");
  return found;
}

const isKind = (kind: string, value: unknown): boolean =>
  kind === "object" ? typeof value === "object" && value !== null && !Array.isArray(value)
    : kind === "array" ? Array.isArray(value)
    : kind === "string" ? typeof value === "string"
    : kind === "boolean" ? typeof value === "boolean"
    : kind === "null" ? value === null
    : kind === "integer" ? typeof value === "number" && Number.isInteger(value)
    : kind === "number" ? typeof value === "number" : false;

/** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
export function schemaCheck(value: any, schema: Schema, path = "$"): string[] {
  const wants: string[] = Array.isArray(schema.type) ? schema.type : schema.type ? [schema.type] : [];
  if (wants.length > 0 && !wants.some((k) => isKind(k, value))) return [`${path}: expected ${wants.join(" or ")}`];
  const errors: string[] = [];
  if (schema.enum && !schema.enum.includes(value)) errors.push(`${path}: ${JSON.stringify(value)} is not one of ${JSON.stringify(schema.enum)}`);
  if (isKind("object", value)) {
    for (const k of schema.required ?? []) if (!(k in value)) errors.push(`${path}.${k}: is required`);
    if (schema.additionalProperties === false) for (const k of Object.keys(value)) if (!(k in (schema.properties ?? {}))) errors.push(`${path}.${k}: is not allowed`);
    for (const [k, sub] of Object.entries(schema.properties ?? {})) if (k in value) errors.push(...schemaCheck(value[k], sub as Schema, `${path}.${k}`));
  }
  if (Array.isArray(value) && schema.items) value.forEach((item, i) => errors.push(...schemaCheck(item, schema.items, `${path}[${i}]`)));
  return errors;
}

export type Policy = { min_severity: string; disabled_categories: string[]; fail_on: string[] };
export type Gate = { exit: number; comments: Array<{ file: string; line: number; severity: string; body: string }>; problems: string[] };

/** Decide a review job from what `claude -p --output-format json --json-schema ...` printed. A run that failed in any way fails the job: it never passes by saying nothing. */
export function gate(stdout: string, exitCode: number, schema: Schema, policy: Policy): Gate {
  const problems: string[] = [];
  if (exitCode !== 0) problems.push(`claude exited with status ${exitCode}`);
  let envelope: any = null;
  try {
    envelope = JSON.parse(stdout);
  } catch {
    envelope = null;
  }
  if (!isKind("object", envelope)) return { exit: 1, comments: [], problems: [...problems, "the output is not a JSON object"] };
  if (envelope.is_error || envelope.subtype !== "success") problems.push(`the run ended with ${envelope.subtype}`);
  const output = envelope.structured_output;
  if (output === undefined || output === null) problems.push("the result has no structured_output");
  else problems.push(...schemaCheck(output, schema).map((e) => `schema ${e}`));
  if (problems.length > 0) return { exit: 1, comments: [], problems };
  const floor = SEVERITIES.indexOf(policy.min_severity);
  const comments = output.findings
    .filter((f: any) => !policy.disabled_categories.includes(f.category) && SEVERITIES.indexOf(f.severity) >= floor)
    .map((f: any) => ({ file: f.file, line: f.line, severity: f.severity, body: `${f.issue} Suggested fix: ${f.suggested_fix}` }));
  const blocked = comments.some((c: any) => policy.fail_on.includes(c.severity));
  return { exit: blocked ? 1 : 0, comments, problems: [] };
}

export function envelope(over: Record<string, unknown> = {}): string {
  return JSON.stringify({ type: "result", subtype: "success", is_error: false, result: "done", num_turns: 3, total_cost_usd: 0.05, session_id: "s-1", ...over });
}

function main() {
  const command = buildCommand("Review the diff on standard input.", { type: "object", properties: { findings: { type: "array" } } }, 8, ["Read", "Grep"]);
  console.log("command:", command.slice(0, 5).join(" "), "...", JSON.stringify(command.slice(-4)));
  console.log("lint, the good step:", JSON.stringify(lintCommand("git diff main | claude --bare -p 'Review.' --append-system-prompt-file CLAUDE.md --output-format json --json-schema '{}' --max-turns 8 --allowedTools 'Read,Grep'")));
  console.log("lint, a careless step:", JSON.stringify(lintCommand("claude 'Review the change' --allowedTools Bash,Edit")));
  const policy: Policy = { min_severity: "medium", disabled_categories: ["style"], fail_on: ["high"] };
  const findings = [
    { file: "api.py", line: 12, category: "bug", severity: "high", issue: "Unchecked None.", suggested_fix: "Return early.", detected_pattern: "missing-none-check" },
    { file: "api.py", line: 40, category: "style", severity: "high", issue: "Long line.", suggested_fix: "Wrap it.", detected_pattern: "line-length" },
    { file: "ui.py", line: 3, category: "bug", severity: "low", issue: "Odd name.", suggested_fix: "Rename.", detected_pattern: "naming" },
  ];
  const runs: Record<string, [string, number]> = {
    "a finding that blocks": [envelope({ structured_output: { findings } }), 0], "no findings": [envelope({ structured_output: { findings: [] } }), 0],
    "turn limit": [envelope({ subtype: "error_max_turns", is_error: true }), 1], "success without output": [envelope(), 0], "not JSON": ["Error: no key", 1],
    "wrong shape": [envelope({ structured_output: { findings: [{ file: "a.py" }] } }), 0],
  };
  for (const [label, [out, code]] of Object.entries(runs)) {
    const result = gate(out, code, REVIEW_SCHEMA, policy);
    console.log(`${label}: exit ${result.exit}, ${result.comments.length} comment(s)${result.problems.length ? ", " + result.problems[0] : ""}`);
  }
}

if (import.meta.main) main();
