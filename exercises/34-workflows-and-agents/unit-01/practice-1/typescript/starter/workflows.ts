// Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md.
type Ask = (prompt: string) => string;

export function orchestrate(ask: Ask, task: string, maxSubtasks = 5): any {
  // TODO: plan with one call, run a worker call for each subtask, combine the results with one more call.
  return null;
}

export function refine(write: Ask, judge: Ask, task: string, maxRounds = 3, threshold = 8): any {
  // TODO: write a draft, judge it, and revise with the feedback until the score reaches the threshold or the rounds run out.
  return null;
}

export function route(ask: Ask, text: string, routes: Record<string, (text: string) => string>, fallbackLabel: string): any {
  // TODO: classify the text with one model call, then run the handler of the label (or of the default label).
  return null;
}

export function vote(ask: Ask, prompt: string, n = 5): any {
  // TODO: ask n times and return the majority answer, the counts and the share of the winner.
  return null;
}
