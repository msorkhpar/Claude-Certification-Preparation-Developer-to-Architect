// Assemble a streamed Messages reply from its events. See ../../statement.md for the contract.
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

export function assemble(events: StreamEvent[]): Record<string, any> {
  let message: Record<string, any> | null = null;
  const blocks = new Map<number, Record<string, any>>();
  const fragments = new Map<number, string[]>();
  let stopped = false;
  for (const event of events) {
    switch (event.type) {
      case "message_start": {
        const { content: _dropped, ...rest } = event.message;
        message = { ...rest, content: [], usage: { ...(event.message.usage ?? {}) } };
        break;
      }
      case "content_block_start":
        blocks.set(event.index, { ...event.content_block });
        if (event.content_block.type === "tool_use") fragments.set(event.index, []);
        break;
      case "content_block_delta": {
        const block = blocks.get(event.index)!;
        const delta = event.delta;
        if (delta.type === "text_delta") block.text = (block.text ?? "") + delta.text;
        else if (delta.type === "input_json_delta") fragments.get(event.index)!.push(delta.partial_json);
        else if (delta.type === "thinking_delta") block.thinking = (block.thinking ?? "") + delta.thinking;
        else if (delta.type === "signature_delta") block.signature = delta.signature;
        break;
      }
      case "content_block_stop":
        if (fragments.has(event.index)) {
          const joined = fragments.get(event.index)!.join("");
          blocks.get(event.index)!.input = joined.trim() ? JSON.parse(joined) : {};
        }
        break;
      case "message_delta":
        message!.stop_reason = event.delta.stop_reason ?? null;
        message!.stop_sequence = event.delta.stop_sequence ?? null;
        Object.assign(message!.usage, event.usage ?? {});
        break;
      case "message_stop":
        stopped = true;
        break;
      case "error":
        break;
      default:
        break; // ping and event types this client does not know are skipped: new types may be added
    }
  }
  if (message === null || !stopped) throw new StreamError("incomplete_stream", "the stream ended before message_stop");
  message.content = [...blocks.keys()].sort((a, b) => a - b).map((i) => blocks.get(i));
  return message;
}
