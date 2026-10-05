import { logger } from "../logger.ts";
const log = logger("review_gate");
/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
import { schemaCheck } from "./schemaCheck.ts";

export const SEVERITIES = ["low", "medium", "high"];

export function reviewPrompt(diff: string, prior: any[] = [], existingTests: string[] = []): string {
  const lines = ["<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed."];
  if (prior.length > 0) lines.push("Do not repeat a finding listed in <already_reported>.");
  if (existingTests.length > 0) lines.push("Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers.");
  lines.push("</instructions>");
  if (prior.length > 0) lines.push("<already_reported>", ...prior.map((p) => `- ${p.file}:${p.line} [${p.category}] ${p.issue}`), "</already_reported>");
  if (existingTests.length > 0) lines.push("<existing_tests>", ...existingTests.map((t) => `- ${t}`), "</existing_tests>");
  lines.push("<diff>", diff, "</diff>");
  return lines.join("\n");
}

export function gate(stdout: string, exitCode: number, schema: any, policy: any): any {
  log.debug("gate input", stdout);
  const problems: string[] = [];
  if (exitCode !== 0) problems.push(`claude exited with status ${exitCode}`);
  let envelope: any = null;
  try {
    envelope = JSON.parse(stdout);
  } catch {
    envelope = null;
  }
  if (typeof envelope !== "object" || envelope === null || Array.isArray(envelope)) return { exit: 1, comments: [], problems: [...problems, "the output is not a JSON object"] };
  if (envelope.is_error || envelope.subtype !== "success") problems.push(`the run ended with ${envelope.subtype}`);
  const output = envelope.structured_output;
  if (output === undefined || output === null) problems.push("the result has no structured_output");
  else problems.push(...schemaCheck(output, schema).map((e: string) => `schema ${e}`));
  if (problems.length > 0) return { exit: 1, comments: [], problems };
  const floor = SEVERITIES.indexOf(policy.min_severity);
  const comments = output.findings
    .filter((f: any) => !policy.disabled_categories.includes(f.category) && SEVERITIES.indexOf(f.severity) >= floor)
    .map((f: any) => ({ file: f.file, line: f.line, severity: f.severity, body: `${f.issue} Suggested fix: ${f.suggested_fix}` }));
  const blocked = comments.some((c: any) => policy.fail_on.includes(c.severity));
  return { exit: blocked ? 1 : 0, comments, problems: [] };
}
