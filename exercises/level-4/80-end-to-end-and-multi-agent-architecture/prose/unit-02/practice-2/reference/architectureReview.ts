/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. */
import { logger } from "./logger.ts";
const log = logger("architecture_review");

type Finding = { rule: string; severity: string };

const SEVERITY_ORDER: Record<string, number> = { high: 0, medium: 1 };
export const AUTONOMOUS = ["agent", "multi-agent"];

const empty = (value: unknown): boolean => value === undefined || value === null || (Array.isArray(value) && value.length === 0);

const finding = (rule: string, severity: string): Finding => ({ rule, severity });

/** missing-stage:<stage> for each absent stage, then no-feedback when the feedback stage is absent; all high. */
export function stageFindings(stages: any): Finding[] {
  const found = ["input", "processing", "output"].filter((stage) => empty(stages[stage])).map((stage) => finding(`missing-stage:${stage}`, "high"));
  if (empty(stages.feedback)) found.push(finding("no-feedback", "high"));
  return found;
}

/** team-without-independence (high) for more than one agent with a shared context or parts that are not independent. */
export function teamFindings(design: any): Finding[] {
  return (design.agents ?? 1) > 1 && (design.shared_context || !design.parallel_independent) ? [finding("team-without-independence", "high")] : [];
}

/** unapproved-write (high) when the design writes without approval and an audit is needed. */
export function writeFindings(design: any): Finding[] {
  return design.writes_without_approval && design.needs_audit ? [finding("unapproved-write", "high")] : [];
}

/** autonomy-without-need (medium) when an agent or a team is used on a known path. */
export function autonomyFindings(design: any): Finding[] {
  return AUTONOMOUS.includes(design.pattern) && design.path_known ? [finding("autonomy-without-need", "medium")] : [];
}

/** unvalidated-output (medium) when the output stage is present and has no validate step. */
export function outputFindings(stages: any): Finding[] {
  return !empty(stages.output) && !stages.output.includes("validate") ? [finding("unvalidated-output", "medium")] : [];
}

/** High first, then by rule name. */
export function orderFindings(findings: Finding[]): Finding[] {
  return [...findings].sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity] || (a.rule < b.rule ? -1 : a.rule > b.rule ? 1 : 0));
}

/** reject for any high finding, revise for any medium finding, otherwise approve. */
export function verdict(findings: Finding[]): string {
  const severities = new Set(findings.map((f) => f.severity));
  return severities.has("high") ? "reject" : severities.has("medium") ? "revise" : "approve";
}

export function review(design: any): Finding[] {
  log.debug("review input", design);
  const stages = design.stages ?? {};
  return orderFindings([...stageFindings(stages), ...teamFindings(design), ...writeFindings(design), ...autonomyFindings(design), ...outputFindings(stages)]);
}

/** The name of the cheapest design whose verdict is not reject; ties go to the lower name; null when none qualifies. */
export function cheapestAdequate(designs: any[]): string | null {
  const adequate = designs.filter((d) => verdict(review(d)) !== "reject");
  if (adequate.length === 0) return null;
  return [...adequate].sort((a, b) => a.cost - b.cost || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))[0].name;
}
