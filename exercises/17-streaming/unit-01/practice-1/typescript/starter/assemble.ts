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
  // TODO: fold the events into the message a non-streaming call would have returned.
  return {};
}
