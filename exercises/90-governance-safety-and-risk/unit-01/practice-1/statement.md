# Practice: the governance files of a claims assistant

A claims assistant drafts replies, answers policy questions and can issue refunds and close accounts. Its governance is written down in four files, and the draft of them is wrong in several places: a screen lets a request through when it is down, a refund has no person in front of it, an answer goes out unreviewed at a confidence of 80, the audit log keeps the text of every prompt for two years, and the risk register has two rows, no owner and a control that does not exist. In this practice you correct the files. There is no program to write and no model is called: the tests read your files. The numbers the tests check (a confidence of at least 95, a floor of at least 90 days, an erasure within 30 days) are this course's design values, to be tuned to your own error costs and your own legal advice. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the files there; each language folder holds its own copy of the files.

## What to write

The project folder holds four files.

- `governance/controls.json`: the `controls`, each with an `id`, a `layer` (`input`, `output`, `action` or `monitor`), a `tier` (`all` or `high`) and `on_failure` (`hold` or `proceed-flagged`); the list `high_consequence_actions`; and `human_review`, which maps each of those actions to the id of the control that puts a person in front of it.
- `governance/routing.json`: `auto_confidence_min`, `high_consequence_auto` and `unsupported_answer`.
- `governance/retention.json`: the `audit` block (`floor_days`, `ceiling_days`, `retain_days`, `store_content`, `legal_hold_overrides_ceiling`) and the `erasure` block (`remove_vault_mapping`, `max_days_to_complete`).
- `docs/risk-register.md`: one sentence on disclosure, and a table with the columns Risk, Failure mode, Control, Owner and Residual.

Rules the files must follow:

- A control with the tier `high`, or in the layer `input` or `action`, holds when it fails. Only an output or monitor control of the tier `all` may proceed with a flag.
- Every high-consequence action has a human review step: a control that exists, has the tier `high` and holds.
- `auto_confidence_min` is a whole number from 95 to 100. A high-consequence action is never automatic, and an unsupported answer is held.
- The audit window has a floor of at least 90 days. The days kept lie between the floor and the ceiling (the ceiling itself is allowed), the log stores no content, and a legal hold outranks the ceiling.
- The register has at least four rows covering a hallucination, a prompt injection, a privacy leak and an unfair outcome. Each row names a control that is defined in `controls.json` and a named owner (a role, not "TBD").
- The register says that users are told that AI helped produce the output.
- Erasure removes the map from tokens to people and completes in at most 30 days.
- No file holds a home path or an address other than `example.com`.

## Why each part is there, and what you should see

1. **A control that fails open on an input or an action is a hole.** When the screen is down, a request that is let through unscreened is the one an attacker waits for. *You should see* every input and action control hold, and only an output or monitor control proceed, flagged.
2. **A person in front of an irreversible action is a control, and a line in a prompt is not.** *You should see* each high-consequence action mapped to a high-tier control that holds.
3. **A threshold is a decision about error cost.** *You should see* an automatic threshold of at least 95, no automatic path for a high-consequence action, and a held answer when the source does not support it.
4. **The audit log proves what happened and is not a second copy of the data.** *You should see* a floor, a ceiling, a stored period between them, no content, and a legal hold that outranks the ceiling.
5. **A risk with no owner and no control is a wish.** *You should see* four rows, each with a control that exists and an owner you could telephone.
6. **People are told, and the data can be unlinked.** *You should see* the disclosure sentence and an erasure that removes the vault mapping within 30 days.

## The cases

| Id | What it checks |
|---|---|
| `m1` | No control that guards an input or an action fails open |
| `e1` | Every high-consequence action has a human step that exists, is high tier and holds |
| `e2` | The automatic threshold is at least 95 (exactly 95 is allowed), a high-consequence action is never automatic and an unsupported answer is held |
| `e3` | The audit floor is at least 90 days, the days kept lie within the ceiling, no content is stored and a legal hold wins |
| `e4` | The register has four rows with a defined control, a named owner and the four failure modes |
| `e5` | The register says users are told that AI helped, and no file holds a personal path or address |
| `e6` | Erasure removes the vault mapping and completes within 30 days (exactly 30 is allowed) |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
