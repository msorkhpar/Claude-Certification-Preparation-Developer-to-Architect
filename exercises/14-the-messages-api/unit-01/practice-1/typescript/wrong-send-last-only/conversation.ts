// A conversation client that keeps the state the API does not. See ../../statement.md for the contract.
export type Body = Record<string, any>;
export type Send = (body: Body) => Body;
export type Reply = { text: string; stopReason: string; truncated: boolean };

export class Conversation {
  send: Send;
  model: string;
  maxTokens: number;
  system: string | null;
  stopSequences: string[] | null;
  _history: any[] = [];
  _totals = { input_tokens: 0, output_tokens: 0 };

  constructor(send: Send, model: string, maxTokens: number, system: string | null = null, stopSequences: string[] | null = null) {
    this.send = send;
    this.model = model;
    this.maxTokens = maxTokens;
    this.system = system;
    this.stopSequences = stopSequences;
  }

  say(text: string): Reply {
    if (typeof text !== "string" || text.trim() === "") throw new Error("a turn needs text");
    this._history.push({ role: "user", content: text });
    const body: Body = { model: this.model, max_tokens: this.maxTokens, messages: structuredClone(this._history.slice(-1)) };
    if (this.system && this.system.trim() !== "") body.system = this.system;
    if (this.stopSequences && this.stopSequences.length > 0) body.stop_sequences = [...this.stopSequences];
    let response: Body;
    try {
      response = this.send(body);
    } catch (err) {
      this._history.pop();
      throw err;
    }
    this._history.push({ role: "assistant", content: response.content });
    this._totals.input_tokens += response.usage?.input_tokens ?? 0;
    this._totals.output_tokens += response.usage?.output_tokens ?? 0;
    const replyText = response.content.filter((b: any) => b.type === "text").map((b: any) => b.text ?? "").join("");
    return { text: replyText, stopReason: response.stop_reason, truncated: response.stop_reason === "max_tokens" };
  }

  history(): any[] {
    return structuredClone(this._history);
  }

  totals() {
    return { ...this._totals };
  }

  reset(): void {
    this._history = [];
    this._totals = { input_tokens: 0, output_tokens: 0 };
  }
}
