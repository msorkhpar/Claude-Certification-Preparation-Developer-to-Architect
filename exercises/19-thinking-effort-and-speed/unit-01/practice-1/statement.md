# Practice: thinking, effort and speed set correctly per model

Write the function that turns what an application wants (thinking mode, effort, fast mode, sampling) into request
parameters for one model, and refuses the combinations the API would answer with a 400. Pick your language folder
(`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. The rules come from the
documentation read on 2026-10-02 and are summarised in `docs/VERSIONS.md`; the lesson pages explain them.

## The given types

| Name | Meaning |
|---|---|
| `RejectedRequest` | the error to raise: `param` names the offending parameter (`thinking`, `thinking.budget_tokens`, `output_config.effort`, `temperature`, `top_p`, `top_k`, `speed`, `max_tokens` or `model`) |
| `options` | a map that may hold `thinking` (a map with `type`, and `budget_tokens` for `enabled`), `effort` (a string), `speed` (`fast` or `standard`), `batch` (true when the request goes into a Message Batch), `temperature`, `top_p`, `top_k` |

The four models are `claude-fable-5-1`, `claude-opus-5-5`, `claude-sonnet-5-5` and `claude-haiku-4-5-20251001` (the
alias `claude-haiku-4-5` counts as Haiku). Any other model id is rejected with `param` set to `model`.

## `build_params(model, max_tokens, options)` (TypeScript `buildParams`, Java `Params.buildParams`, Kotlin `buildParams`)

Return a map with `model`, `max_tokens` and only the keys the options asked for, or raise `RejectedRequest`:

1. **`max_tokens`** below 1 is rejected.
2. **Effort** goes into `output_config` as `{"effort": level}`, never into `thinking`. Haiku 4.5 does not support effort.
   The levels are `low`, `medium`, `high`, `xhigh` and `max`; anything else (`adaptive` is a thinking mode, not a level)
   is rejected. Fable 5.1, Opus 5.5 and Sonnet 5.5 take all five.
3. **Thinking.** Fable 5.1 and Opus 5.5 take adaptive thinking only: `enabled` and `disabled` are rejected. Sonnet 5.5
   takes `adaptive`, rejects `enabled` and `disabled`, and also takes `between_tools` (thinking off up front) when the
   effective effort is `low`, `medium` or `high` (the effort given, or the model's default: `high` on Sonnet 5.5 and
   Fable 5.1, `medium` on Opus 5.5). Haiku 4.5 takes `enabled` with a `budget_tokens` of at least 1,024 and below
   `max_tokens`, and `disabled`; it rejects `adaptive` and `between_tools`. No `thinking` option means no `thinking` key.
4. **Sampling.** Fable 5.1, Opus 5.5 and Sonnet 5.5 reject `top_p`, `top_k` and any `temperature` other than 1.0.
   Haiku 4.5 passes all three through.
5. **Fast mode** (`speed` of `fast`) exists on Opus 5.5 only and is refused inside a batch; it adds `speed: "fast"` and
   `betas: ["fast-mode-2026-02-01"]` to the result. A `speed` of `standard` adds nothing.
6. Never change the `options` you were given.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each course model gets the request it accepts |
| `e1` | Thinking modes a model does not have are refused |
| `e2` | The mode that skips thinking up front is Sonnet only and needs high effort or below |
| `e3` | Effort needs a supporting model and a real level |
| `e4` | Newer models reject sampling parameters and Haiku keeps them |
| `e5` | Fast mode is Opus only, carries its beta header and is never sent in a batch |
| `e6` | A manual budget is at least 1,024 and below `max_tokens`; `max_tokens` is at least 1 |
| `e7` | Effort lives in `output_config`, never inside `thinking`, and the options are left unchanged |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
