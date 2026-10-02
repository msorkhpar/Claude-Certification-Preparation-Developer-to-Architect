import type { Message, Reply } from "../tests/scripted.ts";

type Tool = (input: any) => unknown;

const textOf = (content: Reply["content"]): string =>
  content.map((b) => (b.type === "text" ? b.text : "")).join("");

export function runAgent(model: (m: Message[]) => Reply, tools: Record<string, Tool>, userText: string, maxTurns = 5): string {
  const messages: Message[] = [{ role: "user", content: userText }];
  for (let turn = 0; turn < maxTurns; turn++) {
    const resp = model(messages);
    messages.push({ role: "assistant", content: resp.content });
    if (textOf(resp.content)) return textOf(resp.content);
    const results = [];
    for (const block of resp.content) {
      if (block.type !== "tool_use") continue;
      try {
        const tool = tools[block.name];
        if (!tool) throw new Error(`unknown tool ${block.name}`);
        results.push({ type: "tool_result", tool_use_id: block.id, content: String(tool(block.input)) });
      } catch (e) {
        results.push({ type: "tool_result", tool_use_id: block.id, content: (e as Error).message, is_error: true });
      }
    }
    messages.push({ role: "user", content: results });
  }
  throw new Error("max_turns exceeded");
}
