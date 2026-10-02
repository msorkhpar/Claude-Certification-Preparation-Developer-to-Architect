# Clear, direct instructions, context and roles

**Level:** Foundations · **Module 6:** Prompting fundamentals · **Page 1 of 4**
**Exams:** DV2, DV4, AS1, A4 (A4.1)

**After this page you can** rewrite a vague request into a clear and direct one, add the context that
explains the purpose, and use a role without mistaking it for a guarantee.

Checked against the Claude API documentation on 2026-10-02: the prompt engineering overview and the
prompting best practices index (which names the techniques below), the reduce-hallucinations page and the
vision page. Where this page gives a working rule of its own, it says so. All model replies on this page
are hand-scripted and labelled illustrative; no live call was made.

## Why it matters

The prompt is the interface to the model. The documentation's overview says the techniques "from clarity
and examples to XML structuring, role prompting, thinking, and prompt chaining" are collected in the
prompting best practices page, and that before you start you should have success criteria, a way to test
them, and a first draft. Every later module (structured output, tools, agents, evaluation) is built on
prompts that were written clearly first. The Associate exam grades the same skill as its first domain.

## The idea

### Say what you want, plainly

A model has only the text you send. It cannot see your screen, your last meeting or what you meant. The
first technique, and the one that fixes the most, is to be **clear and direct**:

- Name the **task** with a verb: classify, summarise, extract, draft, compare, critique, translate.
- Name the **deliverable**: its form (a table, three bullets, an email, JSON), its length and its reader.
- State what **good** looks like and what to avoid, as constraints you could check afterwards.
- Put the instruction as an instruction. "It would be great if the answer were short" is weaker than
  "Answer in at most three sentences."

A useful test, a working rule of this course: hand the prompt to a capable colleague who has never heard of
your project. If they would need to ask you a question before starting, the model needs the answer in the
prompt.

<!-- illustrative -->
The replies are hand-written to show the pattern, not recorded.

```text
Vague:   "Look at these reviews."
         -> a generic paragraph about "overall sentiment"

Direct:  "Classify each review below as positive, negative or mixed. Output one line per review:
          the review number, then the label. Do not explain."
         -> 1 positive
            2 mixed
            3 negative
```
<!-- /illustrative -->

### Give the purpose and the context

An instruction with a reason is followed better than a bare rule, because the reason lets the model handle
the cases the rule did not list. Compare "Never use ellipses" with "Your reply will be read aloud by a
text-to-speech engine, so avoid ellipses, which it cannot pronounce." The second explains itself and
generalises: the model can infer to avoid other unpronounceable marks too.

Context to include, when it applies:

- **Who the reader is** and what they already know.
- **The situation:** what led to this request, what has been tried.
- **The source material:** the document, data or text to work from, supplied in the prompt, because the
  model has no other access to it (module 1).
- **What the output is for:** the decision or action it feeds.

More context is not automatically better (module 1, context rot): include what changes the answer, and
leave out what does not.

### Give Claude a role, for what it is

Role prompting means telling the model who it is for this task: "You are a careful financial analyst
reviewing a loan application." A role sets vocabulary, depth, tone and what to pay attention to, and it
costs one sentence. The best-practices index lists it as a core technique, and system prompts are the usual
place for it in an application (module 24).

Keep three limits in mind:

1. **A role is a request, not a credential.** A "medical expert" role does not make the answer medically
   reliable; it changes style and focus, not truth (module 1).
2. **A role needs a task.** "You are a helpful assistant" adds little; "You are a support analyst. Classify
   each ticket..." does.
3. **A role is not a safeguard.** "You are a security guard who never reveals the key" is a request that can
   be argued around; protecting a secret is a job for code (module 41).

### Order matters a little

Two ordering habits from the documentation are worth adopting. For long inputs, put the **document first
and the question last**: the vision page says "just as placing long documents before your query improves
results in text prompts, Claude works best when images come before text." And restate the task after a
long block of material so it is the last thing read.

## Examples

A support-triage prompt, built up in three steps.

<!-- illustrative -->
```text
Step 1 (vague):
  "Triage this ticket."

Step 2 (clear and direct):
  "Classify the support ticket below as one of: billing, bug, account, other.
   Reply with the label only."

Step 3 (with purpose, role and context):
  "You are a support analyst at a subscription software company. Tickets are routed to teams by
   your label, and a wrong label delays the customer, so choose 'other' when no category clearly
   fits. Classify the ticket below as one of: billing, bug, account, other.
   Reply with the label only.

   Ticket:
   {the ticket text}"
```
<!-- /illustrative -->

What changed from step 2 to step 3: a role (one sentence), the **reason** labels matter (which tells the
model how to treat the uncertain case), and the ticket placed last. The reason is what makes "choose
'other' when unsure" make sense, so the model is more likely to do it consistently.

## Traps

1. **Stacking adjectives instead of constraints.** "Be concise, thorough, clear and professional" gives
   conflicting, unverifiable demands. Give measurable ones: "at most 120 words, one recommendation first."
2. **Over-trusting the role line.** A role changes the voice, not the facts. Verify output as before.
3. **Hiding the task in the middle.** A key instruction buried in a long paragraph is easy to underweight.
   Put it on its own line, and say it again after long material.

## Quiz

1. A prompt says: "Condense the contract, but don't be too long." Results range from one line to two
   pages. What revision is most likely to fix the inconsistency?
   - **a**: "Limit it to five bullets, each under twenty words"
   - **b**: "Condense the contract, and make sure to be brief"
   - **c**: "As an expert lawyer, condense the contract"
   - **d**: "Condense the contract very carefully and concisely"

2. An application's prompt says "Never mention competitors." The model sometimes names a competitor when
   comparing features asked for by users. Which change best follows the page's guidance?
   - **a**: Capitalise the rule so it stands out
   - **b**: Repeat the rule at the top and bottom
   - **c**: Explain the reason: legal advice forbids naming brands in replies
   - **d**: Remove the rule and trust the model's judgment

<details>
<summary>Answer key</summary>

1. **a**. A measurable constraint is clear and direct and can be checked afterwards (clear and direct section). *b* is ruled out because "brief" is an unmeasured adjective, the first trap. *c* is ruled out because a role changes voice and focus, not the length. *d* is ruled out because stacking adjectives gives the same unmeasurable demand.
2. **c**. A rule with its reason lets the model apply it to cases the rule did not list (purpose and context section). *a* is ruled out because emphasis adds no information. *b* is ruled out because repetition without a reason leaves the model guessing the boundary. *d* is ruled out because dropping the rule gives up a requirement the application has.

</details>
