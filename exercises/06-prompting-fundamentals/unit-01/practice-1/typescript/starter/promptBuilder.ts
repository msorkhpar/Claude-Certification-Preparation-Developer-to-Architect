// Build a structured prompt from a spec. See ../../statement.md for the exact format.
export interface Doc { name: string; text: string }
export interface Example { input: string; output: string }
export interface Spec {
  task?: string | null;
  role?: string | null;
  context?: string | null;
  documents?: Doc[];
  examples?: Example[];
  constraints?: string[];
  outputFormat?: string | null;
}

export function buildPrompt(spec: Spec, variables: Record<string, string> = {}): string {
  // TODO: render the sections in the order the statement gives.
  return "";
}
