# Professional mock exam 1

**Level:** Architect Professional · **Module 94:** Exam readiness 4 · **Page 3 of 4**
**Exams:** P1 to P7 (CCAR-P; the questions follow the Professional blueprint over the content of modules 79 to 93)

**After this page you can** tell whether you are ready for the Professional exam and which domains need more work.

This mock exam covers **the content of Level 4** (modules 79 to 93), as the Professional exam draws on the architect's whole practice, and not one page or one module. Level 4 has two mock exams of 63 questions each, the Professional exam's number of items; this is the first, and no question of one repeats a question of the other. It is written in the exam's style: a named scenario of two or three sentences with a constraint, one best answer and three plausible alternatives (or two right answers among five options, where the question says Select two), each of which is a mistake a practitioner could make. Every question is the course's own, written fresh; no question comes from a live exam, and none repeats a question of a page quiz, a module quiz or another mock exam. The facts behind each answer were read on 2026-10-04 from the pages named on the module pages, so a reader who studies the pages can answer each question from the page it is drawn from. The real exam has multiple-choice and multiple-response items; so does this mock: about one item in six ends with (Select two.) and is right only when both keyed options are chosen.

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

This mock exam covers the content of Level 4, modules 79 to 93. Choose one answer for each question, or the number the question states.

1. Scenario: Lowell Freight's invoice job takes every vendor invoice through the same five steps in the same order: read the PDF, match the order, check the amount, flag exceptions and post. A developer proposes an agent that picks its own steps, because agents are flexible. Which two statements does the page support? (Select two.)
   - **a**: An agent suits it, since flexibility is worth its price on every job
   - **b**: Code that fixes the sequence is cheaper, testable and auditable
   - **c**: A single call with a long prompt suits it, since the sequence is a detail
   - **d**: A team of agents suits it, with one worker answering for each stage
   - **e**: A workflow suits it, since the path is known in advance

2. Scenario: Ostrander Bank wants to serve small business owners by chat, a group it has never been able to staff. The sponsor accepts that early answers will be less accurate and asks that adoption be tracked. Which value pillar does the sponsor want served?
   - **a**: Transformation, since it opens up something that could not be done before
   - **b**: Efficiency, since the same work needs less effort for each unit that is handled
   - **c**: Productivity, since the bank's own staff get more done in each working week
   - **d**: Cost, since the work in total is priced lower than the bank pays today

3. Scenario: Brindle Law's drafting task is worth about forty chats, and four agents would each need the whole case file and the other agents' drafts at every step. A designer proposes a coordinator with four subagents. What does the page conclude?
   - **a**: Use the team
   - **b**: Use the team, but give every subagent a larger window to hold the shared file
   - **c**: Use a workflow with four fixed calls
   - **d**: Keep it to one worker

4. Scenario: Fable Retail's reviewer rates three designs. Design A is rejected and costs 30,000 a month. Design B needs a revision and costs 50,000. Design C is approved and costs 70,000. Which does the cheapest-adequate rule pick?
   - **a**: A, because the cheapest design wins whatever its verdict happens to be
   - **b**: B, since price decides only among those that were not turned down
   - **c**: C, because only an approved design may be chosen for a launch
   - **d**: None of them, because a design must be approved before it can be priced

5. Scenario: A coordinator at Marlow Analytics sends four subagents to read hundreds of pages each, and its window is full before it writes the report. The subagents must still run in parallel. Which contract change fixes the design?
   - **a**: Each subagent reads only the first source
   - **b**: Each subagent forwards the pages it read, trimmed to fit
   - **c**: Each subagent waits for the coordinator to ask, so nothing arrives unplanned
   - **d**: Each worker hands back its conclusion with a citation, in a fixed shape

6. Scenario: Kite Pay's payment tool honours an idempotency key, but the agent builds that key from the current time on each attempt, so a retry after a lost response pays twice. Which change fixes the design?
   - **a**: Make a fresh value for each attempt and log every one of them
   - **b**: Derive the label once from the order and the action, and reuse it for every repeat
   - **c**: Lengthen the key, so that two different payments cannot share one
   - **d**: Save the key before the call and the payment after it, in two steps

7. Scenario: A runner at Oaken Labs treats every tool error alike, with five attempts and growing pauses, and a call refused for lack of permission uses all five. Which rule belongs in its design?
   - **a**: Give every attempt a fresh idempotency key, so the tool sees each as new
   - **b**: Open the breaker for the agent, so that every later task skips the tool
   - **c**: Classify faults: repeat passing ones up to a cap, fail permanent ones at once
   - **d**: Raise the attempts to ten with longer pauses, so that the fault outlasts the wait

8. Scenario: A breaker around Quarry Labs' search agent opens during an outage. After its cooldown one probe call goes through and fails. Which behaviour keeps spending low for the rest of the outage?
   - **a**: It reverts to refusing requests at once until a further pause has passed
   - **b**: It resets its count of failures, so that the next three calls are allowed
   - **c**: It closes and lets all traffic back through again
   - **d**: It stays half-open and lets further probes through one after another

9. Scenario: Larch Mutual's claims assistant answers policy questions for staff from documents that change often, and the way through a request never varies. A designer proposes agents. Which two statements does the capstone support? (Select two.)
   - **a**: One agent holding every tool suits it, since a single agent is simpler to audit
   - **b**: A coordinated crew would cost about 15 times a chat, and the value does not pay it
   - **c**: One plain call with all the policy text pasted in suits it, since the route is simple
   - **d**: A team of agents suits it, since the documents are many and change often
   - **e**: A small fixed workflow around a retrieval-backed call suits the known path

10. Scenario: A claims question at Larch Mutual shares no word with any document that the reader may read, though it does match a contract that only the partnership team may read. What does the chain return?
   - **a**: The contract chunk
   - **b**: A hold for lack of evidence
   - **c**: The best readable chunk
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

14. Scenario: Hollin Labs upgrades its assistant to a newer release and copies across a prompt sentence that fixed an over-eager habit on the older one. Nobody has tested the sentence on the release. Which two steps does the page support? (Select two.)
   - **a**: Keep the line, since models in one family behave alike under the same wording
   - **b**: Treat the technique as measured on the old model until re-checked against your own evals
   - **c**: Keep the line and add a second one that repeats it in stronger words
   - **d**: Drop every line that fixed a habit, since new models need no such lines
   - **e**: Run the evaluation set on the new model and see whether the results back the line

15. Scenario: Pallet Support's smaller model costs half as much per token as its larger one, but it needs about three attempts for each ticket, while the larger one answers once. Which comparison does the page require?
   - **a**: The full bill for one completed item, tries included
   - **b**: The price of a token for each of the two models
   - **c**: The latency of one attempt on each model
   - **d**: The size of the context window of each model

16. Scenario: Isle Travel's legacy caller times out after 2,000 ms. The assistant's slow case, the 95th percentile, is 1,500 ms, and the team adds a safety margin of 600 ms. How should the gateway deliver the answer?
   - **a**: Synchronously, since the median call fits well inside the limit
   - **b**: Synchronously, since the slow case alone is under the timeout
   - **c**: By accept-and-poll, since the tail figure with its buffer is over the caller's limit
   - **d**: By accept-and-poll only if the caller asks for the result to be delayed, which it rarely does

17. Scenario: A new project is onboarded to Larch Telecom's gateway with a budget of zero because finance has not yet set one. Its first request arrives with a small estimate. What does admission return?
   - **a**: Allow, since the estimate is small and the spend so far is zero
   - **b**: Warn, since a first request always warns whatever the budget may be
   - **c**: Block, since a ceiling that is not positive leaves no room
   - **d**: Allow once, then block when the spend reaches eighty percent of nothing

18. Scenario: A team at Marsh Retail is close to its budget, and the policy names no cheaper model for the model it uses for refund drafting. The team is not blocked. What does the gateway do with its next request?
   - **a**: Keeps its original pick
   - **b**: Switches to the cheapest model in the whole table
   - **c**: Blocks the request until the budget has been raised by the platform team
   - **d**: Switches to a model of the same tier from another vendor, to stay within the budget

19. Scenario: Tern Fuel's usage report has a row for a model that the price table does not list. The report job must still finish. What should showback do with that row?
   - **a**: Price it at the average of the listed models, so that the total stays near the truth
   - **b**: Skip the row
   - **c**: Price it at zero and add a note
   - **d**: Reject that line and say which one has no rate

20. Scenario: Quay Retail's assistant must give each branch's refund total for February. The figures sit in a table, and the team has cut the table into text chunks, after which the model adds the numbers by eye and gets them wrong. Which two statements does the page support? (Select two.)
   - **a**: A longer chunk size brings every branch's rows together and so fixes the sums
   - **b**: An embedding index over the rows matches the meaning of each figure and so the sums
   - **c**: Slicing the rows into fragments leaves the sums to guesswork
   - **d**: A keyword index on the word refund finds every row and so totals them
   - **e**: A database query issued through a tool sums and filters exactly

21. Scenario: Fallow Utilities' retrieval returns a chunk that reads only "The charge is waived for the first month", and users cannot tell which plan it belongs to. Which repair does the page support?
   - **a**: Return twenty chunks for every question, so that the plan's name turns up in one
   - **b**: Cut the text into smaller windows, so that each chunk holds fewer unrelated words
   - **c**: Ask the model to guess the plan from the rest of the conversation when it answers
   - **d**: Put the document title and section name in front of each piece before indexing

22. Scenario: Tarn Dental's entire knowledge base is about 60,000 tokens, edited a few times a year, and the team plans a vector database with chunking and reranking. What does the page advise first?
   - **a**: Build the vector database
   - **b**: Put the complete corpus into a cached prompt
   - **c**: Fine-tune a model on the corpus
   - **d**: Split the corpus across several agents, each of which holds a part of it

23. Scenario: Dunmore Legal switches its retrieval from one embedding model to a newer one. It embeds only the documents that change from now on, and keeps the old embeddings for the rest. Recall on its labelled questions falls at once. What explains it?
   - **a**: The newer model needs a larger chunk size, so the old chunks are now too small
   - **b**: The old vectors are stale copies of their documents, so a re-index would clear them
   - **c**: Vectors from two different makers share no space, so rebuild the whole index
   - **d**: The keyword index must be rebuilt first, then the vectors

24. Scenario: Ferris Systems' staff each paste the company's access string for the model supplier into their own scripts, and a former employee still has a working copy. The security lead asks for a design that limits the damage of a leaked or abandoned copy. What fits?
   - **a**: A proxy that holds the master secret itself and issues every person a separate token
   - **b**: A rotation of the shared access string every quarter, announced by e-mail
   - **c**: A prompt rule that tells the agent never to reveal the string it was given
   - **d**: A separate supplier account for each team, with one shared string inside each of those accounts

25. Scenario: Mallow Cloud's MCP server requests every permission up front when a person connects, so that nobody sees a second consent prompt. A reviewer objects. What does the guidance say instead?
   - **a**: Begin with read access and widen it when a privileged operation is first attempted
   - **b**: Ask for every scope the server offers, once at the start
   - **c**: Ask for one broad scope that covers all operations, and log each use of it afterwards
   - **d**: Pass the user's own token on to the downstream service, so no scope is needed

26. Scenario: Pewter Labs merges its 40 tools into 8 tools, each with a switch argument, hoping to cure wrong tool selection. Selection does not improve. Which two statements explain it? (Select two.)
   - **a**: A switch argument is a setting that a newer model refuses
   - **b**: Eight tools are still more than a request can hold, so the list must shrink
   - **c**: The choice among behaviours is hidden from the model's own judgement
   - **d**: Merged tools lose their descriptions, so selection has no information left
   - **e**: The number of capabilities is the same as before the change

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
   - **a**: Add every content field to the records
   - **b**: Allow that one content field by name, for a stated purpose and a limited time
   - **c**: Copy the prompts to a private file, so that the shared records stay free of content
   - **d**: Keep the records as they are, and ask users to describe the failing cases

30. Scenario: Sable Cargo changed its model on purpose last Tuesday, and its cost per answer rose by 35 percent against the stored reference. The drift check pages the on-call engineer. What does the page advise?
   - **a**: Keep the old baseline for a year, so that the rise is never forgotten, whatever else changes in between
   - **b**: Raise the tolerance until the page stops
   - **c**: Turn the cost metric off until the next planned change has been decided
   - **d**: Reset the baseline at the switch that was chosen and treat other moves as findings

31. Scenario: After a release, the share of declined requests at Thorn Care's assistant falls from two percent to nothing, and the dashboard shows green. How should the team read the move?
   - **a**: As a signal to investigate, since such a collapse often points to a failed safeguard
   - **b**: As a clear improvement, since fewer declines mean happier users
   - **c**: As noise, since a metric that small cannot move in a meaningful way
   - **d**: As a billing artefact, since declines are counted in a separate system from the one that bills

32. Scenario: Bexley Bank plans to let a language model mark 1,000 free-form write-ups each night against a rubric. No person has yet compared its marks with theirs. What comes first?
   - **a**: Switch the grader on at full volume straight away
   - **b**: Replace the grader with exact-match checks on every write-up
   - **c**: Measure its agreement with human judgement on a small sample
   - **d**: Have people grade all 1,000 reports each night as well

33. Scenario: Colt Retail scores its assistant on the five worked examples written into its own prompt and reports 100 percent. A reviewer says the score tells little. What should make up its evaluation set?
   - **a**: The same worked examples, with more of them added until the score stops moving
   - **b**: Cases written by the developers who built the assistant and know it
   - **c**: Real traffic in its proportions, plus awkward cases added and tagged
   - **d**: A single hard benchmark taken from a public leaderboard of models

34. Scenario: Arden Insurance runs a live test of a new prompt. After 300 trials in each arm the new prompt is two points ahead, and the owner wants to stop and ship while it is in front. Which two statements does the page support? (Select two.)
   - **a**: Add trials until the lead reaches five points, then ship
   - **b**: Quitting on a lead gives false wins
   - **c**: Gather the sample that was planned before reading the result
   - **d**: Run the same 300 trials again, since a repeat will confirm the lead
   - **e**: Stop now, since the larger number is the better version

35. Scenario: Pike Telecom reports a mean latency of 1.4 seconds for its streamed assistant, yet users complain that it is slow. Which figures should the report add?
   - **a**: The mean of the fastest half of the requests
   - **b**: The maximum latency seen over a whole month
   - **c**: The total tokens per answer, averaged over a day
   - **d**: The 95th percentile and the time to first token

36. Scenario: Kern Logistics runs one assistant on Anthropic's own API and on Amazon Bedrock, and plans all its migrations from the dates in Anthropic's table. A model is listed as retiring in four months. What should the team do?
   - **a**: Use the table's date for both
   - **b**: Track a second schedule
   - **c**: Wait for the provider to send a notice before acting at all
   - **d**: Move only the direct traffic and leave Bedrock alone

37. Scenario: Hale Group runs forty programs on three generations of Claude and has no list of which program uses which version when a retirement notice arrives. Which step does the page give first?
   - **a**: Wait for the retirement date to fix the list of models that are still in use
   - **b**: Change the model id in every call site, and see which applications then fail
   - **c**: Ask each team to report the models it believes it uses, from memory
   - **d**: Export usage by API key and model from the Console, then map the keys to applications

38. Scenario: Ivy Retail's gate allows the bill to grow by 25 percent. The new model's total comes out at 25 percent above the old one, and no other check fails. Which two statements does the page support? (Select two.)
   - **a**: The check passes only if the latency check also improves
   - **b**: One point beyond the limit is refused
   - **c**: A rise exactly at the limit passes
   - **d**: A rise at the limit leaves no room for later growth, so it fails
   - **e**: Any rise is a reason to refuse, so the cost check fails

39. Scenario: Larch Mutual's gate judges a change on twelve cases. It adds correct answers in the status and complaint segments and drops none in the refund segment, which is protected. What does the gate return?
   - **a**: Refuse, since every change must be proved by a live test first
   - **b**: Release only after the protected segment has also gained a case
   - **c**: Release, since nothing was lost and the net result is positive
   - **d**: Refuse, since three added answers cannot offset a possible future loss

40. Scenario: In a launch review at Tern Health, a wrong decision costs 3 units and a person's check costs 1 unit, and the architect must state the accuracy above which routing items to a person stops paying. Which figure goes into the review, following the page's rule?
   - **a**: 66, which is 100 minus the percent rounded up
   - **b**: 33, the share the check costs against an error, rounded down
   - **c**: 67, the share left after the exact percent is rounded to the nearest unit
   - **d**: 0, the figure used when no check pays at any accuracy

41. Scenario: A proposal at Larch Mutual moves its change out in exactly three stages, and the sponsor needs the review to say whether the plan blocks the launch. What does the review record for the roll-out?
   - **a**: No finding, since a threshold is met at its edge
   - **b**: A low finding, since three is the least that passes
   - **c**: A high finding, since a roll-out needs more than three stages
   - **d**: A medium finding, since three stages leave no spare stage

42. Scenario: A run of twelve tasks at Pelham Media crashes its process at the tenth task every time, and the runner writes results to the store only when the whole run ends. Which design change keeps finished work safe and cheap to redo?
   - **a**: Retry the whole run three times, keeping the end-of-run write
   - **b**: Persist each outcome as soon as it is done, so a retry takes only the rest
   - **c**: Catch the crash in the runner, so that the end is always reached
   - **d**: Put the first nine results into the prompt of the tenth task

43. Scenario: Sorrel Cloud serves twelve customers from a single shared pool and separates their data by a filter in its application code. An auditor asks how the data of one customer is kept from another. What does the page say about the design?
   - **a**: It is sound
   - **b**: It is sound, as long as each customer is given a key of its own to use
   - **c**: It is a finding only if two of the customers work in the same industry
   - **d**: It is a finding

44. Scenario: Bracken Health's structured output uses a schema whose fixed list of allowed values holds the names of diagnoses taken from real patient records, in a deployment that is meant to be HIPAA ready. Which change does the design need?
   - **a**: Cap the list at ten of the most common values
   - **b**: Give each tenant a schema of its own with that list
   - **c**: Move clinical data out of the definition into the message
   - **d**: Encrypt the schema inside the request body in transit

45. Scenario: Vale Insurance's tokenising layer finds e-mail addresses and member numbers by pattern, and a check shows that customer names reach the model untouched. Which two steps does the page advise? (Select two.)
   - **a**: Add a line to the system prompt telling the model to disregard names
   - **b**: Switch the layer off, since a pattern that misses names gives false comfort
   - **c**: Plant identifiers of every kind in tests and verify that none gets into the request
   - **d**: Add a stronger detector, or keep the free text away from the provider
   - **e**: Accept the gap, since names carry no risk once the addresses are gone

46. Scenario: Brae Insurance's assistant quotes a sentence that is not in the policy text, and its score is 99. The threshold for sending unreviewed is 95. What becomes of the reply?
   - **a**: It goes out unreviewed
   - **b**: It goes to review
   - **c**: It goes out with a note that the quote could not be found in the source
   - **d**: It is held

47. Scenario: A streamed reply in Garnet Health's assistant ends with a refusal, and the user's next message carries on the same conversation. The next call is refused as well. Which recovery belongs in the design?
   - **a**: Drop or rephrase the triggering turn, or send the request to another model
   - **b**: Show the user the category of the refusal, which is always present
   - **c**: Raise the sampling temperature, so that the next reply differs
   - **d**: Resend the same history unchanged until it passes through

48. Scenario: Linnet Legal's assistant summarises inbound e-mails and also holds a file-sharing capability, and one message hides a line telling the model to send the user's files to an outside address. Which design handles the message and that capability?
   - **a**: Add it to the user turn with a request that the model ignore any orders in it
   - **b**: Strip every sentence that is written as an instruction, using a list of patterns
   - **c**: Carry the incoming item as JSON inside a tool result, with its origin named, and narrow that right
   - **d**: Place the text in the system prompt, so that the model reads it before anything else

49. Scenario: Odell Health's register row for an unfair outcome names the parity report as its control, and a reviewer asks what the row should say. Which two entries does the page support? (Select two.)
   - **a**: The residual reads nil, since a measured gap leaves no risk
   - **b**: The residual moves to the vendor, since the model is theirs
   - **c**: The residual reads prevented, since every group is measured before an answer goes out
   - **d**: The residual reads monitored and not prevented
   - **e**: That measure needs an owner, since it repairs nothing itself

50. Scenario: Hollis Mutual's risk register lists four failure modes, and the entry for privacy leaks names a pattern filter that does not appear anywhere in the architecture. What does the page say about that row?
   - **a**: It is a wish
   - **b**: It is sound
   - **c**: It is sound, as long as the owner column names a person
   - **d**: It is a minor flaw

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
   - **c**: No, but only for replies that concern a decision about a person in the service
   - **d**: No, since the duty belongs to the deployment and cannot be toggled

54. Scenario: A sponsor at Cairn Utilities asks that an assistant be "fast and accurate". The architect wants to turn the wish into a specification. Which two statements does the page support? (Select two.)
   - **a**: A requirement lacking any of those goes back to the stakeholder as a question
   - **b**: A requirement is complete when it is written without technical terms
   - **c**: A requirement is complete once it carries a figure, a method of measuring it and a named owner
   - **d**: A requirement is complete when it names the model that will meet it
   - **e**: A requirement is complete when the sponsor has signed it and engineers have read it once

55. Scenario: In the options table of a design record, the entry for the costliest design carries a two-word rationale, "Too much". A reviewer asks for a change. What should that entry say instead?
   - **a**: A single word that names the main cost of the option
   - **b**: A reference to the sponsor's budget for the year
   - **c**: A sentence with a checkable reason, such as top price and slow reply
   - **d**: The name of the engineer who first rejected the option, for traceability

56. Scenario: The one-page note that Vane Rail's architect wrote for the funder describes the saving in detail and says nothing of what the proposal gives up. A colleague objects. What should the note also contain?
   - **a**: The price paid, the exposure that remains and the decision that is requested
   - **b**: The break-even derivation, so that the funder can check the arithmetic
   - **c**: The list of model settings, so that the funder can confirm the build
   - **d**: A second saving figure for a more optimistic case, to balance the first

57. Scenario: A service level promises availability of at least 99.5 percent, written as 995 per mille. The month's measurement is 990 per mille. How does the report read?
   - **a**: Short of the floor by 5, counted in the unit of the target
   - **b**: Failed, with the gap left unstated in the report
   - **c**: Met, as the measurement is within a rounding margin
   - **d**: Short by 5 percent of the monthly availability

58. Scenario: Two months into a pilot, Dray Finance's team sees that its accuracy target for the costly segment will not be met by the agreed deadline. How should it handle the promise?
   - **a**: Keep quiet about the shortfall until the date
   - **b**: Amend it through the channel that made it, with a reason, before the due day
   - **c**: Report the overall average instead of the segment
   - **d**: Move the target in the next sprint plan, without telling the sponsor or the review board

59. Scenario: Larch Mutual's architect has a finished review: a conclusion, a count per domain and twenty-two itemised flaws, and must present it to the sponsor and the engineers. How should the document be arranged?
   - **a**: The flaws first in domain order, then the conclusion and the count at the end
   - **b**: The count first, then a narrative of the design, then the flaws with no conclusion
   - **c**: The engineers' evidence first, so that the sponsor reads the analysis before the conclusion
   - **d**: Lead with the headline outcome, follow with a tally by area, then the ranked details

60. Scenario: Eland Systems fetches its policy from the admin console, and the security lead decides that no developer may begin without the current policy, even when the network is down and work stops. Which setting expresses that decision?
   - **a**: availableModels, which restricts the models a developer may pick
   - **b**: disableSideloadFlags, which rejects the options that load a plugin
   - **c**: forceRemoteSettingsRefresh, which makes startup fail closed
   - **d**: allowManagedHooksOnly, which stops every unmanaged hook from running

61. Scenario: A platform team caps effort at high in managed settings. A project file sets medium and a user file sets max. Which two statements does the page support? (Select two.)
   - **a**: The project file's cap is ignored, since only managed settings may set one
   - **b**: The most restrictive setting wins, so the lowest of the three values is in force
   - **c**: Max is in force, since the user file is read last
   - **d**: High is in force, since managed settings outrank every other level for this key
   - **e**: Nobody can loosen the organisation's ceiling, though a developer may tighten it

62. Scenario: A developer at Gorse Bank says that a restriction in her local file is not honoured, and the platform team wants a runbook step that explains why on her machine. Which two commands does the runbook name?
   - **a**: /memory, which shows the loaded files, and claude init, which writes a fresh settings file
   - **b**: /model, which shows the chosen model, and claude logout, which clears the stored session
   - **c**: /permissions, which lists the rules, and claude update, which fetches the newest policy
   - **d**: /status, which shows the setting sources, and claude doctor, which lists what was dropped

63. Scenario: Hart Media's platform team writes a managed MCP allowlist entry that gives both a server name and its address, believing two keys make the entry stricter. What does the page say about such an entry?
   - **a**: It is invalid
   - **b**: It is valid, but only the address is read when both keys are set
   - **c**: It is valid for a deny list, and invalid for an allow list only
   - **d**: It is valid and stricter

<details>
<summary>Answer key</summary>

1. **b and e**. The path is known, so the code should run it: "If you can write the steps down and their order, the code should run them, because deterministic orchestration is cheaper, testable and auditable" (module 79, page 1). The workflow rung is chosen when "The path is known in advance and the steps differ in kind". *a* is ruled out because "Only then does autonomy earn its price", and the path here needs no model to decide what to do next. *c* is ruled out because a single call suits "one transformation with the input in hand", and five steps in an order are more than that. *d* is ruled out because the team rung needs "the volume of reading exceeds one context", and the steps here run one after another.
2. **a**. The pillar "Accepts a lower first-pass accuracy and a pilot, and measures adoption", which is what the sponsor asked for (module 79, page 1). *b* is ruled out because efficiency claims "The same work, with less effort per unit", and there was no work done for this group before. *c* is ruled out because productivity means "People get more done", and no staff member is the beneficiary here. *d* is ruled out because the cost pillar claims "Work costs less in total", and the sponsor named a new service.
3. **d**. A team needs independent parts, and these share one context: "cannot be split without losing information" (module 80, page 1). *a* is ruled out because value is only the third condition: "it needs independence, volume and value together". *b* is ruled out because the first test fails whatever the window size: "A team that fails the first test is a worse single agent". *c* is ruled out because a workflow is for a known path: "Is the path known? Then it is a workflow, and it costs one chat per step", and drafting that depends on other drafts has no fixed order.
4. **b**. Price comes last and among the survivors: "Price enters last, and only among the designs that were not rejected" (module 80, page 2). *a* is ruled out because "a cheaper design with a missing feedback loop is not cheaper, only unfinished". *c* is ruled out because the rule picks "the cheapest of those", and a design that needs revision is not a rejected one. *d* is ruled out because "when every design is rejected there is no winner", and here two designs survive.
5. **d**. A subagent's reading is thrown away when it finishes, so what goes back is a digest in the contract's shape: "Return a digest, not a transcript" (module 80, page 2). The finding with its source leaves the coordinator's window free for writing. *a* is ruled out because the page asks for the objective and boundaries so "that two subagents do not research the same thing or leave a gap between them". *b* is ruled out because the transcript is the problem and not its size: "The subagent's reading is thrown away when it finishes". *c* is ruled out because "What goes back is the finding, with its source, in the contract's shape".
6. **b**. The key must name the intention and be made once, before the first call, from what identifies the task (module 81, page 1). The page says "A new key for each attempt turns the retry into a new refund", which is the flaw here. *a* is ruled out because "A new key for each attempt turns the retry into a new refund". *c* is ruled out because the flaw is that the value changes between attempts, and "The key names the intention, not the attempt". *d* is ruled out because the page says "Record the key together with the effect", in one transaction.
7. **c**. A refusal is a fatal failure, and the runner "never retries the second" kind (module 81, page 1). A timeout or a rate limit is transient and is repeated up to a fixed number of calls. *a* is ruled out because "The key names the intention, not the attempt", and a missing permission is not a problem of recognising repeats. *b* is ruled out because the breaker counts "consecutive failures for one agent", and a missing permission is a fact about the account. *d* is ruled out because "An unlimited retry is a way to spend money while nothing changes", and ten is the same mistake.
8. **a**. A failed probe opens the breaker again: "A success closes it; a failure opens it again" (module 81, page 1). The agent is not reached until the next probe, which saves calls in a long outage. *b* is ruled out because "A success resets the count, so scattered failures do not open it", which concerns the closed state. *c* is ruled out because "A success closes it", and nothing here succeeded. *d* is ruled out because "One probe call goes through", and a failure sends the breaker back to open.
9. **b and e**. The capstone sets the rung: "an augmented call with a small fixed workflow, not an agent, because the path is known", and it notes "A team of agents would cost about 15 times a chat, and the value does not pay it" (module 93, page 1). *a* is ruled out because autonomous agents come with "higher costs, and the potential for compounding errors". *c* is ruled out because the design needs "The index is replaced when a document changes", and pasted text goes stale. *d* is ruled out because the capstone chooses "an augmented call with a small fixed workflow, not an agent, because the path is known".
10. **b**. The chain removes first and ranks after: "a question that shares nothing with any readable document gets no evidence" (module 93, page 1). *a* is ruled out because "Documents the reader may not read are removed first, then the rest are ranked". *c* is ruled out because "a question that shares nothing with any readable document gets no evidence", so no chunk is returned. *d* is ruled out because "an answer whose quote is missing is held as unsupported", and the chain never sends it out with a flag.
11. **d**. A high finding rejects: "The design is rejected until it is fixed" (module 93, page 2). *a* is ruled out because a medium flaw "weakens a design without breaking it", and a missing way back is a flaw that can cause harm that cannot be undone. *b* is ruled out because the low bullet reads "It is recorded and does not hold the design back", and a missing rollback is not low. *c* is ruled out because "reject if any finding is high", so a high finding is not a revision.
12. **c**. The documented order is tools, system, messages: "a change to a tool definition invalidates the system prompt and the messages that follow it" (module 82, page 1). *a* is ruled out because the page gives the cause as an edit inside the prefix: "The edit is inside the prefix, so everything from there on is a new entry". *b* is ruled out because the documentation fixes the order of the whole request "as tools, then system, then messages", so tools lie in front of the breakpoint. *d* is ruled out because the page names the tool edit as the cause, and says the tool list should be treated "as a stable asset".
13. **b**. Reuse is all or nothing: "nothing is reusable and the answer is zero" (module 82, page 2). *a* is ruled out because "If any block differs, or either prompt has no breakpoint, nothing is reusable". *c* is ruled out because the page says "If any block differs, or either prompt has no breakpoint, nothing is reusable", however small the difference. *d* is ruled out because the check is on the blocks: "every block up to it must match in name and text".
14. **b and e**. The guide says "where a technique names a specific model, treat it as measured on that model and re-check it against your own evals before applying it to another", so the design runs an evaluation set on every candidate model and prompt change (module 82, page 2). *a* is ruled out because "Copy the prompt that worked on the current model; models in one family behave alike" is the page's rejected trap. *c* is ruled out because "a technique is measured on a model, so it is re-checked against your own evaluation before it is applied to another". *d* is ruled out because "an instruction that fixed a behaviour on one model can be unnecessary or harmful on another", so it is tested, not dropped by default.
15. **a**. The comparison is per task: "a weaker model that needs three attempts or a longer prompt can cost more than a stronger one that answers once" (module 82, page 2). *b* is ruled out because "the cheapest model per token is not always the cheapest per task". *c* is ruled out because latency is a limit that rules models out and "Price comes last because it is the only fact that cannot make a model wrong". *d* is ruled out because the page gives no such link, and the tier is "found by evaluating the task, not by reading a model's name".
16. **c**. The gateway "compares the slow case (the 95th-percentile time plus a safety margin) with the caller's timeout" (module 84, page 2); 2,100 ms is over 2,000 ms. *a* is ruled out because the comparison uses the tail and not the median: "the 95th-percentile time plus a safety margin". *b* is ruled out because the comparison is made with "the 95th-percentile time plus a safety margin", and the slow case alone is not the figure. *d* is ruled out because the choice is the gateway's: "If it does not, the gateway uses accept-and-poll".
17. **c**. The edge is exact: "A budget that is not positive blocks" (module 84, page 2). *a* is ruled out because admission is decided against the budget: "A request that would bring the spend to more than the budget blocks". *b* is ruled out because a warning belongs to the case "Reaching 80 percent warns", and a zero budget is blocked before that. *d* is ruled out because the rule has no free first request, and "A budget that is not positive blocks".
18. **a**. Degrading needs a named entry: "when it has no cheaper entry the model is kept" (module 84, page 2). *b* is ruled out because the replacement is "a cheaper one that the policy names", not a free choice of the cheapest. *c* is ruled out because "A blocked team gets no model", and the team here is near its budget and not blocked: "a team is warned, then degraded, then stopped, in that order". *d* is ruled out because the route table is the gateway's: "Route each task to a model by one table".
19. **d**. A gateway does not guess: "A row whose model has no price is refused, because a gateway never guesses a price" (module 84, page 2). *a* is ruled out because "a gateway never guesses a price", and an average is a guess. *b* is ruled out because "A row whose model has no price is refused", which names the problem, and a skipped row understates the bill. *c* is ruled out because "Every token kind has its own price", and a price of zero makes the usage free in the report.
20. **c and e**. When the answer lives in a table or a database the mechanism is a structured query run by a tool, which is "Exact, and able to total or filter", while cutting the table "adds numbers by eye" is the failure it avoids (module 85, page 1). *a* is ruled out because the failure the page names is "The table is cut into chunks and the model adds numbers by eye". *b* is ruled out because embedding models "can miss crucial exact matches", and add nothing up. *d* is ruled out because a keyword index "matches the exact string", and it does not total or filter.
21. **d**. Structure supplies the context a chunk lacks: "The pipeline already knows the document title and the section name" (module 85, page 1). *a* is ruled out because "Returning more chunks per question does not help either". *b* is ruled out because "A smaller window cuts in more places and separates more sentences from their neighbours". *c* is ruled out because "Retrieval hands the model a chunk without its document", and a guess gives no evidence to cite.
22. **b**. A corpus under about 200,000 tokens needs no index: "No stage can fail and nothing can be missed" (module 85, page 1). *a* is ruled out because "A pipeline adds stages that each can fail", and a corpus this small needs none of them. *c* is ruled out because the page says "The Claude API does not currently offer fine-tuning", and weights are weak when facts change and a source must be shown. *d* is ruled out because the corpus fits one window, and a team of agents is justified only when "the reading that has to be done is larger than one agent can hold".
23. **c**. The page gives the rule: "Vectors made by two models are not in one space, so the index must be rebuilt as a whole" (module 85, page 2). *a* is ruled out because "Size then follows from the unit", the structure of the data, and not from the embedding model that scores the chunks. *b* is ruled out because "An index is stale when a chunk no longer matches its source", and these old vectors still match their documents. *d* is ruled out because the fault is that "queries embedded by the new model will be compared with chunks embedded by the old one".
24. **a**. The documentation lists the gain of a gateway: "the provider key stays server-side; developers hold gateway credentials instead" (module 86, page 1), so offboarding revokes one credential. *b* is ruled out because the string stays in every pair of hands between rotations, whereas with a gateway "a leaked developer credential is not a leaked provider key". *c* is ruled out because "a line in the system prompt is a request to the model and not a control". *d* is ruled out because a shared string in each team keeps the same leak, and the page puts the provider credential in one place, "shared by all forwarded traffic".
25. **a**. The guidance is progressive: "Implement a progressive, least-privilege scope model", starting from read operations (module 86, page 1). *b* is ruled out because "A token with every scope granted up front makes a stolen token worth the whole system". *c* is ruled out because "Poor scope design increases token compromise impact, elevates user friction, and obscures audit trails". *d* is ruled out because "Token passthrough is explicitly forbidden in the authorization specification".
26. **c and e**. Merging "hides the behaviours from the model's own choice and leaves the number of capabilities the same" (module 86, page 2), so nothing was removed and the choice is only moved into an argument. *a* is ruled out because the merge is rejected for a different reason: "Merging tools into one with a mode argument is not a cure". *b* is ruled out because the page puts trouble where Claude's ability to pick the right tool "degrades once you exceed" the thirty to fifty mark, and eight is far below it. *d* is ruled out because "the descriptions are what the model chooses by", and a merge keeps them.
27. **d**. The audit lists dormant tools: "They are not removed, because the role needs them; they are reviewed" (module 86, page 2). *a* is ruled out because the tools to remove are "the ones held and not needed", and this one is needed. *b* is ruled out because the page asks for a review, "since either the role description or the usage sample is wrong". *c* is ruled out because deferral is a loading choice for the long tail, and the page says to keep the "most frequently used tools non-deferred", which leaves the audit question open.
28. **c**. The page puts facts before suspicions: "A failed span is a fact, and a stale retrieval is a suspicion" (module 87, page 1). *a* is ruled out because "A system that blames the retrieval whenever it can will send engineers to the index while a tool is broken". *b* is ruled out because "the first span to turn red is not the origin". *d* is ruled out because the model span reported no error, and the origin is "the one that no other failing span has as its parent".
29. **b**. Content is opened narrowly: "allow one by name only for a stated purpose and a limited time" (module 87, page 1). *a* is ruled out because "Telemetry is a copy of your users' data in a place with a different audience". *c* is ruled out because the records "Keep ids, counts, timings, names of models and tools, and statuses", and a private copy of the prompts is an ungoverned store of content. *d* is ruled out because the page says "Drop the content fields, and allow one by name only for a stated purpose and a limited time", so a stated purpose may open one.
30. **d**. The page gives the rule: "reset a baseline deliberately, at a change you decided on, and to treat a move you did not plan as a finding" (module 87, page 2). *a* is ruled out because "The baseline is not permanent", and a planned change moves the numbers on purpose. *b* is ruled out because "Raising the threshold until the pages stop hides the real incident with the noise". *c* is ruled out because the rule is to re-baseline, and the migration guide says "Recount tokens and re-baseline cost".
31. **a**. Drift is read in both directions: "A refusal rate that drops to zero can mean a guardrail stopped working" (module 87, page 2). *b* is ruled out because "a fall is not always an improvement", and both directions matter. *c* is ruled out because the check will "flag every move beyond a tolerance, in either direction", and a fall from two percent to nothing is the largest relative move possible. *d* is ruled out because "A refusal arrives with a normal status", so a refusal metric belongs to the model layer, not to billing.
32. **c**. The documentation says of model grading: "Test to ensure reliability first then scale", and the page adds "Check that agreement before you trust the grader" (module 88, page 1). *a* is ruled out because the documentation says "Test to ensure reliability first then scale", and full volume is the scale step. *b* is ruled out because exact match "rejects a correct report that is worded differently". *d* is ruled out because human grading is "slow and expensive. Avoid if possible", and "Human review of every output does not scale".
33. **c**. The set must mirror use: "Design evals that mirror your real-world task distribution" (module 88, page 1). *a* is ruled out because the examples in a prompt "were picked to teach a format, so a score on them says how well the prompt repeats itself". *b* is ruled out because the set "is built from the traffic and not from the developers' imagination". *d* is ruled out because a single hard case set is not the distribution of real use, and the page asks for "an evaluation set that looks like the traffic".
34. **b and c**. The page names the rule: "Do not stop by who is ahead", because "Stopping or extending a test according to who is ahead produces false wins", so the planned cases are collected first (module 88, page 2). *a* is ruled out because "Decide the metric and the sample before you look." *d* is ruled out because "Running the same cases again repeats the same sample and adds no information." *e* is ruled out because "The larger number is not the better version."
35. **d**. The page says "A mean hides the tail", and "For streamed answers the time to the first token is the number users feel" (module 88, page 2). *a* is ruled out because "A mean hides the tail", and a mean of the fastest half hides it even further. *b* is ruled out because the maximum is one case, whereas "the slowest five in a hundred requests are the ones users complain about". *c* is ruled out because length is something to trim later: "Cut tokens where the evals say it is safe", and it does not tell the report what users feel.
36. **b**. The page says: "Partner-operated platforms (Amazon Bedrock and Google Cloud) set their own retirement schedules" (module 89, page 1). *a* is ruled out because "A multi-platform estate keeps one calendar per platform". *c* is ruled out because "There is notice, and it is finite", and the platform's own schedule is to be tracked, not awaited. *d* is ruled out because the provider's schedule applies to that traffic, "so a model's lifecycle status and dates can differ".
37. **d**. The page describes the audit: "usage broken down by API key and model" from the Usage page (module 89, page 1). *a* is ruled out because "There is notice, and it is finite", and waiting leaves no time to find the applications. *b* is ruled out because a blind change of ids ships the failures the audit is meant to find: "You cannot migrate what you cannot find". *c* is ruled out because a recollection is not an audit, and the export "finds the keys; the keys point to the applications".
38. **b and c**. The edge is stated: "A rise exactly at the budget passes and one point over it is refused" (module 89, page 2). *a* is ruled out because "The example's gate has five checks, in a fixed order, and every failing check adds a reason". *d* is ruled out because "The budget is set from the re-baselined cost of the first page", so the arithmetic decides and not a margin. *e* is ruled out because the check is made "against a budget set before the run".
39. **c**. The example prints the result: "the same change is a go with no loss and three gains" (module 93, page 1). *a* is ruled out because "an A/B test comes after it", and the gate's two keys are no protected loss and no net loss. *b* is ruled out because the rule is "no protected segment may lose a case", and it asks for no gain there. *d* is ruled out because the gate judges the cases it has: "the losses must not outnumber the gains", and a possible future loss is not one of them.
40. **a**. The page works the case: "a third of a hundred is 33.3, which rounds up to 34" (module 93, page 2), so the accuracy is 100 minus 34, which is 66. *b* is ruled out because 33 is the rounded-down share, and the rule is "100 minus the review cost as a percent of the error cost, with that percent rounded up". *c* is ruled out because the percent is taken "with that percent rounded up", which gives 66 and not 67. *d* is ruled out because zero comes only when "A check that costs as much as an error, or more, needs no accuracy".
41. **a**. The page treats the edge as passing: "three stages pass", and "One step past any of them is a finding" (module 93, page 2). The roll-out here sits exactly at the edge. *b* is ruled out because the review must "treat the edge as the design's friend", and a pass at the edge carries no note. *c* is ruled out because the `big-bang-rollout` rule is medium and applies to "fewer than three roll-out stages", not to three. *d* is ruled out because the rule fires only below three: "fewer than three roll-out stages".
42. **b**. Results are written as work is done: "Write on success, immediately" (module 81, page 2). A crash in the tenth task then loses nothing from the first nine, and the next run does the rest. *a* is ruled out because a restart is the expensive path: "it discards the work that finished, which is the expensive part". *c* is ruled out because "swallowing it hides the defect and reports unverified results". *d* is ruled out because the checkpoint is a store of finished results, not context for the next task: "A checkpoint is a store of finished results".
43. **d**. The separation is the platform's: "one workspace per tenant, with its own keys, limits and spend" (module 83, page 1). *a* is ruled out because "code-only separation fails the first time a filter is forgotten". *b* is ruled out because the workspace is the unit: "A single shared workspace for all tenants is a finding for a multi-tenant requirement". *c* is ruled out because the finding follows from the shape of the deployment, since "the separation then rests on application code alone".
44. **c**. The page warns that schemas "are compiled into grammars that are cached separately from message content, so they do not get the protections of the prompt" (module 83, page 1), so health data stays out of them. *a* is ruled out because the page names no such limit, and the rule it gives is "put no health data in a schema". *b* is ruled out because tenants are separated by workspace, "one workspace per tenant", and a schema per tenant would still hold the data. *d* is ruled out because the objection concerns where the schema is stored, and the page says "put no health data in a schema".
45. **c and d**. A person's name has no fixed shape, so a pattern layer leaves it in, and the page asks for "a stronger detector for names, or a design in which the free text does not reach the model at all, and tests that plant identifiers of every kind" (module 83, page 2). *a* is ruled out because "the instruction is read by the model that has already received the data". *b* is ruled out because "a reviewer can test the layer with sample text, which cannot be done for an instruction". *e* is ruled out because "A layer that has not been tested against the identifiers it claims to remove is a hope with code around it."
46. **d**. The route checks the source before the score: "An answer whose quote is not in the source is held (`hold: unsupported`) even at confidence 99" (module 90, page 1). *a* is ruled out because "confidence is not evidence", and a model can be certain and wrong. *b* is ruled out because review is for answers the source supports: "a low-consequence action goes out unreviewed only at a confidence of at least 95, and below it goes to review". *c* is ruled out because the example holds the answer: "An answer whose quote is not in the source is held", and a note would still send the unsupported text out.
47. **a**. The design treats a refusal as an outcome: "the context is reset (the turn that caused it removed or rephrased) before the conversation continues, or the request is retried on a different model" (module 90, page 1). *b* is ruled out because "the stop_details fields can be null", so the user-facing message comes from the application. *c* is ruled out because the page names two recoveries, with "or the request is retried on a different model" as the second, and sampling is neither. *d* is ruled out because the page says "the context is reset (the turn that caused it removed or rephrased) before the conversation continues", not resent.
48. **c**. The injection control has several parts: "untrusted content only in tool results, JSON-encoded; screen tool output; least privilege" (module 90, page 1). The narrow right limits what a hidden line can do. *a* is ruled out because "a line in a prompt is a request, and a control is a step the model cannot skip". *b* is ruled out because pattern lists belong to the input layer, and the page says "A screen with a small model reduces risk and does not remove it". *d* is ruled out because untrusted content belongs "only in tool results", and not among the instructions.
49. **d and e**. The report is a measure with an owner and not a repair: "The report does not fix anything on its own", which is why the residual column says "monitored and not prevented" (module 90, page 2). *a* is ruled out because "a headline average hides a failing group exactly as it hides a failing segment". *b* is ruled out because "Compliance belongs to the deployment and its agreements." *c* is ruled out because "The design cannot prevent unfairness by saying so; it measures it."
50. **a**. The register's test is mechanical: "a row with a control that does not exist is a wish" (module 90, page 2). *b* is ruled out because "every control in the register is a control in the design". *c* is ruled out because "A row with no owner has nobody to call; a row with a control that does not exist is a wish", and the two faults are separate. *d* is ruled out because "the residual column says what is left" once a control works, and a control that does not exist leaves the whole risk.
51. **d**. The page ties the assumption to a computation: "Compute reviewer hours per day from volume and routing rates" (module 79, page 2). *a* is ruled out because the pilot's own conditions are the problem: "The pilot's conditions were friendly in ways nobody wrote down". *b* is ruled out because that test belongs to another row: "Count escalations per hundred tasks and staff for the rate" answers whether people covered edge cases. *c* is ruled out because "Test permissions with the least privileged real role" is the test for the access assumption, not for reviewers.
52. **b**. The page names two deadlines: "the date by which the new model must be ready, and the date after which the old one cannot be the fallback" (module 89, page 2). *a* is ruled out because the stem gives no sign of missing tests, and the fault named is timing: "a roll-out that starts the week before retirement has no rollback at all". *c* is ruled out because the stakeholder message is a separate duty: "A recommended replacement is a starting point, not a decision". *d* is ruled out because the page says to "Recount tokens and re-baseline cost", which is a step of the migration, and not what the retirement week removes.
53. **d**. The transparency duty is fixed: "make that part of the deployment and not a setting someone may switch off" (module 90, page 2). *a* is ruled out because the duty is to "Tell every person who receives output that AI helped produce it", not to tell once per user. *b* is ruled out because the duty applies to "every person who receives output", whatever a user already knows. *c* is ruled out because the duty reaches every reply, and "the Level 1 module on policy covers disclosure for decisions about people" only as a further case.
54. **a and c**. The page says "A requirement is complete when it has a number, a way to measure it and a person who owns it; a requirement missing one of the three goes back to the stakeholder as a question" (module 91, page 1). *b* is ruled out because "A useful discovery asks, and writes down, each of these." *d* is ruled out because the heading reads "Discovery ends in numbers and an owner". *e* is ruled out because "Fast", "accurate" and "safe" are not answers, "because nobody can fail them".
55. **c**. The page asks for argument: "A rejected option has a reason in a sentence" (module 91, page 1). *a* is ruled out because a rejected option "has a reason in a sentence", and one word is not a sentence. *b* is ruled out because "An option that misses a service level is not a candidate, however cheap", and the reason is a checkable fact about the option, not a budget name. *d* is ruled out because the reader's question is "why not the simple thing", and a name does not answer it.
56. **a**. The summary follows an order: "what improves, what it costs, what is at risk and what is asked" (module 91, page 1). *b* is ruled out because the derivation belongs to the engineer's layer, and "The summary is first because the sponsor stops reading early". *c* is ruled out because the sponsor decides and the engineer builds, and settings are "the numbers that produced it" for the engineer. *d* is ruled out because "Leaving out the downside is the common failure", and a second saving adds to the upside only.
57. **a**. A miss is reported with its size: "A miss says by how much" (module 91, page 2). *b* is ruled out because a bare failure "starts an argument that a number would have ended". *c* is ruled out because "A ceiling is met below it and a floor is met above it", and 990 is below the floor. *d* is ruled out because the unit is the target's own: "The target has a number and a unit and a direction".
58. **b**. The page gives the habit: "change a promise through the same channel that made it, with a reason, before the date it matters" (module 91, page 2). *a* is ruled out because the page says "Say early what the design cannot promise". *c* is ruled out because "Promise what you measure and measure what you promise", and the average hides the costly segment. *d* is ruled out because a change must go "through the same channel that made it", and the sponsor was promised the number.
59. **d**. The review is written for two readers: "the verdict first, then the scorecard, then the findings in their order, each with the evidence from the design and the fix" (module 93, page 2). *a* is ruled out because "A list ordered only by domain buries the high findings in the middle", and the sponsor stops early. *b* is ruled out because a review without a conclusion leaves the sponsor nothing to decide: "The sponsor stops at the verdict and the scorecard". *c* is ruled out because "the engineers read the findings", and the sponsor does not read the analysis.
60. **c**. The default is to carry on: "If the fetch fails, Claude Code continues without the remote policy and warns, unless `forceRemoteSettingsRefresh` is set, which makes startup fail closed" (module 92, page 1). *a* is ruled out because it is a list of models: "The lock is `availableModels`, a list". *b* is ruled out because that key rejects the flags "that sideload plugins, agents and MCP servers", and has nothing to do with fetching. *d* is ruled out because that key means "only managed hooks run", and says nothing of a start without the policy.
61. **b and e**. The page gives the rule: "when several levels set a cap, the lowest applies, so a developer may lower the organisation's cap and nobody can raise it" (module 92, page 1). *a* is ruled out because "when several levels set a cap, the lowest applies". *c* is ruled out because "a cap of `xhigh` or `max` in the managed file would be no cap at all for the levels it was meant to hold back". *d* is ruled out because the key is one of the two that go the other way, where "a stricter value is always welcome".
62. **d**. The page gives the lookup: "`/status` shows the `Setting sources` line, which names the managed source Claude Code selected, and `claude doctor` lists what it dropped" (module 92, page 1). *a* is ruled out because a fresh settings file would hide the evidence, and the answer is "a lookup and not a guess". *b* is ruled out because the lookup needs the line that "names the managed source Claude Code selected", and a model choice or a sign-out shows none. *c* is ruled out because the answer is "a lookup and not a guess", and a command that fetches a policy does not say what was dropped.
63. **a**. The page states the shape: "Each entry has exactly one of `serverName`, `serverCommand` or `serverUrl`: an entry with two keys is invalid" (module 92, page 2). *b* is ruled out because the page gives no preference between keys, and states that "Each entry has exactly one of". *c* is ruled out because the rule is about the entry itself, and "A denied server wins over an allowed one" is a different rule about two lists. *d* is ruled out because "an entry with two keys is invalid", and it is not stricter.

</details>
