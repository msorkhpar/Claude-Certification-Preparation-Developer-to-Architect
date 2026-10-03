# Revising Level 2: flashcards, the review bank and a study plan

**Level:** Developer · **Module 44:** Exam readiness 2 · **Page 2 of 4**
**Exams:** DV1 to DV8 (CCDV-F)

**After this page you can** use the Level 2 flashcards and the spaced-review question bank on a schedule, read a mock exam result by domain and by cause of the miss, and run a four-week plan that spends time where the marks are.

Written from the course's own revision files and the exam map, on 2026-10-03. The spaced-review schedule is the one module 11 introduced; the plan is this course's working method and the guides do not publish a method for preparing. The official exam guide and the Anthropic pages decide every fact about the exam, so check them before you rely on a number here.

## Why it matters

Level 2 holds more than seventy pages, and nobody keeps all of it in mind. The mock exams tell you how you stand, and the revision files tell you how to move. Without a routine, revision becomes rereading the pages that felt good the first time. With one, each session targets a named weakness, returns to it on a schedule, and is finished when the weakness stops appearing in the mock.

## The idea

### The kit

| Tool | What it is | Use it to |
|---|---|---|
| **Flashcards** | 218 short question-and-answer cards for modules 12 to 43, one fact or distinction each | Learn and recall limits, defaults, names and distinctions |
| **Review bank** | 110 scenario questions for modules 12 to 43, with a key and a reason | Practise applying a page to a situation, on a spaced schedule |
| **Mock exam 1 and 2** | Two Developer mock exams of 53 questions each, pages 3 and 4 | Test readiness under time and find weak domains |
| **The Level 1 kit** | The Level 1 flashcards, review bank and mock exam of module 11 | Keep the shared foundations fresh |

The flashcards and the bank are plain data files in `exercises/44-exam-readiness-2/`, `flashcards.json` and `review-bank.json`, and `course/README.md` documents their format. Every card and item names the course page that states the fact, and the Developer domains it serves (DV1 to DV8), so you can filter by domain, by module or by page. The Level 1 files in `exercises/11-exam-readiness-1/` carry the Associate domains; for the Developer exam they still matter, because Level 1 is part of its path.

### Spaced review

Memory fades quickly and recovers with well-timed practice. The schedule is the same as in module 11 and is stored in the bank as `intervals_days`: review a new card or question on the day you meet it, then after 1, 3, 7, 14 and 30 days. An item that you answer correctly moves to the next interval, and an item that you miss goes back to the first. Three habits make it work.

- **Answer before you look.** Say the answer aloud, or write it, then turn the card. The effort of recall is what strengthens memory, and a look at the answer is not recall.
- **Keep sessions short and daily.** Twenty minutes every day beats three hours on a Sunday.
- **Read the page behind a miss.** A wrong answer on a card or an item names a page. Reread that section once, then retest it tomorrow.

### Reading a mock result

A percentage hides what you need. Score by domain, using the table on the mock page, and keep an error log with one line for every miss. Write down three things: the question's number and domain, the page that the key names, and the **cause**.

| Cause | What it looks like | What to do |
|---|---|---|
| Did not know | You have no memory of the fact | Reread the page, make a card for the fact, add it to the schedule |
| Misread | You knew it and answered another question | Practise reading the ask first (module 11, page 3); slow down on "first", "best" and "most likely" |
| Took a distractor | You knew the topic and chose a plausible mistake | Read the key's reasons for each wrong option, and name the mistake in your log |

The distribution of causes tells you what to change. Mostly "did not know" in one domain is a study problem. Mostly "misread" everywhere is a technique problem, and no more reading of pages will fix it. The mock exam is not a conversion to the exam's score: **the pass mark is a scaled score of 720 on a scale from 100 to 1,000**, and no percentage is published for it. Use the domain figures to decide where to work, and not to predict the result.

### A four-week plan

The plan assumes about forty minutes on most days and a little more at the weekend. Change the numbers to your time, and keep the order.

1. **Week 1: baseline.** Take mock exam 1 untimed, with no notes, and read every explanation. Fill the error log. Rank the domains by the weight of the domain times the share you missed. Start the flashcard schedule for the top two domains.
2. **Weeks 2 and 3: the weak domains, by weight.** Work the review bank for the ranked domains, and reread the pages behind your misses. Make a card for every fact that you did not know. In the second week add the next domain on the list. Keep the daily flashcard session going; it is short, and it is the part that holds.
3. **Week 4: exam conditions.** Take mock exam 2 in one sitting of 120 minutes, with no notes and the pacing of page 1. Compare its domain scores with the first mock. Spend the next three days on the domains that still lag. The mock exams use different questions on purpose: the second is a check on what you learned, and not a repeat of the first.
4. **The last two days.** Review flashcards and your error log only, and check the logistics of module 11, page 2: the name on the booking against the ID, the booking time, the system check or the journey.

If your first mock already shows an even profile, shorten the plan and move mock exam 2 earlier. If it shows a gap in a heavy domain, add a week and begin with that domain's pages, not its cards: cards recall a fact, and a pattern needs the page.

### What to do the day before

Stop studying new material. Read the error log once, the flashcards that were missed twice and the decision patterns on page 1. Sleep. The mock exams are practice, and one more of them on the last night teaches you little and costs you rest.

## Traps

1. **Rereading what you already know.** The pages that felt easy are the ones you reread. Work the misses.
2. **Memorising the mock.** The questions are the course's own and will not appear in the exam. What carries over is the reasoning, so learn why each wrong option fails.
3. **Treating "misread" as bad luck.** A misread is a repeatable habit. Name it in the log and fix the habit.
4. **Skipping the schedule.** A card reviewed once and never again is forgotten. The intervals are the method.

## Quiz

1. A candidate answers a flashcard correctly on the day it was made, and again one day later. When should it come back next?
   - **a**: Three days after that review
   - **b**: Never, since it was answered twice
   - **c**: The following morning, to make sure
   - **d**: Thirty days after the first review

2. After mock exam 1 a candidate sees that most misses are items where the right topic was known and a plausible alternative was chosen. What does the page advise?
   - **a**: Read the key's reasons for each wrong option, and name the mistake
   - **b**: Reread every page of the course in order, from module 12
   - **c**: Memorise the mock's answers so that the next sitting goes faster
   - **d**: Take the same mock again tomorrow until the percentage rises

3. A candidate with a first mock showing a gap in a heavy domain has four weeks. Which choice does the plan recommend?
   - **a**: Begin with that area's pages, then use cards and the bank
   - **b**: Only add cards for that domain, which recall a fact
   - **c**: Skip it, since the pass mark ignores any single domain
   - **d**: Move the second mock to the first week to measure the gap

<details>
<summary>Answer key</summary>

1. **a**. The schedule is "after 1, 3, 7, 14 and 30 days", and "An item that you answer correctly moves to the next interval". *b* is ruled out because "A card reviewed once and never again is forgotten. The intervals are the method." *c* is ruled out because the next interval after one day is three days, not a repeat of the first, since a miss is what "goes back to the first". *d* is ruled out because the schedule runs "then after 1, 3, 7, 14 and 30 days", and thirty days is its last step.
2. **a**. The table gives "Took a distractor" the action "Read the key's reasons for each wrong option, and name the mistake in your log". *b* is ruled out because that is a study answer for "Did not know", while the page says the distribution of causes "tells you what to change". *c* is ruled out because "The questions are the course's own and will not appear in the exam", and "What carries over is the reasoning". *d* is ruled out because the plan says the second mock is "a check on what you learned, and not a repeat of the first".
3. **a**. The plan says "If it shows a gap in a heavy domain, add a week and begin with that domain's pages, not its cards: cards recall a fact, and a pattern needs the page." *b* is ruled out for that reason: "cards recall a fact, and a pattern needs the page". *c* is ruled out because the page says to "Use the domain figures to decide where to work", and a weak heavy domain costs the most marks. *d* is ruled out because the plan keeps "mock exam 2 in one sitting of 120 minutes" for week 4, and the first mock is already the baseline.

</details>

## Module quiz

This quiz covers the first two pages of the module. The two mock exams that follow are separate.

1. Domain A weighs 33 percent and was answered at 60 percent. Domain B weighs 8 percent and was answered at 40 percent. Which should lead the study plan?
   - **a**: B goes ahead, since its score is the lower one
   - **b**: A takes priority, since its missed share costs more marks overall
   - **c**: Either one, since a mock percentage does not predict the result
   - **d**: A can wait, since a heavy domain is already well covered

2. A candidate scores well on the review bank but keeps losing marks on mock items because the ask was noticed late. Which change fits the page?
   - **a**: Add another pass through the flashcard schedule
   - **b**: Read the question sentence first, then the situation
   - **c**: Skip the explanations and spend the time on new pages
   - **d**: Raise the pace of the first pass to sixty seconds an item

3. A scenario in an item describes an internal stock-lookup API that several Claude applications should share. What does the decision table lead to, and which published item shows it?
   - **a**: A built-in tool, because it reaches every internal system
   - **b**: A batch job, since nobody is waiting on the lookups
   - **c**: An MCP server, as the guide's third sample illustrates
   - **d**: A longer system prompt, since it is reusable by default

4. The evening before the exam, a candidate has notes about past misses, a flashcard deck and one unread module. What does the plan recommend?
   - **a**: Read the unread module through, to leave no gaps
   - **b**: Take a third mock exam under timed conditions
   - **c**: Review the error log and the facts missed twice, then rest
   - **d**: Make forty new cards from the module until late

<details>
<summary>Answer key</summary>

1. **b**. The page says "a domain at 33 percent where you score 60 percent loses more than a domain at 8 percent where you score 40 percent". *a* is ruled out because the lower score alone is not the measure, and the page says to "spend it where the product of the weight and the miss rate is largest". *c* is ruled out because the page uses the mock "to decide where to work, and not to predict the result", which is a different job from ordering the plan. *d* is ruled out because the page says "one item in three is DV1", so a heavy domain is where the marks are.
2. **b**. The page says to practise "reading the ask first (module 11, page 3)" for a misread, and "slow down on 'first', 'best' and 'most likely'". *a* is ruled out because for a habit of misreading "no more reading of pages will fix it". *c* is ruled out because the page says to "Read the key's reasons for each wrong option", and that needs the explanations. *d* is ruled out because a faster first pass of "about 90 seconds an item" is the guide-based pace, and a shorter one gives less time to read the ask.
3. **c**. The decision table gives "One internal service reused by several Claude applications" the answer "An MCP server", and the guide's third sample asks the same question. *a* is ruled out because the page says "a built-in tool does not reach an arbitrary internal API". *b* is ruled out because the table gives batches to "A large, non-urgent job where cost matters" and the scenario describes sharing. *d* is ruled out because the sample's reasoning says "Prompts and pasted data are neither reusable nor live".
4. **c**. The page says to "Read the error log once, the flashcards that were missed twice and the decision patterns on page 1. Sleep." *a* is ruled out because the page says "Stop studying new material". *b* is ruled out because "one more of them on the last night teaches you little and costs you rest". *d* is ruled out because making new cards is new study, and the plan keeps the last two days to "Review flashcards and your error log only".

</details>
