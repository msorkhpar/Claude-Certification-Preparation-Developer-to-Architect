# Sampling, knowledge and hallucination

**Level:** Foundations · **Module 1:** How a language model works, for engineers · **Page 2 of 2**
**Exams:** all (AS2, DV2, DV4)

**After this page you can** explain why the same prompt gives different answers, say what the model knows
and does not, and choose a mitigation for a confident wrong answer.

Checked against the Anthropic documentation on 2026-10-02 (glossary, models overview, reduce
hallucinations) and by running the example below in the course container (Python 3 and Node 24 on the
runner image, offline). The example is a toy model, not Claude.

## Why it matters

A prompt that gave a perfect answer in the playground gives a slightly different one in production, and a
colleague concludes "it is flaky". A summary cites a clause that does not exist. A bot insists the newest
release is the one it was trained on. These are not three bugs; they are three consequences of how the
next token is chosen, what the model was trained on, and what it does when it does not know. An engineer who
can name the mechanism can pick the right fix, and the exams ask for exactly that choice.

## The idea

### From scores to a choice: sampling

At each step the model produces a score for every token it could emit next. Those scores are turned into
probabilities, and the sampler picks one token. **Temperature** is the control on that step. The glossary:
"Higher temperatures lead to more creative and diverse outputs... Lower temperatures result in more
conservative and deterministic outputs that stick to the most probable phrasing and answers."

Source: Glossary, Claude API documentation.

The idea in one formula: divide the scores by the temperature before turning them into probabilities. A
temperature below 1 widens the gap between the likely token and the others; above 1 it narrows the gap.
Picking always the single most likely token is called greedy decoding.

The example below is a four-token toy with fixed scores. It has no relation to Claude's actual numbers; it
exists so you can see the effect, with a seeded generator so every run prints the same lines.

<!-- example: m1-sampler tabs: python,typescript -->
```python
EXAMPLE_PYTHON
```
```text
EXAMPLE_OUT_PY
```
```typescript
EXAMPLE_TS
```
```text
EXAMPLE_OUT_TS
```
<!-- /example -->

(Java and Kotlin readers: the example needs only the standard library of any language; the numbers above
are the same in every language, because the generator is a four-line recurrence.)

Read the output. At the lowest temperature almost all the probability sits on `blue`, and the ten draws are
nearly identical. At the highest, the three weaker tokens together gain a large share and the draws mix.
That is the whole trade: low temperature for extraction, classification and anything with one right answer;
higher for brainstorming and varied drafting.

### Why the same prompt gives different answers

Two sources:

1. **Sampling.** Any temperature above zero draws from a distribution, so two runs can diverge at the first
   token and then diverge further, because every later step conditions on the earlier choice.
2. **Infrastructure.** The glossary warns that "even with temperature set to 0, the results will not be fully
   deterministic and identical inputs may produce different outputs across API calls", on Anthropic's own
   service and on third-party cloud providers alike.

Consequences an engineer must accept: never judge a prompt from one run; compare prompts on a set of
inputs (module 42); never write a test that expects byte-identical model output; and never rely on
temperature zero as a guarantee. Which sampling parameters a given model accepts changes between
generations, so read the model's own page before relying on one (module 18).

### What the model knows, and when it stopped knowing

Knowledge comes from training data with a cut-off. The models overview separates two dates: the **reliable
knowledge cutoff**, "the date through which the model's knowledge is most extensive and reliable", and the
broader **training data cutoff**.

| Model | Reliable knowledge cutoff | Training data cutoff |
|---|---|---|
| Claude Fable 5.1, Opus 5.5, Sonnet 5.5 | June 2026 | June 2026 |
| Claude Haiku 4.5 | February 2025 | July 2025 |

Source: Models overview, read on 2026-10-02.

Three practical rules follow. The model has no clock: if the date matters, put it in the prompt. Anything
after the cutoff, and anything private (your tickets, your policies), is unknown to it unless you supply it
in the context or give it a tool that fetches it. And a model answering about "the latest version" of
something is answering about the latest version it saw.

### Steerability

The model is steered by what is in its context. Instructions, examples, a stated audience and a persona
all change the distribution over next tokens, which is why prompting works (module 6) and also why it
is only a request. A sentence in a prompt can make a behaviour much more likely; it cannot make it certain.
When a behaviour must hold every time, it belongs in code (a validator, a check, a hook), a rule the later
modules return to again and again.

### Hallucination

A hallucination is output that is fluent and confident but wrong or unsupported: an invented citation, a
function that does not exist, a statistic with no source. It follows from the mechanism: the model produces
a likely continuation, and a plausible-looking fact is a likely continuation whether or not it is true. The
glossary's description of the honest-model goal says an honest AI "will acknowledge its limitations and
uncertainties when appropriate", which is a goal of training and not a guarantee of any single answer.

The documentation lists techniques that reduce hallucinations, and says plainly that they "don't eliminate
them entirely":

- **Allow "I don't know".** Tell the model it may say so when the material is not enough.
- **Ground in quotes.** For long documents, ask for word-for-word quotes first, then the analysis based only
  on those quotes.
- **Cite and retract.** Ask for a supporting quote for each claim, and to drop any claim it cannot support.
- **Compare runs.** Run the same prompt several times; disagreement is a warning sign.
- **Restrict knowledge.** Tell the model to use only the provided documents, not its general knowledge.

Source: Reduce hallucinations, Claude API documentation.

The two lessons the exams keep returning to: **confidence is not evidence** (asking a model how sure it is
does not make a wrong answer right), and **the check belongs with something that can actually check**: the
source document, a database, a test. Module 5 builds the habit of checking; module 25 builds the code.

## Traps

1. **Fixing variation with "be consistent".** A prompt sentence does not remove sampling. If you need the
   same label for the same input, lower the randomness where the model allows it, constrain the output
   format, and validate in code.
2. **Trusting temperature zero as determinism.** The glossary says it is not fully deterministic.
3. **Asking the model about the present.** Questions about today's date, prices, versions or events after
   the cutoff are answered from stale or absent knowledge unless the context supplies the facts.

## Quiz

1. A classifier labels support tickets. In testing, the same ticket gets "billing" on one run and "account"
   on the next, even though the prompt never changes. Which action best addresses the root cause?
   - **a**: Add a final line asking the model to stay consistent
   - **b**: Constrain the label to a fixed list and validate it in code
   - **c**: Ask the model to explain its reasoning at length
   - **d**: Run once per ticket and trust whichever label returns

2. A model confidently states that an internal refund policy allows 45 days. The policy file, which the
   model never saw, says 30. Why did it answer so firmly?
   - **a**: Its reliable knowledge cutoff was set to a past year
   - **b**: Its temperature was set below the recommended level
   - **c**: The context window had overflowed with earlier text
   - **d**: It filled the gap with a likely-sounding number

<details>
<summary>Answer key</summary>

1. **b**. Variation comes from sampling and infrastructure (the sampling section), so a sentence cannot remove it; narrowing what the model may output and checking it in code can. *a* is ruled out because a prompt line is only a request, as the steerability section says. *c* is ruled out because a longer explanation adds more sampled text and does not fix the label. *d* is ruled out because trusting one run is the trap the page names: a single run says nothing about the spread.
2. **d**. The model has no access to private material unless it is placed in the context, and a plausible figure is a likely continuation (the hallucination section). *b* is ruled out because temperature changes how varied answers are, not whether unknown facts are known. *c* is ruled out because nothing in the scenario fills the window, and an overflow would produce an error or a stop reason, not a confident figure. *a* is ruled out because a cutoff is fixed by training and says nothing about a private file.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A product team wants an assistant that drafts varied marketing taglines and, separately, one that
   extracts invoice totals. They plan one fixed sampling setting for both. What is the sound adjustment?
   - **a**: Use lower randomness for extraction and higher for drafting
   - **b**: Keep a single moderate setting and add more prompt wording
   - **c**: Use the highest randomness for both, to avoid repetition
   - **d**: Use the lowest randomness for both, to avoid mistakes

2. A developer measures a prompt's cost with a word count, then ships. The bill is about a third higher
   than predicted after migrating to a newer Claude generation. Which step would have prevented the surprise?
   - **a**: Counting words with a stricter definition of a word
   - **b**: Lowering `max_tokens` until the estimate matched
   - **c**: Recounting tokens against the model that will serve production
   - **d**: Shortening each prompt by deleting its examples

3. An assistant is asked, mid-conversation, what day it is, and answers with a day from its training period.
   What should the application do?
   - **a**: Raise the temperature so the answer varies
   - **b**: Pass the current date in the system prompt
   - **c**: Wait for the next model release to fix it
   - **d**: Ask users to correct the model each time

4. A long contract is pasted into a request, and the model's summary cites a clause number that is not in
   the contract. Which prompt change most directly reduces this failure?
   - **a**: Ask the model to repeat the contract first
   - **b**: Ask the model to rate its own confidence at the end
   - **c**: Ask for a longer and more formal summary
   - **d**: Ask for word-for-word supporting quotes first

<details>
<summary>Answer key</summary>

1. **a**. Low randomness suits one-right-answer tasks and higher randomness suits varied drafting (the example and its reading). *b* is ruled out because added wording does not set the distribution. *c* is ruled out because the highest setting makes totals vary most. *d* is ruled out because the lowest setting removes the variety the tagline task wants, and the page says it still does not guarantee determinism.
2. **c**. Newer generations from Claude Opus 4.7 on use a tokenizer that yields roughly 30 percent more tokens for the same text, and the documentation says to recount against the model you plan to use. *a* is ruled out because words are not the billing unit. *b* is ruled out because `max_tokens` caps output, not input. *d* is ruled out because trimming examples changes the prompt, which hides the miscount instead of correcting it.
3. **b**. The model has no clock and the date must be supplied in the context (the knowledge section). *a* is ruled out because temperature does not add knowledge. *c* is ruled out because every release has a cutoff, so a later model still lacks today's date. *d* is ruled out because it moves a code-level fix onto users.
4. **d**. Grounding in word-for-word quotes is the documented technique for long documents. *b* is ruled out because the page says confidence is not evidence. *c* is ruled out because a more formal summary does not add grounding. *a* is ruled out because repeating the text proves nothing about the claims made later.

</details>
