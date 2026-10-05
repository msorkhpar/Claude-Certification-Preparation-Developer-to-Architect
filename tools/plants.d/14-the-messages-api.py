# Planted wrong solutions of module 14-the-messages-api: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/14-the-messages-api/unit-01/practice-1"] = {
    "python": ("conversation.py", {
        "wrong-send-last-only": [('"messages": copy.deepcopy(self._history)}', '"messages": copy.deepcopy(self._history[-1:])}')],
        "wrong-no-rollback": [("            self._history.pop()\n            raise", "            raise")],
        "wrong-shared-list": [('"messages": copy.deepcopy(self._history)}', '"messages": self._history}')],
    }),
    "typescript": ("conversation.ts", {
        "wrong-send-last-only": [("messages: structuredClone(this._history)", "messages: structuredClone(this._history.slice(-1))")],
        "wrong-no-rollback": [("      this._history.pop();\n      throw err;", "      throw err;")],
        "wrong-shared-list": [("messages: structuredClone(this._history)", "messages: this._history")],
    }),
    "java": ("Conversation.java", {
        "wrong-send-last-only": [('body.put("messages", copy(history));', 'body.put("messages", copy(history.subList(history.size() - 1, history.size())));')],
        "wrong-no-rollback": [("            history.remove(history.size() - 1);\n            throw e;", "            throw e;")],
        "wrong-shared-list": [('body.put("messages", copy(history));', 'body.put("messages", history);')],
    }),
    "kotlin": ("Conversation.kt", {
        "wrong-send-last-only": [('"messages" to copy(history))', '"messages" to copy(history.takeLast(1)))')],
        "wrong-no-rollback": [("            history.removeAt(history.size - 1)\n            throw e", "            throw e")],
        "wrong-shared-list": [('"messages" to copy(history))', '"messages" to history)')],
    }),
}
