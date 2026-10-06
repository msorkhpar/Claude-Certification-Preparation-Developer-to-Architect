# The prompt as a specification

**Level:** Foundations · **Module 6:** Prompting fundamentals · **Page 3 of 4**
**Exams:** DV2, DV4, AS1, A4 (A4.1)

**After this page you can** write a prompt as a specification with checkable requirements, split a complex
request into stages, and pick a prompting strategy for analysis, research, drafting and brainstorming.

Checked against the Associate exam guide (domain 1, version 1.0, July 2026) and the Claude API
documentation on 2026-10-02 (prompt engineering overview, which says to define success criteria and test
against them first, and the models overview). Replies shown are hand-scripted and labelled illustrative.

## Why it matters

A prompt that "seems to work" is a prompt nobody has tested. Teams that treat a prompt like a
specification, with stated requirements and checks, can change it, hand it over, compare versions and trust
it. The Associate exam names the skill ("apply task decomposition techniques to structure complex
requests", "adapt prompting strategies based on task type"), and the Developer exam folds it into
prompt versioning and evaluation.

## The idea

### Start with success criteria

The documentation's overview begins with three preconditions: "a clear definition of the success criteria
for your use case", "some ways to empirically test against those criteria" and "a first draft prompt".
Without the first two, you cannot tell whether an edit helped. So before writing, write down:

- **What counts as a good answer**, as statements you can check: "every figure appears in the source",
  "at most 120 words", "one of four labels".
- **What counts as a bad one:** invented figures, wrong format, a missing section.
- **A few real inputs**, including awkward ones, to run each version against.

### A specification has parts

Treat the prompt like a short contract. The practice at the end of this module is a builder for exactly this
shape, so learn the parts:

| Part | Question it answers | Example |
|---|---|---|
| **Role** (optional) | Who is doing the work? | "You are a support analyst." |
| **Context** | Why, for whom, with what background? | "Tickets are routed by your label." |
| **Documents** | What material to use? | The ticket, the policy |
| **Examples** (optional) | What does a good answer look like? | Three labelled tickets |
| **Constraints** | What must hold? | "Use only the policy. One word." |
| **Output format** | What shape does the answer take? | "Label only." |
| **Task** | What exactly is to be done now? | "Classify the ticket." |

Order: material first, the task last, as page 1 said. Every part is something you can point at in a review,
and every part is something a test can check.

<!-- illustrative -->
A specification, and the checks that go with it:

```text
Role:         support analyst for a subscription software company
Documents:    the refund policy; the customer's message
Constraints:  use only the policy; if the policy does not decide the case, say "needs a person"
Output:       JSON {"decision": "refund" | "no_refund" | "needs a person", "policy_clause": string}
Task:         decide whether the customer is entitled to a refund

Checks:       (1) decision is one of the three values   (2) policy_clause appears in the policy
              (3) the "needs a person" case returns that value on the ambiguous test message
```
<!-- /illustrative -->

### Stages for a complex request

A complex request asks for several different things at once: read this, decide that, write something. Models
(like people) do better when the request is **split into stages** whose outputs you can look at:

1. **Gather:** extract the relevant facts or quotes from the material.
2. **Reason:** analyse or decide, using only the gathered facts.
3. **Write:** produce the deliverable in the requested form.
4. **Check:** compare against the criteria, or ask for a review pass.

You can run the stages as separate requests (chaining, covered in module 24), or as numbered steps within
one prompt for modest tasks. The benefits are the same: each stage is smaller and clearer, a failure shows
up at the stage where it happened, and you can run a code check between stages. The documentation's
hallucination technique is a two-stage prompt, and a good model of the pattern: for long documents, "ask
Claude to extract word-for-word quotes first before performing its task", and in the page's own example
the second step says "Only base your analysis on the extracted quotes."

### A strategy per task type

The Associate guide lists four task types by name. Their prompts differ:

| Task type | What to emphasise | Typical failure to guard against |
|---|---|---|
| **Analysis** | The material, the question, the criteria; "show the evidence for each conclusion" | Conclusions without evidence |
| **Research** | Sources named, claims tied to sources, "say what you could not find" | Invented or unsupported claims |
| **Drafting** | Audience, tone, length, an example of the target style | Generic text in the wrong voice |
| **Brainstorming** | Breadth first, many options, no early judgment; then a second pass to evaluate | Few, similar ideas; or premature narrowing |

Brainstorming also shows why the same prompt is not right for every task: the approach that suits
extraction (precision, low variety) works against ideation (module 1, sampling).

## Traps

1. **Editing without a test set.** Changing a prompt and re-reading one answer is anecdote. Keep a handful of
   fixed inputs, including hard ones.
2. **One prompt for all stages.** A giant do-everything prompt makes failures hard to locate. Split at the
   point where an intermediate result could be checked.
3. **The same style for every task.** An instruction that helps a research prompt ("cite sources") can be
   noise in a brainstorm. Choose by task type.

## Quiz

1. A team keeps editing a prompt and judging each edit by reading one reply. Over a month quality seems to
   drift and nobody can say which edit caused it. What is the best remedy?
   - **a**: Revert to the version in place when quality last seemed good, and stop editing
   - **b**: Write the criteria down and score every version on a fixed set of inputs
   - **c**: Keep each edit that fixes the latest bad reply and drop the rest
   - **d**: Ask three colleagues to read one reply per edit and vote on it

2. A request asks Claude to read a fifty-page report, judge whether a supplier is at risk and draft a board
   email. The email's claims are sometimes unsupported. Which restructuring helps most?
   - **a**: Resend the same request three times and merge the three emails
   - **b**: Request the email first and the assessment afterwards, to compare them
   - **c**: Add a closing line: "Be careful; do not include unsupported claims"
   - **d**: Split the work so the assessment uses only quotes pulled out first

<details>
<summary>Answer key</summary>

1. **b**. Success criteria and test inputs are the preconditions the overview names, and without them no edit can be compared (the success criteria section). *a* is ruled out because "Without the first two, you cannot tell whether an edit helped", so nobody can know which version was the good one. *c* is ruled out because judging an edit by whether it fixed the latest reply is still a single case, and "Changing a prompt and re-reading one answer is anecdote". *d* is ruled out because more readers of the same single reply still test each edit on one input, while the page says to "Keep a handful of fixed inputs".
2. **d**. Staging with an evidence step first is the pattern the page models on the documentation's quote-first technique, with a code check possible between stages. *a* is ruled out because the benefit of stages is that "a failure shows up at the stage where it happened", which repetition does not give. *b* is ruled out because the stages start with "extract the relevant facts or quotes from the material", so the email cannot come first. *c* is ruled out because "A giant do-everything prompt makes failures hard to locate", and a closing line is a request, not a stage.

</details>
