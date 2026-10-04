/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. */

const SEVERITY_ORDER: Record<string, number> = { high: 0, medium: 1 };
export const AUTONOMOUS = ["agent", "multi-agent"];

const empty = (value: unknown): boolean => value === undefined || value === null || (Array.isArray(value) && value.length === 0);

export function review(design: any): Array<{ rule: string; severity: string }> {
  const stages = design.stages ?? {};
  const findings: Array<{ rule: string; severity: string }> = [];
  for (const stage of ["input", "processing", "output"]) {
    if (empty(stages[stage])) findings.push({ rule: `missing-stage:${stage}`, severity: "high" });
  }
  if (empty(stages.feedback)) findings.push({ rule: "no-feedback", severity: "high" });
  if ((design.agents ?? 1) > 1 && (design.shared_context || !design.parallel_independent)) findings.push({ rule: "team-without-independence", severity: "high" });
  if (design.writes_without_approval && design.needs_audit) findings.push({ rule: "unapproved-write", severity: "high" });
  if (AUTONOMOUS.includes(design.pattern) && design.path_known) findings.push({ rule: "autonomy-without-need", severity: "medium" });
  if (!empty(stages.output) && !stages.output.includes("validate")) findings.push({ rule: "unvalidated-output", severity: "medium" });
  return findings.sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity] || (a.rule < b.rule ? -1 : a.rule > b.rule ? 1 : 0));
}

export function verdict(findings: Array<{ rule: string; severity: string }>): string {
  const severities = new Set(findings.map((f) => f.severity));
  return severities.has("high") ? "reject" : severities.has("medium") ? "revise" : "approve";
}

export function cheapestAdequate(designs: any[]): string | null {
  const adequate = designs.filter((d) => verdict(review(d)) !== "reject");
  if (adequate.length === 0) return null;
  return [...adequate].sort((a, b) => a.cost - b.cost || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))[0].name;
}
