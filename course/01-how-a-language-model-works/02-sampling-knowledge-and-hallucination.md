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
"""A toy next-token sampler. It is not Claude: it only shows what temperature does."""
import math

TOKENS = ["blue", " clear", " falling", "green"]
LOGITS = [4.0, 2.5, 1.0, -1.0]


def softmax(logits, temperature):
    """Turn scores into probabilities. Lower temperature sharpens, higher flattens."""
    scaled = [x / temperature for x in logits]
    top = max(scaled)
    exps = [math.exp(x - top) for x in scaled]
    total = sum(exps)
    return [e / total for e in exps]


class Lcg:
    """A tiny seeded random generator, the same in every language of this course."""

    def __init__(self, seed):
        self.state = seed % 2**32

    def next(self):
        self.state = (self.state * 1664525 + 1013904223) % 2**32
        return self.state / 2**32


def sample(probs, rng):
    u = rng.next()
    acc = 0.0
    for i, p in enumerate(probs):
        acc += p
        if u < acc:
            return i
    return len(probs) - 1


def greedy(probs):
    return max(range(len(probs)), key=lambda i: probs[i])


def main():
    for t in (0.5, 1.0, 2.0):
        probs = softmax(LOGITS, t)
        print(f"T={t}: " + "  ".join(f"{tok.strip()}={p:.3f}" for tok, p in zip(TOKENS, probs)))
    print("greedy:", TOKENS[greedy(softmax(LOGITS, 1.0))])
    for t in (0.2, 1.0, 2.0):
        probs = softmax(LOGITS, t)
        rng = Lcg(7)
        picks = [TOKENS[sample(probs, rng)].strip() for _ in range(10)]
        print(f"T={t} ten draws:", " ".join(picks))


if __name__ == "__main__":
    main()
```
```text
T=0.5: blue=0.950  clear=0.047  falling=0.002  green=0.000
T=1.0: blue=0.781  clear=0.174  falling=0.039  green=0.005
T=2.0: blue=0.563  clear=0.266  falling=0.126  green=0.046
greedy: blue
T=0.2 ten draws: blue blue blue blue blue blue blue blue blue blue
T=1.0 ten draws: blue clear blue clear blue clear blue blue blue falling
T=2.0 ten draws: blue falling clear falling blue falling blue blue blue green
```
```typescript
// A toy next-token sampler. It is not Claude: it only shows what temperature does.
export const TOKENS = ["blue", " clear", " falling", "green"];
export const LOGITS = [4.0, 2.5, 1.0, -1.0];

export function softmax(logits: number[], temperature: number): number[] {
  const scaled = logits.map((x) => x / temperature);
  const top = Math.max(...scaled);
  const exps = scaled.map((x) => Math.exp(x - top));
  const total = exps.reduce((a, b) => a + b, 0);
  return exps.map((e) => e / total);
}

// A tiny seeded random generator, the same in every language of this course.
export class Lcg {
  state: number;
  constructor(seed: number) {
    this.state = seed >>> 0;
  }
  next(): number {
    this.state = (Math.imul(this.state, 1664525) + 1013904223) >>> 0;
    return this.state / 2 ** 32;
  }
}

export function sample(probs: number[], rng: Lcg): number {
  const u = rng.next();
  let acc = 0;
  for (let i = 0; i < probs.length; i++) {
    acc += probs[i];
    if (u < acc) return i;
  }
  return probs.length - 1;
}

export function greedy(probs: number[]): number {
  return probs.indexOf(Math.max(...probs));
}

export function main(): void {
  for (const t of [0.5, 1.0, 2.0]) {
    const probs = softmax(LOGITS, t);
    const cells = TOKENS.map((tok, i) => `${tok.trim()}=${probs[i].toFixed(3)}`);
    console.log(`T=${t.toFixed(1)}: ` + cells.join("  "));
  }
  console.log("greedy:", TOKENS[greedy(softmax(LOGITS, 1.0))]);
  for (const t of [0.2, 1.0, 2.0]) {
    const probs = softmax(LOGITS, t);
    const rng = new Lcg(7);
    const picks = Array.from({ length: 10 }, () => TOKENS[sample(probs, rng)].trim());
    console.log(`T=${t.toFixed(1)} ten draws:`, picks.join(" "));
  }
}

if (process.argv[1] && import.meta.url.endsWith(process.argv[1].split("/").pop()!)) main();
```
```text
T=0.5: blue=0.950  clear=0.047  falling=0.002  green=0.000
T=1.0: blue=0.781  clear=0.174  falling=0.039  green=0.005
T=2.0: blue=0.563  clear=0.266  falling=0.126  green=0.046
greedy: blue
T=0.2 ten draws: blue blue blue blue blue blue blue blue blue blue
T=1.0 ten draws: blue clear blue clear blue clear blue blue blue falling
T=2.0 ten draws: blue falling clear falling blue falling blue blue blue green
```
<!-- /example -->

(Java and Kotlin readers: the example needs only the standard library of any language; the generator is a
four-line recurrence, so a port prints the same numbers.)

Read the output. At T=0.5 the probability of `blue` is 0.950, and at T=0.2 all ten draws are `blue`. At
T=2.0 `blue` falls to 0.563, the three weaker tokens together hold the rest, and the draws mix `falling`,
`clear` and `green` in. The program prints the same lines in Python and in TypeScript.
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

1. A ticket router asks Claude to put each support ticket into one category. In testing, the same ticket
   lands in "billing" on one run and "account" on the next, though the prompt never changes. Which change
   best addresses the cause?
   - **a**: Restrict the output to a fixed list and check the result in code
   - **b**: Append a line asking for the same category every time, however the ticket reads
   - **c**: Set the temperature to zero and treat the output as fixed
   - **d**: Compare three runs and keep whichever category wins the vote

2. A refund bot is not given the company's policy file. It states firmly that refunds are allowed for
   45 days, while the real policy says 30. What best explains the firm wrong answer?
   - **a**: Its reliable knowledge cutoff predates the policy, so the answer is stale
   - **b**: It produced a typical-sounding number because nothing supplied the true one
   - **c**: Its sampling randomness was too low, which locks in mistaken answers
   - **d**: Earlier text overflowed the window and pushed the policy out of view

<details>
<summary>Answer key</summary>

1. **a**. Variation comes from sampling and infrastructure (the sampling section), so a sentence cannot remove it; narrowing what the model may output and checking it in code can. *b* is ruled out because a prompt line is only a request, as the steerability section says. *c* is ruled out because the glossary says that even at temperature zero results are not fully deterministic. *d* is ruled out because comparing runs, as the hallucination list describes it, exposes disagreement as a warning sign and does not remove it, and it multiplies the cost.
2. **b**. The policy is private material, unknown to the model unless it is placed in the context, and a plausible figure is a likely continuation (the hallucination section). *a* is ruled out because the knowledge section says anything private is unknown whatever the cutoff, so a cutoff date does not decide this. *c* is ruled out because temperature changes how varied answers are, not whether a fact is known. *d* is ruled out because the scenario never supplied the file, and the limit section says an overflow ends in an error or a stop reason, not a confident figure.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A product team plans one fixed sampling setting for two assistants: one invents campaign slogans and
   one reads amounts off bills. Which adjustment is sound?
   - **a**: Split the settings: conservative for the exact job, adventurous for the creative one
   - **b**: Keep one middle setting and strengthen the prompt wording for both jobs
   - **c**: Use zero for both, since zero makes every output fully deterministic
   - **d**: Use the highest setting for both and filter the extra variety out in code afterwards

2. A developer sizes a prompt by counting its words, then ships. After the team migrates to a newer Claude
   generation, the bill is about a third higher than predicted. Which earlier step would have prevented
   the surprise?
   - **a**: Applying a stricter definition of a word before estimating the size
   - **b**: Requesting an estimate from the token endpoint for the production model
   - **c**: Setting `max_tokens` low enough that the estimate matched the bill
   - **d**: Deleting the few-shot examples from every prompt before estimating

3. A scheduling assistant is asked what day it is mid-conversation and names a day from its training
   period. Which fix sits at the right layer?
   - **a**: Switch to the newest model, whose cutoff is closest to today
   - **b**: Add a prompt line telling it never to guess the current day
   - **c**: Inject today's date into the instructions on every call
   - **d**: Ask users to restate the date at the start of each chat

4. A long contract is pasted into a request. The summary Claude returns cites a clause number that does
   not appear in the contract. Which prompt change most directly targets this failure?
   - **a**: Ask for a longer, more detailed summary so every clause is covered
   - **b**: Ask Claude to state its confidence level beside each citation
   - **c**: Lower the temperature to zero so clause numbers cannot vary
   - **d**: Have it pull exact quotes first and summarize only from those

<details>
<summary>Answer key</summary>

1. **a**. Low randomness suits one-right-answer tasks and higher randomness suits varied drafting (the example and its reading). *b* is ruled out because added wording is only a request and does not set the distribution (the steerability section). *c* is ruled out because the glossary says temperature zero is not fully deterministic, and it removes the variety the slogans want. *d* is ruled out because the example's T=2.0 line shows the likely token losing probability, so the amounts would vary.
2. **b**. The token counting endpoint returns an estimate against the model you name, and the same text yields about 30 percent more tokens from Opus 4.7 on. *a* is ruled out because words are not the unit that is counted or billed. *c* is ruled out because `max_tokens` caps output, not input. *d* is ruled out because the miscount comes from the same text producing more tokens, so trimming the prompt hides the error and does not correct it.
3. **c**. The model has no clock, so the date must be supplied in the context (the knowledge section). *a* is ruled out because every model has a cutoff, so a later one still lacks today's date. *b* is ruled out because a prompt line cannot supply a fact the model lacks, and the steerability section calls such a line a request. *d* is ruled out because it moves a code-level fix onto users.
4. **d**. Grounding in word-for-word quotes is the documented technique for long documents. *a* is ruled out because a longer output adds more room for unsupported claims and no grounding. *b* is ruled out because the page says confidence is not evidence. *c* is ruled out because a fixed invented number is still invented, and temperature zero is not fully deterministic.

</details>
