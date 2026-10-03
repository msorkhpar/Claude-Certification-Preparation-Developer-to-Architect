# Level 1 mock exam

**Level:** Foundations · **Module 11:** Exam readiness 1 · **Page 4 of 4**
**Exams:** all (the questions follow the Associate blueprint, AS1 to AS7, over the content of modules 1 to 11)

**After this page you can** tell whether you are ready for the Associate exam, and which domains of Level 1 need more
work before you sit it.

This mock exam covers **the whole of Level 1** (modules 1 to 11) and not one page or one module. It is written in the
exam's style: a short scenario, one best answer and three plausible alternatives, each of which is a mistake a practitioner
could make. Every question is the course's own, written fresh; no question comes from a live exam. The facts behind each
answer were read on 2026-10-02 from the official pages named on the module pages, so a reader who studies
the pages can answer each question from them.

## How to take it

1. **Choose your conditions.** For a readiness test, take all 30 questions in **60 minutes** with no notes, which is the
   Associate exam's pace of two minutes a question. For learning, take it untimed and read each explanation.
2. **Answer every question.** The guides describe no penalty for a wrong answer.
3. **Mark each answer** and open the folded key at the end only when you finish.
4. **Score by domain,** not only in total, using the table below, and spend your next study session on the weakest domain
   with the most weight.

There is no official conversion from a mock percentage to a result: **the real pass mark is a scaled score of 720 on a
scale from 100 to 1,000**, so treat the mock as a guide to where you stand in each domain and never as a prediction.

## What it covers

The 30 questions are spread by the Associate blueprint's weights and grouped by domain here for easy scoring; the real
exam mixes them. The modules column shows which pages to revisit for a miss.

| Questions | Associate domain | Weight | Count | Drawn from modules |
|---|---|---|---|---|
| 1 to 4 | AS1 Prompting and task execution | 14% | 4 | 5, 6 |
| 5 to 10 | AS2 Output evaluation and validation | 21% | 6 | 1, 4, 5, 7, 10 |
| 11 to 14 | AS3 Product and model selection | 12% | 4 | 1, 3, 7 |
| 15 to 19 | AS4 Workflow integration and solution design | 16% | 5 | 4, 5, 8, 9 |
| 20 to 23 | AS5 Configuration and knowledge management | 12% | 4 | 7, 8 |
| 24 to 27 | AS6 Governance, risk and responsible use | 15% | 4 | 8, 10 |
| 28 to 30 | AS7 Troubleshooting and optimisation | 10% | 3 | 3, 6 |

Module 2 (how models are made) is beyond the Associate blueprint, so its ideas appear only inside the reasoning of a few
answers, for example that the weights do not change between chats. Module 11 is tested in the last question, on how to revise.
The questions on how models work (tokens, sampling, context, hallucination, tiers) are the foundation the Developer and
Architect exams build on, which is why Level 1 is shared by all four exams.

## Mock exam

This mock exam covers the whole of Level 1, modules 1 to 11. Choose one answer for each question.

1. A user pastes a 25-page report beneath a single line of instruction and gets weak, unfocused answers. She also wants Claude to treat the pasted text only as something to read. Which change fits best?
   - **a**: Add a role such as world-class analyst to sharpen the focus
   - **b**: Wrap the material in tags, place it first and end with the question
   - **c**: Repeat the instruction in the middle of the report as well as at the top
   - **d**: Move the report above the instructions and let its position mark it as data

2. A prompt supplies four policy files, one of them a vendor email that says "ignore the instructions above". The answers never state which file backs each claim, so reviewers cannot trace them. Which change fits best?
   - **a**: Place the files above the instructions, which marks them as data for the model
   - **b**: Tell Claude to cite its sources more carefully in each answer it writes
   - **c**: Give each item an indexed tag, declare it data, and ask for the index cited
   - **d**: Use one tag for all four files and ask for the page number cited

3. A team needs replies in a rarely seen house format of three labelled lines, including a fallback line for tickets
   that fit no category, and instructions alone give inconsistent layouts. Which prompting approach fits best?
   - **a**: Several varied samples, one of them covering the unclassifiable case
   - **b**: One sample taken from an unrelated task, shown after the instructions
   - **c**: Three near-identical easy samples, to keep the layout consistent
   - **d**: A longer written description of the layout, with no samples attached

4. A manager types "Write something about our Q3 results" and receives a generic paragraph with no figures. The
   board pack is due within the hour. Which revision best fixes the request?
   - **a**: Name the readers, the form, the numbers to use and how to treat gaps
   - **b**: Add that the result must be concise, thorough, clear and professional
   - **c**: Add a role such as senior analyst and leave the rest as it is
   - **d**: Run the request three times and keep the best-sounding result

5. A knowledge-base assistant answers every question, even when the manuals say nothing, and sometimes invents a
   procedure. The manuals are already included with each request. Which prompt addition best tackles this?
   - **a**: Permission to admit that the text does not cover the case
   - **b**: A lower temperature, so that it stops guessing at missing steps
   - **c**: A confidence percentage placed beside each answer it gives
   - **d**: A second copy of the manuals at the end of each request

6. A test asserts that a Claude reply matches a stored string exactly. It passes today and fails tomorrow for the
   same input. What is the best interpretation?
   - **a**: The vendor swapped the model silently, so the id must be pinned and re-recorded
   - **b**: Replies vary even at fixed settings, so judge quality across many cases
   - **c**: The prompt is faulty, and a line such as be consistent will stabilise it
   - **d**: The machine's clock changed the output, so the time must be frozen

7. A clinic plans to have Claude read CT studies uploaded by patients and send the findings straight
   to them. Which concern matters most?
   - **a**: Cost: each scan would use too many visual tokens for the clinic's budget
   - **b**: These scans lie outside the design, so a qualified reviewer must vet outputs
   - **c**: Naming: Claude would add the patients' names to its findings
   - **d**: Privacy: the scans would stay in the chat and surface in later conversations

8. A communications assistant has Claude draft a press note that goes out under the organisation's name within the
   hour. It holds a remark attributed to the chief executive and three figures. What should be checked first?
   - **a**: Spelling and tone, since colleagues will proofread those anyway
   - **b**: The quote and the statistics, against their originals
   - **c**: Whether Claude says it is confident about each claim
   - **d**: Whether a second Claude draft repeats the same claims

9. An analyst needs Claude's output to load into a database through a script, and also to show executives a short takeaway.
   Which pair of formats fits?
   - **a**: A published artifact with shared storage that serves both audiences at once
   - **b**: An artifact for the ingest step, and a table in a slide deck for leadership
   - **c**: Inline replies for both audiences, since artifacts only suit code
   - **d**: Machine-readable results for the ingest step, a brief inline note for leadership

10. An analyst asks Claude for the reasons a new policy will succeed, and the reply gives only supporters' views.
   The memo goes to a review committee. What is the best next step?
   - **a**: Ask Claude whether its own answer was biased and accept a no
   - **b**: Add a line saying that an AI drafted the memo for the committee
   - **c**: Reword the request to ask for the strongest case on each side
   - **d**: Move to a larger tier, whose answers lean less toward one view

11. A retailer will draft about 5,000 short order-status replies an hour, and a few each hour need delicate
   judgment. Speed and cost matter most. What is the best opening move?

   - **a**: Use the middle tier until the invoices arrive, then decide
   - **b**: Use the most capable tier for every reply to protect quality
   - **c**: Begin on the fast, cheap tier and send hard cases higher
   - **d**: Use the cheapest tier for every reply, hard cases included

12. A long conversation in the app starts contradicting decisions made hours earlier, though no error appears. The lead
   wants the project to continue with every decision kept. Which action fits?
   - **a**: Ask Claude to restate every decision in the same conversation and continue
   - **b**: Switch to a model with a larger window and continue in the same conversation
   - **c**: Begin a new chat with a short summary of what was agreed
   - **d**: Paste the whole earlier transcript into a new chat

13. A strategist must compare twelve vendors using public sources and wants cited findings by tomorrow, with several rounds
    of investigation expected. Which feature fits best?
    - **a**: Research mode, with the web lookup setting enabled
    - **b**: A Project holding the vendors' brochures and no web access
    - **c**: A plain chat reply, since citations arrive with every answer
    - **d**: An artifact that renders the comparison as a dashboard first

14. A user tells Claude about her project in an incognito chat and finds that the next chat has no trace of it. Is this a fault?
    - **a**: No: she should have used a Project, since Projects always remember everything
    - **b**: Yes: the weights should have updated after the conversation
    - **c**: Yes: memory failed because the conversation was too short
    - **d**: No: such conversations are kept out of memory and history by design

15. A COO asks which of four chores suit Claude with review and no extra tools: (1) drafting vendor-email variants,
   (2) making the final call on which supplier to drop, (3) summarising call transcripts, (4) calculating invoice
   totals to the cent in the chat. Which pair is right?
   - **a**: The second and fourth
   - **b**: The first and third
   - **c**: The third and fourth
   - **d**: The first and second

16. A department head must explain to staff what Claude changes in the team's work. Which message has all three parts of the course's structure?
   - **a**: It speeds first drafts and summaries, and people still check claims and decisions
   - **b**: It speeds drafts and summaries, people still check claims, results get measured
   - **c**: People still check claims and decisions, and we will measure spot-check rates
   - **d**: It handles drafts and summaries, and review can shrink once the first month looks clean

17. An FP&A analyst must trace how a revenue figure in a long workbook is derived before presenting it. Which capability of the
    Excel add-in helps most directly?
    - **a**: Reading closed workbooks stored in the shared drive
    - **b**: Automatic sign-off of the figure by the add-in once it is asked
   - **c**: Answers about the open file that carry cell-level citations
   - **d**: Macro execution that recalculates the whole workbook automatically

18. A shop's support inbox gets about 300 routine order questions a day, plus occasional warranty and refund
   disputes, and nobody reviews the answers sent today. Which workflow change fits?
   - **a**: Draft everything on the top tier and send the replies out automatically
   - **b**: Draft everything on a fast tier and spot-check the results, disputes included as well
   - **c**: Keep the replies manual and use Claude only to correct their spelling
   - **d**: Spot-checked fast-tier drafts, with a person signing off any reply that commits money

19. An online shop wants every order with a postcode outside its delivery zones flagged automatically, and 12,000 orders arrive
    daily. Which design is best?
    - **a**: A short program applies the geographic rule; Claude drafts customer notes
    - **b**: Claude reads each order and decides whether the postcode lies out of zone
    - **c**: Claude checks a sample of orders and the rest are assumed to be fine
    - **d**: Claude flags suspicious postcodes by feel, then code confirms them

20. A coordinator builds a small published artifact where parents of pupils enter their children's dietary needs,
   and it keeps data. What should she check before inviting people?
   - **a**: Whether its storage is personal or visible to every user
   - **b**: Whether the artifact runs past fifteen lines, which decides its storage
   - **c**: Whether each visitor has the creator's plan, as usage bills to it
   - **d**: Whether visitors have connected the creator's own apps

21. A Project's HR assistant quotes a leave policy that was replaced, and both the old and the new document sit in
   its knowledge. Compliance wants the prior edition to stay retrievable for audits. What is the best fix?
   - **a**: Ask Claude to merge both files into a third file and use that one
   - **b**: Keep both and add an instruction to prefer the newer document when they conflict
   - **c**: Take the outdated file out of the workspace and archive it in your own storage
   - **d**: Switch on memory so that Claude learns which policy is current

22. A team lead wants the whole organisation to use a Project of staff guidance, but salary bands may be seen only by the HR group. Which approach is right?
   - **a**: Share one Project widely and tell its instructions to withhold the salary bands
   - **b**: Share one Project widely with view-only access, which keeps viewers out of the knowledge
   - **c**: Share one Project widely now, and delete the salary bands later if anyone objects
   - **d**: Put the confidential figures in a second workspace for HR alone and share the other widely

23. A team wants every Claude Tag reply in one Slack channel to be brief and to link its source. How should the lead set this up?
    - **a**: Edit the model's settings so that its temperature drops to a lower value
    - **b**: Ask each member to type the rule again in every single thread they open
    - **c**: Set it in a Project, because Slack reads the Project's own instructions first
    - **d**: Tell the assistant so right there, where it is saved as standing instructions

24. A researcher must report survey findings by area and age group, and policy restricts personal data. The
   responses include names and a free-text column. Which preparation is best?
   - **a**: Delete only the names column and upload the rest, since open text rarely identifies anyone
   - **b**: Coarsen places and ages into bands, strip identities, and read the open answers
   - **c**: Replace each name with a code and keep the key list in the same file
   - **d**: Upload everything and ask for a summary that leaves out personal details

25. A professional asks Claude in Chrome to shortlist conference venues while her banking site is open in another
   tab, and she has ten minutes before a call. Which habit best limits harm?
   - **a**: Rely on the classifiers, which screen each action
   - **b**: Leave the bank tab open but ask Claude to skip it
   - **c**: Switch on Skip all approvals so that no prompt interrupts the task
   - **d**: Use a clean profile, and approve each action manually

26. A team on a commercial plan asks whether Anthropic may use its inputs to build better models, and how long chats
   are kept. Which answer is accurate?
   - **a**: They are used unless an owner opts out, and they are then kept for up to five years
   - **b**: They are never used, and they are always deleted the moment a chat comes to an end
   - **c**: They are used only if each member allows it in their own privacy settings
   - **d**: Not by default, and deleted within 30 days unless custom or zero retention applies

27. A manager lets Cowork reach her entire documents drive to save time, and it holds tax returns and exported passwords. What
    should change?
    - **a**: Keep the access and rely on the cloud sandbox to protect everything
    - **b**: Keep the access but switch to automatic approval for speed
    - **c**: Connect a dedicated folder containing only the task's files
    - **d**: Keep the access, but ask Claude to ignore the sensitive files

28. A model gives correct, well-formatted labels but is too slow for a live widget, and the labels must stay as
   accurate as today. The instructions and context are good. Which step fits?
   - **a**: Cut the instructions to a single line to save processing time
   - **b**: Add three more examples so the model answers more briefly
   - **c**: Test a faster tier or lower effort on the same set of cases
   - **d**: Turn off thinking everywhere and ship without re-running the tests

29. A nightly job re-scores 200,000 archived documents, and the results are due the next morning. Which cost lever fits best?
    - **a**: Batched requests, which are half the price of standard ones
    - **b**: Prompt cache reads, which price every output token lower
    - **c**: Fast mode, which doubles the output speed at premium pricing
    - **d**: Higher effort, which trades latency for cost

30. A nightly job summarises 3,000 documents. The output is good, but the bill is above budget and the run finishes
   after the morning deadline. Which plan fits the course's method?
   - **a**: Rewrite the instructions, add examples and change tier together, then compare
   - **b**: Move every document to the lower tier and skip testing, since the instructions are unchanged
   - **c**: Raise effort to the maximum so that fewer retries are needed
   - **d**: Name the cause, try one lower-cost faster tier on the same test set, and compare

<details>
<summary>Answer key</summary>

1. **b**. Long inputs go near the top, tagged as data, with the query at the end (module 6, pages 1 and 2). *a* is
   ruled out because "A role is a request, not a credential", so it changes voice and not focus on the material. *d*
   is ruled out because "Moving a pasted document above the instructions does not mark it as data". *c* is ruled out
   because "A key instruction buried in a long paragraph is easy to underweight".
2. **c**. Nested tagged blocks with an index let the answer name the one it used, and a statement that the content
   is data does the marking (module 6, page 2). *a* is ruled out because "Order is not a boundary", and an email
   that says "ignore the instructions above" still reads as an instruction wherever it sits. *b* is ruled out
   because the guidance is to "Refer to tags by name in the instructions", which a general plea does not do. *d* is
   ruled out because the documents must nest "each with an index or a name, so the answer can say which one it
   used".
3. **a**. A rare format with subtle boundaries calls for several varied examples, including a borderline case
   (module 6, page 2). *d* is ruled out because zero-shot suits a case where "The task is common and easy to
   describe", and this format is not. *b* is ruled out because examples should "Mirror your actual use case
   closely". *c* is ruled out because "Three easy, near-identical examples teach one pattern", and the fallback case
   would be mishandled.
4. **a**. A good description gives the product, the process and the performance, with the goal and context. *b* is
   ruled out because "Stacking adjectives instead of constraints" gives conflicting, unverifiable demands. *c* is
   ruled out because "A role is a request, not a credential", so a role does not supply missing figures. *d* is
   ruled out because "Regenerating the same request and hoping is the reflex; revising the request is the method".
5. **a**. Permission to say the material is insufficient is the first listed technique (module 1, page 2). *b* is
   ruled out because "A prompt sentence does not remove sampling" and a setting does not give the model a way to say
   it does not know. *c* is ruled out because "asking a model how sure it is does not make a wrong answer right".
   *d* is ruled out because "more context isn't automatically better", and the manuals are already supplied.
6. **b**. Output varies by sampling and infrastructure, so one run proves nothing (module 1, page 2). *a* is ruled
   out because every id is pinned, "including the dateless IDs used from the 4.6 generation on". *c* is ruled out
   because "A prompt sentence does not remove sampling". *d* is ruled out because "The model has no clock", so
   freezing one changes nothing.
7. **b**. The vision limits include scans, and a decision reaching individuals needs a qualified reviewer (modules 4
   and 10). *a* is ruled out because Claude is "not designed to interpret complex diagnostic scans such as CTs or
   MRIs", whatever the cost or the image quality. *d* is ruled out because "A conversation with Claude does not
   teach it anything that carries to the next conversation". *c* is ruled out because "Claude cannot be used to name
   people in images, and refuses to".
8. **b**. The claims that would hurt if wrong are checked against the source of record (module 5, page 2). *a* is
   ruled out because "Readers check tone and grammar, which are visible, and skip figures and citations, which are
   where the damage is". *c* is ruled out because "Self-reported confidence is not a measure of accuracy". *d* is
   ruled out because the routine is "Check those against the source of record, not against the model".
9. **d**. Data a program reads is structured data, and a short takeaway stays inline (module 7, page 2). *b* is ruled out because "Data that another program will read is structured data", not an artifact. *c* is ruled out because an artifact suits "A document, deck, dashboard or small tool to hand to someone", not only code. *a* is ruled out because the warning is to "check whether the artifact uses shared storage" before sensitive entries.
10. **c**. Bias often comes from the framing of the request, so the request is revised and both framings are
   reviewed (module 5, page 2). *a* is ruled out because asking the model "for its confidence does not verify
   anything". *b* is ruled out because the fairness question is whether the output will "present one side as the
   whole", which a disclosure line leaves untouched. *d* is ruled out because "Bias in a response often comes from
   the framing of the request, so check the prompt as well as the answer".
11. **c**. High-volume routine work starts on the efficient tier with the hard cases sent upward, and it is
   validated on real data (module 3, page 1). *a* is ruled out because the decision rule is to "build benchmark
   tests for your use case", not wait for invoices. *b* is ruled out because "the top tier for everything wastes the
   budget". *d* is ruled out because "the cheapest model for everything under-serves exactly the cases that need
   depth".
12. **c**. A fresh start with a summary keeps the useful context and drops the rot (module 1, page 1). *a* is ruled
   out because "accuracy and recall degrade, a phenomenon known as context rot", and a restatement inside the same long conversation leaves the degraded context in place. *b* is ruled out because "Larger windows raise the ceiling; they do not
   remove the need to decide what goes in". *d* is ruled out because "more context isn't automatically better", and
   the whole transcript brings the rot with it.
13. **a**. Research runs many linked searches with citations and needs web search on (module 7, page 2). *b* is ruled out because a Project suits "Recurring work with fixed rules and reference files". *c* is ruled out because plain chat is for "A one-off question, a draft, a quick comparison". *d* is ruled out because an artifact is for "A document, deck, dashboard or small tool to hand to someone", not for gathering sources.
14. **d**. Incognito chats are kept out of memory and history on purpose (modules 7 and 10). *b* is ruled out because "Weights are fixed at inference time". *c* is ruled out because incognito chats "keep a conversation out of memory and history", so memory did not fail. *a* is ruled out because "Each project has its own separate memory space and dedicated project summary", and a Project does not remember everything.
15. **b**. Chores 1 and 3 are language-shaped work that a review can catch, while the supplier decision and exact
   sums are not (module 5, page 1). *a* is ruled out because chore 2 needs "judgment that carries accountability",
   and chore 4 needs exactness. *c* is ruled out because chore 4, the totals, needs "exactness (money, counts,
   identifiers)". *d* is ruled out because "A step stays with a person, or with code" when it needs accountable
   judgment such as the supplier decision in chore 2.
16. **b**. A fair message says what it speeds up, what still needs a person and how the team will know it works
   (module 5, page 2). *a* is ruled out because the message must also say "how you will know it works (a spot-check
   rate, an example set, a before and after on time)". *d* is ruled out because "A good draft does not show the
   figures were right", so review cannot shrink on a clean run. *c* is ruled out because the message must also say
   "what it speeds up (first drafts, summaries, comparisons, variations)".
17. **c**. The add-in answers with clickable cell-level citations (module 8, page 2). *b* is ruled out because it is "Not recommended for final client deliverables without human review". *d* is ruled out because "Macros and VBA, and data tables, are unsupported". *a* is ruled out because "Claude can read and write only files that are open at that moment".
18. **d**. High-volume low-stakes drafting suits a faster, cheaper model with a spot check, and a customer's money
   does not go out on trust (module 9, page 2). *a* is ruled out because "the top tier for everything wastes the
   budget", and nothing stops an unchecked commitment. *b* is ruled out because "a contract, a legal term or a
   customer's money does not go out on trust". *c* is ruled out because "High-volume, low-stakes drafting suits a
   faster, cheaper model" with a spot check, so manual work throws the benefit away.
19. **a**. A rule that fits in one line belongs in code (module 4, page 2). *b* is ruled out because code is "Cheaper, faster, deterministic, testable". *c* is ruled out because "A prompt is a request, not a guarantee", and a sample leaves the rest unchecked. *d* is ruled out because "If you can write the rule, write it; add a model only for the cases the rule cannot express".
20. **a**. Shared storage is visible to every user, so check it before sensitive entries (module 7, page 2). *b* is
   ruled out because the fifteen-line figure is about when an artifact is made, "typically over 15 lines, and
   reusable on its own". *c* is ruled out because "Their use counts against their own plan limits, not the
   creator's". *d* is ruled out because "each person connects their own apps even in a shared artifact".
21. **c**. A deleted file is a guarantee, and the archived copy in your own storage meets the audit need (module 7,
   page 1). *a* is ruled out because "Judge the source before you add it", and a merged file adds a new unchecked
   source. *b* is ruled out because "an instruction is a request and a deleted file is a guarantee", so the conflict
   stays possible. *d* is ruled out because "memory is not a substitute for a knowledge file".
22. **d**. Sharing shows the instructions and every file to everyone with access, so the bands live in a Project
   that only HR can open (module 7, page 1). *a* is ruled out because the guidance is "Treat sharing as publishing",
   and an instruction is a request. *b* is ruled out because can view gives "read-only access to contents, knowledge
   and instructions", which is reading. *c* is ruled out because "anyone with access can read the instructions and
   every file in the knowledge base", and deleting later does not undo that.
23. **d**. Standing instructions are set by telling Claude in the channel, and they go to channel memory (module 8, page 2). *b* is ruled out because "they go to channel memory, which admins can review and delete", so members need not repeat them. *c* is ruled out because "Project instructions apply to every chat in that Project", not to Slack. *a* is ruled out because "The models this course uses do not accept them", so a temperature setting is not available.
24. **b**. Generalise, and check free text, which hides details (module 10, page 1). *a* is ruled out because "Notes
   columns hide names and details". *c* is ruled out because "Pseudonymised data is still personal data under many
   rules, because the key can reverse it". *d* is ruled out because asking for a summary without personal details
   "is a request, and it does not satisfy a policy control".
25. **d**. A separate profile and manual approval limit what Claude can see and do (module 8, page 2). *a* is ruled
   out because the page says plainly that "the risk is not zero". *b* is ruled out because "whatever is visible in
   them becomes part of the conversation". *c* is ruled out because in that mode "Claude does not pause and nothing
   checks its actions".
26. **d**. Commercial products are not used to train models by default, and data is deleted within 30 days by
   default (module 10, page 1). *a* is ruled out because "By default Anthropic does not use inputs or outputs from
   commercial products to train models". *b* is ruled out because "Inputs and outputs are deleted within 30 days by
   default", not at once. *c* is ruled out because the choice in privacy settings is the consumer rule, while
   commercial plans start from "does not use inputs or outputs from commercial products to train models".
27. **c**. A dedicated working folder limits what a mistake or an injected instruction can reach (module 8, page 1). *b* is ruled out because the habit is to "use Manual approval for sensitive files, new tools and hard-to-undo actions". *a* is ruled out because the isolation "doesn't limit what Claude reads or does" through connected tools. *d* is ruled out because the habit is to "grant access selectively and avoid financial documents, credentials and personal records".
28. **c**. Correct instructions and context with a speed miss point to another tier or lower effort, tested on the
   same set (module 6, page 4). *a* is ruled out because the table describes this case as "Correct instructions,
   correct context, still a capability or speed miss". *b* is ruled out because "every example is tokens in every
   request", which adds delay. *d* is ruled out because the habit is to "Change one thing at a time and re-run the
   same test inputs".
29. **a**. Latency-tolerant bulk work suits batches, which are 50 percent cheaper (module 3, page 1). *b* is ruled out because cache reads are described as costing "a fraction of the base input price", and these documents are all different. *c* is ruled out because fast mode offers "up to 2.5x higher output speed at premium pricing", which raises cost. *d* is ruled out because effort "trades intelligence for latency and cost within a single model", not bulk cost.
30. **d**. The method is to diagnose first, change one thing and compare on the same inputs, and a faster lower-cost
   tier answers both the bill and the deadline. *a* is ruled out because the habit is to "Change one thing at a time
   and re-run the same test inputs". *b* is ruled out because "Having a good evaluation set is the most important
   step". *c* is ruled out because effort "trades intelligence for latency and cost within a single model", and a
   maximum setting adds both.

</details>
