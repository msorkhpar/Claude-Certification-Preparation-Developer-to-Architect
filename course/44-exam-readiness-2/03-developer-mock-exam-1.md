# Developer mock exam 1

**Level:** Developer · **Module 44:** Exam readiness 2 · **Page 3 of 4**
**Exams:** DV1 to DV8 (CCDV-F; the questions follow the Developer blueprint over the content of modules 1 to 43)

**After this page you can** tell whether you are ready for the Developer exam and which domains need more work.

This mock exam covers **the content of Levels 1 and 2** (modules 1 to 43), as the Developer exam does, and not one page or one module. It is
written in the exam's style: a short scenario with a constraint, one best answer and three plausible alternatives, each of which is a mistake a
practitioner could make. Every question is the course's own, written fresh; no question comes from a live exam, and none repeats a question of a page
quiz, a module quiz or the Level 1 mock exam. The facts behind each answer were read on 2026-10-02 from the official pages named on the module
pages, so a reader who studies the pages can answer each question from the page it is drawn from. The real exam has multiple-choice and multiple-response items, and so does this mock: a multiple-response item ends with (Select two.) and is right only when both keyed options are chosen.

## How to take it

1. **Choose your conditions.** For a readiness test, take all 53 questions in **120 minutes** with no notes, which is the Developer exam's pace of
   about 2.3 minutes an item. For learning, take it untimed and read each explanation.
2. **Answer every question.** The guides describe no penalty for a wrong answer.
3. **Mark each answer** and read the explanations only after you finish.
4. **Score by domain,** not only in total, using the table below, and spend your next study session on the weakest domain with the most weight.

There is no official conversion from a mock percentage to a result. The real exam reports the percent correct for each domain, but **pass or fail
depends on the total: a scaled score of 720 on a scale from 100 to 1,000**. Treat the mock as a guide to where you stand in each domain and never as
a prediction.

## What it covers

The mock has **53 questions**, the number of items on the real exam. They are spread by the Developer blueprint's weights (each domain's weight times 53,
rounded so that the total stays 53) and grouped by domain here for easy scoring; the real exam mixes them. The share column is the count divided by 53, set
beside the official weight so that you can see the split matches.

| Questions | Domain | Weight | Count | Share of this mock | Drawn from modules |
|---|---|---|---|---|---|
| 1 to 17 | DV1 Applications and integration | 33.1% | 17 | 32.1% | 3, 12, 13, 14, 15, 16, 17, 21, 22, 23, 25, 30, 39, 40 |
| 18 to 26 | DV2 Model selection and optimisation | 16.8% | 9 | 17.0% | 1, 3, 4, 6, 18, 19, 20, 21 |
| 27 to 34 | DV3 Agents and workflows | 14.7% | 8 | 15.1% | 34, 35, 36, 37 |
| 35 to 40 | DV4 Prompt and context engineering | 11.0% | 6 | 11.3% | 24, 25, 28, 29 |
| 41 to 46 | DV5 Tools and MCP | 10.6% | 6 | 11.3% | 26, 27, 31, 32, 33 |
| 47 to 50 | DV6 Security and safety | 8.1% | 4 | 7.5% | 10, 31, 41 |
| 51 to 52 | DV7 Claude Code | 3.1% | 2 | 3.8% | 38, 39 |
| 53 | DV8 Evaluation, testing and debugging | 2.6% | 1 | 1.9% | 42 |

For a miss, open the page that the question's explanation names, as (module, page), and reread the passage it quotes. A cluster of misses in one
domain points at that domain's modules in the last column. The questions on the API, cloud platforms and streaming (DV1) and on model choice, thinking
and caching (DV2) carry most of the weight, so they repay the first revision session.

## Mock exam

This mock exam covers the content of Levels 1 and 2, modules 1 to 43. Choose one answer for each question, or the number the question states.

1. A team's monitoring shows its per-minute token use at about 40 percent of its tier's limit, yet 429 errors appear in sharp bursts whenever a scheduled job starts. Which explanation fits best?
   - **a**: Cached tokens are counted twice against the input limit, so the average hides the real total
   - **b**: The allowance resets in full at fixed minute boundaries, so jobs that start together all collide at once
   - **c**: Spare capacity is topped up gradually, so a sudden spike empties it even when the mean is low
   - **d**: A raised max_tokens value is charged against the output limit even when the reply is short

2. After a provider update, a hand-written client that worked for months throws an exception on every reply because the JSON now carries a field it has never seen. Which change fits best?
   - **a**: Pin the request to an earlier documented version so replies keep their old shape
   - **b**: Pick out just the values the code uses and skip whatever else arrives
   - **c**: Validate every reply against a strict schema and reject anything that has an extra entry
   - **d**: Report the provider to support, because adding output values breaks the contract

3. A support bot caches a long prefix. After turn four the team wants a new tone rule applied to later turns on Claude Sonnet 5.5, without discarding the cached earlier part. Which two statements describe the approach that fits? (Select two.)
   - **a**: Rewrite the top-level system field with the new rule on the next request
   - **b**: Append a system-role instruction after the latest user message
   - **c**: End the list with a partial assistant message that already follows the rule
   - **d**: Prior content keeps its saved computation
   - **e**: Start a fresh conversation and resend a summary that includes the rule

4. A dashboard charts only the plain input count from each response and shows 50 for a call that sent a 200,000-token document from the cache. The team asks whether the figure is broken. What should they be told?
   - **a**: The field was capped at 50 because the document exceeded the context limit of that model
   - **b**: Input tokens are billed separately from the cache, so the figure shown is already complete and exact
   - **c**: Streaming reports that figure only in the closing event, so the dashboard read it early
   - **d**: It holds just the tail after the final breakpoint, so combine it with the read and write totals

5. Every call returns a 400 whose message begins with "You have reached your specified API usage limits". The team reformats the request body three times with no change. What should they do next?
   - **a**: Back off and retry after the interval that the response supplies
   - **b**: Rotate the API key, since the credential has probably expired or been revoked
   - **c**: Raise the ceiling that an administrator configured below the plan's cap
   - **d**: Shrink the payload toward the 32 MB size limit and send it again

6. A client computes a 2-second back-off after a rate-limit response, but that response carries a header asking for 30 seconds. Which delay should the client apply?
   - **a**: The computed value, because the header is only advisory
   - **b**: The longer of the two waits, because earlier retries will fail
   - **c**: The header value minus the time the failed request already took
   - **d**: Zero, so the limit is probed again as soon as it refills

7. A batch service must sustain 600 requests a minute against the API, and each call takes about 3 seconds. Which cap on simultaneous calls fits that target without inviting rate-limit errors?
   - **a**: About 10, the number of requests that begin during each single second
   - **b**: About 300, half of the requests that are sent during that minute
   - **c**: About 30, from the arrival frequency times each exchange's duration
   - **d**: About 600, one slot for each request sent during the minute

8. A logging layer for a streamed reply records the stop reason from the first chunk it receives and always stores a null. Which two statements explain it? (Select two.)
   - **a**: It arrives in the HTTP status line of the response
   - **b**: The value arrives in the delta frame sent close to the finish
   - **c**: It arrives in a content_block_stop frame, once each block completes
   - **d**: An interrupted transfer never delivers one
   - **e**: It arrives in message_start, alongside the input token count

9. An operator stops a running batch and sees an interim state named canceling. What should the team expect for the requests that had already run?
   - **a**: Every one is refunded because the batch never reached its normal end
   - **b**: Their charges stand, and their outputs may show up once polling reaches the end
   - **c**: Their results are discarded and nothing can be downloaded afterwards
   - **d**: The whole batch is deleted at once, so its results address stops working for everyone

10. A developer ports a working direct-API request to the Messages API of Claude in Amazon Bedrock and removes the version header, assuming the platform now handles versioning. What does the page say?
   - **a**: The date moves into the body and is renamed to a platform-specific field name
   - **b**: The platform infers it from the model identifier, so no value is sent
   - **c**: It stays, with the value 2023-06-01, exactly as on Anthropic's own endpoint
   - **d**: AWS signing replaces it, since SigV4 authenticates the whole call

11. A document-review service on Google Cloud attaches dozens of page images to each request and is refused although the token count is well under the window. Which limit did it most likely hit?
   - **a**: The 600-image ceiling that applies to models with larger windows
   - **b**: A payload size ceiling of 30 MB enforced by the platform
   - **c**: A tokens-per-minute quota that is shared across the account
   - **d**: A requirement that images arrive as links instead of encoded strings

12. A security lead wants to stop all inference against foundation models in an AWS account today, while keeping the policy as short as possible. Which statement holds?
   - **a**: Each related action needs its own explicit deny statement as well
   - **b**: A wildcard in a deny rule is the same defect as one in an allow rule
   - **c**: Deny statements are ignored, so in practice only a narrow allow-list can remove access
   - **d**: Denying the invoke action alone also blocks the neighbouring conversation APIs

13. A service that extracted invoices by forcing one tool on every call moves to Claude Sonnet 5.5 and every request now fails with a 400. Which replacement fits?
   - **a**: Switch the choice to any, which lets the model pick among the tools
   - **b**: A fixed-shape reply through the output format setting with a JSON schema
   - **c**: Ask for JSON in the prompt and parse whatever text comes back
   - **d**: Add an assistant message that opens a brace so that the reply begins as JSON

14. A team reads PDFs through Amazon Bedrock's Converse API and finds that answers ignore every chart, although the same files work well on the Claude API. Which two statements explain it and the fix? (Select two.)
   - **a**: Switching to base64 sources hides the links that blocked the images
   - **b**: A PDF surcharge disables page images unless it is paid
   - **c**: The visual mode requires citations enabled
   - **d**: Converting each PDF to plain text first adds the image content
   - **e**: Without them the service falls back to basic text extraction

15. A guard hook works on its author's laptop but fails for every teammate who adds it from the marketplace; the command line names an absolute path. Which fix fits?
   - **a**: Copy the program into each teammate's home directory during onboarding
   - **b**: Move the guard into the agent file with a permission mode setting
   - **c**: Reach the program through the variable holding the install root
   - **d**: Place the program beside the manifest in the dot-folder

16. A workflow lets Claude push fixes to a branch, but the test pipeline that should run on each push never starts. The workflow signs in with the repository's built-in credential. Which change fits?
   - **a**: Commit with a GitHub App token, since the default one's commits trigger nothing
   - **b**: Pin the action to the beta tag so event delivery is restored
   - **c**: Grant the job write access to every scope, including the identity token
   - **d**: Limit parallel runs with concurrency controls so that the pipeline is not skipped over

17. A team wrote its build commands and conventions into the repository's memory file while using the terminal tool. A developer now opens the same repository in an IDE extension. What should the team expect?
   - **a**: Nothing carries over, because each surface keeps its own separate memory
   - **b**: The conventions apply, but the settings and servers must be recreated
   - **c**: Only the terminal reads that file, and extensions need the Agent SDK
   - **d**: Matching rules and configuration apply, as each surface runs one engine

18. Nightly bulk jobs share a 30,000-token instruction block, yet cache hit rates stay low because each entry lapses before many requests have arrived. Which adjustment does the page suggest?
   - **a**: Submit that block as a separate batch beforehand to warm the entry
   - **b**: Ask for the one-hour lifetime on the common material
   - **c**: Remove the cache markers, because hits are impossible inside a batch
   - **d**: Vary the instruction block slightly per call so that entries differ

19. A team began a long-running coding agent on the recommended default tier, and its own evaluations at higher effort still fall short of the quality bar. Which step fits the model overview?
   - **a**: Abandon the design, because no model suits long-horizon work of this kind
   - **b**: Add a second agent on the same tier so that answers receive a majority vote
   - **c**: Drop to Claude Sonnet 5.5, since faster models usually score higher on such tests
   - **d**: Switch to Claude Fable 5.1, which the page reserves for demanding reasoning

20. A shared request builder sends Claude Sonnet 5.5 a thinking setting that keeps only the short updates between tool calls, together with xhigh effort, and every call returns a 400. Which two statements explain it and the fix? (Select two.)
   - **a**: That mode works at the three lowest levels
   - **b**: Pass adaptive as the effort value so that the model chooses its own
   - **c**: Raise max_tokens, because the 400 reports a truncated reply
   - **d**: Add temperature 0 so that the behaviour becomes deterministic
   - **e**: At the two highest levels it is rejected

21. After a deploy, a service immediately fires ten simultaneous requests that share a 30,000-token prefix with a marker on it. Usage shows ten writes and no reads. What is the fix?
   - **a**: Raise the lifetime to one hour so later entries reach the earlier one
   - **b**: Alternate the effort setting on every call to spread out the writes
   - **c**: Send one, wait until its response begins, then release the other nine
   - **d**: Shrink the prefix below the minimum so that nothing has to be written

22. A team has no evaluation data yet and no sense of how hard its new feature is. Which starting plan matches the documentation's method?
   - **a**: Pick the cheapest tier at once, since the pricing page says to use it for simple tasks
   - **b**: Begin with a capable model, test on real cases, then step down a tier at a time
   - **c**: Use the top tier permanently, since it protects quality for every task
   - **d**: Choose by reputation, then adjust after the first production complaint

23. A request to Claude Haiku 4.5, whose limit is 200K, carries about 150,000 input tokens and sets max_tokens to 64,000, which together go beyond that limit. What happens?
   - **a**: The call is accepted, and a lengthy reply may end early with a dedicated stop reason
   - **b**: The call is rejected with a 400, because input plus output may not exceed the limit
   - **c**: The excess is trimmed from the start of the input history before any processing begins
   - **d**: The output allowance is silently lowered to fit, so the reply always completes

24. Two otherwise identical multi-turn services use thinking: one runs on Claude Opus 5.5 and one on Claude Haiku 4.5. The first one's conversations fill their context faster. Which two statements explain the gap? (Select two.)
   - **a**: Haiku 4.5 has a smaller window of 64K, so its context is cleared sooner
   - **b**: Adaptive thinking bills reasoning as input, which crowds the window
   - **c**: The larger model retains earlier reasoning by default
   - **d**: Opus 5.5 can disable thinking, so it compensates with longer prompts
   - **e**: The smaller model drops earlier reasoning blocks

25. A drafting feature scores 62 percent on the team's fixed test set against a 90 percent target and costs more than planned. Someone proposes switching tiers immediately. What does the module advise?
   - **a**: Switch tiers now, since a lower price makes later quality work affordable
   - **b**: Judge the lower tier on the ten easiest items, then roll it out
   - **c**: Rewrite several parts of the prompt together to gain accuracy quickly
   - **d**: Hit the quality bar before looking for any saving that holds it

26. A team migrates a reasoning-heavy workload to a newer model and copies its old effort values unchanged. Quality drifts. Which step does the guidance give?
   - **a**: Reapply the old thinking budgets, which carry over between generations
   - **b**: Run a sweep across levels on its own test set
   - **c**: Raise every level by one notch, since newer models think less per level
   - **d**: Pin the old model id until the next generation arrives

27. A pipeline starts two model calls at once: one drafts a reply and one checks the reply's tone, but the checker keeps failing because the draft does not exist yet. How should the team restructure it?
   - **a**: Run them in sequence, so the second receives the first one's output
   - **b**: Vote across three identical drafting calls, then check the winner
   - **c**: Route the draft to a specialised prompt chosen by a classifier
   - **d**: Replace both calls with an autonomous agent that decides the order of the steps

28. In a draft-and-review loop, the reviewer's reply is sometimes prose such as "looks fine to me" instead of a score the program can parse. The loop currently treats that as a pass. What should it do?
   - **a**: Accept it as a pass, since no objection was raised
   - **b**: Count the attempt as unchecked, giving it zero with a stated cause
   - **c**: Pull the first digit from the text and treat it as the grade
   - **d**: Return the latest draft, since the reviewer is evidently satisfied

29. A service sets a dollar budget on its Agent SDK run, and the run fans work out to secondary workers. Finance worries that the workers' spending escapes the limit. What does the documentation say?
   - **a**: No cost control exists, so the team must stop runs by hand
   - **b**: The cap counts only turns that use a tool, never dollars
   - **c**: The cap is checked once the run finishes, so overruns are unavoidable
   - **d**: The ceiling reaches down to the delegates as well

30. A custom in-process tool is annotated as read-only, and the team lets it run alongside other tools without review, assuming that it cannot modify anything. Which two statements explain why that assumption fails? (Select two.)
   - **a**: The handler's code can still write
   - **b**: The label is enforced when the tool is served from outside the process boundary
   - **c**: The SDK rejects any write that such a tool attempts while the run is under way
   - **d**: Tools carrying the label are always run one at a time, to stay on the safe side
   - **e**: The label is scheduling metadata and not a guarantee

31. A team promotes a new system prompt for a hosted agent and wants production runs to keep the previous behaviour until a person approves the switch. Which approach fits?
   - **a**: Override the system field on every run, since overrides merge with the saved agent
   - **b**: Create the sessions with just the agent ID, which pins the first version
   - **c**: Create each session with a pinned version number
   - **d**: Edit the agent in place, because environments are versioned for rollback

32. A security officer approves self-hosted sandboxes for a managed agent on the grounds that nothing the agent touches will ever leave the company network. What is wrong with that reasoning?
   - **a**: Inputs and outputs of each call still reach the provider's control plane
   - **b**: Only the sandbox image is hosted by the provider, so source files are exposed
   - **c**: Files and processes also run on the provider's side, so only egress rules differ
   - **d**: The environment key must be stored inside the image so the worker can start

33. A typed agent returns a refund record that passes its schema, showing an amount of 4999 cents for a 49-cent item, and the team ships it because validation succeeded. What should they conclude?
   - **a**: The schema is wrong, because a correct type guarantees a correct number
   - **b**: Form checks prove only the shape, so a separate rule must judge the value
   - **c**: The retry loop should have caught it, since failed validation retries by itself
   - **d**: A graph framework would have prevented it, since its checkpoints verify values

34. A team wants a framework mainly because it is popular, though nobody can say which missing feature it would supply. Which step does the page advise?
   - **a**: Write the requirement with a number in it and test the simplest option
   - **b**: Adopt it and trim the unused parts after the first release goes out
   - **c**: Count the features each candidate offers and pick the longest list of them
   - **d**: Pick the family by the brand that has published the most releases

35. An application pastes a user's forwarded email between fixed tags in its prompt. A tester shows that an email containing the closing tag followed by new orders is treated as part of the instructions. Which measure from the page on pasted content fits best?
   - **a**: Move the email above the instructions so that its position marks it as data
   - **b**: Place the email in the system prompt, where it carries the most weight with the model
   - **c**: Fence it with a random identifier and warn the model it could hold directives
   - **d**: Rely on tags alone, because the model treats text inside tags as inert data

36. A JSON schema for a classifier includes a property named "thinking_steps" that demands the model's reasoning, and some requests now come back refused. Which change fits?
   - **a**: Raise max_tokens so that the long reasoning is not cut off
   - **b**: Retry the same call until the model stops refusing
   - **c**: Lower the effort so the model reasons less before filling the field
   - **d**: Ask for a brief explanation in its place

37. An extraction loop repairs failed replies by sending the original prompt again with the note "try again". The same mistakes keep recurring. Which two changes fit? (Select two.)
   - **a**: Retry without a limit, since eventual success is likely on a hard document
   - **b**: Return the model's own answer with the request
   - **c**: List every problem with its path and message
   - **d**: Add a prefilled assistant turn that opens the JSON object
   - **e**: Report only the first problem each time, to keep the note short

38. A retrieval service embeds both stored passages and incoming questions with the same provider call and no further options, and results are mediocre. Which change do the course pages back?
   - **a**: Truncate each passage to a single sentence before embedding
   - **b**: Switch to Anthropic's own embedding model, which handles both uses
   - **c**: Set the parameter that labels each input as a query or a document
   - **d**: Replace the vectors with keyword scores, which match meaning exactly

39. An agent compacts its history every forty turns by adding a fresh summary, and after several rounds the conversation holds four summaries that partly contradict each other. Which fix fits?
   - **a**: Keep every summary and let the newest override the rest
   - **b**: Delete the oldest summary and keep the other three
   - **c**: Stop compacting and raise max_tokens to avoid a full window
   - **d**: Fold the earlier one into the newest, leaving exactly one

40. A team enables automatic clearing of old tool results, yet every clearing frees only a few hundred tokens and the next request pays a large cache write. Which parameter addresses this?
   - **a**: A longer list of excluded tools so fewer results qualify
   - **b**: A higher keep value so more recent pairs stay
   - **c**: A lower bound on the savings each pass must achieve
   - **d**: A lower trigger so that clearing begins at a smaller context size

41. A service forces a named tool on Claude Haiku 4.5 and every reply is a bare tool call with no sentence before it, but product wants a brief explanation first. What fits?
   - **a**: Switch to the any setting, which adds an explanation before the call
   - **b**: Disable parallel use, since one action per reply lets text appear first
   - **c**: Set the choice to none so that commentary is written before anything else
   - **d**: Use the automatic setting and say in a user message to run that function

42. After a refactor, a model that used to call three independent tools in one reply now calls them one at a time. The code sends each output back in its own message. What is the likely cause?
   - **a**: Parallel use is limited to operations that merely read data, so that setting changed
   - **b**: The default for the choice field was switched to disable parallel use between releases
   - **c**: Separate returns teach Claude to avoid issuing several requests together
   - **d**: Parallel calls vanish whenever a conversation passes a certain length of history

43. A developer connects forty tool-bearing servers to Claude Code and worries that every request now carries all their schemas. Which two statements does the documentation support? (Select two.)
   - **a**: Dormant integrations cost little space
   - **b**: Discovery on demand is switched on by default
   - **c**: The schemas belong in CLAUDE.md, which loads when a question needs them
   - **d**: Servers load nothing at start, so adding more affects latency alone
   - **e**: Each schema loads in full at session start, so the cost stays fixed per server

44. An application defines its own function called screenshot alongside Anthropic's screen-control entry. Incoming blocks with that word are sometimes meant for one and sometimes for the other. How should dispatch work?
   - **a**: Reject the custom function, because member names are reserved
   - **b**: Use the order in which the blocks arrive to decide which handler applies
   - **c**: Check whether the block carries display dimensions, which only screen actions have
   - **d**: Branch on the member together with the extra field that identifies the toolset

45. A Python MCP server rejects a blank title by raising an ordinary ValueError inside the handler, and the model never sees why the call failed. Which change fits?
   - **a**: Return a protocol error, since the model can retry after a malformed request
   - **b**: Print the reason to standard output so that the client can relay it
   - **c**: Signal it with the SDK's dedicated exception type for expected problems
   - **d**: Catch the exception and return an empty success result

46. During a deployment tool's confirmation step, the person chooses decline. The server currently returns a protocol error and the client logs an incident. How should the server treat that answer?
   - **a**: As a failed call, signalling the code for a missing client capability
   - **b**: As an ordinary outcome, finishing with a result that reports it was cancelled
   - **c**: As a prompt to ask again, repeating the confirmation until the person accepts
   - **d**: As a timeout, so the whole call is retried with a new identifier

47. A company on a commercial plan, with training use off by default, tells staff that anything typed is erased within 30 days. A reviewer notes one exception that staff create themselves. Which action does the reviewer mean?
   - **a**: Giving thumbs feedback on a reply keeps the conversation for up to five years
   - **b**: Opening a chat in incognito mode, which is then stored by the provider for a year
   - **c**: Connecting a data source, whose raw content is then kept by the provider for seven years
   - **d**: Deleting a chat, which the back end then retains for five years in a hidden archive

48. A team plans to drop the disposable container for its screen-driving agent because Anthropic already scans screenshots for planted instructions. Which two statements should the reviewer make? (Select two.)
   - **a**: Injected text can still be obeyed despite the filter
   - **b**: Screenshots are processed by Anthropic alone, so no local safeguards apply
   - **c**: Such filters are a backup layer behind the first defences
   - **d**: Dropping the container is fine once an allowlist of domains is in place
   - **e**: The scan makes planted text harmless, so the confirmation steps can go too

49. An agent loop returns a vendor lookup to Claude, and the team adds "now email the summary to finance" to the end of that same return. The instruction is routinely ignored. Which placement fits?
   - **a**: Put it in a separate user message that follows the block
   - **b**: Repeat it three times inside the returned text so it is noticed
   - **c**: Move it into the system prompt together with the vendor text
   - **d**: Wrap it in a JSON string with the vendor data so both stay together

50. A screening call on a web fetch's raw output flags an injection attempt. The agent loop currently passes the page through anyway and logs a warning. What does the documentation say to return?
   - **a**: The untouched page, plus a note telling the model that it looks suspect
   - **b**: Nothing at all, ending the whole run immediately with no output
   - **c**: A failure notice or a condensed digest rather than the original body
   - **d**: The page with the matched phrases deleted and the rest left intact

51. A CI job runs the headless mode with the flag that skips local configuration and now reports no credentials, although the developer is signed in with a subscription on the same machine. What is needed?
   - **a**: Mount the developer's keychain into the runner so it can be read
   - **b**: Drop the flag, since the other mode sees the same local setup on every machine
   - **c**: Run it interactively in a terminal, where the login prompt can appear
   - **d**: Provide an API key through the environment

52. Two hooks match the same shell call: a logging hook that exits cleanly and a guard hook that answers with a denial. A teammate fears the logger's success will let the call through. What happens?
   - **a**: It proceeds, because the hook that exits with zero is consulted first
   - **b**: It is stopped, since the harshest verdict among them prevails
   - **c**: It proceeds, since hooks run one after another and the first result decides
   - **d**: It pauses for a person, because any disagreement produces an ask

53. Between two evaluation runs the pass rate rose from 0.71 to 0.80, but a diff shows that four failing cases were deleted from the set. How should the harness treat this?
   - **a**: Flag the dropped items as removed and reject the check
   - **b**: Accept it, since a higher pass rate means the change helped
   - **c**: Treat the deleted ones as passes, since nothing contradicts them
   - **d**: Ignore the deletions when the edge-tagged cases still reach their threshold

<details>
<summary>Answer key</summary>

1. **c**. The limit works as a token bucket that is "continuously replenished up to your maximum limit", so a spike can empty it (module 12, page 2). *a* is ruled out because the page says that for most models "only uncached input tokens count toward ITPM", so cached tokens are not double counted. *b* is ruled out because the bucket refills "rather than being reset at fixed intervals", so there is no minute boundary. *d* is ruled out because the rate-limit page says "does not factor into OTPM rate limit calculations" for the max_tokens parameter.
2. **b**. A client should "Read what you need and ignore the rest", since an unknown field is not an error (module 13, page 1). *a* is ruled out because earlier versions "may be unavailable for new users" and the only current value is the one already in use. *c* is ruled out because a client that fails on an unknown field "breaks on a change the versioning page allows". *d* is ruled out because the versioning policy reserves the right to "add additional values to the output", so nothing was broken.
3. **b and d**. A message with the system role after a user turn "does not invalidate a cached prefix that came before it" (module 14, page 1), so the earlier part of the conversation stays reusable. *a* is ruled out because adding the rule partway "changes the system prompt and invalidates the conversation's earlier thinking blocks". *c* is ruled out because a conversation that ends on the model's side "must end with a user message" and returns a 400. *e* is ruled out because the page assigns "the mid-conversation message for instructions that only become relevant later", so no restart is needed.
4. **d**. The plain count "only represents tokens that appear after your last cache breakpoint", so the other two fields must be added (module 14, page 2). *a* is ruled out because the page gives the same case, "reads 50 although the total input is 200,050", and nothing was capped. *b* is ruled out because the page states that "the total input is the sum of three numbers", not one. *c* is ruled out because the page says "the input side arrives in message_start", so timing does not explain a low value.
5. **c**. A limit set below the tier's cap returns a 400, and "Lifting the limit is the cure, not changing the request" (module 15, page 1). *a* is ruled out because a 400 is in the group where "the same request fails again", so waiting does not help. *b* is ruled out because the page explains that "A limit you set yourself, below the tier's cap, also stops requests". *d* is ruled out because an oversized body gives a different outcome: "a larger request is refused with a 413".
6. **b**. The header sets a floor, so wait "the larger of your back-off and the header" (module 15, page 2). *a* is ruled out because the page says "it is a floor, not a suggestion", so the header cannot be treated as advice. *c* is ruled out because the header text says "Earlier retries will fail", and nothing in it is reduced by elapsed time. *d* is ruled out because the page warns that "hammering the endpoint in the meantime only fails".
7. **c**. In-flight calls follow Little's law: "the number of requests in flight equals the request rate times the time each takes", so 10 a second for 3 seconds gives 30 (module 16, page 1). *a* is ruled out because the page defines the bound as "the number of requests in flight equals the request rate times the time each takes", which includes the duration. *b* is ruled out because a bound above the need "would only invite 429 errors". *d* is ruled out because the rule is to "compute the bound from the limit you must stay under", not to match the total sent.
8. **b and d**. The stop reason "arrives in message_delta, near the end", so a cut stream has none (module 17, page 1). *a* is ruled out because "stop_reason indicates normal completion; HTTP errors indicate failures". *c* is ruled out because that frame means "The block at index is complete" and carries no reason. *e* is ruled out because the opening frame carries "an empty content and the input token count in usage", not the stop reason.
9. **b**. Batches that are stopped "end up with a status of ended and may contain partial results", and processed work is billed (module 21, page 2). *a* is ruled out because the page says "the requests that had already run are still billed". *c* is ruled out because the batch "may contain partial results for requests that were processed before cancellation". *d* is ruled out because "Cancellation may not be instant", so the batch is not removed at once.
10. **c**. On Bedrock the version "stays a header, as on the direct API" (module 22, page 1). *a* is ruled out because that placement is the Google Cloud rule: "anthropic_version is passed in the request body". *b* is ruled out because the contract lists the version as "which the overview lists as required". *d* is ruled out because the page treats them as separate items: "Authentication, version and body type are all a plain request needs".
11. **b**. Google Cloud "limits request payloads to 30 MB", which many images can reach first (module 22, page 2). *a* is ruled out because the page says the request size limit "can be reached first", before any image count. *c* is ruled out because the refusal came while the count was low, and a team with many images "can reach before the token limit". *d* is ruled out because on this platform "only base64-encoded sources are currently available", so links are not an option.
12. **d**. Denying InvokeModel is enough because other actions "are blocked automatically when InvokeModel is denied" (module 23, page 1). *a* is ruled out because the page lists the neighbours, "such as Converse and StartAsyncInvoke", as blocked without extra statements. *b* is ruled out because the page says "In a guardrail, the wildcard is the point", so the wildcard is legitimate in a deny. *c* is ruled out because a "statement belongs in a separate guardrail", which is where deny rules live.
13. **b**. The page recommends structured outputs "when you need a response in a fixed JSON shape" (module 25, page 1). *a* is ruled out because on these models `any` and `tool` "return a 400 error" just as a named tool does. *c* is ruled out because a prompt request gives "no guarantee; the program must parse defensively". *d* is ruled out because prefilled assistant messages "return a 400 error" on these models.
14. **c and e**. "Without citations, Converse falls back to basic text extraction", so enabling them restores the full visual mode (module 30, page 2). *a* is ruled out because on that platform "only base64 sources are available", so that is already the case. *b* is ruled out because "Standard API pricing applies with no additional PDF fees". *d* is ruled out because text extraction is the mode that "cannot analyze images or charts".
15. **c**. A hook script in a plugin "cannot assume where the plugin was installed", so it uses the plugin-root variable (module 39, page 3). *a* is ruled out because the page says "The path differs on every machine", so a copied location cannot be shared. *b* is ruled out because agent fields of that kind "are ignored when loading agents from a plugin". *d* is ruled out because "Only plugin.json goes inside .claude-plugin/", so other files do not belong there.
16. **a**. "GitHub doesn't trigger workflows on commits made with the default GITHUB_TOKEN", so an app token is needed (module 40, page 2). *b* is ruled out because the page says "workflows written for @beta need to move", which concerns versions and not triggers. *c* is ruled out because the page tells you to "Grant the least a job needs". *d* is ruled out because the page says "Use GitHub's concurrency controls to limit parallel runs", which bounds cost and does not trigger anything.
17. **d**. Each surface "connects to the same underlying Claude Code engine", so the files carry across (module 3, page 2). *a* is ruled out because the page says "settings, and MCP servers work across all of them". *b* is ruled out because memory files "and settings that travel with the repository across its surfaces". *c* is ruled out because Claude Code is "available in the terminal, IDE extensions, a desktop app and the browser".
18. **b**. Because a batch can outlast five minutes, "it suggests the one-hour lifetime for shared context" (module 21, page 1). *a* is ruled out because warming "targets time-to-first-token, which does not apply to batch processing". *c* is ruled out because the page says "The pricing discounts from prompt caching and Message Batches can stack". *d* is ruled out because hits depend on "identical cache_control blocks in every request".
19. **d**. The overview points to Claude Fable 5.1 "when evals at higher effort still fall short" (module 3, page 1). *a* is ruled out because Fable 5.1 is listed for "demanding reasoning and long-horizon agentic work". *b* is ruled out because the course advises "adding complexity only when it demonstrably improves outcomes". *c* is ruled out because the method is to "move down a tier at a time until quality drops", which comes after the target is met.
20. **a and e**. The mode "works at `low`, `medium` and `high` effort and returns a 400 at `xhigh` and `max`" (module 19, page 1), so the fix is one of the three lowest values. *b* is ruled out because "Don't pass adaptive as an effort value". *c* is ruled out because a cut reply is a different case, because it "arrives with status 200". *d* is ruled out because on these models "a non-default value of any of them is rejected with a 400 error".
21. **c**. The page says to "wait for the first response before sending subsequent requests" (module 20, page 2). *a* is ruled out because a one-hour entry "writes at twice the base price" and still does not exist before the first response. *b* is ruled out because "Changing the thinking mode or the effort always discards the cached messages". *d* is ruled out because a prefix below the minimum "will be processed without caching", so the saving is lost.
22. **b**. The method is to "measure on your own cases, then move down a tier at a time" (module 18, page 1). *a* is ruled out because the first step is to "start high, so that a failure means the task is hard". *c* is ruled out because the course notes that nothing "says the biggest model is the safest default for every task". *d* is ruled out because "A tier is right only if it passed your evaluation at the lowest cost".
23. **a**. Input plus max_tokens beyond the window is allowed because "the request is still accepted", and generation stops at the limit (module 1, page 1). *b* is ruled out because "Older models returned a validation error instead". *c* is ruled out because chat products "manage the window on a rolling first-in, first-out basis; the API does not". *d* is ruled out because the page says "if generation then reaches the window limit it stops with" a stop reason, not a lowered allowance.
24. **c and e**. Earlier thinking is kept where "current Opus, Sonnet, Fable models keep them by default" (module 4, page 2), and the smaller model strips it, which is why the first service's context fills faster. *a* is ruled out because Haiku 4.5 has a window of "200K for Claude Haiku 4.5", and 64K is only its output ceiling. *b* is ruled out because reasoning tokens "are billed as output tokens". *d* is ruled out because "Opus 5.5 use adaptive thinking that is always on".
25. **d**. The order is "first reach the quality bar; then look for the cheaper way to hold it" (module 6, page 4). *a* is ruled out because a smaller tier fits only "where the evaluation set says quality holds". *b* is ruled out because the page says to "Test the cheap tier first on the hard cases, not on the easy ones". *c* is ruled out because "Several simultaneous edits leave you unable to say what helped".
26. **b**. The page says to "Run an effort sweep on your own evals rather than carrying settings over from an earlier model" (module 18, page 3). *a* is ruled out because a manual budget "or a manual budget_tokens, is rejected" on the newer models. *c* is ruled out because on Sonnet 5.5 "its levels are recalibrated", so no direction can be assumed. *d* is ruled out because the page promises "at least 60 days' notice before model retirement", which allows time to test instead of waiting.
27. **a**. Sectioning needs independent work, and "Parallel calls cannot see each other's output", so a dependent step belongs in a chain (module 34, page 1). *b* is ruled out because sectioning means "independent subtasks run in parallel", which these are not. *c* is ruled out because routing "directs it to a specialized followup task" and does not order two calls. *d* is ruled out because an agent suits a case where "the path cannot be written in advance", and this path is fixed.
28. **b**. "If the verdict cannot be parsed, the draft is unchecked", so it is graded zero with a reason (module 34, page 2). *a* is ruled out because "a reply that is not a bare score counts as ungradable", and an ungradable case fails. *c* is ruled out because such a parse "turns a broken grader into a plausible number". *d* is ruled out because "A refinement loop can get worse", so the page keeps the best draft and not the last.
29. **d**. The page states that "the budget covers subagents too" (module 35, page 1). *a* is ruled out because the option is described as "Maximum cost before stopping". *b* is ruled out because only the turn limit "counts tool-use turns only", while the budget measures cost. *c* is ruled out because "When a limit is reached the run ends with a result whose subtype names it".
30. **a and e**. "Annotations are metadata, not enforcement", and "A tool marked read-only can still write if its handler does" (module 35, page 2). *b* is ruled out because clients treat annotations as "untrusted unless they come from trusted servers", wherever the tool is served. *c* is ruled out because "Annotations are metadata, not enforcement". *d* is ruled out because the hint "lets the tool run in parallel with other read-only tools".
31. **c**. The page says "a session can instead pin a version" so a prompt can be promoted or rolled back without a deploy (module 36, page 1). *a* is ruled out because "overrides replace and never merge". *b* is ruled out because a session made with just an agent ID "creates the session with the latest agent version". *d* is ruled out because "Environments are not versioned".
32. **a**. "Tool inputs and outputs still flow to Anthropic's control plane" (module 36, page 2). *b* is ruled out because "Anthropic does not inspect or verify your sandbox image", and the image is the customer's. *c* is ruled out because the page says "files, processes and network traffic stay in your environment". *d* is ruled out because the key belongs in "a secrets manager, never an image".
33. **b**. "Validation proves the shape", and nothing more (module 37, page 1). *a* is ruled out because "A refund of 4999 cents passes the schema whether or not it is right". *c* is ruled out because only "a reply that does not validate against the output type is refused", and this one validated. *d* is ruled out because "A checkpoint is a copy of the state at a point in the graph", not a check.
34. **a**. The page says to "check whether the simplest option meets it" (module 37, page 2). *b* is ruled out because the advice is to "start with direct calls and add layers when a need appears". *c* is ruled out because "A framework that offers persistence helps only when the product must resume". *d* is ruled out because "The question is who should own the route, not which library is popular".
35. **c**. The page wraps pasted text in a tag "with a random id that your application generates" and says the text may carry instructions (module 24, page 1). *a* is ruled out because "Moving a pasted document above the instructions does not mark it as data". *b* is ruled out because data in the system prompt "gets the authority of an instruction". *d* is ruled out because "Tags mark where data starts and ends, but they do not make data safe".
36. **d**. The page says to "Ask for a short explanation instead" of the reasoning (module 24, page 2). *a* is ruled out because "a 200 with refusal is not an answer", and a refusal is not a cut reply. *b* is ruled out because "The same request gets the same decision". *c* is ruled out because the page says "Do not ask for the reasoning as output", whatever the effort.
37. **b and c**. The next call needs the model's own reply and "a user message that lists each problem with its path and message" (module 25, page 2). *a* is ruled out because "An unbounded retry on a hard document is a cost leak". *d* is ruled out because "Prefilling the assistant response returns a 400 error" on current models. *e* is ruled out because "Collect all the problems in one pass".
38. **c**. The documentation says "Do not omit input_type or set input_type=None" (module 28, page 1). *a* is ruled out because "Too small a chunk loses context". *b* is ruled out because "Anthropic does not offer its own embedding model". *d* is ruled out because keyword scoring fails on paraphrases because "it matches words, not meaning".
39. **d**. "A second compaction should fold the first summary into the new one", so that exactly one remains (module 29, page 1). *a* is ruled out because "exactly one block is sent per request". *b* is ruled out because the page says "an earlier summary is folded into the next one rather than stacked". *c* is ruled out because "Raising it does not enlarge the window".
40. **c**. The setting exists for the cache, because clearing "invalidates cached prompt prefixes when content is cleared" (module 29, page 2). *a* is ruled out because exclusions are "tool names whose results are never cleared", which makes clears smaller. *b* is ruled out because keep sets "how many recent tool use and result pairs stay", which shrinks each clear further. *d* is ruled out because the trigger sets "when clearing starts; input_tokens or tool_uses" and not how much each pass frees.
41. **d**. The page says "To get both a tool call and an explanation, use auto and say in a user message to use the tool" (module 26, page 1). *a* is ruled out because "the API prefills the assistant message to force a tool to be used", so no text comes first. *b* is ruled out because with a forced type "Claude calls exactly one tool", and no text is promised. *c* is ruled out because none "prevents Claude from using any tools".
42. **c**. The rule is to "return one tool_result for each tool_use block, all together in the next user message" (module 26, page 3). *a* is ruled out because "By default, Claude may call multiple tools in a single response". *b* is ruled out because the switch is a field of the choice object: "It is not a top-level request parameter". *d* is ruled out because "The most common issue is formatting tool results incorrectly in the conversation history".
43. **a and b**. "Tool search is on by default, so idle MCP tools consume minimal context" (module 27, page 1). *c* is ruled out because "CLAUDE.md is paid for on every request". *d* is ruled out because the context cost is "low until a tool is used", and not zero. *e* is ruled out because the loading table shows "tool names; full schemas on demand".
44. **d**. "toolset_name is what marks a block as a computer action" (module 31, page 1). *a* is ruled out because "A custom tool in the same request can share a member's name". *b* is ruled out because the page says to "Dispatch on the pair" of member and field, not on arrival order. *c* is ruled out because "The entry takes no display size", so blocks carry no dimensions.
45. **c**. "In the Python SDK, raise ToolError for an expected failure" (module 32, page 2). *a* is ruled out because protocol errors are for "issues with the request structure itself that models are less likely to be able to fix". *b* is ruled out because "The server MUST NOT write anything to its stdout that is not a valid MCP message". *d* is ruled out because a swallowed failure "tells the model the call worked".
46. **b**. "An answer of decline or cancel is a normal outcome" (module 33, page 1). *a* is ruled out because that error is for a capability the call did not declare, "which names the missing capabilities". *c* is ruled out because the page lists three forms of answer and says "a server handles each". *d* is ruled out because a retry exists to supply information: the client "retries the original request including the additional requested information".
47. **a**. "Giving feedback with the thumbs buttons stores the conversation for up to five years", on both plan types (module 10, page 1). *b* is ruled out because "Incognito chats are not used to improve Claude". *c* is ruled out because "Raw content fetched through a connector is excluded unless you copy it into the chat". *d* is ruled out because where improvement is off, "a deleted chat leaves the back end within 30 days".
48. **a and c**. The classifiers "are a second layer", and "Claude will follow commands found in content even when they conflict with your instructions" (module 31, page 2). *b* is ruled out because screenshots "are captured and stored in your environment, not by Anthropic". *d* is ruled out because the precautions begin with "a dedicated virtual machine or container with minimal privileges". *e* is ruled out because "They are a second layer", behind the container, the missing secrets, the allowlist and the confirmation.
49. **a**. The page says "Don't put your own instructions in tool results", so send them in a user turn after the block (module 41, page 1). *b* is ruled out because "Claude treats that content as untrusted data, so your instruction may be ignored", however often it is repeated. *c* is ruled out because third-party content goes "never in system prompts or plain user text blocks". *d* is ruled out because the JSON step applies to untrusted content: "Wrap third-party strings in a JSON object".
50. **c**. The page says to "return an error or a stripped summary in the tool_result block instead of the raw content" (module 41, page 3). *a* is ruled out because content is returned only "if the screen reports no injection attempt". *b* is ruled out because the page adds "consider surfacing the attempt to the user", which is not a reason to end the run. *d* is ruled out because "A list of patterns catches the phrasing someone thought of and misses the paraphrase".
51. **d**. Bare mode "never reads OAuth credentials or the system keychain", so a key must come from the environment (module 38, page 3). *a* is ruled out because bare mode "never reads OAuth credentials or the system keychain", so mounting it changes nothing. *b* is ruled out because the flag is "useful for CI and scripts where you need the same result on every machine". *c* is ruled out because the headless mode is meant to "Run Claude non-interactively for CI, pre-commit hooks, or batch processing".
52. **b**. When several hooks match, "the most restrictive answer applies", so the denial wins (module 39, page 2). *a* is ruled out because "A logging hook that exits 0 does not weaken a guard hook that denies". *c* is ruled out because "Several hooks can match one call, and they run in parallel". *d* is ruled out because the order is "in the order deny, defer, ask, allow", so deny outranks ask.
53. **a**. The harness "reports it as removed and fails the comparison" (module 42, page 3). *b* is ruled out because "An average never excuses a regression". *c* is ruled out because "A case that disappears is not a pass". *d* is ruled out because the page says "dropping the case that fails is the easiest way to improve a score".

</details>
