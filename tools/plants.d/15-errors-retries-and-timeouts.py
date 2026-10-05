# Planted wrong solutions of module 15-errors-retries-and-timeouts: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/15-errors-retries-and-timeouts/unit-01/practice-1"] = {
    "python": ("retry.py", {
        "wrong-retry-everything": [("    if response.status in RETRYABLE or response.status >= 500:", "    if response.status >= 400:")],
        "wrong-ignore-retry-after": [("            wait = _retry_after(response)", "            wait = 0.0")],
        "wrong-no-cap": [("jitter(min(cap, base_delay * 2 ** (attempt - 1)))", "jitter(base_delay * 2 ** (attempt - 1))")],
        "wrong-sleep-after-last": [("            if attempt >= max_attempts:\n                raise _failure(response, attempt)",
                                    "            if attempt >= max_attempts:\n                sleep(base_delay)\n                raise _failure(response, attempt)")],
    }),
    "typescript": ("retry.ts", {
        "wrong-retry-everything": [("if (response.status === 408 || response.status === 409 || response.status === 429 || response.status >= 500) {", "if (response.status >= 400) {")],
        "wrong-ignore-retry-after": [("      wait = retryAfter(response);", "      wait = 0;")],
        "wrong-no-cap": [("Math.min(cap, baseDelay * 2 ** (attempt - 1))", "baseDelay * 2 ** (attempt - 1)")],
        "wrong-sleep-after-last": [("      if (!retryable(response) || attempt >= maxAttempts) throw failure(response, attempt);",
                                    "      if (!retryable(response)) throw failure(response, attempt);\n      if (attempt >= maxAttempts) {\n        sleep(baseDelay);\n        throw failure(response, attempt);\n      }")],
    }),
    "java": ("Retry.java", {
        "wrong-retry-everything": [("if (s == 408 || s == 409 || s == 429 || s >= 500) {", "if (s >= 400) {")],
        "wrong-ignore-retry-after": [("                wait = retryAfter(response);", "                wait = 0.0;")],
        "wrong-no-cap": [("Math.min(policy.cap(), policy.baseDelay() * Math.pow(2, attempt - 1))", "policy.baseDelay() * Math.pow(2, attempt - 1)")],
        "wrong-sleep-after-last": [("                if (!retryable(response) || attempt >= policy.maxAttempts()) throw failure(response, attempt);",
                                    "                if (!retryable(response)) throw failure(response, attempt);\n                if (attempt >= policy.maxAttempts()) {\n                    sleep.sleep(policy.baseDelay());\n                    throw failure(response, attempt);\n                }")],
    }),
    "kotlin": ("Retry.kt", {
        "wrong-retry-everything": [("if (s == 408 || s == 409 || s == 429 || s >= 500) {", "if (s >= 400) {")],
        "wrong-ignore-retry-after": [("            wait = retryAfter(response)", "            wait = 0.0")],
        "wrong-no-cap": [("minOf(policy.cap, policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble()))", "policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble())")],
        "wrong-sleep-after-last": [("            if (!retryable(response) || attempt >= policy.maxAttempts) throw failure(response, attempt)",
                                    "            if (!retryable(response)) throw failure(response, attempt)\n            if (attempt >= policy.maxAttempts) {\n                sleep(policy.baseDelay)\n                throw failure(response, attempt)\n            }")],
    }),
}
