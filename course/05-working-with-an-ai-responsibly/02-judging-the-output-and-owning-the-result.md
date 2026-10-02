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

1. An analyst asks Claude to summarise a regulation for compliance colleagues. The summary cites "subsection
   14(b)(ii)" for a retention rule, and the analyst is about to forward it. What is the most appropriate step?
   - **a**: Look the passage up in the source of record before sending it on
   - **b**: Ask Claude whether it is confident, and forward the summary if it says yes
   - **c**: Ask Claude to cite the subsection again in a fresh reply and compare the two
   - **d**: Forward it with a note that an AI drafted it, so readers can judge

2. A competitor table has one cell marked "data unavailable" while every other cell is filled. A colleague
   asks you to drop that row so the table looks finished. Which response is best?
   - **a**: Ask Claude to try again until every cell holds a value
   - **b**: Fill it with a typical industry figure and label it an estimate
   - **c**: Retain the entry as it stands and state what could not be located
   - **d**: Show a dash in that cell and drop any mention of the missing data

<details>
<summary>Answer key</summary>

1. **a**. Specific citations are the kind of detail a model can invent, so the claim that would hurt if wrong is checked against the source of record (the checking routine). *b* is ruled out because the routine says that asking the model how sure it is, or "for its confidence does not verify anything". *c* is ruled out because the routine says to check "against the source of record, not against the model", and a second reply from the same model is not the source. *d* is ruled out because "Check in proportion to the cost of being wrong", and a note about an AI draft leaves the unchecked claim in place.
2. **c**. A declared gap is information, and the careful move is to keep it and decide what to do (the spotting a gap section). *a* is ruled out because "Regenerating the same request and hoping is the reflex", and pressing for a value invites invention. *b* is ruled out because "the careless move is to fill the gap with an estimate". *d* is ruled out because the page says "Keep the gap and decide what to do about it", not hide it behind a dash.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team lead wants to hand a monthly board pack entirely to Claude. Which first move reflects sound
   delegation?
   - **a**: Give Claude the whole pack once it passes a single test run
   - **b**: List the steps and keep judgment and figure-checking with people
   - **c**: Give it only the narrative sections and send them without review
   - **d**: Hand over everything but ask it to flag anything it is unsure of

2. A marketing assistant tells Claude to "make it better" and gets a full rewrite in an unwanted voice. What
   is the most useful next step?
   - **a**: Ask again with the same wording and pick the best of the replies
   - **b**: Move to the highest tier, which follows loose instructions better
   - **c**: Accept the rewrite and edit it by hand to the voice wanted
   - **d**: Name the audience, the register to aim for and what to keep, then compare

3. A manager must explain Claude's role in a new reporting process to colleagues. Which message is most
   accurate?
   - **a**: It produces the report end to end, with a spot check each quarter
   - **b**: It is too unreliable for reports, so it should only reformat finished text
   - **c**: It speeds up drafting and summaries, while people verify figures and decide
   - **d**: It drafts, and it also ranks which findings matter to the board

4. A compliance officer must decide how much review an AI-assisted email to a regulator needs. What should
   that depend on mainly?
   - **a**: What a wrong claim in it would cost if acted on
   - **b**: How polished the draft reads after one editing pass
   - **c**: Which model tier drafted it, since larger tiers need less review
   - **d**: Whether the draft came with supporting quotes attached

<details>
<summary>Answer key</summary>

1. **b**. Sorting steps into judgment and automatable work is the delegating skill (page 1). *a* is ruled out because "A good draft does not show the figures were right". *c* is ruled out because the table keeps "Check every figure in the draft against the source" with a person or a script, and review is skipped here. *d* is ruled out because "Claude can state a figure that was never supplied", and a flag raised by the same model is not an independent check.
2. **d**. Describing the audience, the style and what to keep, then comparing, is the iterate-not-re-roll method. *a* is ruled out because "Regenerating the same request and hoping is the reflex", and an identical request is a second guess. *b* is ruled out because "the description is where most of the quality is decided", not the tier. *c* is ruled out because the page says "When an output is wrong, ask why before asking again", and hand-editing accepts the rewrite without finding the gap in the request.
3. **c**. The fair message has three parts: what it speeds up, what still needs a person, and how you will know it works. *a* is ruled out because it hands the verification away, the first extreme the page names: "it will do the job". *b* is ruled out because it is the opposite extreme: "it cannot be trusted". *d* is ruled out because deciding which findings matter is "Decide which variances matter to the board", a step the table gives to a person.
4. **a**. Check in proportion to the cost of being wrong (the checking routine). *b* is ruled out because "Readers check tone and grammar, which are visible", and the damage is in figures and citations. *c* is ruled out because the routine says "Do not check everything equally", and names no tier that makes verification unnecessary. *d* is ruled out because supporting quotes make checking faster: "These make checking faster; they do not replace it".

</details>
