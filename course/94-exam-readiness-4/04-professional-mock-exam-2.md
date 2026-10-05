# Professional mock exam 2

**Level:** Architect Professional · **Module 94:** Exam readiness 4 · **Page 4 of 4**
**Exams:** P1 to P7 (CCAR-P; the questions follow the Professional blueprint over the content of modules 79 to 93)

**After this page you can** tell whether you are ready for the Professional exam and which domains need more work.

This mock exam covers **the content of Level 4** (modules 79 to 93), as the Professional exam draws on the architect's whole practice, and not one page or one module. Level 4 has two mock exams of 63 questions each, the Professional exam's number of items; this is the second, and no question of one repeats a question of the other. It is written in the exam's style: a named scenario of two or three sentences with a constraint, one best answer and three plausible alternatives (or two right answers among five options, where the question says Select two), each of which is a mistake a practitioner could make. Every question is the course's own, written fresh; no question comes from a live exam, and none repeats a question of a page quiz, a module quiz or another mock exam. The facts behind each answer were read on 2026-10-04 from the pages named on the module pages, so a reader who studies the pages can answer each question from the page it is drawn from. The real exam has multiple-choice and multiple-response items; so does this mock: about one item in six ends with (Select two.) and is right only when both keyed options are chosen.

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
| 42 to 50 | P5 Governance, safety and risk | 14% | 9 | 14.3% | 83, 90 |
| 51 to 59 | P6 Stakeholder communication and lifecycle | 14% | 9 | 14.3% | 79, 91, 93 |
| 60 to 63 | P7 Developer productivity and operational enablement | 7% | 4 | 6.3% | 92 |

For a miss, open the page that the question's explanation names, as (module, page), and reread the passage it quotes. A cluster of misses in one domain points at that domain's modules in the last column. Integration (P3) and solution design (P1) carry the most weight, so they repay the first revision session.

## Mock exam

This mock exam covers the content of Level 4, modules 79 to 93. Choose one answer for each question, or the number the question states.

1. Scenario: Fell Utilities must let an agent call a vendor's booking tool that changes records and offers no idempotency key. The agent's runner retries on timeouts. What does the architect do?
   - **a**: Give it an identifier of its own, or keep repeats away from it
   - **b**: Tell the agent in its prompt to avoid booking the same slot twice
   - **c**: Retry the call until some reply finally arrives
   - **d**: Allow the retries to run exactly as they are today

2. Scenario: Marden Health's pilot ran with ten volunteers who sent clean cases, and the sponsor wants the roll-out to two thousand users to begin next week. Which test does the roll-out plan need for the pilot's assumption about what the volunteers sent?
   - **a**: Buy licences for all two thousand users and read the first week's tickets
   - **b**: Raise the model tier before the roll-out, to handle odd inputs better
   - **c**: Sample real production traffic and evaluate it by slice
   - **d**: Count escalations per hundred tasks and staff the help desk for that rate

3. Scenario: A design at Tamsin Rail passes each customer message straight to a model with no check on its length, its content or its source, and it has a good processing stage, an output check and a feedback loop. What does the four-stage review record?
   - **a**: No finding for the design, which has three strong stages
   - **b**: A medium finding for the missing input side, which asks for revision
   - **c**: A low finding for the missing input side, which can be fixed after launch
   - **d**: A high finding for the missing input side, which rejects it

4. Scenario: A refund screen at Ashby Mutual shows the handler a model's ruling and a green button. Handlers approve over 99 percent of what appears. What does the design lack?
   - **a**: A disclosure line on the screen, so that the handler knows AI helped
   - **b**: The source material next to the answer, so that the review means something
   - **c**: A lower accuracy target for the model, so that handlers have more to correct
   - **d**: A second model that repeats the verdict, so that two opinions agree

5. Scenario: A review at Harlow Retail examines an agent that chooses its own tools for open-ended supplier research, where the path depends on what is found. Everything else in the design is sound. Which finding does the rubric raise for the pattern?
   - **a**: A low finding for team below price, since a lone agent costs about four chats
   - **b**: A medium finding for autonomy without need, since any agent is more than a plan requires
   - **c**: None, since the rule fires only where the route is already known
   - **d**: A high finding for a missing stage, since an agent has no fixed processing step

6. Scenario: An automated workflow at Torrance Mutual approves payments under a daily spending limit. The team measured 99.8 percent accuracy in testing and proposes to enforce the limit in the system prompt. Where should the limit live?
   - **a**: In the model's tool description, to have the model read it at every call
   - **b**: In the prompt, with a check in the next test run
   - **c**: In a reviewer's checklist, to have a person catch every breach by hand
   - **d**: In code outside the model

7. Scenario: A sponsor at Pellam Retail funds a chat design for its agreed targets, the 95th percentile latency and availability. Which value claim is that, and what does it influence in the design?
   - **a**: The service-level pillar, which affects tier, caching, concurrency and accept-and-poll
   - **b**: Cost, which counts tokens, review time and rework and not the model price alone
   - **c**: Productivity, which favours an augmented assistant with a person in the loop
   - **d**: Efficiency, which favours a workflow on the routine path with people kept for exceptions

8. Scenario: At Northwind Insurance about 15 percent of claims are disputed, and nobody can list the steps in advance, though a handler will decide each one anyway. Which two statements does the worked example support? (Select two.)
   - **a**: A single call receives the claim text and returns an approval
   - **b**: An assistant gathers the file for the person who rules
   - **c**: A coordinated crew investigates each dispute in parallel and rules on it
   - **d**: A workflow of fixed calls rules on each dispute in a set order
   - **e**: None of the decisions here calls for a team of agents

9. Scenario: A sponsor at Ivel Bank claims its new triage assistant lowers cost. The team plans to show the saving as the model's price per token, which is below the price of the old tool. What should the design measure?
   - **a**: The share of customers reached in a new way by the assistant each month
   - **b**: Spend per finished task including review and rework, against a human baseline
   - **c**: The 95th percentile latency across all the requests the assistant serves
   - **d**: The count of hours saved for each person each week, summed across the whole team

10. Scenario: A research run at Pike Biotech crashes after a payment tool acts but before the run writes its result to the store. The next run starts from the store. Which two statements does the page support? (Select two.)
   - **a**: A fresh identifier is minted for the new run, with a new key
   - **b**: The unsaved task is retried under its recorded identifier
   - **c**: Results are written at the end of the run, so a crash is never half done
   - **d**: The vendor's endpoint returns the first receipt and does not pay again
   - **e**: The task is skipped and counted as done

11. Scenario: A lead agent at Rowan Research dispatches three subagents with the one-line task "look into the market", and the subagents return overlapping findings with a gap in the middle. What should the lead write instead?
   - **a**: A brief naming the objective, format, tools and boundaries
   - **b**: A longer one-line task with more adjectives about thoroughness
   - **c**: The full conversation so far, leaving no subagent without context
   - **d**: The same one-line task with the market named more precisely

12. Scenario: Mossgate Bank's assembler has only static modules left and they still exceed the token budget for the request. Which two statements describe the right behaviour? (Select two.)
   - **a**: The end of the policy is trimmed until the request fits
   - **b**: The request is sent anyway and the model discards the surplus
   - **c**: Building stops rather than a truncated policy being sent
   - **d**: The lowest-priority static block is dropped like the dynamic ones
   - **e**: Rule-bearing blocks are never dropped, whatever the ceiling

13. Scenario: A voice product at Hallow Studio peaks at 500 calls a minute, each carrying 800 fresh input tokens and producing 1,500 tokens of reply. The plan allows 30 percent spare capacity and checks Start (1,000 requests, 2 million input, 400,000 output tokens a minute) and Build (5,000, 5 million, 1 million). Which plan does it pick?
   - **a**: Scale, because tokens read from a cache count toward the input limit
   - **b**: Start, because the request rate of 650 and input volume of 520,000 are both inside it
   - **c**: The larger of those two, since generated text at 975,000 outruns the smaller one's cap
   - **d**: Start, because only the request rate of a service decides the tier

14. Scenario: Admission at Elwood Foods checks a team whose monthly allowance is 100 and whose spend so far is 90, and the next call is estimated to take exactly 10. Which decision does admission return?
   - **a**: Warn, since the total reaches the ceiling without passing it
   - **b**: Degrade to a cheaper model silently, since the team is nearly out
   - **c**: Allow without comment, since the budget is not exceeded
   - **d**: Block, since the estimate takes the team to its whole budget

15. Scenario: Harbor Lines marks its brief standing instructions for caching, the calls succeed, and the bill shows no savings from it and no error. What explains it?
   - **a**: The prefix is under the model's minimum, so the tag it carries has no effect and nothing is stored
   - **b**: The cache was written and expired between calls, so a longer lifetime is needed
   - **c**: Short prefixes need a batch request, so the calls should be resubmitted that way
   - **d**: An error was raised and missed in the logs, so the logging level must be raised

16. Scenario: Perrin Cloud's assembler holds two dynamic modules of equal priority, a long customer history and a long list of offers, and the request is over budget by a little. Which module does it drop first?
   - **a**: Neither module is dropped at all
   - **b**: The one that sits later in the final sequence
   - **c**: The one that the caller names first in the request
   - **d**: The one that holds more tokens in its current form

17. Scenario: Orlan Support picks the model for a classification workload by reading its name in the lineup, and a new release replaces the old one without a new check. Which practice does the page require for the capability the task needs?
   - **a**: Evaluate the candidates, and repeat it on each swap
   - **b**: Copy the tier from the previous model into the new one
   - **c**: Read the capability from the model's price and its position in the lineup
   - **d**: Take the highest rung of the lineup for every workload

18. Scenario: Ferrow Retail's finance lead asks for showback that takes each team's total volume and applies a single blended rate. What does the gateway design do instead?
   - **a**: Round each row first, then add the rounded rows together
   - **b**: Price the input tokens only and leave the output unbilled across teams
   - **c**: Use the average of every model's price across the teams
   - **d**: Charge each usage kind at its model's price, cache reads cheap

19. Scenario: A prompt module at Lanyard Health is marked static and holds the text "Dear {customer}" in its first line, and the team defends it as convenient. What does the assembler do, and why?
   - **a**: It moves the module behind the dynamic ones, since the order is chosen by callers
   - **b**: It refuses to build the request, since a changing prefix value breaks the cache
   - **c**: It fills the value in and caches the result, since the cache matches on meaning
   - **d**: It drops the module as the lowest priority, since a budget would cut it first

20. Scenario: A team at Selby Finance enables the variable that emits raw request and response bodies, intending only to see tool inputs. What does the page say about that setting?
   - **a**: It redacts prompts by default and so changes nothing
   - **b**: It reveals tool inputs alone, and nothing else in the payload at all
   - **c**: It exposes all other content too, including the whole conversation
   - **d**: It stays off in a project file unless the repository sets it

21. Scenario: A compliance assistant at Brannock Bank must find a policy clause, then the exception it points to, then the free-text procedure that exception names, where each lookup depends on the last result. A one-shot ranked retrieval fails on it. Which two statements does the page support? (Select two.)
   - **a**: A keyword index with a larger k is the fit
   - **b**: A cached prompt that holds the whole rulebook is the fit
   - **c**: An agent that searches issues one query after another
   - **d**: A structured query run by a tool is the fit
   - **e**: A single top-k list cannot follow a chain

22. Scenario: Valmont Care plans one gateway for forty teams and its security lead asks what the design must budget for besides licences. Which cost does the page name?
   - **a**: Nothing beyond a configuration file and a short setup guide for teams
   - **b**: Running it as infrastructure that follows what its clients send
   - **c**: A second provider key for every team that uses the gateway
   - **d**: The loss of the logs from retired teams and old projects

23. Scenario: Pelham Health's guidance changes weekly, differs by reader group and must name its source in every answer. A proposal is to train the facts into the model. What does the page conclude?
   - **a**: Retrieval from a store the team keeps current
   - **b**: Train the facts into the model once a month
   - **c**: Train them in, using the Claude API's fine-tuning service
   - **d**: Cache the facts in the prompt of each call

24. Scenario: To cut the monitoring bill, a team at Rowley Travel proposes to keep a random tenth of the data points on every time series. What does the page conclude?
   - **a**: It is wrong only for alerts, since dashboards can tolerate gaps
   - **b**: It is sound, since a random sample estimates any rate well
   - **c**: It is sound, since traces are sampled the same way in the example here
   - **d**: It is wrong, since a metric is a count and a missing count is wrong

25. Scenario: A nightly job at Farrow Clinics adds the chunks of new and changed documents to its index and reports success. A leaflet that held a patient's details was withdrawn last month, yet its text still appears in answers. Which two changes does the page call for? (Select two.)
   - **a**: Lower the number of results returned, to push the old leaflet out of the top
   - **b**: Drop a revised source's earlier copy before loading its replacement
   - **c**: Delete the records of retired sources
   - **d**: Run the same job twice a night
   - **e**: Re-embed every chunk with the same model before each run

26. Scenario: A small internal agent at Yarrow Print has six tools, every one of which is used in nearly every request, and each definition is short. An engineer proposes the tool search tool to modernise it. What does the page say?
   - **a**: Defer all six, since the cache keeps the prefix untouched
   - **b**: Keep them all loaded, since discovery adds a step and nothing to tune away
   - **c**: Split the six into two agents, since fewer tools per agent is always safer
   - **d**: Defer five of the six, since discovery is the better design for any agent

27. Scenario: A drift check at Oakley Pay stores a baseline of zero for the share of requests refused. This week the share is 3 percent, and the check divides by the baseline. What should the rule do?
   - **a**: Skip the metric entirely until a nonzero baseline has been stored again
   - **b**: Raise the baseline to the current value and then divide
   - **c**: Treat any rise from nothing as a 100-point move, with no division
   - **d**: Report the move as 3 percent of the whole traffic

28. Scenario: In a trace at Dunmore Media, one lookup was repeated four times in a row before it succeeded, and the request returned a correct answer with a normal latency. Under a tail-based rule, why might that trace still be kept?
   - **a**: Because a correct answer with normal latency is the rarest kind to find
   - **b**: Because retries of a tool are always logged as errors in the span
   - **c**: Because every successful trace is stored when it follows a failure
   - **d**: Because three or more calls of a single tool mark a loop that is worth keeping

29. Scenario: Ordway Telecom indexes eight years of call transcripts in fixed windows of 200 words, and answers quote half a line from one participant and half from the other. Where should the cut be made?
   - **a**: At a larger window of 800 words, to bring every exchange in whole
   - **b**: At the same size, with a stronger embedding model to rank the pieces
   - **c**: At a smaller window of 50 words, to put fewer sentences in a chunk
   - **d**: At each change of speaker, with long turns split at sentence ends

30. Scenario: A review at Lowden Care compares a support agent's tools with its role and plans to delete everything the role does not use, and nothing else. The role also needs a tool the agent was never given. What does the page say about the review?
   - **a**: It is complete
   - **b**: It must add logging for every tool left
   - **c**: It must also report the shortfall
   - **d**: It must remove the missing tool from the role description, to make the two agree

31. Scenario: Hendry Stores' agent is offered 120 tools through a search tool, and every request starts with a search because all tools are deferred. Latency is poor. Which configuration does the guidance advise?
   - **a**: Replace the search tool with shorter tool descriptions for all 120
   - **b**: Load the three to five most used definitions and defer the rest
   - **c**: Defer nothing, loading every definition up front
   - **d**: Defer the most used ones too, along with the rest

32. Scenario: Ashgrove Bank proposes to test a new model on half of live traffic for a month to find out whether refund answers regress. Which two statements does the page support? (Select two.)
   - **a**: Release to half of users, which gives the clearest data
   - **b**: An A/B test on all users comes first and alone predicts reaction
   - **c**: Exposure to real users follows once the shadow run says the change is safe to show
   - **d**: A shadow run exposes no user to a degradation
   - **e**: Compare average segment accuracies, which summarise the change

33. Scenario: A change at Ravel Freight is refused by the gate on a refund case that must pass, and the old model's retirement is 40 days away. The lead proposes to ship anyway. What does the page say?
   - **a**: Ship it as proposed and note the failure in the release
   - **b**: Re-weight the failing case until the average rises
   - **c**: Make it a work list, one owner for each reason
   - **d**: Watch the failure in production and fix it later

34. Scenario: Dovedale Insurance's assistant answers a coverage question with a claim the policy extract does not make, though the right passage was in the prompt and the reply has the right shape. Where does the diagnosis place the fault, and what does it try?
   - **a**: In retrieval, by rebuilding the index with a different chunk size and overlap
   - **b**: In the model, by moving to a larger one from the same family
   - **c**: In format, by adding an output schema with stricter field types
   - **d**: In grounding, by permitting an admission of ignorance and quoting first

35. Scenario: A team at Merrin Air trims its instructions and switches to a smaller model to cut delay before any evaluation of the full-strength system exists. Which order does the documentation advise?
   - **a**: Reduce latency first, then judge the quality of the answers afterwards on a sample
   - **b**: Reduce latency only after the prompt performs well without constraints
   - **c**: Reduce latency, measured only on the mean across all requests
   - **d**: Reduce latency and measure quality together in a single combined step

36. Scenario: Eskdale Telecom's gate measures the 95th percentile of the new model's timings on forty cases against its limit. One run shows a single slow case, and a second run shows three. What should the check do?
   - **a**: Pass at one, refuse at three, since a lone outlier is tolerated
   - **b**: Pass both, since the mean is within the limit
   - **c**: Refuse neither, since latency cannot gate a change
   - **d**: Refuse both, since any slow case breaks the limit for the worst user

37. Scenario: A review at Quill Telecom runs the launch rubric on a design where a human review costs 60 and a wrong decision costs 50. What accuracy does the needed-accuracy rule return?
   - **a**: Minus 20, since the check exceeds the error by that percent
   - **b**: Zero, since the check never pays and the result is clamped
   - **c**: Eighty, since the error is 80 percent of the check
   - **d**: A hundred, since a check this dear needs a perfect model

38. Scenario: Tolley Foods can grade its label-type answers by code, and its engineers plan to hand-grade thirty polished cases instead of running six hundred machine-graded ones, because hand grading has higher quality. What does the documentation advise for those outputs?
   - **a**: Hand-grade thirty, since quality is the property that makes a set trustworthy
   - **b**: Wait for a few thousand cases before grading any, since small sets mislead
   - **c**: Run the broad automated set, since breadth beats refinement here
   - **d**: Hand-grade all six hundred, since people are the only valid judges

39. Scenario: Corran Health's release gate for its claims assistant measures accuracy, latency and cost, and an audit finds that prompt-injection attempts reach the tool layer. Which addition does the guide's list of dimensions require?
   - **a**: A user rating collected after each answer, averaged by month and by team name
   - **b**: A higher accuracy floor on ordinary cases, checked on the same sample
   - **c**: A mean latency per request, reported with the median for comparison
   - **d**: The share of hostile inputs that succeeded, on cases built to provoke them

40. Scenario: Tavern Group moves an assistant to a model whose replies include adaptive thinking, and many replies are now cut off mid-sentence though max_tokens is unchanged. Which two statements explain it and the fix? (Select two.)
   - **a**: Thinking is off by default, so a safety filter stops the reply
   - **b**: The reply format changed, so the parser must read the first block as text
   - **c**: Reasoning shares the same ceiling as the visible text
   - **d**: Raise the ceiling, recount the usage and re-baseline cost
   - **e**: The prices changed, so the limit must be lowered to keep the old bill

41. Scenario: At Linford Retail an A/B test with 500 cases in each arm shows 410 right for the current prompt and 431 right for the new one, and the planned minimum was 200. The sponsor wants the new prompt shipped. What does the two-proportion test at 95 percent conclude?
   - **a**: The new prompt is better, since 431 is more than 410 on a sample over the minimum
   - **b**: Too few cases, since a test of this kind needs thousands per arm
   - **c**: The old prompt is better, since a clear test can only favour the version in use
   - **d**: No clear difference, since a gap of that size on that sample is within chance

42. Scenario: A fairness review at Stanton Mutual wants a parity report across customer groups, but the system holds no field that labels a customer's group. What does the page say about the report?
   - **a**: The cohorts must be defined where they can be measured, or the gap hides
   - **b**: An overall average can stand in, covering every group
   - **c**: Skipping it is the right course for the report now
   - **d**: The gap fixes itself once the labels exist, with no owner needed

43. Scenario: Beacon Clinics plans to process protected health information on Claude Platform on AWS, and its design note says that zero data retention covers the HIPAA obligation. Which two statements does the page support? (Select two.)
   - **a**: One arrangement covers both, and zero retention implies the agreement
   - **b**: Neither matters, and de-identified input removes the duty
   - **c**: Both are offered there, and the platform shares the Claude API's policy
   - **d**: The agreement-based one is offered on the first-party API alone
   - **e**: Storing nothing and holding a contract are two different arrangements

44. Scenario: A support assistant at Fenwick Mutual must tell readers where an answer came from, and the team wants a design that shows the basis without storing the answer in the audit trail. Which elements does the page give?
   - **a**: The full prompt and reply stored in the trace for later audit
   - **b**: A line saying that the model is confident in its answer
   - **c**: The source quote, the document version and a contact person
   - **d**: Nothing beyond a disclosure that AI helped write the answer

45. Scenario: Ingram Bank's design puts three strong controls on the input layer, a harmlessness screen, injection patterns and an untrusted-content rule, and none elsewhere. A confident wrong answer and a rogue tool call both occur in testing. What does the review conclude?
   - **a**: Other tiers are bare, so add output, action and monitor safeguards
   - **b**: A longer system prompt is needed to carry all of the rules
   - **c**: A fourth input control is needed for tool arguments
   - **d**: It is sound as it stands and needs no further layers added later

46. Scenario: Calloway Mutual's policy caps log retention at 365 days. On the night, the purge job meets a record stored for exactly 365 days, and nobody has placed a hold on it. What does the job do?
   - **a**: Keeps it forever, since an entry at the edge is under a hold
   - **b**: Removes it after a warning, since the limit is a soft one
   - **c**: Keeps it, since removal needs an age beyond the limit
   - **d**: Removes it, since the limit has been reached

47. Scenario: A design review at Norwood Energy hears the claim that the assistant is GDPR compliant, resting on the model vendor's reputation. What should the architect ask to see?
   - **a**: A signed statement from the vendor about the model's handling of data
   - **b**: A line in the system prompt that names the regulation in full
   - **c**: A passing score on the accuracy suite for the assistant itself
   - **d**: The requirement, the part of the system meeting it, and the evidence

48. Scenario: Hartwell Group's control that checks reply format at the output layer, an ordinary-tier control, is down for an hour while traffic flows. Which two statements follow the page's rule? (Select two.)
   - **a**: Every reply is held until the check returns
   - **b**: The outage counts as a pass and leaves no trace
   - **c**: The input screen is switched off as well
   - **d**: The record keeps the gap visible
   - **e**: Replies go on carrying a mark that the guard did not run

49. Scenario: A user of Pellew Insurance's assistant triggers refusals eleven times in a day, each ending with a refusal stop reason. What does the design do with that count?
   - **a**: Tell the person it breaches policy, then throttle or end access
   - **b**: Raise the sampling temperature to make the replies differ next time
   - **c**: Show the category of each refusal to the user in a message
   - **d**: Ignore the count and leave the reply path unchanged

50. Scenario: The tokenising layer at Wyndham Care replaces each e-mail address with a placeholder, and a reviewer asks where the map from placeholder to address lives and whether one person keeps one placeholder. What does the design say?
   - **a**: In the provider's logs, to let the model restore each value in its reply
   - **b**: In a vault held only by the caller, and equal values get one stand-in each
   - **c**: In the system prompt, to let the model reason about who wrote twice
   - **d**: In a fresh placeholder for each mention, leaving no pattern to be learnt

51. Scenario: A sponsor at Addison Travel says the new assistant must be quick. The agents who use it sit at a screen, and a nightly job also calls it. What should discovery write down for the agents?
   - **a**: A target of fast, since the sponsor chose the word
   - **b**: The mean time, since it is the one figure that everybody knows well
   - **c**: A minutes-long allowance, since the nightly job sets the pace
   - **d**: A 95th-percentile ceiling like 2 seconds, since a person waits

52. Scenario: A handover note at Oldham Health gives an owner and a runbook for a new assistant, and one monitor, the error rate, compared with a baseline. What does the hand-off section still lack?
   - **a**: A named vendor contact who can be reached after hours
   - **b**: A statement that the design cannot change after launch, to keep it stable
   - **c**: A second signal, read against the old figures both ways, and a rollback
   - **d**: A third document, the architecture diagram of the whole service

53. Scenario: A sponsor at Penrose Care insists on a single accuracy figure for the board, though the system answers three kinds of request with very different error costs. Which figure does the page advise giving?
   - **a**: The overall average across all three kinds of request
   - **b**: The best segment's figure, quoted with the sample that produced it
   - **c**: No figure at all until the system has been in use a year
   - **d**: The number for the decision they face, with what it leaves out

54. Scenario: An architect at Dalby Foods shows the sponsor only the option she recommends, and the sponsor asks why nobody considered the simple thing. Which two statements does the page support? (Select two.)
   - **a**: Four or more alternatives belong in the record, the weak ones among them
   - **b**: Each alternative has a monthly cost, a verdict on the service levels, a status and a reason
   - **c**: A longer argument for the recommended option is the cure
   - **d**: Two alternatives suffice, the recommended one and its nearest rival
   - **e**: Every option the vendor ever listed belongs in the record, ranked by novelty

55. Scenario: Two months after launch, the review at Brandt Telecom finds that a new customer segment has appeared and a threshold no longer fits. A manager reads this as proof that the original design failed. How does the page frame it?
   - **a**: As a reason to freeze the design until the next sponsor review
   - **b**: As planned iteration
   - **c**: As a reason to restart discovery
   - **d**: As a failure

56. Scenario: After three months of work, an architect at Hollis Rail cannot show the sponsor any saving, though the assistant clearly handles disputes. The discovery notes record the volume and the error cost. Which discovery question was left out?
   - **a**: What exists today, with its price and quality, as the baseline
   - **b**: Who is accountable, so that someone can be telephoned
   - **c**: What counts as done, with a measure, a threshold and an owner
   - **d**: What is out of bounds, such as data that may not leave a region

57. Scenario: A design record at Westmere Health lists two options as recommended, both cheaper than the baseline and both within the service levels, so that the sponsor can pick the one that suits. What does the review say?
   - **a**: Keep both, since a choice gives the sponsor a sense of control
   - **b**: Name a single winner, since two endorsements push the choice back to the reader
   - **c**: Recommend the more capable of the two, since capability is the safer reason
   - **d**: Remove the options that were not recommended, since a record is an announcement

58. Scenario: A service record at Merton Care names an accountable party as "the project team", and the incident review cannot find anyone to call at night. What does the page say should have been written?
   - **a**: The vendor, named as the supplier of the underlying model
   - **b**: The sponsor, named as the executive who approved the work
   - **c**: A role reachable by phone, such as the billing operations manager
   - **d**: A committee, named as the group that reviews every incident each week

59. Scenario: A launch review at Garston Retail runs the rubric on a design that promises "fast replies" with no figure, though an owner and the needed accuracy are stated. What does the rubric record?
   - **a**: A medium finding for no latency number, so the verdict is revise
   - **b**: No finding at all, so the verdict is approve as proposed
   - **c**: A low finding for unstated accuracy, so the verdict is approve
   - **d**: A high finding for no accountable owner, so the verdict is reject

60. Scenario: Dunmow Group sets an organisation spend limit of 20,000, group limits of 6,000, 8,000 and 6,000, and gives one member a ceiling of 9,000 in the 6,000 group. Which two statements does the page support? (Select two.)
   - **a**: The groups together exceed the organisation, which breaks the sum
   - **b**: That individual figure is unreachable
   - **c**: The organisation limit is a safety net that makes the other figures optional
   - **d**: The three shares sum to the grand total exactly
   - **e**: Nothing is wrong, and a member may exceed a group's limit with consent

61. Scenario: Rowan Labs has no device management and sets its policy in the admin console, and its security lead wants a stricter policy for the contractors' group alone. What does the page say?
   - **a**: Ask any administrator to edit the setting for the contractors' group
   - **b**: Set it in the shared project file that every contractor checks out
   - **c**: It cannot target a subset yet, so a separate profile goes to them
   - **d**: Set the stricter values in the console for the group of contractors

62. Scenario: Kinsale Software wants developers to install plugins only from its own marketplace repository, and an engineer sets the managed allowlist of marketplace sources to an empty list to be safe. What happens?
   - **a**: Every origin is blocked, the firm's and the official one alike
   - **b**: Every source is allowed, the firm's own and any public one
   - **c**: Only the official source is allowed, and the firm's own is refused
   - **d**: Only the company's source is allowed, and the official one is refused

63. Scenario: A platform team at Larkin Pay enables Claude Code for an organisation with zero data retention and promises leadership a dashboard of merged pull requests with assistance. What should it check first?
   - **a**: The baseline of four weeks, since the metrics begin once the baseline period ends
   - **b**: Whether those contribution metrics are available, since they need the GitHub app
   - **c**: The count of lines accepted, since that is the outcome that matters
   - **d**: The seat allowance, since it limits how many merges are recorded

<details>
<summary>Answer key</summary>

1. **a**. The key belongs to the tool's design as much as the agent's, so when a tool that changes things offers none, the architect wraps it with one or does not let an agent retry it (module 81, page 1). The danger is the lost response after the action happened. The page puts it as "When a tool that changes things offers no key, the architect wraps it with one, or does not let an agent retry it". *b* is ruled out because "the agent cannot see an attempt whose response was lost", and a control that must hold is enforced in code. *c* is ruled out because "An unlimited retry is a way to spend money while nothing changes." *d* is ruled out because "repeats the change unless the tool can tell that this attempt is a repeat"
2. **c**. Ten volunteers sent clean cases, but production brings a long tail of odd, long, multilingual or hostile inputs, so the test is to sample real inputs and evaluate by slice (module 79, page 2). A plan that lists the assumptions, the evidence and "the trigger that stops the roll-out" is a plan. *a* is ruled out because the page rejects it: "Scale the pilot as it is and buy more licences". *b* is ruled out because the page says "None of the rows is about the model getting worse". *d* is ruled out because that tests a different assumption: "People covered the edge cases".
3. **d**. The input stage holds where requests come from, how they are validated and bounded and what is stripped, and a design that sends raw text straight to the model lacks it, which is a high finding (module 80, page 1). The rubric rejects on any high finding. The page puts it as "Where requests come from, how they are validated and bounded, what is stripped before the model sees it". *a* is ruled out because the stem lists the other stages as present, and the rubric raises a high finding when "A stage is empty or absent", whichever other stages are strong. *b* is ruled out because a missing stage is a high rule and "any `high` gives `reject`, otherwise any `medium` gives `revise`", so it cannot stop at a medium finding. *c* is ruled out because a low finding "is recorded and does not hold the design back", and a missing input stage is not a matter to improve later.
4. **b**. A person can only hold the line when the design makes the review real, which means they see the evidence and not only the model's verdict (module 79, page 2). The page warns that the interface must not turn review into "a click-through". The page puts it as "they see the evidence, not only the model's verdict". *a* is ruled out because the page says "people who receive output directly are told that AI helped produce it", and the handler is not one of them. *c* is ruled out because the page ties the target to "an item is worth reviewing when its expected error cost exceeds the review cost", and not to keeping handlers busy. *d* is ruled out because the usage policy asks that "a qualified professional in that field must review the content or decision prior to dissemination or finalization", and a second model is not a person.
5. **c**. Autonomy without need is conditional on a second fact, a known path, and here the path depends on what is found (module 93, page 2). The page says "A rule that fires without its second fact is a false alarm". *a* is ruled out because that rule concerns "a team's value is below 15 chats' worth", and this design has no team. *b* is ruled out because the page says "an agent is only flagged when the path is known", and a rule that fires without its second fact is "a false alarm". *d* is ruled out because the page raises that when "A stage is empty or absent", and an agent's processing is present.
6. **d**. A control that must always hold is enforced in code, because the model's behaviour is probabilistic (module 79, page 2). The page adds that "a regulatory control never rests on the model alone", whatever the test score was. *a* is ruled out because the page says to "Enforce in code any control that must always hold, such as a spending limit or a mandatory approval", and a tool description is only text the model reads. *b* is ruled out because the page says "Probabilistic behaviour cannot give a guarantee, however high the measured accuracy". *c* is ruled out because people are kept for "decisions that carry accountability, cannot be undone or fall under a rule that demands a human", whereas a control that must always hold is enforced in code.
7. **a**. The pillar of agreed targets such as latency, availability and accuracy by case type is performance service levels, and it shapes model tier, caching, concurrency and accept-and-poll (module 79, page 1). The other three each claim a different kind of value with a different measure. The page puts it as "Shapes model tier, caching, concurrency and accept-and-poll". *b* is ruled out because the page defines cost as "Work costs less in total", measured against the human baseline. *c* is ruled out because the page defines it as "People get more done", measured in hours saved per person per week. *d* is ruled out because the page lists efficiency as "The same work, with less effort per unit", measured by handling time per claim.
8. **b and e**. For disputes the design is an assistant that gathers the file for the person, and the example concludes "No decision calls for a team of agents" (module 79, page 1). The page says "The model prepares the file; a qualified person decides." *a* is ruled out because "Automate decisions that are reversible, cheap to get wrong and easy to audit afterwards". *c* is ruled out because a team of agents fits only where "The parts are independent, the volume of reading exceeds one context and the value pays for the cost", and here a person decides each dispute anyway. *d* is ruled out because "Only then does autonomy earn its price", and a fixed order suits only a path known in advance.
9. **b**. The cost pillar claims that work costs less in total, and its measure is the cost per completed task against the human baseline, counting tokens, review time and rework and not model price alone (module 79, page 1). The page puts it as "Counts tokens, review time and rework, not model price alone". *a* is ruled out because that is the transformation pillar, "Share of customers served in a new way". *c* is ruled out because a latency figure belongs to another pillar, whose measure is "95th percentile latency, availability, accuracy by case type", and it says nothing of cost per task. *d* is ruled out because that is the productivity pillar, "Hours saved per person per week", which claims a different kind of value.
10. **b and d**. A task whose effect happened but whose result was not stored is retried with the same key, and the tool returns the recorded result and does not act again (module 81, page 2). The page says "The checkpoint and the idempotency key cover the two halves of the same gap." *a* is ruled out because "The key names the intention, not the attempt." *c* is ruled out because "Write on success, immediately." *e* is ruled out because "A run that starts with a store takes every task found there as done", and this task's result never reached the store, so it is run again under its key.
11. **a**. A subagent sees only what it is given, so the lead writes the task like a brief, with the objective, the output format, the tools and sources, and the boundaries (module 80, page 2). That keeps "two subagents do not research the same thing or leave a gap between them". *b* is ruled out because "the lead must put everything the task needs into the task description", and adjectives about thoroughness add none of it. *c* is ruled out because "Give every subagent the whole conversation so nothing is lost" is the page's trap, since the subagent's small context is why it exists. *d* is ruled out because "A subagent starts with only what it is given."
12. **c and e**. The static modules are never dropped, and when only they remain and still exceed the budget the assembler refuses the request and does not send a truncated policy (module 82, page 1). The page calls a silent truncation "worse than a refusal". *a* is ruled out because "When the request is too long, truncate the end of the system prompt" is rejected, because "static modules hold the rules and are never cut". *b* is ruled out because "a request that exceeds a budget must lose something" and the page says to "Decide beforehand what: each dynamic module has a priority", so the cut is made before sending and not left to the model. *d* is ruled out because "The static modules are never dropped, since they hold the rules".
13. **c**. The needed figures are 650 requests, 520,000 input tokens and 975,000 output tokens a minute, and the output side exceeds Start's 400,000 while fitting Build's 1 million (module 84, page 1). The page says "the limit that fails first is the binding one". *a* is ruled out because "cache reads do not count toward ITPM", and nothing here is read from a cache. *b* is ruled out because the output side is the third limit: "the workload fits a tier only if all three fit". *d* is ruled out because the page rejects it: "Size the service on requests per minute; tokens are the model's business."
14. **a**. The edges are exact: a request that brings the spend exactly to the budget does not block, and at or beyond 80 percent it warns (module 84, page 2). The page's order is "warned, then degraded, then stopped". *b* is ruled out because degrading is a routing step with its own rule, and admission returns "allow, warn or block". *c* is ruled out because "Reaching 80 percent warns", and the team is already beyond that mark. *d* is ruled out because "one that brings it exactly to the budget does not" block.
15. **a**. Below the model's minimum the marking "has no effect and does not report an error", so the cache stays inactive and shows up only in the bill (module 82, page 1). *b* is ruled out because "A cache that was never active shows up only in the bill", so nothing was written that could expire. *c* is ruled out because the check is "to read the usage figures the API returns for cache writes and cache reads", and not to change the call type. *d* is ruled out because "A cache that was never active shows up only in the bill", so the logs hold no error to find.
16. **b**. Equal priorities are broken by position, and the later module goes first (module 82, page 1). The page's rule is that "the assembler drops the lowest first, and among equal priorities the later one". *a* is ruled out because "a request that exceeds a budget must lose something", and a tie in priority is settled by position and not by dropping nothing. *c* is ruled out because "The order is the same every time", and "callers cannot" decide it. *d* is ruled out because the page orders the cut by rank and not by size: "each dynamic module has a priority".
17. **a**. The tier a task needs is found by evaluating the task, not by reading a model's name, and it can change when a model is replaced (module 82, page 2). The page adds that price "can only rank models that are already right". *b* is ruled out because the page says the needed tier "can change when a model is replaced". *c* is ruled out because the page says price does "Nothing alone; it ranks the models that remain", so it cannot show what a model can do. *d* is ruled out because "Use the largest model for every workload" is rejected, because the requirement is a rung, a speed limit and a price.
18. **d**. Showback is computed from usage rows by token kind at the price of the row's model, and the total is divided by a million and rounded once at the end (module 84, page 2). A bill that ignores the kinds is wrong. The page puts it as "Every token kind has its own price". *a* is ruled out because "rounding each row first gains or loses cents". *b* is ruled out because leaving one kind out misstates the bill: "a bill that ignores either is wrong". *c* is ruled out because an average hides what the page states: "cache reads are cheap, output is dear".
19. **b**. A static module that holds a variable is refused when the request is assembled, because a value that changes in the prefix breaks the cache (module 82, page 1). The page's rule is that "Variables belong in dynamic modules only." *a* is ruled out because "The order is the same every time", and "callers cannot" decide it. *c* is ruled out because the page says "Two requests share a cache entry only when everything up to the breakpoint is identical, character for character." *d* is ruled out because "The static modules are never dropped, since they hold the rules"
20. **c**. The variable that emits raw bodies implies consent to everything the three narrower variables would reveal, because the bodies include the entire conversation history (module 87, page 1). The page puts it as "include the entire conversation history". *a* is ruled out because the defaults say "Spans redact user prompt text, tool input details, and tool content by default", and this setting overrides that. *b* is ruled out because the page says it "implies consent to everything" the other three would reveal. *d* is ruled out because "a repository can't use them to turn telemetry on, choose where it goes, or capture content".
21. **c and e**. When the next query depends on the last result, the mechanism is an agent that searches one query after another, because "A single top-k list that cannot follow a chain" is the failure the page names (module 85, page 1). *a* is ruled out because a larger k is still one list from one index, and "One index's misses are never seen". *b* is ruled out because the page reserves that for a corpus "under about 200,000 tokens". *d* is ruled out because that mechanism fits when "The answer lives in a table or a database".
22. **b**. The page states the cost plainly: the gateway becomes infrastructure the organisation operates, and it has to forward what its clients send as they change (module 86, page 1). The page puts it as "The tradeoff is that the gateway becomes infrastructure your organization operates". *a* is ruled out because the gateway is "a proxy your organization runs between the clients and the provider", which is a service to operate and not a file and a guide. *c* is ruled out because the page says "the provider key stays server-side; developers hold gateway credentials instead". *d* is ruled out because "every decision, the refusals included, leaves a record", so the logs are a gain and not a cost.
23. **a**. Facts that change weekly, belong to different readers and must be cited are retrieval's case: a weight cannot be edited for one document, name its source or be taken from one reader (module 85, page 2). The page puts it as "a weight cannot be edited for one document, cannot name its source and cannot be taken away from one reader". *b* is ruled out because "Facts change, a source must be shown or a reader's access must be respected" are the cases where the weights are weak. *c* is ruled out because the glossary says "The Claude API does not currently offer fine-tuning". *d* is ruled out because that suits "a small, stable body of rules shared by every request", which weekly guidance is not.
24. **d**. Metrics are counts, so dropping a random share of points gives a wrong rate, and the lever for cost is fewer labels with many values (module 87, page 2). The page puts it as "metrics are counts, and a missing count is a wrong count". *a* is ruled out because the page rejects "a random sample of metric points" for every use of a rate. *b* is ruled out because the page puts the cost in labels: "Metrics are cheap to keep and costly when a label has many values", so thinning the points is not what makes them dear. *c* is ruled out because the example keeps traces by rule: "Tail-based sampling decides when the trace is complete".
25. **b and c**. A re-index has four outcomes: it keeps unchanged documents, replaces changed ones, adds new ones and removes withdrawn ones (module 85, page 2). For a withdrawn document "the removal is a correctness and, for personal data, a compliance matter, not a tidiness one". *a* is ruled out because the old chunk "competes with the new one for a place in the top results". *d* is ruled out because "A re-index that runs less often than the documents change leaves old text in the index between runs". *e* is ruled out because "Nothing in a document refresh touches the model, its sampling settings or the size of its window", so re-embedding with the same model leaves the withdrawn text where it was.
26. **b**. Where the tool set is small, loading everything is the better design, with no extra step and nothing to tune (module 86, page 2). The documentation lists "10 or more tools" and "tool definitions consume more than 10k tokens" as the signs for search. *a* is ruled out because deferring pays on "a large or growing" set, and here it adds a round trip when the needed tool is absent. *c* is ruled out because the documentation puts the drop at "Claude's ability to pick the right tool degrades once you exceed 30–50 available tools", and six is far below that. *d* is ruled out because the documentation says standard calling is "a better fit when you have fewer than 10 tools, every tool is used in every request, or your tool definitions are small".
27. **c**. A move from zero is treated as a move of 100 percent, and the check never divides by it, so the metric is flagged beyond tolerance (module 87, page 2). The page puts it as "the example treats any move from zero as a move of 100 percent, and never divides by it". *a* is ruled out because "A metric whose baseline is zero needs a rule of its own", and silence is not one. *b* is ruled out because "to treat a move you did not plan as a finding", and resets are made at a change you decided on. *d* is ruled out because the check computes "the relative change in whole percent", and a change from zero has no relative size.
28. **d**. The retries rule keeps a trace in which one tool was called three times or more, a sign of a loop that eventually succeeded (module 87, page 1). It ranks below error and slow and above feedback and the sample. The page puts it as "one tool was called three times or more, a sign of a loop that eventually succeeded". *a* is ruled out because the rule keeps "the rest, kept by a share of their ids", and this trace has a reason of its own. *b* is ruled out because the page lists the reasons apart: "error: any span failed" is one, and a retried call belongs to the retries reason, for a loop that eventually succeeded. *c* is ruled out because "The example keeps a trace for the first of these reasons, in this order", so a success earns a place only as part of the sample.
29. **d**. The cut follows the joints of the data, and for a transcript the joint is the speaker turn, with a unit longer than the limit split at sentence ends (module 85, page 1). The page says "Size then follows from the unit". *a* is ruled out because the page asks for the cut to follow the data: "A heading, a clause number, a function, a table row and a support ticket are the joints of their kinds of data". *b* is ruled out because "changing the index changes how the pieces are scored, not where they were cut". *c* is ruled out because "A smaller window cuts in more places and separates more sentences from their neighbours."
30. **c**. The audit compares what an agent holds with what its role needs and returns four lists: tools to remove, needed tools it lacks, risky ones named first, and dormant ones to review (module 86, page 2). A review that only removes will break the role. The page puts it as "The needed tools the agent lacks are listed too". *a* is ruled out because "a review that only removes will break the role", and a review that stops at removal is not complete. *b* is ruled out because "Logging and confirmation prompts are compensating controls", and they do not replace the audit of what is held and needed. *d* is ruled out because the audit "compares what an agent holds with what its role needs", so the role is the reference and the shortfall is reported and not edited away.
31. **b**. The common path stays loaded so that most requests never search, and the long tail is deferred, where a search costs less than the definitions would on every request (module 86, page 2). The page names a floor of three to five loaded tools. The page puts it as "most frequently used tools non-deferred so Claude can call them without searching first". *a* is ruled out because "Shorten every tool description to save tokens" fails because it "trades selection quality for tokens". *c* is ruled out because "Past the 30 to 50 tools" accuracy falls, so loading all 120 trades one cost for another. *d* is ruled out because "when the tool that is needed is not loaded, the model calls the search first, and that is a round trip", and deferring the common tools makes that happen on most requests.
32. **c and d**. A shadow run sends copies of live requests to the new version and keeps the answers away from users, and "It comes first; an A/B test comes after it, once the shadow run says the change is safe to show" (module 88, page 2). *a* is ruled out because "A change that ships to half of the traffic exposes half of the users to the regression before anyone has measured it, so that is not a gate." *b* is ruled out because only a shadow run "keeps the answers away from users", whereas an A/B test "sends live traffic to two versions at random", so on all users it cannot come first. *e* is ruled out because "An average of segment accuracies weights a rare segment like a common one and hides its price."
33. **c**. A refused gate with a retirement date in 40 days becomes a work list with an owner for each reason (module 89, page 2). The page adds that the gate "says what must be fixed or accepted first". *a* is ruled out because for a must-pass case "If any fails, the change is refused", and a note in the release does not change that. *b* is ruled out because "Re-weighting a failed case turns a rule into a preference". *d* is ruled out because "watching a known failure in production makes customers the test".
34. **d**. The evidence was there and the answer claims more than it says, so the fault is grounding (module 88, page 2). The documentation's remedies are permission to say it does not know, quotes extracted first and a citation for every claim. The page puts it as "permission to say it does not know, quotes extracted first, a citation for every claim". *a* is ruled out because the order begins "Was the evidence found?", and here the passage was supplied. *b* is ruled out because "A larger model does not know a document it was never shown", and the model comes last because it is the most expensive fix. *c* is ruled out because "Format and instruction failures are cheap to see and cheap to fix in the prompt", but the stem says the shape was right.
35. **b**. The floor comes first: a prompt that works well without model or prompt constraints shows what top performance looks like, and latency reduction follows (module 88, page 2). The documentation warns that reducing it prematurely can hide that. The page puts it as "Trying to reduce latency prematurely might prevent you from discovering what top performance looks like". *a* is ruled out because "Optimisation is a search for the cheapest and fastest system that still meets the floor, so the floor comes first." A check of quality afterwards on a sample comes too late. *c* is ruled out because "A mean hides the tail", and "the slowest five in a hundred requests are the ones users complain about". *d* is ruled out because the order is "first engineer a prompt that works well", and two changes at once blur which one moved a score.
36. **a**. The check is the nearest-rank 95th percentile, and a single slow case among forty does not block the change while three do (module 89, page 2). The example uses the percentile and not the worst case. The page puts it as "A single slow case among forty does not block the change, and three do". *b* is ruled out because "A mean hides the tail", and the slowest five in a hundred are what the check is for. *c* is ruled out because the page lists "Latency. The tail, by the nearest-rank 95th percentile of the new timings" as a check of the gate. *d* is ruled out because "The example's check is the percentile and not the worst case", so one slow case does not break it.
37. **b**. The accuracy is 100 minus the review cost as a percent of the error cost, rounded up and never below 0, so a check that costs as much as an error or more needs no accuracy (module 93, page 2). The result stays 0 and does not go negative. The page puts it as "A check that costs as much as an error, or more, needs no accuracy". *a* is ruled out because "the result stays 0 and does not go negative." *c* is ruled out because the rule rounds the percent "up, and never below 0", and 80 is not a result of it. *d* is ruled out because the formula is "100 minus the review cost as a percent of the error cost", and the result is clamped at zero.
38. **c**. For outputs that a machine can grade, the page prefers volume to polish, quoting the documentation that more questions with slightly lower signal and automated grading are better than fewer hand-graded ones (module 88, page 1). Code grading is "Fastest and most reliable, extremely scalable". *a* is ruled out because the documentation says "More questions with slightly lower signal automated grading is better than fewer questions with high-quality human hand-graded evals." *b* is ruled out because "We often hear that AI developer teams delay creating evals because they believe that only large evals with hundreds of test cases are useful." *d* is ruled out because human grading is "Most flexible and high quality, but slow and expensive. Avoid if possible."
39. **d**. A release gate needs a number for each dimension that can fail it, and for security that number is the share of injection or leak attempts that got through, on attack cases (module 88, page 1). The page lists five dimensions: accuracy, latency, cost, safety and security. The page puts it as "the share of injection or leak attempts that got through, on attack cases". *a* is ruled out because "A criterion must be one you can fail", and a monthly average of ratings sets no number that can fail a release on hostile inputs. *b* is ruled out because the guide lists five dimensions, and "Safety" and "Security" are separate from "Accuracy: the share of cases answered correctly, by case type". *c* is ruled out because "Latency: a percentile and not a mean", and speed says nothing of what got through.
40. **c and d**. On this model max_tokens covers thinking plus text, and "thinking tokens are billed as output tokens", so a limit sized for the old model cuts replies short (module 89, page 1). The checklist says to "Recount tokens and re-baseline cost". *a* is ruled out because "a request with no `thinking` field runs with adaptive thinking". *b* is ruled out because the page says "Read content blocks by `type`", which is a different failure from a cut-off reply. *e* is ruled out because "a budget that cannot change is a decision to stay on a model whose date is fixed", and a lower cap would cut more replies short.
41. **d**. The page gives the contrast: 410 right of 500 against 438 is a clear gain, and 410 against 431 is not (module 88, page 2). The sample is above the minimum, so the verdict is that the gap is within chance. The page puts it as "410 right of 500 against 438 is a clear gain, and 410 against 431 is not". *a* is ruled out because "The larger number is not the better version." *b* is ruled out because "The example's minimum is 200; below it the verdict is `too few cases`", and 500 is above it. *c* is ruled out because "A clear difference can go either way", and a test that can only report wins "is not a test".
42. **a**. The report computes one metric per group and shows the gap, and the groups must be defined where they can be measured, since a segment you cannot label cannot be reported (module 90, page 2). The page puts it as "The groups must be defined where they can be measured". *b* is ruled out because "a headline average hides a failing group exactly as it hides a failing segment". *c* is ruled out because "Bias, in the guide's wording, is a property of outcomes." *d* is ruled out because "The report does not fix anything on its own", which is why the register gives it an owner.
43. **d and e**. Zero data retention means Anthropic does not store prompts or responses at rest, while HIPAA readiness is a signed agreement and a setting that applies to the Claude API only (module 83, page 1). The page says "Zero data retention and HIPAA readiness are different things". *a* is ruled out because the two are obtained differently: zero retention is requested from Anthropic, while HIPAA readiness needs "The business associate agreement signed and the setting enabled". *b* is ruled out because "Neither arrangement removes the need to de-identify". *c* is ruled out because HIPAA readiness applies to "The Claude API only, not Claude Platform on AWS or Microsoft Foundry".
44. **c**. Transparency has two duties: tell people AI helped, and show the basis, which means the quote the grounding check used, the document version and the way to reach a person (module 90, page 2). The page puts it as "A trace that holds ids and sizes can show which document supported an answer without holding the answer". *a* is ruled out because the log holds "request id, action, consequence, outcome and the size of the text, and never the text itself". *b* is ruled out because "confidence is not evidence", and a model can be certain and wrong. *d* is ruled out because "Two duties travel together", and the second is to let a reader see the basis of an answer.
45. **a**. The layers answer different attackers and mistakes, so a design with all its controls on one layer has a gap on the others (module 90, page 1). The review adds an output control, an action control and a monitor. The page puts it as "An input control cannot catch a confident wrong answer". *b* is ruled out because "the model is told in the system prompt" is "a weak instance of one layer". *c* is ruled out because "an output control cannot stop a tool call that already happened", and the reverse holds for input. *d* is ruled out because "A design with all its controls on one layer has a gap on the others", and the two failures in testing fall on the other layers.
46. **c**. Purging follows the same precision as the check: an entry is removed only when it is more than the ceiling old, so one exactly at the ceiling stays (module 83, page 2). A retention equal to a limit is acceptable. The page puts it as "an entry is removed only when it is more than the ceiling old". *a* is ruled out because a hold is a separate matter: "an entry under a legal hold is never removed, whatever its age". *b* is ruled out because the page treats a ceiling as a limit, and "a retention equal to a limit as acceptable". *d* is ruled out because the window allows "the days actually kept, which lie between them, the ceiling itself allowed", so a record at exactly 365 days is within it.
47. **d**. A claim about a regime needs a source: which requirement, which part of the system meets it and what evidence shows that (module 90, page 2). The page points to a trust centre for the compliance material. The page puts it as "A claim about a regime needs a source". *a* is ruled out because "A model does not make a system compliant". *b* is ruled out because a regime is "a list of requirements, not a feature", and a prompt line is a request to the model. *c* is ruled out because the page reads a regime as "requirements on the design", and not as a test result.
48. **d and e**. "Only an output or monitor control of the ordinary tier may proceed with a flag", and the example marks such a reply `auto (unscreened)` so that the gap shows in the record (module 90, page 1). *a* is ruled out because for an ordinary output control "the answers themselves are still checked by other controls", so a flag is enough and a hold is not needed. *b* is ruled out because the page wants the gap to show "so that the gap shows in the record". *c* is ruled out because "a control on the input or the action layer, and any control of the high tier, holds."
49. **a**. Counting refusals per user feeds the response to repeat offenders: tell the user the action violates the usage policy and throttle or end the access (module 90, page 1). A refusal is an outcome the application handles. The page puts it as "Counting refusals per user also feeds the response to repeat offenders that the jailbreak page describes". *b* is ruled out because the page treats a refusal as an outcome with a branch: "the context is reset (the turn that caused it removed or rephrased)". *c* is ruled out because "the `stop_details` fields can be null, so the user-facing message comes from the application". *d* is ruled out because "The design treats it as an outcome with its own branch", and a count that is ignored has none.
50. **b**. The map stays in a vault inside the caller, the provider sees only tokens, and the same value gets the same token so the model can still reason about one person writing twice (module 83, page 2). Restoration happens after the reply. The page puts it as "The same value gets the same token". *a* is ruled out because "The vault is local" and "The provider sees tokens and never the values." *c* is ruled out because the map is data that "never leaves the caller", and a prompt would send it across. *d* is ruled out because "The same value gets the same token", so the model can still reason about "the same person wrote twice".
51. **d**. The question is who waits and for how long, and for a person on screen the answer is a number such as 2 seconds at the 95th percentile, while a batch job would allow minutes (module 91, page 1). The page puts it as "so the answer must come within 2 seconds at the 95th percentile". *a* is ruled out because "Fast", "accurate" and "safe" are not answers, "because nobody can fail them". *b* is ruled out because a latency is "a ceiling (95th percentile of at most 2000 ms)", and a mean hides the tail. *c* is ruled out because "a batch job would allow minutes", but the agents at a screen cannot wait that long.
52. **c**. The hand-off names the owner, the runbook, at least two monitors, such as refusals or tokens per answer, each compared with a baseline in both directions, and the rollback to the previous model that stays tested (module 91, page 2). The page puts it as "at least two signals, such as refusals, tokens per answer". *a* is ruled out because the page lists "the owner of the service (a role), the owner of the runbook that explains each alert, the monitors" and the rollback. *b* is ruled out because "Iteration is not a failure of the design; it is the part of the design that was planned for." *d* is ruled out because "a diagram without the runbook leaves the first incident to the person who happens to remember".
53. **d**. If the sponsor insists on one figure, the architect gives the number that matches the decision they face and says what it does not cover (module 79, page 2). The page adds that the accuracy to ask for is "99 percent on the confident slice, with the rest reviewed". *a* is ruled out because "Full automation at 6 percent wrong barely beats the human baseline once errors are priced", and an average hides the costly slice. *b* is ruled out because "Promise what you measure and measure what you promise", and a flattering slice is a promise the design cannot keep. *c* is ruled out because "If the sponsor insists on a single figure, give the number that matches the decision they face and say what it does not cover".
54. **a and b**. A record compares at least four options, including obviously wrong ones, because the reader's first question is why not the simple thing (module 91, page 1). Each has "a monthly cost, whether it meets the service levels, a status and a reason". *c* is ruled out because "A design record that shows one option is an announcement." *d* is ruled out because the page asks for "at least four, including the ones that are obviously wrong". *e* is ruled out because "among those that meet them the cheapest wins", and the newest is not a criterion.
55. **b**. The record is reviewed at each stage of the roll-out, and each review may change the design: a new segment appears, a threshold moves, a model is replaced (module 91, page 2). Iteration is the part of the design that was planned for. The page puts it as "a new segment appears, a threshold moves, a model is replaced". *a* is ruled out because "The guide lists five phases: discovery, design, handoff, monitoring and iteration", and a freeze would stop the design before the last phase. *c* is ruled out because the record "is reviewed at each stage of the roll-out, and each review is allowed to change the design". *d* is ruled out because "Iteration is not a failure of the design; it is the part of the design that was planned for."
56. **a**. Discovery asks what exists today, such as the cost and quality of the current way, because without a baseline no saving can be shown (module 91, page 1). The page's example is 315,000 a month with people alone. The page puts it as "Without a baseline no saving can be shown". *b* is ruled out because the page lists that separately: "A role that can be telephoned". *c* is ruled out because that question concerns the target, "A measure, a threshold, an owner", and the saving needs the figure for the current way. *d* is ruled out because that question asks for "Data that may not leave a region, actions that need a person", which sets limits and gives no figure to measure a saving against.
57. **b**. The record recommends exactly one option, the cheapest that meets the service levels, and two recommended options push the choice back to the reader (module 91, page 1). The other options stay in the record with a reason in a sentence. The page puts it as "Two recommended options push the choice back to the reader". *a* is ruled out because the page says "Exactly one is recommended." *c* is ruled out because "the newest" and "the most capable" are not criteria, as the page says: "among those that meet them the cheapest wins". *d* is ruled out because "A design record that shows one option is an announcement."
58. **c**. Accountability is a role that can be telephoned, written in the record, and where the model prepares and a person decides the record says so (module 91, page 1). The page calls an ownerless target "nobody's". The page puts it as "A role that can be telephoned". *a* is ruled out because "deploying an automated decision means owning its outcomes", so the deploying organisation owns it. *b* is ruled out because "the owner is a role that can be called when it moves", and a sponsor is not that. *d* is ruled out because "A target without a measure cannot be reported, and one without an owner is nobody's."
59. **a**. The rule `sla-without-numbers` is a medium P6 finding raised when a latency or an availability has no number, and any medium finding with no high one gives revise (module 93, page 2). The page puts it as "a latency or an availability with no number". *b* is ruled out because the rule raises on "a latency or an availability with no number", and a service level needs "a target, a measure and an owner". *c* is ruled out because the table gives `accuracy-unstated` as "the needed accuracy is not stated", and the stem states it. *d* is ruled out because `no-accountable-owner` is raised when there is "no owner for the service", which the stem excludes.
60. **b and d**. A member's limit is no higher than the smallest group's, since "a member limit above a group's own makes the member limit a number that can never be reached", and the group limits add up exactly to the organisation's total (module 92, page 2). *a* is ruled out because "a total of exactly 20000 passes and 20001 does not". *c* is ruled out because "make the groups add up exactly", because the organisation limit decides first otherwise. *e* is ruled out because "a member's limit is no higher than the smallest group's", so a figure above the group's cannot stand.
61. **c**. Server-managed settings apply to everyone in the organisation, and a different policy for one group means a different file or profile deployed to that group (module 92, page 1). They are fetched at startup and refreshed hourly. The page puts it as "per-group policy is not yet supported there". *a* is ruled out because it "is edited by an Owner or Primary Owner, not by any administrator". *b* is ruled out because "a policy belongs in managed settings, and the shared file holds what the team agrees for itself". *d* is ruled out because server-managed settings are applied to everyone: "It applies to everyone in the organisation".
62. **a**. An empty list blocks every source, the official marketplace included, so allow nothing and allow ours are different lists (module 92, page 2). The company's own repository must be listed to be allowed. The page puts it as "An empty list blocks every source, the official one included". *b* is ruled out because the allowlist is "an allowlist of the sources plugins may come from", and an empty one lists none. *c* is ruled out because "Allow all plugins from the official marketplace and block the rest" is rejected, because an empty list blocks the official one too. *d* is ruled out because "The practice's policy lists one: the company's own marketplace repository", and an empty list names none, so the firm's source is refused too.
63. **b**. Contribution metrics need the GitHub app and are not available for organisations with zero data retention enabled, so the team checks that before promising them (module 92, page 2). The page puts it as "are not available for organisations with zero data retention enabled". *a* is ruled out because the baseline is collected before the launch: "the plan takes four weeks of data before the rollout". *c* is ruled out because "Lines and suggestions accepted are reported and not targeted." *d* is ruled out because "a member's use draws from a seat allowance", which concerns spend and not metrics.

</details>
