/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. */

export const VAGUE = ["be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment"]; // phrases that name no pattern

export function buildReviewPrompt(spec: any, diff: string): string | null {
  // TODO: refuse a specification that is vague or incomplete (throw an Error), then write the prompt: criteria, examples, diff last.
  return null;
}

export function categoryReport(findings: any[], minReviewed = 5, minPrecision = 0.5): any {
  // TODO: per category the number reviewed, the precision, whether to disable it and its most dismissed patterns.
  return null;
}

export function nextStep(request: any, required: string[], defaults: Record<string, string>, attended: boolean): any {
  // TODO: proceed, ask or stop for a request with missing fields, and state the assumptions made.
  return null;
}
