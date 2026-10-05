# Sampling, knowledge and hallucination

**Level:** Foundations · **Module 1:** How a language model works, for engineers · **Page 2 of 2**
**Exams:** all (AS2, DV2, DV4)

**After this page you can** explain why the same prompt gives different answers, say what the model knows
and does not, and choose a mitigation for a confident wrong answer.

Checked against the Anthropic documentation on 2026-10-02 (glossary, models overview, reduce
hallucinations, the Messages API reference, the Claude Opus 5.5 migration guide and What's new in Claude
Fable 5.1) and by running the example below in the course container (Python 3 and Node 24 on the runner
image, offline). The example is a toy model, not Claude.

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

<!-- example: m1-sampler tabs: python,typescript,java,kotlin -->
```python
"""A toy next-token sampler. It is not Claude: it only shows what temperature does."""
import logging
import math

log = logging.getLogger(__name__)

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
import { logger } from "./logger.ts";
const log = logger("sampler");

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
```java
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** A toy next-token sampler. It is not Claude: it only shows what temperature does. */
public final class Sampler {
    private static final System.Logger LOG = System.getLogger(Sampler.class.getName());
    static final String[] TOKENS = {"blue", " clear", " falling", "green"};
    static final double[] LOGITS = {4.0, 2.5, 1.0, -1.0};

    /** Turn scores into probabilities. Lower temperature sharpens, higher flattens. */
    static double[] softmax(double[] logits, double temperature) {
        double[] scaled = new double[logits.length];
        double top = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < logits.length; i++) {
            scaled[i] = logits[i] / temperature;
            top = Math.max(top, scaled[i]);
        }
        double total = 0;
        double[] exps = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = Math.exp(scaled[i] - top);
            total += exps[i];
        }
        for (int i = 0; i < exps.length; i++) exps[i] /= total;
        return exps;
    }

    /** A tiny seeded random generator, the same in every language of this course. */
    static final class Lcg {
        private long state;

        Lcg(long seed) {
            state = Math.floorMod(seed, 1L << 32);
        }

        double next() {
            state = (state * 1664525L + 1013904223L) & 0xFFFFFFFFL;
            return state / 4294967296.0;
        }
    }

    static int sample(double[] probs, Lcg rng) {
        double u = rng.next();
        double acc = 0.0;
        for (int i = 0; i < probs.length; i++) {
            acc += probs[i];
            if (u < acc) return i;
        }
        return probs.length - 1;
    }

    static int greedy(double[] probs) {
        int best = 0;
        for (int i = 1; i < probs.length; i++) if (probs[i] > probs[best]) best = i;
        return best;
    }

    public static void main(String[] args) {
        for (double t : new double[] {0.5, 1.0, 2.0}) {
            double[] probs = softmax(LOGITS, t);
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < probs.length; i++) cells.add(String.format(Locale.ROOT, "%s=%.3f", TOKENS[i].strip(), probs[i]));
            System.out.println("T=" + t + ": " + String.join("  ", cells));
        }
        System.out.println("greedy: " + TOKENS[greedy(softmax(LOGITS, 1.0))]);
        for (double t : new double[] {0.2, 1.0, 2.0}) {
            double[] probs = softmax(LOGITS, t);
            Lcg rng = new Lcg(7);
            List<String> picks = new ArrayList<>();
            for (int n = 0; n < 10; n++) picks.add(TOKENS[sample(probs, rng)].strip());
            System.out.println("T=" + t + " ten draws: " + picks.stream().collect(Collectors.joining(" ")));
        }
    }
}
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
```kotlin
import kotlin.math.exp

private val log = System.getLogger("sampler")

/** A toy next-token sampler. It is not Claude: it only shows what temperature does. */
val TOKENS = listOf("blue", " clear", " falling", "green")
val LOGITS = listOf(4.0, 2.5, 1.0, -1.0)

/** Turn scores into probabilities. Lower temperature sharpens, higher flattens. */
fun softmax(logits: List<Double>, temperature: Double): List<Double> {
    val scaled = logits.map { it / temperature }
    val top = scaled.max()
    val exps = scaled.map { exp(it - top) }
    val total = exps.sum()
    return exps.map { it / total }
}

/** A tiny seeded random generator, the same in every language of this course. */
class Lcg(seed: Long) {
    private var state = seed.mod(1L shl 32)

    fun next(): Double {
        state = (state * 1664525L + 1013904223L) and 0xFFFFFFFFL
        return state / 4294967296.0
    }
}

fun sample(probs: List<Double>, rng: Lcg): Int {
    val u = rng.next()
    var acc = 0.0
    for ((i, p) in probs.withIndex()) {
        acc += p
        if (u < acc) return i
    }
    return probs.lastIndex
}

fun greedy(probs: List<Double>): Int = probs.indices.maxBy { probs[it] }

fun main() {
    for (t in listOf(0.5, 1.0, 2.0)) {
        val probs = softmax(LOGITS, t)
        println("T=$t: " + TOKENS.indices.joinToString("  ") { "%s=%.3f".format(TOKENS[it].trim(), probs[it]) })
    }
    println("greedy: " + TOKENS[greedy(softmax(LOGITS, 1.0))])
    for (t in listOf(0.2, 1.0, 2.0)) {
        val probs = softmax(LOGITS, t)
        val rng = Lcg(7)
        println("T=$t ten draws: " + List(10) { TOKENS[sample(probs, rng)].trim() }.joinToString(" "))
    }
}
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

(The Java and Kotlin tabs print the same numbers: the example needs only the standard library of any language, and
the generator is a four-line recurrence.)

Read the output. At T=0.5 the probability of `blue` is 0.950, and at T=0.2 all ten draws are `blue`. At
T=2.0 `blue` falls to 0.563, the three weaker tokens together hold the rest, and the draws mix `falling`,
`clear` and `green` in. The program prints the same lines in Python and in TypeScript.
That is the whole trade: low temperature for extraction, classification and anything with one right answer;
higher for brainstorming and varied drafting.

### What the current models let you set

The toy sampler has a temperature knob, and older Claude models exposed `temperature`, `top_p` and `top_k`
on the Messages API. The models this course uses do not accept them. The Messages API reference
(page title "Messages") marks `temperature` as deprecated: "Models released after Claude Opus 4.6 do not
support setting temperature. A value of 1.0 will be accepted for backwards compatibility, all other values
will be rejected". That covers Claude Fable 5.1, Opus 5.5 and Sonnet 5.5. The migration guide for Opus 5.5
(page title "Migrating to Claude Opus 5.5") names all three parameters: "Omit `temperature`, `top_p`, and
`top_k`, or leave them at their defaults: any other value is rejected. Use prompting to guide the model's
behavior." It adds that on Opus 4.7 and later a non-default value "returns a 400 error". The page "What's new
in Claude Fable 5.1" lists "Non-default `temperature`, `top_p`, or `top_k` values return a 400 error" among the
behaviours unchanged from Claude Fable 5. Claude Haiku 4.5 is older than that cut and is not covered by these
sentences; this course does not rely on a sampling setting for it either. Read on 2026-10-02.

So the lever a Claude engineer pulls for steadiness is not a number in the request. It is a narrower output
(a fixed list of labels, a schema), a check in code, and a prompt that leaves less to chance. The same
migration guide notes that a zero "never guaranteed identical outputs on prior models". The toy above still
shows the mechanism that makes output vary; it is not a setting you can reach on these models.

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
generations (see the section above), so read the model's own page before relying on one (module 18).

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
- **Compare runs.** Run the same prompt several times; disagreement is a warning sign. Comparing detects the
  variation; it does not remove it, and every extra run is another paid request.
- **Restrict knowledge.** Tell the model to use only the provided documents, not its general knowledge.

Source: Reduce hallucinations, Claude API documentation.

The two lessons the exams keep returning to: **confidence is not evidence** (asking a model how sure it is
does not make a wrong answer right), and **the check belongs with something that can actually check**: the
source document, a database, a test. Module 5 builds the habit of checking; module 25 builds the code.

## Traps

1. **Fixing variation with "be consistent".** A prompt sentence does not remove sampling. If you need the
   same label for the same input, constrain the output to a fixed set, and validate in code; lower the
   randomness only where the model allows it, which the current models do not.
2. **Trusting temperature zero as determinism.** The glossary says it is not fully deterministic.
3. **Asking the model about the present.** Questions about today's date, prices, versions or events after
   the cutoff are answered from stale or absent knowledge unless the context supplies the facts.

## Quiz

1. A ticket router asks Claude to put each support ticket into one category. In testing, the same ticket lands
   in "billing" on one run and "account" on the next, though the prompt never changes. Which change best
   addresses the cause?
   - **a**: Append a line asking for the same category every time, however the ticket reads
   - **b**: Constrain replies to an allowed set of labels and validate each one in code
   - **c**: Set the temperature to zero and treat the output as fixed
   - **d**: Compare three runs and keep whichever category wins the vote

2. A refund bot is not given the company's policy file. It states firmly that refunds are allowed for 45
   days, while the real policy says 30. What best explains the firm wrong answer?
   - **a**: Its reliable knowledge cutoff predates the policy, so the answer is stale
   - **b**: Earlier text overflowed the window and pushed the policy out of view
   - **c**: It ignored a policy instruction because the system prompt was worded too weakly
   - **d**: It produced a typical-sounding number because nothing supplied the true one

<details>
<summary>Answer key</summary>

1. **b**. Variation comes from sampling and infrastructure (the sampling section), so a sentence cannot remove it; narrowing what the model may output and checking it in code can. *a* is ruled out because a prompt line "can make a behaviour much more likely; it cannot make it certain" (the steerability section). *c* is ruled out because the glossary warns that "even with temperature set to 0, the results will not be fully deterministic", and the current models reject any non-default value anyway (the sampling section). *d* is ruled out because comparing runs only reveals the variation: "Comparing detects the variation; it does not remove it" (the hallucination list).
2. **d**. The policy is private material, unknown to the model unless it is placed in the context, and a plausible figure is a likely continuation (the hallucination section). *a* is ruled out because the knowledge section says "anything private (your tickets, your policies), is unknown to it" whatever the cutoff, so a cutoff date does not decide this. *c* is ruled out because the scenario never supplied a policy file, and private material is unknown to the model "unless you supply it in the context", so there was no instruction to ignore. *b* is ruled out because the scenario never supplied the file, and private material is unknown to the model "unless you supply it in the context", so nothing was pushed out of view.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team runs two assistants on Claude Sonnet 5.5: one drafts campaign slogans and the other reads amounts
   off bills. The lead wants the slogans varied and the amounts exact, and plans to tune each assistant's
   randomness separately through request settings. What actually happens?
   - **a**: Anything but the defaults comes back as a 400 error, so correctness has to be enforced by checks in code
   - **b**: Each assistant takes its own value, so slogan variety and amount accuracy can be tuned
     independently
   - **c**: Zero is accepted for the amounts assistant and then makes every output come out identical
   - **d**: Only top_p stays adjustable, so slogans can still be varied through that one setting

2. A team plans to cut wrong answers by running every prompt three times and comparing the replies. It
   budgets the cost from what a token-counting tool reported for Claude Haiku 4.5. After the job moves to
   Claude Opus 5.5 the bill is about a third above budget, although the run count is as planned. Which
   earlier step would have given a sound budget?
   - **a**: Reusing the Haiku numbers, since Claude generations share one tokenizer
   - **b**: Dropping the comparison runs, since every extra run is another paid request
   - **c**: Querying the endpoint again with the destination model named, before the change
   - **d**: Setting max_tokens low, so output cost cannot exceed the plan

3. A scheduling assistant is asked to book "next Friday" mid-conversation and proposes a day from its
   training period. Which fix sits at the right layer?
   - **a**: Switch to the newest model, whose cutoff is closest to today
   - **b**: Have the application put the current calendar date in every request
   - **c**: Add a prompt line telling it never to guess the current day
   - **d**: Resend older conversations with each call so they hint at today's date

4. A long contract is pasted into a request. The overview Claude returns cites a clause number that does not
   appear in the contract. Which prompt change most directly targets this failure?
   - **a**: Ask for a longer, more detailed summary so every clause is covered
   - **b**: Ask Claude to state its confidence level beside each citation
   - **c**: Tell it to be an honest assistant that cites only clauses that really exist in the text
   - **d**: Have it first copy the relevant passages out exactly, then write only from them

<details>
<summary>Answer key</summary>

1. **a**. Claude Sonnet 5.5, like Fable 5.1 and Opus 5.5, rejects any non-default temperature, top_p or top_k with an error, so exactness for the bills comes from restricting and validating the output, and the slogans need no extra setting (the sampling section). *b* is ruled out because the Messages reference says "all other values will be rejected", so no assistant can take a value of its own. *c* is ruled out because even where a zero is accepted, "even with temperature set to 0, the results will not be fully deterministic", and these models reject it. *d* is ruled out because the migration guide names all three parameters: "Omit temperature, top_p, and top_k, or leave them at their defaults: any other value is rejected".
2. **c**. The token counting endpoint returns an estimate against the model you name, and the same text yields about 30 percent more tokens from Opus 4.7 on, so a count made for the model the job moves to would have shown the rise (page 1, tokens are not words). *b* is ruled out because the run count was already budgeted and the bill still rose: "the same text produces about 30 percent more tokens than on earlier models", so the cost moved with the model and not with the runs. *a* is ruled out because "Tokenizers change between model generations", so the old numbers do not carry over. *d* is ruled out because "max_tokens is a cap on output, not on context", and the input side is where the new tokenizer adds tokens.
3. **b**. The model has no clock, so the date has to be supplied in the context on every call (the knowledge section). *a* is ruled out because "Knowledge comes from training data with a cut-off", so even the newest model stops before today. *c* is ruled out because a prompt shapes the distribution and that is "why it is only a request", so a line cannot supply a fact the model lacks. *d* is ruled out because "The model has no clock", so replaying older conversations gives it nothing to read the present from.
4. **d**. Grounding the work in exact passages first means every claim can be traced to text that exists, which is the documented technique for long documents (the hallucination list). *a* is ruled out because "a plausible-looking fact is a likely continuation whether or not it is true", so more output only gives an invented clause more chances to appear. *b* is ruled out because the page says "confidence is not evidence". *c* is ruled out because honesty is "a goal of training and not a guarantee of any single answer", so asking for it does not ground the citations.

</details>
