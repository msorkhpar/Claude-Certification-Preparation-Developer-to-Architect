/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */
const SEVERITY: Record<string, number> = { high: 0, medium: 1, low: 2 };
const DOMAINS = ["P1", "P2", "P3", "P4", "P5", "P6", "P7"];

/** TODO: the findings "<severity> <domain> <rule>", high first, then by domain, then by rule id. */
export function launchReview(flags: Set<string>, numbers: Record<string, number>): string[] {
  return [];
}

/** TODO: reject for a high finding, revise for a medium one, otherwise approve. */
export function verdict(findings: string[]): string {
  return "";
}

/** TODO: the number of findings in each domain, P1 to P7. */
export function scorecard(findings: string[]): number[] {
  return [];
}

/** TODO: 100 minus the review cost as a percent of the error cost, rounded up, never below 0; 0 when an error costs nothing. */
export function neededAccuracy(errorCost: number, reviewCost: number): number {
  return -1;
}
