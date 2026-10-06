# Revising Level 4: flashcards, the review bank and a study plan

**Level:** Architect Professional · **Module 94:** Exam readiness 4 · **Page 2 of 4**
**Exams:** P1 to P7 (CCAR-P)

**After this page you can** use the Level 4 flashcards and the spaced-review question bank on a schedule, read a mock exam result by domain and by cause of the miss, and run a four-week plan that spends time where the marks are.

Written from the course's own revision files and the exam map, on 2026-10-04. The spaced-review schedule is the one module 11 introduced; the plan is this course's working method and the guides do not publish a method for preparing. The official exam guide and the Anthropic pages decide every fact about the exam, so check them before you rely on a number here.

## Why it matters

Level 4 holds fifteen modules, and nobody keeps all of it in mind. The mock exam tells you how you stand, and the revision files tell you how to move. Without a routine, revision becomes rereading the pages that felt good the first time. With one, each session targets a named weakness, returns to it on a schedule, and is finished when the weakness stops appearing in the mock. Level 4 adds a difficulty of its own: its questions are scenarios, and a fact that you can recite is not yet a fact that you can apply to a scenario in a minute and a half.

## The idea

### The kit

| Tool | What it is | Use it to |
|---|---|---|
| **Flashcards** | 95 short question-and-answer cards for modules 79 to 93, one fact or distinction each | Learn and recall limits, rules, names and distinctions |
| **Review bank** | 48 scenario questions for modules 79 to 93, with a key and a reason | Practise applying a page to a situation, on a spaced schedule |
| **Professional mock exam** | Two mock exams of 63 questions, pages 3 and 4 | Test readiness under time and find weak domains |
| **The earlier kits** | The Level 1 and Level 2 flashcards, review banks and mock exams of modules 11 and 44 | Keep the shared foundations fresh |

The flashcards and the bank are plain data files in `exercises/94-exam-readiness-4/`, `flashcards.json` and `review-bank.json`, and `course/README.md` documents their format. Every card and item names the course page that states the fact, and the Professional domains it serves (P1 to P7), so you can filter by domain, by module or by page. The earlier kits still matter, because Levels 1 to 3 are the path to this exam and the Professional questions assume what they teach.

### Spaced review

Memory fades quickly and recovers with well-timed practice. The schedule is the same as in module 11 and is stored in the bank as `intervals_days`: review a new card or question on the day you meet it, then after 1, 3, 7, 14 and 30 days, each counted from the previous review. An item that you answer correctly moves to the next interval, and an item that you miss goes back to the first. Three habits make it work.

- **Answer before you look.** Say the answer aloud, or write it, then turn the card. The effort of recall is what strengthens memory, and a look at the answer is not recall.
- **Keep sessions short and daily.** Twenty minutes every day beats three hours on a Sunday.
- **Read the page behind a miss.** A wrong answer on a card or an item names a page. Reread that section once, then retest it tomorrow.

### Reading a mock result

A percentage hides what you need. Score by domain, using the table on the mock page, and keep an error log with one line for every miss. Write down three things: the question's number and domain, the page that the key names, and the **cause**.

| Cause | What it looks like | What to do |
|---|---|---|
| Did not know | You have no memory of the fact | Reread the page, make a card for the fact, add it to the schedule |
| Misread | You knew it and answered another question | Read the ask first, the last sentence of the scenario; slow down on "first", "best" and "most likely" |
| Took a distractor | You knew the topic and chose a plausible mistake | Read the key's reasons for each wrong option, and name the mistake in your log |
| Wrong domain | You applied a rule of another layer to the scenario | Name the failing layer before you choose, using the decision patterns of page 1 |

The distribution of causes tells you what to change. Mostly "did not know" in one domain is a study problem. Mostly "misread" everywhere is a technique problem, and no more reading of pages will fix it. "Wrong domain" is specific to scenario exams: the stem mentions retrieval, evaluation and governance in the same breath, and the item rewards the candidate who finds which of them carries the weight. The mock exam is not a conversion to the exam's score: **the pass mark is a scaled score of 720 on a scale from 100 to 1,000**, and no percentage is published for it. Use the domain figures to decide where to work, and not to predict the result.

### A four-week plan

The plan assumes about forty minutes on most days and a little more at the weekend, and it assumes that you have studied Level 4 once. Change the numbers to your time, and keep the order.

1. **Week 1: baseline.** Take the mock exam untimed, with no notes, and read every explanation. Fill the error log. Rank the domains by the weight of the domain times the share you missed. Start the flashcard schedule for the top two domains.
2. **Weeks 2 and 3: the weak domains, by weight.** Work the review bank for the ranked domains, and reread the pages behind your misses. Make a card for every fact that you did not know. In the second week add the next domain on the list. Keep the daily flashcard session going; it is short, and it is the part that holds. Read module 93 once more in week 3: it is the one module that uses all seven domains together.
3. **Week 4: exam conditions.** Take the other mock exam in one sitting of 120 minutes, with no notes and the pacing of page 1, after at least two weeks since the first sitting so that you answer from understanding and not from memory of the keys. Compare its domain scores with the first sitting. Spend the next three days on the domains that still lag.
4. **The last two days.** Review flashcards and your error log only, and check the logistics of module 11, page 2: the name on the booking against the ID, the booking time, the system check or the journey.

If your first sitting already shows an even profile, shorten the plan and move the second sitting earlier. If it shows a gap in a heavy domain, add a week and begin with that domain's pages, not its cards: cards recall a fact, and a pattern needs the page.

### What to do the day before

Stop studying new material. Read the error log once, the flashcards that were missed twice and the decision patterns on page 1. Sleep. The mock exam is practice, and one more sitting on the last night teaches you little and costs you rest.

## Traps

1. **Rereading what you already know.** The pages that felt easy are the ones you reread. Work the misses.
2. **Memorising the mock.** The questions are the course's own and will not appear in the exam. What carries over is the reasoning, so learn why each wrong option fails.
3. **Treating "misread" as bad luck.** A misread is a repeatable habit. Name it in the log and fix the habit.
4. **Skipping the schedule.** A card reviewed once and never again is forgotten. The intervals are the method.
