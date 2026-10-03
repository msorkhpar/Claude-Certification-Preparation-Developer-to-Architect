// Session state with the Agent SDK: choose between resuming and starting fresh, say what changed, carry a summary, set the options. See ../../statement.md.
import { query } from "@anthropic-ai/claude-agent-sdk";

export function resolveName(name: string, index: Array<{ id: string; name: string }>): string | null {
  // TODO: the id of the one session with this name; throw an Error for an unknown or a shared name.
  return null;
}

export function planSession(record: any, current: { [path: string]: string }, now: number, fork = false): any {
  // TODO: fresh, fresh_with_summary, resume_with_notice or resume, with the changed, deleted and added files.
  return null;
}

export function changeNotice(plan: any): string | null {
  // TODO: the text that tells a resumed session which files differ.
  return null;
}

export function buildSummary(findings: string[], decisions: string[], openQuestions: string[], files: { [path: string]: string }): string | null {
  // TODO: the fixed-layout summary for a fresh session.
  return null;
}

export function firstPrompt(plan: any, task: string, summary = ""): string | null {
  // TODO: put the notice or the summary in front of the task.
  return null;
}

export function sessionOptions(plan: any, extra: { [key: string]: unknown } = {}): any {
  // TODO: resume by id, fork together with it, nothing for a fresh start.
  return null;
}

export function continueOptions(sessionsInDirectory: unknown[], extra: { [key: string]: unknown } = {}): any {
  // TODO: continue only when exactly one session exists.
  return null;
}

export async function runSession(prompt: string, options: any): Promise<any> {
  // TODO: run one query; return { session_id, result, error }.
  return null;
}
