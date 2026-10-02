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
  earlier turns is kept depends on the model: current Opus, Sonnet, Fable models keep them by default, and
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

- Images sent for computer use are subject to the vision limits above: the vision documentation says the
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

1. A support tool uses its highest reasoning setting on every request, including one-word intent labels, and costs
   are rising without any quality gain. What does the documentation support as the first remedy?
   - **a**: Disable output billing for reasoning tokens
   - **b**: Lower effort for simple requests and keep it for hard ones
   - **c**: Raise `max_tokens` to give reasoning more room
   - **d**: Move every request to the largest context window

2. A team wants to check whether each uploaded invoice is dated within the past thirty days. Timestamps
   arrive in a single known format. Which design is best?
   - **a**: Ask the model to judge each date and trust the verdict
   - **b**: Send the date to the model three times and vote
   - **c**: Ask the model for its confidence and gate on it
   - **d**: Parse the value and compare it in code

<details>
<summary>Answer key</summary>

1. **b**. Thinking tokens are billed as output, so reserving them for tasks that benefit cuts cost, and effort is the steering parameter. *a* is ruled out because the page states the billing and offers no switch. *c* is ruled out because a larger cap would allow more spend, not less. *d* is ruled out because a bigger window adds capacity and no saving.
2. **d**. A rule in a known format is code's job (the when-not-to table). *a* is ruled out because the model approximates where code is exact. *c* is ruled out because confidence is not evidence. *b* is ruled out because voting spends three calls on a one-line rule and still yields an approximation.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A legal team asks Claude to tell them how many times "indemnify" appears in a 200-page contract and gets
   a number that looks plausible. How should the team proceed?
   - **a**: Search the document with code and use that count
   - **b**: Accept the number, since the contract fits the window
   - **c**: Ask for a recount at a lower temperature
   - **d**: Split the contract in halves and average two counts

2. A property app asks Claude to read a handwritten, sideways, low-resolution photo of a meter. Which risk
   does the vision documentation name for this input?
   - **a**: A hard limit of one image per account
   - **b**: Automatic refusal of every handwritten image
   - **c**: Errors and hallucination on poor-quality, turned or tiny images
   - **d**: Silent conversion of the image into audio

3. A product owner wants an assistant that books a customer's flights by operating the airline's website
   directly. Which design point matters most for safety?
   - **a**: Run it on the largest context window available
   - **b**: Run it in an isolated environment and confirm purchases with a person
   - **c**: Run it with the lowest effort setting
   - **d**: Run it with all stored passwords visible

4. Which request is the best fit for Claude without extra machinery?
   - **a**: Calculating payroll to the cent for 800 staff
   - **b**: Reporting today's closing price of a stock
   - **c**: Verifying a customer's identity from a photo
   - **d**: Drafting a polite reply to a customer's complaint

<details>
<summary>Answer key</summary>

1. **a**. Exact tallies belong in code (page 1). *b* is ruled out because fitting in the window says nothing about counting accuracy. *c* is ruled out because temperature does not give the model character-level or item-level exactness. *d* is ruled out because averaging two approximations is still an approximation.
2. **c**. The vision page says exactly that. *b* is ruled out because no handwriting refusal is stated. *a* is ruled out because the page gives a per-request count, not a per-account one. *d* is ruled out because no audio conversion is described.
3. **b**. Computer use needs an isolated machine, no exposed secrets and human confirmation of consequential steps (computer use section). *a* is ruled out because capacity does not reduce risk. *c* is ruled out because effort steers reasoning depth, not safety. *d* is ruled out because exposed secrets is the opposite of the advice.
4. **d**. Drafting language with review is the strong-fit row. *a* is ruled out because exact payroll is code's job. *c* is ruled out because naming people from images is something the vision page says Claude refuses, and identity checks are high stakes. *b* is ruled out because live prices are after the cut-off and need a tool or data feed.

</details>
