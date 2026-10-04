/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */

export const STATUSES = ["done", "running", "failed"];

export function addFinding(findings: any[], area: string, fact: string, location: string): any[] | null {
  // TODO: a new list with the finding added unless the same fact is already recorded for the area.
  return null;
}

export function renderScratchpad(findings: any[]): string | null {
  // TODO: Markdown text: one "## <area>" heading per area, in first-seen order, with one line per finding.
  return null;
}

export function buildManifest(agents: any[]): any {
  // TODO: { version: 1, agents: [...] } sorted by name; an error for a duplicate name or an unknown status.
  return null;
}

export function resumePlan(manifest: any, existingFiles: Set<string>): Array<[string, string]> | null {
  // TODO: [name, action] for each agent of the manifest: reuse, resume or restart.
  return null;
}

export function resumePrompt(task: string, stateLines: string[]): string | null {
  // TODO: the prompt that continues a task from the state the last run exported.
  return null;
}

export function compactCommand(keep: string[]): string | null {
  // TODO: the /compact command, telling it what to keep.
  return null;
}
