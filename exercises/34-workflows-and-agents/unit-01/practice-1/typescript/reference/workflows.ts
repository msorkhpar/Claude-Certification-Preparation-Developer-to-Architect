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

function cleanSteps(data: unknown): string[] {
  const steps: string[] = [];
  if (Array.isArray(data)) {
    for (const item of data) if (typeof item === "string" && item.trim() && !steps.includes(item.trim())) steps.push(item.trim());
  }
  return steps;
}

/** Subtasks from the planner's reply, or [[task], true] when the reply cannot be used. */
export function parsePlan(text: string, task: string, maxSubtasks: number): [string[], boolean] {
  const data = parseJson(slice(text, "[", "]"));
  const steps = cleanSteps(data);
  if (steps.length === 0) return [[task], true];
  return [steps.slice(0, maxSubtasks), false];
}

function runWorker(ask: Ask, subtask: string, task: string): Result {
  log.debug("runWorker input", subtask);
  try {
    const output = ask(`Subtask: ${subtask}\nTask: ${task}`);
    if (typeof output !== "string" || !output.trim()) throw new Error("empty reply");
    return { subtask, status: "ok", output };
  } catch (err) {
    return { subtask, status: "failed", error: (err as Error).message }; // one worker failing must not stop the others
  }
}

function combinePrompt(task: string, results: Result[]): string {
  const lines = results.map((r, i) => `${i + 1}. ${r.subtask} -> ${r.status === "ok" ? r.output : "FAILED"}`);
  return "Combine: write one answer to the task from the results.\nTask: " + task + "\n" + lines.join("\n");
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

function scoreOk(score: unknown): boolean {
  return typeof score === "number" && score >= 0 && score <= 10;
}

/** [score, feedback] from the judge's reply; [0, a fixed note] when the reply cannot be read. */
export function readJudgement(text: string): [number, string] {
  const data: any = parseJson(slice(text, "{", "}"));
  const score = data !== null && typeof data === "object" && !Array.isArray(data) ? data.score : undefined;
  if (!scoreOk(score)) return [0, "The judge reply could not be read."];
  return [score, typeof data.feedback === "string" ? data.feedback : ""];
}

function writerPrompt(task: string, draft: string | null, feedback: string, round: number): string {
  return round === 1 ? `Task: ${task}` : `Task: ${task}\nPrevious draft: ${draft}\nFeedback: ${feedback}\nRevise the draft.`;
}

function isBetter(score: number, bestScore: number | null): boolean {
  return bestScore === null || score > bestScore;
}

function errorResult(best: string | null, bestScore: number | null, rounds: number, history: unknown[], err: unknown) {
  return { status: "error", draft: best, score: bestScore, rounds, history, error: (err as Error).message };
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

function normaliseLabel(reply: unknown): string {
  return typeof reply === "string" ? reply.trim().replace(/^[.,;:!"'`]+|[.,;:!"'`]+$/g, "").trim().toLowerCase() : "";
}

/** Classify the text with one model call, then run the handler of the label. `routes` maps label to a function of the text. */
export function route(ask: Ask, text: string, routes: Record<string, (text: string) => string>, fallbackLabel: string) {
  const reply = ask(`Classify: ${text}\nLabels: ${Object.keys(routes).join(", ")}`);
  let label = normaliseLabel(reply);
  const fallback = !Object.hasOwn(routes, label);
  if (fallback) label = fallbackLabel;
  return { label, output: routes[label](text), fallback };
}

function pickWinner(counts: Map<string, number>, top: number): string {
  return [...counts].find(([, c]) => c === top)![0];
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
