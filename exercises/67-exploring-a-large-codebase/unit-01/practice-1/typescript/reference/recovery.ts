import { logger } from "../logger.ts";
const log = logger("recovery");
/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */

export const STATUSES = ["done", "running", "failed"];

export function addFinding(findings: any[], area: string, fact: string, location: string): any[] {
  if (findings.some((f) => f.area === area && f.fact === fact)) return [...findings];
  return [...findings, { area, fact, location }];
}

export function renderScratchpad(findings: any[]): string {
  const areas: string[] = [];
  for (const f of findings) if (!areas.includes(f.area)) areas.push(f.area);
  const blocks = areas.map((area) => `## ${area}\n` + findings.filter((f) => f.area === area).map((f) => `- ${f.fact} (${f.location})`).join("\n"));
  return blocks.join("\n\n");
}

export function buildManifest(agents: any[]): any {
  log.debug("buildManifest input", agents);
  const names = agents.map((a) => a.name);
  if (new Set(names).size !== names.length) throw new Error("duplicate agent name");
  for (const a of agents) if (!STATUSES.includes(a.status)) throw new Error(`unknown status ${a.status}`);
  const sorted = [...agents].sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  return { version: 1, agents: sorted.map((a) => ({ name: a.name, state_file: a.state_file, status: a.status })) };
}

export function resumePlan(manifest: any, existingFiles: Set<string>): Array<[string, string]> {
  return manifest.agents.map((a: any): [string, string] => {
    let action: string;
    if (!existingFiles.has(a.state_file)) action = "restart";
    else if (a.status === "done") action = "reuse";
    else action = "resume";
    return [a.name, action];
  });
}

export function resumePrompt(task: string, stateLines: string[]): string {
  if (stateLines.length === 0) return task;
  return task + "\n\nState from the last run:\n" + stateLines.map((line) => `- ${line}`).join("\n") + "\nContinue from the first unfinished step.";
}

export function compactCommand(keep: string[]): string {
  return keep.length === 0 ? "/compact" : "/compact Focus on " + keep.join(", ");
}
