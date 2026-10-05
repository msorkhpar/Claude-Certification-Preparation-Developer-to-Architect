import { logger } from "../logger.ts";
const log = logger("recovery");
/** What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md. */

export const STATUSES = ["done", "running", "failed"];

export function addFinding(findings: any[], area: string, fact: string, location: string): any[] {
  // TODO 1 of 6 (finish this to pass m1): the scratchpad's duplicate rule. Receives the findings so far and a new one
  //   (area, fact, location). When a finding with the same area and the same fact already exists, return the findings
  //   unchanged; otherwise return them with the new one added last. Example: adding (api, "uses JWT") twice -> one entry.
  return [...findings, { area, fact, location }];
}

export function renderScratchpad(findings: any[]): string {
  const areas: string[] = [];
  for (const f of findings) if (!areas.includes(f.area)) areas.push(f.area);
  // TODO 2 of 6 (finish this to pass e1): the lines of one area. Receives all the findings and one area. Return one line
  //   "- fact (location)" for each finding of that area only, in order. Example: findings of api and db, area api -> the
  //   api lines only.
  const blocks = areas.map((area) => `## ${area}\n` + findings.map((f) => `- ${f.fact} (${f.location})`).join("\n"));
  return blocks.join("\n\n");
}

export function buildManifest(agents: any[]): any {
  log.debug("buildManifest input", agents);
  const names = agents.map((a) => a.name);
  // TODO 3 of 6 (finish this to pass e2): the checks of the manifest. Refuse with an error when two agents have the same
  //   name, and when an agent's status is not one of STATUSES. Example: two agents named a -> refused; status "paused" ->
  //   refused.
  const sorted = [...agents].sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  return { version: 1, agents: sorted.map((a) => ({ name: a.name, state_file: a.state_file, status: a.status })) };
}

export function resumePlan(manifest: any, existingFiles: Set<string>): Array<[string, string]> {
  return manifest.agents.map((a: any): [string, string] => {
    let action: string;
    // TODO 4 of 6 (finish this to pass e3, e4, e5): the action for one agent. When its state file does not exist,
    //   restart; otherwise when its status is done, reuse it; otherwise resume it from the state file. Example: done with
    //   its file -> reuse; failed with its file -> resume; any status without its file -> restart.
    action = "skip";
    return [a.name, action];
  });
}

export function resumePrompt(task: string, stateLines: string[]): string {
  // TODO 5 of 6 (finish this to pass e6): the resume prompt. Receives the task and the state lines. With no state lines,
  //   return the task alone; otherwise the task, a blank line, "State from the last run:", one "- line" per state line,
  //   and "Continue from the first unfinished step.". Example: [] -> the task.
  return task;
}

export function compactCommand(keep: string[]): string {
  // TODO 6 of 6 (finish this to pass e7): the compact command. Receives the things to keep. Return /compact when there
  //   are none, otherwise "/compact Focus on " followed by them joined with ", ". Example: [auth flow, schema] -> /compact
  //   Focus on auth flow, schema.
  return "/compact";
}
