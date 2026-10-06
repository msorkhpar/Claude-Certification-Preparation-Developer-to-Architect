// A conversation client that keeps the state the API does not. See ../../statement.md for the contract.
import { logger } from "./logger.ts";
const log = logger("conversation");

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

  checkText(text: string): void {
    if (typeof text !== "string" || text.trim() === "") throw new Error("a turn needs text");
  }

  requestBody(): Body {
    return { model: this.model, max_tokens: this.maxTokens, messages: structuredClone(this._history) };
  }

  optionalFields(body: Body): void {
    if (this.system && this.system.trim() !== "") body.system = this.system;
    if (this.stopSequences && this.stopSequences.length > 0) body.stop_sequences = [...this.stopSequences];
  }

  sendOrRollBack(body: Body): Body {
    try {
      return this.send(body);
    } catch (err) {
      this._history.pop();
      throw err;
    }
  }

  assistantTurn(response: Body): Body {
    return { role: "assistant", content: response.content };
  }

  addUsage(usage: Body): void {
    this._totals.input_tokens += usage?.input_tokens ?? 0;
    this._totals.output_tokens += usage?.output_tokens ?? 0;
  }

  makeReply(response: Body): Reply {
    const text = response.content.filter((b: any) => b.type === "text").map((b: any) => b.text ?? "").join("");
    return { text, stopReason: response.stop_reason, truncated: response.stop_reason === "max_tokens" };
  }

  say(text: string): Reply {
    log.debug("say input", text);
    this.checkText(text);
    this._history.push({ role: "user", content: text });
    const body = this.requestBody();
    this.optionalFields(body);
    const response = this.sendOrRollBack(body);
    this._history.push(this.assistantTurn(response));
    this.addUsage(response.usage);
    return this.makeReply(response);
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
