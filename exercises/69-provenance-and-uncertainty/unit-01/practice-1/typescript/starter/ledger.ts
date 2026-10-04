/** Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md. */

export const REQUIRED = ["claim", "value", "source", "date"];
export const KINDS = ["financial", "news", "technical"];

export function checkFinding(finding: Record<string, any>): string[] | null {
  // TODO: the names of the required fields that are missing or empty, in the order of REQUIRED.
  return null;
}

export function merge(findings: any[]): any[] | null {
  // TODO: one entry per claim, with its values, their sources and a status; refuse an incomplete finding.
  return null;
}

export function coverageNote(planned: string[], merged: any[], unavailable: Record<string, string>): any {
  // TODO: which claims are well supported, single-source, changed, contested, and which planned claims are gaps.
  return null;
}

export function render(entry: any, kind: string): string | null {
  // TODO: a table for financial data, prose for news, a bulleted list for technical findings.
  return null;
}
