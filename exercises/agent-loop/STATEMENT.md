# Practice: a tiny agent loop (survey proof, illustrative)

Write `run_agent(model, tools, user_text, max_turns=5)` in `agent.py`.

- `model(messages)` returns a Messages-API-shaped dict: `content` is a list of blocks
  (`text` or `tool_use`) and `stop_reason` is `"tool_use"` or `"end_turn"`.
- While `stop_reason` is `"tool_use"`: append the assistant turn to the history, run every
  `tool_use` block through `tools[name](**input)`, and append ONE user turn holding a
  `tool_result` block per call (same `tool_use_id`).
- A tool that raises, or an unknown tool name, gives a `tool_result` with `is_error: true`
  and the message as content; the loop goes on.
- On `end_turn` return the text of the last assistant turn.
- Past `max_turns` model calls raise `RuntimeError`.

The scripted model in `tests/scripted.py` is hand-written and illustrative, not a capture.
