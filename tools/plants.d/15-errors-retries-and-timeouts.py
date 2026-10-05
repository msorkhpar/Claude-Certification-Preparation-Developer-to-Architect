# Planted wrong solutions of module 15-errors-retries-and-timeouts: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/15-errors-retries-and-timeouts/unit-01/practice-1"] = {
    "python": ("retry.py", {
        "wrong-retry-everything": [("    return status in RETRYABLE or status >= 500", "    return status >= 400")],
        "wrong-ignore-retry-after": [("            wait = _retry_after(response)", "            wait = 0.0")],
        "wrong-no-cap": [("jitter(min(cap, base_delay * 2 ** (attempt - 1)))", "jitter(base_delay * 2 ** (attempt - 1))")],
        "wrong-spend-cap-retried": [('    return details.get("error_code") == "enforced_spend_limit_reached"', "    return False")],
        "wrong-connection-not-retried": [("        except TransportError:\n            if attempt >= max_attempts:\n                raise _connection_failure(attempt)",
                                          "        except TransportError:\n            if True:\n                raise _connection_failure(attempt)")],
        "wrong-sleep-after-last": [("            if attempt >= max_attempts:\n                raise _failure(response, attempt)",
                                    "            if attempt >= max_attempts:\n                sleep(base_delay)\n                raise _failure(response, attempt)")],
    }),
    "typescript": ("retry.ts", {
        "wrong-retry-everything": [("return status === 408 || status === 409 || status === 429 || status >= 500;", "return status >= 400;")],
        "wrong-ignore-retry-after": [("      wait = retryAfter(response);", "      wait = 0;")],
        "wrong-no-cap": [("jitter(Math.min(cap, baseDelay * 2 ** (attempt - 1)))", "jitter(baseDelay * 2 ** (attempt - 1))")],
        "wrong-spend-cap-retried": [('return response.body?.error?.details?.error_code === "enforced_spend_limit_reached";', "return false;")],
        "wrong-connection-not-retried": [("if (attempt >= maxAttempts) throw connectionFailure(attempt);", "throw connectionFailure(attempt);")],
        "wrong-sleep-after-last": [("      if (!retryable(response) || attempt >= maxAttempts) throw failure(response, attempt);",
                                    "      if (!retryable(response)) throw failure(response, attempt);\n      if (attempt >= maxAttempts) {\n        sleep(baseDelay);\n        throw failure(response, attempt);\n      }")],
    }),
    "java": ("Retry.java", {
        "wrong-retry-everything": [("return s == 408 || s == 409 || s == 429 || s >= 500;", "return s >= 400;")],
        "wrong-ignore-retry-after": [("                wait = retryAfter(response);", "                wait = 0.0;")],
        "wrong-no-cap": [("Math.min(policy.cap(), policy.baseDelay() * Math.pow(2, attempt - 1))", "policy.baseDelay() * Math.pow(2, attempt - 1)")],
        "wrong-spend-cap-retried": [('return error(response).get("details") instanceof Map<?, ?> d && "enforced_spend_limit_reached".equals(d.get("error_code"));', "return false;")],
        "wrong-connection-not-retried": [("if (attempt >= policy.maxAttempts()) throw connectionFailure(attempt);", "throw connectionFailure(attempt);")],
        "wrong-sleep-after-last": [("                if (!retryable(response) || attempt >= policy.maxAttempts()) throw failure(response, attempt);",
                                    "                if (!retryable(response)) throw failure(response, attempt);\n                if (attempt >= policy.maxAttempts()) {\n                    sleep.sleep(policy.baseDelay());\n                    throw failure(response, attempt);\n                }")],
    }),
    "kotlin": ("Retry.kt", {
        "wrong-retry-everything": [("s == 408 || s == 409 || s == 429 || s >= 500", "s >= 400")],
        "wrong-ignore-retry-after": [("            wait = retryAfter(response)", "            wait = 0.0")],
        "wrong-no-cap": [("minOf(policy.cap, policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble()))", "policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble())")],
        "wrong-spend-cap-retried": [('return details?.get("error_code") == "enforced_spend_limit_reached"', "return false")],
        "wrong-connection-not-retried": [("if (attempt >= policy.maxAttempts) throw connectionFailure(attempt)", "throw connectionFailure(attempt)")],
        "wrong-sleep-after-last": [("            if (!retryable(response) || attempt >= policy.maxAttempts) throw failure(response, attempt)",
                                    "            if (!retryable(response)) throw failure(response, attempt)\n            if (attempt >= policy.maxAttempts) {\n                sleep(policy.baseDelay)\n                throw failure(response, attempt)\n            }")],
    }),
}
