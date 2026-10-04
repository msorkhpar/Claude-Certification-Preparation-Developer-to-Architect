/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. */

export const CURRENCIES = ["USD", "EUR", "GBP", "other", "unclear"];
export const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]); // models whose API rejects tool_choice any and tool, as read on 2026-10-03

export function validate(record: any, document: string, required: string[] = []): any[] | null {
  // TODO: the errors of a record, each { kind, field, message }: syntax, semantic, ungrounded and absent.
  return null;
}

export function extractDocument(document: string, callModel: (document: string, feedback: any) => any, required: string[] = [], maxRetries = 2): any {
  // TODO: call the model, validate, retry with feedback only for errors a second look can fix, and report a status.
  return null;
}

export function mergeChunks(records: any[]): any {
  // TODO: merge the records of the chunks of one long document, keeping the first value and recording a conflict.
  return null;
}

export function accuracy(results: Record<string, any>, labels: Record<string, any>): any {
  // TODO: the share of documents extracted correctly, measured on all of them and on the validated ones only.
  return null;
}

export function requestChoice(model: string, tools: string[], forced: string | null = null): any {
  // TODO: the tool_choice of the request, with the fallback for models that reject a forced choice.
  return null;
}
