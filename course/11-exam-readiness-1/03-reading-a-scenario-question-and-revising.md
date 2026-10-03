# Reading a scenario question, and revising

**Level:** Foundations · **Module 11:** Exam readiness 1 · **Page 3 of 4**
**Exams:** all

**After this page you can** read a scenario question for what it really asks, rule out the distractor types the exams use,
and run a revision routine with the course's flashcards, review bank and mock exam.

Written from the exam guides' own descriptions of the item format and from the Associate guide's three sample questions
(version 1.0, July 2026, read 2026-10-02), plus the course's own quiz-writing rules. The guides do not publish a technique
for answering items; the method below is this course's working method, and it is marked as such.

## Why it matters

Every exam in the family is made of scenario questions with one best answer and plausible alternatives. A candidate who
knows the material can still lose marks by answering a question that was not asked, picking an option that is true but
not best, or running out of time on the early items. Technique does not replace knowledge, but it protects it.

## The idea

### What a scenario item looks like

The guides describe items that are **multiple-choice or multiple-response**, and **each item states how many responses to
select**. A typical item has three parts:

1. **A short situation:** a person, a task, a constraint, sometimes a failure.
2. **The ask:** "what is the best first step", "which approach fits", "what is the most likely cause".
3. **Options:** one best answer, and others that are plausible to someone who has made a particular mistake.

The Associate guide's own three samples show the pattern: a summary with a confident-looking citation (verify against the
source), a high volume of short replies where speed and cost matter (a faster, lower-cost model), and a spreadsheet of
personal data under a restrictive policy (remove or anonymise before upload). Each is a small decision with a reasoned
answer and three tempting errors.

### A working method (the course's own)

1. **Read the ask first.** Find the question sentence, then read the situation with it in mind. "First", "best" and "most
   likely" mean different things.
2. **Note how many answers to select.** The item says. Check it again before you submit.
3. **Mark the constraint.** Cost, speed, risk, policy, audience, a deadline: the constraint is what separates the best
   option from the merely possible one.
4. **Name the problem type before you read the options.** Is it missing context, a wrong feature, a data-sensitivity
   problem, a verification gap, a model-choice trade-off? A quick label predicts what the answer looks like.
5. **Predict, then compare.** Form a one-line answer, then look for it. If none matches, re-read the constraint.
6. **Rule out, with a reason each.** For each option you reject, be able to say in a few words why it fails. If you cannot,
   you do not understand it yet.
7. **Pick the best, not the first correct-sounding.** More than one option may be partly true. The best is the one that
   addresses the root cause at the right scale.
8. **Change an answer only for a reason.** A new fact you noticed is a reason; a feeling is not.

### The distractor families

Wrong options are written to be tempting. They fall into families, and spotting the family is often quicker than analysing
the option:

| Family | What it does | Typical sound |
|---|---|---|
| **Too much machinery** | Solves a simple problem with a heavy build | "Build a custom pipeline", "switch to the largest model for everything" |
| **A different problem** | Fixes something real that is not the one described | "Reword the prompt" when the data is missing |
| **Self-verification** | Asks the model to confirm itself | "Ask Claude how confident it is" |
| **A request in place of a guarantee** | Adds an instruction where a check or control is needed | "Tell Claude not to retain the data" |
| **Going around the rule** | Gets the result by bypassing policy | "Use a personal account", "it is only internal" |
| **Right idea, wrong moment** | Correct step done too early, too late or out of order | Scheduling a task before it has been proved |
| **The absolute** | Uses always, never or all | "Always use the top tier" |
| **True but not best** | A correct statement that does not answer this question | A real feature that does not fit the constraint |

The same few lessons answer a large share of Level 1 items, which is why the course repeats them:

- **Confidence is not evidence;** check a claim that matters against the source of record.
- **Exact work belongs in code;** a model approximates counts and sums.
- **A prompt is a request;** what must hold every time belongs in a check or a control.
- **Change one thing at a time** and re-run the same test inputs.
- **Match the model to the task;** the biggest tier for everything wastes budget, the cheapest for everything under-serves.
- **Anonymise before you upload,** and raise a policy conflict with the person who owns the policy.
- **Least reach, least action:** give an assistant only the folders, tools and sites the task needs, and keep a person on
  consequential steps.

### Time

At 120 minutes for 60 items you have about two minutes an item, and less if you want time to review. A sensible pattern is
a first pass at roughly 90 seconds an item, answering what you know and keeping a note of the uncertain ones, then a second
pass on the uncertain ones with the time left. The guides do not describe how the exam interface lets you move between
items or mark them, so **learn the interface from the exam provider's tutorial and do not assume a feature**. The guides also
do not describe any penalty for a wrong answer, so leave no item blank by choice.

### The revision kit in this module

The course gives you three revision tools for Level 1. Each answers a different question.

| Tool | What it is | Use it to |
|---|---|---|
| **Flashcards** | A set of short question-and-answer cards, one fact or distinction each | Learn and recall the terms, numbers and distinctions |
| **Review bank** | A bank of scenario questions with answers and reasons, tagged by domain and by page | Practise applying the ideas, on a spaced schedule |
| **Mock exam** | Thirty exam-style questions across the whole level, weighted like the Associate exam | Test readiness under time, and find weak domains |

All three are plain data files in this module's exercises folder, and `course/README.md` documents their format. Tags let
you filter by Associate domain (AS1 to AS7) or by module.

**Spaced review.** Memory fades quickly and recovers with well-timed practice. A simple schedule: review a new card or
question on the day you meet it, then after 1 day, 3 days, 7 days, 14 days and 30 days, moving a card to the next interval
when you answer it right and back to the first when you do not. Short daily sessions beat long weekend ones. Answer from
memory before you look, because recall effort is what strengthens memory.

**A plan for the last fortnight.**

1. Take the mock exam once, untimed, and mark each miss with its domain and the reason (did not know, misread, took a distractor).
2. Study the weakest domains first, weighted by their share of the exam; domain 2 is the heaviest.
3. Work the review bank for those domains on the spaced schedule, and read the page behind every miss.
4. Take the mock exam again under exam conditions: 60 minutes for its 30 questions, no notes.
5. In the last two days only review flashcards and your own error list, and check the logistics of page 2 (name matches
   ID, booking time, system test or travel).

## Traps

1. **Answering a different question.** Reading the situation and jumping to a familiar fix, without reading the ask or the
   constraint.
2. **Choosing the first option that is true.** More than one can be true; the best one fits the constraint and the root
   cause.
3. **Cramming the night before.** Spaced practice over weeks does more than one long session.

## Quiz

1. A scenario describes a manager asking Claude to summarise a regulation, with a clause cited. Among the options is one
   that has the assistant rate its own certainty. Which distractor family is this?
   - **a**: Self-verification, where the model vouches for itself
   - **b**: Too much machinery, meaning a heavy build for a simple problem
   - **c**: A different problem, meaning a real fix aimed at the wrong target
   - **d**: Right idea, wrong moment, meaning a correct step taken too late

2. A candidate spends several minutes going over the whole scenario twice before locating the question sentence, and then
   runs short of time. Which step of the working method was skipped?
   - **a**: Begin with the ask itself, then take in the situation with that in mind
   - **b**: Form a one-line answer only after reading every option in turn
   - **c**: Change an answer only after spotting a new fact in the text
   - **d**: Count the answers to select just before submitting the item

3. With three weeks left, a candidate misses most mock questions on one heavily weighted topic area. Which plan fits the
   page?
   - **a**: Skip it, because pass or fail ignores results by area
   - **b**: Memorise the mock's answers, since the real exam repeats them
   - **c**: Study that block first, and practise it on a spaced schedule
   - **d**: Take the mock again at once, hoping the result improves

<details>
<summary>Answer key</summary>

1. **a**. The table gives "Asks the model to confirm itself" as the self-verification family, with "Ask Claude how confident it is" as its sound. *b* is ruled out because "Too much machinery" means "Solves a simple problem with a heavy build", and a request for a rating builds nothing. *c* is ruled out because "A different problem" is one that "Fixes something real that is not the one described", while this option targets the real problem with a weak method. *d* is ruled out because "Right idea, wrong moment" is a "Correct step done too early, too late or out of order", and a self-rating is not a correct step at any time.
2. **a**. The first step of the method is "Read the ask first", then the situation is read with it in mind. *b* is ruled out because the method says "Form a one-line answer, then look for it", so the prediction comes before the options and not after them. *c* is ruled out because "A new fact you noticed is a reason; a feeling is not" concerns revising an answer, not the order of reading. *d* is ruled out because the step reads "Check it again before you submit", which comes at the end and does not explain time lost at the start.
3. **c**. The plan is to study the weakest domains first and work the review bank for them on the spaced schedule. *b* is ruled out because the mock's job is to "Test readiness under time, and find weak domains", not to be learned by heart. *a* is ruled out because the plan says to "Study the weakest domains first, weighted by their share of the exam". *d* is ruled out because the plan says to "Take the mock exam once, untimed, and mark each miss with its domain and the reason" before any second attempt.

</details>

## Module quiz

This quiz covers pages 1 to 3 of the module and the mock exam page.

1. A candidate with a documented need for extra time wants to sit the Architect Foundations exam. She books a slot first,
   then requests the adjustment. Which statement is accurate?
   - **a**: The slot is fine, since adjustments are granted on the day itself
   - **b**: Approval had to come before the appointment was made
   - **c**: Approval is unnecessary, as a proctor can add time whenever asked
   - **d**: The adjustment can be made only at a test centre, forcing a rebooking

2. A score report shows 80 percent in one domain, 40 percent in another and a scaled score of 700. What should the candidate
   conclude?
   - **a**: A fail, and a retake can happen the very next day at no extra cost
   - **b**: A pass, because the strong section outweighs the weak one
   - **c**: A fail, as the pass mark is 720; study the weak area first
   - **d**: Borderline, so a reviewer will re-score the weakest section by hand

3. On a 60-item sitting of 120 minutes, a candidate spends five minutes on each of the first twenty items. What follows?
   - **a**: She should rush the first items instead, since early items carry most weight
   - **b**: Nothing, because unanswered items are not scored against her
   - **c**: The proctor pauses the clock, since the sitting allows extra minutes on hard items
   - **d**: Only about thirty seconds per remaining question is left, so she must speed up

4. A candidate plans to take timed mocks only, one per week, and to learn the exam software the night before. Which change fits
   the page best?
   - **a**: Drop the mocks entirely, since the real items will differ in any case
   - **b**: Review missed topics on a spaced schedule, and study the interface well ahead
   - **c**: Cram everything into the final weekend, when memory is freshest
   - **d**: Take two mocks in one day, so that the score settles sooner

<details>
<summary>Answer key</summary>

1. **b**. The fixed order is to request the accommodation and wait for approval before scheduling. *a* is ruled out because "Extra time or a break you did not arrange in advance is not available on the day". *c* is ruled out because "an unapproved break counts as misconduct", so a proctor cannot add time on request. *d* is ruled out because accommodations are for "documented disabilities or needs, in line with applicable law", with no limit to test centres.
2. **c**. The pass mark is a scaled score of 720, so 700 fails, and the sensible reaction is to use the domain figures to aim revision. *b* is ruled out because pass or fail "depends on the total scaled score", not on the stronger domain. *a* is ruled out because retake waits are "14 days after the first, 30 days after the second", and each attempt costs the full fee. *d* is ruled out because you get "pass or fail, your scaled score, and the percentage correct in each domain", with no review of a score by hand.
3. **d**. Twenty items at five minutes use 100 of the 120 minutes, leaving 20 minutes for 40 items. *b* is ruled out because the page says to "leave no item blank by choice", since no penalty for a wrong answer is described. *c* is ruled out because the exam is "120 minutes of exam time", a fixed limit. *a* is ruled out because the sensible pattern is "answering what you know and keeping a note of the uncertain ones", not rushing the start.
4. **b**. The routine pairs spaced practice on misses with learning the interface from the provider's tutorial, well before the day. *a* is ruled out because the mock exists to "Test readiness under time, and find weak domains". *c* is ruled out because "Spaced practice over weeks does more than one long session". *d* is ruled out because "Short daily sessions beat long weekend ones", and two mocks in a day is a long session.

</details>
