# Clear, direct instructions, context and roles

**Level:** Foundations · **Module 6:** Prompting fundamentals · **Page 1 of 4**
**Exams:** DV2, DV4, AS1, A4 (A4.1)

**After this page you can** rewrite a vague request into a clear and direct one, add the context that
explains the purpose, and use a role without mistaking it for a guarantee.

Checked against the Claude API documentation on 2026-10-02: the prompt engineering overview, the
Prompting best practices page (which covers the techniques below for Claude Fable 5.1, Opus 5.5, Sonnet 5.5
and Haiku 4.5, among others), the reduce-hallucinations page and the vision page. Where this page gives a working rule of its own, it says so. All model replies on this page
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

Two ordering habits are worth adopting. For long inputs (the best-practices page says 20k tokens and more),
put the **document first and the question last**. The page's wording: "Place your long documents and inputs
near the top of your prompt, above your query, instructions, and examples", and "Queries at the end can
improve response quality by up to 30 percent in tests, especially with complex, multidocument inputs." The
vision page makes the same point for pictures: "just as placing long documents before your query improves
results in text prompts, Claude works best when images come before text." The second habit is a working rule
of this course, not a documented one: restate the task after a long block of material so that it is the
last thing read.

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
   Put it on its own line, and, as the course's working rule, say it again after long material.

## Quiz

1. A prompt says "Condense the contract, but don't be too long." Outputs range from one line to two pages.
   Which revision best fixes the inconsistency?
   - **a**: Cap it at five bullets, each under twenty words
   - **b**: Add "be concise, thorough and precise" so every summary stays focused
   - **c**: Open with "You are a senior contracts lawyer" to set the right length
   - **d**: Restate "keep it reasonably short" as the last line after the contract

2. An application prompt says "Never mention competitors." Users who ask for feature comparisons still
   sometimes get a rival brand in the reply. Which change best follows the page's guidance?
   - **a**: Put the rule in capitals and add "this is critical, never break it" to stress it
   - **b**: Move the rule into a role line: "You are a loyal brand ambassador"
   - **c**: Add the reason, for example that legal advice bars naming other firms
   - **d**: Soften it to "try to avoid competitors where practical"

<details>
<summary>Answer key</summary>

1. **a**. A measurable constraint is clear and direct and can be checked afterwards (the clear and direct section). *c* is ruled out because "A role sets vocabulary, depth, tone and what to pay attention to", not a length that was never stated. *b* is ruled out because stacking adjectives "gives conflicting, unverifiable demands" (the first trap). *d* is ruled out because restating only helps the model find the task: the page's rule is to "restate the task after a long block of material", and "reasonably short" is still an unmeasured demand.
2. **c**. A rule with its reason lets the model apply it to cases the rule did not list (the purpose and context section, with the text-to-speech example). *b* is ruled out because "A role is a request, not a credential", and a role changes the voice, not the obligation. *a* is ruled out because emphasis adds no reason, and "The second explains itself and generalises", which a shouted rule does not. *d* is ruled out because the page contrasts "It would be great if the answer were short" with a firm instruction, so a softer rule gives the model less to generalise from.

</details>
