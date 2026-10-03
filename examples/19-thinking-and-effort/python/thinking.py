"""Adaptive thinking steered by effort: the request, the reply's blocks and what the thinking cost.

The reply is an illustrative, hand-written response in the API's shape (claude-opus-5-5), not a capture. It carries
an omitted thinking block (the default display on this model: an empty `thinking` field and a signature) and the
`output_tokens_details.thinking_tokens` breakdown the thinking page documents.
"""
from harness import scripted_client
from harness.scripted import message, text

MODEL = "claude-opus-5-5"
PRICE_OUT = 20.0  # dollars per million output tokens, pricing page 2026-10-02
THINKING_BLOCK = {"type": "thinking", "thinking": "", "signature": "illustrative-signature"}
USAGE = {"input_tokens": 410, "output_tokens": 1900, "output_tokens_details": {"thinking_tokens": 1650}}


def thinking_tokens(usage):
    """usage.output_tokens_details.thinking_tokens, read whether the SDK types the field or keeps it as an extra."""
    details = getattr(usage, "output_tokens_details", None) or (usage.model_extra or {}).get("output_tokens_details")
    return details["thinking_tokens"] if isinstance(details, dict) else details.thinking_tokens


def request(client, effort):
    return client.messages.create(model=MODEL, max_tokens=8000, thinking={"type": "adaptive"}, output_config={"effort": effort},
                                  messages=[{"role": "user", "content": "Which of these two schedules has no conflicts?"}])


def main():
    reply_body = message([THINKING_BLOCK, text("Schedule B has no conflicts.")], usage=USAGE)
    client, transport = scripted_client(reply_body, message([text("B.")], usage={"input_tokens": 410, "output_tokens": 12}))
    reply = request(client, "high")
    sent = transport.requests[0]
    print("thinking sent:", sent["thinking"], "| effort sent:", sent["output_config"])
    print("blocks:", [b.type for b in reply.content], "| thinking text shown:", repr(reply.content[0].thinking))
    thinking = thinking_tokens(reply.usage)
    print(f"output_tokens {reply.usage.output_tokens} = thinking {thinking} + answer {reply.usage.output_tokens - thinking}")
    print(f"output cost: ${reply.usage.output_tokens * PRICE_OUT / 1_000_000:.4f} (thinking is billed as output, shown or not)")
    quick = request(client, "low")
    print("a low-effort turn may skip thinking:", [b.type for b in quick.content], "| output_tokens", quick.usage.output_tokens)
    print("effort differs between the two requests:", transport.requests[0]["output_config"] != transport.requests[1]["output_config"])


if __name__ == "__main__":
    main()
