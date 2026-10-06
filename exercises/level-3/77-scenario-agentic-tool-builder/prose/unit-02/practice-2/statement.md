# Practice: review a tool that an agent proposes

A team lets its agent write its own tools: the agent proposes a name, a description, the permissions it needs, the limits it wants and the code. Before any proposed tool runs, a gate reads the
proposal and decides: approve it, approve it behind a person's sign-off, send it back for a fix, or refuse it. In this practice you write that gate. The model is not called and no proposed code
is ever executed: the gate reads the text of the proposal. The shapes of the proposal, of the policy and of the report, and the crude scan of the code, are this course's own design for the
capstone, not an Anthropic interface, and a text scan is not a sandbox: it is the cheap first check in front of one. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`review`); TypeScript has the same name and the same snake-case fields (`timeout_s`, `memory_mb`). Java has the static method `ToolReview.review` and the records `Proposal`,
`Policy` and `Report` with camel-case fields (`timeoutS`, `memoryMb`, `minWords`); Kotlin has the top-level function `review` and the data classes of the same names.

## What is already written, and what you write

The starter is a working review with six gaps cut out of it. The forbidden-token list, the table of markers that shows which permission a piece of code uses, and the assembly of the report are written and correct. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file; a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `findings_of` unlocks `e1`, `e2` and `e3`: the name, the description and the limits.
2. `forbidden_calls` unlocks `e4`: the forbidden tokens in the code, alphabetical.
3. `permissions_used` unlocks `e5`, `e6` and `e8`: the permissions the code shows, alphabetical.
4. `refusals_of` unlocks `e4` and `e5`: the three groups of refusals, in order.
5. `is_gated` unlocks `e6`: whether a declared permission needs a person's approval.
6. `decide` unlocks `m1`, `e6` and `e7`: refuse, then revise, then gate, then approve.

About fifteen lines in all.

## What to write

A proposal is `{name, description, permissions, timeout_s, memory_mb, code}`; `permissions` is a list drawn from `read_files`, `write_files`, `network` and `run_process`. The policy is
`{min_words, max_timeout, max_memory, denied, approval}`: the fewest words of a description, the largest timeout in seconds and memory in megabytes, the permissions that are never granted, and the
permissions that need a person's approval.

`review(proposal, policy)` returns a report `{name, decision, refusals, findings, used, audit}`:

- `findings`, in this order: `bad_name` when the name does not match lower-case letters, digits and underscores, starting with a letter, 3 to 64 characters long (`[a-z][a-z0-9_]{2,63}`);
  `short_description` when the description has fewer than `min_words` words; `timeout` when `timeout_s` is greater than `max_timeout`; `memory` when `memory_mb` is greater than `max_memory`.
- `used`: the permissions the code text shows, in alphabetical order. The scan looks for these markers in the code: `network`: `requests.`, `urllib`; `write_files`: `.write(`, `shutil.`;
  `read_files`: `open(`, `.read(`; `run_process`: `subprocess`.
- `refusals`, in this order: `forbidden:<token>` for each of `os.system`, `subprocess`, `eval(`, `exec(` and `__import__` that the code contains, in alphabetical order; `undeclared:<permission>` for each
  permission in `used` that the proposal does not declare, in alphabetical order; `denied:<permission>` for each declared permission that the policy denies, in alphabetical order.
- `decision`: `refuse` when there are refusals; otherwise `revise` when there are findings; otherwise `approve_with_gate` when any declared permission needs approval; otherwise `approve`. `audit` is
  `<name>: <decision>`.

## Why each part is there, and what you should see

1. **A description is the routing contract.** A tool the model cannot choose correctly is not ready. *You should see* 12 words accepted with a minimum of 12, and 11 sent back.
2. **Limits are met at the limit.** *You should see* a timeout of exactly 10 seconds and memory of exactly 256 accepted, and one more of either flagged, and a name of exactly 64 characters accepted.
3. **What the code does must match what the proposal says.** A tool that declares reading and fetches a web page is lying. *You should see* an undeclared permission refused, however well the
   rest is written.
4. **An irreversible effect waits for a person.** *You should see* a declared write approved only behind a gate, and a read-only tool approved outright.
5. **Refuse before you revise.** A fix cannot make a forbidden call acceptable. *You should see* a refusal that also has a finding stay a refusal.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A well-formed read-only tool is approved and leaves an audit line |
| `e1` | A description needs at least the minimum number of words |
| `e2` | The timeout and the memory may equal their limits and not exceed them |
| `e3` | A name is lower-case snake case of at most 64 characters |
| `e4` | Forbidden calls in the code refuse the tool and are all listed in order |
| `e5` | A permission the code uses without declaring it, or a denied one, refuses the tool |
| `e6` | A declared write is approved only behind a gate, and a read alone is approved outright |
| `e7` | A refusal beats a revision and a revision beats a gate |
| `e8` | The permissions the code uses are reported in alphabetical order |
