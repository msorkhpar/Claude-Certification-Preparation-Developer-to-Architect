/** When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md. */

export function decide(c: any, maxAttempts = 2): { action: string; reason: string; acknowledge: boolean } | null {
  // TODO: { action: "resolve" | "clarify" | "escalate", reason, acknowledge } for a case.
  return null;
}

export function clarifyingFields(matches: Array<Record<string, string>>): string[] | null {
  // TODO: the fields (never "id") on which the matching records differ, in the order of the first record.
  return null;
}

export function handoffText(c: any): string | null {
  // TODO: the text of a hand-off: six labelled lines, no transcript; an error when the customer id or the issue is missing.
  return null;
}
