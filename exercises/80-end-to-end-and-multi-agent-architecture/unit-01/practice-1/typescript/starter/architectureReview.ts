/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("architecture_review");

type Finding = { rule: string; severity: string };

const SEVERITY_ORDER: Record<string, number> = { high: 0, medium: 1 };
export const AUTONOMOUS = ["agent", "multi-agent"];

const empty = (value: unknown): boolean => value === undefined || value === null || (Array.isArray(value) && value.length === 0);

const finding = (rule: string, severity: string): Finding => ({ rule, severity });

/**
 * TODO 1 of 8 (unlocks e1): the findings about absent stages.
 * Receives the design's `stages` object. Returns a list of findings: `missing-stage:input`, `missing-stage:processing` and `missing-stage:output` for each of
 * those stages that is absent or empty, then `no-feedback` when `feedback` is absent or empty; every one with severity `high`.
 * Example: { input: ["parse"], processing: ["act"], output: ["send"] } -> [{ rule: "no-feedback", severity: "high" }]
 */
export function stageFindings(stages: any): Finding[] {
  return [];
}

/**
 * TODO 2 of 8 (unlocks e3): the finding about a team.
 * Receives the design. Returns [`team-without-independence`, severity `high`] when there is more than one agent (`agents`, default 1) and either
 * `shared_context` is true or `parallel_independent` is not true; otherwise an empty list.
 * Example: { agents: 3, shared_context: true, parallel_independent: true } -> one finding; { agents: 1, shared_context: true } -> []
 */
export function teamFindings(design: any): Finding[] {
  return [];
}

/**
 * TODO 3 of 8 (unlocks e4): the finding about an unapproved write.
 * Receives the design. Returns [`unapproved-write`, severity `high`] when `writes_without_approval` and `needs_audit` are both true; otherwise an empty list.
 * Example: { writes_without_approval: true, needs_audit: false } -> []
 */
export function writeFindings(design: any): Finding[] {
  return [];
}

/**
 * TODO 4 of 8 (unlocks e2): the finding about autonomy.
 * Receives the design. Returns [`autonomy-without-need`, severity `medium`] when `pattern` is in `AUTONOMOUS` and `path_known` is true; otherwise an empty list.
 * Example: { pattern: "agent", path_known: false } -> []
 */
export function autonomyFindings(design: any): Finding[] {
  return [];
}

/**
 * TODO 5 of 8 (unlocks e5): the finding about unvalidated output.
 * Receives the design's `stages` object. Returns [`unvalidated-output`, severity `medium`] when the `output` stage is present and does not contain `validate`;
 * otherwise an empty list (an absent output stage is already reported as a missing stage).
 * Example: { output: ["send"] } -> one finding; { output: ["validate", "send"] } -> []
 */
export function outputFindings(stages: any): Finding[] {
  return [];
}

/**
 * TODO 6 of 8 (unlocks e6): order the findings.
 * Receives a list of findings. Returns them ordered by severity (`high` before `medium`, see `SEVERITY_ORDER`) and then by rule name.
 * Example: [medium "autonomy-without-need", high "no-feedback"] -> the high one first
 */
export function orderFindings(findings: Finding[]): Finding[] {
  return findings;
}

/**
 * TODO 7 of 8 (unlocks m1 and e6): the verdict.
 * Receives a list of findings. Returns `reject` when any is `high`, otherwise `revise` when any is `medium`, otherwise `approve`.
 * Example: verdict([]) -> "approve"
 */
export function verdict(findings: Finding[]): string {
  return "";
}

export function review(design: any): Finding[] {
  log.debug("review input", design);
  const stages = design.stages ?? {};
  return orderFindings([...stageFindings(stages), ...teamFindings(design), ...writeFindings(design), ...autonomyFindings(design), ...outputFindings(stages)]);
}

/**
 * TODO 8 of 8 (unlocks e7): the cheapest design that is not rejected.
 * Receives a list of designs, each with `name` and `cost`. Returns the name of the one with the lowest `cost` among those whose `verdict(review(d))` is not
 * `reject`; equal costs go to the lower name; `null` when none qualifies or the list is empty.
 * Example: costs 2 (name "zeta") and 2 (name "alpha"), both sound -> "alpha"
 */
export function cheapestAdequate(designs: any[]): string | null {
  return null;
}
