# Architect mock exam 1

**Level:** Architect · **Module 78:** Exam readiness 3 · **Page 3 of 5**
**Exams:** A1 to A5 (CCAR-F; the questions follow the Architect blueprint over the content of modules 45 to 77)

**After this page you can** tell whether you are ready for the Architect exam, which domains need more work, and which scenario slows you down.

This mock exam covers **the content of Level 3** (modules 45 to 77), as the Architect exam covers the scenario work of this level, and not one page or one module. It is written in the exam's style: a scenario, a situation of two or three sentences with a constraint, one best answer and three plausible alternatives (or two right answers among five options, where the question says Select two), each of which is a mistake a practitioner could make. Every question is the course's own, written fresh; no question comes from a live exam, and none repeats a question of a page quiz or a module quiz. The facts behind each answer were read on 2026-10-04 from the official pages named on the module pages, so a reader who studies the pages can answer each question from the page it is drawn from. The real exam has multiple-choice and multiple-response items; so does this mock: about one item in six ends with (Select two.) and is right only when both keyed options are chosen.

## How to take it

1. **Choose your conditions.** For a readiness test, take all 60 questions in **120 minutes** with no notes, which is the Architect exam's pace of 2 minutes an item. For learning, take it untimed and read each explanation.
2. **Answer every question.** The guide describes no penalty for a wrong answer.
3. **Mark each answer** and read the explanations only after you finish.
4. **Score by domain and by scenario,** not only in total, using the tables below, and spend your next study session on the weakest domain with the most weight.

There is no official conversion from a mock percentage to a result. **Pass or fail depends on the total: a scaled score of 720 on a scale from 100 to 1,000**, and the domain weights are not used for the result. Treat the mock as a guide to where you stand and never as a prediction.

## What it covers

The real exam draws four scenarios from a bank of six and sets 60 items on them. This mock follows that shape with S1, S3, S5 and S6, fifteen questions each, grouped by scenario here for easy scoring; the real exam mixes the order. The questions on the two scenarios beyond the exam's six (modules 76 and 77) sit in the nearest official scenario, since the bank has no other.

| Questions | Scenario | Count |
|---|---|---|
| 1 to 15 | S1 customer support resolution agent | 15 |
| 16 to 30 | S3 multi-agent research system | 15 |
| 31 to 45 | S5 Claude Code in CI | 15 |
| 46 to 60 | S6 structured data extraction | 15 |

The questions are spread by the blueprint's weights (each domain's weight times 60, rounded so that the total stays 60). The share column is the count divided by 60, set beside the official weight so that you can see the split matches.

| Domain | Weight | Count | Share of this mock | Questions | Drawn from modules |
|---|---|---|---|---|---|
| A1 Agentic architecture and orchestration | 27% | 16 | 26.7% | 1 to 5, 16 to 21, 31 to 32, 46 to 48 | 45 to 50, 71 |
| A2 Tool design and MCP integration | 18% | 11 | 18.3% | 6 to 9, 22 to 24, 33, 49 to 51 | 52 to 55 |
| A3 Claude Code configuration and workflows | 20% | 12 | 20.0% | 10 to 11, 25, 34 to 40, 52 to 53 | 57, 58, 60, 71, 74 |
| A4 Prompt engineering and structured output | 20% | 12 | 20.0% | 12, 26 to 27, 41 to 44, 54 to 58 | 61 to 63, 75 |
| A5 Context management and reliability | 15% | 9 | 15.0% | 13 to 15, 28 to 30, 45, 59 to 60 | 64 to 69 |

For a miss, open the page that the question's explanation names, as (module, page), and reread the passage it quotes. A cluster of misses in one domain points at that domain's modules in the last column; a cluster in one scenario points at the scenario module (70 to 77) and the modules it names.

## Mock exam

This mock exam covers the content of Level 3, modules 45 to 77, with questions set in four of the exam's scenarios. Choose one answer for each question, or the number the question states.

1. Scenario S1. A prompt says that no refund may be issued before the customer's identity has been verified. The sentence appears once, at the start of a long session, and after many turns the system begins to skip it. What explains this, and where should the requirement live?
   - **a**: The model loses interest in a rule after a set number of turns, so the sentence is repeated in every user message of the whole session
   - **b**: The context window resets between turns, so each request must carry the whole conversation by hand
   - **c**: The sentence is too short to carry weight, so it is lengthened with examples of identity checks
   - **d**: Compaction replaces early messages with a summary, so a rule that must hold throughout goes in CLAUDE.md or a hook

2. Scenario S1. A customer writes in one message that a refund is wanted on one order, an address has changed, and a charge appears twice. Nothing has been looked up yet, and the three concerns relate to an order, an address record and a card statement. Which two steps does the page give before any tool runs? (Select two.)
   - **a**: Pass the whole text to a person
   - **b**: Work on them in parallel with the shared context they need
   - **c**: Run the three matters one after another in a single chain, where each sees the previous conclusion
   - **d**: List the matters separately, each with its own question and result
   - **e**: Handle only the first matter and ask the customer to send the others separately

3. Scenario S1. A refund-limit hook should govern only the coordinator, yet it also fires for subagents that call the same tool. How can it tell the two apart?
   - **a**: Read the identity fields in its input present only for nested workers
   - **b**: Use a matcher that names the coordinator in its pattern
   - **c**: Register the hook only in the coordinator's options file
   - **d**: Wait for a subagent stop event and undo the call afterwards by hand and log it

4. Scenario S1. A gate refuses a refund because identity is not verified. A teammate suggests that the gate should run the verification itself and carry on. Why not?
   - **a**: A gate may only read state and can never call a backend service, whatever the rule of the system
   - **b**: Running it would double the cost of every refund request that the desk handles
   - **c**: Verification calls are blocked inside any hook or dispatcher by the platform
   - **d**: Only the end user can supply the code, so it should decline and state what is lacking

5. Scenario S1. In a loop built on the SDK, which party should own the decision whether a refund call runs, and where does that decision live?
   - **a**: The code, in permissions and hooks that act before execution
   - **b**: The model, through the stop reason of its reply to the call
   - **c**: The model, by weighing the policy text in its prompt each turn
   - **d**: A person, through a question after each call

6. Scenario S1. A support server exposes cancel_order, hold_order and release_order, which take the same inputs and return the same shape and differ only in the verb. What does the page advise?
   - **a**: Keep three separate tools, because separate names always improve the choice for the model
   - **b**: Fold them into a single tool with an action parameter, so that the model has fewer names to confuse
   - **c**: Split each by customer segment so that no two descriptions can overlap in the index
   - **d**: Merge them with a tool that infers the verb from the user's message each time

7. Scenario S1. A tool retries a transient billing failure, and the response carries a retry-after wait. Which two statements follow the page? (Select two.)
   - **a**: Without a hint, the pause doubles from a base
   - **b**: A constant pause of one second keeps the pressure even
   - **c**: The computed schedule is kept even when a wait is named
   - **d**: The delay that the service names replaces the computed value for that attempt
   - **e**: The hint is ignored and the retry is made at once

8. Scenario S1. A refund tool keeps getting amounts in the wrong format, and the model apologises after a few corrections. What does the page advise during development?
   - **a**: Raise the retry bound to let the model correct itself more often than it does now in each session
   - **b**: Ask the model in the prompt to stop apologising and keep trying until it succeeds
   - **c**: Write more detailed descriptions, and let the error name the field with an example value
   - **d**: Switch the field to free text to leave no value for the tool to reject

9. Scenario S1. A refund cap tool receives the boolean true as the amount in the model's call. The backend would accept the value without complaint, yet the cap check is meant to rely on whole numbers. What should the tool layer do?
   - **a**: Treat it as one unit and carry on with the cap check
   - **b**: Refuse it as malformed, with a result that says what is needed
   - **c**: Pass it through to the backend unchanged, which the backend accepts
   - **d**: Round it to the nearest allowed value and move to the next step

10. Scenario S1. A new engineer's session ignores the team rule that every endpoint validates its input. A senior engineer wrote the rule into the personal memory file in his home folder, and nothing in the repository carries it. Where should the rule go?
   - **a**: In the project's own instruction document, committed so that everyone receives it
   - **b**: In each engineer's home-folder file, since one exists for every developer
   - **c**: In a managed policy location, which applies to everyone on a machine and cannot be excluded
   - **d**: In a local-only note beside the project instructions that is kept out of version control

11. Scenario S1. A team's refund-issuing procedure is a skill with real side effects. Which frontmatter setting stops the model from deciding on its own that the time has come to run it?
   - **a**: user-invocable: false, which hides the slash entry from the person
   - **b**: allowed-tools with a narrow list, which prevents any other tool from being called by the skill
   - **c**: The flag that blocks automatic invocation, so only a person typing the name starts it
   - **d**: A description that tells the model to wait for confirmation before it starts

12. Scenario S1. A support team adds examples to the prompt of its refund decision reviewer. Besides judgement, what else do the examples steer?
   - **a**: The tone of the wording only, which is why they should sound friendly
   - **b**: The length of the prompt, which is why they should be kept short
   - **c**: The language of the reply, which is why they should use the customer's own words
   - **d**: The format of the answer, which is why they should mirror the real output

13. Scenario S1. Which escalation triggers can the application itself check in code, without relying on the model's recollection?
   - **a**: The customer's mood, measured by a count of exclamation marks in the message
   - **b**: The model's own confidence in its answer, which the model reports itself
   - **c**: Whether the issue sounds complicated to a reader of the transcript
   - **d**: Those that are plain facts, such as a spoken request for a person

14. Scenario S1. A developer's request sends only the newest customer message to the Messages API, and the reply reads as if the chat had just begun. Why?
   - **a**: Short messages are treated as new sessions by default in the platform
   - **b**: The reply is cached from the last request and replayed to the caller
   - **c**: The model resets its memory when a message is under ten words long
   - **d**: The platform stores no conversation, so each call must carry all the history it needs

15. Scenario S1. A support chat must shrink its history, and its oldest message is a tool call whose result is the next message. How should the window cut?
   - **a**: Remove the pair as a unit, since half of an exchange is an invalid record
   - **b**: Drop only the call and keep the result, since the result holds the facts
   - **c**: Drop only the result and keep the call, since the call records the intent
   - **d**: Keep both and drop the newest message instead

16. Scenario S3. A team plans to answer short factual look-ups from free-tier users with a coordinator and several helpers, because the published design does this. Each look-up is worth very little to the business. What is the sound objection?
   - **a**: Helpers cannot answer factual questions from an isolated context
   - **b**: A coordinator can only delegate to helpers whose descriptions are written in code
   - **c**: Such arrangements cost many times the tokens of a chat, so they suit only valuable tasks
   - **d**: Several helpers on one question always produce conflicting answers that need a human referee

17. Scenario S3. A research coordinator runs under a spending cap, and its subagents have used most of it. The cap is reached while two subagents are still working. Which two things happen in the Agent SDK? (Select two.)
   - **a**: The run is paused until the next billing period and then resumed
   - **b**: Starting another helper fails with a budget error
   - **c**: The running subagents finish, and only the coordinator's own calls count toward the cap
   - **d**: Background helpers stop and the query ends with a budget subtype
   - **e**: The cap is lowered for later subagents and the run continues on the remaining funds

18. Scenario S3. A research system must use only its own restricted subagents, but the coordinator sometimes starts the general-purpose one that comes with the SDK. The team defined three narrow specialists with limited abilities and wants no fallback to anything broader. Which setting prevents it?
   - **a**: A longer description on each custom definition, to make the model always prefer it to the built-in one
   - **b**: An allow rule for the Agent tool, which limits it to custom definitions
   - **c**: The environment variable that removes the bundled helper, after which such a call fails
   - **d**: A lower turn limit for the coordinator, to stop it reaching the built-in one

19. Scenario S3. A coordinator must run a style review, a security scan and a coverage check on one change, and then write a summary that uses all three results. How should it invoke the subagents?
   - **a**: Start them one at a time across separate turns, with nothing conflicting
   - **b**: Start all four together, while the summary agent reads the others' files as they appear on disk
   - **c**: Start the summary agent first, which asks the others for what it needs
   - **d**: Start the three together in one response, then compose the closing brief from their reports

20. Scenario S3. A coordinator session runs in bypassPermissions for a batch job, and a reviewer definition sets permissionMode to plan. The job runs unattended overnight on a build server. In which mode does the reviewer run?
   - **a**: Plan, because the definition is more specific than the session that started the whole batch job
   - **b**: Full autonomy, because the parent's blanket approval carries over and a helper's own setting cannot narrow it
   - **c**: Default, because the SDK lowers a subagent's mode to the safest one that exists in the product
   - **d**: Plan, because a subagent may never run in a mode more permissive than its own definition

21. Scenario S3. A coordinator's subagents keep spawning further subagents of their own, and the team wants to stop all nesting. Which setting does that?
   - **a**: Lower the maximum depth variable to one
   - **b**: Raise the concurrency variable to twenty, which limits spawn chains
   - **c**: Remove the Agent tool from the parent's allowed list
   - **d**: Set a budget cap so low that no deeper spawn can pay

22. Scenario S3. A research system connects many servers, and the SDK's automatic mode counts the tokens of deferrable definitions against the window. When does tool search become active?
   - **a**: At half of the window, which leaves a lot of room for the conversation with the user
   - **b**: At ten percent of the context size, below which everything loads up front
   - **c**: As soon as a second server connects, whatever the token count
   - **d**: Only when a person turns it on in the settings file

23. Scenario S3. A research team needs a standard integration with a widely used service. Which does the guide prefer, and which kind of work still justifies a build of the team's own?
   - **a**: A custom server, only a self-built one is trusted with company credentials
   - **b**: A custom server, with its descriptions tuned to the model that uses it
   - **c**: Several overlapping servers, and let the agent always choose the best one for the task
   - **d**: A reviewed existing connector, with fresh development kept for unique processes

24. Scenario S3. A team sets allowed_tools to Read and Grep for a writer subagent and expects that nothing else is available. What is true?
   - **a**: The rest is removed, since a list of allowed names is a whitelist that excludes whatever is omitted
   - **b**: The rest still exists, since the list only approves and unlisted calls are governed by the permission mode
   - **c**: The rest is denied, since unlisted calls need a person to approve each one individually
   - **d**: The rest is hidden from the model, but can still run if the model insists on it

25. Scenario S3. A skill with the setting context: fork is started from a research coordinator that has built up a long conversation. What does the forked subagent see?
   - **a**: A copy of the parent conversation, which a fork takes whole
   - **b**: The parent's tool results but not its messages
   - **c**: Its own instructions, so these need to stand on their own
   - **d**: The summary that compaction produced for the parent

26. Scenario S3. A research system's extraction returns claims with sources. Which two statements are true of the check that lets code catch a claim that no source supports? (Select two.)
   - **a**: The model double-checks its own output and reports changes
   - **b**: The schema gives every field a minimum length of several words
   - **c**: Each assertion carries a verbatim quote, tested by plain substring
   - **d**: A grounded value is not thereby a correct one
   - **e**: The model reports its confidence in each claim, and low ones are dropped

27. Scenario S3. A long report is extracted in chunks, and the figures of the chunks must be combined into one total. Who should add them?
   - **a**: The synthesis model, asked to add the figures
   - **b**: Ordinary code that recomputes the sum, never a model asked for arithmetic
   - **c**: The first chunk's extraction, which keeps its own total and never revises it
   - **d**: The last chunk's extraction, which keeps a running total

28. Scenario S3. Five topics were scheduled for research, and four were covered. The report leaves out the fifth. Which two statements describe what the coverage note does? (Select two.)
   - **a**: The topic is listed under no findings, among those that were searched
   - **b**: That section sets the plan against what happened
   - **c**: The topic is omitted from the note, like any topic with no entry
   - **d**: Something never looked into is recorded as a gap
   - **e**: The topic is listed as partial, among those that rest on part of their sources

29. Scenario S3. A pipeline wants every finding to carry a source. Where should a finding without one be refused?
   - **a**: At the point of entry, with the origin made part of the required schema
   - **b**: In the final report, where the writer drops the lines that lack a reference
   - **c**: At synthesis, where the model decides which findings are worth keeping
   - **d**: After publication, when a reader asks about the figure's origin

30. Scenario S3. One report is cited on two dates in a ledger, and no other source agrees with its figure. How is the claim classed in the coverage note?
   - **a**: Well supported, since two dates give two separate confirmations
   - **b**: Contested, since the same figure appears there with two dates
   - **c**: Changed, since a value was cited at different times in the ledger by the same publisher
   - **d**: As resting on a single origin, since a repeated citation counts once

31. Scenario S5. A guard script for Claude Code fails with an unhandled exception on unexpected input. What is the effect on the action it guards?
   - **a**: The call goes ahead, since a crash ends with status 1, which does not block
   - **b**: The call is blocked, because a crashed guard counts as a refusal
   - **c**: The whole session stops, because the crash is reported as a fatal error
   - **d**: The call is held until a person approves it, since a failed guard falls back to asking

32. Scenario S5. A CI session must not end its turn while the tests still fail, and a request in the prompt has not been reliable. Which control is stronger?
   - **a**: A hook on the stop event that runs the suite and keeps the run open until it passes
   - **b**: A stronger sentence at the end of the prompt asking for passing results
   - **c**: A larger turn limit to give the model time to fix the failures
   - **d**: A second model call that confirms whether the output looks done

33. Scenario S5. A developer adds a server named github in the local scope with another URL than the project file's entry of that name. Which definition does Claude Code end up using?
   - **a**: Both, in turn, since each definition is treated as a separate server by the client
   - **b**: Once, using the private version, since the highest-ranking source wins whole and no blending occurs
   - **c**: The project's entry, since a shared definition always outranks a private one on a team
   - **d**: A merge of the two, which takes the URL from one entry and the headers from the other entry

34. Scenario S5. Each item from a review becomes an inline comment with its file, line, severity and fix. Which extra field lets the team tune its criteria?
   - **a**: A free-text apology field that softens the tone of every comment
   - **b**: A hash of the whole diff to let duplicate comments be removed later
   - **c**: A label naming the detected pattern per entry, which makes dismissals analysable
   - **d**: The reviewer's temperature setting, to let separate runs be compared with one another

35. Scenario S5. A CI step sometimes loops for an hour, and its prompt already asks it to be quick. Which two controls does the page give? (Select two.)
   - **a**: A second sentence in the prompt names the minutes it may use
   - **b**: An environment variable switches on a quick mode
   - **c**: The tools that run without asking are listed on the command
   - **d**: A turn limit on the command ends the run
   - **e**: A longer job timeout gives the loop room to end itself

36. Scenario S5. A review of forty files in one pass gives detailed notes for some and a glance for others, and a team proposes a model with a larger window for the same pass. What is the verdict?
   - **a**: Repeat the single pass three times and keep what two runs agree upon
   - **b**: More room does not restore attention, so split the work per item and add a cross stage
   - **c**: Keep one pass but ask the reviewer to take equal care with every file
   - **d**: Lower the number of findings requested, and give each file a longer note

37. Scenario S5. An unattended pipeline gives its code-checking job a shell because it sometimes runs git. Why is that a finding?
   - **a**: Reading and searching suffice, so nothing able to change files should be listed
   - **b**: It is slower than the file tools and delays the whole checking job
   - **c**: Allowed-tools patterns cannot restrict it in an unattended pipeline run
   - **d**: It is needed for the job to read the diff, so it must stay

38. Scenario S5. A headless review step runs in a workflow with no interactive login. Which two statements describe how it should be set up? (Select two.)
   - **a**: The key is written into the workflow file as a literal, so every runner has it
   - **b**: The key is read from the developer's subscription login in the runner's keychain
   - **c**: The key is passed in the prompt, which the model attaches to its requests
   - **d**: The tool list is complete and minimal, with read tools and no bare shell
   - **e**: The API key comes from a repository secret exposed as an environment variable

39. Scenario S5. A CI runner relies on notes that Claude wrote to its auto memory on a developer's laptop. Will the runner have them?
   - **a**: Yes, since auto memory is committed with the repository
   - **b**: Yes, since it is synced through the cloud account for every session
   - **c**: Only the first 200 lines are synced, and the rest stays local
   - **d**: No, since those entries stay on the host that produced them

40. Scenario S5. A team asks what line count the documentation gives as the target for a root memory file, since its practice exercise sets a much lower limit for a small project.
   - **a**: Twenty-five lines, which the documentation sets for projects of a small size
   - **b**: Roughly two hundred, whereas the stricter cap is a testing choice
   - **c**: Five hundred lines, the ceiling the documentation names
   - **d**: No target at all, which the documentation leaves to each team

41. Scenario S5. A team adds minLength to a required field to stop placeholder strings. Why does that not work?
   - **a**: Placeholder strings tend to be longer than real values, so the limit rejects real ones
   - **b**: A length limit applies to optional fields and not to required ones
   - **c**: Decoding does not enforce it, and size cannot tell a real value from an invented one
   - **d**: The model ignores each constraint in a schema, whatever its kind

42. Scenario S5. A team puts a tool-using review into a batch. Which two results are possible? (Select two.)
   - **a**: An entry may come back as a call to run something
   - **b**: A multi-round procedure runs live, or each round becomes its own entry later
   - **c**: The entry is refused at submission
   - **d**: The entry streams partial answers that the client reassembles
   - **e**: An entry returns a finished answer from tools that the batch ran itself

43. Scenario S5. A developer at the keyboard asks for a review but names neither a branch nor a reviewer. Which handling does the page prefer for an attended session?
   - **a**: Ask about every missing detail before doing anything
   - **b**: Stop with a plain failure that lists the missing fields
   - **c**: Proceed silently on guesses, which leaves the developer uninterrupted
   - **d**: Ask only what cannot be assumed, and state the assumptions

44. Scenario S5. A CI extraction step fails the sum check and is retried. What must the second request hold besides the original document?
   - **a**: The faulty record and the specific errors with their fields
   - **b**: Nothing more, which gives a new sample each time it is sent again
   - **c**: Only the error message, to keep the request short
   - **d**: A note asking the model to try harder

45. Scenario S5. Six exploring workers may be lost to a crash. When should the coordinator write each worker's manifest entry?
   - **a**: When the worker finishes, so that the entry shows a final status for it in the ledger of the run
   - **b**: At launch of each one, so that an abrupt stop leaves a trace of what began
   - **c**: After each worker's last message, together with its closing summary
   - **d**: Once at the end of the whole job, rebuilt from the logs of all runs

46. Scenario S6. A fan-out extraction pass receives a document that holds only blank pages. What should the run do?
   - **a**: Send it with a note that it is empty so that the model can confirm that
   - **b**: Not send it, because a call with nothing to examine only costs money
   - **c**: Send it anyway and discard whatever comes back from the model
   - **d**: Split it into parts so that each part can be checked separately

47. Scenario S6. An extraction pass is limited to 200 lines per call, and a document has exactly 200 lines. Which two statements follow the page? (Select two.)
   - **a**: The document is cut into two parts, with the final line in the second
   - **b**: The document is cut into two parts and the second is dropped
   - **c**: Only a file too long for one run is cut
   - **d**: The document is cut into two parts that overlap by one line
   - **e**: A file as long as the maximum is one part, not two

48. Scenario S6. A post-call hook converts amounts from cents to decimals and dates to a readable form. A result also holds a field that the hook's table does not list. What should the hook do with it?
   - **a**: Drop it, so that the model sees just the fields the hook knows
   - **b**: Convert it with the closest rule, to keep the output uniform
   - **c**: Block the call, since an unknown field means that something failed
   - **d**: Pass it through as it was, since a normaliser changes only what it understands

49. Scenario S6. An extraction tool rejects a date given as next Friday. Which two statements describe the reply that lets the model repair its input? (Select two.)
   - **a**: It is a success with an empty field, which the next stage must ask about again
   - **b**: It is a transient error with retry set, which the user may answer by typing again
   - **c**: The message names the format to use
   - **d**: It is a protocol error with a generic code
   - **e**: It is a flagged execution error in the validation category

50. Scenario S6. A field accepts only open, shipped or closed. How should the tool schema express it?
   - **a**: As a sentence in the description that lists the three allowed words
   - **b**: As a free string, validated by the pipeline after the call returns
   - **c**: As an enum, so the model sees the allowed values and strict use refuses others
   - **d**: As a number code that the model must remember from the prompt text

51. Scenario S6. A third-party extraction server marks its delete_record tool as read-only, and a team plans to let the agent call it without confirmation on that basis. What is the objection?
   - **a**: Read-only hints are ignored by the SDK, which treats every integration as writable by default
   - **b**: Hints are verified by the client against the handler, so a false one is caught
   - **c**: Such hints are the vendor's claim and count as untrusted unless the source is trusted
   - **d**: Read-only hints exist only for built-in tools and cannot be set by a server

52. Scenario S6. A skill that extracts fields from documents sets context: fork and names no agent. Which subagent type runs it?
   - **a**: The Explore one, which is limited to read-only search
   - **b**: A new subagent that inherits the parent's conversation
   - **c**: None, which means the skill runs inside the main conversation
   - **d**: The general-purpose one, which is the default

53. Scenario S6. A skill body uses $1, and the user types only one argument. What does the second placeholder become?
   - **a**: It stays in the text, where a named one expands to nothing
   - **b**: An empty string in the place of the missing second argument
   - **c**: The first argument repeated in the second slot of the text
   - **d**: The full list of arguments joined together into a single string

54. Scenario S6. A document gives a currency that the closed enum does not list, and another document leaves the currency unstated. Which enum design handles both honestly?
   - **a**: Widen the enum with each new currency as it appears in a document
   - **b**: Turn the enum into free text, which leaves nothing to be refused
   - **c**: Map values that are not covered to the nearest covered value
   - **d**: Add unclear for the silent case, and other with a detail field for new values

55. Scenario S6. A dashboard reports 97 percent among the records that were accepted, yet a tenth of the clients' uploads broke and never reached the next system. Which base should the figure use?
   - **a**: Only the accepted records that reached the next system
   - **b**: All inputs that arrived, with each one lost on the way scored as wrong
   - **c**: A mean taken over the kinds of input, which gives rare kinds equal count
   - **d**: Only the records that a person reviewed by hand

56. Scenario S6. A pipeline handles invoices, receipts and credit notes, each with its own extraction tool, and the model allows forcing. Which setting fits?
   - **a**: The option that demands a call but lets the system pick which one
   - **b**: A named choice of the invoice extraction entry alone
   - **c**: Automatic choice, which leaves the model free to answer in text
   - **d**: No tools at all, with the answer parsed from prose by a regular expression

57. Scenario S6. A team makes every field of an invoice schema nullable, to be safe. Reviewers say that a fully optional schema feels safer because nothing can be rejected. What is the cost?
   - **a**: Nothing, and every field still arrives in the result
   - **b**: Checking is weaker, and null passes where a real value belongs
   - **c**: Parsing fails whenever one of the fields is left empty or missing
   - **d**: The model refuses the whole document whenever one field has no value

58. Scenario S6. Two chunks of one document give the vendor as null in the first and as a name with a quote in the second. How does the merge treat that field?
   - **a**: It keeps the earliest usable entry, with its passage
   - **b**: It keeps null, the value of the first chunk
   - **c**: It marks a conflict and asks a person to resolve it by hand
   - **d**: It joins both into one string separated by a comma and a space

59. Scenario S6. In a sample of accepted extractions, one wrong item fits no known pattern of mistakes. What does it signal?
   - **a**: A known weakness, to be logged and then ignored by the team
   - **b**: Noise from random sampling, which more samples will average out later
   - **c**: A reviewer slip, to be corrected by a second reviewer afterwards
   - **d**: A new kind of fault, which is the cue to change the pipeline

60. Scenario S6. On a labelled set, a team wants the precision of automation at a candidate confidence cut-off of 85. How is it measured?
   - **a**: Average the confidence values of the items that fall below 85 on the set
   - **b**: Take the items just under 85 and count how many of them are wrong
   - **c**: Take every item at or above 85 and see what share of them is right
   - **d**: Sample the accepted items by stratum and take the mean of the sample

<details>
<summary>Answer key</summary>

1. **d**. The page says what compaction does (module 45, page 2): "Compaction replaces older messages with a summary, so specific instructions from early in the conversation may not be preserved." *a* is ruled out because the cause is compaction and not boredom, and the remedy is placement: "Put rules that must hold for the whole run in `CLAUDE.md`, or enforce them in a hook." *b* is ruled out because the window does not reset: "The context window does not reset between turns within a session." *c* is ruled out because more words are not the remedy, since a hook "runs on every call, and a prompt does not always"
2. **b and d**. The coordinator lists the items before any tool runs so that each has its own question and its own result, and the guide asks it to "look into each of them in parallel with the shared context they need" (module 48, page 2). *a* is ruled out because a person is wanted for the item that needs one and not for the whole text: "If one item needs a person, the hand-off below is for that item, and the others are answered." *c* is ruled out because each item is worked on with its own question: "Each item is told its own question and not the other items' findings" *e* is ruled out because the point of listing first is that "none is lost behind another".
3. **a**. The page names the fields (module 49, page 1): "Hooks also run inside subagents, with an `agent_id` and an `agent_type` in the input (module 47), so a hook that must apply only to the coordinator has to look at those fields." *b* is ruled out because a matcher sees names only: "Matchers only match tool names, not file paths or other arguments." *c* is ruled out because a hook set up for the coordinator still fires in its workers: "Hooks fire inside subagents too." *d* is ruled out because a call cannot be undone afterwards: "A post hook can tell the model that something went wrong. It cannot make the refund not have happened."
4. **d**. The page gives the reason (module 48, page 1): "the customer's identity code is something the model must obtain from the customer, so the gate refuses and says what is missing". *a* is ruled out because the gate does call the backend after its checks: "For every tool call the model asks for, it checks the prerequisites, and only then does it call the backend." *b* is ruled out because the point is honesty and a small gate: "That keeps the conversation honest and keeps the gate small." *c* is ruled out because the gate does not repair: "The gate does not repair the call."
5. **a**. The page assigns the decision (module 45, page 1): "Permissions and hooks, before the tool executes". *b* is ruled out because the stop reason answers a different question: "The stop reason of its reply" *c* is ruled out because a model weighs text as advice: "Memory is advice with good delivery." *d* is ruled out because a hook runs on every call: "It runs on every call, and a prompt does not always"
6. **b**. The page gives the fix (module 52, page 1): "Group under one tool with an `action` parameter". *a* is ruled out because more is not better: "More tools don't always lead to better outcomes." *c* is ruled out because a tool is a unit of choice: "so the right number is the number of different decisions you want it" *d* is ruled out because a smarter merge is a larger change: "it is a larger change than a first step needs"
7. **a and d**. The page states it: "When the failure carries a wait (a rate limit with a `retry-after`), that value replaces the computed one for that attempt", and otherwise "The wait doubles from a base: 100, 200, 400 milliseconds" (module 53, page 2). *b* is ruled out because "A constant wait keeps the pressure on a service that is already struggling". *c* is ruled out because the computed value gives way to the named one: "The service's word first." *e* is ruled out because the wait exists to ease a service in trouble: "A constant wait keeps the pressure on a service that is already struggling"
8. **c**. The page quotes the documentation (module 53, page 2): "your best bet during development is to try the request again with more-detailed `description` values". *a* is ruled out because the model already retries a few times: it "will retry 2-3 times with corrections before apologizing to the user" *b* is ruled out because the message does the teaching: "A message that names the field and gives an example value" *d* is ruled out because a validation failure is repaired by one value: "A validation failure is repaired by changing one value"
9. **b**. The page lists the check (module 54, page 2): "an amount that is missing, a fraction, a string or a boolean is refused as a bad amount". *a* is ruled out because a cap needs a real quantity: "A cap needs a number" *c* is ruled out because the layer is code and judgement is not: "the model's own judgement is probabilistic, and the layer is code" *d* is ruled out because a refusal is a usable result: "Each refusal is a result that the agent can use (module 53): it says what is needed and who can give it"
10. **a**. The page states the fix (module 57, page 1): "The rule goes in the project file, committed." *b* is ruled out because a personal file travels with its owner: "so a new teammate never receives it" *c* is ruled out because the question is whether the text travels with the code: "whether it travels with the repository" *d* is ruled out because a local note is private: "A note about one project that nobody else needs, such as a sandbox address, goes in `CLAUDE.local.md`"
11. **c**. The page names the flag (module 58, page 1): "keeps the model from deciding by itself that the time has come". *a* is ruled out because that setting is for background knowledge: "Background knowledge that is no action, such as an explanation of a legacy system, is `user-invocable: false`." *b* is ruled out because a tool list is not a restriction: "the product treats it as a pre-approval: Edit and Write stay callable." *d* is ruled out because a description only helps the model find a skill: "For a skill Claude may invoke, the description is the only thing in context until it loads"
12. **d**. The page states it (module 61, page 2): "They should also mirror the real output: show the format the answer must have, since examples are the surest way to steer format as well as judgement." *a* is ruled out because examples are for the border cases: "Few-shot examples are not a sample of typical cases; typical cases are already handled by the criteria." *b* is ruled out because a count is a sanity check: "the guide asks for two to four targeted examples, and a long list teaches accidents" *c* is ruled out because examples teach accidents of form: "five examples that all end with the same label teach the label"
13. **d**. The page states it (module 65, page 1): "can be checked by the application, which then does not rely on the model's recollection of an instruction". *a* is ruled out because mood is not on the list: "Sentiment, a count of exclamation marks and the model's own confidence are not on the list." *b* is ruled out because a self-reported number is not calibrated: "It is not calibrated to whether the case is within policy" *c* is ruled out because a feeling says little about the case: "none of them says whether the case is within what the agent may decide"
14. **d**. The page states it (module 64, page 1): "The Messages API keeps no conversation: every request carries the whole history it needs". *a* is ruled out because the platform keeps no sessions to name, and the client carries the history: "In practice your code resends the whole history on every request." (module 1, page 1) *b* is ruled out because what to resend is a decision: "Trimming and summarising are therefore decisions about what to resend" *c* is ruled out because the window does not reset: "The context window does not reset between turns within a session."
15. **a**. The page states it (module 64, page 2): "The unit to drop is the exchange, not the message: a tool call and its result stay together or go together". *b* is ruled out because a half exchange breaks the record: "a result with no call" is invalid. *c* is ruled out because a call left alone is just as invalid: "or a call with no result". *d* is ruled out because the oldest goes first: "keep the newest turns that fit the budget and drop the oldest"
16. **c**. The article gives the price (module 46, page 1): "multi-agent systems use about 15× more tokens than chats", so the value has to pay for it. *a* is ruled out because isolation is not the obstacle: the page says what comes back, "the work is self-contained and can return a summary." *b* is ruled out because the coordinator chooses from natural-language descriptions: "Claude uses each subagent's description to decide when to delegate tasks." *d* is ruled out because conflict is not the objection, and the page ties effort to the question: "Simple fact-finding requires just 1 agent with 3-10 tool calls"
17. **b and d**. Once the cap is reached, "spawning another subagent fails with `Budget limit reached`" and Claude Code stops any background subagents still running, and the query ends with the `error_max_budget_usd` subtype (module 46, page 2). *a* is ruled out because Claude Code "stops any background subagents still running" once the cap is reached. *c* is ruled out because "the budget cap covers subagents: their spend counts toward the total." *e* is ruled out because a refused spawn is "a normal outcome of a run that spent its budget".
18. **c**. The page names the switch (module 47, page 1): "Setting `CLAUDE_AGENT_SDK_DISABLE_BUILTIN_AGENTS=1` removes the built-in agent, and such a call then fails with `subagent_type is required`." *a* is ruled out because a description steers the choice and does not remove the option: "Claude uses each subagent's description to decide when to delegate tasks." *b* is ruled out because an allow rule only skips approval: the Agent tool is among the tools "that don't ask before running", listed or not *d* is ruled out because a turn limit bounds a run and does not change which subagents exist; the page says "A team that must only ever use its own named, restricted subagents sets it, because the general-purpose agent has every tool."
19. **d**. The page says when parallel fits (module 47, page 2): "independent subtasks finish in the time of the slowest one rather than the sum of all of them", and a part that needs the others' output comes after them. *a* is ruled out because independent parts gain from running together: "they finish in the time of the slowest when run together, and several calls in one coordinator response start them together." *b* is ruled out because a part that needs another's output is a sequence: "It does not fit when one part needs another's output: that is a sequence" *c* is ruled out because the summary needs the reports first: "Run dependent parts in sequence, and write the second brief with the first result."
20. **b**. The page gives the rule (module 47, page 1): "A subagent runs in `bypassPermissions` mode only when the parent session itself does", and a definition's `permissionMode` applies only under a parent in `default`, `dontAsk` or `plan` mode. *a* is ruled out because specificity does not win: "A subagent runs in the parent session's permission mode unless you set `permissionMode` on its `AgentDefinition`" *c* is ruled out because nothing lowers the mode: "inheriting `bypassPermissions` grants them full, autonomous system access." *d* is ruled out because the definition does not cap the parent's mode: "An architect who runs the coordinator in a bypass mode for CI has also decided that for every subagent."
21. **a**. The page gives the default (module 46, page 1): nesting reaches "up to three layers below the main conversation" unless the depth variable is lowered to one. *b* is ruled out because concurrency counts parallel workers and not depth: "at most 20 subagents run at once by default" *c* is ruled out because a tool left off the allowed list still exists: "a tool that is not listed still exists" *d* is ruled out because a budget counts spending: "the budget cap counts the subagents' spending toward the total"
22. **b**. The page states the threshold (module 52, page 2): "When the total reaches 10% of the window, tool search activates." *a* is ruled out because the threshold is low because accuracy drops early: "Tool selection accuracy degrades with more than 30-50 tools loaded at once." *c* is ruled out because the trigger is a token count: "counts the tokens of the definitions that can be deferred" *d* is ruled out because it is not a switch left to a person: "Tool search is on by default, with the exceptions listed in Configure tool search"
23. **d**. The page states the preference (module 55, page 2): "For a standard integration the guide chooses an existing community server over a custom one, and keeps custom servers for team-specific workflows." *a* is ruled out because trust is checked for every server: "Verify you trust each server before connecting it." *b* is ruled out because the model reads names and descriptions of whatever exists: "The model picks a tool from its name and description" *c* is ruled out because overlap distracts: "Too many tools or overlapping tools can also distract agents from pursuing efficient strategies."
24. **b**. The page states it (module 54, page 1): "Any other tool not listed in `allowed_tools` is still available to Claude, and a call to it that needs approval falls through to the permission mode". *a* is ruled out because the name suggests more than the field does: "The restriction is real in the SDK, but it is not where the name suggests" *c* is ruled out because an unlisted call is not always put to a person: "calls that need approval go to your callback, and with no callback they are denied" (module 35) *d* is ruled out because removal is another control: "a bare tool name in `disallowedTools` removes the tool from the agent's context"
25. **c**. The page states it (module 58, page 2): "The subagent doesn't see your conversation history, so the skill's instructions have to stand on their own." *a* is ruled out because the name misleads: "The name misleads: this is not a fork of the conversation" *b* is ruled out because the results are withheld as well as the messages: "The parent's conversation history or tool results" (module 47, page 2) *d* is ruled out because the prompt is the skill: it "gives it the skill content as its prompt"
26. **c and d**. "The check is a plain substring test in code: it does not ask the model whether it is right", and "Provenance does not prove a value is correct, only that it is grounded" (module 62, page 1). *a* is ruled out because a self-check "carries the same blind spot as the finding". *b* is ruled out because "A length limit such as `minLength` is not the answer". *e* is ruled out because "the answer to a precision problem is explicit categories and examples, never an adjective about confidence".
27. **b**. The page states it (module 62, page 2): "recompute the total in code, not by asking a model to add". *a* is ruled out because a total that does not add up is a semantic error, and the page gives those to code: "Your code: checks that read the record against the source" (module 62, page 1). *c* is ruled out because the first value is kept for fields and not for totals: "keep the first value that is neither null nor unclear, with the quote it came with" *d* is ruled out because a different total is a conflict: "when a later chunk gives a different vendor or total, do not pick silently"
28. **b and d**. "A topic that was never searched is a gap", and the note is "the place where the system's own plan is compared with what happened" (module 66, page 2). *a* is ruled out because "`No findings`: topics that were searched and have nothing". *c* is ruled out because "A report that quietly omits a topic is saying something false by leaving it out". *e* is ruled out because "`Partial`: topics that rest on part of their sources, with the cause".
29. **a**. The page states it (module 69, page 1): "so that a finding without one is refused where it enters the pipeline, and not discovered in the final report". *b* is ruled out because sources cannot be rebuilt from prose: "the sources are lost in the compression and cannot be rebuilt from prose" *c* is ruled out because a prose hand-off drops attribution: "A single prose hand-off anywhere in the chain is where attribution drops out." *d* is ruled out because an unsourced statement is not returned as a finding: "A subagent that cannot name a source for a statement does not return it as a finding"
30. **d**. The page states it (module 69, page 2): "\"At least two distinct sources\" counts source names: the same report cited on two dates is one source, and agreement needs independence." *a* is ruled out because well supported needs two sources: "`agreed`, and the value has at least two distinct sources" *b* is ruled out because contested means different values on one date: "Several values on the same date" *c* is ruled out because changed means different values on different dates: "Several values on different dates"
31. **a**. The page names the trap (module 49, page 2): "an unhandled exception exits with 1, which does not block". *b* is ruled out because a crash looks like a refusal, and that is the trap: "It is tempting because a crash looks like a refusal" *c* is ruled out because for most events the result is mild: "the action goes ahead and a notice appears in the transcript" *d* is ruled out because nothing asks anyone: "A guard that crashes on unexpected input is therefore a guard that lets the call through."
32. **a**. The page names the control (module 71, page 2): a Stop hook "blocks the turn from ending until it passes". *b* is ruled out because a model stops when the work looks done: "Claude stops when the work looks done" *c* is ruled out because time does not add a check: "A longer job timeout only lets a loop run longer." *d* is ruled out because a judgement is not a gate: "a decision made on prose is a guess"
33. **b**. The page states it (module 55, page 1): "Claude Code connects to it once, using the definition from the highest-precedence source." *a* is ruled out because a clash is a conflict and not two servers: "Claude Code warns about a conflict in `claude mcp list` and in `/mcp`." *c* is ruled out because local ranks first: "The order is local, project, user, plugin-provided servers, then claude.ai connectors." *d* is ruled out because the private entry takes nothing from the team's: "they do not inherit the team's `Authorization` line"
34. **c**. The page names the field (module 60, page 2): "a `detected_pattern` field per finding lets the team analyse which patterns draw dismissals and tune the criteria". *a* is ruled out because the comment already has its fields: "each finding becomes an inline comment with its file, line, severity and suggested fix" *b* is ruled out because the prompt carries the history: "A record of findings between runs, which the prompt carries, is the only state the pipeline has." *d* is ruled out because repeating does not help: "Retrying inside the job repeats cost with no reason to expect a different result"
35. **c and d**. "`--max-turns` stops a loop, and `--allowedTools` lists what runs without asking, because nobody can approve a prompt" (module 74, page 1). *a* is ruled out because "A prompt that asks the run to be quick is a request, not a limit." *b* is ruled out because "There is no headless environment variable and no `--batch` flag on the command line". *e* is ruled out because "A longer job timeout only lets a loop run longer."
36. **b**. The page states it (module 74, page 2): "a larger context window does not fix attention quality". *a* is ruled out because the remedy fits the cause: "The remedy that fits the cause is to divide the work." *c* is ruled out because a request for care is not a mechanism: "A sentence that asks for more care is a request" *d* is ruled out because attention thins with the number of files: "attention thins as the number of files in one pass grows"
37. **a**. The page states it (module 74, page 2): "A shell tool is a way to change things, and a pipeline that nobody watches should not hold one for a job that only reads." *b* is ruled out because the point is capability and not speed: "A review changes nothing, so its tools are `Read`, `Grep` and `Glob`" *c* is ruled out because patterns do restrict: "`Bash(git tag *)` pre-approves `git tag v1.2.0` and not `git push --force`" *d* is ruled out because the diff arrives another way: "The diff goes in on standard input or in the prompt."
38. **d and e**. A bare run needs the key in the environment, "in the workflow that is a secret reference, never a literal", and "the tool list must be complete and minimal: read tools for a review, and no bare `Bash`" (module 60, page 1). *a* is ruled out because a key in a committed file is a leak: "Never commit API keys or OAuth tokens directly to your repository." (module 41, page 2) *b* is ruled out because "because it does not use a subscription login". *c* is ruled out because "A credential is a reference, never a value and never a default."
39. **d**. The page states it (module 57, page 1): the files "are not shared across machines or cloud environments". *a* is ruled out because the notes live in the home folder: "Claude writes its own notes to `~/.claude/projects/<project>/memory/`" *b* is ruled out because they stay on the machine: "keeps them on the machine" *c* is ruled out because the 200 lines are a load limit: "the first 200 lines of `MEMORY.md`, or the first 25KB, whichever comes first"
40. **b**. The page states it (module 71, page 1): "The documented target is 200." *a* is ruled out because the lower figure is the course's: "That number is the course's, chosen to make the point testable." *c* is ruled out because the root file loads at launch: "The root `CLAUDE.md` and the rule files without a `paths` list load at the start of every session" *d* is ruled out because length dilutes guidance: "A word on dilution, which the guide names as a reason to keep files short"
41. **c**. The page states it (module 62, page 1): "A length limit such as `minLength` is not the answer: constrained decoding does not enforce it (it is on the list of unsupported constraints), and it would not tell a real number from an invented one." *a* is ruled out because a placeholder looks like data: "And do not give a default that looks like data (`PO-0000`, `unknown`): downstream code cannot tell it from a real value." *b* is ruled out because the remedy is in the schema: "a field that may be missing from the source is optional or nullable" *d* is ruled out because the schema has to stay within what is supported: "Keep to the features structured outputs support"
42. **a and b**. "A request that offers tools may come back with a tool call as its answer", and a workflow that needs several rounds with tools "therefore runs synchronously, or is cut so that each round is its own entry in successive batches" (module 63, page 1). *c* is ruled out because "Tools, system prompts, earlier turns and extended thinking are allowed." *d* is ruled out because "the results come back as one file, not a stream". *e* is ruled out because "In a synchronous loop your code would run the tool and send the result in the next request".
43. **d**. The page states it (module 61, page 2): "to state the assumptions made for the rest". *a* is ruled out because questions are for what cannot be assumed: "ask only what cannot be assumed" *b* is ruled out because a failure belongs to the unattended case: "In an unattended run (CI) nobody can answer, so it never asks: it proceeds on stated assumptions where a default exists, and stops with a clear failure where a required field has none." *c* is ruled out because silence is not acceptable: "A job never passes by saying nothing."
44. **a**. The page states it (module 62, page 2): "The request on the second attempt holds three things: the original document, the failed record, and the specific errors". *b* is ruled out because a repeat gets the same result: "A retry that repeats the request gets the same answer with the same probability." *c* is ruled out because what went wrong is the useful part: "A retry that carries what went wrong gives the model something to correct." *d* is ruled out because some values cannot be supplied by any retry: "a retry cannot supply it and may fabricate it"
45. **b**. The page states it (module 67, page 2): "It writes an agent's entry when the agent starts, so that a crash leaves a trace of work that began and has nothing to show." *a* is ruled out because an entry exists while the work runs, its status being one of three: "its status (`running`, `done`, `failed`)" *c* is ruled out because the entry cannot wait for a last message that a crash prevents: "It is written as the agent learns and not only at the end, so that a crash half-way leaves something." *d* is ruled out because the manifest is read first on restart, so it cannot wait for the end: "the one thing the coordinator reads first"
46. **b**. The page states it (module 50, page 2): "A file of blank lines is not sent at all." *a* is ruled out because a note does not make the answer true: "returns an answer that sounds as if it reviewed something" *c* is ruled out because the call is paid for and gains nothing: "A call with nothing to review costs money" *d* is ruled out because splitting is for long files: "A file that does not fit a pass of its own is cut into parts of a fixed number of lines"
47. **c and e**. The page states it: "A file that is exactly as long as the limit is one part, not two", and a file is cut only when "A file that does not fit a pass of its own is cut into parts of a fixed number of lines" (module 50, page 2). *a* is ruled out because a cut is for a file that does not fit: "A file of four thousand lines does not fit a pass of its own." *b* is ruled out because no part of a long file is dropped: "The findings of the parts are joined in order, and the summaries are joined into one" *d* is ruled out because "The parts do not overlap, so a finding is not reported twice."
48. **d**. The page states the rule (module 49, page 2): "Fields that are not in the table pass through as they were." *a* is ruled out because a normaliser changes only what it understands: "Change only what you understand." *b* is ruled out because a readable value is left as it is: "A value that is already readable is not touched" *c* is ruled out because a post hook cannot stop anything: "It cannot make the refund not have happened."
49. **c and e**. The page classifies it: "an input validation failure, so a tool execution error with the flag, and the message can say which format to use" (module 53, page 1). *a* is ruled out because "Write instructive error messages." *b* is ruled out because "The input was wrong: a missing field, a value out of range, a bad format". *d* is ruled out because "A request that names a tool the server does not have is a protocol error, since there is nothing to run."
50. **c**. The page gives the benefit (module 52, page 1): "The model sees the allowed values in the schema, and a strict tool (module 26) refuses the others." *a* is ruled out because the schema carries what a script can check: "The schema carries what a script can check" *b* is ruled out because the schema is where syntax is removed: "The schema: constrained decoding or strict tool use" *d* is ruled out because the model reads the schema each time: "A tool is therefore a name, a description and a schema, and the model reads all three each time it decides."
51. **c**. The page cites the specification (module 52, page 2): "MUST consider tool annotations to be untrusted unless they come from trusted servers". *a* is ruled out because the SDK does use the hint: "The Agent SDK uses the hints in one place" *b* is ruled out because a hint is not checked against the handler: "A tool marked `readOnlyHint: true` can still write to disk if that's what the handler does." *d* is ruled out because servers do set them: "Optional properties describing tool behavior"
52. **d**. The page states it (module 58, page 2): "`context: fork` starts a new subagent of the type named in `agent` (the default is `general-purpose`)". *a* is ruled out because Explore is a separate built-in: "Claude Code ships an Explore subagent for exactly this work" *b* is ruled out because a forked skill sees no history: "The subagent doesn't see your conversation history" *c* is ruled out because a forked skill runs apart from the conversation: "interactively it runs in the background unless `background: false`"
53. **a**. The page states it (module 58, page 2): an indexed placeholder with no argument "stays in the content unchanged". *b* is ruled out because that is the behaviour of a named placeholder: "while a named one expands to an empty string" *c* is ruled out because indexes count from zero and do not wrap: "The argument at index N, counting from 0" *d* is ruled out because the full list belongs to another placeholder: "All arguments, as typed"
54. **d**. The page gives the pair (module 62, page 1): "`other` plus a detail field for a value the enum did not foresee". *a* is ruled out because widening hides the question: "Both beat the alternative of widening the enum for every new value, which hides the question of whether the value matters." *b* is ruled out because free text cannot be compared: "with closed enums for category and severity it makes the policy computable" *c* is ruled out because a closed set forces a choice: "A closed enum forces a choice even when the document is silent or the value is new."
55. **b**. The page defines it (module 75, page 1): "A document that failed is a document the customer sent." *a* is ruled out because validated records alone flatter the run: "The figure on the validated ones alone is a different figure and is always at least as high." *c* is ruled out because an average hides weak kinds: "an average over easy documents" *d* is ruled out because reviewed records are a subset: "The accuracy of a run is the share of all documents whose delivered record is correct."
56. **a**. The page states it (module 62, page 2): "On a model that supports forcing, a pipeline with several document types uses `any`, and a pipeline with one type may name it." *b* is ruled out because a named tool fixes one choice: "One particular tool must be the call of this request" *c* is ruled out because automatic choice guarantees nothing: "Nothing: it may answer in text" *d* is ruled out because prose is not a status: "a decision made on prose is a guess"
57. **b**. The page states it (module 62, page 1): "Do not make every field nullable; nullability is for values that can really be absent, since a required field with a valid value is a stronger check." *a* is ruled out because a null in a key field has a price: "a null in one of them sends the document to review instead of passing it" *c* is ruled out because a field may be missing from the source and then be nullable: "a field that may be missing from the source is optional or nullable" *d* is ruled out because a schema cannot express a refusal, and a missing value is answered with null: "refuse, which a schema cannot express, or fill the field" and "so that `null` is a valid and honest answer"
58. **a**. The page states the rule (module 62, page 2): "keep the first value that is neither null nor unclear, with the quote it came with". *b* is ruled out because null means the source did not state it: "a field that may be missing from the source is optional or nullable" *c* is ruled out because a conflict needs two real values: "when a later chunk gives a different vendor or total, do not pick silently" *d* is ruled out because only line items are joined: "concatenate the line items"
59. **d**. The page states it (module 68, page 1): "a wrong item that fits no known pattern is a novel error, which is the signal to change the pipeline". *a* is ruled out because new kinds appear when formats change: "the true error rate among accepted items is unknown, and new kinds of error appear when document formats change" *b* is ruled out because the sample does two jobs: "Checking the accepted sample does two jobs." *c* is ruled out because a single pull says almost nothing about hard strata: "A single random pull from the whole stream is mostly easy cases and says almost nothing about the small, hard strata."
60. **c**. The page states it (module 68, page 1): "For a candidate threshold, take every item at or above it, and measure how many are right. This is the precision of automation at that threshold." *a* is ruled out because the first step groups by confidence: "Group the labelled items by the confidence they were given." *b* is ruled out because items below the cut-off are not what automation accepts: "Everything below it goes to a person." *d* is ruled out because stratified sampling is a different job: "The sample must be random within each stratum"

</details>
