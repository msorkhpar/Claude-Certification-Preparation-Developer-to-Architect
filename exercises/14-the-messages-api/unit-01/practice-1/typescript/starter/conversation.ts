// A conversation client that keeps the state the API does not. See ../../statement.md for the contract.
import { logger } from "../logger.ts";
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
    // TODO 1 of 8 (finish this to pass e6): refuse a blank turn before anything is sent.
    // Receives the text of the turn. Throws an Error when it is not a string, empty or only whitespace; otherwise returns nothing.
    // Example: checkText("   ") -> Error, checkText("hi") -> undefined
  }

  requestBody(): Body {
    // TODO 2 of 8 (finish this to pass m1 and e5): the request body, a snapshot.
    // Receives nothing (it reads this). Returns an object with `model`, `max_tokens` and `messages`, where `messages` is a deep copy
    // (structuredClone) of the whole history so far (the new user turn is already in it), so a later turn cannot change a request
    // already sent. Example: after say("a"), the first body has messages [{ role: "user", content: "a" }]
    return { model: this.model, max_tokens: this.maxTokens, messages: [] };
  }

  optionalFields(body: Body): void {
    // TODO 3 of 8 (finish this to pass e4): the top-level fields that are only sometimes there.
    // Receives the body and adds to it: `system` (a top-level field, never a message) when this.system is not blank, and
    // `stop_sequences` (a copy of the array) when this.stopSequences is given and not empty. Returns nothing.
    // Example: with system "Be brief." the body gains { system: "Be brief." }; with system "  " it gains nothing
  }

  sendOrRollBack(body: Body): Body {
    // TODO 4 of 8 (finish this to pass e2): send the body, and leave no dangling user turn when the call fails.
    // Receives the body. Returns this.send(body). When send throws, removes the user turn that say added to this._history and throws
    // the same error again, so roles keep alternating on the next call.
    return this.send(body);
  }

  assistantTurn(response: Body): Body {
    // TODO 5 of 8 (finish this to pass m1): the turn to store for the reply.
    // Receives the response. Returns { role: "assistant", content: <the response's content array, as received> }.
    // Example: content [{ type: "text", text: "Paris." }] -> { role: "assistant", content: [that same array] }
    return { role: "assistant", content: [] };
  }

  addUsage(usage: Body): void {
    // TODO 6 of 8 (finish this to pass e1): keep the running totals.
    // Receives the response's usage (it may be undefined or lack a key). Adds its input_tokens and output_tokens to this._totals.
    // Returns nothing. Example: totals input_tokens 12 plus usage { input_tokens: 30, output_tokens: 9 } -> input_tokens 42
  }

  makeReply(response: Body): Reply {
    // TODO 7 of 8 (finish this to pass m1 and e3): what say returns.
    // Receives the response. Returns a Reply: the `text` of the content blocks whose `type` is "text" joined with nothing between
    // them, the response's stop_reason as `stopReason`, and `truncated`, true only when the stop reason is "max_tokens".
    // Example: stop_reason "max_tokens" -> truncated true; "end_turn" -> truncated false
    return { text: "", stopReason: "", truncated: false };
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
    // TODO 8 of 8 (finish this to pass e5): the turns so far, as a copy.
    // Returns a deep copy (structuredClone) of this._history, so changing what the caller gets changes nothing here.
    // Example: chat.history().push(x) leaves chat.history().length unchanged
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
