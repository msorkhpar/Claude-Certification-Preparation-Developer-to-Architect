"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from platforms import PlatformError, build_request, unsupported_features

body = {"model": "ignored-by-the-builder", "max_tokens": 256, "messages": [{"role": "user", "content": "Hello, Claude"}]}
OPUS = "claude-opus-5-5"

# The same message for three front doors: the URL, the model id and the version header change.
try:
    for platform, config in (("anthropic", {}), ("bedrock", {"region": "us-east-1"}), ("vertex", {"project": "my-project"})):
        request = build_request(platform, OPUS, dict(body), config) or {}
        print(platform, "->", request.get("url"))
        print("   model in body:", (request.get("body") or {}).get("model"), "| headers:", sorted(request.get("headers") or {}))
except PlatformError as err:
    print("platform error:", err)

# What a team would lose by moving to Bedrock.
print("missing on bedrock:", unsupported_features("bedrock", ["batches", "fast_mode", "files_api"]))
