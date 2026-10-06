# Revising Level 3: flashcards, the review bank and a study plan

**Level:** Architect · **Module 78:** Exam readiness 3 · **Page 2 of 5**
**Exams:** A1 to A5 (CCAR-F)

**After this page you can** use the Level 3 flashcards and the spaced-review question bank on a schedule, read a mock exam result by domain, by scenario and by cause of the miss, and run a four-week plan that spends time where the marks are.

Written from the course's own revision files and the exam map, on 2026-10-04. The spaced-review schedule is the one module 11 introduced; the plan is this course's working method and the guides do not publish a method for preparing. The official exam guide and the Anthropic pages decide every fact about the exam, so check them before you rely on a number here.

## Why it matters

Level 3 holds more than sixty pages of design decisions, and nobody keeps all of it in mind. The mock exams tell you how you stand, and the revision files tell you how to move. Without a routine, revision becomes rereading the pages that felt good the first time. With one, each session targets a named weakness, returns to it on a schedule, and is finished when the weakness stops appearing in the mock.

## The idea

### The kit

| Tool | What it is | Use it to |
|---|---|---|
| **Flashcards** | Short question-and-answer cards for modules 45 to 77, one fact or distinction each | Learn and recall limits, defaults, names and distinctions |
| **Review bank** | Scenario questions for modules 45 to 77, with a key and a reason | Practise applying a page to a situation, on a spaced schedule |
| **Mock exam 1 and 2** | Two Architect mock exams of 60 questions each, pages 3 and 4 | Test readiness under time and find weak domains and scenarios |
| **The earlier kits** | The Level 1 and Level 2 flashcards, banks and mocks of modules 11 and 44 | Keep the shared foundations fresh |

The flashcards and the bank are plain data files in `exercises/78-exam-readiness-3/`, `flashcards.json` and `review-bank.json`, and `course/README.md` documents their format. Every card and item names the course page that states the fact, and the Architect domains it serves (A1 to A5), so you can filter by domain, by module or by page. The Level 2 files in `exercises/44-exam-readiness-2/` carry the Developer domains; for the Architect exam they still matter, because Levels 1 and 2 are part of its path.

### Spaced review

Memory fades quickly and recovers with well-timed practice. The schedule is the same as in modules 11 and 44 and is stored in the bank as `intervals_days`: review a new card or question on the day you meet it, then after 1, 3, 7, 14 and 30 days, each counted from the previous review. An item that you answer correctly moves to the next interval, and an item that you miss goes back to the first. Three habits make it work.

- **Answer before you look.** Say the answer aloud, or write it, then turn the card. The effort of recall is what strengthens memory, and a look at the answer is not recall.
- **Keep sessions short and daily.** Twenty minutes every day beats three hours on a Sunday.
- **Read the page behind a miss.** A wrong answer on a card or an item names a page. Reread that section once, then retest it tomorrow.

### Reading a mock result

A percentage hides what you need. Score by domain and by scenario, using the tables on the mock page, and keep an error log with one line for every miss. Write down three things: the question's number and domain, the page that the key names, and the **cause**.

| Cause | What it looks like | What to do |
|---|---|---|
| Did not know | You have no memory of the fact | Reread the page, make a card for the fact, add it to the schedule |
| Misread | You knew it and answered another question | Read the constraint first; slow down on "first", "best" and "most likely" |
| Took a distractor | You knew the topic and chose a plausible mistake | Read the key's reasons for each wrong option, and name the mistake in your log |

The distribution of causes tells you what to change. Mostly "did not know" in one domain is a study problem. Mostly "misread" everywhere is a technique problem, and no more reading of pages will fix it. A cluster of misses in one scenario, with no pattern in the domains, means that the setting itself slows you down: reread its capstone module (70 to 75) and work its example again. The mock exam is not a conversion to the exam's score: **the pass mark is a scaled score of 720 on a scale from 100 to 1,000**, and no percentage is published for it. Use the figures to decide where to work, and not to predict the result.

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
