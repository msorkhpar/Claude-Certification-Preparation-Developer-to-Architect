// Hand-written, illustrative scripted model: replies in the Messages API shape.
export type Block =
  | { type: "text"; text: string }
  | { type: "tool_use"; id: string; name: string; input: Record<string, unknown> };
export type Reply = { id: string; type: "message"; role: "assistant"; content: Block[]; stop_reason: "tool_use" | "end_turn" };
export type Message = { role: "user" | "assistant"; content: unknown };

export const text = (t: string): Block => ({ type: "text", text: t });
export const toolUse = (id: string, name: string, input: Record<string, unknown> = {}): Block =>
  ({ type: "tool_use", id, name, input });
export const reply = (content: Block[], stop_reason: Reply["stop_reason"]): Reply =>
  ({ id: "msg_illustrative", type: "message", role: "assistant", content, stop_reason });

export class ScriptedModel {
  replies: Reply[];
  seen: Message[][] = [];
  constructor(...replies: Reply[]) { this.replies = [...replies]; }
  call = (messages: Message[]): Reply => {
    this.seen.push(structuredClone(messages));
    const r = this.replies.shift();
    if (!r) throw new Error("scripted model ran out of replies");
    return r;
  };
}
