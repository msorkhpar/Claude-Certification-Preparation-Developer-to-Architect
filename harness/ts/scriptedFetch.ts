// A scripted `fetch` for the TypeScript SDK's own hook: `new Anthropic({ fetch })`.
// The reader's code calls the SDK unchanged; the request goes here, never to a socket.
// A reply is a body (JSON), or `sse` (a list of event objects framed as server-sent events).

export type Reply = {
  status?: number;
  body?: unknown;
  headers?: Record<string, string>;
  sse?: Array<{ type: string } & Record<string, unknown>>;
  text?: string; // a raw body, such as the .jsonl file of batch results
  contentType?: string; // for `text`; defaults to application/x-jsonl
  networkError?: boolean; // the connection fails before any reply
};

export type Seen = { url: string; method: string; headers: Record<string, string>; body: any };

export function sseEncode(events: Array<{ type: string }>): string {
  return events.map((e) => `event: ${e.type}\ndata: ${JSON.stringify(e)}\n\n`).join("");
}

export function scriptedFetch(script: Array<Reply | ((body: any) => Reply)>, opts: { delayMs?: number } = {}) {
  const queue = [...script];
  const seen: Seen[] = [];
  const state = { inFlight: 0, maxInFlight: 0 };
  const fetchFn = async (url: any, init: any = {}): Promise<Response> => {
    const headers: Record<string, string> = {};
    new Headers(init.headers).forEach((v, k) => (headers[k] = v));
    const raw = typeof init.body === "string" ? init.body : undefined;
    seen.push({ url: String(url), method: init.method ?? "GET", headers, body: raw ? JSON.parse(raw) : undefined });
    const mine = seen[seen.length - 1];
    state.inFlight++;
    state.maxInFlight = Math.max(state.maxInFlight, state.inFlight);
    try {
      if (opts.delayMs) await new Promise((r) => setTimeout(r, opts.delayMs));
      const next = queue.shift();
      const reply = typeof next === "function" ? next(mine.body) : next;
      if (!reply) {
        return new Response(JSON.stringify({ type: "error", error: { type: "api_error", message: "scripted model ran out of replies" } }), { status: 500 });
      }
      if (reply.networkError) throw new TypeError("fetch failed");
      if (reply.sse) {
        return new Response(sseEncode(reply.sse), { status: 200, headers: { "content-type": "text/event-stream", ...reply.headers } });
      }
      if (reply.text !== undefined) {
        return new Response(reply.text, { status: reply.status ?? 200, headers: { "content-type": reply.contentType ?? "application/x-jsonl", ...reply.headers } });
      }
      return new Response(JSON.stringify(reply.body), { status: reply.status ?? 200, headers: { "content-type": "application/json", ...reply.headers } });
    } finally {
      state.inFlight--;
    }
  };
  return { fetch: fetchFn, seen, state };
}

/** A Messages API response body the real API could return (same shapes, same stop_reason values). */
export function message(content: unknown[], stopReason = "end_turn", usage = { input_tokens: 1, output_tokens: 1 }, model = "claude-sonnet-5-5") {
  return { id: "msg_illustrative", type: "message", role: "assistant", model, content, stop_reason: stopReason, stop_sequence: null, usage };
}
export const text = (t: string) => ({ type: "text", text: t });
