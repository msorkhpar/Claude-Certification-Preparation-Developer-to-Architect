# Architect mock exam 2

**Level:** Architect · **Module 78:** Exam readiness 3 · **Page 4 of 4**
**Exams:** A1 to A5 (CCAR-F; the questions follow the Architect blueprint over the content of modules 45 to 77)

**After this page you can** tell whether you are ready for the Architect exam, which domains need more work, and which scenario slows you down.

This mock exam covers **the content of Level 3** (modules 45 to 77), as the Architect exam covers the scenario work of this level, and not one page or one module. It is written in the exam's style: a scenario, a situation of two or three sentences with a constraint, one best answer and three plausible alternatives, each of which is a mistake a practitioner could make. Every question is the course's own, written fresh; no question comes from a live exam, and none repeats a question of a page quiz or a module quiz. The facts behind each answer were read on 2026-10-04 from the official pages named on the module pages, so a reader who studies the pages can answer each question from the page it is drawn from. The real exam has multiple-choice and multiple-response items; this mock has single-answer items only.

## How to take it

1. **Choose your conditions.** For a readiness test, take all 60 questions in **120 minutes** with no notes, which is the Architect exam's pace of 2 minutes an item. For learning, take it untimed and read each explanation.
2. **Answer every question.** The guide describes no penalty for a wrong answer.
3. **Mark each answer** and read the explanations only after you finish.
4. **Score by domain and by scenario,** not only in total, using the tables below, and spend your next study session on the weakest domain with the most weight.

There is no official conversion from a mock percentage to a result. **Pass or fail depends on the total: a scaled score of 720 on a scale from 100 to 1,000**, and the domain weights are not used for the result. Treat the mock as a guide to where you stand and never as a prediction.

## What it covers

The real exam draws four scenarios from a bank of six and sets 60 items on them. This mock follows that shape with S2, S4, S6 and S1, fifteen questions each, grouped by scenario here for easy scoring; the real exam mixes the order. The questions on the two scenarios beyond the exam's six (modules 76 and 77) sit in the nearest official scenario, since the bank has no other.

| Questions | Scenario | Count |
|---|---|---|
| 1 to 15 | S2 code generation with Claude Code | 15 |
| 16 to 30 | S4 developer productivity agent | 15 |
| 31 to 45 | S6 structured data extraction | 15 |
| 46 to 60 | S1 customer support resolution agent | 15 |

The questions are spread by the blueprint's weights (each domain's weight times 60, rounded so that the total stays 60). The share column is the count divided by 60, set beside the official weight so that you can see the split matches.

| Domain | Weight | Count | Share of this mock | Questions | Drawn from modules |
|---|---|---|---|---|---|
| A1 Agentic architecture and orchestration | 27% | 16 | 26.7% | 1 to 2, 16 to 19, 31 to 33, 46 to 52 | 45, 49 to 51 |
| A2 Tool design and MCP integration | 18% | 11 | 18.3% | 3, 20 to 23, 34 to 35, 53 to 56 | 52, 53, 55, 56, 77 |
| A3 Claude Code configuration and workflows | 20% | 12 | 20.0% | 4 to 11, 24 to 26, 57 | 57 to 59, 71 |
| A4 Prompt engineering and structured output | 20% | 12 | 20.0% | 12 to 14, 36 to 42, 58 to 59 | 61 to 63, 75, 76 |
| A5 Context management and reliability | 15% | 9 | 15.0% | 15, 27 to 30, 43 to 45, 60 | 64, 66 to 69 |

For a miss, open the page that the question's explanation names, as (module, page), and reread the passage it quotes. A cluster of misses in one domain points at that domain's modules in the last column; a cluster in one scenario points at the scenario module (70 to 77) and the modules it names.

## Mock exam

This mock exam covers the content of Level 3, modules 45 to 77, with questions set in four of the exam's scenarios. Choose one answer for each question.

1. Scenario S2. A hook that must replace what the model sees as a tool's output has to work for built-in tools and for MCP tools, in both SDKs. Which field should it set?
   - **a**: additionalContext, since it overwrites the whole result with the text that is given
   - **b**: The newer successor setting, since the older one is limited to one server type and is deprecated
   - **c**: updatedInput, since it rewrites the call before the model reads the answer
   - **d**: permissionDecisionReason, since the model reads that text in place of the actual result

2. Scenario S2. An adaptive loop halts when its planner issues a step it has already run. After a while the planner re-issues the step with different capitalisation. How should the check treat it?
   - **a**: Compare the exact bytes, since any difference between two steps shows real progress
   - **b**: Ignore case and surrounding spaces, since a faltering one seldom copies itself byte for byte
   - **c**: Compare only the first word of each step, since the rest is only detail
   - **d**: Skip the check altogether and rely on the step limit alone to end the run

3. Scenario S2. An Edit call is refused because the target text appears three times in the file, and only one of the places should change. What is the first remedy?
   - **a**: Set replace_all, which changes just the line that was meant and no other
   - **b**: Read the file again and then retry the identical call
   - **c**: Use Read and Write to rewrite the whole file
   - **d**: Lengthen the anchor string until just the intended spot matches

4. Scenario S2. A developer has a deploy skill in ~/.claude/skills and the repository has its own deploy skill. Both places hold a skill called deploy, and the developer types the slash command without any prefix. Which one runs on /deploy?
   - **a**: The repository's, since committed files always override personal ones on every machine that uses the project
   - **b**: Neither, since a name clash disables both of them until one is renamed
   - **c**: Both, one after the other, starting with the committed one
   - **d**: The home-folder one, because that location outranks the committed one for a shared name

5. Scenario S2. A team rule seems ignored in a session. Which command shows what instruction files actually loaded?
   - **a**: /context, since an entry missing from its list cannot be seen by the model
   - **b**: /memory, which lists the files that loaded and the ones that were skipped, with the reason for each
   - **c**: /clear, which reloads every file from disk
   - **d**: /compact, which prints the instructions that survive summarising

6. Scenario S2. A developer approves a plan in plan mode and picks the option that auto-accepts edits. What does approval do to the session?
   - **a**: It exits that state and switches to the permission setting that the choice describes
   - **b**: It stays in plan mode and queues the edits until the end of the whole session
   - **c**: It restarts the session with the plan as its very first message
   - **d**: It applies the plan to a copy of the repository before touching the real files of the project

7. Scenario S2. A developer asks Claude to write the code and then its tests from that code. The session produced twelve tests that all pass at once. Why is that weak?
   - **a**: Tests written after the code run more slowly than tests written before it was started by the team
   - **b**: Claude cannot write tests for code that it wrote itself earlier in the same session
   - **c**: Generated tests are rejected by the runner unless a person has signed them off first
   - **d**: Cases born of one misreading confirm nothing, so the first checks should come from a person

8. Scenario S2. A skill's description and when_to_use text together run to several thousand symbols. What happens in the skill listing?
   - **a**: The skill is dropped from the listing until the wording is shortened by its author
   - **b**: They are cut at 1,536, so the key use case belongs first
   - **c**: Only the description is shown and when_to_use is discarded from the listing
   - **d**: A model call summarises the wording before the skill is listed for use

9. Scenario S2. A team has a repeatable procedure that should only enter context when someone starts it. Where does it belong?
   - **a**: In a project skill, whose body loads only when it is invoked
   - **b**: In the root memory file, which loads at launch
   - **c**: In a scoped rule with a paths list, which loads on a match between the files of the project
   - **d**: In the settings file as a hook that runs at session start

10. Scenario S2. A developer wants short answers in every project on their machine, and the team does not. Where does that preference go?
   - **a**: In the project's root memory file, committed so that the whole team receives it
   - **b**: In a local note beside the root file, ignored by version control on every machine
   - **c**: In the user-level memory document or the user rules folder in the home directory
   - **d**: In the managed policy file that the organisation distributes to its machines

11. Scenario S2. A developer asks Claude to refactor a module, and the session ends with code that looks finished but breaks a test. The prompt asked only to clean up the module and gave no further instruction. What was missing?
   - **a**: A longer prompt describing the module in much more detail than before
   - **b**: A higher thinking budget so that the model reviews its own work harder
   - **c**: A second conversation that reviews the refactor once the session is over and reports back to the team
   - **d**: A runnable check, since otherwise an appearance of completion is the only signal

12. Scenario S2. A review prompt holds standards, a few samples and the material under review. In what order should they appear?
   - **a**: The material first, so that the model reads the input before any instruction
   - **b**: Samples first, then the material, with the standards in a trailing footer
   - **c**: Interleaved, with each sample placed beside the part of the material it resembles
   - **d**: Rules first, then worked cases each in its own tag, with the input to judge last

13. Scenario S2. A batch finishes with some entries succeeded, some expired, and one rejected as an invalid request. What is the sound reaction?
   - **a**: Resubmit the whole batch so that the results stay consistent with one another
   - **b**: Resubmit all the failures unchanged, since errors of this kind are transient
   - **c**: Keep the completed ones, resend the timed-out ones as they were, repair the faulty one
   - **d**: Discard the batch and run all of the work synchronously instead

14. Scenario S2. A bulk job submits batch entries that set stream to true. What happens?
   - **a**: Results stream back entry by entry as they finish, within the window of the batch
   - **b**: The flag is ignored and results arrive after the window has closed
   - **c**: It is refused with a validation error, since results come back as one file
   - **d**: The batch is accepted but billed at the full price of the live API

15. Scenario S2. A team proposes to log every tool result into the notes file during an exploration. Why is that wrong?
   - **a**: A file cannot be read back into a session once it has been written
   - **b**: Notes files are loaded into every session like an instructions file is
   - **c**: Working memory should hold findings, not a transcript, which is what filled the window
   - **d**: Tool results contain nothing that is worth keeping for later questions

16. Scenario S4. On Wednesday an engineer wants to carry on Monday's exploration of the payments module as a single line of work, although two other sessions have run in that directory since. Which control fits?
   - **a**: Resume that session by its id, which adds to the original and keeps one thread
   - **b**: Fork the session, since a fork keeps the whole history and carries on the work
   - **c**: Continue the most recent session, which takes the newest one whatever it is
   - **d**: Start fresh with a prompt that asks the model to recall Monday from memory

17. Scenario S4. Two forks of one session each try a refactor in the same working directory. What happens?
   - **a**: Each gets a private copy of the whole directory, so that the two attempts are isolated
   - **b**: Both edit the shared files, since branching copies the conversation and not the disk
   - **c**: The second is rejected at once, since a directory admits just one branch at a time
   - **d**: Their edits are merged automatically by the tool when the sessions end

18. Scenario S4. A service runs an exploration on one machine, and an engineer wants to continue it on a CI worker. What limits this?
   - **a**: Sessions can be resumed only by the account that created them, whatever host is used
   - **b**: A saved session expires after one hour of idleness and cannot be reopened
   - **c**: Only the Agent SDK can resume sessions, never the command line
   - **d**: Saved conversations stay on the host that wrote them, so copy them or use a digest

19. Scenario S4. A process died while a tool call was running, and the session is resumed. What does the model see?
   - **a**: The operation flagged as interrupted before its outcome was stored, with a prompt to verify it
   - **b**: The call is finished again silently when the session resumes, without any notice
   - **c**: The call is shown as succeeded, since the process had already started it
   - **d**: The history is cut at the last completed message and the session carries on as normal

20. Scenario S4. A team denies Write(secrets/**) to keep the agent from changing a folder. Why does that protect nothing?
   - **a**: Deny rules apply only to shell commands and are not applied to any of the file tools
   - **b**: Folders that hold credentials are exempt from deny rules for reading and for writing
   - **c**: Deny rules are advisory, and the model may decide to ignore them when it sees fit
   - **d**: Path checks for modifications use the edit spelling, so a rule spelled the other way is never used

21. Scenario S4. A team scans code that an agent wrote for dangerous calls and runs it unattended whenever the scan is clean. What is wrong with relying on this alone?
   - **a**: Code written by a model cannot be read by any automatic check
   - **b**: A clean result proves nothing about what the code declares, so the proposal is ignored
   - **c**: Running it on every proposal costs too much, so it must be sampled
   - **d**: Detection covers only what its authors anticipated, so a disguised version slips past it

22. Scenario S4. A tool design whose name breaks the required form also declares an effect that needs a person's sign-off. What should happen first?
   - **a**: It goes straight to the person, who can fix the name while approving
   - **b**: It returns for revision, so approval is asked only of a proposal worth reading
   - **c**: It is refused, since any format error is treated as a forbidden call
   - **d**: It is approved outright, since a name has no effect on safety

23. Scenario S4. A documentation tool's description is long, with its key rule in the last paragraph. What does Claude Code do with the text?
   - **a**: It reads all of it, since only the first sentence is shown to the user in the list, as a rule of the product
   - **b**: It summarises the description with a model call when the text is very long indeed
   - **c**: It truncates at 2,048 characters, so critical details should sit near the beginning
   - **d**: It rejects the server at launch with an error message naming the limit in force

24. Scenario S4. A skill from an installed plugin has the same folder name as a project skill. How can both be used?
   - **a**: The extension's is prefixed with its package label, so two separate commands exist
   - **b**: The project's overrides the plugin's, since committed files win over anything installed
   - **c**: The plugin's overrides the project's, since plugins are newer than the repository
   - **d**: Neither runs until one of them is renamed to remove the clash

25. Scenario S4. A user-level file and a repository-level file give conflicting style advice. Which appears later in context?
   - **a**: The user-level one, since personal preferences are applied last in the order
   - **b**: Neither, since conflicting advice is dropped before anything is loaded
   - **c**: The more specific one, since loading runs from the broadest scope to the most specific
   - **d**: Whichever was edited most recently, since the loader sorts by time

26. Scenario S4. A skill lists disallowed-tools: Bash(rm *). What does that do while the skill is active?
   - **a**: It denies matching commands and keeps the shell available, since only a bare name removes it
   - **b**: It removes the shell from the available pool for the whole turn of the skill
   - **c**: It has no effect, since specifiers are ignored in the headers of skills
   - **d**: It blocks every command until the user sends the next message to the session

27. Scenario S4. A developer wants the built-in exploring subagent to dig deeply into a tangled module. What can the caller request?
   - **a**: A level of thoroughness, from quick through medium to very thorough
   - **b**: A write permission, from none to full access to the whole tree of the project
   - **c**: A larger window, from standard to extended for the long search
   - **d**: A count of files to read, from ten up to a thousand files

28. Scenario S4. A large exploration moves from mapping modules to tracing flows. How should the findings of the first phase reach the second?
   - **a**: A digest from the notes goes into the opening prompts of the later workers
   - **b**: Pass the full transcript of the first phase to each second-phase worker so that nothing is lost
   - **c**: Let the second phase rediscover them, since fresh eyes avoid bias
   - **d**: Keep the first phase's workers running and reuse their windows

29. Scenario S4. A manager tells a helper to explore the whole repository and gets back a long account. What was wrong with the delegation?
   - **a**: The helper should have been given write access so that it could keep notes
   - **b**: The helper's window was too small for such a long search of the tree
   - **c**: The brief was too broad, so the reply gave its author nothing new
   - **d**: The summary should have been requested in a table instead of prose

30. Scenario S4. A coordinator runs five sources in order, and the second fails with a timeout. The run stops there. What does that waste?
   - **a**: Everything scheduled after the broken one, whose work was never done
   - **b**: Only the failed source's partial results, which were small anyway and a little context
   - **c**: Nothing, since later sources depend on the second one
   - **d**: The retry budget, which is reset by stopping

31. Scenario S6. A normalising hook runs twice on the same result, for instance when a result is replayed. Which property should it have?
   - **a**: Determinism across tools: the same date shape is applied to each field that it sees
   - **b**: Statelessness: it must not read the tool input of the call that it handles
   - **c**: Idempotence: an already readable value is left alone and the answer stays empty
   - **d**: Atomicity: it must write the fields together or not write any of them

32. Scenario S6. A team registers a hook with the matcher Write|Edit. How is that value read?
   - **a**: As a regular expression that also matches names containing either word in the middle
   - **b**: As a list of two exact tool names, because it holds only letters and a pipe
   - **c**: As a path filter for the locations that those tools write to disk
   - **d**: As a request to match both names only when they occur in the same turn

33. Scenario S6. A fan-out runs one extraction pass per document, then a cross-check pass over their summaries. One of twelve passes fails. What should the run do?
   - **a**: Abort the whole run, since one failure makes the rest unreliable
   - **b**: Log it with its message, carry on with the rest, and exclude it from the last step
   - **c**: Retry that one forever until it succeeds, then continue
   - **d**: Give the last step an empty summary for it, so that the count stays complete in the report

34. Scenario S6. Logs of an extraction agent show many redundant tool calls and few errors. What does the article suggest?
   - **a**: Rewrite the descriptions with clearer examples
   - **b**: Remove half of the tools so that fewer calls are possible
   - **c**: Raise the output ceiling so that each call returns more
   - **d**: Rightsize the pagination or token limit parameters

35. Scenario S6. An extraction tool looks up a vendor record by its number, and the call times out. Which handling does the page give?
   - **a**: Send it again only after the vendor service confirms the first call did not run
   - **b**: Treat the timeout as proof that the record does not exist
   - **c**: Send it again, since repeating a read changes nothing
   - **d**: Send it once more with a new record number, so that attempts are told apart

36. Scenario S6. An extraction stores for each field the quote it came from. What does a passing check prove?
   - **a**: That the value is correct, since the quote came from the source
   - **b**: That the model is confident, since it supplied a quote
   - **c**: That the schema is valid, since provenance is a required object
   - **d**: That the value is grounded in the text, not that it is correct

37. Scenario S6. A schema gives a missing vendor name the default unknown. Why does that hurt downstream?
   - **a**: It breaks the parser, since a string is not allowed for that field in the schema
   - **b**: It lowers accuracy in the report, since defaults count as errors
   - **c**: It forces the model to guess the vendor on every record
   - **d**: Code cannot tell it from a real value, so a placeholder passes as genuine

38. Scenario S6. A team plans a weekly extraction over thousands of stored documents and wants the lowest cost. Which interface fits, and what must each entry carry?
   - **a**: Batch processing, with a unique custom_id per request so that results match their inputs
   - **b**: The synchronous interface with streaming, since results arrive sooner that way
   - **c**: Batch processing with stream set to true, so that results arrive one by one
   - **d**: The synchronous interface in parallel threads, since discounts need a contract

39. Scenario S6. A pipeline's overall accuracy clears its target, and the team proposes dropping manual review for everything. What does the page say decides this?
   - **a**: The overall average, since a figure above target covers every kind
   - **b**: Evidence for each kind of input, never the average
   - **c**: The workload that reviewers face in a normal week
   - **d**: The model's own confidence on each record it returns

40. Scenario S6. A record has the currency other and no currency_detail. How does the pipeline classify it?
   - **a**: As a semantic error, since the value is plausible but unchecked
   - **b**: As a syntax error, because the pair is the contract
   - **c**: As valid, since other is a member of the enum
   - **d**: As an absent value that goes straight to review untried

41. Scenario S6. A batch's results file lists entries in a different order from the requests. How should the pipeline pair each result with its request?
   - **a**: By position, since results mirror the sequence in which the entries were sent to the batch service
   - **b**: By matching the text of each answer against the text of each prompt sent
   - **c**: By the unique label that each submission carries, because sequence is not promised
   - **d**: By timestamps, since the earliest request normally finishes first

42. Scenario S6. One pass reports the same defect twice, and another pass reports it once. How many independent confirmations are there?
   - **a**: Two, since a reviewer that repeats itself still counts as one voice
   - **b**: Three, since each report of the defect is a confirmation
   - **c**: One, since the second reviewer only repeats the first one
   - **d**: Zero, since independent confirmation needs at least three reviewers

43. Scenario S6. On a labelled set, no confidence cut-off reaches the 95% target. What follows?
   - **a**: Use the cut-off with the highest precision and automate everything above it
   - **b**: Lower the target until some cut-off qualifies for automation
   - **c**: Ask the model to restate its confidence on a hundred-point scale
   - **d**: Every item needs review until the pipeline improves, since no honest boundary exists

44. Scenario S6. A subagent cannot name a source for a statement that the schema demands one for. What should it do?
   - **a**: Invent a plausible citation so that the schema validates the finding
   - **b**: Cite the search engine as the source of each unsourced claim
   - **c**: Return it as a finding with an empty reference field and a warning
   - **d**: Report it apart as lacking an origin, or omit it, rather than return it as a finding

45. Scenario S6. Six extraction workers may be lost to a restart. When should each write its state file?
   - **a**: As it learns, so that a crash half-way still leaves something
   - **b**: At the end only, when its findings are complete and checked
   - **c**: Once an hour on a timer, whatever it has learned in that time
   - **d**: Only when a person asks for a checkpoint of its work

46. Scenario S1. A loop checks a turn limit before every model call. On one run the limit is ten, and the tenth reply ends its turn with a full answer. Which status should a correct loop report?
   - **a**: Max turns, because the number of calls reached the limit on that very reply of the run
   - **b**: Truncated, because the loop made its last permitted request and must therefore assume that the text was cut off
   - **c**: Unexpected, because a loop that has no call left cannot confirm how the reply ended
   - **d**: Done, because the tally was tested before that last request and nothing was cut

47. Scenario S1. A reply that describes a refund policy ends with the stop reason `max_tokens`, in the middle of a sentence. What should the loop do?
   - **a**: Treat the run as finished, since the model stopped producing tool calls
   - **b**: Leave with a status of its own, because the output was cut off and a higher ceiling is the code's call
   - **c**: Call the model again at once with identical messages, since a cut-off reply is a transient fault of the service
   - **d**: Ask the model to repeat the answer in fewer words, since the ceiling is the reply's fault

48. Scenario S1. A pre-call hook denies refunds above 500. The hook returns a short sentence that the model reads before it decides what to tell the customer. Which reason should it return to the model?
   - **a**: Forbidden by company policy for this kind of request
   - **b**: Amounts over that limit go to a person, so hand this case over
   - **c**: Permission denied for the requested action by the rules
   - **d**: Blocked for now: please retry the same call again in a moment or two, after a short pause

49. Scenario S1. A hook converts epoch seconds to dates in order results. A tool returns a plain-text error. What should the hook do?
   - **a**: Parse it as JSON and fill the missing fields with defaults so that the shape is uniform for the next stage
   - **b**: Replace it with a tidy message so that every result looks the same
   - **c**: Raise an exception, so that the call is retried by the loop
   - **d**: Pass it through unchanged with an empty answer, since rewriting it would hide the failure

50. Scenario S1. A hook script blocks a refund by exiting with code 2 and also prints a JSON decision. What does the guide advise?
   - **a**: Always print the JSON as well, since the exit code alone is ignored
   - **b**: Choose a single style for that component, since mixing leaves the winner to the reference
   - **c**: Prefer the exit code and discard the JSON, because the code is read first
   - **d**: Print the JSON first and exit with 2 afterwards so that both are recorded

51. Scenario S1. An agent's loop answers two tool calls that came in one reply. In what shape must the results go back?
   - **a**: Together in one user message, with those blocks placed before any added text
   - **b**: In separate user messages, one per call, in the order the calls were made by the model itself
   - **c**: In one assistant message, since the model asked for the work
   - **d**: In one user message, with explanatory text first and the blocks after it

52. Scenario S1. A loop receives a reply whose stop reason is stop_sequence. The setup lists two custom endings for a generation. What should it do?
   - **a**: Continue with the next request, since only an end of turn means that the work is finished
   - **b**: Raise the output ceiling and ask again with the same messages
   - **c**: Treat it as finished, and check which of the configured strings fired if that matters
   - **d**: Report an unexpected value that needs an alert and a person to look at it

53. Scenario S1. A lookup finds no orders for a customer. The customer exists, but no purchase was made in the period that was searched. Which reply is right?
   - **a**: An error result flagged as not found, so that the agent tries another route through the other service
   - **b**: An empty string, so that the agent decides what it means
   - **c**: A successful response saying that nothing matched the identifier and the date range
   - **d**: A transient error with retry set, in case the index is catching up

54. Scenario S1. The wrapper has no information yet about the state of the remote system. Which of these calls may be sent again after a timeout without first checking state?
   - **a**: An email send, because the outbox accepted it before the timeout occurred
   - **b**: A refund that carries an idempotency key which the service honours
   - **c**: A refund with no key, because the amount is validated
   - **d**: A ticket creation, because duplicates can be merged later

55. Scenario S1. A refund call fails with a permission error, and the loop retries it three times. The error text says that the caller lacks the right to issue refunds. What went wrong?
   - **a**: Only a person with authority settles it, so repeating puts a human question to a machine
   - **b**: Three is too many, and a single retry after a short pause would have succeeded
   - **c**: The error should have been marked as a validation failure by the tool author
   - **d**: The loop should have retried with a different amount each time it failed

56. Scenario S1. A support agent's tools come from a billing service and a shipping service, and both define a tool named lookup. How should the names be set?
   - **a**: Rename them lookup1 and lookup2, since numbers keep the labels short and unique across the whole catalogue
   - **b**: Begin each one with the system it belongs to, so that each family is easy to tell apart
   - **c**: Keep both as lookup and rely on the descriptions to separate them in each turn
   - **d**: Merge them into one lookup that tries both of the services in turn

57. Scenario S1. An organisation wants data-handling reminders delivered to every machine and wants a particular shell command blocked. Which pairing is right?
   - **a**: A managed memory document for both, since individual settings cannot exclude it
   - **b**: Managed settings for both, since they carry text and rules alike
   - **c**: A managed memory document for the text and managed settings for the prohibition
   - **d**: A user-level memory document for both, pushed by a login script

58. Scenario S1. A support assistant's role is set in one sentence of the system prompt. What does the documentation say about such a sentence?
   - **a**: It carries no weight unless a page of rules follows it
   - **b**: It overrides each instruction that appears later in the conversation
   - **c**: A brief line is enough to make a difference to focus and tone
   - **d**: It applies to the first reply of a session and then lapses

59. Scenario S1. A support team wants a reviewer that shares none of the refund assistant's reasoning. What gives the product that building block?
   - **a**: A second turn in the same conversation with a stricter instruction
   - **b**: A larger thinking budget given to the same session
   - **c**: A separate helper that begins with a fresh window and hands back only a summary
   - **d**: A copy of the conversation passed to a new instance with the notes attached for its own reading

60. Scenario S1. A long input holds twenty order summaries and a question. Where should the question and the key facts go?
   - **a**: The question at the start, so that the model reads it before the summaries
   - **b**: The ask at the end, with a short digest of the essentials at the start
   - **c**: Both in the middle, where the attention of the model is strongest
   - **d**: The question repeated after every summary so that it is never forgotten

<details>
<summary>Answer key</summary>

1. **b**. The page names the field (module 49, page 2): "To replace the tool's output before Claude sees it, set `updatedToolOutput`, which works for any tool in both SDKs." *a* is ruled out because that field is for adding: "to append information to the tool result" *c* is ruled out because that field changes the call, not the result: "A replacement input for the tool" *d* is ruled out because that text appears only on a refusal: "for a denial it is what the model reads"
2. **b**. The page states it (module 50, page 2): "the repeated-step check ignores case and outer spaces, because a planner that has been stuck rarely repeats itself byte for byte". *a* is ruled out because the status names the cause: "The plan has stopped making progress" *c* is ruled out because the planner is the only judge of success: "The only success; the summary is the planner's" *d* is ruled out because the limit counts steps and the check counts repeats: "The planner is asked once more after the last allowed step"
3. **d**. The page orders the remedies (module 56, page 1): "A longer anchor changes only the occurrence you meant." *a* is ruled out because replace-all is for renames: "`replace_all` changes every occurrence, which is right for a rename and wrong when only one of three similar lines should change." *b* is ruled out because a second read does not change the match count: "The order of remedies is the order of risk." *c* is ruled out because rewriting is the last resort: "which is the guide's fallback, changes the file by rewriting all of it"
4. **d**. The page gives the order (module 58, page 1): "Enterprise over personal, and personal over project." *a* is ruled out because personal outranks project: "so a personal skill with the team's name silently replaces the team's version on your machine" *b* is ruled out because a clash has a winner: "`/deploy` runs the personal one" *c* is ruled out because the rule is about skills that share a name: "Of skills that share a name"
5. **a**. The page names the command (module 57, page 1): "`/context` lists the memory files that loaded into the session". *b* is ruled out because that command lists locations and opens them: "`/memory` lists the memory file locations and opens them in your editor, including ones you have not created yet." *c* is ruled out because clearing starts again and does not report: "Run `/clear` and start again" *d* is ruled out because compaction replaces text: "Compaction replaces older messages with a summary"
6. **a**. The page states it (module 59, page 1): approving "exits plan mode and switches the session to the permission mode each approve option describes". *b* is ruled out because plan mode does not change the source: "In plan mode Claude researches and proposes, and does not change your source." *c* is ruled out because approval offers ways to proceed: "approve and start editing (in auto mode, or auto-accepting edits, or approving each edit by hand)" *d* is ruled out because the session continues and is not copied: "Reading is free, and shell commands are controlled by the session"
7. **d**. The page states it (module 59, page 2): "A test written from the same misunderstanding as the code verifies nothing, which is why the first test cases come from a person or from the examples, and not from the code under test." *a* is ruled out because the loop is the strength of tests: "Claude does the work, runs the check, reads the result, and iterates until the check passes." *b* is ruled out because a check Claude can run is the aim: "Give Claude a check it can run: tests, a build, a screenshot to compare." *c* is ruled out because the developer writes or commissions them first: "The developer's part is to write, or have Claude write, the tests first and to give the target as the failing tests."
8. **b**. The page states it (module 58, page 1): "It should say what the skill does and when to use it, with the key use case first". *a* is ruled out because the text is used to decide: "Claude uses this to decide when to apply the skill" *c* is ruled out because the description is the only thing in context: "For a skill Claude may invoke, the description is the only thing in context until it loads" *d* is ruled out because the cut is a fact, not a removal of the skill: "is truncated at 1,536 characters in the skill listing"
9. **a**. The page gives the table (module 58, page 2): "Its body loads only when invoked". *b* is ruled out because the root file is for every task: "Needed in every task of the project" *c* is ruled out because a scoped rule is for a kind of file: "About one kind of file, wherever it sits" *d* is ruled out because a hook is for fixed points: "it runs at a fixed point, whether or not the model remembers"
10. **c**. The page states it (module 57, page 2): "A preference that follows you into every project, such as short answers, goes in the user file or in `~/.claude/rules/`". *a* is ruled out because a committed file is for the team: "Two kinds of line do not belong in a committed file." *b* is ruled out because a local note is for one project: "A note about one project that nobody else needs, such as a sandbox address, goes in `CLAUDE.local.md`" *d* is ruled out because a managed file reaches everyone: "Everyone on the machine, and individual settings cannot exclude it"
11. **d**. The page states it (module 71, page 2): "Give Claude a check it can run: tests, a build, a screenshot to compare." *a* is ruled out because the model stops when the work looks done: "Claude stops when the work looks done" *b* is ruled out because more room is not a new starting point: "a larger thinking budget, which gives the model more room to reason from the same starting point" *c* is ruled out because a second review in the same conversation shares the context: "a second review in the same conversation, which adds a third reader who shares the first two readers' context"
12. **d**. The page gives the order (module 61, page 2): "Examples sit between the criteria and the input, each in its own tag, with the diff last." *a* is ruled out because structure separates parts: "Structure helps the model tell instructions from examples from data" *b* is ruled out because the practice checks an order: "The practice asks for the order criteria, examples, diff last, and its tests check it" *c* is ruled out because each case has its own tag: "`<examples>` with one `<example>` per case, then `<diff>`"
13. **c**. The page lists the reactions (module 63, page 1): "An entry that expired, was canceled, or hit a server error is resubmitted unchanged in the next batch". *a* is ruled out because a full resubmission pays twice: "Resubmitting the whole batch pays twice for work that is done." *b* is ruled out because an invalid entry fails again: "sending it again fails again" *d* is ruled out because the discount is worth keeping: "The half price is a reason to move work that can wait."
14. **c**. The page lists it (module 63, page 1): "Three parameters are refused with a validation error: `stream: true` (the results come back as one file, not a stream)". *a* is ruled out because the results come back as one file: "the results come back as one file, not a stream" *b* is ruled out because a batch finishes within a window: "A batch finishes within 24 hours (usually much sooner)" *d* is ruled out because the discount applies to work that can wait: "The half price is a reason to move work that can wait."
15. **c**. The page states it (module 67, page 1): "it is not a transcript, which is the thing that filled the window in the first place". *a* is ruled out because later questions begin by reading it: "Later questions begin by reading the scratchpad" *b* is ruled out because the instructions file is another thing: "It is not a replacement for the project's instructions file, which holds what is true of the project for every session" *d* is ruled out because reading is what fills the window: "Exploration is the most context-hungry thing an agent does."
16. **a**. The page separates the two (module 51, page 1): "Resume to continue one thread; fork to branch it." *b* is ruled out because a fork starts a second line: "a fork creates a new session with its own id, so the line of work now exists twice" *c* is ruled out because continue takes the newest: "Picks up the most recent session in the current directory and adds to it" *d* is ruled out because the history is what holds the earlier analysis: "A session is the conversation history the SDK accumulates while your agent works."
17. **b**. The guide states the limit (module 51, page 1): "Forking branches the conversation history, not the filesystem." *a* is ruled out because the changes are real and shared: "If a forked agent edits files, those changes are real and visible to any session working in the same directory." *c* is ruled out because nothing refuses the second: "Two forks that each try a refactor in the same working tree write over each other." *d* is ruled out because no merge happens: "Forking the session without isolating the files leaves both attempts editing the same checkout"
18. **d**. The page states it (module 51, page 1): "Session files are local to the machine that created them." *a* is ruled out because the limit is the machine and not the account: "You can resume from another directory on the same machine" *b* is ruled out because idleness only changes the cost: a session "has been inactive for more than about an hour and is over 100,000 tokens" *c* is ruled out because the command line can resume SDK sessions: "You can still resume one by passing its session ID to `claude --resume <session-id>`."
19. **a**. The page states it (module 51, page 2): the model "is told to check whether it took effect before running it again". *b* is ruled out because nothing runs again: "doesn't finish or run again when you resume" *c* is ruled out because a session does not mark old results: "Nothing in the session marks the old tool results as old." *d* is ruled out because a session holds the conversation and not the world: "Sessions persist the conversation, not the filesystem."
20. **d**. The page states it (module 56, page 2): "a `Write(path)` rule is never matched by the file permission checks". *a* is ruled out because the edit rules do cover file tools: "rules govern all built-in tools that write files" *b* is ruled out because a read deny is the strongest rule for such a folder: "A `Read` deny is the one rule that stops reading, searching and writing." *c* is ruled out because a deny is enforced by the harness, and what is not matched is the spelling: "Bash rules are matched as written."
21. **d**. The page states it (module 77, page 1): "A scan finds what its authors thought of: a call spelled differently, built from strings or imported by name slips past it." *a* is ruled out because the gate does read generated code: "The gate in this module does read the code" *b* is ruled out because the scan does compare with the declaration: "it compares what the code does with what the proposal declares" *c* is ruled out because the scan is cheap: "and it costs nothing"
22. **b**. The page states it (module 77, page 2): "A revision comes before a gate, because a person should be asked to approve a proposal that is worth reading". *a* is ruled out because revision comes first: "A revision comes before a gate" *c* is ruled out because a format error only sends it back: "Four things only send it back: a name that is not in the fixed form" *d* is ruled out because only a clean proposal passes outright: "Only a proposal with none of these is approved outright."
23. **c**. The page states the limit (module 55, page 2): "Claude Code truncates each tool description and each server's instructions at 2,048 characters by default". *a* is ruled out because the cut is a fact: "Descriptions and server instructions are truncated at 2,048 characters by default." *b* is ruled out because the advice follows from it: "Keep them concise, and put critical details near the start." *d* is ruled out because the page names the effect: "The exam rejects the length: Claude Code truncates at 2,048 characters, so what you put last may be cut."
24. **a**. The page lists the location (module 58, page 1): "with the name prefixed by the plugin's name". *b* is ruled out because the order of precedence is for skills that share a name: "Of skills that share a name" *c* is ruled out because a plugin skill is available where it is enabled: "Where the plugin is enabled" *d* is ruled out because a name clash has a winner: "`/deploy` runs the personal one"
25. **c**. The page states it (module 57, page 1): "from broadest scope to most specific, so a project instruction appears in context after a user instruction". *a* is ruled out because memory is advice and not an override: "Memory is advice with good delivery." *b* is ruled out because all discovered files are used: "All discovered files are concatenated into context" *d* is ruled out because order is by scope and not by time: "at the start of every session"
26. **a**. The page states it (module 58, page 2): "A bare tool name like `Bash` removes the tool from Claude's context entirely". *b* is ruled out because only a bare name removes a tool: "only a bare name removes a tool" *c* is ruled out because a scoped entry has an effect: "a scoped entry such as `Edit(src/**)` is a narrower rule that leaves the tool in place" *d* is ruled out because patterns are exact: "The patterns in `allowed-tools` are as exact as the rules in module 38"
27. **a**. The page states it (module 67, page 1): "The caller can ask for a thoroughness (quick, medium or very thorough)." *b* is ruled out because it is read-only: "with Write and Edit denied" *c* is ruled out because the cost of reading is the window: "Exploration is the most context-hungry thing an agent does." *d* is ruled out because the question has to be specific: "The question must be specific"
28. **a**. The page states it (module 67, page 1): "The coordinator summarises the first phase's findings (from the scratchpad) and puts that summary into the initial prompts of the second phase's subagents." *b* is ruled out because the transcript is what filled the window: "It does not get the old transcript, which is what filled the first window." *c* is ruled out because one phase starts from the next: "The findings of one phase are the starting point of the next." *d* is ruled out because a worker does not inherit anything: "the brief must carry what the subagent needs, because it does not inherit the conversation"
29. **c**. The page states it (module 67, page 1): "The question must be specific". *a* is ruled out because the explorer is read-only by design: "with Write and Edit denied" *b* is ruled out because a helper reads in a separate window of its own: "Subagents run in separate context windows" *d* is ruled out because the brief has to carry what the helper needs: "the brief must carry what the subagent needs, because it does not inherit the conversation"
30. **a**. The page states it (module 66, page 1): "aborting on the first failure throws away every source after the failing one". *b* is ruled out because partial results are work already done: "Discarding them wastes work the subagent did" *c* is ruled out because a failed topic does not end the plan: "There is no entry that ends the run." *d* is ruled out because a transient failure is the worker's to retry: "A transient failure, a timeout or a brief outage, is the subagent's to retry"
31. **c**. The page lists the rule (module 49, page 2): "A value that is already readable is not touched". *a* is ruled out because changing everything is the opposite of the rule: "Change only what you understand." *b* is ruled out because a hook reads the input and the id that ties events together: "the tool use id that ties a pre and a post event together" *d* is ruled out because unknown fields pass: "Fields that are not in the table pass through as they were."
32. **b**. The page gives the syntax (module 49, page 1): "A value with only letters, digits, underscores, hyphens, spaces, commas and pipes is read as an exact name or a list of exact names". *a* is ruled out because only other values are expressions: "anything else is read as a regular expression" *c* is ruled out because matchers see names and nothing else: "Matchers only match tool names, not file paths or other arguments." *d* is ruled out because no matcher means every event: "Hooks without a matcher run for every event of that type"
33. **b**. The page states it (module 50, page 2): "The practice records it (path and message) and goes on with the others, and it leaves the failed file out of the cross pass." *a* is ruled out because a failure belongs to its item: "a failure in one is a fact about that file" *c* is ruled out because a loop with no bound never ends: "A loop with no bound turns a service outage into a run that never ends." *d* is ruled out because only reviewed items reach the last pass: "The cross pass sees only the files that were reviewed"
34. **d**. The page quotes the article (module 52, page 2): "Lots of redundant tool calls might suggest some rightsizing of pagination or token limit parameters is warranted". *a* is ruled out because errors, not repeats, point at descriptions: "lots of tool errors for invalid parameters might suggest tools could use clearer descriptions or better examples" *b* is ruled out because the article warns about overlap and not about a count: "Too many tools or overlapping tools can also distract agents from pursuing efficient strategies." *c* is ruled out because a response should be lean: "Design tool responses to return only high-signal information."
35. **c**. The page gives the rule (module 53, page 2): "Repeating a read changes nothing, so a timed out lookup can simply be sent again." *a* is ruled out because a read needs no confirmation: "Repeating a read changes nothing" *b* is ruled out because a timeout carries no news: "A timeout after sending is not a failure of the effect; it is the absence of news." *d* is ruled out because the number is the lookup key: "so a timed out lookup can simply be sent again"
36. **d**. The page limits the check (module 62, page 1): "Provenance does not prove a value is correct, only that it is grounded". *a* is ruled out because other checks cover correctness: "the sum check and the review of conflicts cover the rest" *b* is ruled out because the test is plain code: "it does not ask the model whether it is right" *c* is ruled out because provenance is part of the shape: "a required `provenance` object that maps each field to a quote"
37. **d**. The page gives the reason (module 62, page 1): "downstream code cannot tell it from a real value". *a* is ruled out because the parse is fine: "The JSON never fails to parse." *b* is ruled out because a clean record passes: "A pipeline that stops at schema validation reports a high success rate and passes wrong records on." *c* is ruled out because nullability is the remedy: "a field that may be missing from the source is optional or nullable"
38. **a**. The page names the shape (module 63, page 1): "A batch entry is `{custom_id, params}`, where `params` are the parameters of a Messages request." *b* is ruled out because the discount is for work that can wait: "The half price is a reason to move work that can wait." *c* is ruled out because streaming is refused: "the results come back as one file, not a stream" *d* is ruled out because standard documents belong in the queue: "Standard documents go to the batch queue"
39. **b**. The page states it (module 75, page 1): "decided by evidence for each kind, never by the average." *a* is ruled out because an average can hide a hopeless kind: "The dashboard says 99 percent and the reviewers say handwriting is hopeless" *c* is ruled out because a workload is not evidence: "decided by evidence for each kind" *d* is ruled out because confidence is not the test: "The status is derived from the checks, and the model's own opinion plays no part."
40. **b**. The page states it (module 62, page 1): "A record with `other` and no detail is a syntax error, because the pair is the contract." *a* is ruled out because semantic errors are about meaning: "a vendor in the currency field" *c* is ruled out because other comes with its detail: "`other` plus a detail field for a value the enum did not foresee" *d* is ruled out because absence is another case: "A required value that the model reports as absent is not retried: the document goes to review"
41. **c**. The page states it (module 63, page 1): "it is the only link between a request and its result". *a* is ruled out because results come in any order: "can be returned in any order" *b* is ruled out because the label is a short identifier: "The `custom_id` is 1 to 64 characters of letters, digits, hyphens and underscores" *d* is ruled out because each entry has its own result: "A finished batch has a result per entry: succeeded, errored, canceled or expired."
42. **a**. The page states it (module 63, page 2): "A pass that reports it twice counts once, since repetition inside a pass is not independent evidence." *b* is ruled out because identity is by file, line and issue: "The same finding, identified by file, line and issue, reported by two passes is one finding." *c* is ruled out because independence is about the reviewer: "A reviewer is independent if it could not have copied the author's rationale." *d* is ruled out because passes produce lists that are combined by rules: "Several independent passes produce several lists."
43. **d**. The page states it (module 68, page 1): "If no threshold reaches the target, there is no honest cut-off: every item needs review until the pipeline improves." *a* is ruled out because the rule picks the lowest qualifying threshold: "The lowest qualifying threshold automates the most work while meeting the target." *b* is ruled out because a threshold has to reach the target: "Pick the lowest threshold whose precision reaches the target." *c* is ruled out because a restated number is still a score: "The number is only a score"
44. **d**. The page states it (module 69, page 1): "A subagent that cannot name a source for a statement does not return it as a finding: it reports it separately as unsourced, or leaves it out". *a* is ruled out because a required field pushes toward invention: "a required field pushes a model to invent a value when there is none" *b* is ruled out because a source travels with its claim: "A source survives as long as it travels with its claim in a field." *c* is ruled out because a finding without a source is refused on entry: "a finding without one is refused where it enters the pipeline"
45. **a**. The page states it (module 67, page 2): "It is written as the agent learns and not only at the end, so that a crash half-way leaves something." *b* is ruled out because a crash gives no warning: "A crash does not warn." *c* is ruled out because the file holds findings in a fixed form: "holding its findings in the scratchpad form of page 1: specific, once, grouped" *d* is ruled out because the path is fixed by the design: "Each agent exports its state to a known location"
46. **d**. The page puts the check where it protects the last call (module 45, page 1): "a reply that ends the turn on the very last allowed call is still done". *a* is ruled out because a reply that ended its turn is finished, and the count is only a backstop: "Keep the count as a backstop, above the real need, with a status that is not `done`." *b* is ruled out because a reply that ended its turn is not cut off, since the page reads that stop reason as "Claude finished its response naturally." *c* is ruled out because a known value is handled by name: "in your own code, give every other value a status of its own."
47. **b**. The page lists the value (module 45, page 1): "Leave with a status of its own: the reply is cut off, and raising the limit is a decision for your code". *a* is ruled out because a cut-off reply is not a finished one: "Leave with a status of its own: the reply is cut off, and raising the limit is a decision for your code" *c* is ruled out because the loop continues on one value only: "Run the calls and send the results; this is the only value that continues" *d* is ruled out because the loop does not guess at an unknown reason: "A value the loop has never seen must not be guessed at."
48. **b**. The page prefers a reason that teaches (module 49, page 1): a reason "tells the model what to do next, which is the same design as the refusal sentence of module 48". *a* is ruled out because a bare refusal says nothing to do: "A reason is part of the interface." *c* is ruled out because a flat denial ends the attempt: "Writing to /etc is not allowed" *d* is ruled out because the reason "tells the model why, so it avoids retrying."
49. **d**. The page limits the normaliser (module 49, page 2): "since rewriting an error hides the failure". *a* is ruled out because it changes only what it understands: "Fields that are not in the table pass through as they were." *b* is ruled out because the page rejects it by name: "Normalise by rewriting every result, errors included." *c* is ruled out because a crash is not a repair: "A guard that crashes on unexpected input is therefore a guard that lets the call through."
50. **b**. The page quotes the guide (module 49, page 2): "Use exit 2 to block with a stderr message, or exit 0 with JSON for structured control. Choose one approach per hook." *a* is ruled out because the winner is left open: "Mixing them leaves it to the reference to say which wins, and a guard should not depend on that." *c* is ruled out because the reason of a block goes to stderr: "Where the reason lands depends on the event" *d* is ruled out because with exit 0 the JSON decides: "No objection from the exit code; with JSON printed on standard output, the JSON decides"
51. **a**. The page gives the shape (module 45, page 2): "the tool_result blocks must come FIRST in the content array." *b* is ruled out because one message answers the whole turn: "A turn with three calls gets one user message with three results" *c* is ruled out because the answer is a user message: "Tool result blocks must immediately follow their corresponding tool use blocks in the message history" *d* is ruled out because blocks come first: "the tool_result blocks must come FIRST in the content array."
52. **c**. The page gives the action (module 45, page 1): "Leave: done, and read which sequence fired". *a* is ruled out because only one value continues: "Run the calls and send the results; this is the only value that continues" *b* is ruled out because a cut-off is another value: "Leave with a status of its own: the reply is cut off, and raising the limit is a decision for your code" *d* is ruled out because this value is known: "A value the loop has never seen must not be guessed at."
53. **c**. The page treats it as success (module 53, page 1): "A lookup that finds no orders for a customer has worked." *a* is ruled out because a failure and an empty answer are different: "from a valid empty result, which is a successful query that found no matches" *b* is ruled out because the two replies mean opposite things: "The two replies look alike in a log and mean opposite things" *d* is ruled out because a transient failure is a timeout or a limit: "A timeout, an unavailable service, a rate limit: the same call may work soon"
54. **b**. The page lists the safe cases (module 53, page 2): "A call that carries an idempotency key that the service honours." *a* is ruled out because a sent email cannot be recalled: "an email that left the outbox cannot be called back" *c* is ruled out because validation does not prevent a second payment: "a refund sent twice refunds twice" *d* is ruled out because a duplicate is real: "a ticket created twice is two tickets"
55. **a**. The page states it (module 53, page 1): "a loop that retries a permission failure three times has asked a person's question of a machine three times". *b* is ruled out because a transient fault is the kind that clears: "A timeout, an unavailable service, a rate limit: the same call may work soon" *c* is ruled out because the two failures are repaired differently: "but the agent repairs one and escalates the other" *d* is ruled out because a validation failure is about the input: "The input was wrong: a missing field, a value out of range, a bad format"
56. **b**. The page quotes the documentation (module 52, page 1): "When your tools span multiple services or resources, prefix names with the service (for example, `github_list_prs`, `slack_send_message`)." *a* is ruled out because a name is the first thing the model reads: "A name is the first thing the model reads." *c* is ruled out because near-identical texts leave nothing to choose by: "Two tools whose texts say nearly the same thing leave the model nothing to choose by" *d* is ruled out because a merge is a larger change: "it is a larger change than a first step needs"
57. **c**. The page separates them (module 57, page 1): "blocking a tool, a command or a path is done with managed settings". *a* is ruled out because memory is context and not enforcement: "Claude treats them as context, not enforced configuration" *b* is ruled out because a managed memory file is for behaviour: "A managed CLAUDE.md is for behaviour" *d* is ruled out because a user-level file is personal: "You, in every project"
58. **c**. The page quotes the documentation (module 76, page 1): "Setting a role in the system prompt focuses Claude's behavior and tone for your use case. Even a single sentence makes a difference." *a* is ruled out because a persona needs little: "A persona needs a name, a manner and its limits, not a page of rules" *b* is ruled out because rules that must hold belong elsewhere: "the rules that must hold belong in the second row" *d* is ruled out because a role focuses behaviour for the use case: "focuses Claude's behavior and tone for your use case"
59. **c**. The page states it (module 63, page 1): "In Claude Code a subagent starts with a fresh context window that does not include the conversation history, the skills already invoked or the files the main agent has read, and returns only a summary." *a* is ruled out because a stricter instruction leaves the reasoning in place: "a stricter instruction in the same conversation, since the reasoning is still there" *b* is ruled out because more room is the same starting point: "a larger thinking budget, which gives the model more room to reason from the same starting point" *d* is ruled out because the reviewer gets the code and not the reasoning: "the code, and not the reasoning"
60. **b**. The page states it (module 64, page 1): "Put the long material first and the question last." *a* is ruled out because the middle is where findings go missing: "a long input's middle is where findings go missing" *c* is ruled out because detail goes under headers: "Organise the detail under explicit headers" *d* is ruled out because quoting before answering is another tool: "Asking the model to quote the relevant passages before answering is a fourth tool for long documents."

</details>
