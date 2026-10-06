// Assemble a streamed Messages reply from its events. See ../../statement.md for the contract.
import { logger } from "./logger.ts";
const log = logger("assemble");

export type StreamEvent = { type: string } & Record<string, any>;

/** The stream carried an error event, or ended before message_stop (errorType "incomplete_stream"). */
export class StreamError extends Error {
  errorType: string;
  detail: string;
  constructor(errorType: string, message: string) {
    super(`${errorType}: ${message}`);
    this.errorType = errorType;
    this.detail = message;
  }
}

/** The message a message_start event begins: its fields except content, an empty content list and a copy of its usage. */
function startMessage(start: Record<string, any>): Record<string, any> {
  const { content: _dropped, ...rest } = start;
  return { ...rest, content: [], usage: { ...(start.usage ?? {}) } };
}

/** Fold one content_block_delta into its block; `pieces` is the fragment array of a tool_use block. */
function applyDelta(block: Record<string, any>, pieces: string[] | undefined, delta: Record<string, any>): void {
  if (delta.type === "text_delta") block.text = (block.text ?? "") + delta.text;
  else if (delta.type === "input_json_delta") pieces!.push(delta.partial_json);
  else if (delta.type === "thinking_delta") block.thinking = (block.thinking ?? "") + delta.thinking;
  else if (delta.type === "signature_delta") block.signature = delta.signature;
}

/** At content_block_stop: a tool_use block (it has `pieces`) gets its input, the fragments joined and parsed, {} when empty. */
function finishBlock(block: Record<string, any>, pieces: string[] | undefined): void {
  if (pieces === undefined) return;
  const joined = pieces.join("");
  block.input = joined.trim() ? JSON.parse(joined) : {};
}

/** Fold a message_delta event: the stop reason and sequence, and each usage key replaces the same key (the output count is cumulative). */
function applyMessageDelta(message: Record<string, any>, event: StreamEvent): void {
  message.stop_reason = event.delta.stop_reason ?? null;
  message.stop_sequence = event.delta.stop_sequence ?? null;
  Object.assign(message.usage, event.usage ?? {});
}

/** An error event ends the assembly with a StreamError carrying the error's type and message. */
function raiseError(event: StreamEvent): void {
  throw new StreamError(event.error?.type ?? "unknown", event.error?.message ?? "");
}

/** A stream that never started or never reached message_stop is an error, not a short message. */
function checkComplete(message: Record<string, any> | null, stopped: boolean): void {
  if (message === null || !stopped) throw new StreamError("incomplete_stream", "the stream ended before message_stop");
}

export function assemble(events: StreamEvent[]): Record<string, any> {
  log.debug("assemble input", events);
  let message: Record<string, any> | null = null;
  const blocks = new Map<number, Record<string, any>>();
  const fragments = new Map<number, string[]>();
  let stopped = false;
  for (const event of events) {
    switch (event.type) {
      case "message_start":
        message = startMessage(event.message);
        break;
      case "content_block_start":
        blocks.set(event.index, { ...event.content_block });
        if (event.content_block.type === "tool_use") fragments.set(event.index, []);
        break;
      case "content_block_delta":
        applyDelta(blocks.get(event.index)!, fragments.get(event.index), event.delta);
        break;
      case "content_block_stop":
        finishBlock(blocks.get(event.index)!, fragments.get(event.index));
        break;
      case "message_delta":
        applyMessageDelta(message!, event);
        break;
      case "message_stop":
        stopped = true;
        break;
      case "error":
        raiseError(event);
        break;
      default:
        break; // ping and event types this client does not know are skipped: new types may be added
    }
  }
  checkComplete(message, stopped);
  message!.content = [...blocks.keys()].sort((a, b) => a - b).map((i) => blocks.get(i));
  return message!;
}
