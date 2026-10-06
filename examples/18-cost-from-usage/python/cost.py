"""Count tokens before sending, then price the reply from its usage object.

Both calls go through a scripted transport, so nothing leaves the container. The replies are illustrative,
hand-written responses in the API's shapes (claude-sonnet-5-5), not captures. Prices are dollars per million
tokens, read from the Claude pricing page on 2026-10-02.
"""
import logging
from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
PRICES = {  # input, output, cache-read multiplier
    "claude-haiku-4-5-20251001": (1.0, 5.0, 0.1),
    "claude-sonnet-5-5": (2.0, 10.0, 0.1),
    "claude-opus-5-5": (4.0, 20.0, 0.05),
    "claude-fable-5-1": (10.0, 50.0, 0.025),
}
PARAMS = dict(model=MODEL, max_tokens=300, system="You answer from the policy document.",
              messages=[{"role": "user", "content": "Summarise the refund policy in two sentences."}])
USAGE = {"input_tokens": 120, "output_tokens": 340, "cache_read_input_tokens": 0, "cache_creation_input_tokens": 4000,
         "cache_creation": {"ephemeral_5m_input_tokens": 4000, "ephemeral_1h_input_tokens": 0}}


def cost(model, usage, batch=False):
    """Dollars for one request: cache writes cost 1.25x input (5 minutes) or 2x (1 hour), reads a model-specific fraction."""
    price_in, price_out, read = PRICES[model]
    per_token = (usage.input_tokens * price_in
                 + usage.cache_creation.ephemeral_5m_input_tokens * price_in * 1.25
                 + usage.cache_creation.ephemeral_1h_input_tokens * price_in * 2.0
                 + usage.cache_read_input_tokens * price_in * read
                 + usage.output_tokens * price_out)
    return per_token / 1_000_000 * (0.5 if batch else 1.0)


def main():
    client, transport = scripted_client({"input_tokens": 4821}, message([text("Refunds take 14 days. Opened items are excluded.")], usage=USAGE))
    counted = client.messages.count_tokens(model=MODEL, system=PARAMS["system"], messages=PARAMS["messages"])
    print(f"count_tokens -> {counted.input_tokens} input tokens (POST {transport.urls[0].split('.com')[1]})")
    price_in = PRICES[MODEL][0]
    print(f"estimate before sending, input only: ${counted.input_tokens * price_in / 1_000_000:.6f} on {MODEL}")
    reply = client.messages.create(**PARAMS)
    u = reply.usage
    print(f"usage: input {u.input_tokens}, cache write {u.cache_creation_input_tokens}, cache read {u.cache_read_input_tokens}, output {u.output_tokens}")
    print(f"cost of this request: ${cost(MODEL, u):.6f}  (batch: ${cost(MODEL, u, batch=True):.6f})")
    print("the same usage on each model:")
    for model in PRICES:
        print(f"  {model:28} ${cost(model, u):.6f}")


if __name__ == "__main__":
    main()
