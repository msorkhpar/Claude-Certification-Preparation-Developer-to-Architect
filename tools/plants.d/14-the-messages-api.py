# Planted wrong solutions of module 14-the-messages-api: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/14-the-messages-api/unit-01/practice-1"] = {
    "python": ("conversation.py", {
        "wrong-assistant-text-only": [('return {"role": "assistant", "content": response["content"]}', 'return {"role": "assistant", "content": self._reply(response).text}')],
        "wrong-usage-input-overwritten": [('self._totals["input_tokens"] += usage.get("input_tokens", 0)', 'self._totals["input_tokens"] = usage.get("input_tokens", 0)')],
        "wrong-no-rollback": [("            self._history.pop()\n            raise", "            raise")],
        "wrong-truncated-on-any-stop": [('stop == "max_tokens")', 'stop != "end_turn")')],
        "wrong-stop-sequences-dropped": [('        if self.stop_sequences:\n            body["stop_sequences"] = list(self.stop_sequences)\n', "")],
        "wrong-history-not-copied": [("return copy.deepcopy(self._history)", "return self._history")],
        "wrong-blank-turn-accepted": [("if not isinstance(text, str) or not text.strip():", "if not isinstance(text, str):")],
    }),
    "typescript": ("conversation.ts", {
        "wrong-assistant-text-only": [('return { role: "assistant", content: response.content };', 'return { role: "assistant", content: this.makeReply(response).text };')],
        "wrong-usage-input-overwritten": [("this._totals.input_tokens += usage?.input_tokens ?? 0;", "this._totals.input_tokens = usage?.input_tokens ?? 0;")],
        "wrong-no-rollback": [("      this._history.pop();\n      throw err;", "      throw err;")],
        "wrong-truncated-on-any-stop": [('truncated: response.stop_reason === "max_tokens"', 'truncated: response.stop_reason !== "end_turn"')],
        "wrong-stop-sequences-dropped": [("    if (this.stopSequences && this.stopSequences.length > 0) body.stop_sequences = [...this.stopSequences];\n", "")],
        "wrong-history-not-copied": [("return structuredClone(this._history);", "return this._history;")],
        "wrong-blank-turn-accepted": [('if (typeof text !== "string" || text.trim() === "") throw', 'if (typeof text !== "string") throw')],
    }),
    "java": ("Conversation.java", {
        "wrong-assistant-text-only": [('assistant.put("content", response.get("content"));', 'assistant.put("content", makeReply(response).text());')],
        "wrong-usage-input-overwritten": [('if (usage.get("input_tokens") instanceof Number n) inputTokens += n.longValue();', 'if (usage.get("input_tokens") instanceof Number n) inputTokens = n.longValue();')],
        "wrong-no-rollback": [("            history.remove(history.size() - 1);\n            throw e;", "            throw e;")],
        "wrong-truncated-on-any-stop": [('"max_tokens".equals(stop)', '!"end_turn".equals(stop)')],
        "wrong-stop-sequences-dropped": [('if (stopSequences != null && !stopSequences.isEmpty()) body.put("stop_sequences", new ArrayList<>(stopSequences));', "")],
        "wrong-history-not-copied": [("return copy(history);", "return history;")],
        "wrong-blank-turn-accepted": [("if (text == null || text.isBlank()) throw", "if (text == null) throw")],
    }),
    "kotlin": ("Conversation.kt", {
        "wrong-assistant-text-only": [('"content" to response["content"])', '"content" to makeReply(response).text)')],
        "wrong-usage-input-overwritten": [('(usage["input_tokens"] as? Number)?.let { inputTokens += it.toLong() }', '(usage["input_tokens"] as? Number)?.let { inputTokens = it.toLong() }')],
        "wrong-no-rollback": [("            history.removeAt(history.size - 1)\n            throw e", "            throw e")],
        "wrong-truncated-on-any-stop": [('stop == "max_tokens")', 'stop != "end_turn")')],
        "wrong-stop-sequences-dropped": [('if (!stopSequences.isNullOrEmpty()) body["stop_sequences"] = stopSequences.toList()', "")],
        "wrong-history-not-copied": [("MutableList<MutableMap<String, Any?>> = copy(history)", "MutableList<MutableMap<String, Any?>> = history")],
        "wrong-blank-turn-accepted": [('require(text.isNotBlank()) { "a turn needs text" }', 'require(true) { "a turn needs text" }')],
    }),
}
