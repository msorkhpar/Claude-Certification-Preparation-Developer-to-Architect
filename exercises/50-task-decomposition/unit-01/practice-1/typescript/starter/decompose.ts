// Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md.
export function reviewChanges(files: Array<{ path: string; text: string }>, filePass: any, crossPass: any, maxLines = 40): any {
  // TODO: review each file alone (long files in parts), then run one cross pass over the summaries of the reviewed files.
  return null;
}

export function runAdaptive(planner: any, worker: any, goal: string, maxSteps = 6): any {
  // TODO: ask the planner what to do next after every step, and stop when it is done, stuck or out of steps.
  return null;
}

export function chooseStrategy(task: Record<string, unknown>): any {
  // TODO: fixed_chain, per_item_then_cross or adaptive.
  return null;
}
