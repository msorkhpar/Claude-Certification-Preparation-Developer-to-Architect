// Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("workflows");
type Ask = (prompt: string) => string;
type Result = { subtask: string; status: string; output?: string; error?: string };

function slice(text: string, open: string, close: string): string {
  const first = text.indexOf(open), last = text.lastIndexOf(close);
  return first !== -1 && last > first ? text.slice(first, last + 1) : "";
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

/**
 * TODO 1 of 9 (unlocks e1): the usable subtasks from the parsed plan.
 * Receives whatever JSON.parse gave (an array, or anything else) and returns the string items of an array, trimmed, with empty
 * ones and repeats dropped (the first of a repeat stays), in order; anything that is not an array gives [].
 * Example: cleanSteps(["a", " a ", "", 3, "b"]) -> ["a", "b"]
 */
function cleanSteps(data: unknown): string[] {
  return [];
}

/** Subtasks from the planner's reply, or [[task], true] when the reply cannot be used. */
export function parsePlan(text: string, task: string, maxSubtasks: number): [string[], boolean] {
  const data = parseJson(slice(text, "[", "]"));
  const steps = cleanSteps(data);
  if (steps.length === 0) return [[task], true];
  return [steps.slice(0, maxSubtasks), false];
}

/**
 * TODO 2 of 9 (unlocks m1 and e2): run one worker call for one subtask.
 * Receives `ask`, the subtask and the task. Calls `ask` with `Subtask: ${subtask}\nTask: ${task}` and returns
 * { subtask, status: "ok", output } for a good reply, or { subtask, status: "failed", error } when the reply is empty or only
 * spaces (error `empty reply`) or the call throws (error: the exception's message). It never throws.
 * Example: a worker that replies "  " -> { subtask: "s", status: "failed", error: "empty reply" }
 */
function runWorker(ask: Ask, subtask: string, task: string): Result {
  log.debug("runWorker input", subtask);
  return { subtask, status: "failed", error: "" };
}

/**
 * TODO 3 of 9 (unlocks m1): the prompt of the combine call.
 * Receives the task and the results in plan order. Returns `Combine: write one answer to the task from the results.\nTask: ${task}`
 * followed by one line per result, `\n${i}. ${subtask} -> ${output}` or `\n${i}. ${subtask} -> FAILED` (i from 1).
 * Example: task "T", one ok result "a" with output "x" -> "Combine: write one answer to the task from the results.\nTask: T\n1. a -> x"
 */
function combinePrompt(task: string, results: Result[]): string {
  return "";
}

export function orchestrate(ask: Ask, task: string, maxSubtasks = 5) {
  let calls = 1;
  const [plan, fallback] = parsePlan(ask(`Plan: split the task into at most ${maxSubtasks} independent subtasks. Reply with a JSON array of strings only.\nTask: ${task}`), task, maxSubtasks);
  const results: Result[] = [];
  for (const subtask of plan) {
    calls++;
    results.push(runWorker(ask, subtask, task));
  }
  const ok = results.filter((r) => r.status === "ok");
  if (ok.length === 0) return { status: "failed", plan, fallback, results, answer: null, calls };
  const answer = ask(combinePrompt(task, results));
  return { status: ok.length === results.length ? "done" : "partial", plan, fallback, results, answer, calls: calls + 1 };
}

/**
 * TODO 4 of 9 (unlocks e4): is this a usable judge score?
 * Receives the `score` field of the judge's JSON (any value, or undefined). Returns true for a number from 0 to 10 and false for
 * anything else, a boolean included.
 * Example: scoreOk(7.5) -> true, scoreOk(true) -> false, scoreOk(11) -> false
 */
function scoreOk(score: unknown): boolean {
  return typeof score === "number"; // a number of any size; add the range and the NaN exclusion
}

/** [score, feedback] from the judge's reply; [0, a fixed note] when the reply cannot be read. */
export function readJudgement(text: string): [number, string] {
  const data: any = parseJson(slice(text, "{", "}"));
  const score = data !== null && typeof data === "object" && !Array.isArray(data) ? data.score : undefined;
  if (!scoreOk(score)) return [0, "The judge reply could not be read."];
  return [score, typeof data.feedback === "string" ? data.feedback : ""];
}

/**
 * TODO 5 of 9 (unlocks e3): the prompt for the writer in one round.
 * Receives the task, the latest draft, the judge's latest feedback and the round number (from 1). Returns `Task: ${task}` in round 1
 * and afterwards `Task: ${task}\nPrevious draft: ${draft}\nFeedback: ${feedback}\nRevise the draft.`
 * Example: writerPrompt("T", "d", "f", 2) -> "Task: T\nPrevious draft: d\nFeedback: f\nRevise the draft."
 */
function writerPrompt(task: string, draft: string | null, feedback: string, round: number): string {
  return `Task: ${task}`;
}

/**
 * TODO 6 of 9 (unlocks e4): does this draft replace the best one so far?
 * Receives the new score and the best score so far (null before any draft was judged). Returns true when there is no best yet or
 * the new score is strictly higher (so the earliest wins a tie).
 * Example: isBetter(5, null) -> true, isBetter(5, 5) -> false
 */
function isBetter(score: number, bestScore: number | null): boolean {
  return false;
}

/**
 * TODO 7 of 9 (unlocks e7): the result when the writer threw.
 * Receives the best draft and score so far (null, null before any), the rounds completed, the history and the error. Returns the
 * object { status: "error", draft, score, rounds, history, error: <the error's message> }.
 * Example: errorResult("d", 4, 1, [], new Error("boom")).error -> "boom"
 */
function errorResult(best: string | null, bestScore: number | null, rounds: number, history: unknown[], err: unknown) {
  return { status: "error", draft: best, score: bestScore, rounds, history, error: "" };
}

export function refine(write: Ask, judge: Ask, task: string, maxRounds = 3, threshold = 8) {
  const history: { round: number; score: number; feedback: string }[] = [];
  let best: string | null = null, bestScore: number | null = null, draft: string | null = null, feedback = "";
  for (let round = 1; round <= maxRounds; round++) {
    const prompt = writerPrompt(task, draft, feedback, round);
    try {
      draft = write(prompt);
    } catch (err) {
      return errorResult(best, bestScore, round - 1, history, err);
    }
    let score: number;
    [score, feedback] = readJudgement(judge(`Judge: score the draft from 0 to 10 and reply with JSON {"score": n, "feedback": "..."}.\nTask: ${task}\nDraft: ${draft}`));
    history.push({ round, score, feedback });
    if (isBetter(score, bestScore)) [best, bestScore] = [draft, score];
    if (score >= threshold) return { status: "accepted", draft, score, rounds: round, history };
  }
  return { status: "max_rounds", draft: best, score: bestScore, rounds: maxRounds, history };
}

/**
 * TODO 8 of 9 (unlocks e5): the label in a classifier's reply.
 * Receives the reply (a string, or anything else). Returns it trimmed of spaces, then of the punctuation `.,;:!"'` and the backtick
 * at both ends, then of spaces again, in lower case; "" when the reply is not a string.
 * Example: normaliseLabel('  "Billing." ') -> "billing"
 */
function normaliseLabel(reply: unknown): string {
  return "";
}

/** Classify the text with one model call, then run the handler of the label. `routes` maps label to a function of the text. */
export function route(ask: Ask, text: string, routes: Record<string, (text: string) => string>, fallbackLabel: string) {
  const reply = ask(`Classify: ${text}\nLabels: ${Object.keys(routes).join(", ")}`);
  let label = normaliseLabel(reply);
  const fallback = !Object.hasOwn(routes, label);
  if (fallback) label = fallbackLabel;
  return { label, output: routes[label](text), fallback };
}

/**
 * TODO 9 of 9 (unlocks e6): the answer that wins the vote.
 * Receives the counts (answer -> count, in order of first appearance) and the highest count. Returns the first answer whose count
 * is that highest count, so a tie goes to the one seen first.
 * Example: pickWinner(new Map([["a", 2], ["b", 2]]), 2) -> "a"
 */
function pickWinner(counts: Map<string, number>, top: number): string {
  return "";
}

export function vote(ask: Ask, prompt: string, n = 5) {
  const counts = new Map<string, number>();
  for (let i = 0; i < n; i++) {
    const answer = ask(prompt).trim().toLowerCase();
    counts.set(answer, (counts.get(answer) ?? 0) + 1);
  }
  if (counts.size === 0) return { answer: null, votes: {}, agreement: 0.0 };
  const top = Math.max(...counts.values());
  const winner = pickWinner(counts, top);
  return { answer: winner, votes: Object.fromEntries(counts), agreement: top / n };
}
