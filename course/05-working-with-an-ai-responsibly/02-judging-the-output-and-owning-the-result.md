# Judging the output and owning the result

**Level:** Foundations · **Module 5:** Working with an AI, responsibly · **Page 2 of 2**
**Exams:** AS2, AS4, AS6 (and all exams)

**After this page you can** evaluate an output for accuracy, completeness, bias and audience fit, choose how
much checking a result needs, explain Claude's value and limits to colleagues, and say what you owe the
people who receive AI-assisted work.

Checked against the Associate exam guide (domains 2, 4 and 6, version 1.0, July 2026) and the Claude API
documentation on reducing hallucinations (2026-10-02). Examples are hand-scripted and labelled
illustrative.

## Why it matters

Domain 2 of the Associate exam, evaluating and validating output, is the largest (21 percent), and the same
skill sits under the Developer and Architect exams as "skepticism toward confident output" and human
review. A fluent answer is cheap to produce and easy to believe. The people who use AI well are the ones
who check cheaply, check the right things, and say what they did.

## The idea

### Discern: four questions to ask of any output

1. **Is it accurate?** Are the specific claims, figures, names, dates, quotations and citations true? These
   are the details a model can invent (module 1). Confidence of tone is not a signal; the documentation
   calls the hallucination techniques mitigations that "don't eliminate them entirely".
2. **Is it complete?** What is missing? Silent omissions are the quiet failure: a summary can be accurate in
   every sentence and still leave out the clause that matters. Ask what a careful reader would expect to
   see, and look for it.
3. **Is it fair?** Does it lean on stereotypes, favour one framing, or present one side as the whole? Bias
   in a response often comes from the framing of the request, so check the prompt as well as the answer.
4. **Does it fit the audience?** Tone, length, vocabulary and format for the person who will read it. An
   accurate technical answer can be wrong for a board.

### A checking routine that scales

Do not check everything equally. Check in proportion to the cost of being wrong:

1. **Mark the claims that would hurt if wrong:** figures, quotations, citations, legal or medical statements,
   anything that will be acted on.
2. **Check those against the source of record**, not against the model. Asking the model "are you sure?" or
   for its confidence does not verify anything.
3. **Route high stakes to a human reviewer**: regulated content, legal commitments, medical or safety
   statements, anything sent externally in your organisation's name.
4. **Then adapt for the audience** (tone, format, length).

The documentation's own techniques turn some of this into a prompt: ask for supporting quotes, for a
citation per claim, and for the model to withdraw claims it cannot support (module 1, page 2). These make
checking faster; they do not replace it.

<!-- illustrative -->
A hand-scripted check, labelled illustrative. The reply is invented for this page.

```text
Request:  "Summarise the attached regulation for the compliance team. Quote the clause behind each
           point, word for word. If you cannot find a clause, say 'not in the document'."
Reply:    1. Records must be kept for six years. Clause 12.3: "...retained for a period of not less
             than six years..."
          2. Breaches must be reported to the regulator. Not in the document.
Review:   check clause 12.3 against the regulation text (it is a quotation, so a search finds it
          or does not). Point 2 is flagged by the model itself; the reviewer decides whether the
          requirement exists elsewhere before the summary goes out.
```
<!-- /illustrative -->

### Spotting a gap

A declared gap is information. A model that says "data unavailable" is telling you something true; the
careless move is to fill the gap with an estimate, or delete the empty row so the table looks complete.
Keep the gap and decide what to do about it, a rule that appears again in modules 61 and 69.

### Iterate, do not re-roll

When an output is wrong, ask why before asking again. If the request was ambiguous, a second attempt is a
second guess. Name what is wrong, change one thing in the description (page 1, and module 6), and compare
the result with the last. Regenerating the same request and hoping is the reflex; revising the request is
the method.

### Take responsibility

You own what you send, whoever drafted it. That has four consequences:

- **Accountability.** An error in AI-assisted work is your error to correct, and the person who relied on it
  will not distinguish between you and the tool.
- **Disclosure.** Be honest with the people who receive the work about the AI's part when it matters to
  them: when they would rely on it differently, when a policy or a rule requires it, or when they ask. Follow
  your organisation's AI policy (module 10), which may decide the question for you.
- **Data care.** Do not put in what you may not share; anonymise first (module 10).
- **Fairness and impact.** Consider who is affected by the work and whether the AI's part in it is
  appropriate for that purpose.

### Explaining value and limits to colleagues

Associates are expected to tell stakeholders plainly what Claude adds and where it stops. A fair message has
three parts: **what it speeds up** (first drafts, summaries, comparisons, variations), **what still needs a
person** (checking claims, decisions, anything regulated), and **how you will know it works** (a spot-check
rate, an example set, a before and after on time). Avoid both extremes: "it will do the job" and "it cannot
be trusted".

## Traps

1. **Asking the model to grade itself.** Self-reported confidence is not a measure of accuracy.
2. **Checking the easy parts.** Readers check tone and grammar, which are visible, and skip figures and
   citations, which are where the damage is.
3. **Treating a clean-looking table as a complete one.** If a requirement is missing, the table will not
   say so unless you asked it to flag gaps.

## Quiz

1. An analyst asks Claude to summarise a regulation for a compliance team. The summary cites "subsection
   14(b)(ii)" for a retention rule. The analyst is about to forward it. What is the most appropriate step?
   - **a**: Ask Claude how confident it is and forward if high
   - **b**: Look the cited passage up in the original text first
   - **c**: Reword the summary in a more formal register
   - **d**: Forward it and add a note that Claude wrote it

2. A competitor comparison table has one cell marked "data unavailable" while every other cell is filled. A
   colleague asks you to remove that row. Which response is best?
   - **a**: Remove it, to make the table look finished
   - **b**: Ask for repeated attempts until the cell is populated
   - **c**: Replace it with a typical figure for the industry
   - **d**: Keep it, since the gap tells readers where evidence is thin

<details>
<summary>Answer key</summary>

1. **b**. Specific citations are the kind of detail a model can invent, so the claim that would hurt if wrong is checked against the source of record (the checking routine). *a* is ruled out because self-reported confidence verifies nothing. *c* is ruled out because tone does not touch correctness. *d* is ruled out because disclosure does not replace the check, and the recipient would be relying on an unchecked claim.
2. **d**. A declared gap is information (the spotting a gap section). *a* is ruled out because deleting it hides a limitation. *c* is ruled out because an estimate presented as data manufactures evidence. *b* is ruled out because pressing the model for a figure invites invention.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team lead wants to give a monthly board pack entirely to Claude. Which first move reflects sound
   delegation?
   - **a**: List the steps, keep judgment and figure-checking, and hand over drafting
   - **b**: Give Claude the figures and send its pack unchanged
   - **c**: Give Claude the pack only if it is under a page
   - **d**: Hand over everything once Claude has passed one test

2. A marketing assistant asks Claude to "make it better" and receives a full rewrite in an unwanted voice.
   What is the most useful next step?
   - **a**: Ask again with identical wording until it improves
   - **b**: Switch to the most capable tier
   - **c**: State the audience, the tone and what to keep, then compare
   - **d**: Accept the rewrite and edit it by hand

3. A manager wants to tell colleagues about Claude's role in a new report process. Which message is the most
   accurate?
   - **a**: It produces the report, so verification is unneeded
   - **b**: It drafts and summarises faster; people verify figures and decide
   - **c**: It is unreliable, so it should not touch the report
   - **d**: It decides which findings are significant

4. A compliance officer must decide how much to review an AI-assisted email to a regulator. What should the
   amount of review depend on?
   - **a**: The length of the email
   - **b**: The tier of the model that wrote it
   - **c**: The speed at which it was produced
   - **d**: The cost of an error in the claims it makes

<details>
<summary>Answer key</summary>

1. **a**. Sorting steps by judgment versus automatable work is the delegating skill (page 1). *b* is ruled out because nothing checks the output. *c* is ruled out because length is not the criterion. *d* is ruled out because one test does not show the figures hold next month.
2. **c**. Describing the audience, tone and what to keep, then comparing, is the iterate-not-re-roll method. *a* is ruled out because an identical request is a second guess. *b* is ruled out because capability does not supply missing description. *d* is ruled out because it accepts a result that missed the brief without learning why.
3. **b**. It balances value and limits as the page prescribes. *a* is ruled out because verification is the point of discernment. *c* is ruled out because it ignores real value. *d* is ruled out because significance is a judgment step for a person.
4. **d**. Check in proportion to the cost of being wrong (the checking routine). *a*, *c* and *b* are unrelated to consequences: a long email can be harmless, a quick one costly, and the model tier does not replace verification.

</details>
