import { logger } from "../logger.ts";
const log = logger("review_gate");
/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
import { schemaCheck } from "./schemaCheck.ts";

export const SEVERITIES = ["low", "medium", "high"];

export function reviewPrompt(diff: string, prior: any[] = [], existingTests: string[] = []): string {
  // TODO 1 of 9 (finish this to pass e4): the first instructions of the prompt. Add the sentence "Report only findings
  //   that are new or still unaddressed." as the third line of the instructions, after the line that points to the project
  //   criteria. Example: the prompt starts with <instructions>, the review line, then this sentence.
  const lines = ["<instructions>", "Review the change in <diff> against the criteria in the project instructions."];
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
  // TODO 2 of 9 (finish this to pass e1): the exit status. When claude exited with a status other than 0, add the
  //   problem "claude exited with status N" (the job fails whatever the output looks like). Example: exit 2 and a perfect
  //   answer -> exit 1 and that problem.
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
  // TODO 3 of 9 (finish this to pass e2): the schema check. Check the structured output against the schema with the
  //   provided helper and add one problem per error, as "schema " followed by the error (which names the path). Example:
  //   severity "critical" -> "schema $.findings[0].severity: ...".
  if (problems.length > 0) return { exit: 1, comments: [], problems };
  // TODO 4 of 9 (finish this to pass m1): the comments of a valid answer. Keep each finding whose severity is at or
  //   above policy min_severity (the order is low, medium, high) and whose category is not in disabled_categories. Return
  //   one comment {file, line, severity, body} per kept finding, in order, with the body "ISSUE Suggested fix: FIX".
  //   Example: a medium bug at api.py:12 with floor medium -> one comment.
  const comments: any[] = [];
  // TODO 5 of 9 (finish this to pass e3): the decision. The job fails (exit 1) when at least one posted comment has a
  //   severity listed in policy fail_on; otherwise it only comments (exit 0). Example: fail_on [high], one posted medium
  //   comment -> exit 0.
  const blocked = false;
  return { exit: blocked ? 1 : 0, comments, problems: [] };
}
