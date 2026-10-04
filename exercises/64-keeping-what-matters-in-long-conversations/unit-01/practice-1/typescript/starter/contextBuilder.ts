/** What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md. */

export function estimateTokens(text: string): number {
  return Math.ceil(text.length / 4);
}

export function trimRecord(record: Record<string, string>, keep: string[]): Record<string, string> | null {
  // TODO: the fields of the record named in keep, in that order, with their exact values.
  return null;
}

export function updateFacts(facts: Record<string, any>, name: string, value: string, asOf: string): Record<string, any> | null {
  // TODO: a new facts map { name: { value, as_of, superseded } }; the input is not changed.
  return null;
}

export function buildContext(customer: string, facts: any[], summary: string, recent: any[]): string | null {
  // TODO: the text of the context: case facts of this customer, then the summary, then the recent messages, each under a heading.
  return null;
}

export function missingFromSummary(summary: string, facts: any[]): string[] | null {
  // TODO: the names of the facts whose value the summary no longer holds.
  return null;
}

export function window(messages: any[], budget: number): any[] | null {
  // TODO: the newest messages that fit the token budget, keeping every tool call together with its result.
  return null;
}
