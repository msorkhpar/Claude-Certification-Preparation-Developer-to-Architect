/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. */

const SEVERITY_ORDER: Record<string, number> = { high: 0, medium: 1 }; // high findings come first
export const AUTONOMOUS = ["agent", "multi-agent"];

export function review(design: any): Array<{ rule: string; severity: string }> | null {
  // TODO: the findings of the rubric, each { rule, severity }, ordered by severity and then by rule.
  return null;
}

export function verdict(findings: Array<{ rule: string; severity: string }>): string | null {
  // TODO: "reject", "revise" or "approve" from the worst severity among the findings.
  return null;
}

export function cheapestAdequate(designs: any[]): string | null {
  // TODO: the name of the cheapest design that is not rejected, or null.
  return null;
}
