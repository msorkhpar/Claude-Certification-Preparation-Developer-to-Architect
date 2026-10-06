import { logger } from "./logger.ts";
const log = logger("state_manifest");
/**
 * Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.
 *
 * The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
 * restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a dictionary
 * stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
 */
export type Fs = Record<string, string>;
const MANIFEST = "state/manifest.txt";
const TRANSCRIPT_CHARS: Record<string, number> = { auth: 3200, billing: 2400, search: 1600 };

export function tokens(textOrChars: string | number): number {
  const chars = typeof textOrChars === "number" ? textOrChars : textOrChars.length;
  return Math.ceil(chars / 4);
}

export function readManifest(fs: Fs): Record<string, [string, string]> {
  const entries: Record<string, [string, string]> = {};
  for (const line of (fs[MANIFEST] ?? "").split("\n").filter((l) => l !== "")) {
    const [name, status, path] = line.split("|");
    entries[name] = [status, path];
  }
  return entries;
}

function writeManifest(fs: Fs, entries: Record<string, [string, string]>): void {
  fs[MANIFEST] = Object.entries(entries).map(([name, [status, path]]) => `${name}|${status}|${path}`).join("\n");
}

/** The manifest is written when the agent starts, so that a crash leaves a trace. */
export function start(fs: Fs, agent: string): void {
  const entries = readManifest(fs);
  entries[agent] = ["running", `state/${agent}.md`];
  writeManifest(fs, entries);
}

/** The state file is written when the agent has something to keep, and the manifest then says done. */
export function finish(fs: Fs, agent: string, findings: Array<[string, string]>): void {
  const entries = readManifest(fs);
  const path = entries[agent][1];
  fs[path] = findings.map(([fact, where]) => `- ${fact} (${where})`).join("\n");
  entries[agent] = ["done", path];
  writeManifest(fs, entries);
}

export function recoveryPlan(fs: Fs, planned: string[]): Array<[string, string]> {
  const entries = readManifest(fs);
  return planned.map((agent): [string, string] => {
    const [status, path] = entries[agent] ?? ["running", `state/${agent}.md`];
    return [agent, !(path in fs) ? "restart" : status === "done" ? "reuse" : "resume"];
  });
}

/** What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again. */
export function injectedState(fs: Fs, plan: Array<[string, string]>): string {
  const entries = readManifest(fs);
  return plan.filter(([, action]) => action === "reuse" || action === "resume").map(([agent]) => `${agent}:\n${fs[entries[agent][1]]}`).join("\n");
}

function main() {
  const fs: Fs = {};
  const work: Record<string, Array<[string, string]>> = {
    auth: [["sessions expire after 30 minutes", "auth/Session.java:18"], ["tokens are signed in TokenSigner", "auth/TokenSigner.java:12"], ["the login route is POST /login", "auth/Routes.java:7"]],
    billing: [["amounts are integer cents", "billing/Money.java:5"], ["refunds go through RefundService", "billing/RefundService.java:41"]],
  };
  for (const [agent, findings] of Object.entries(work)) {
    start(fs, agent);
    finish(fs, agent, findings);
    console.log(`${agent}: exported ${readManifest(fs)[agent][1]} (${findings.length} findings), manifest says ${readManifest(fs)[agent][0]}`);
  }
  start(fs, "search");
  console.log("search: manifest says running, state file never written (crash)");
  const plan = recoveryPlan(fs, ["auth", "billing", "search"]);
  console.log("recovery plan: " + plan.map(([agent, action]) => `${agent} ${action}`).join(", "));
  const state = injectedState(fs, plan);
  const replay = Object.values(TRANSCRIPT_CHARS).reduce((sum, n) => sum + tokens(n), 0);
  console.log(`injected into the next phase: ${Object.values(work).reduce((s, f) => s + f.length, 0)} findings from ${Object.keys(work).length} agents, about ${tokens(state)} tokens`);
  console.log(`replaying the three transcripts instead: about ${replay} tokens`);
}

if (import.meta.main) main();
