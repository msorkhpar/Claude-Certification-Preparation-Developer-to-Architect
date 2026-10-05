"""A capacity and cost model for one workload: the limits it needs, the tier that gives them, and the monthly bill.

The Claude documentation on rate limits (read on 2026-10-04) says that "for most Claude models, only uncached input tokens count toward
your ITPM rate limits": `input_tokens` and `cache_creation_input_tokens` count, `cache_read_input_tokens` do not. The limits of the Start,
Build and Scale tiers below are the documented figures for Claude Sonnet 5.5, and the prices are the documented Sonnet 5.5 prices on the
Claude API (input 2, output 10, 5-minute cache write 2.50, cache read 0.20 dollars per million tokens, batch at half price). Prices and
limits change; re-read them before you plan. Money is kept in whole cents so that every language prints the same figures.
"""
import logging

log = logging.getLogger(__name__)

TIERS = [("Start", 1000, 2_000_000, 400_000), ("Build", 5000, 5_000_000, 1_000_000), ("Scale", 10_000, 10_000_000, 2_000_000)]  # name, RPM, ITPM, OTPM
CENTS_PER_MTOK = {"input": 200, "cache_write": 250, "cache_read": 20, "output": 1000}

CACHED = {"rpm": 800, "input": 1500, "cache_write": 200, "cache_read": 6000, "output": 400}  # tokens per request, requests per minute
UNCACHED = {"rpm": 800, "input": 7700, "cache_write": 0, "cache_read": 0, "output": 400}  # the same prompts with no caching
REQUESTS_PER_MONTH = 2_000_000


def required_capacity(workload, headroom_percent):
    """RPM, ITPM and OTPM to ask for. Cache reads do not count toward ITPM; every figure is rounded up after the headroom."""
    def up(x):
        return -(-x * (100 + headroom_percent) // 100)
    return {"rpm": up(workload["rpm"]), "itpm": up(workload["rpm"] * (workload["input"] + workload["cache_write"])), "otpm": up(workload["rpm"] * workload["output"])}


def smallest_tier(need, tiers):
    for name, rpm, itpm, otpm in tiers:
        if need["rpm"] <= rpm and need["itpm"] <= itpm and need["otpm"] <= otpm:
            return name
    return "Custom"


def monthly_cents(workload, requests, batch_percent):
    """Cents per month. The share of requests sent through the Batch API is billed at half price in every category."""
    log.debug("monthly_cents input: %r", workload)
    per_request = sum(workload[k] * CENTS_PER_MTOK[k] for k in CENTS_PER_MTOK)  # cents times tokens, per million
    return requests * per_request * (200 - batch_percent) // (200 * 1_000_000)


def dollars(cents):
    return f"${cents // 100:,}.{cents % 100:02d}"


def main():
    for label, workload in (("with caching", CACHED), ("without caching", UNCACHED)):
        need = required_capacity(workload, 30)
        print(f"{label}: need {need['rpm']} rpm, {need['itpm']} itpm, {need['otpm']} otpm -> tier {smallest_tier(need, TIERS)}")
    print("monthly bill with caching:", dollars(monthly_cents(CACHED, REQUESTS_PER_MONTH, 0)))
    print("monthly bill without caching:", dollars(monthly_cents(UNCACHED, REQUESTS_PER_MONTH, 0)))
    print("monthly bill with caching and 30 percent batch:", dollars(monthly_cents(CACHED, REQUESTS_PER_MONTH, 30)))


if __name__ == "__main__":
    main()
