# Developer mock exam 2

**Level:** Developer · **Module 44:** Exam readiness 2 · **Page 4 of 4**
**Exams:** DV1 to DV8 (CCDV-F; the questions follow the Developer blueprint over the content of modules 1 to 43)

**After this page you can** tell whether you are ready for the Developer exam and which domains need more work.

This second mock exam covers **the content of Levels 1 and 2** (modules 1 to 43), as the Developer exam does, and not one page or one module. It has the
same shape as the first one and the same split of questions by domain, but it tests other facts: where a domain has pages that the first mock did not use,
this one draws on them, and where it returns to a page, it asks about a different fact in a different scenario. Every question is the course's own, written
fresh; no question comes from a live exam, and none repeats a question of a page quiz, a module quiz or the first mock exam. The facts behind each answer
come from the official pages named on the module pages, so a reader who studies the pages can answer each question from the page it is drawn from. The real
exam has multiple-choice and multiple-response items, and so does this mock: a multiple-response item ends with (Select two.) and is right only when both keyed options are chosen.

## How to take it

1. **Choose your conditions.** For a readiness test, take all 53 questions in **120 minutes** with no notes, which is the Developer exam's pace of
   about 2.3 minutes an item. For learning, take it untimed and read each explanation. Take it after the first mock, or on another day, so that the two
   readings of your weak domains are independent.
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
| 1 to 17 | DV1 Applications and integration | 33.1% | 17 | 32.1% | 12, 13, 14, 15, 16, 17, 21, 22, 23, 30, 39, 40 |
| 18 to 26 | DV2 Model selection and optimisation | 16.8% | 9 | 17.0% | 1, 4, 6, 18, 19, 20, 21 |
| 27 to 34 | DV3 Agents and workflows | 14.7% | 8 | 15.1% | 34, 35, 36, 37 |
| 35 to 40 | DV4 Prompt and context engineering | 11.0% | 6 | 11.3% | 24, 25, 28, 29 |
| 41 to 46 | DV5 Tools and MCP | 10.6% | 6 | 11.3% | 26, 27, 32, 33 |
| 47 to 50 | DV6 Security and safety | 8.1% | 4 | 7.5% | 10, 31, 41 |
| 51 to 52 | DV7 Claude Code | 3.1% | 2 | 3.8% | 38 |
| 53 | DV8 Evaluation, testing and debugging | 2.6% | 1 | 1.9% | 43 |

For a miss, open the page that the question's explanation names, as (module, page), and reread the passage it quotes. A cluster of misses in one
domain points at that domain's modules in the last column. The questions on the API, cloud platforms and streaming (DV1) and on model choice, thinking
and caching (DV2) carry most of the weight, so they repay the first revision session.

## Mock exam

This mock exam covers the content of Levels 1 and 2, modules 1 to 43. Choose one answer for each question, or the number the question states.

1. A support console's spec says that no summary may repeat a card number found in a ticket thread, and the team must decide where that rule is enforced. Which approach fits best?
   - **a**: A firm instruction in the prompt, written in capitals and repeated twice at the top
   - **b**: A target that at least 90 percent of summaries are rated accurate by two leads
   - **c**: A regular expression in the application code that screens each output before display
   - **d**: A longer explanation of the rule, placed in the user turn of every request

2. A team wants an alert that fires before its first 429 arrives, not after. Which signal fits best?
   - **a**: The remaining allowance that the headers report for requests and tokens
   - **b**: The count of 5xx errors per hour, which reflects the provider's capacity
   - **c**: The count of 400 replies per hour, which reflects the load on the service
   - **d**: The latency percentile at the edge of the service, compared with the budget

3. A Python service uses the official SDK and now needs the rate-limit headers of each reply, which the typed return value does not expose. Which route fits best?
   - **a**: Replace the SDK with a hand-written client that sets three headers on every request
   - **b**: Turn on debug logging and read the headers out of the log output
   - **c**: Read the usage fields of each reply, which hold the remaining allowance
   - **d**: Make the request through the raw-response accessor and parse the result afterwards

4. A hand-written reader for streamed replies waits for a final data line reading [DONE] and hangs on every call. Which two statements explain it? (Select two.)
   - **a**: A longer read timeout lets the missing line finally arrive
   - **b**: The format uses only named events
   - **c**: A websocket is needed to signal the end of a reply
   - **d**: A keep-alive ping is what normally marks the end of a reply
   - **e**: The old closing marker no longer exists

5. After a tool loop, Claude Sonnet 5.5 sometimes ends its turn with no content at all. The client packs each tool's findings into the message together with a sentence announcing that data is attached. Which change fits best?
   - **a**: Raise max_tokens so that the model has room to produce a full answer
   - **b**: Retry the identical request until a non-empty reply finally arrives
   - **c**: Send the result blocks alone, with no text of any kind placed beside them
   - **d**: Switch the request to streaming so the empty turn is delivered in pieces

6. A chat form lets a person submit an empty message, and each one still triggers a paid call whose reply wanders. Which handling fits best?
   - **a**: Refuse the blank turn in the client, before anything is sent over the network
   - **b**: Send it anyway and discard the reply when it comes back from the service
   - **c**: Insert a default question in place of the blank so that the call is useful
   - **d**: Add a line to the system prompt telling Claude to ignore empty messages

7. A service decides whether to retry by searching the error message for the word overloaded. After the provider rewords the message, retries stop. Which change fits best?
   - **a**: Add the provider's new wording to the search and keep matching on the message text
   - **b**: Retry every error for ten minutes whatever its status
   - **c**: Catch only the SDK's base class, so that every failure takes one path
   - **d**: Branch on the status and type through the SDK's typed classes, most specific first

8. A team streams five-minute reports and sets a read timeout of 30 seconds, worried that long generations will always trip it. What should the review say?
   - **a**: It will trip, because the limit covers the whole generation from its very first byte
   - **b**: The clock restarts at every event, so steady output survives and only a stall fails
   - **c**: It will fail unless the timeout is raised to the ten-minute default
   - **d**: The timeout applies only before the first byte, so it never matters afterwards

9. A service sends the same 30,000-token instruction block with every request and reaches its input-token ceiling long before its request ceiling. Which two statements describe the cause and the lever? (Select two.)
   - **a**: Reads from a cache are left out of that count
   - **b**: Routing through a second inference geography adds a separate rate pool
   - **c**: A second workspace raises the organisation's overall allowance
   - **d**: Marking the shared prefix for reuse raises throughput
   - **e**: Raising max_tokens gives each reply more output per call

10. An assembler for streamed replies joins `thinking_delta` fragments into the thinking text but discards the `signature_delta` events as noise. Which two statements describe what it should do? (Select two.)
   - **a**: That attribute must travel back unmodified
   - **b**: Regenerate the value locally from a hash of the thinking text
   - **c**: Ignore them, since only the text deltas matter for the final message
   - **d**: Their payload becomes an attribute of the block
   - **e**: Append their text to the end of the thinking text

11. A background service sets a very large max_tokens, and the SDK refuses its non-streaming request before sending it. Nobody watches the output, the answer is needed within minutes, and the team wants the least extra code. Which option fits best?
   - **a**: Write an event handler of your own that assembles each content block as it arrives
   - **b**: Lower max_tokens until the request passes the size check and goes out
   - **c**: Use the final-message helper, which relies on events internally and returns one message
   - **d**: Move the request into a batch, whose results arrive together as one file

12. A team's list holds 1,000 request ids and the result file has 998 lines, yet its program reports every request as handled. What should it do?
   - **a**: Treat the missing two as succeeded, since nothing in the file contradicts them
   - **b**: Diff the sent identifiers against the returned ones and resend any that are missing
   - **c**: Resubmit the whole batch, since a short result file means that processing has failed
   - **d**: Match the two by position, since the file keeps the order of submission

13. A team on Google Cloud wants to reference a PDF by its web address in each request instead of embedding it. What do the platform pages say?
   - **a**: Web addresses work there, and a PDF counts as a document block like any other
   - **b**: Web addresses do not work there for documents, though they do for images
   - **c**: A single upload through the Files API lets later requests reference the PDF by id
   - **d**: That source type is not offered there, so another way to supply the file is needed

14. A team calls Claude Sonnet 5.5 through Amazon Bedrock with valid credentials and a correct policy, yet the call is refused. Which prerequisite does the page name?
   - **a**: The model must first be enabled for the account, since access is gated per model
   - **b**: A quota request to Anthropic, because new accounts start with zero tokens per minute
   - **c**: Request logging must be on, because Bedrock refuses calls that are not logged
   - **d**: A Model Garden model card must be opened first, as on Google Cloud

15. A prompt asks Claude to return each box as percentages of the picture, and the boxes land in the wrong places. Which change fits best?
   - **a**: Pad every picture to a multiple of 28 pixels before sending it
   - **b**: Divide each returned point by the padded size to obtain the fractions
   - **c**: Request pixel coordinates, then convert them to ratios in the program
   - **d**: Upload the image through the Files API so that its coordinates are kept

16. A team member keeps a skill called deploy in the user-level skills location, and the repository commits a skill with the same name. Which one runs in a session there?
   - **a**: The repository's copy, because committed files take precedence
   - **b**: The developer's own copy, since that scope outranks the project's
   - **c**: Neither one, because Claude asks which to use when the names collide
   - **d**: Both in turn, with the project steps appended after the personal ones

17. Two Claude sessions edit two different features in the same checkout, and each keeps overwriting the other's files. Which setup fits best?
   - **a**: Let the sessions take turns, committing to main after each change
   - **b**: Ask each session to avoid files that the other is likely to touch
   - **c**: Switch both sessions to plan mode so that neither writes until approved
   - **d**: Give every run a separate worktree, each one placed on a branch of its own

18. A reply begins with a confident wrong figure, and the rest of the answer then builds on it without any correction. What explains this?
   - **a**: The model consults a stored draft of the whole answer and then defends that draft
   - **b**: Each later piece is chosen with the earlier text in view, so the slip becomes context
   - **c**: The window dropped the opening lines, so the model lost track of what it had said
   - **d**: A hidden checker reviews each sentence and approves it before the sentence is shown

19. A team sends an entire 800,000-token archive with every request because the window allows it, and answers worsen while bills climb. Which two statements describe the problem? (Select two.)
   - **a**: Recall degrades as the space fills
   - **b**: A larger window restores accuracy, since more room means better recall
   - **c**: Lowering the effort speeds up processing of the extra tokens
   - **d**: Repeating the key instruction inside the archive fixes the drift
   - **e**: Trimming to what the task needs helps

20. A system prompt contains an API key and tells Claude it is a guard who must never reveal it. A tester extracts the key with a clever request. Which redesign fits best?
   - **a**: Remove the credential from the context and protect it in program logic
   - **b**: Strengthen the role line, saying the guard is the best in the world
   - **c**: Add a second line that repeats the instruction in capital letters
   - **d**: Split the key into two halves placed in different turns of the conversation

21. A classification prompt carries five examples. Some show the label alone and others wrap it in a sentence, and live replies now vary in the same way. Which change fits best?
   - **a**: Add more sentence-style samples so that this style outweighs the other one
   - **b**: Drop every sample and rely on the written instruction to fix the shape
   - **c**: Rewrite every sample answer so that all of them share one identical layout
   - **d**: Wrap every sample in extra tags so that the model separates them better

22. A support prompt is one long paragraph, and reviewers cannot tell which piece of it causes made-up verdicts when the policy is silent. Which change fits best?
   - **a**: Give the assistant a flattering role so that it feels bound to be careful
   - **b**: Separate the parts, then add a fallback value and a test for it
   - **c**: Ask for a longer reply in which the model explains its reasoning at length
   - **d**: Move the documents below the task so that the policy is read last

23. A team plans to use the token-counting tool before each message request and worries that those calls will use up the allowance for its message calls. What should the review say?
   - **a**: Counting needs a paid tier of its own before it can be used at all in a live service
   - **b**: Counting is unlimited, so it can be called as often as needed without any pacing
   - **c**: They are limited separately and independently, though the pre-check has its own cap
   - **d**: Both draw on one allowance, so each count spends a message request

24. A feature returns a short label, and the team wants it to appear sooner. It proposes fast mode on Claude Opus 5.5. Which two statements should the review make? (Select two.)
   - **a**: Fast mode also lowers latency to the first token for any interactive feature
   - **b**: Fast mode applies once the work moves to the Message Batches API
   - **c**: Time to the first word barely changes
   - **d**: Fast mode is free on Opus 5.5 and needs only a beta header
   - **e**: Its benefit is tokens per second once streaming begins

25. A team avoids adding a second `cache_control` entry to its prompt, believing each extra entry is billed on its own. What should the review say?
   - **a**: Each marker is billed at the input price, on top of the tokens it covers
   - **b**: A marker is billed once per request, whether or not the prefix is reused
   - **c**: A marker costs nothing by itself; billing covers tokens written, read and left uncached
   - **d**: Markers are free, but only the first one placed in a request can ever be read

26. A nightly job sends 8 million input tokens and 2 million output tokens to Claude Sonnet 5.5, whose standard prices are $2 per million input and $10 per million output. What does the job cost as a batch?
   - **a**: $26
   - **b**: $18
   - **c**: $36
   - **d**: $28

27. A router asks a cheap model to classify each question, then maps the reply to a prompt. One reply comes back as 'Refunds?', which matches no route, and the request crashes. Which fix fits best?
   - **a**: Normalise the text, look it up, and send anything unknown to a default branch
   - **b**: Retry the classification up to five times until the reply matches a known route
   - **c**: Let the model add a new route whenever its reply names an unknown label
   - **d**: Run every question through all routes in parallel and keep the first reply

28. An orchestrator splits a task into three subtasks, and all three workers throw errors. The last step is asked to merge the results and writes a confident answer. What should the program do instead?
   - **a**: Run the last step anyway, but tell it to mention that some work failed
   - **b**: Retry each worker in a loop until at least one of them returns a result
   - **c**: Skip that stage and flag the whole job as failed, with nothing produced for the user
   - **d**: Return the plan text as the answer, since it already describes the work

29. A service stops reading an agent run's messages the moment the closing summary message arrives, and a few late items never reach its logs. Which change fits best?
   - **a**: Raise max_turns so that the run produces its late items before the closing message
   - **b**: Switch to partial-message events so that nothing arrives after the result
   - **c**: Resume the session by its identifier to fetch the missing items afterwards
   - **d**: Iterate to the end of the stream, since trailing events still follow the result

30. A team wires its audit rule into the permission callback of its agent, yet calls to its read tools never appear in the audit log. Which two statements describe the cause and the fix? (Select two.)
   - **a**: Registering a second callback makes it run before the first
   - **b**: Anything allowed in advance skips it
   - **c**: An earlier hook is the place that sees everything
   - **d**: Switching the mode to bypass sends every call to the callback
   - **e**: The callback runs only for denied calls, so a deny rule must fire first

31. An agent's safety relies on beginning with a particular permission setting, and a refactor drops the line that sets it. The unit checks of the policy function still pass. Which kind of run would catch the change?
   - **a**: Start the stand-in binary and read the mode back from the flags it received
   - **b**: Add more unit checks of the policy function, covering unusual tool names
   - **c**: Call the live model once per release and watch which mode each run shows
   - **d**: Rely on the SDK default, since it always matches the intended level

32. A hosted agent's shell command prints about 400,000 characters. What does the model receive for that output?
   - **a**: The full text, sent in several consecutive events until it is complete
   - **b**: Nothing, since the output is not kept once the tool call has ended
   - **c**: A truncated preview, with the path of a file in the sandbox that holds the rest
   - **d**: An error result, because any output above the limit is rejected outright by the harness

33. A security review expects a new sandbox for a hosted agent to be cut off from the internet, because the create request never mentions the network field. What will happen?
   - **a**: The platform applies limited networking with an empty host list
   - **b**: The create request is rejected with a 400 until networking is set
   - **c**: Each session inherits the networking of the previous session's environment
   - **d**: Omission means unrestricted access, apart from a general safety blocklist

34. A typed agent's reply does not fit its declared output type. What does this style do next?
   - **a**: It accepts the reply and flags the record for a later human review by the team
   - **b**: It sends the validation error back to the model as a retry and the run continues
   - **c**: It ends the run at once and raises an exception to the caller
   - **d**: It repairs the record by guessing the missing fields from the earlier replies it saw

35. A template builder leaves a blank where the document variable was not supplied, and the model answers confidently about nothing. Which behaviour fits best?
   - **a**: Fill the blank with a default document so that the model always has material
   - **b**: Send the request anyway and ask the model to say when material is missing
   - **c**: Retry the call and let the model recall the missing document from memory
   - **d**: Stop with a clear error before any request is made, saying what is absent

36. A three-step pipeline over an 80,000-token contract resends the whole contract at every step, and costs run high. Step two only needs the clauses that step one found, and the team also wants to look at what each stage produces. Which two statements describe the change? (Select two.)
   - **a**: Delete every clause that looks unrelated to the question first
   - **b**: Move the contract into the system prompt of each step
   - **c**: Hand on only the extracted excerpts
   - **d**: Intermediate outputs can be read separately
   - **e**: Merge the three steps into one prompt so the contract goes once

37. A service inserts a fresh random property into the shape of its JSON output format on every request, and every call shows added latency on first use. Which change fits best?
   - **a**: Keep the schema fixed, because compiled grammars are cached for 24 hours
   - **b**: Rename the schema's fields on every request so the cache sees a fresh entry
   - **c**: Switch to strict tool use, which skips grammar compilation entirely
   - **d**: Send each schema once an hour from a scheduled job so the cache stays warm

38. A hard document keeps failing validation, and the extraction loop keeps re-prompting for hours, with the bill growing each time. Which change fits best?
   - **a**: Raise the temperature setting so that a later attempt differs from earlier ones
   - **b**: Re-prompt with only the original prompt, since the error list distracts the model
   - **c**: Cap the attempts, then report the outcome with its last problems for a person
   - **d**: Remove the validation step for that document so that the first reply is accepted

39. A team wants to prepend a short context sentence to each of its 50,000 chunks before indexing, but fears the model calls would be too costly. Which fact answers the worry?
   - **a**: The sentences are generated once per chunk for free, since indexing is not billed
   - **b**: Contextual retrieval needs no extra calls, since the embeddings already hold the context
   - **c**: Only the first thousand chunks need a sentence, since later ones inherit it
   - **d**: Caching the document makes the work affordable, at about a dollar per million tokens

40. A team asks Claude in the prompt to quote its sources for each claim, and the replies get expensive. Which feature addresses this?
   - **a**: The Files API, which stores the quoted material outside the request
   - **b**: Citations, whose cited text is not counted toward output tokens
   - **c**: Structured outputs, which compress each quote into a fixed shape
   - **d**: A prompt rule asking for shorter quotes of ten words at most

41. An assistant keeps calling a tool that keeps failing, and the loop, which only watches the stop reason, runs until the budget is gone. Which two changes fit? (Select two.)
   - **a**: Make the handler return an empty string so the tool seems to succeed
   - **b**: Wait for the model to decide to stop by itself
   - **c**: Cap the number of model calls
   - **d**: Report a separate outcome when the cap is reached
   - **e**: Rely on the runner's default behaviour, which has no limit

42. A migration touches hundreds of files, and the team wants its findings cross-checked before anyone sees them, which is beyond what a handful of helpers can manage. Which extension fits best?
   - **a**: A subagent, because Claude decides turn by turn what runs next
   - **b**: A skill, because it adds reference material to every conversation
   - **c**: A dynamic workflow, where a script rather than the model decides what runs next
   - **d**: A line in CLAUDE.md that tells Claude to double-check its own findings

43. A new server design wants to borrow the host application's language model for a completion, so that the server needs no key of its own. What does the 2026-07-28 revision advise?
   - **a**: Use sampling, which is the preferred way to reuse the host's model
   - **b**: Integrate with a provider's API directly, since sampling is deprecated
   - **c**: Use roots, which let the server borrow the model through the client's paths
   - **d**: Use elicitation, which asks the host's model for a completion on the user's behalf

44. A server test checks a refused call by demanding that the client's reply equal the handler's own sentence, and it fails because the client adds extra words at the start. Which change fits best?
   - **a**: Wait for an exception from the client, since errors are raised and not returned to it
   - **b**: Call the handler directly in the test, which returns the exact text it produced
   - **c**: Change the handler so that it adds the same extra words to its own sentence
   - **d**: Assert that the error contains the expected phrase rather than matching it whole

45. A remote server advertises every permission it has in its metadata, and clients request all of them, so a stolen token would reach widely. Which design change fits best?
   - **a**: Start with a small set of low-risk read scopes and escalate by targeted challenge
   - **b**: Issue longer-lived tokens so that clients are asked for consent less often by the server
   - **c**: Keep every scope but require the token in the URL query string to track use
   - **d**: Drop authorization on HTTP and trust the Origin header alone

46. A client of a Streamable HTTP server wants to cancel a long tool call it started. Which signal does the specification expect?
   - **a**: A cancel message naming the request, sent on a second connection
   - **b**: A progress token that decreases to zero to signal that work should stop
   - **c**: Closing the event channel that carries that reply
   - **d**: A GET request to a cancel endpoint of the server

47. A compliance lead states that every Claude interaction in the company is covered by the Enterprise audit logs. One team uses the Microsoft 365 add-ins. What should the review say?
   - **a**: The statement holds, since the Enterprise plan logs all activity across every product
   - **b**: The add-ins inherit the custom retention settings, so their activity is logged for the same time
   - **c**: Streaming the add-in events to the monitoring tools would close the gap
   - **d**: Verify each product separately, since these extensions fall outside that record

48. A production agent calls a partner service with an access secret that the team set as an environment variable inside the agent's sandbox. Which change fits best?
   - **a**: Rotate the secret weekly so that a leaked copy expires quickly afterwards
   - **b**: Keep an opaque stand-in there and let the platform swap in the real value at send time
   - **c**: Encrypt the variable inside the image so that just the agent process can read it
   - **d**: Tell the agent in its instructions to keep the secret out of its output

49. A screen-driving loop acts on just the first step of each assistant turn and sends back a single outcome. After a turn that planned click, type and click, the next call fails with a 400. Which two statements fit? (Select two.)
   - **a**: A smaller screenshot makes the request fit the limit
   - **b**: A pause between turns gives the earlier request time to finish
   - **c**: A longer client timeout lets the same turn go through
   - **d**: Every block of that message needs an answer
   - **e**: Unanswered items count as an invalid request

50. A support bot's users sometimes craft inputs to bypass its rules, and the team has no record of who tried what. Which defence is missing?
   - **a**: A log of every attempt, so that repeat offenders get throttled or banned
   - **b**: A pattern list of known attack phrases, which makes logging unnecessary
   - **c**: Placing each user's text in the system prompt so the model gives it priority
   - **d**: A larger context window, so that the whole history of the attacker's inputs fits

51. Claude ran a script that altered rows in a production database. The developer presses Esc twice to rewind, expecting the rows to come back. What happens?
   - **a**: Everything is undone, because the checkpoint also snapshots the database
   - **b**: The database is restored only if Claude is asked to undo the script itself
   - **c**: Only file edits are undone; changes made to outside systems stay in place
   - **d**: The rewind is refused, because a session cannot rewind after running a command

52. A team adds a deny entry `Write(./config/prod.yaml)` to its settings, yet Claude still changes that configuration. Which fix fits best?
   - **a**: Move the entry from the shared file to the user file, which ranks lower
   - **b**: Add a line to the memory file asking Claude not to change that configuration
   - **c**: Replace the deny entry with an ask entry for Write, so that Claude must ask first
   - **d**: Use an Edit or Read rule instead, since a Write path rule is never consulted

53. A response arrives whose stop reason is `model_context_window_exceeded`. How should the client handle it?
   - **a**: Treat it as truncated, then trim or compact the history before the next call
   - **b**: Retry the identical request on the same model, since the failure is on the service side
   - **c**: Raise max_tokens so that the reply has room to finish
   - **d**: Switch to a fallback model, since the model declined to answer

<details>
<summary>Answer key</summary>

1. **c**. A guarantee that must always hold "belongs in code, not in a prompt", so the screening runs on every output (module 12, page 1). *a* is ruled out because a rule such as never output a card number "is only a request to the model". *b* is ruled out because a quality target is "stated as a rate over a set of examples", which tolerates misses, while this rule must hold for every summary. *d* is ruled out because wording in any turn only raises the odds, since "it cannot make it certain".
2. **a**. The headers report the remaining allowance, "so you can alert before the first 429" (module 12, page 3). *b* is ruled out because "a rise in 5xx is a provider story", not a sign of your own headroom. *c* is ruled out because "a rise in 400s is a bug in your requests", and says nothing about the allowance left. *d* is ruled out because latency is a separate signal, while "Headroom against the limits" is the one that warns before a 429.
3. **d**. The SDK offers a raw-response accessor in Python that gives the headers first, then the parsed object, because "each SDK has an escape hatch" (module 13, page 2). *a* is ruled out because the SDK is not wrong here, since "Each SDK has an escape hatch" for exactly this need. *b* is ruled out because "Debug logging is a development tool", and bodies stay visible in it. *c* is ruled out because the usage fields report "what the call consumed", not the allowance that is left.
4. **b and e**. The page records that "All events are named events" in the version the API uses, and "the old data: [DONE] marker is gone", so the end of a reply is a named event (module 13, page 3). *a* is ruled out because "the old data: [DONE] marker is gone", so no amount of waiting will bring it. *c* is ruled out because "The Messages API does not need it". *d* is ruled out because a ping is "a keep-alive with no content", and it does not mark the end.
5. **c**. Text added next to the results "teaches Claude to expect user input after every tool use", so the results go alone (module 14, page 2). *a* is ruled out because an empty reply with end_turn "is almost always self-inflicted", and a larger ceiling does not touch the cause. *b* is ruled out because "Don't retry empty responses without modification". *d* is ruled out because streaming only "changes when the bytes arrive", not what the model produces.
6. **a**. The page says an empty user turn is a wasted call, so "refuse it before anything is sent" (module 14, page 3). *b* is ruled out because "An empty user turn is a wasted call at best", and discarding the reply does not save the cost. *c* is ruled out because that makes "a different conversation from the one the user sees". *d* is ruled out because "A sentence in a prompt can make a behaviour much more likely", but the call is still paid for.
7. **d**. The documentation says to "catch the SDK's typed classes rather than string-matching error messages" (module 15, page 1). *a* is ruled out because "a message can be reworded without notice", so the same break will return. *b* is ruled out because a 400, 401, 404 or 413 "returns the same answer each time". *c* is ruled out because the base class is kept "as a last resort", after the most specific classes.
8. **b**. "a long generation that is steadily producing text does not hit a short read timeout" (module 15, page 3). *a* is ruled out because for a streamed reply "the clock restarts with every event", so the whole generation is not one interval. *c* is ruled out because "Streaming changes the arithmetic in your favour", so a short limit can stay. *d* is ruled out because the read clock measures "how long to wait between bytes of the reply".
9. **a and d**. "Only uncached input counts toward ITPM" for most models, so cache reads free capacity (module 16, page 2). *b* is ruled out because "Rate limits are shared across all inference_geo values". *c* is ruled out because "organisation limits always apply" whatever the workspace. *e* is ruled out because the setting "does not factor into OTPM rate limit calculations", and the pressure is on input.
10. **a and d**. A signature "must be sent back unchanged with the thinking block" (module 17, page 1), and a signature_delta sets it. *b* is ruled out because "thinking or redacted_thinking blocks in the latest assistant message cannot be modified". *c* is ruled out because the rule to "ignore event types you do not know" covers unknown types, and this one is documented. *e* is ruled out because "a signature_delta sets the signature", and it does not extend the thinking.
11. **c**. The helpers "stream underneath and return one message", so a long call needs no event handling (module 17, page 2). *a* is ruled out because a long call "does not need the event handling in your own code". *b* is ruled out because the refusal is a hint, since "if you see it, you wanted streaming". *d* is ruled out because a batch does not fit "work needed within the hour".
12. **b**. The page says to "compare the ids you sent with the ids you received" (module 21, page 2). *a* is ruled out because "a request without a result line is a gap", not a success. *c* is ruled out because "the failure of one request in a batch does not affect the processing of other requests". *d* is ruled out because "Batch results can be returned in any order".
13. **d**. The Google Cloud list of gaps includes "Input sources (URL sources for images and documents, Files API)" (module 22, page 2). *a* is ruled out because the gap list names "URL sources for images and documents", so documents are excluded. *b* is ruled out because the missing item covers "URL sources for images and documents" together. *c* is ruled out because the table shows "Files API | yes | no | no", so it is missing there too.
14. **a**. "Amazon Bedrock sets access criteria for each Claude model individually" (module 23, page 2). *b* is ruled out because "Default quota is 2 million input tokens per minute (TPM)", so a quota is already in place. *c* is ruled out because Anthropic only "recommends retaining activity logs on at least a 30-day rolling basis". *d* is ruled out because that is the Google route: "go to its Model Garden model card".
15. **c**. "Claude does not work well when you ask for normalized coordinates", so the program normalises (module 30, page 1). *a* is ruled out because "Claude then pads every image on the bottom and right up to a multiple of 28" by itself. *b* is ruled out because "Always normalize or rescale by the resized dimensions, not the padded dimensions". *d* is ruled out because "every box and point it returns is in that size and not in yours", whatever the source type.
16. **b**. The order is "Enterprise over personal, and personal over project" (module 39, page 1). *a* is ruled out because committing only shares a skill: "Commit it so your team gets it too". *c* is ruled out because "When two skills share a name, the location decides". *d* is ruled out because "When two skills share a name, the location decides", so one copy wins and the other is not merged.
17. **d**. "Each git worktree is a separate checkout on its own branch" (module 40, page 1). *a* is ruled out because the rule is "Commit on a branch, never on main". *b* is ruled out because memory text is only context: "Claude treats them as context, not enforced configuration". *c* is ruled out because plan mode only delays the writes: "Claude reads files and proposes a plan but makes no edits until you approve".
18. **b**. The page says "An answer is a long chain of single choices, each made with everything before it in view" (module 1, page 1). *a* is ruled out because "There is no plan stored somewhere and no lookup of a finished answer". *c* is ruled out because every choice is "made with everything before it in view", so the opening lines are still there. *d* is ruled out because the loop is only "The model computes, for every token it knows, how likely it is to come next", with no review step in it.
19. **a and e**. "A bigger window is capacity, not a reason to send everything" (module 4, page 1), and "accuracy and recall degrade as the window fills". *b* is ruled out because "accuracy and recall degrade as the window fills". *c* is ruled out because "cost grows with every token you send", whatever the effort. *d* is ruled out because "more context isn't automatically better".
20. **a**. "protecting a secret is a job for code" (module 6, page 1). *b* is ruled out because "a role is a request, not a credential". *c* is ruled out because such a line "is a request that can be argued around". *d* is ruled out because "a role is not a safeguard", and both halves still sit in the context.
21. **c**. The page states the course's own rule to "keep the format consistent across examples, since the model will copy inconsistencies" (module 6, page 2). *a* is ruled out because the count is not the problem: "Include 3-5 examples for best results." *b* is ruled out because multi-shot is for when "The task has subtle boundaries, or the format is unusual". *d* is ruled out because tags only mark examples as "wrapped in <example> tags", so that they are distinguished from instructions, and they do not change what the answers look like.
22. **b**. The page treats the prompt "like a short contract" whose parts are each checkable, with a constraint for the silent case and a test for it (module 6, page 3). *a* is ruled out because, as module 6, page 1 puts it, "A role is a request, not a credential". *c* is ruled out because what must hold is stated as a constraint: "use only the policy; if the policy does not decide the case, say", and a longer reply pins nothing down. *d* is ruled out because the page says "Order: material first, the task last".
23. **c**. The endpoint's own limits are stated: "it has its own limits" (module 18, page 2). *a* is ruled out because "Token counting is free to use". *b* is ruled out because it is "subject to requests per minute rate limits based on your usage tier". *d* is ruled out because "Token counting and message creation have separate and independent rate limits".
24. **c and e**. "A feature that waits for the first word gains little" (module 19, page 2), because fast mode raises output speed and not the time to the first token. *a* is ruled out because "Speed benefits are focused on output tokens per second (OTPS), not time to first token (TTFT)". *b* is ruled out because "Fast mode is not available with the Batch API". *d* is ruled out because it is priced "twice the standard $4 and $20".
25. **c**. "Marking costs nothing by itself" (module 20, page 1). *a* is ruled out because "You pay for what is written, what is read and what is neither". *b* is ruled out because "Cache breakpoints themselves don't add any cost". *d* is ruled out because "You can place up to four" explicit breakpoints, each of which can be read.
26. **b**. Batch usage is charged at half, and "it is the same ratio at any size" (module 21, page 1). *a* is ruled out because that halves only the output, but "The discount covers input, output and special tokens". *c* is ruled out because that is the standard price, and "at the standard $2 and $10 they cost $90" in the page's own case, twice the batch figure. *d* is ruled out because that halves only the input, yet "All usage is charged at 50% of the standard API prices".
27. **a**. "A router must normalize the model's reply and have a default route" (module 34, page 1). *b* is ruled out because "The model's reply is text, so it may carry a capital letter, a full stop or a word nobody planned", and a retry can repeat it. *c* is ruled out because workflows are "orchestrated through predefined code paths", so the routes are fixed by the program. *d* is ruled out because routing "directs it to a specialized followup task", one task and not all of them.
28. **c**. "when every worker failed, nothing is combined" (module 34, page 2). *a* is ruled out because "a synthesis over no results is a made-up answer". *b* is ruled out because "a loop with no maximum on its iterations is an open bill". *d* is ruled out because "A model's plan is text", and not a result.
29. **d**. The page says to "iterate the stream to the end instead of breaking at the result" (module 35, page 1). *a* is ruled out because the limit counts tool-use turns only, "so the final text-only answer is not counted". *b* is ruled out because partial messages only add "the stream event (only when partial messages are enabled)". *c* is ruled out because the identifier "lets you resume a conversation later with the earlier context restored", and does not replay events.
30. **b and c**. "Auto-approved tools never reach canUseTool", and "To gate every call, use a `PreToolUse` hook" (module 35, page 2). *a* is ruled out because "the callback sees only what nothing earlier decided". *d* is ruled out because a call that "bypassPermissions approved skips the callback". *e* is ruled out because "An allow rule approves a call before the callback is consulted".
31. **a**. "Set the permission mode in the options and read it back from the flags in a test" (module 35, page 3). *b* is ruled out because "decide can be right while build_options leaves an allow list in". *c* is ruled out because "The tests never call a model", which keeps them repeatable. *d* is ruled out because "If you omit it, the session can start in auto mode".
32. **c**. "the model gets a truncated preview with the path" (module 36, page 1). *a* is ruled out because an output past the limit "is automatically written to a file in the sandbox". *b* is ruled out because "Event history is persisted server-side and can be fetched in full". *d* is ruled out because such an output "is automatically written to a file in the sandbox" instead of being rejected.
33. **d**. "a create request that omits it gets `unrestricted`" (module 36, page 2). *a* is ruled out because limited mode must be chosen, and it "Restricts sandbox network access to the hosts in `allowed_hosts`". *b* is ruled out because "an omitted field is not neutral", but it is accepted and read as unrestricted. *c* is ruled out because "each session gets its own isolated sandbox (a fresh Linux container)".
34. **b**. "the failure goes back to the model as a retry message and the run continues" (module 37, page 1). *a* is ruled out because "a reply that does not validate against the output type is refused". *c* is ruled out because the design "turns a malformed reply into a recoverable event". *d* is ruled out because "a reply that does not validate against the output type is refused", and nothing is guessed.
35. **d**. The builder "raises an error for an unfilled variable" (module 24, page 1). *a* is ruled out because "A blank where a document should be gives a confident answer about nothing", and a default only hides the gap. *b* is ruled out because "A missing variable is an error". *c* is ruled out because private material "is unknown to it unless you supply it in the context or give it a tool that fetches it".
36. **c and d**. "Pass forward only what the next step needs" (module 24, page 2), which shrinks what later stages carry, and chaining lets you inspect intermediate outputs. *a* is ruled out because the method is to "ask Claude to quote relevant parts of the documents first", not to delete by guesswork. *b* is ruled out because "Each step costs the full input again" wherever the text sits. *e* is ruled out because chaining is kept to "inspect intermediate outputs or enforce a specific pipeline structure".
37. **a**. "A schema that changes on every request therefore never benefits from it" (module 25, page 1). *b* is ruled out because "Changing only `name` or `description` fields does not invalidate the cache", so renaming gains nothing. *c* is ruled out because "The cache is invalidated when the schema's structure changes or the set of tools changes". *d* is ruled out because "Compiled grammars are cached for 24 hours from last use", and the changing shape defeats any warm-up.
38. **c**. "An unbounded retry on a hard document is a cost leak" (module 25, page 2). *a* is ruled out because current models "do not support setting temperature". *b* is ruled out because "the model has no way to know what was wrong" without it. *d* is ruled out because a parse that succeeds "says nothing about the content".
39. **d**. The write-up says to "make contextualization affordable with prompt caching" (module 28, page 2). *a* is ruled out because "The model is asked to write that sentence for each chunk, with the whole document in view". *b* is ruled out because "A chunk lifted out of its document can lose its meaning". *c* is ruled out because the fix is to prepend "chunk-specific explanatory context to each chunk before embedding".
40. **b**. "`cited_text` does not count toward your output tokens" (module 29, page 3). *a* is ruled out because "File content used in Messages requests is priced as input tokens". *c* is ruled out because "Citations and structured outputs are incompatible". *d* is ruled out because "citations are guaranteed to contain valid pointers to the provided documents", which a prompt rule is not.
41. **c and d**. "The loop needs its own limit of model calls, and a status for reaching it" (module 26, page 2). *a* is ruled out because "A result that quietly returns an empty string for a failure teaches the model that the tool worked". *b* is ruled out because "A model that keeps calling a tool that keeps failing never decides". *e* is ruled out because the runner loops "until it reaches `max_iterations`".
42. **c**. "In a workflow, the script decides" (module 27, page 2). *a* is ruled out because a subagent is for "a quick, focused worker". *b* is ruled out because a skill "adds to your main window". *d* is ruled out because "Claude follows both as instructions, so neither is enforced".
43. **b**. The advice is to "call a model provider directly instead of sampling" (module 32, page 1). *a* is ruled out because "New implementations SHOULD NOT adopt it". *c* is ruled out because "roots and logging over the protocol are deprecated" as well, and neither supplies a completion. *d* is ruled out because "Elicitation lets a server ask the user for more information", not a model.
44. **d**. "a test checks that the message contains your text instead of comparing it whole" (module 32, page 3). *a* is ruled out because "a test reads the flag and the text and does not wait for an exception". *b* is ruled out because "A direct call does not see stray output, the schema or the transport". *c* is ruled out because "An SDK can add a prefix", and the fix belongs in the comparison.
45. **a**. The guidance is "a progressive, least-privilege model" (module 33, page 2). *b* is ruled out because an everything-scope design gives "users a consent dialog nobody reads", and longer lives add risk. *c* is ruled out because "Access tokens MUST NOT be included in the URI query string". *d* is ruled out because "servers should implement authentication for all connections".
46. **c**. On Streamable HTTP "Closing the SSE response stream" is the signal (module 33, page 1). *a* is ruled out because "On stdio the client sends a `notifications/cancelled` message that names the request", and that is the stdio rule. *b* is ruled out because a progress token carries "a value that must increase with each one". *d* is ruled out because "the GET stream endpoint was removed" in this revision.
47. **d**. "the Microsoft 365 add-ins are outside the Enterprise audit logs" (module 10, page 2). *a* is ruled out because "coverage is not uniform" across products. *b* is ruled out because the add-ins "do not inherit custom retention settings". *c* is ruled out because that feature is "streaming of Cowork events to the organisation's monitoring tools", not add-in events.
48. **b**. The page says that vaults store "an opaque placeholder" in the sandbox, so "The agent never sees the secret value." (module 41, page 2). *a* is ruled out because the rule for production agents is to "keep the value out of the sandbox", and rotation leaves it there. *c* is ruled out because "the environment key goes in a secrets manager and never in an image". *d* is ruled out because an instruction is a request, while "A key that appears in a tool result or an audit line has left your control".
49. **d and e**. "Leaving a block unanswered is an `invalid_request_error`" (module 31, page 1). *a* is ruled out because the page ties image size to "the toolset takes no display dimensions and the API doesn't downscale for you". *b* is ruled out because "Every block still needs a result", whatever the timing. *c* is ruled out because the cause is "an agent loop that reads only the first block", which a timeout leaves as it was.
50. **a**. "you cannot respond to a pattern you did not log" (module 41, page 1). *b* is ruled out because "A list of patterns catches the phrasing someone thought of and misses the paraphrase". *c* is ruled out because "data that sits in the system prompt gets the authority of an instruction". *d* is ruled out because "more context isn't automatically better".
51. **c**. Checkpoints "only cover file changes" (module 38, page 1). *a* is ruled out because "Actions that affect remote systems (databases, APIs, deployments) can't be checkpointed". *b* is ruled out because "Before Claude edits a file, it snapshots the current contents", and nothing is snapshotted for a database. *d* is ruled out because the command "rolls code and conversation back to a checkpoint".
52. **d**. "a `Write(...)` path rule is not consulted, so protect files with `Edit` or `Read` rules" (module 38, page 2). *a* is ruled out because "Lists, such as the allow and deny arrays, are not replaced: they combine across layers", so the layer changes nothing. *b* is ruled out because "Claude treats them as context, not enforced configuration". *c* is ruled out because it would still be a Write rule, and "a Write(...) path rule is not consulted".
53. **a**. The page says to "Treat the response as truncated, then trim or compact the context" (module 43, page 1). *b* is ruled out because the origin is "Integration: the context is yours". *c* is ruled out because the cause is that "the response filled the model's context window", which no output ceiling widens. *d* is ruled out because the fallback is the cure for a refusal: "Read `stop_details` and retry on a fallback model".

</details>
