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
   - **a**: Ask Claude whether it is confident, and forward the summary if it says yes
   - **b**: Look the passage up in the source of record before the text goes out
   - **c**: Ask Claude to cite the subsection again in a fresh reply and compare the two
   - **d**: Forward it with a note that an AI drafted it, so readers can judge

2. A competitor table has one cell marked "data unavailable" while every other cell is filled. A colleague
   asks you to drop that row so the table looks finished. Which response is best?
   - **a**: Ask Claude to try again until every cell holds a value
   - **b**: Fill it with a typical industry figure and label it an estimate
   - **c**: Show a dash in that cell and drop any mention of the missing data
   - **d**: Retain the entry as it stands and state what could not be located

<details>
<summary>Answer key</summary>

1. **b**. Specific citations are the kind of detail a model can invent, so the claim that would hurt if wrong is checked against the source of record (the checking routine). *a* is ruled out because the routine says that asking the model how sure it is, or "for its confidence does not verify anything". *c* is ruled out because the routine says to check "against the source of record, not against the model", and a second reply from the same model is not the source. *d* is ruled out because "Check in proportion to the cost of being wrong", and a note about an AI draft leaves the unchecked claim in place.
2. **d**. A declared gap is information, and the careful move is to keep it and decide what to do (the spotting a gap section). *a* is ruled out because "Regenerating the same request and hoping is the reflex", and pressing for a value invites invention. *b* is ruled out because "the careless move is to fill the gap with an estimate". *c* is ruled out because the page says "Keep the gap and decide what to do about it", not hide it behind a dash.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team uses Claude to draft a monthly variance narrative from a finance export. After a few clean months
   the lead proposes two changes: Claude will also decide which variances reach the directors, and the
   figure-by-figure review stops. Which response applies both pages?
   - **a**: Keep the judgment call with a person and keep verifying every number against its source
   - **b**: Let Claude take the judgment call, since clean drafts show sound judgment, and keep verifying
     numbers
   - **c**: Drop the verification, since clean drafts show the numbers are right, and keep the judgment call
     human
   - **d**: Let Claude take both, and ask it to flag any figure it doubts

2. A support lead gets a drafted reply to an angry customer that is accurate but reads like a legal notice.
   The lead needs the next attempt to come out right. Which change to the request follows the pages?
   - **a**: Open with a role line: "You are a world-class customer-care writer"
   - **b**: Send the identical request again and keep the first answer that sounds friendly
   - **c**: Describe the audience, add one sample in the voice wanted, then compare
   - **d**: Move to a higher tier, which follows loose instructions better

3. A consultant forwards a client a report that Claude drafted. The client later finds a wrong number in it.
   Who answers for the mistake, and what should have happened first?
   - **a**: The tool's provider answers, since its software produced the number
   - **b**: The sender owns the work and should have verified the figures at their source
   - **c**: The recipient answers, since the number was accepted without question
   - **d**: The sender answers, but only if the work lacked a note about the AI's part

4. A team asks Claude to fill a supplier price table from a PDF. Every row comes back filled, including one cost the
   PDF never states, and the table goes to the finance director today. Which pair of actions fits both pages?
   - **a**: Send the table on as filled, trusting the figures that fit the layout
   - **b**: Ask for a confidence beside each entry, then keep the entries marked high
   - **c**: Fill the unknown cost with the average of the other rows and label it an estimate
   - **d**: Have it flag any missing value, keep the gap, and check the rest against the source

<details>
<summary>Answer key</summary>

1. **a**. Judgment that carries accountability stays with a person, and figures are checked against the source of record however clean past drafts were (page 1 delegation table; page 2 checking routine). *b* is ruled out because the table gives "Decide which variances matter to the board" to a person, as judgment with accountability, and a clean run does not move it. *c* is ruled out because "A good draft does not show the figures were right", so a run of clean drafts is no reason to stop verifying. *d* is ruled out because "Self-reported confidence is not a measure of accuracy", and a flag from the model replaces neither the person nor the check.
2. **c**. The description should say who reads it and show the target style, and the iterate-not-re-roll method compares the new result with the last (page 1, describe; page 2, iterate). *b* is ruled out because "Regenerating the same request and hoping is the reflex", and an identical request is a second guess. *a* is ruled out because a role line names no reader or purpose, and the page says to "Add what a colleague new to the job would need". *d* is ruled out because "the description is where most of the quality is decided", and a higher tier does not know the readers either.
3. **b**. The sender owns what was sent, and figures are checked against the source of record before release (page 2, take responsibility and the checking routine; page 1, where checking every figure sits with a person or a script). *a* is ruled out because "You own what you send, whoever drafted it". *c* is ruled out because "the person who relied on it will not distinguish between you and the tool", so blame does not move to the reader. *d* is ruled out because "An error in AI-assisted work is your error to correct", with or without a disclosure note; disclosure is a separate duty.
4. **d**. Page 1 puts the rule in the description ("say so rather than estimating"), page 2 says a declared gap is
   information to keep, and the figures that remain are checked against the source of record. *a* is ruled out
   because "Readers check tone and grammar, which are visible, and skip figures and citations", which is where the
   damage is. *b* is ruled out because "Self-reported confidence is not a measure of accuracy". *c* is ruled out
   because "the careless move is to fill the gap with an estimate", and a label on an invented value still puts it
   in the table.

</details>
