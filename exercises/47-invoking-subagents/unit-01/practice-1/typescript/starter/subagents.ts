// Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md.
import { query } from "@anthropic-ai/claude-agent-sdk";

type Spec = { description?: string; prompt?: string; tools?: string[] | null; model?: string };

export function buildOptions(agents: Record<string, Spec>, cwd: string, cliPath?: string, maxBudgetUsd = 2.0, maxTurns = 20, maxConcurrent = 5): any {
  // TODO: turn the specs into agent definitions and set the tools, limits and environment on the options.
  return null;
}

export function spawned(messages: any[]): any {
  // TODO: list the calls of the tool that starts a subagent.
  return null;
}

export function bySubagent(messages: any[]): any {
  // TODO: group the messages that ran inside a subagent under the call that started it.
  return null;
}

export function makeBrief(task: string, files?: string[], facts?: string[], output?: string): any {
  // TODO: write the prompt a subagent needs, with every fact it cannot get any other way.
  return null;
}

export function packageFinding(claim: string, url?: string, title?: string, page?: number): any {
  // TODO: keep the claim apart from its source.
  return null;
}

export function mergeFindings(findings: any[]): any {
  // TODO: merge repeated claims and keep every source.
  return null;
}

export async function runTeam(prompt: string, options: any): Promise<any> {
  // TODO: collect every message of a single-shot run, and keep what was received when the SDK throws after an error result.
  return null;
}
