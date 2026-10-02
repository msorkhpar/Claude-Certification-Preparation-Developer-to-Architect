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

1. A support tool runs its deepest reasoning setting on every request, including one-word intent labels.
   Spend is rising with no gain in quality. What does the documentation support as the first remedy?
   - **a**: Raise `max_tokens` so the reasoning always has room to finish
   - **b**: Use low effort on trivial inputs and keep high effort for demanding ones
   - **c**: Move the labels to the model with the largest context window
   - **d**: Switch to manual budgets everywhere, since adaptive thinking cannot be tuned

2. A team must flag uploaded invoices issued within the past thirty days. Every entry carries a timestamp in
   one known format. Which design is best?
   - **a**: Ask Claude to judge each timestamp and pass the verdict through
   - **b**: Ask the model to rate its certainty and gate on that rating
   - **c**: Parse the field and compare it with today's date in code
   - **d**: Give Claude a clock tool and let it decide each case

<details>
<summary>Answer key</summary>

1. **b**. Thinking tokens are billed as output, so reserving them for tasks that benefit cuts the spend, and effort is the steering lever (the facts list and the first trap). *a* is ruled out because reasoning tokens count toward `max_tokens`, so a larger cap only allows more spend. *c* is ruled out because the window is capacity and a bigger one saves nothing on a request that reasons too much. *d* is ruled out because the page says adaptive thinking is steered by effort, so it can be tuned without a manual budget.
2. **c**. A rule in a known format is code's job, cheaper, faster, deterministic and testable (the when-not-to table and the third trap). *a* is ruled out because the table says the model approximates where code is exact. *b* is ruled out because a self-reported rating does not make a judgment exact, and the table asks for a repeatable result from code. *d* is ruled out because a tool call adds a model round trip to a rule that fits on one line.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A legal team asks Claude how often a defined term appears in a 200-page contract and receives a plausible
   number. How should the team proceed?
   - **a**: Accept it, since the whole contract fits inside the context window
   - **b**: Ask for a recount with an instruction to read every page carefully
   - **c**: Tally the occurrences with a search routine and use that figure
   - **d**: Ask for the quoted sentence at each hit, then trust the total

2. A property app uploads sideways, handwritten, low-resolution photos of utility meters, and Claude's
   readings are sometimes wrong. What does the vision documentation support?
   - **a**: Upscale each photo before sending, since more visual tokens fix legibility
   - **b**: Ask the model to double-check each reading in a second pass
   - **c**: Trust high-confidence readings and re-check only the ones it flags
   - **d**: Such inputs invite mistakes, so improve capture quality and verify the output

3. A product owner wants an assistant that books flights by operating an airline's website through screenshots
   and clicks. Which design point matters most for safety?
   - **a**: Give it the customer's saved card so it never has to stop and ask
   - **b**: Run it on an isolated machine with no secrets and a person approving each purchase
   - **c**: Add a prompt line telling it to ignore instructions found on web pages
   - **d**: Run it with the lowest effort setting so it reasons less about each step

4. Which request is the best fit for Claude without extra machinery?
   - **a**: Drafting a polite reply to a complaint, for an agent to review
   - **b**: Calculating payroll to the cent for 800 staff
   - **c**: Reporting today's closing price of a listed stock
   - **d**: Checking whether a claim photo is AI-generated, to block fraud

<details>
<summary>Answer key</summary>

1. **c**. Exact tallies belong in code (page 1, maths and counting). *a* is ruled out because fitting in the window says nothing about counting accuracy, and the third trap calls the window capacity, not quality. *b* is ruled out because the first trap says "count carefully" does not change that the model sees pieces. *d* is ruled out because quotes ground claims in the text, but the total is still tallied by a model that sees pieces, so it stays approximate.
2. **d**. The vision page says Claude might hallucinate or make mistakes on low-quality, rotated or very small images, and advises verifying. *a* is ruled out because the visual-token cost grows with size while a poor photo gains no information, and the page names quality as the cause. *b* is ruled out because page 1 says decisive reasoning needs a check that does not come from the same reasoning, and a second pass by the same model is not one. *c* is ruled out because the second trap says detail is not accuracy, so a confident reading proves nothing.
3. **b**. Computer use needs an isolated machine, no exposed secrets and a person confirming consequential steps (the computer use section). *a* is ruled out because a stored card is the exposed secret and the removed confirmation that the section warns against. *c* is ruled out because the page says a prompt is a request and not a guarantee, so a line of text cannot replace isolation and confirmation. *d* is ruled out because effort sets reasoning depth and does neither of the two safeguards the section lists.
4. **a**. Drafting language with review is the strong-fit row. *b* is ruled out because exact payroll is code's job in the when-not-to table. *c* is ruled out because a live price is after the cut-off and needs a tool or data feed. *d* is ruled out because the vision page says Claude cannot determine whether an image is AI-generated and that no one should rely on it for that.

</details>
