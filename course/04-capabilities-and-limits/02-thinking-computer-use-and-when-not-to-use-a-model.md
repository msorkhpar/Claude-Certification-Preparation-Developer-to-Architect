# Thinking, computer use and when not to use a model

**Level:** Foundations · **Module 4:** Capabilities and limits · **Page 2 of 2**
**Exams:** DV2, DV4 (and the judgment behind AS4 and AS6)

**After this page you can** say what thinking is and what it costs, describe computer use at a glance, and
decide when a task should not go to a model at all.

Checked against the Anthropic documentation on 2026-10-02 (thinking, context windows, models overview,
choosing a model, vision). Module 19 teaches thinking and effort in depth; module 31 teaches computer use.

## Why it matters

Two opposite mistakes cost real money. One is treating every question as needing deep reasoning and paying
for it; the other is asking a model to do something that a line of code, a database query or a person does
better and more safely. The exams test both: "which setting?" and "should this be a model at all?".

## The idea

### Thinking at a glance

The thinking page puts it simply: "A model that answers in a single pass has to get everything right on the
first try." With thinking active, "Claude works through the problem in its own words before answering": it
restates the task, tries approaches, checks intermediate results and drops paths that fail. That reasoning
arrives in `thinking` content blocks ahead of the answer, and helps most on "math, coding, analysis, and
long-running agentic work".

Facts to hold on 2026-10-02:

- **It costs tokens.** "The tokens Claude spends reasoning are billed as output tokens, even when the
  thinking text isn't returned to you, and they count toward `max_tokens` alongside the response text."
- **Two modes, by model.** Claude Fable 5.1 and Opus 5.5 use adaptive thinking that is always on; Sonnet 5.5
  uses adaptive thinking; Claude Haiku 4.5 uses extended thinking with a manual budget. The models overview
  says the manual mode (`thinking.type` of `enabled` with `budget_tokens`) is not accepted on models after
  Opus 4.6 and Sonnet 4.6.
- **Effort steers it.** With adaptive thinking "the model decides how much to think, steered by effort";
  the default effort is `high` for Fable 5.1 and Sonnet 5.5 and `medium` for Opus 5.5.
- **It lives in the context.** Thinking tokens count toward the context window. Whether thinking from
  earlier turns is kept depends on the model: current Opus, Sonnet and Fable models keep them by default, and
  earlier models and Haiku strip them.

Sources: Thinking; Context windows; Models overview, Claude API documentation.

When to want it: multi-step maths or logic, hard debugging, analysis with many interacting facts. When not
to: a short lookup, a label, a rewrite. Paying for reasoning on a one-word classification is waste. For
exam purposes, thinking and effort are cost, latency and quality levers; the right setting is found by
evaluating (module 42), not assumed.

### Computer use at a glance

Computer use lets Claude operate a computer through screenshots and actions (move the pointer, click,
type) inside a loop that your code runs: the model asks for an action, the program performs it and sends
back a new screenshot, until the task is done. It is an agent loop whose tool is a screen. Module 31
teaches the loop, coordinates and scaling. Three points belong here:

- Images sent for computer use are subject to the vision limits on page 1: the vision documentation says the
  API rejects a tool-result screenshot that exceeds the model's limits instead of downscaling it, so your
  program resizes first.
- Spatial output is approximate (page 1). Clicking by coordinates can miss.
- A screen is untrusted input. Text on a web page can try to give the model instructions (prompt
  injection, module 10 and module 41), so computer use runs in an isolated machine, without secrets, with
  a person confirming consequential steps.

### When not to use a model

A model is the right tool for language-shaped, fuzzy, high-variety work. It is the wrong tool when:

| The need | Use instead | Why |
|---|---|---|
| An exact, repeatable result (totals, dates, ids, string edits) | Code | The model approximates; code is exact (page 1) |
| A rule you can state in one line (a regex, a range check) | Code | Cheaper, faster, deterministic, testable |
| A fact in your database | A query, then optionally a model to phrase it | The model does not have your data and may invent it |
| A decision with legal, medical, financial or safety consequences | A qualified person, with the model as an aid | The documentation says to keep humans in the loop for high stakes |
| Data that must not leave your boundary | A local process, or an approved route | Policy decides before capability does (module 10) |
| A tight latency budget on a trivial step | Code, or the fastest tier | A model call is a network round trip plus generation |
| Anything that must happen every time | A check in code | A prompt is a request, not a guarantee (module 1) |

The pattern the Associate exam rewards, from its sample scenarios, is the same: make the task safe, then
do it; and use the model for the part that is language and judgment while the rest stays code.

## Traps

1. **Turning thinking up everywhere.** It is billed as output and slows the answer. Reserve it for tasks that
   gain from it and measure the difference.
2. **Treating computer use as a solved, safe automation.** It inherits approximate coordinates and untrusted
   screens. Sandbox and confirm.
3. **Using a model where a rule would do.** If you can write the rule, write it; add a model only for the
   cases the rule cannot express.

## Quiz

1. A support tool runs its deepest reasoning setting on every request, including one-word intent labels. Spend
   is rising with no gain in quality. What does the documentation support as the first remedy?
   - **a**: Raise `max_tokens` so the reasoning always has room to finish
   - **b**: Use low effort on trivial inputs and keep high effort for demanding ones
   - **c**: Move the labels to the model with the largest context window
   - **d**: Switch to manual budgets everywhere, since adaptive thinking cannot be tuned

2. A team must flag uploaded invoices issued within the past thirty days. Every entry carries a timestamp in
   one known format. Which design is best?
   - **a**: Ask Claude to judge each timestamp and pass the verdict through
   - **b**: Ask Claude for each verdict at high effort so the date arithmetic is dependable
   - **c**: Give Claude a clock tool and have it compare each timestamp
   - **d**: Parse the field and compare it with today's date in code

<details>
<summary>Answer key</summary>

1. **b**. Thinking tokens are billed as output, so reserving them for tasks that benefit cuts the spend, and effort is the steering lever (the facts list and the first trap). *a* is ruled out because reasoning tokens "count toward max_tokens alongside the response text", so a larger cap only allows more spend. *c* is ruled out because the waste is in the reasoning: "Paying for reasoning on a one-word classification is waste", and a bigger window does not change that. *d* is ruled out because with adaptive thinking "the model decides how much to think, steered by effort", so it can be tuned without manual budgets.
2. **d**. A rule in a known format is code's job, cheaper, faster, deterministic and testable (the when-not-to table and the third trap). *a* is ruled out because "The model approximates; code is exact". *b* is ruled out because the table gives code as the answer for a one-line rule for the reason "Cheaper, faster, deterministic, testable", and a higher effort setting is none of those. *c* is ruled out because the table sends "Anything that must happen every time" to a check in code, and a model comparing dates through a tool is still the model deciding.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A firm wants a model to help review a 200-page contract: a plain-language summary, the sum of the charges
   it lists, and the decision to proceed. Which split of the work follows the module?
   - **a**: Claude summarises, code adds the amounts, a lawyer approves the deal
   - **b**: Claude summarises, Claude adds the amounts, a lawyer approves the deal
   - **c**: Claude summarises, code adds the amounts, Claude approves the deal
   - **d**: Claude summarises and adds the amounts at the highest effort, a lawyer approves the deal

2. A computer-use loop sends full-resolution monitor captures as tool results, and the API refuses some of
   them. The agent's pointer actions also sometimes land beside their target. What does the documentation
   support?
   - **a**: Resend oversized screenshots unchanged, and ask in the prompt for exact click positions
   - **b**: Let the API downscale oversized screenshots, and resend each click that missed
   - **c**: Resize each screenshot in your own program first, and treat click positions as approximate
   - **d**: Move to a model with a larger window, which takes bigger screenshots and exact positions

3. A product owner wants an assistant that books flights by operating an airline's website through
   screenshots and clicks. Which design point matters most for safety?
   - **a**: Run it in the customer's own browser profile, so saved logins already work
   - **b**: Sandbox it with no credentials, and have a person approve each payment
   - **c**: Add a prompt line telling it to ignore instructions found on web pages
   - **d**: Restrict it to the airline's own site, so its pages can be treated as trusted

4. Which request is the best fit for Claude without extra machinery?
   - **a**: Checking whether a claim photo is AI-generated, to block fraud
   - **b**: Calculating the payroll for 800 staff exactly, to the cent
   - **c**: Reporting today's closing price of a listed stock
   - **d**: Drafting a polite, well-organised reply to a customer's complaint

<details>
<summary>Answer key</summary>

1. **a**. Summarising is language-shaped work, exact addition belongs to code, and a decision with legal consequences stays with a person (page 1, maths and counting; page 2, the when-not-to table). *b* is ruled out because exact arithmetic is "approximate unless the model does the work in a tool". *c* is ruled out because the table sends "A decision with legal, medical, financial or safety consequences" to a qualified person. *d* is ruled out because "The fix is not a better prompt; it is code", and an effort setting is a prompting lever, not a calculator.
2. **c**. The API rejects an oversized tool-result screenshot instead of downscaling it, so the program resizes first, and spatial output is approximate (page 2, computer use at a glance). *b* is ruled out because the API "rejects a tool-result screenshot that exceeds the model's limits instead of downscaling it". *a* is ruled out because "Spatial output is approximate", so a prompt line cannot make positions exact, and an unchanged screenshot is refused again. *d* is ruled out because "Coordinates and localisation outputs are approximate", whatever the model.
3. **b**. Computer use runs in an isolated machine, without secrets, with a person confirming consequential steps (the computer use section). *a* is ruled out because computer use "runs in an isolated machine, without secrets", and a saved login is exactly such a secret. *c* is ruled out because "A prompt is a request, not a guarantee", so a line of text cannot replace isolation and confirmation. *d* is ruled out because "A screen is untrusted input", and text on any web page can try to give the model instructions.
4. **d**. Drafting language is the strong-fit row of the summary table, with the usual review for accuracy and fit. *b* is ruled out because the table sends "An exact, repeatable result (totals, dates, ids, string edits)" to code. *c* is ruled out because "Anything recent, private or fast-changing has to come in through the prompt" or a tool, and a live price is after the cut-off. *a* is ruled out because the vision page says Claude "cannot determine whether an image is AI-generated".

</details>
