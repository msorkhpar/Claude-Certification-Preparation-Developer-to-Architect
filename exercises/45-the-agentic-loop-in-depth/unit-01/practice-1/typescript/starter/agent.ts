// The agent loop, driven by the stop reason. See ../../statement.md.
type Model = (messages: any[]) => { stop_reason: string; content: Array<Record<string, any>> };
type Tools = Record<string, (input: any) => string>;

export function runAgent(model: Model, tools: Tools, task: string, maxTurns = 8): any {
  // TODO: send the task, run the tool calls the model asks for, and decide what to do next from the stop reason only.
  return null;
}
