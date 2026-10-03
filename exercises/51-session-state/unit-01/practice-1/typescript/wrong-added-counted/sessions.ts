// Session state with the Agent SDK: choose between resuming and starting fresh, say what changed, carry a summary, set the options. See ../../statement.md.
import { query } from "@anthropic-ai/claude-agent-sdk";

const WEEK_SECONDS = 7 * 24 * 3600; // a session idle for longer than this is not resumed
const STALE_SHARE = 0.5; // more than this share of the analysed files changed or gone: start fresh

type Saved = { id: string; name?: string; last_used: number; files: { [path: string]: string } };

/** The id of the one session with this exact name; an unknown or a shared name is refused. */
export function resolveName(name: string, index: Array<{ id: string; name: string }>): string {
  const ids = index.filter((entry) => entry.name === name).map((entry) => entry.id);
  if (ids.length === 0) throw new Error(`no session is named ${name}`);
  if (ids.length > 1) throw new Error(`${ids.length} sessions are named ${name}: resume by id`);
  return ids[0];
}

/** Decide how to continue from a saved record given the files as they are now. */
export function planSession(record: Saved | null, current: { [path: string]: string }, now: number, fork = false): any {
  if (record === null) return { action: "fresh", session_id: null, changed: [], deleted: [], added: [], fork: false };
  const before = record.files;
  const paths = Object.keys(before);
  const changed = paths.filter((p) => p in current && before[p] !== current[p]).sort();
  const deleted = paths.filter((p) => !(p in current)).sort();
  const added = Object.keys(current).filter((p) => !(p in before)).sort();
  const share = paths.length ? (changed.length + deleted.length + added.length) / paths.length : 0;
  let action: string;
  if (share > STALE_SHARE || now - record.last_used > WEEK_SECONDS) action = "fresh_with_summary";
  else if (changed.length || deleted.length || added.length) action = "resume_with_notice";
  else action = "resume";
  const resumed = action === "resume" || action === "resume_with_notice";
  return { action, session_id: resumed ? record.id : null, changed, deleted, added, fork: Boolean(fork) && resumed };
}

/** What to tell a resumed session: the files that differ from what it analysed, and to read them again. */
export function changeNotice(plan: any): string {
  const lines: string[] = [];
  for (const [label, key] of [["changed", "changed"], ["deleted", "deleted"], ["new", "added"]]) {
    if (plan[key].length) lines.push(`- ${label}: ${plan[key].join(", ")}`);
  }
  if (!lines.length) return "";
  return ["Since your earlier analysis:", ...lines, "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."].join("\n");
}

function clean(items: unknown[] | undefined): string[] {
  const seen: string[] = [];
  for (const item of items ?? []) {
    const text = String(item).trim();
    if (text && !seen.includes(text)) seen.push(text);
  }
  return seen;
}

/** A fixed-layout summary for a fresh session: conclusions and the state they rest on, never raw tool output. */
export function buildSummary(findings: string[], decisions: string[], openQuestions: string[], files: { [path: string]: string }): string {
  const parts: string[] = [];
  for (const [title, items] of [["Findings", clean(findings)], ["Decisions", clean(decisions)], ["Open questions", clean(openQuestions)]] as Array<[string, string[]]>) {
    parts.push(`## ${title}\n` + (items.length ? items.map((item) => `- ${item}`).join("\n") : "- none"));
  }
  const listed = Object.keys(files).sort().map((path) => `- ${path} (${files[path]})`);
  parts.push("## Files\n" + (listed.length ? listed.join("\n") : "- none"));
  return parts.join("\n\n");
}

/** The prompt of the next run: the notice or the summary goes in front of the task. */
export function firstPrompt(plan: any, task: string, summary = ""): string {
  if (plan.action === "resume_with_notice") return `${changeNotice(plan)}\n\n${task}`;
  if (plan.action === "fresh_with_summary") return `${summary}\n\n${task}`;
  return task;
}

/** SDK options for a plan: resume by id, and fork together with it. A fresh start sets neither. */
export function sessionOptions(plan: any, extra: { [key: string]: unknown } = {}): any {
  const options: any = { ...extra };
  if (plan.session_id !== null) {
    options.resume = plan.session_id;
    if (plan.fork) options.forkSession = true;
  }
  return options;
}

/** `continue` takes the most recent session of the directory, so it is only safe when there is exactly one. */
export function continueOptions(sessionsInDirectory: unknown[], extra: { [key: string]: unknown } = {}): any {
  if (sessionsInDirectory.length !== 1) throw new Error(`${sessionsInDirectory.length} sessions in this directory: resume one by id`);
  return { ...extra, continue: true };
}

/** Run one single-shot query and keep the session id, which every result carries, and the error the SDK throws after an error result. */
export async function runSession(prompt: string, options: any): Promise<{ session_id: string | null; result: string | null; error: string | null }> {
  let sessionId: string | null = null;
  let result: string | null = null;
  let error: string | null = null;
  try {
    for await (const message of query({ prompt, options }) as AsyncIterable<any>) {
      if (message.type === "result") {
        sessionId = message.session_id;
        result = message.result ?? null;
      }
    }
  } catch (exc) { // the error result, if there was one, has already been read
    error = String(exc instanceof Error ? exc.message : exc);
  }
  return { session_id: sessionId, result, error };
}
