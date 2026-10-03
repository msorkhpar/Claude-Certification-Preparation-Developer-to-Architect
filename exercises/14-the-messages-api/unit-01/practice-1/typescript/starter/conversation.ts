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
    // TODO: add the user turn, send the whole history, keep the assistant turn, count usage.
    return { text: "", stopReason: "", truncated: false };
  }

  history(): any[] {
    return [];
  }

  totals() {
    return { ...this._totals };
  }

  reset(): void {
    this._history = [];
    this._totals = { input_tokens: 0, output_tokens: 0 };
  }
}
