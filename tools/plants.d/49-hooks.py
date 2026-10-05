# Planted wrong solutions of module 49-hooks: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/49-hooks/unit-01/practice-1"] = {
    "python": ("hooks.py", {
        "wrong-200-asks": [("if amount <= AUTO_LIMIT:", "if amount < AUTO_LIMIT:")],
        "wrong-500-denied": [("if amount <= ASK_LIMIT:", "if amount < ASK_LIMIT:")],
        "wrong-missing-amount-allowed": [('.get("amount")\n    if isinstance(amount, bool)', '.get("amount", 0.01)\n    if isinstance(amount, bool)')],
        "wrong-coerces-amount": [('.get("amount")\n    if isinstance(amount, bool)', '.get("amount")\n    if isinstance(amount, (str, bool)):\n        amount = float(amount)\n    if isinstance(amount, bool)')],
        "wrong-gates-every-tool": [('    if input_data.get("tool_name") != "process_refund":\n        return {}\n', "")],
        "wrong-always-milliseconds": [("seconds = value / 1000 if value > 1e11 else value", "seconds = value / 1000")],
        "wrong-cents-kept": [('cents = out.pop("amount_cents", None)', 'cents = out.get("amount_cents")')],
        "wrong-rewrites-plain-text": [("    except ValueError:\n        return {}\n    if not isinstance(data, dict):", '    except ValueError:\n        return _answer("PostToolUse", updatedToolOutput="null")\n    if not isinstance(data, dict):')],
        "wrong-not-idempotent": [("    if out == data:\n        return {}\n", "")],
        "wrong-no-matcher": [('HookMatcher(matcher="process_refund", ', "HookMatcher(")],
        "wrong-no-timeout": [("hooks=[pre_refund], timeout=5", "hooks=[pre_refund]")],
        "wrong-exit-one": [('return {"exit": 2, "stderr": reason}', 'return {"exit": 1, "stderr": reason}')],
        "wrong-bad-input-passes": [('return {"exit": 2, "stderr": "The hook input', 'return {"exit": 0, "stderr": "The hook input')],
        "wrong-settings-no-timeout": [('"command": script, "timeout": timeout}', '"command": script}')],
        "wrong-turn-limit-too-low": [("max_turns=6", "max_turns=2")],
    }),
    "typescript": ("hooks.ts", {
        "wrong-200-asks": [("if (amount <= AUTO_LIMIT)", "if (amount < AUTO_LIMIT)")],
        "wrong-500-denied": [("if (amount <= ASK_LIMIT)", "if (amount < ASK_LIMIT)")],
        "wrong-missing-amount-allowed": [("const amount = input?.tool_input?.amount;", "const amount = input?.tool_input?.amount ?? 0.01;")],
        "wrong-coerces-amount": [("const amount = input?.tool_input?.amount;", 'const given = input?.tool_input?.amount;\n  const amount = typeof given === "string" || typeof given === "boolean" ? Number(given) : given;')],
        "wrong-gates-every-tool": [('  if (input?.tool_name !== "process_refund") return {};\n', "")],
        "wrong-always-milliseconds": [("new Date(value > 1e11 ? value : value * 1000)", "new Date(value)")],
        "wrong-cents-kept": [("    delete out.amount_cents;\n", "")],
        "wrong-rewrites-plain-text": [("    } catch {\n      return {};\n    }\n  }\n  if (data === null", '    } catch {\n      return answer("PostToolUse", { updatedToolOutput: "null" });\n    }\n  }\n  if (data === null')],
        "wrong-not-idempotent": [("return changed ? answer(", "return true ? answer(")],
        "wrong-no-matcher": [('{ matcher: "process_refund", hooks', "{ hooks")],
        "wrong-no-timeout": [("hooks: [preRefund], timeout: 5", "hooks: [preRefund]")],
        "wrong-exit-one": [("return { exit: 2, stderr: reason }", "return { exit: 1, stderr: reason }")],
        "wrong-bad-input-passes": [("return { exit: 2, stderr: \"The hook input", "return { exit: 0, stderr: \"The hook input")],
        "wrong-settings-no-timeout": [("command: script, timeout }", "command: script }")],
        "wrong-turn-limit-too-low": [("maxTurns: 6", "maxTurns: 2")],
    }),
}
