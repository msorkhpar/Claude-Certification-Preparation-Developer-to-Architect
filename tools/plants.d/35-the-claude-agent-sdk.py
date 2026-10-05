# Planted wrong solutions of module 35-the-claude-agent-sdk: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/35-the-claude-agent-sdk/unit-01/practice-1"] = {
    "python": ("agent.py", {
        "wrong-rm-not-disallowed": [('disallowed_tools=["Bash(rm *)"], ', "disallowed_tools=[], ")],
        "wrong-edit-in-readonly": [('if tool_name in EDIT_TOOLS and mode != "edit":', "if False:")],
        "wrong-env-variants": [('return base.startswith(".env") and base != ".env.example"', 'return base == ".env"')],
        "wrong-chaining-allowed": [('r"[;&|<>`]|\\$\\("', 'r"[<>`]|\\$\\("')],
        "wrong-danger-no-interrupt": [('return deny("Dangerous command", True)', 'return deny("Dangerous command", False)')],
        "wrong-push-substring": [('r"\\bgit\\s+push\\b"', 'r"\\bgit\\s+push"')],
        "wrong-raises-after-error-result": [("        if not any(isinstance(m, ResultMessage) for m in messages):\n            raise\n", "        raise\n")],
        "wrong-hides-crash": [("        if not any(isinstance(m, ResultMessage) for m in messages):\n            raise\n", "        pass\n")],
        "wrong-status-unmapped": [(', "error_during_execution": "failed"}', "}")],
        "wrong-denied-capped": [("denied += _denied(message.content)", "denied = min(denied + _denied(message.content), 1)")],
    }),
    "typescript": ("agent.ts", {
        "wrong-rm-not-disallowed": [('disallowedTools: ["Bash(rm *)"], ', "disallowedTools: [], ")],
        "wrong-edit-in-readonly": [('if (EDIT_TOOLS.includes(toolName) && mode !== "edit")', "if (false)")],
        "wrong-env-variants": [('base.startsWith(".env") && base !== ".env.example";', 'base === ".env";')],
        "wrong-chaining-allowed": [("/[;&|<>`]|\\$\\(/", "/[<>`]|\\$\\(/")],
        "wrong-danger-no-interrupt": [('return deny("Dangerous command", true)', 'return deny("Dangerous command", false)')],
        "wrong-push-substring": [("/\\bgit\\s+push\\b/", "/\\bgit\\s+push/")],
        "wrong-raises-after-error-result": [('    if (!messages.some((m) => m.type === "result")) throw error;', "    throw error;")],
        "wrong-hides-crash": [('    if (!messages.some((m) => m.type === "result")) throw error;', "    void error;")],
        "wrong-status-unmapped": [(', error_during_execution: "failed" }', " }")],
        "wrong-denied-capped": [("denied += countDenied(m.message.content);", "denied = Math.min(denied + countDenied(m.message.content), 1);")],
    }),
}
