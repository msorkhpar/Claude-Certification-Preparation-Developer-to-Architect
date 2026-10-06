// Session state with the Agent SDK: choose between resuming and starting fresh, say what changed, carry a summary, set the options. See ../../statement.md.
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("sessions");

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
  log.debug("planSession input", record);
  if (record === null) return { action: "fresh", session_id: null, changed: [], deleted: [], added: [], fork: false };
  const before = record.files;
  const paths = Object.keys(before);
  // TODO 1 of 10 (finish this to pass m1, e1): the differences. Receives the saved digests `before` and the digests
  //   `current` (path -> digest). Fill `changed` (in both, digest differs), `deleted` (saved, now gone) and `added` (new
  //   now), each sorted by path. Example: before {a: 1, b: 2}, current {a: 9, c: 3} -> changed [a], deleted [b], added
  //   [c].
  const changed: string[] = [];
  const deleted: string[] = [];
  const added: string[] = [];
  // TODO 2 of 10 (finish this to pass m1, e2): the share. Return the share of the saved files that changed or are gone:
  //   (changed + deleted) / saved files, and 0 when nothing was saved. New files do not count. Example: 4 saved, 1
  //   changed, 1 deleted -> 0.5.
  const share = 0;
  let action: string;
  // TODO 3 of 10 (finish this to pass e3): the age rule. Start fresh with a summary when the share is above STALE_SHARE
  //   or the session has been idle for more than WEEK_SECONDS (now minus last_used). Example: idle 7 days exactly ->
  //   resume; 7 days and 1 second -> fresh_with_summary.
  if (share > STALE_SHARE) action = "fresh_with_summary";
  else if (changed.length || deleted.length || added.length) action = "resume_with_notice";
  else action = "resume";
  const resumed = action === "resume" || action === "resume_with_notice";
  // TODO 4 of 10 (finish this to pass e4): the fork flag of the plan. `resumed` is true when the action is resume or
  //   resume_with_notice. The plan forks only when fork was asked for and the session is resumed. Example: fork asked on a
  //   stale session -> fork false.
  return { action, session_id: resumed ? record.id : null, changed, deleted, added, fork: Boolean(fork) };
}

/** What to tell a resumed session: the files that differ from what it analysed, and to read them again. */
export function changeNotice(plan: any): string {
  const lines: string[] = [];
  // TODO 5 of 10 (finish this to pass e5): the notice lines. For each of the lists changed, deleted and added of the
  //   plan that is not empty, add one line `- label: a, b` (labels changed, deleted, new). Example: changed [a.py] -> `-
  //   changed: a.py`.
  if (!lines.length) return "";
  return ["Since your earlier analysis:", ...lines, "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."].join("\n");
}

function clean(items: unknown[] | undefined): string[] {
  const seen: string[] = [];
  // TODO 6 of 10 (finish this to pass e6): the cleaning of a list for the summary. Receives the items (text or none).
  //   Return them trimmed, without blank items and without repeats, in first-seen order. Example: [" a ", "", "a", "b"] ->
  //   [a, b].
  for (const item of items ?? []) seen.push(String(item));
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
  // TODO 7 of 10 (finish this to pass e5): the first prompt. Return the text the next run starts with: for
  //   resume_with_notice the change notice, a blank line, then the task; for fresh_with_summary the summary, a blank line,
  //   then the task; otherwise the task. Example: action resume -> the task unchanged.
  return task;
}

/** SDK options for a plan: resume by id, and fork together with it. A fresh start sets neither. */
export function sessionOptions(plan: any, extra: { [key: string]: unknown } = {}): any {
  const options: any = { ...extra };
  // TODO 8 of 10 (finish this to pass e7): the options of a plan. When the plan has a session id, set the resume option
  //   to it, and the fork option to true only when the plan forks; set neither for a fresh plan. Example: plan with
  //   session s1 and fork true -> resume s1 and fork true.
  return options;
}

/** `continue` takes the most recent session of the directory, so it is only safe when there is exactly one. */
export function continueOptions(sessionsInDirectory: unknown[], extra: { [key: string]: unknown } = {}): any {
  // TODO 9 of 10 (finish this to pass e7): the guard of `continue`. When the directory does not hold exactly one
  //   session, refuse with an error that names the count and says to resume one by id. Example: three sessions -> error "3
  //   sessions in this directory: resume one by id".
  return { ...extra, continue: true };
}

/** Run one single-shot query and keep the session id, which every result carries, and the error the SDK throws after an error result. */
export async function runSession(prompt: string, options: any): Promise<{ session_id: string | null; result: string | null; error: string | null }> {
  let sessionId: string | null = null;
  let result: string | null = null;
  let error: string | null = null;
  try {
    for await (const message of query({ prompt, options }) as AsyncIterable<any>) {
      // TODO 10 of 10 (finish this to pass e8): the reading of the result message. When a message is the result message,
      //   keep its session id and its result text. Example: a result with session s9 -> session_id s9, even if the SDK
      //   then raises an error.
    }
  } catch (exc) { // the error result, if there was one, has already been read
    error = String(exc instanceof Error ? exc.message : exc);
  }
  return { session_id: sessionId, result, error };
}
