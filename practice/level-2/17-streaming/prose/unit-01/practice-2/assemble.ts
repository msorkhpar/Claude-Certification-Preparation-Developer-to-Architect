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

function startMessage(start: Record<string, any>): Record<string, any> {
  // TODO 1 of 6 (finish this to pass m1 and e5): the message a message_start event begins.
  // Receives the `message` object of the event. Returns a new object with its fields except `content`, then `content` as an empty array and `usage` as a COPY of its usage.
  // Example: startMessage({ id: "m", content: [], usage: { input_tokens: 5 } }) -> { id: "m", content: [], usage: { input_tokens: 5 } }
  return {};
}

function applyDelta(block: Record<string, any>, pieces: string[] | undefined, delta: Record<string, any>): void {
  // TODO 2 of 6 (finish this to pass m1 and e6): fold one content_block_delta into its block.
  // Receives the block, `pieces` (the fragment array of a tool_use block, else undefined) and the `delta`. `text_delta` appends `text` to the block's text,
  // `thinking_delta` appends `thinking`, `signature_delta` sets `signature`, `input_json_delta` pushes `partial_json` onto `pieces`. Returns nothing.
  // Example: block { text: "He" }, delta { type: "text_delta", text: "llo" } -> block { text: "Hello" }
}

function finishBlock(block: Record<string, any>, pieces: string[] | undefined): void {
  // TODO 3 of 6 (finish this to pass e1): at content_block_stop, give a tool_use block its input.
  // Receives the block and its fragment array `pieces` (undefined for a block that is not tool_use). When `pieces` is an array, sets block.input to the fragments
  // joined and parsed as JSON, or to {} when the joined text is empty or only white space. Returns nothing.
  // Example: pieces ['{"ci', 'ty": "Pa', 'ris"}'] -> block.input is { city: "Paris" }; pieces [""] -> {}
}

function applyMessageDelta(message: Record<string, any>, event: StreamEvent): void {
  // TODO 4 of 6 (finish this to pass m1 and e5): fold a message_delta event into the message.
  // Receives the message and the event. Sets message.stop_reason and message.stop_sequence from event.delta (null when absent), and each key of event.usage replaces
  // the same key in message.usage (the output count is cumulative: replace, do not add). Returns nothing.
  // Example: usage { input_tokens: 52, output_tokens: 1 } and event usage { output_tokens: 38 } -> { input_tokens: 52, output_tokens: 38 }
}

function raiseError(event: StreamEvent): void {
  // TODO 5 of 6 (finish this to pass e3): an error event ends the assembly.
  // Receives the event, whose `error` holds a `type` and a `message`. Throws new StreamError(type, message).
  // Example: { error: { type: "overloaded_error", message: "Overloaded" } } throws StreamError("overloaded_error", "Overloaded")
}

function checkComplete(message: Record<string, any> | null, stopped: boolean): void {
  // TODO 6 of 6 (finish this to pass e4): a stream that never started or never reached message_stop is an error.
  // Receives the message (null before message_start) and whether message_stop was seen. Throws StreamError("incomplete_stream", ...) unless both hold.
  // Example: checkComplete({}, false) throws a StreamError with errorType "incomplete_stream"
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
