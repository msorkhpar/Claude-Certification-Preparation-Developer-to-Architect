# Professional mock exam

**Level:** Architect Professional · **Module 94:** Exam readiness 4 · **Page 3 of 3**
**Exams:** P1 to P7 (CCAR-P; the questions follow the Professional blueprint over the content of modules 79 to 93)

**After this page you can** tell whether you are ready for the Professional exam and which domains need more work.

This mock exam covers **the content of Level 4** (modules 79 to 93), as the Professional exam draws on the architect's whole practice, and not one page or one module. It is written in the exam's style: a named scenario of two or three sentences with a constraint, one best answer and three plausible alternatives, each of which is a mistake a practitioner could make. Every question is the course's own, written fresh; no question comes from a live exam, and none repeats a question of a page quiz, a module quiz or another mock exam. The facts behind each answer were read on 2026-10-04 from the pages named on the module pages, so a reader who studies the pages can answer each question from the page it is drawn from. The real exam has multiple-choice and multiple-response items; this mock has single-answer items only.

## How to take it

1. **Choose your conditions.** For a readiness test, take all 63 questions in **120 minutes** with no notes, which is the Professional exam's pace of about 1.9 minutes an item. For learning, take it untimed and read each explanation.
2. **Answer every question.** The guides describe no penalty for a wrong answer.
3. **Mark each answer** and read the explanations only after you finish.
4. **Score by domain,** not only in total, using the table below, and spend your next study session on the weakest domain with the most weight.

There is no official conversion from a mock percentage to a result. The real exam reports the percent correct for each domain, but **pass or fail depends on the total: a scaled score of 720 on a scale from 100 to 1,000**. Treat the mock as a guide to where you stand in each domain and never as a prediction.

## What it covers

The mock has **63 questions**, the number of items on the real exam. They are spread by the Professional blueprint's weights (each domain's weight times 63, rounded so that the total stays 63) and grouped by domain here for easy scoring; the real exam mixes them. The share column is the count divided by 63, set beside the official weight so that you can see the split matches.

| Questions | Domain | Weight | Count | Share of this mock | Drawn from modules |
|---|---|---|---|---|---|
| 1 to 11 | P1 Solution design and architecture | 17% | 11 | 17.5% | 79, 80, 81, 93 |
| 12 to 19 | P2 Claude models, prompting and context | 13% | 8 | 12.7% | 82, 84 |
| 20 to 31 | P3 Integration | 19% | 12 | 19.0% | 85, 86, 87 |
| 32 to 41 | P4 Evaluation, testing and optimisation | 16% | 10 | 15.9% | 88, 89, 93 |
| 42 to 50 | P5 Governance, safety and risk | 14% | 9 | 14.3% | 81, 83, 90 |
| 51 to 59 | P6 Stakeholder communication and lifecycle | 14% | 9 | 14.3% | 79, 89, 90, 91, 93 |
| 60 to 63 | P7 Developer productivity and operational enablement | 7% | 4 | 6.3% | 92 |

For a miss, open the page that the question's explanation names, as (module, page), and reread the passage it quotes. A cluster of misses in one domain points at that domain's modules in the last column. Integration (P3) and solution design (P1) carry the most weight, so they repay the first revision session.

## Mock exam

This mock exam covers the content of Level 4, modules 79 to 93. Choose one answer for each question.

1. Scenario: Lowell Freight's invoice job takes every vendor invoice through the same five steps in the same order: read the PDF, match the order, check the amount, flag exceptions and post. A developer proposes an agent that picks its own steps, because agents are flexible. Which rung does the page support?
   - **a**: An agent, since the flexibility of choosing steps is worth its price on every job
   - **b**: A team of agents, so that each of the five steps has a worker that answers for it
   - **c**: A workflow, since a path fixed in advance is run by code
   - **d**: One call that carries the PDF and a long prompt, since the order of steps is a detail

2. Scenario: Ostrander Bank wants to serve small business owners by chat, a group it has never been able to staff. The sponsor accepts that early answers will be less accurate and asks that adoption be tracked. Which value pillar does the sponsor want served?
   - **a**: Transformation, since it opens up something that could not be done before
   - **b**: Efficiency, since the same work needs less effort for each unit that is handled
   - **c**: Productivity, since the bank's own staff get more done in each working week
   - **d**: Cost, since the work in total is priced lower than the bank pays today

3. Scenario: Brindle Law's drafting task is worth about forty chats, and four agents would each need the whole case file and the other agents' drafts at every step. A designer proposes a coordinator with four subagents. What does the page conclude?
   - **a**: Use the team, since a value of forty chats pays the multiple of fifteen
   - **b**: Use the team, but give every subagent a larger window to hold the shared file
   - **c**: Use a workflow with four fixed calls, since the work is known to have four parts
   - **d**: Keep it to one worker, since parts sharing one context cannot be split

4. Scenario: Fable Retail's reviewer rates three designs. Design A is rejected and costs 30,000 a month. Design B needs a revision and costs 50,000. Design C is approved and costs 70,000. Which does the cheapest-adequate rule pick?
   - **a**: A, because the cheapest design wins whatever its verdict happens to be
   - **b**: B, since price decides only among those that were not turned down
   - **c**: C, because only an approved design may be chosen for a launch
   - **d**: None of them, because a design must be approved before it can be priced

5. Scenario: Marlow Analytics' research subagent returns its whole reading, thousands of lines of page text, to the lead agent, whose window fills before it can write the report. What should the subagent hand back?
   - **a**: The full reading, but in a smaller font so that more of it fits in the window
   - **b**: Nothing until the lead asks, so that the window is never filled by surprise
   - **c**: The reading of the first source only, since the others repeat what it says
   - **d**: The finding with its source, in the shape the contract names

6. Scenario: Kite Pay's agent builds the key for its payment from the current time at every attempt. After a lost response, the retry pays a second time. What is the flaw?
   - **a**: The key is stored in the same step as the payment, so a crash can orphan it
   - **b**: It labels each try, when it should label the intention behind it
   - **c**: The key is too short, so that two different payments can share the same one
   - **d**: The key is made before the first call, so the tool cannot tell attempts apart

7. Scenario: A tool call made by Oaken Labs' agent returns a refusal because the service account lacks a permission. The runner retries it five times with growing pauses. What should the runner do instead?
   - **a**: Retry ten times, since a longer wait is more likely to outlast the fault
   - **b**: Switch to a fresh idempotency key, so the tool treats the call as new
   - **c**: Fail the task at once with a clear reason, since a missing right will not clear itself
   - **d**: Open the breaker of the agent, so every later task skips the tool as well

8. Scenario: Quarry Labs' circuit breaker for one agent is refusing calls. After its cooldown a probe is allowed through, and the probe fails. What is the breaker's state afterwards?
   - **a**: Back to blocking, since the lone trial did not pass
   - **b**: Closed, since the pause has already passed and counts as a recovery
   - **c**: Half-open, with further probe calls allowed one after another
   - **d**: Closed, once the next success resets the count of failures in a row

9. Scenario: Larch Mutual's claims assistant answers policy questions for staff from documents that change often, and the way through a request never varies. A designer proposes agents. Which pattern does the capstone choose?
   - **a**: An augmented call inside a small fixed workflow, since the route is predetermined
   - **b**: A team of agents, since the documents are many and change often
   - **c**: One agent with every tool, since a single agent is easier to audit
   - **d**: One plain call with all the policy text pasted into every prompt

10. Scenario: A claims question at Larch Mutual shares no word with any document that the reader may read, though it does match a contract that only the partnership team may read. What does the chain return?
   - **a**: The contract chunk, since it is the closest match in the whole index
   - **b**: A hold for lack of evidence, since barred material is never a candidate
   - **c**: The best readable chunk, since something readable is always better than nothing
   - **d**: An answer from the model alone, flagged as unsupported by any document

11. Scenario: A review at Larch Mutual of a proposal finds no way back to the previous model if the new one fails, and no other flaw. What does the written review record and conclude?
   - **a**: A medium item in P4, so it goes back for revision
   - **b**: A low item in P6, so it is approved with a note
   - **c**: A high-severity item in P1, so it goes back for revision
   - **d**: A high-severity item in P4, so the design is rejected

12. Scenario: Nettle Support edits the description of one tool every Friday to tune its wording, and the cache hit rate falls after each edit even though the system prompt and the messages never change. What explains it?
   - **a**: The cache lifetime ends on Fridays, so entries expire together and are rebuilt
   - **b**: Tool descriptions count as dynamic text, so they sit after the breakpoint and are never cached
   - **c**: Definitions lead the request, so altering one invalidates everything that follows it
   - **d**: The system prompt is hashed together with the date, so a weekly edit always changes it

13. Scenario: Two workloads at Cobble Insurance both mark a breakpoint after their third block. Their first two blocks are identical, and their third blocks differ by one sentence. How much of the prefix can they share?
   - **a**: The first two blocks, since those are identical in both prompts
   - **b**: Nothing, since the match must hold for each piece up to the cut-off
   - **c**: Most of it, since only one sentence differs between the two prompts
   - **d**: All of it, provided that both prompts name the same model

14. Scenario: Hollin Labs upgrades its assistant to a newer release and copies across a prompt sentence that fixed an over-eager habit on the older one. Nobody has tested the sentence on the release. What does the page advise?
   - **a**: Keep the line, since models in one family behave alike under the same wording
   - **b**: Drop every line that was added to fix a habit, since new models need no such lines
   - **c**: Keep the line and add a second one that repeats it in stronger words
   - **d**: Run the evaluation set on the new model and keep the line only if the results support it

15. Scenario: Pallet Support's smaller model costs half as much per token as its larger one, but it needs about three attempts for each ticket, while the larger one answers once. Which comparison does the page require?
   - **a**: The full bill for one completed item, tries included
   - **b**: The price of a token, since the choice should follow the published rates
   - **c**: The latency of one attempt, since speed decides which model suits a ticket
   - **d**: The size of the context window, since a larger window removes extra attempts

16. Scenario: Isle Travel's legacy caller times out after 2,000 ms. The assistant's slow case, the 95th percentile, is 1,500 ms, and the team adds a safety margin of 600 ms. How should the gateway deliver the answer?
   - **a**: Synchronously, since the median call fits well inside the limit
   - **b**: Synchronously, since the slow case alone is under the timeout
   - **c**: By accept-and-poll, since the tail figure with its buffer is over the caller's limit
   - **d**: By accept-and-poll only if the caller asks for the result to be delayed

17. Scenario: A new project is onboarded to Larch Telecom's gateway with a budget of zero because finance has not yet set one. Its first request arrives with a small estimate. What does admission return?
   - **a**: Allow, since the estimate is small and the spend so far is zero
   - **b**: Warn, since a first request always warns whatever the budget may be
   - **c**: Block, since a ceiling that is not positive leaves no room
   - **d**: Allow once, then block when the spend reaches eighty percent of nothing

18. Scenario: A team at Marsh Retail is close to its budget, and the policy names no cheaper model for the model it uses for refund drafting. The team is not blocked. What does the gateway do with its next request?
   - **a**: Keeps its original pick, since only a listed alternative would replace it
   - **b**: Switches to the cheapest model in the whole table, since the budget is nearly spent
   - **c**: Blocks the request until the budget has been raised by the platform team
   - **d**: Switches to a model of the same tier from another vendor, to stay within the budget

19. Scenario: Tern Fuel's usage report has a row for a model that the price table does not list. The report job must still finish. What should showback do with that row?
   - **a**: Price it at the average of the listed models, so that the total stays near the truth
   - **b**: Skip the row, since an unpriced model cannot add to a team's bill
   - **c**: Price it at zero and add a note, since a note is enough to warn the reader
   - **d**: Reject that line and say which one has no rate

20. Scenario: Quay Retail's assistant must give each branch's refund total for February. The figures sit in a table, and the team has cut the table into text chunks, after which the model adds the numbers by eye and gets them wrong. Which mechanism fits?
   - **a**: An embedding index over the rows, which matches the meaning of each figure
   - **b**: A database query issued through a tool, which sums and filters exactly
   - **c**: A longer chunk size, so that every branch's rows arrive together in one chunk
   - **d**: A keyword index on the word refund, which finds every row that mentions it

21. Scenario: Fallow Utilities' retrieval returns a chunk that reads only "The charge is waived for the first month", and users cannot tell which plan it belongs to. Which repair does the page support?
   - **a**: Return twenty chunks for every question, so that the plan's name turns up in one
   - **b**: Cut the text into smaller windows, so that each chunk holds fewer unrelated words
   - **c**: Ask the model to guess the plan from the rest of the conversation when it answers
   - **d**: Put the document title and section name in front of each piece before indexing

22. Scenario: Tarn Dental's entire knowledge base is about 60,000 tokens, edited a few times a year, and the team plans a vector database with chunking and reranking. What does the page advise first?
   - **a**: Build the vector database, since every knowledge base needs an index to be searched
   - **b**: Put the complete corpus into a cached prompt, since no stage is left to fail
   - **c**: Fine-tune a model on the corpus, since facts held in weights need no retrieval step
   - **d**: Split the corpus across several agents, each of which holds a part of it

23. Scenario: Dunmore Legal switches its retrieval from one embedding model to a newer one. It embeds only the documents that change from now on, and keeps the old embeddings for the rest. Recall on its labelled questions falls at once. What explains it?
   - **a**: The newer model needs a larger chunk size, so the old chunks are now too small
   - **b**: The old vectors are stale copies of their documents, so a re-index would clear them
   - **c**: Vectors from two different makers are not in one space, so the whole index must be rebuilt
   - **d**: The keyword index must be rebuilt first, since the vectors are scored through it

24. Scenario: Ferris Systems' staff each paste the company's access string for the model supplier into their own scripts, and a former employee still has a working copy. The security lead asks for a design that limits the damage of a leaked or abandoned copy. What fits?
   - **a**: A proxy that holds the master secret itself and issues every person a separate token
   - **b**: A rotation of the shared access string every quarter, announced by e-mail
   - **c**: A prompt rule that tells the agent never to reveal the string it was given
   - **d**: A separate supplier account for each team, with one shared string inside each

25. Scenario: Mallow Cloud's MCP server requests every permission up front when a person connects, so that nobody sees a second consent prompt. A reviewer objects. What does the guidance say instead?
   - **a**: Begin with read access and widen it when a privileged operation is first attempted
   - **b**: Ask for every scope, since fewer prompts mean fewer chances for users to refuse
   - **c**: Ask for one broad scope that covers all operations, and log each use of it afterwards
   - **d**: Pass the user's own token on to the downstream service, so no scope is needed

26. Scenario: Pewter Labs merges its 40 tools into 8 tools, each with a switch argument, hoping to cure wrong tool selection. Selection does not improve. Why not?
   - **a**: Eight tools are still more than the model can read in a single request, so the list must shrink
   - **b**: The choice among behaviours is hidden, and nothing was removed
   - **c**: A switch argument is a sampling parameter, which a new model refuses outright
   - **d**: Merged tools lose their descriptions, so selection has no information left to use

27. Scenario: An audit of Quill Support's agent lists a tool that the role needs and the agent holds, and the logs show no use of it in twelve months. What does the audit do with it?
   - **a**: Removes it, since a tool that is never used is only an attack surface
   - **b**: Keeps it silently, since the role needs it and nothing more is to be said
   - **c**: Defers it behind the search tool, since unused tools should always load last
   - **d**: Reviews it, since either the job description or the usage sample is mistaken

28. Scenario: In a trace at Rill Media, one step reports that it served old chunks and another, lower in the same trace, crashed while fetching a file. The orchestrator reports that the research step broke. Where does the analysis place the origin?
   - **a**: At the retrieval step, since stale evidence explains every confident wrong answer
   - **b**: At the orchestrator, since it is the first one in the trace to report an error
   - **c**: At the lowest point of failure, since a breakdown is a fact and staleness only a suspicion
   - **d**: At the model, since it produced the final text that the user saw

29. Scenario: Wick Health's log records hold ids, counts and timings, and no message text. An engineer needs the prompt of a few failing cases to find a fault. Which handling does the page support?
   - **a**: Add every content field to the records, since a fault needs full context
   - **b**: Allow that one content field by name, for a stated purpose and a limited time
   - **c**: Copy the prompts to a private file, so that the shared records stay free of content
   - **d**: Keep the records as they are, and ask users to describe the failing cases

30. Scenario: Sable Cargo changed its model on purpose last Tuesday, and its cost per answer rose by 35 percent against the stored reference. The drift check pages the on-call engineer. What does the page advise?
   - **a**: Keep the old baseline for a year, so that the rise is never forgotten
   - **b**: Raise the tolerance until the page stops, since the change was planned
   - **c**: Turn the cost metric off until the next planned change has been decided
   - **d**: Reset the baseline at the switch that was chosen and treat other moves as findings

31. Scenario: After a release, the share of declined requests at Thorn Care's assistant falls from two percent to nothing, and the dashboard shows green. How should the team read the move?
   - **a**: As a signal to investigate, since such a collapse often points to a failed safeguard
   - **b**: As a clear improvement, since fewer declines mean happier users
   - **c**: As noise, since a metric that small cannot move in a meaningful way
   - **d**: As a billing artefact, since declines are counted in a separate system

32. Scenario: Bexley Bank plans to let a language model mark 1,000 free-form write-ups each night against a rubric. No person has yet compared its marks with theirs. What comes first?
   - **a**: Switch the grader on at full volume, since a model grader is fast and scalable
   - **b**: Replace the grader with exact-match checks, since they are the most reliable kind
   - **c**: Measure how closely it agrees with human judgement on a small sample
   - **d**: Have people grade all 1,000 reports each night, since human grading is the best

33. Scenario: Colt Retail scores its assistant on the five worked examples written into its own prompt and reports 100 percent. A reviewer says the score tells little. What should make up its evaluation set?
   - **a**: The same worked examples, with more of them added until the score stops moving
   - **b**: Cases written by the developers, since they know the product best of all
   - **c**: Real traffic with its proportions kept, plus awkward cases added on purpose and tagged
   - **d**: A single hard benchmark, since a hard test shows the ceiling of the system

34. Scenario: Arden Insurance runs a live test of a new prompt. After 300 trials in each arm the new prompt is two points ahead, and the owner wants to stop and ship while it is in front. What does the page advise?
   - **a**: Gather the sample that was planned, since quitting on a lead gives false wins
   - **b**: Stop now, since a lead after 300 trials in each arm is enough evidence
   - **c**: Run the same 300 trials again, since a repeat will confirm the lead
   - **d**: Add the trials that arrive until the lead grows to five points or more

35. Scenario: Pike Telecom reports a mean latency of 1.4 seconds for its streamed assistant, yet users complain that it is slow. Which figures should the report add?
   - **a**: The mean of the fastest half, since users remember the quick answers
   - **b**: The maximum over a month, since one very slow case shows what users feel
   - **c**: The total tokens per answer, since long answers are the cause of slow ones
   - **d**: The 95th percentile and the time to the first token

36. Scenario: Kern Logistics runs one assistant on Anthropic's own API and on Amazon Bedrock, and plans all its migrations from the dates in Anthropic's table. A model is listed as retiring in four months. What should the team do?
   - **a**: Use the table's date for both, since the model is the same on each platform
   - **b**: Track a second schedule, since the cloud provider publishes a separate calendar
   - **c**: Wait for the provider to send a notice before acting at all
   - **d**: Move only the direct traffic and leave Bedrock alone, since it is out of scope

37. Scenario: Hale Group runs forty programs on three generations of Claude and has no list of which program uses which version when a retirement notice arrives. Which step does the page give first?
   - **a**: Wait for the retirement date to fix the list of models that are still in use
   - **b**: Change the model id in every call site, and see which applications then fail
   - **c**: Ask each team to report the models it believes it uses, from memory
   - **d**: Export usage by API key and model from the Console, then map the keys to applications

38. Scenario: Ivy Retail's gate allows the bill to grow by 25 percent. The new model's total comes out at 25 percent above the old one, and no other check fails. What does the cost check return?
   - **a**: It fails, since any rise is a reason to refuse the change
   - **b**: It passes, since an increase equal to the limit is within the budget
   - **c**: It fails, since a rise at the limit leaves no room for later growth
   - **d**: It passes only if the latency check shows an improvement as well

39. Scenario: Larch Mutual's gate judges a change on twelve cases. It adds correct answers in the status and complaint segments and drops none in the refund segment, which is protected. What does the gate return?
   - **a**: Refuse, since every change must be proved by a live test first
   - **b**: Release only after the protected segment has also gained a case
   - **c**: Release, since nothing was lost and the net result is positive
   - **d**: Refuse, since three added answers cannot offset a possible future loss

40. Scenario: A mistake costs 3 and a person's check costs 1. How is the accuracy computed above which the check no longer pays?
   - **a**: 66 percent, since a third of 100 rounds up to 34 and the rest is 66
   - **b**: 67 percent, since a third rounds to the nearest whole percent
   - **c**: 33 percent, since the check is a third of the cost of a mistake
   - **d**: 0 percent, since a check that costs less than a mistake always pays

41. Scenario: A proposal at Larch Mutual rolls its change out in exactly three stages. The review's rule asks for at least three. What does the review record for the roll-out?
   - **a**: No finding, since a threshold is met at its edge
   - **b**: A medium finding, since three stages leave no spare stage
   - **c**: A low finding, since three is the least that passes
   - **d**: A high finding, since a roll-out needs more than three stages

42. Scenario: Pelham Media's runner saves its results to the store only when the whole run ends. The tenth of twelve tasks crashes the process on every run. What should change?
   - **a**: Store results at the end, but retry the whole run three times
   - **b**: Write each outcome out the moment its work finishes
   - **c**: Store the results of the first nine tasks in the prompt of the tenth
   - **d**: Catch the crash in the runner, so the end of the run is always reached

43. Scenario: Sorrel Cloud serves twelve customers from a single shared pool and separates their data by a filter in its application code. An auditor asks how the data of one customer is kept from another. What does the page say about the design?
   - **a**: It is sound, since a well-tested filter in code is the stronger separation
   - **b**: It is sound, as long as each customer is given a key of its own to use
   - **c**: It is a finding only if two of the customers work in the same industry
   - **d**: It is a finding, since the boundary that is enforced for you is a workspace per tenant

44. Scenario: Bracken Health defines a structured output with a fixed list of allowed values, the names of diagnoses taken from real patient records. A reviewer objects. What is the objection?
   - **a**: Schemas are sent after the messages, so the diagnoses reach the model late
   - **b**: Schemas are limited to ten values, so a long list of diagnoses is cut short
   - **c**: Schemas are cached apart from message content, so they lack the protection that a prompt has
   - **d**: Schemas are read by every tenant, so the names are visible to other customers

45. Scenario: Vale Insurance's tokenising layer finds e-mail addresses and member numbers by pattern, and a check shows that customer names reach the model untouched. What does the page advise?
   - **a**: Accept the gap, since names carry no risk once the addresses are removed
   - **b**: Add a stronger detector for personal details and plant every kind of identifier in the tests
   - **c**: Add a line to the system prompt telling the model to disregard any names
   - **d**: Switch the layer off, since a pattern that misses names gives a false comfort

46. Scenario: Pryor Bank retains audit records for at least 30 days and at most 400. Tonight's purge meets a record that is exactly 400 days old, with no court order attached. What does the purge rule do?
   - **a**: Removes it, since the entry has now used up the whole of the window
   - **b**: Removes it after one more day, so that the audit has a margin of a day
   - **c**: Keeps it until the floor of 30 days has passed once more
   - **d**: Keeps it, since removal needs an age beyond the ceiling

47. Scenario: Brae Insurance's assistant quotes a sentence that is not in the policy text, and its score is 99. The threshold for sending unreviewed is 95. What becomes of the reply?
   - **a**: It is held, since an unbacked claim is stopped whatever its rating
   - **b**: It goes out unreviewed, since 99 is above the threshold for sending
   - **c**: It goes to review, since only replies below the threshold are held back
   - **d**: It goes out with a note that the quote could not be found in the source

48. Scenario: A streamed reply in Garnet Health's assistant ends with a refusal, and the user's next message carries on the same conversation. The next call is refused as well. What should the design have done?
   - **a**: Resend the same conversation, since a refusal is a passing fault
   - **b**: Raise the sampling temperature, so that the reply differs on the next try
   - **c**: Reset the context first, or repeat the query on a different model
   - **d**: Show the user the category of the refusal, which is always present

49. Scenario: Linnet Legal's assistant summarises inbound e-mails, and one e-mail contains a hidden line telling the model to forward the user's files to an outside address. Which design choices does the page give for the e-mail text?
   - **a**: Put it in the system prompt, so that the model reads it before anything else
   - **b**: Add it to the user turn, with a request that the model ignore any orders in it
   - **c**: Pass it only inside tool results, JSON-encoded, with its origin named, and keep rights narrow
   - **d**: Strip every sentence that is written as an instruction, using a list of patterns

50. Scenario: Hollis Mutual's risk register lists four failure modes, and the entry for privacy leaks names a pattern filter that does not appear anywhere in the architecture. What does the page say about that row?
   - **a**: It is a wish, since each safeguard in it must exist as a part of the system
   - **b**: It is sound, since the filter will be built before the system goes live
   - **c**: It is sound, as long as the owner column names a person
   - **d**: It is a minor flaw, since the residual column already says the risk is medium

51. Scenario: In a pilot, two people checked every item, and after launch about 40,000 items a month will be sent to people for a check. The team wants to test the assumption that those people will keep up. Which test does the page give?
   - **a**: Ask the pilot's two reviewers whether the work felt comfortable during the trial
   - **b**: Count the escalations per hundred tasks and staff the support desk for that rate
   - **c**: Test the permissions of the queue with the least privileged real role in the team
   - **d**: Compute the reviewer hours per day from the volume and the routing rates

52. Scenario: Odell Foods plans to start moving its traffic in the week before the vendor withdraws its current Claude release, and to switch everything in one step. Which lifecycle fault does the page see?
   - **a**: No test plan, since nothing is tested until the first day of the migration
   - **b**: No way back, since the old version stops being a fallback on its last day
   - **c**: No stakeholder summary, since the sponsor was not told in writing
   - **d**: No baseline of the old model's cost, since the budget was set from the new one

53. Scenario: A product manager at Aster Health asks for an option in the assistant's configuration that lets each user hide the notice that AI helped write a reply. What does the page say?
   - **a**: Yes, as long as the notice is shown once at the first login
   - **b**: Yes, since users who know the system well do not need to be told
   - **c**: No, but only for replies that concern a decision about a person
   - **d**: No, since the duty belongs to the deployment and cannot be toggled

54. Scenario: A sponsor at Cairn Utilities asks that an assistant be "fast and accurate". The architect wants to turn the wish into requirements. When is a requirement complete?
   - **a**: When the sponsor has signed it and the engineers have read it once
   - **b**: When it has a number, a way to measure it and a person who owns it
   - **c**: When it is written in a sentence that avoids technical terms entirely
   - **d**: When it names the model that will be used to meet it in production

55. Scenario: In the options table of a design record, the entry for the costliest design carries a two-word rationale, "Too much". A reviewer asks for a change. What should that entry say instead?
   - **a**: A single word that names the main cost, since short reasons read faster
   - **b**: A reference to the sponsor's budget, since the budget is what decides
   - **c**: A sentence with a checkable reason, such as top price and slowest reply
   - **d**: The name of the engineer who first rejected the option, for traceability

56. Scenario: The one-page note that Vane Rail's architect wrote for the funder describes the saving in detail and says nothing of what the proposal gives up. A colleague objects. What should the note also contain?
   - **a**: The price paid, the exposure that remains and the decision that is requested
   - **b**: The break-even derivation, so that the funder can check the arithmetic
   - **c**: The list of model settings, so that the funder can confirm the build
   - **d**: A second saving figure for a more optimistic case, to balance the first

57. Scenario: A service level promises availability of at least 99.5 percent, written as 995 per mille. The month's measurement is 990 per mille. How does the report read?
   - **a**: Short of the floor by 5, counted in the unit of the target
   - **b**: Failed, since a bare statement ends the argument more quickly
   - **c**: Met, since 990 is within 1 percent of the promised target
   - **d**: Short by 5 percent, since the two figures differ by five units

58. Scenario: Two months into a pilot, Dray Finance's team sees that its accuracy target for the costly segment will not be met by the agreed deadline. How should it handle the promise?
   - **a**: Keep quiet until the date, since the measurement may still improve
   - **b**: Amend it through the same channel that made it, with a reason, ahead of the due day
   - **c**: Report the overall average instead, since it is above the target
   - **d**: Move the target in the next sprint plan, without telling the sponsor

59. Scenario: Larch Mutual's architect has a finished review: a conclusion, a count per domain and twenty-two itemised flaws, and must present it to the sponsor and the engineers. How should the document be arranged?
   - **a**: The flaws first in domain order, then the conclusion and the count at the end
   - **b**: The count first, then a narrative of the design, then the flaws with no conclusion
   - **c**: The engineers' evidence first, so that the sponsor reads the analysis before the conclusion
   - **d**: Lead with the headline outcome, follow with a tally by area, and end with the ranked details

60. Scenario: Eland Systems fetches its policy from the admin console. The network is down when a developer opens Claude Code in the morning. The security lead wants the tool to refuse to proceed without the policy. Which setting does that?
   - **a**: allowManagedHooksOnly, which stops every unmanaged hook from running
   - **b**: availableModels, which restricts the models a developer may pick
   - **c**: forceRemoteSettingsRefresh, which makes startup fail closed
   - **d**: disableSideloadFlags, which rejects the options that load a plugin

61. Scenario: Fenn Labs limits effort at high in its managed settings. A developer sets a lower ceiling, medium, in a project file, and another team sets max in a user file. Which ceiling is in force?
   - **a**: High, since managed settings outrank every other level for this key
   - **b**: The smallest of them, since the strictest value from any source wins
   - **c**: Max, since the user file is read last and so wins
   - **d**: High, since the project file may not lower an organisation's cap

62. Scenario: A developer at Gorse Bank says that a restriction she set in her local file is not honoured, and the platform team cannot tell why. Which two commands on her machine answer that?
   - **a**: /permissions, which lists the rules, and claude update, which fetches the newest policy
   - **b**: /memory, which shows the loaded files, and claude init, which writes a fresh settings file
   - **c**: /model, which shows the chosen model, and claude logout, which clears the stored session
   - **d**: /status, which shows the setting sources, and claude doctor, which lists what was dropped

63. Scenario: Hart Media's managed policy lists an MCP server in a single record that gives both its name and its address, to be safe. What does the page say about such a record?
   - **a**: It is invalid, since an entry holds exactly one of the three identifying keys
   - **b**: It is valid and stricter, since two keys must both match for the server
   - **c**: It is valid, but only the address is read when both keys are set
   - **d**: It is valid for a deny list, and invalid for an allow list only

<details>
<summary>Answer key</summary>

1. **c**. The path is known, so the code runs it: "If you can write the steps down and their order, the code should run them" (module 79, page 1). *a* is ruled out because "Only then does autonomy earn its price", and the path here needs no model to decide what to do next. *b* is ruled out because "A team that fails the first test is a worse single agent", and these steps are not independent parts. *d* is ruled out because a single call suits a task that is "one transformation with the input in hand", and this job has five steps.
2. **a**. The pillar "Accepts a lower first-pass accuracy and a pilot, and measures adoption", which is what the sponsor asked for (module 79, page 1). *b* is ruled out because efficiency claims "The same work, with less effort per unit", and there was no work done for this group before. *c* is ruled out because productivity means "People get more done", and no staff member is the beneficiary here. *d* is ruled out because the cost pillar claims "Work costs less in total", and the sponsor named a new service.
3. **d**. A team needs independent parts, and these share one context: "cannot be split without losing information" (module 80, page 1). *a* is ruled out because value is only the third condition: "it needs independence, volume and value together". *b* is ruled out because the first test fails whatever the window size: "A team that fails the first test is a worse single agent". *c* is ruled out because a workflow is for a known path: "Is the path known? Then it is a workflow, and it costs one chat per step", and drafting that depends on other drafts has no fixed order.
4. **b**. Price comes last and among the survivors: "Price enters last, and only among the designs that were not rejected" (module 80, page 2). *a* is ruled out because "a cheaper design with a missing feedback loop is not cheaper, only unfinished". *c* is ruled out because the rule picks "the cheapest of those", and a design that needs revision is not a rejected one. *d* is ruled out because "when every design is rejected there is no winner", and here two designs survive.
5. **d**. A subagent hands back a digest: "Return a digest, not a transcript" (module 80, page 2). *a* is ruled out because the transcript is the problem and not its size: "The subagent's reading is thrown away when it finishes". *b* is ruled out because the contract fixes the output: "What goes back is the finding, with its source, in the contract's shape". *c* is ruled out because the page asks for the objective and boundaries so "that two subagents do not research the same thing or leave a gap between them".
6. **b**. The key must name the intention: "A new key for each attempt turns the retry into a new refund" (module 81, page 1). *a* is ruled out because recording both together is the cure: "Record the key together with the effect". *c* is ruled out because the flaw is that the value changes between attempts, and "The key names the intention, not the attempt". *d* is ruled out because making it once is correct: "The key is made once, before the first call, from what identifies the task".
7. **c**. A refusal is a fatal failure, and the runner "never retries the second" kind (module 81, page 1). *a* is ruled out because "An unlimited retry is a way to spend money while nothing changes", and ten is the same mistake. *b* is ruled out because "The key names the intention, not the attempt", and a missing permission is not a problem of recognising repeats. *d* is ruled out because the breaker counts "consecutive failures for one agent", and a missing permission is a fact about the account.
8. **a**. The table gives the rule: "A success closes it; a failure opens it again" (module 81, page 1). *b* is ruled out because a breaker closes only on a success: "A success closes it". *c* is ruled out because in the half-open state "One probe call goes through", and a failure sends the breaker back to open. *d* is ruled out because a success is needed to close it, and the page says "A success resets the count, so scattered failures do not open it", which concerns the closed state.
9. **a**. The capstone sets the rung: "an augmented call with a small fixed workflow, not an agent, because the path is known" (module 93, page 1). *b* is ruled out because "A team of agents would cost about 15 times a chat, and the value does not pay it". *c* is ruled out because "The pattern is the lowest rung that meets the need", and an agent with every tool is a higher one. *d* is ruled out because the design needs "Retrieve by the reader's rights", which a pasted text does not give.
10. **b**. The chain removes first and ranks after: "a question that shares nothing with any readable document gets no evidence" (module 93, page 1). *a* is ruled out because "Documents the reader may not read are removed first, then the rest are ranked". *c* is ruled out because "a question that shares nothing with any readable document gets no evidence", so no chunk is returned. *d* is ruled out because "an answer whose quote is missing is held as unsupported", and the chain never sends it out with a flag.
11. **d**. A high finding rejects: "The design is rejected until it is fixed" (module 93, page 2). *a* is ruled out because a medium flaw "weakens a design without breaking it", and a missing way back is a flaw that can cause harm that cannot be undone. *b* is ruled out because the low bullet reads "It is recorded and does not hold the design back", and a missing rollback is not low. *c* is ruled out because "reject if any finding is high", so a high finding is not a revision.
12. **c**. The documented order is tools, system, messages: "a change to a tool definition invalidates the system prompt and the messages that follow it" (module 82, page 1). *a* is ruled out because the page gives the cause as an edit inside the prefix: "The edit is inside the prefix, so everything from there on is a new entry". *b* is ruled out because the documentation fixes the order of the whole request "as tools, then system, then messages", so tools lie in front of the breakpoint. *d* is ruled out because the page names the tool edit as the cause, and says the tool list should be treated "as a stable asset".
13. **b**. Reuse is all or nothing: "nothing is reusable and the answer is zero" (module 82, page 2). *a* is ruled out because "If any block differs, or either prompt has no breakpoint, nothing is reusable". *c* is ruled out because the page says "If any block differs, or either prompt has no breakpoint, nothing is reusable", however small the difference. *d* is ruled out because the check is on the blocks: "every block up to it must match in name and text".
14. **d**. Technique is measured per model: "treat it as measured on that model and re-check it against your own evals" (module 82, page 2). *a* is ruled out because the trap names this belief: "models in one family behave alike" is the tempting mistake, and the exam rejects it. *b* is ruled out because the page says an instruction "can be unnecessary or harmful on another" model, which is a reason to test it and not to assume it away. *c* is ruled out because a stronger repeat is another untested change, and the design answer is "an evaluation set that runs on every candidate model and every prompt change".
15. **a**. The comparison is per task: "a weaker model that needs three attempts or a longer prompt can cost more than a stronger one that answers once" (module 82, page 2). *b* is ruled out because "the cheapest model per token is not always the cheapest per task". *c* is ruled out because latency is a limit that rules models out and "Price comes last because it is the only fact that cannot make a model wrong". *d* is ruled out because the page gives no such link, and the tier is "found by evaluating the task, not by reading a model's name".
16. **c**. The gateway "compares the slow case (the 95th-percentile time plus a safety margin) with the caller's timeout" (module 84, page 2); 2,100 ms is over 2,000 ms. *a* is ruled out because the comparison uses the tail and not the median: "the 95th-percentile time plus a safety margin". *b* is ruled out because the comparison is made with "the 95th-percentile time plus a safety margin", and the slow case alone is not the figure. *d* is ruled out because the choice is the gateway's: "If it does not, the gateway uses accept-and-poll".
17. **c**. The edge is exact: "A budget that is not positive blocks" (module 84, page 2). *a* is ruled out because admission is decided against the budget: "A request that would bring the spend to more than the budget blocks". *b* is ruled out because a warning belongs to the case "Reaching 80 percent warns", and a zero budget is blocked before that. *d* is ruled out because the rule has no free first request, and "A budget that is not positive blocks".
18. **a**. Degrading needs a named entry: "when it has no cheaper entry the model is kept" (module 84, page 2). *b* is ruled out because the replacement is "a cheaper one that the policy names", not a free choice of the cheapest. *c* is ruled out because "A blocked team gets no model", and the team here is near its budget and not blocked: "a team is warned, then degraded, then stopped, in that order". *d* is ruled out because the route table is the gateway's: "Route each task to a model by one table".
19. **d**. A gateway does not guess: "A row whose model has no price is refused, because a gateway never guesses a price" (module 84, page 2). *a* is ruled out because "a gateway never guesses a price", and an average is a guess. *b* is ruled out because "A row whose model has no price is refused", which names the problem, and a skipped row understates the bill. *c* is ruled out because "Every token kind has its own price", and a price of zero makes the usage free in the report.
20. **b**. When the answer lives in a table, the page names the mechanism: "A structured query run by a tool", which is "Exact, and able to total or filter" (module 85, page 1). *a* is ruled out because an embedding index "can miss crucial exact matches" and adds no figures, so exact totals are not its job. *c* is ruled out because longer chunks keep the original fault: "The table is cut into chunks and the model adds numbers by eye". *d* is ruled out because a keyword index is the one that "matches the exact string" of an identifier, and it still leaves the model to add the rows.
21. **d**. Structure supplies the context a chunk lacks: "The pipeline already knows the document title and the section name" (module 85, page 1). *a* is ruled out because "Returning more chunks per question does not help either". *b* is ruled out because "A smaller window cuts in more places and separates more sentences from their neighbours". *c* is ruled out because "Retrieval hands the model a chunk without its document", and a guess gives no evidence to cite.
22. **b**. A corpus under about 200,000 tokens needs no index: "No stage can fail and nothing can be missed" (module 85, page 1). *a* is ruled out because "A pipeline adds stages that each can fail", and a corpus this small needs none of them. *c* is ruled out because the page says "The Claude API does not currently offer fine-tuning", and weights are weak when facts change and a source must be shown. *d* is ruled out because the corpus fits one window, and a team of agents is justified only when "the reading that has to be done is larger than one agent can hold".
23. **c**. The page gives the rule: "Vectors made by two models are not in one space, so the index must be rebuilt as a whole" (module 85, page 2). *a* is ruled out because "Size then follows from the unit", the structure of the data, and not from the embedding model that scores the chunks. *b* is ruled out because "An index is stale when a chunk no longer matches its source", and these old vectors still match their documents. *d* is ruled out because the fault is that "queries embedded by the new model will be compared with chunks embedded by the old one".
24. **a**. The documentation lists the gain of a gateway: "the provider key stays server-side; developers hold gateway credentials instead" (module 86, page 1), so offboarding revokes one credential. *b* is ruled out because the string stays in every pair of hands between rotations, whereas with a gateway "a leaked developer credential is not a leaked provider key". *c* is ruled out because "a line in the system prompt is a request to the model and not a control". *d* is ruled out because a shared string in each team keeps the same leak, and the page puts the provider credential in one place, "shared by all forwarded traffic".
25. **a**. The guidance is progressive: "Implement a progressive, least-privilege scope model", starting from read operations (module 86, page 1). *b* is ruled out because "A token with every scope granted up front makes a stolen token worth the whole system". *c* is ruled out because "Poor scope design increases token compromise impact, elevates user friction, and obscures audit trails". *d* is ruled out because "Token passthrough is explicitly forbidden in the authorization specification".
26. **b**. Merging "hides the behaviours from the model's own choice and leaves the number of capabilities the same" (module 86, page 2). *a* is ruled out because the page puts the trouble where "once you exceed 30–50 available tools", and eight is far below that. *c* is ruled out because the sampling parameters are `temperature`, `top_p` and `top_k`, for which "a non-default value returns the error", and a switch argument is none of them. *d* is ruled out because the page names the cause as one that "hides the behaviours from the model's own choice", not lost descriptions.
27. **d**. The audit lists dormant tools: "They are not removed, because the role needs them; they are reviewed" (module 86, page 2). *a* is ruled out because the tools to remove are "the ones held and not needed", and this one is needed. *b* is ruled out because the page asks for a review, "since either the role description or the usage sample is wrong". *c* is ruled out because deferral is a loading choice for the long tail, and the page says to keep the "most frequently used tools non-deferred", which leaves the audit question open.
28. **c**. The page puts facts before suspicions: "A failed span is a fact, and a stale retrieval is a suspicion" (module 87, page 1). *a* is ruled out because "A system that blames the retrieval whenever it can will send engineers to the index while a tool is broken". *b* is ruled out because "the first span to turn red is not the origin". *d* is ruled out because the model span reported no error, and the origin is "the one that no other failing span has as its parent".
29. **b**. Content is opened narrowly: "allow one by name only for a stated purpose and a limited time" (module 87, page 1). *a* is ruled out because "Telemetry is a copy of your users' data in a place with a different audience". *c* is ruled out because the records "Keep ids, counts, timings, names of models and tools, and statuses", and a private copy of the prompts is an ungoverned store of content. *d* is ruled out because the page says "Drop the content fields, and allow one by name only for a stated purpose and a limited time", so a stated purpose may open one.
30. **d**. The page gives the rule: "reset a baseline deliberately, at a change you decided on, and to treat a move you did not plan as a finding" (module 87, page 2). *a* is ruled out because "The baseline is not permanent", and a planned change moves the numbers on purpose. *b* is ruled out because "Raising the threshold until the pages stop hides the real incident with the noise". *c* is ruled out because the rule is to re-baseline, and the migration guide says "Recount tokens and re-baseline cost".
31. **a**. Drift is read in both directions: "A refusal rate that drops to zero can mean a guardrail stopped working" (module 87, page 2). *b* is ruled out because "a fall is not always an improvement", and both directions matter. *c* is ruled out because the check will "flag every move beyond a tolerance, in either direction", and a fall from two percent to nothing is the largest relative move possible. *d* is ruled out because "A refusal arrives with a normal status", so a refusal metric belongs to the model layer, not to billing.
32. **c**. The documentation says of model grading: "Test to ensure reliability first then scale", and the page adds "Check that agreement before you trust the grader" (module 88, page 1). *a* is ruled out because the documentation says "Test to ensure reliability first then scale", and full volume is the scale step. *b* is ruled out because exact match "rejects a correct report that is worded differently". *d* is ruled out because human grading is "slow and expensive. Avoid if possible", and "Human review of every output does not scale".
33. **c**. The set must mirror use: "Design evals that mirror your real-world task distribution" (module 88, page 1). *a* is ruled out because the examples in a prompt "were picked to teach a format, so a score on them says how well the prompt repeats itself". *b* is ruled out because the set "is built from the traffic and not from the developers' imagination". *d* is ruled out because a single hard case set is not the distribution of real use, and the page asks for "an evaluation set that looks like the traffic".
34. **a**. The page names the rule: "Do not stop by who is ahead" (module 88, page 2). *b* is ruled out because "Stopping or extending a test according to who is ahead produces false wins". *c* is ruled out because "Running the same cases again repeats the same sample and adds no information". *d* is ruled out because the sample is fixed first: "Decide the metric and the sample before you look".
35. **d**. The page says "A mean hides the tail", and "For streamed answers the time to the first token is the number users feel" (module 88, page 2). *a* is ruled out because "A mean hides the tail", and a mean of the fastest half hides it even further. *b* is ruled out because the maximum is one case, whereas "the slowest five in a hundred requests are the ones users complain about". *c* is ruled out because length is something to trim later: "Cut tokens where the evals say it is safe", and it does not tell the report what users feel.
36. **b**. The page says: "Partner-operated platforms (Amazon Bedrock and Google Cloud) set their own retirement schedules" (module 89, page 1). *a* is ruled out because "A multi-platform estate keeps one calendar per platform". *c* is ruled out because "There is notice, and it is finite", and the platform's own schedule is to be tracked, not awaited. *d* is ruled out because the provider's schedule applies to that traffic, "so a model's lifecycle status and dates can differ".
37. **d**. The page describes the audit: "usage broken down by API key and model" from the Usage page (module 89, page 1). *a* is ruled out because "There is notice, and it is finite", and waiting leaves no time to find the applications. *b* is ruled out because a blind change of ids ships the failures the audit is meant to find: "You cannot migrate what you cannot find". *c* is ruled out because a recollection is not an audit, and the export "finds the keys; the keys point to the applications".
38. **b**. The edge is stated: "A rise exactly at the budget passes and one point over it is refused" (module 89, page 2). *a* is ruled out because the check refuses only a rise beyond the budget, and cost is judged "against a budget set before the run". *c* is ruled out because the check is made "against a budget set before the run", and the rule keeps no reserve beyond it. *d* is ruled out because the five checks are each judged on their own, and "every failing check adds a reason".
39. **c**. The example prints the result: "the same change is a go with no loss and three gains" (module 93, page 1). *a* is ruled out because "an A/B test comes after it", and the gate's two keys are no protected loss and no net loss. *b* is ruled out because the rule is "no protected segment may lose a case", and it asks for no gain there. *d* is ruled out because the gate judges the cases it has: "the losses must not outnumber the gains", and a possible future loss is not one of them.
40. **a**. The page works the case: "a third of a hundred is 33.3, which rounds up to 34" (module 93, page 2). *b* is ruled out because the percent is taken "with that percent rounded up", which gives 66 and not 67. *c* is ruled out because 33 is the rounded-down share, and the rule is "100 minus the review cost as a percent of the error cost, with that percent rounded up". *d* is ruled out because zero comes only when "A check that costs as much as an error, or more, needs no accuracy", and here the check is cheaper.
41. **a**. The page treats the edge as passing: "three stages pass" and "One step past any of them is a finding" (module 93, page 2). *b* is ruled out because the rule fires only below three: "fewer than three roll-out stages". *c* is ruled out because the review must "treat the edge as the design's friend", and a pass at the edge carries no note. *d* is ruled out because the `big-bang-rollout` rule is medium and applies to "fewer than three roll-out stages", not to three.
42. **b**. Results are written as work is done: "Write on success, immediately" (module 81, page 2). *a* is ruled out because a restart is the expensive path: "it discards the work that finished, which is the expensive part". *c* is ruled out because the checkpoint is a store of finished results, not context for the next task: "A checkpoint is a store of finished results". *d* is ruled out because "swallowing it hides the defect and reports unverified results".
43. **d**. The separation is the platform's: "one workspace per tenant, with its own keys, limits and spend" (module 83, page 1). *a* is ruled out because "code-only separation fails the first time a filter is forgotten". *b* is ruled out because the workspace is the unit: "A single shared workspace for all tenants is a finding for a multi-tenant requirement". *c* is ruled out because the finding follows from the shape of the deployment, since "the separation then rests on application code alone".
44. **c**. The page warns that schemas "are compiled into grammars that are cached separately from message content, so they do not get the protections of the prompt" (module 83, page 1). *a* is ruled out because the objection concerns where the schema is stored, and the page says only "put no health data in a schema". *b* is ruled out because the page names no such limit, and the rule it gives is "put no health data in a schema". *d* is ruled out because tenants are separated by workspace, "one workspace per tenant", and the page does not say that a schema crosses tenants.
45. **b**. A layer needs testing against what it claims to remove: "tests that plant identifiers of every kind and check that none reaches the request" (module 83, page 2). *a* is ruled out because "A person's name has no fixed shape, so a pattern layer leaves it in", and the page treats that as a gap to close. *c* is ruled out because "the data has already crossed the boundary, nothing proves the instruction was followed". *d* is ruled out because the layer still removes the shapes it finds, and the page says it "cannot leak what it never had".
46. **d**. An entry goes only past the limit: "an entry is removed only when it is **more than** the ceiling old" (module 83, page 2). *a* is ruled out because "the configuration check treats a retention equal to a limit as acceptable". *b* is ruled out because the rule has no margin: the page says "removed only when it is more than the ceiling old", and 365 is not more than 365. *c* is ruled out because the floor is the shortest time to keep records, and "deleting sooner is a breach", and it plays no part in a purge at the ceiling.
47. **a**. The route checks the source before the score: "An answer whose quote is not in the source is held (`hold: unsupported`) even at confidence 99" (module 90, page 1). *b* is ruled out because "confidence is not evidence", and a model can be certain and wrong. *c* is ruled out because review is for answers the source supports: "a low-consequence action goes out unreviewed only at a confidence of at least 95, and below it goes to review". *d* is ruled out because the example holds the answer: "An answer whose quote is not in the source is held", and a note would still send the unsupported text out.
48. **c**. The design treats a refusal as an outcome: "the context is reset (the turn that caused it removed or rephrased) before the conversation continues, or the request is retried on a different model" (module 90, page 1). *a* is ruled out because the page says "the context is reset (the turn that caused it removed or rephrased) before the conversation continues", not resent. *b* is ruled out because the page names two recoveries, with "or the request is retried on a different model" as the second, and sampling is neither. *d* is ruled out because "the stop_details fields can be null", so the user-facing message comes from the application.
49. **c**. The injection control has several parts: "untrusted content only in tool results, JSON-encoded; screen tool output; least privilege" (module 90, page 1). *a* is ruled out because untrusted content belongs "only in tool results", and not among the instructions. *b* is ruled out because "a line in a prompt is a request, and a control is a step the model cannot skip". *d* is ruled out because pattern lists belong to the input layer, and the page says "A screen with a small model reduces risk and does not remove it".
50. **a**. The register's test is mechanical: "a row with a control that does not exist is a wish" (module 90, page 2). *b* is ruled out because "every control in the register is a control in the design". *c* is ruled out because "A row with no owner has nobody to call; a row with a control that does not exist is a wish", and the two faults are separate. *d* is ruled out because "the residual column says what is left" once a control works, and a control that does not exist leaves the whole risk.
51. **d**. The page ties the assumption to a computation: "Compute reviewer hours per day from volume and routing rates" (module 79, page 2). *a* is ruled out because the pilot's own conditions are the problem: "The pilot's conditions were friendly in ways nobody wrote down". *b* is ruled out because that test belongs to another row: "Count escalations per hundred tasks and staff for the rate" answers whether people covered edge cases. *c* is ruled out because "Test permissions with the least privileged real role" is the test for the access assumption, not for reviewers.
52. **b**. The page names two deadlines: "the date by which the new model must be ready, and the date after which the old one cannot be the fallback" (module 89, page 2). *a* is ruled out because the stem gives no sign of missing tests, and the fault named is timing: "a roll-out that starts the week before retirement has no rollback at all". *c* is ruled out because the stakeholder message is a separate duty: "A recommended replacement is a starting point, not a decision". *d* is ruled out because the page says to "Recount tokens and re-baseline cost", which is a step of the migration, and not what the retirement week removes.
53. **d**. The transparency duty is fixed: "make that part of the deployment and not a setting someone may switch off" (module 90, page 2). *a* is ruled out because the duty is to "Tell every person who receives output that AI helped produce it", not to tell once per user. *b* is ruled out because the duty applies to "every person who receives output", whatever a user already knows. *c* is ruled out because the duty reaches every reply, and "the Level 1 module on policy covers disclosure for decisions about people" only as a further case.
54. **b**. The page says: "A requirement is complete when it has a number, a way to measure it and a person who owns it" (module 91, page 1). *a* is ruled out because "Fast", "accurate" and "safe" are not answers, "because nobody can fail them", and a signature adds no number. *c* is ruled out because plain language is for the sponsor's summary, and a requirement missing a number goes "back to the stakeholder as a question". *d* is ruled out because naming a model answers none of the three: "a requirement missing one of the three goes back to the stakeholder as a question".
55. **c**. The page asks for argument: "A rejected option has a reason in a sentence" (module 91, page 1). *a* is ruled out because a rejected option "has a reason in a sentence", and one word is not a sentence. *b* is ruled out because "An option that misses a service level is not a candidate, however cheap", and the reason is a checkable fact about the option, not a budget name. *d* is ruled out because the reader's question is "why not the simple thing", and a name does not answer it.
56. **a**. The summary follows an order: "what improves, what it costs, what is at risk and what is asked" (module 91, page 1). *b* is ruled out because the derivation belongs to the engineer's layer, and "The summary is first because the sponsor stops reading early". *c* is ruled out because the sponsor decides and the engineer builds, and settings are "the numbers that produced it" for the engineer. *d* is ruled out because "Leaving out the downside is the common failure", and a second saving adds to the upside only.
57. **a**. A miss is reported with its size: "A miss says by how much" (module 91, page 2). *b* is ruled out because a bare failure "starts an argument that a number would have ended". *c* is ruled out because "A ceiling is met below it and a floor is met above it", and 990 is below the floor. *d* is ruled out because the unit is the target's own: "The target has a number and a unit and a direction".
58. **b**. The page gives the habit: "change a promise through the same channel that made it, with a reason, before the date it matters" (module 91, page 2). *a* is ruled out because the page says "Say early what the design cannot promise". *c* is ruled out because "Promise what you measure and measure what you promise", and the average hides the costly segment. *d* is ruled out because a change must go "through the same channel that made it", and the sponsor was promised the number.
59. **d**. The review is written for two readers: "the verdict first, then the scorecard, then the findings in their order, each with the evidence from the design and the fix" (module 93, page 2). *a* is ruled out because "A list ordered only by domain buries the high findings in the middle", and the sponsor stops early. *b* is ruled out because a review without a conclusion leaves the sponsor nothing to decide: "The sponsor stops at the verdict and the scorecard". *c* is ruled out because "the engineers read the findings", and the sponsor does not read the analysis.
60. **c**. The page says the default is to carry on: "If the fetch fails, Claude Code continues without the remote policy and warns, unless `forceRemoteSettingsRefresh` is set, which makes startup fail closed" (module 92, page 1). *a* is ruled out because that key means "only managed hooks run", and says nothing of a start without the policy. *b* is ruled out because it is a list of models: "The **lock** is `availableModels`, a list". *d* is ruled out because that key rejects the flags "that sideload plugins, agents and MCP servers", and has nothing to do with fetching.
61. **b**. The page gives the rule: "when several levels set a cap, the lowest applies, so a developer may lower the organisation's cap and nobody can raise it" (module 92, page 1). *a* is ruled out because the key is one of the two that go the other way, where "a stricter value is always welcome". *c* is ruled out because "nobody can raise it", and a higher cap in a lower file has no effect. *d* is ruled out because "a developer may lower the organisation's cap".
62. **d**. The page gives the lookup: "`/status` shows the `Setting sources` line, which names the managed source Claude Code selected, and `claude doctor` lists what it dropped" (module 92, page 1). *a* is ruled out because the answer is "a lookup and not a guess", and a command that fetches a policy does not say what was dropped. *b* is ruled out because a fresh settings file would hide the evidence, and the answer is "a lookup and not a guess". *c* is ruled out because the lookup needs the line that "names the managed source Claude Code selected", and a model choice or a sign-out shows none.
63. **a**. The page states the shape: "Each entry has exactly one of `serverName`, `serverCommand` or `serverUrl`: an entry with two keys is invalid" (module 92, page 2). *b* is ruled out because "an entry with two keys is invalid", and it is not stricter. *c* is ruled out because the page gives no preference between keys, and states that "Each entry has exactly one of". *d* is ruled out because the rule is about the entry itself, and "A denied server wins over an allowed one" is a different rule about two lists.

</details>
