# Level 1 mock exam 2

**Level:** Foundations · **Module 11:** Exam readiness 1 · **Page 5 of 5**
**Exams:** all (the questions follow the Associate blueprint, AS1 to AS7, over the content of modules 1 to 11)

**After this page you can** tell whether you are ready for the Associate exam, and which domains of Level 1 need more
work before you sit it.

This mock exam covers **the whole of Level 1** (modules 1 to 11) and not one page or one module. Level 1 has two mock exams of 60 questions each, the Associate exam's number of items; this is the second, and no question of one repeats a question of the other. Each is written in the
exam's style: a short scenario, one best answer and three plausible alternatives, each of which is a mistake a practitioner
could make. Every question is the course's own, written fresh; no question comes from a live exam. The facts behind each
answer were read on 2026-10-02 from the official pages named on the module pages, so a reader who studies
the pages can answer each question from them. The real exam has multiple-choice and multiple-response items, and so does this mock: a multiple-response item ends with (Select two.) and is right only when both keyed options are chosen.

## How to take it

1. **Choose your conditions.** For a readiness test, take all 60 questions in **120 minutes** with no notes, which is the
   Associate exam's pace of two minutes a question. For learning, take it untimed and read each explanation.
2. **Answer every question.** The guides describe no penalty for a wrong answer.
3. **Mark each answer** and read the explanations only after you finish.
4. **Score by domain,** not only in total, using the table below, and spend your next study session on the weakest domain
   with the most weight.

There is no official conversion from a mock percentage to a result: **the real pass mark is a scaled score of 720 on a
scale from 100 to 1,000**, so treat the mock as a guide to where you stand in each domain and never as a prediction.

## What it covers

The mock has **60 questions**, the number of items on the real exam. They are spread by the Associate blueprint's weights (each domain's weight times 60, rounded so that the total stays 60) and grouped by domain here for easy scoring; the real exam mixes them. The share column is the count divided by 60, set beside the official weight so that you can see the split matches. The modules column shows which pages to revisit for a miss.

| Questions | Associate domain | Weight | Count | Share of this mock | Drawn from modules |
|---|---|---|---|---|---|
| 1 to 8 | AS1 Prompting and task execution | 14% | 8 | 13.3% | 5, 6 |
| 9 to 21 | AS2 Output evaluation and validation | 21% | 13 | 21.7% | 1, 4, 5, 7, 8, 9 |
| 22 to 28 | AS3 Product and model selection | 12% | 7 | 11.7% | 3, 7, 8 |
| 29 to 38 | AS4 Workflow integration and solution design | 16% | 10 | 16.7% | 5, 8, 9 |
| 39 to 45 | AS5 Configuration and knowledge management | 12% | 7 | 11.7% | 7, 8 |
| 46 to 54 | AS6 Governance, risk and responsible use | 15% | 9 | 15.0% | 5, 8, 10 |
| 55 to 60 | AS7 Troubleshooting and optimisation | 10% | 6 | 10.0% | 1, 3, 4, 7, 8 |

Module 2 (how models are made) is beyond the Associate blueprint, so this mock leaves it out. Module 11 teaches the exam's format and carries no quiz of its own, so the questions draw on modules 1 to 10. The questions on how models work (tokens, sampling, context, hallucination, tiers) are the foundation the Developer and Architect exams build on, which is why Level 1 is shared by all four exams.

## Mock exam

This mock exam covers the whole of Level 1, modules 1 to 11. Choose one answer for each question, or the number the question states.

1. A prompt template wraps pasted material in tags, but each author uses different tag names for the same sort of text, and the instructions refer to them loosely. Which rule from the page fits?
   - **a**: Give each kind of input one meaningful label, used identically everywhere
   - **b**: Vary the names, keeping any two sections from being mistaken for each other
   - **c**: Drop the tags and rely on the order of the sections to mark the data
   - **d**: Use numbered tags such as part1 and part2

2. A prompt places a 30-page policy in the middle, with the instruction to list exceptions buried in a paragraph above it. The replies often summarise instead. Which edit helps most?
   - **a**: Add several paragraphs of background to make the exceptions stand out more
   - **b**: Write the instruction in capital letters but keep it inside the same paragraph
   - **c**: Move the policy below the instruction, to be read most recently
   - **d**: Give the task its own line, and restate it after the long material

3. A marketing lead gets a social post in the wrong tone, regenerates the same request four times, and keeps getting similar misses. What is the better method?
   - **a**: Regenerate the post more often and keep whichever result looks best
   - **b**: Rewrite the whole request from scratch each time, changing several parts together
   - **c**: Switch to a larger model before touching the brief
   - **d**: Name the flaw, change one element of the brief and compare results

4. A planner asks Claude to schedule five deliveries under several constraints and gets a wrong plan with no explanation. The planner wants to see where it went wrong, and a program reads only the final plan. Which prompt fits?
   - **a**: Ask the same question five times and keep the version with most agreement
   - **b**: Ask for the result only, with no working shown at all
   - **c**: Ask for working first, then the result after a marker for code to take
   - **d**: Ask for working and result mixed in one paragraph

5. A classifier prompt lists four allowed labels, but when a ticket fits none the model invents a fifth one. Which two changes address it? (Select two.)
   - **a**: Lower the temperature setting to make the answers vary less
   - **b**: Spell out the closed set in the output format
   - **c**: Ask the model to be more careful and precise in its choice
   - **d**: Name a fallback value for when it is unsure
   - **e**: Remove the list and let the model pick the closest option freely

6. A team lead asks Claude to assess whether a pilot met its targets from a set of results tables, and the replies state conclusions with no support. Which emphasis does the course give for an analysis task?
   - **a**: Breadth first, many options and judgment kept for a later pass
   - **b**: The material, the question, the criteria and proof for each finding
   - **c**: Audience, tone, length and an example of the target style
   - **d**: Named references, claims tied to them and gaps declared in the text

7. A prompt writer keeps mixing the rule "Use only the policy. One word." into the description of the response layout, and results wander. In the course's specification, where do rules that must hold belong?
   - **a**: In the task line, folded into the verb that names the work to be done
   - **b**: In the role line, written at the start of the prompt
   - **c**: In the documents section, next to the material those rules cover
   - **d**: In a constraints part of its own, apart from the one that sets shape

8. A prompt says only "Make the tone better", and the reply is a more formal rewrite although the writer wanted it casual, and a rerun gives something different each time. Which cause does the course name, and what is its fix?
   - **a**: Ambiguity, so state the exact request and add constraints, an example and a format
   - **b**: Missing context, so supply the facts, the readers and the purpose of the text
   - **c**: The wrong feature, so use a tool, retrieval or code in place of a longer prompt
   - **d**: The wrong model, so evaluate another tier or raise the effort setting for the work

9. A charity's draft appeal describes the families it serves in a pitying tone. Which check from the non-profit list applies?
   - **a**: Whether the wording suits the people it speaks about
   - **b**: Whether the appeal overstates what the programme has achieved so far
   - **c**: Whether the figures cited match the programme's own records
   - **d**: Whether the appeal names each family for the donor to follow up

10. A project manager asks Cowork for a one-page status pack and wants the reviewer to spot weak points quickly. Which line in the outcome description supports that?
   - **a**: Ask it to leave out anything uncertain, keeping the page clean
   - **b**: Ask it to list any figure it could not verify and any item with no owner
   - **c**: Ask it to schedule the pack weekly before anyone has reviewed the first one
   - **d**: Ask it to write with extra confidence, making the pack read as decisive

11. A student notices that after a few exchanges with Claude, her thesis has shifted toward the conclusion that Claude kept suggesting. Which check from the student's list was she missing?
   - **a**: Whether the explanation Claude gave was actually correct in each detail
   - **b**: Whether the rules of the course allow the amount of help that was used
   - **c**: Whether the tool is steering her to a view rather than helping her reach one
   - **d**: Whether each fact and citation in the essay has been confirmed independently

12. A clerk sends Claude a rotated, low-resolution photo of a handwritten delivery note and enters the quantities it reports without a second look. Which concern applies?
   - **a**: No concern arises, models reading handwriting reliably
   - **b**: Such images invite mistakes or invention, so the entries need verifying
   - **c**: The entries are safe if Claude is asked to say how sure it is of each one
   - **d**: The entries are safe when the quantities are small whole numbers

13. Reviewers of a research assistant's answers want each claim to be traceable and any weak claim removed before reading. Which prompt addition fits?
   - **a**: A second run of each question to compare the two answers
   - **b**: A score out of ten that the assistant gives to each answer it writes
   - **c**: A supporting quote per statement, and withdrawal of anything unbacked
   - **d**: Permission to say it does not know whenever the material is thin

14. A strategist uses Research mode for a vendor comparison and receives a report with a citation after each claim. Which step is still needed before it goes to the board?
   - **a**: Run the same research again with web search off and compare the totals
   - **b**: None, each claim already carrying its citation
   - **c**: Check the statements that would hurt if wrong against the sources cited
   - **d**: Repeat the research several times and keep the statements that every run shares

15. A team adds quotes, citations, an "I don't know" option and a restricted source list, and then tells users the answers no longer need review. Which two statements describe how the course treats that claim? (Select two.)
   - **a**: It is sound, because each technique adds an independent check in code
   - **b**: It overreaches, since errors are reduced and not removed
   - **c**: It is reasonable if users are told to ask Claude how sure it is
   - **d**: It is sound, because grounded prompts leave hallucinations at zero
   - **e**: Checking stays, since these steps make it faster and do not replace it

16. An HR assistant uploads a group photo from a company event and asks Claude to name each person pictured for a directory. What should the team expect?
   - **a**: Names looked up from the staff files after Claude checks each face against them
   - **b**: Accurate names for everyone whose face is clear, with a note of uncertainty
   - **c**: Approximate names, with each face marked by pixel coordinates
   - **d**: A refusal to identify them, so labels come from a roster or the people

17. Claude works through a multi-step pricing calculation and gives a confident chain of steps ending in a total. A single wrong step early on would spoil everything after it. What should decide whether the total is trusted?
   - **a**: The model's own statement that it re-checked each step it took
   - **b**: A check outside that reasoning, such as a test or a second reader
   - **c**: A higher thinking setting for the same calculation
   - **d**: The length and detail of the steps shown to the reader

18. A retailer asks Claude to return pixel coordinates of price tags in shelf photos and crops each photo automatically. Some crops miss the tags by a few pixels. Which limit explains it?
   - **a**: Claude reads only the text printed in images, so positions are pure guesses
   - **b**: Claude cannot read images under 200 pixels, so every tag is missed
   - **c**: Spatial answers are approximate, so each cut-out needs a margin or a check
   - **d**: The photos were not sent ahead of the request text, so positions drift

19. A benefits chatbot answers from the company handbook, but some replies add rules borrowed from how other employers work. Which prompt instruction targets this?
   - **a**: Run each question three times and keep the answer that repeats most often
   - **b**: Give it the role of an HR director with many years of experience
   - **c**: Ask it to state how sure it is about each rule that it gives
   - **d**: Tell it to rely solely on the supplied texts, ignoring its training

20. A student uses Claude to find sources for an essay and pastes three citations into the bibliography, one of which does not exist. Which two habits would have prevented it? (Select two.)
   - **a**: Verify every fact that enters graded work
   - **b**: Use the largest model available, which never invents references
   - **c**: Tell Claude that the essay is graded, to make it take more care
   - **d**: Confirm each reference in a library or database
   - **e**: Ask Claude whether each reference is real before using it

21. A freelance writer uses Claude to draft a short story opening and wants to be sure it is safe to publish under her own name. Which check does the course name for creative work?
   - **a**: Whether a disclosure line can be skipped for short pieces
   - **b**: Whether it is too close to an existing piece, and sounds like her
   - **c**: Whether the plot can be handed to Claude to finish
   - **d**: Whether Claude's reply says that the opening is original

22. A team has no benchmark yet and must pick a first model for a mixed set of tasks. What does the models overview advise as the default starting point for most workloads?
   - **a**: Begin with Claude Haiku 4.5 and move up if it fails
   - **b**: Begin with Claude Sonnet 5.5 and never test the other tiers against it later
   - **c**: Begin with Claude Fable 5.1 and move down for cost
   - **d**: Begin with Opus 5.5, moving to Fable 5.1 if high-effort tests fall short

23. A designer uses Firefox and a phone, and asks whether Claude in Chrome can be installed there. Which two statements does the course support? (Select two.)
   - **a**: It is a side panel in a single browser
   - **b**: It works only on the free plan, which is the one that includes it
   - **c**: It works in any browser that is built on the same open-source base
   - **d**: It installs as a desktop extension that runs a local server on the phone
   - **e**: Other browsers and mobile are excluded

24. A start-up needs Claude inside its own mobile screens, with its own conversation storage and tools. Which surface does the page choose?
   - **a**: Claude Code, the terminal agent for everyday software work
   - **b**: A Project in the apps, with its own shared files and notes
   - **c**: Managed agents, hosted and run by the provider
   - **d**: The Messages API, with the loop and the state written by hand

25. A developer team wants a bespoke agent that reuses Claude Code's built-in capabilities, with its own control over orchestration, tool access and permissions. Which surface fits?
   - **a**: The SDK, which exposes that engine as a library
   - **b**: Claude Code itself, used as a ready-made tool
   - **c**: Managed agents, hosted and run by the provider
   - **d**: The Messages API, called directly with a loop

26. A company on a Pro plan wants Claude Tag in its Slack workspace. What does the Tag page say about availability?
   - **a**: It works on any plan, but only where zero data retention is enabled
   - **b**: It is for Team and Enterprise subscriptions only, and on Anthropic's own service
   - **c**: It is offered on every paid plan, including Pro and Max, for any workspace
   - **d**: It is offered through cloud platforms such as Bedrock as well as directly

27. A free-plan user wants to hand Cowork a folder on her computer. What does the help centre say it needs?
   - **a**: A terminal with the command-line tool installed
   - **b**: A Team plan only, no other plan having it at all
   - **c**: A paid subscription and the desktop app, open and connected
   - **d**: Nothing beyond a browser with the web app open and a login

28. A manager asks Claude for a two-sentence reply to a colleague and then for a reusable 40-line status dashboard for her team. Which pair of outputs does the help centre's rule produce?
   - **a**: The short reply as an artifact and the dashboard inline
   - **b**: Both as artifacts, opened in a side panel for the manager
   - **c**: The short answer stays in chat; the larger one opens in a panel
   - **d**: Both inline, kept in the chat thread as plain text

29. A team plans an agent that will update records and send emails by itself. Which approach reflects the course's habit for human and agent teams?
   - **a**: Treat it as a new colleague: set a scope, review before consequences, keep a log
   - **b**: Give it full access at first and trim its rights after the first incident occurs
   - **c**: Let it act freely and read a summary of its work at the end of each month
   - **d**: Limit it to read-only work and let nobody approve anything that it proposes

30. An office manager considers letting Claude clear old customer files from a shared drive automatically each night. Which reason from the delegation guidance argues against handing this step over?
   - **a**: The step tolerates an occasional miss that a reviewer would catch
   - **b**: A mistake there would be hard to undo once it ran
   - **c**: The task is small, clear and has a visible result for a person to judge
   - **d**: The work is shaped like language, so a person must do it

31. A manager sets a recurring Cowork task for the early morning and wants it to run while her laptop is shut and the desktop app is closed. Which two statements does the page support? (Select two.)
   - **a**: No awake machine is needed
   - **b**: It is not possible, Cowork being unable to schedule anything
   - **c**: It runs only while the desktop app stays open and connected
   - **d**: It runs on the laptop and waits until the machine is next powered up
   - **e**: Anthropic's servers do the job

32. A ceramic artist asks whether to adopt Claude across her practice. Which starting point does the creative-work course recommend?
   - **a**: Full use from the start, dropped later wherever it harms the style
   - **b**: Use at every stage of the work, from sketch to final glaze
   - **c**: Use only for the final signature and the public statements about the piece
   - **d**: No AI at first, adding it where analysis shows it earns its place

33. In Anthropic's four-part fluency framework, which official competency matches the course's habit of taking responsibility, and what question does it ask?
   - **a**: Description: have I said what I want so the job can be done and judged?
   - **b**: Diligence: will I stand behind this and tell people what Claude did?
   - **c**: Discernment: is what came back accurate, complete, fair and fit for its reader?
   - **d**: Delegation: which parts of this job go to Claude and which stay with me?

34. A buyer compares three suppliers' public price pages and fills a quote form, wanting to watch each step. Which tool and approval choice fit?
   - **a**: Claude in Chrome on trusted sites, each action confirmed, clean profile
   - **b**: Claude in Chrome with Skip all approvals switched on for speed
   - **c**: Cowork on the whole drive with Auto approval, letting no single step wait
   - **d**: Research mode with web search left open on all sites

35. Six months ago a manager used plain chat for a one-off competitor summary. The work now repeats every week with the same rules and files. What does the course suggest?
   - **a**: Switch to Claude in Chrome to have the web pages reread each week
   - **b**: Keep using plain chat for the weekly job as before
   - **c**: Revisit the choice of tool, moving the routine into a Project or skill
   - **d**: Move to Cowork and let it run the weekly job on its own

36. A lecturer asks Claude to draft a marking rubric and then to grade the pupils' essays against it, and records the grades. What stays with the lecturer under the course's guidance?
   - **a**: Little, Claude being able to mark and record the essays
   - **b**: The final signature, once Claude has produced each mark for the class
   - **c**: The first draft of the rubric, with the grading handed over
   - **d**: Evaluating the work and any decision that affects the learners

37. A team lead tags Claude in a thread to compile a report, then closes Slack for the evening. A colleague adds a correction as a later message. Which two statements describe what happens? (Select two.)
   - **a**: Replies posted afterwards are followed
   - **b**: It ignores the late addition, reading only the tagged post
   - **c**: Work stops when Slack closes, the task running inside the browser tab
   - **d**: It needs a fresh tag before it reads anything new
   - **e**: The work carries on in the cloud

38. A new team member wants Claude to produce the entire quarterly customer review, from data pull to final slides, in one go on day one. What does the delegation guidance suggest?
   - **a**: Hand over the entire job in one go on day one
   - **b**: Hand over a small, clear step whose result shows, then widen it
   - **c**: Hand over only the final slides, with the data pulled
   - **d**: Hand over everything but ask for a confidence note at the end

39. An operations lead wants to package the team's meeting-notes format so that Claude applies it whenever relevant, and has no programmer available. What is needed?
   - **a**: A script in Python that calls the API once for each set of notes
   - **b**: Markdown instructions in the required folder structure, zipped and uploaded
   - **c**: A connector to the notes folder with write access for the assistant
   - **d**: A plugin built by a partner company for note-taking and shared with the team

40. A team builds a Project knowledge base from a forwarded summary, a draft policy and a thick scan with unreadable pages. Which principle did they ignore?
   - **a**: Judge the material first, preferring approved primary text
   - **b**: Remove superseded versions from the knowledge base
   - **c**: Name an owner and a review date for each file
   - **d**: Cite the source in each answer given to a reader of the Project

41. A team wants Claude to use a connector that reads a local application on one employee's machine, not a service on the internet. Which shape fits?
   - **a**: A skill that bundles the application's instructions for Claude to load
   - **b**: A custom connector in the directory, which Anthropic has already verified
   - **c**: A desktop extension, which hosts its endpoint on that same computer
   - **d**: A remote connector, which reaches an MCP server from Anthropic's cloud

42. A power user has connected twelve connectors and notices conversations feel crowded with tool lists. Which two statements fit the help centre's suggestion? (Select two.)
   - **a**: Blocked removes every tool from the list for good
   - **b**: Auto loads every tool name at the start of each conversation
   - **c**: Always allowed keeps the lists short by hiding the prompts
   - **d**: Load each integration only when the work needs it
   - **e**: Ten or more is the point where it pays off

43. A team's Project holds a pricing file that nobody has reviewed for a year, and answers drift out of date without any alert. Which practice would have prevented it?
   - **a**: Telling Claude in the instructions to flag old files whenever it notices them
   - **b**: Naming an accountable person for each source and fixing a recurring check
   - **c**: Uploading the file again whenever somebody happens to remember to do it
   - **d**: Keeping every earlier version in the Project in case it is needed

44. An analyst sets a formatting convention in the Excel add-in's instructions, then finds the Word add-in ignores it. Why?
   - **a**: Excel instructions work only when macros are enabled in the workbook
   - **b**: Cross-app work is switched off, which blocks every instruction from passing between apps
   - **c**: Each one keeps its own rules, so the setting does not carry into another app
   - **d**: Word needs an Enterprise plan before it can read any instructions

45. A paid-plan Project nears its context limit, yet it keeps answering from its large knowledge base. What does the help centre describe, and what follows from it?
   - **a**: It retrieves the relevant parts, so muddled files spoil replies at scale
   - **b**: It drops the oldest files one by one until the whole Project fits again
   - **c**: It folds every file into the instructions and deletes the original files
   - **d**: It stops accepting questions until enough files have been removed

46. A freelancer on a Pro plan asks whether Anthropic uses her chats to improve Claude. What does the course say?
   - **a**: Only for her incognito chats, which feed improvements to the models
   - **b**: Only if she opts in through privacy settings, or a conversation is flagged
   - **c**: Never, whatever settings she has chosen or plan she holds
   - **d**: Always, with no setting available to stop the use in any plan

47. A user sets Claude in Chrome to its least restrictive approval mode to save clicks while she browses her bank. Which two statements does the page support? (Select two.)
   - **a**: Claude still reviews each action for safety and blocks anything unsafe
   - **b**: It is advised for sites she visits often, such as her bank
   - **c**: It suits only complete trust
   - **d**: Nothing checks what is done
   - **e**: Downloads and sensitive entries are still skipped silently there

48. A manager wants to paste an unreleased financial forecast, marked confidential, into a chat tool that the company has sanctioned. Which rule fits?
   - **a**: Ask Claude to forget the forecast afterwards, which satisfies the rule
   - **b**: Treat it as internal, so any tool that is approved is fine for everyone
   - **c**: Treat it as regulated, with no tool ever allowed to receive it
   - **d**: Permitted there for those who need it, often with identifiers removed

49. A security lead wants Claude in chat and Cowork to stop reaching the open web for everyone, without disabling the apps. Which control do the pages describe?
   - **a**: A lookup-feature switch that account owners set organisation-wide
   - **b**: A sites allowlist in the browser extension, which also covers chat and Cowork
   - **c**: A retention setting that deletes browsing history after thirty days
   - **d**: A per-user prompt telling Claude not to use the internet

50. An assistant asks a user to confirm "Send this email to everyone on the list?" while she works through a long task, and she presses Allow without a glance. Which two statements does the course support? (Select two.)
   - **a**: It matters only if the email carries an attachment or a link
   - **b**: It is a formality, the permission having been set at the start of the task
   - **c**: It is harmless, classifiers already screening every action taken
   - **d**: Approving unread hands approval to whatever was injected
   - **e**: The prompt is the control

51. A team lead tags Claude in a public channel and asks it to summarise what the finance team said in their private channel. What will happen?
   - **a**: It reads the private room through the tools that an admin connected
   - **b**: It searches the private room as a person would
   - **c**: It summarises from a Slack canvas shared in the public room
   - **d**: It cannot search that conversation

52. A campaign group plans to have Claude send thousands of personal-looking messages to voters that hide their automated origin. Which prohibited category on the policy's list fits?
   - **a**: Compromising privacy or identity of private persons
   - **b**: Criminal-justice, censorship and surveillance uses
   - **c**: Undermining democratic processes, such as deceptive content
   - **d**: Fraudulent, abusive or predatory practices aimed at customers

53. A consultant sends a client a market summary drafted with Claude, and the client's own policy asks suppliers to say when AI helped. She says nothing. Which responsibility did she skip?
   - **a**: Data care, such as keeping client files out of the chat
   - **b**: Accountability for the accuracy of what she sends to the client
   - **c**: Fairness and impact on the people the summary covers
   - **d**: Being honest about the tool's part when a rule demands it

54. A company builds an autonomous agent that acts on customers' behalf, and its lawyer asks whether the Usage Policy's rules apply when no person reads each output. What does the page say?
   - **a**: Only the high-risk requirements apply
   - **b**: Unattended operation must still conform, and extra guidance exists for it
   - **c**: The policy covers chatbots and nothing else, so an agent falls outside its reach
   - **d**: The rules apply only when a person reads each output before it is used

55. A researcher uses Research mode all morning and then finds her usage limits nearly exhausted, though her ordinary chats were few. What explains it?
   - **a**: Research runs the model at maximum effort throughout the whole morning session
   - **b**: Memory was switched on, which re-reads every earlier chat each time it answers
   - **c**: Research is billed separately, so the ordinary limits are left untouched by it
   - **d**: It retrieves many sources, which drains allowances faster than simple talk

56. A multi-turn assistant on Claude Opus 5.5 with thinking active fills its window sooner than the team expected, though visible replies are short. What contributes?
   - **a**: Earlier thinking is stripped each turn, so only the latest turn matters
   - **b**: Earlier reasoning is retained by default and takes up room
   - **c**: Visible replies are billed twice, so they fill the space twice over
   - **d**: Thinking is stored on the server, so it never reaches the request at all

57. On a Team plan, an analyst's Excel conversation cannot write into the open Word memo, though the add-in works fine in each app. What is the likeliest cause?
   - **a**: The cross-file setting is off by default there, and set per device
   - **b**: Excel instructions carry into Word, so the two sets must have conflicted
   - **c**: Claude can open and switch files itself, so the memo must have been locked
   - **d**: Word memos can be read by Claude only through a cloud platform

58. A nightly job that summarised last month's reports now fails with a "prompt is too long" error on this month's larger reports, using the same code. Which statement fits?
   - **a**: Raising the output limit enlarges the window, so the job will then pass
   - **b**: The request is accepted anyway and simply ends with a stop reason
   - **c**: The input alone exceeds the window, so it must shrink or the window grow
   - **d**: A newer tokenizer shrank the text, so the same words now fit more easily

59. A request with thinking active returns an answer cut off mid-sentence, though the output limit was set generously and the input is small. Which two statements explain it? (Select two.)
   - **a**: The input exceeded the window, which ends the request with an error
   - **b**: Reasoning tokens count against the same cap
   - **c**: The model reached a word count that Claude sets for answers
   - **d**: Less room is left for the visible text
   - **e**: Thinking text is free, so it leaves the budget alone

60. A summarisation job on Claude Opus 5.5 sends short inputs but generates long answers, and the bill is higher than planned. Which price fact points to the lever?
   - **a**: Output tokens are free once the window is not full, so length is harmless
   - **b**: Input and output tokens share one price, so only the prompt length matters
   - **c**: Each produced token costs about five times a fed-in one, so brevity pays most
   - **d**: A larger window cuts the price of each output token, so a bigger model saves money

<details>
<summary>Answer key</summary>

1. **a**. One meaningfully named tag per kind of content, used the same way every time (module 6, page 2). *b* is ruled out because "The names have no magic; they have to be meaningful and used the same way every time". *c* is ruled out because "Order is not a boundary". *d* is ruled out because the rule is "One tag per kind of content".
2. **d**. A key instruction goes on its own line and is restated after long material (module 6, page 1). *a* is ruled out because the page says "More context is not automatically better". *b* is ruled out because the page says "A key instruction buried in a long paragraph is easy to underweight". *c* is ruled out because the page advises "Place your long documents and inputs near the top of your prompt, above your query, instructions, and examples".
3. **d**. Revising the request one change at a time is the method, and re-rolling is the reflex (module 5, page 2). *a* is ruled out because "Regenerating the same request and hoping is the reflex; revising the request is the method". *b* is ruled out because the method is to "change one thing in the description", not several at once. *c* is ruled out because the page advises "ask why before asking again".
4. **c**. Reasoning is requested before the answer and kept apart, so a program uses the answer while a person reads the steps (module 6, page 2). *a* is ruled out because "Comparing detects the variation; it does not remove it". *b* is ruled out because the page notes that "when an answer looks wrong, the steps tell you where it went wrong". *d* is ruled out because the page says to "Keep the reasoning separate from the answer".
5. **b and d**. Say what to do when unsure, for example use unknown, and list the values as in "label is one of billing, bug, account, other" (module 6, page 2). *a* is ruled out because "The models this course uses do not accept them". *c* is ruled out because the page names "Stacking adjectives instead of constraints" as a trap. *e* is ruled out because the format lists allowed values, where "label is one of billing, bug, account, other".
6. **b**. Analysis wants the material, the question and the criteria, and the evidence for each conclusion (module 6, page 3). *a* is ruled out because the brainstorming task wants "Breadth first, many options, no early judgment". *c* is ruled out because the drafting task wants "Audience, tone, length, an example of the target style". *d* is ruled out because the research task wants "Sources named, claims tied to sources".
7. **d**. Constraints answer "What must hold?" and the output format fixes the shape, so each is its own part of the specification (module 6, page 3). *a* is ruled out because the task part answers "What exactly is to be done now?". *b* is ruled out because the role part answers "Who is doing the work?". *c* is ruled out because the documents part answers "What material to use?".
8. **a**. A reasonable answer to a different question, with results that change from run to run, is ambiguity, and the fix is to "Say exactly what is wanted; add constraints, an example, a format" (module 6, page 4). *b* is ruled out because missing context shows as "Generic or invented content", not as a reasonable answer to the wrong question. *c* is ruled out because the wrong feature is the case where "The job needs something the prompt cannot give". *d* is ruled out because the wrong model is "Correct instructions, correct context, still a capability or speed miss".
9. **a**. The non-profit list includes whether the language fits the community described (module 9, page 2). *b* is ruled out because that check is "whether a draft overstates what the programme achieved". *c* is ruled out because that check is "every claim about impact, every statistic and every funder requirement against the source". *d* is ruled out because "Donor and beneficiary records are sensitive".
10. **b**. The worked task asks it to list any figure it could not verify against the tracker, and any action item with no owner (module 8, page 1). *a* is ruled out because "A declared gap is information". *c* is ruled out because "Only once the output is reliably right is a weekly schedule added". *d* is ruled out because "Confidence of tone is not a signal".
11. **c**. The student's checks include whether the tool is leading her to a conclusion rather than helping her reach one (module 9, page 1). *a* is ruled out because that check is "whether an explanation is actually correct", which concerns correctness. *b* is ruled out because that is "honesty about help received under the rules of their course", which concerns ownership. *d* is ruled out because that check is "every fact and citation that goes into graded work", which concerns accuracy and not direction.
12. **b**. Low-quality or rotated images invite mistakes and invention, so the readings are verified (module 4, page 1). *a* is ruled out because the vision page says it "might hallucinate or make mistakes when interpreting low-quality, rotated, or very small images". *c* is ruled out because "Self-reported confidence is not a measure of accuracy". *d* is ruled out because "Do not use Claude for tasks requiring perfect precision or sensitive image analysis without human oversight".
13. **c**. Cite and retract: ask for a supporting quote for each claim, and to drop any claim it cannot support (module 1, page 2). *a* is ruled out because "Comparing detects the variation; it does not remove it". *b* is ruled out because "Self-reported confidence is not a measure of accuracy". *d* is ruled out because that technique tells the model "it may say so when the material is not enough", and it traces nothing.
14. **c**. A citation shows where an answer came from, not that the source is right, so the statements that matter are checked against it (module 7, page 2). *a* is ruled out because Research needs "web search must be turned on". *b* is ruled out because "a citation shows where an answer came from, not that the source is right". *d* is ruled out because "Comparing detects the variation; it does not remove it".
15. **b and e**. The techniques are mitigations, and "These make checking faster; they do not replace it" (module 5, page 2). *a* is ruled out because "These make checking faster; they do not replace it". *c* is ruled out because "confidence is not evidence". *d* is ruled out because the documentation says they "don't eliminate them entirely".
16. **d**. Claude cannot be used to name people in images and refuses to (module 4, page 1). *a* is ruled out because what training gives excludes "Access to your files, systems or today's date". *b* is ruled out because "Claude cannot be used to name people in images, and refuses to". *c* is ruled out because "Coordinates and localisation outputs are approximate", and the refusal still applies.
17. **b**. Reasoning that decides something important needs a check from outside the same reasoning (module 4, page 1). *a* is ruled out because "Self-reported confidence is not a measure of accuracy". *c* is ruled out because turning thinking up is "billed as output and slows the answer", and it does not supply a separate check. *d* is ruled out because "A confident chain of steps can contain a wrong step that makes everything after it wrong".
18. **c**. The vision page lists spatial reasoning as approximate: coordinates and localisation outputs are approximate (module 4, page 1). *a* is ruled out because all current models "support text and image input". *b* is ruled out because the page warns about "low-quality, rotated, or very small images under 200 pixels", not about all tags. *d* is ruled out because "Claude works best when images come before text".
19. **d**. Restricting knowledge tells the model to use only the provided documents, not its general knowledge (module 1, page 2). *a* is ruled out because "Comparing detects the variation; it does not remove it". *b* is ruled out because "A role is a request, not a credential". *c* is ruled out because "Self-reported confidence is not a measure of accuracy".
20. **a and d**. A student checks every fact and citation that goes into graded work, because a hallucination includes "an invented citation, a function that does not exist" (module 9, page 1). *b* is ruled out because even the documented techniques "don't eliminate them entirely", so no model never invents references. *c* is ruled out because "a plausible-looking fact is a likely continuation whether or not it is true". *e* is ruled out because "Self-reported confidence is not a measure of accuracy".
21. **b**. For creative work the checks are originality, accuracy, tone and whether the piece still sounds like the maker (module 9, page 2). *a* is ruled out because "Disclosure matters more here than in many fields". *c* is ruled out because a maker keeps "the idea, the voice, the final choices and the signature". *d* is ruled out because "Self-reported confidence is not a measure of accuracy".
22. **d**. The overview's default advice is to start with Claude Opus 5.5 for most workloads and use Claude Fable 5.1 when evals at higher effort still fall short (module 3, page 1). *a* is ruled out because "The overview's default advice is to start with Claude Opus 5.5 for most workloads". *b* is ruled out because "Having a good evaluation set is the most important step". *c* is ruled out because "the top tier for everything wastes the budget".
23. **a and e**. Claude in Chrome is an extension in the Chrome side panel on the paid plans, and "It is not supported on other Chromium browsers or on mobile" (module 8, page 2). *b* is ruled out because the extension is offered "on the paid plans (Pro, Max, Team, Enterprise)". *c* is ruled out because "It is not supported on other Chromium browsers or on mobile". *d* is ruled out because "Desktop extensions are for the desktop app only".
24. **d**. A product needing Claude inside its own screens uses the Messages API (module 3, page 2). *a* is ruled out because that row is for "A developer wants help changing a codebase". *b* is ruled out because the apps row reads "Instructions and uploads, not code". *c* is ruled out because that row is for "A job runs for hours and should not tie up your servers".
25. **a**. The Agent SDK lets you build your own agents powered by Claude Code's tools and capabilities, with full control over orchestration, tool access and permissions (module 3, page 2). *b* is ruled out because that row is for "A developer wants help changing a codebase". *c* is ruled out because that row is for "A job runs for hours and should not tie up your servers". *d* is ruled out because the API row says "Direct access to the model; your own loop, tools and state".
26. **b**. Claude Tag is available on Team and Enterprise plans only, on Anthropic's own service, and not for organisations with zero data retention or customer-managed encryption (module 8, page 2). *a* is ruled out because it is "not for organisations with zero data retention or customer-managed encryption, because it stores channel memory and session transcripts". *c* is ruled out because the page says it is "available on Team and Enterprise plans only". *d* is ruled out because the page says it runs "on Anthropic's own service".
27. **c**. Cowork needs a paid plan, and local file access needs the desktop app for macOS or Windows to be open and connected (module 8, page 1). *a* is ruled out because "There is no terminal". *b* is ruled out because "Cowork needs a paid plan (Pro, Max, Team or Enterprise)". *d* is ruled out because "Local file access, browser use and computer automation need the Claude desktop app for macOS or Windows to be open and connected".
28. **c**. A short answer stays inline, and significant, self-contained, reusable content becomes an artifact (module 7, page 2). *a* is ruled out because an artifact is made for content "typically over 15 lines, and reusable on its own". *b* is ruled out because "A short answer stays inline in the chat". *d* is ruled out because artifacts include "documents, code, web pages, images and diagrams, dashboards and interactive tools".
29. **a**. The agent is treated like a new colleague with limited access (module 5, page 1). *b* is ruled out because the page says "Start narrow, learn how Claude does on your material, then widen". *c* is ruled out because the habit needs "a review before consequences". *d* is ruled out because the questions include "what must a person approve, who is notified of what it did, and where does it stop".
30. **b**. A step stays with a person or code when a mistake would be hard to reverse (module 5, page 1). *a* is ruled out because a good step is "tolerant of an occasional miss that a review will catch". *c* is ruled out because "A small, clear step with a visible result is easier to judge". *d* is ruled out because a step suits Claude when it is "drafting, summarising, comparing, extracting, rewording, brainstorming".
31. **a and e**. A recurring task set with /schedule "runs in the cloud, so it does not need your computer awake or the desktop app open" (module 8, page 1). *b* is ruled out because "A recurring task, set with a /schedule command". *c* is ruled out because a recurring task "runs in the cloud, so it does not need your computer awake or the desktop app open". *d* is ruled out because "Tasks run in the cloud on Anthropic's servers; sessions can continue when your computer is offline".
32. **d**. The course recommends a deliberate starting point of no AI, adding it only where analysis says it earns its place (module 9, page 2). *a* is ruled out because the course "recommends a deliberate starting point of no AI". *b* is ruled out because "audiences, commissioners and platforms may have rules or expectations about AI involvement". *c* is ruled out because a maker keeps "the idea, the voice, the final choices and the signature".
33. **b**. Diligence is the official name for taking responsibility: will I stand behind this, tell people honestly what Claude did and have I protected the data (module 9, page 1). *a* is ruled out because that question belongs to the habit of describing: "Have I said what I want, so the job can be done and judged?" *c* is ruled out because the table gives that question as "Is what came back accurate, complete, fair and fit for its reader?" for the habit of judging. *d* is ruled out because that question belongs to deciding what to hand over: "Which parts of this job go to Claude, and which stay with me?"
34. **a**. A step-by-step task on trusted live pages suits Claude in Chrome with manual approval and a clean profile (module 8, page 3). *b* is ruled out because in that setting "Claude does not pause and nothing checks its actions". *c* is ruled out because "A dedicated folder limits what an injected instruction or a mistake can reach". *d* is ruled out because Research suits "A job one document answers", and not a step-by-step task that the buyer wants to watch.
35. **c**. A task that began as a one-off and became weekly deserves a Project or a skill (module 8, page 3). *a* is ruled out because Chrome is for "A step-by-step task on a website you trust". *b* is ruled out because the page says to "revisit it when the work changes". *d* is ruled out because "Picking the most powerful surface" is named as a trap.
36. **d**. Judging student work and consequential decisions stays with the educator (module 9, page 1). *a* is ruled out because the educator "Owns: what students are taught, how they are assessed and the fairness of grades". *b* is ruled out because "Judging student work and decisions with consequences for a student stay with the educator". *c* is ruled out because the usage policy treats "academic testing and admissions as a high-risk use needing human review".
37. **a and e**. Tasks run in the cloud, "Claude keeps working after you close Slack", and once in a thread it follows every reply (module 8, page 2). *b* is ruled out because "once it is in a thread it follows every reply". *c* is ruled out because "Claude keeps working after you close Slack". *d* is ruled out because a thread it has joined needs no new tag, because "once it is in a thread it follows every reply".
38. **b**. Decide how much to hand over at once: a small, clear step with a visible result is easier to judge, so start narrow and widen (module 5, page 1). *a* is ruled out because "A good draft does not show the figures were right". *c* is ruled out because the figures need "Exact numbers, authoritative source" from code or an analyst. *d* is ruled out because "Self-reported confidence is not a measure of accuracy".
39. **b**. A simple custom skill needs no code: Markdown instructions in the required folder structure, zipped and uploaded (module 7, page 2). *a* is ruled out because "A simple custom skill needs no code". *c* is ruled out because skills are "folders of instructions, scripts and resources that Claude loads dynamically". *d* is ruled out because partner skills are "From other companies, built to work with their connectors".
40. **a**. Quality in is quality out: prefer final approved text, primary documents and short clean files (module 7, page 1). *b* is ruled out because "Two versions of one document side by side invite Claude to quote either", and the stem lists no duplicates. *c* is ruled out because "nothing alerts you when a file goes stale", which is about upkeep and not about the quality of what goes in. *d* is ruled out because "a citation shows where an answer came from, not that the source is right".
41. **c**. Desktop extensions are for the desktop app only and run a server locally, while remote connectors reach a server from Anthropic's cloud (module 7, page 3). *a* is ruled out because skills are "folders of instructions, scripts and resources that Claude loads dynamically". *b* is ruled out because "a custom connector has not been verified by Anthropic". *d* is ruled out because remote connectors "reach an MCP server from Anthropic's cloud, not from your device".
42. **d and e**. The page suggests On demand "when you have ten or more connectors" (module 7, page 3). *a* is ruled out because the page describes how "connector tools are loaded", and blocked is a permission instead. *b* is ruled out because the page suggests On demand "when you have ten or more connectors". *c* is ruled out because "each tool can be always allowed, needs approval or blocked" is a permission rule, not a loading mode.
43. **b**. Each source needs an owner and a review date, because nothing alerts you when a file goes stale (module 7, page 1). *a* is ruled out because "nothing alerts you when a file goes stale". *c* is ruled out because a stale file should be "a missed appointment, not a surprise". *d* is ruled out because "Old and new documents side by side give Claude two sources to quote".
44. **c**. Instructions are set per add-in and do not carry from Excel into Word or PowerPoint (module 8, page 2). *a* is ruled out because "Macros and VBA, and data tables, are unsupported". *b* is ruled out because cross-app work concerns files, through "Let Claude work across files". *d* is ruled out because the add-ins are "installed from Microsoft AppSource, for the paid plans".
45. **a**. Near the limit the paid plans switch on retrieval of the relevant parts, and a retrieved passage is only as good as its source (module 7, page 1). *b* is ruled out because Claude "enables RAG mode, retrieval of the relevant parts" and does not remove files. *c* is ruled out because retrieval "expands the Project's capacity" and leaves the originals in place. *d* is ruled out because "A big knowledge base is possible" under that mode.
46. **b**. Consumer chats are used only if the user allows it in privacy settings, or if a conversation is flagged (module 10, page 1). *a* is ruled out because "Incognito chats are not used to improve Claude". *c* is ruled out because "By default Anthropic does not use inputs or outputs from commercial products to train models" applies to Team, Enterprise and the API. *d* is ruled out because chats are used "only if you choose to allow it in your privacy settings".
47. **c and d**. Skip all approvals means nothing checks the actions, so it is for complete trust only, and the habit is "do not manage financial accounts, legal documents or medical information through it" (module 8, page 2). *a* is ruled out because that is the automatic mode, where "Claude keeps working and reviews each action for safety". *b* is ruled out because the habit is "do not manage financial accounts, legal documents or medical information through it". *e* is ruled out because even then "certain things still need your explicit approval".
48. **d**. Confidential data goes only in approved tools, with a need to know and often with identifiers removed (module 10, page 1). *a* is ruled out because "An instruction to forget is not a control". *b* is ruled out because confidential examples are "Contracts, unreleased financials, customer lists". *c* is ruled out because the regulated class holds "Health records, payment-card data, government IDs, personal data under GDPR".
49. **a**. Owners can turn web search off for chat and Cowork (module 10, page 2). *b* is ruled out because "Claude in Chrome sites can be allow-listed or blocklisted". *c* is ruled out because the table lists "Retention settings, including custom retention or zero data retention for Enterprise agreements". *d* is ruled out because "A prompt is a request, not a guarantee".
50. **d and e**. The confirmation prompt is the control, and the habit is to "read each confirmation before agreeing" (module 10, page 2). *a* is ruled out because the habit is to "read each confirmation before agreeing". *b* is ruled out because "Do not press Allow out of habit". *c* is ruled out because "the risk is not zero".
51. **d**. Claude Tag reads other public channels only by searching, and private channels and DMs only from inside them (module 8, page 2). *a* is ruled out because "Where you tag decides what it can use and who sees the result". *b* is ruled out because "Private channels and DMs are read only from inside them". *c* is ruled out because "A Slack canvas is not readable".
52. **c**. The policy lists undermining democratic processes, such as deceptive political content or automated messages that hide their artificial origin (module 10, page 1). *a* is ruled out because that category covers "sharing personal information without consent". *b* is ruled out because that category covers "tracking a person's location without consent or scoring people's trustworthiness". *d* is ruled out because that category lists "phishing, fake reviews, falsified documents".
53. **d**. Disclosure: be honest about the AI's part when it matters to them, when a policy requires it or when they ask (module 5, page 2). *a* is ruled out because "Do not put in what you may not share; anonymise first". *b* is ruled out because "An error in AI-assisted work is your error to correct". *c* is ruled out because the duty is to "Consider who is affected by the work".
54. **b**. The policy has universal standards, high-risk requirements and additional guidelines for chatbots, minors, agentic use and MCP servers, and agentic use must still comply (module 10, page 1). *a* is ruled out because the policy has "universal usage standards that apply to everyone". *c* is ruled out because it adds guidelines for "chatbots, products serving minors, agentic use and servers built on the Model Context Protocol (MCP)". *d* is ruled out because "agentic use must still comply with the policy".
55. **d**. Research counts against the same limits as ordinary chat but can use them up faster because it retrieves many sources (module 7, page 2). *a* is ruled out because it uses limits faster "because it retrieves many sources". *b* is ruled out because Research "can use them up faster because it retrieves many sources". *c* is ruled out because "Research counts against the same limits as ordinary chat".
56. **b**. Thinking tokens count toward the context window, and current Opus, Sonnet and Fable models keep earlier thinking by default (module 4, page 2). *a* is ruled out because current Opus, Sonnet and Fable models "keep them by default". *c* is ruled out because "Thinking tokens count toward the context window". *d* is ruled out because "It lives in the context".
57. **a**. "Let Claude work across files" is off by default for Team and Enterprise, on by default for Pro and Max, and set per device (module 8, page 2). *b* is ruled out because a formatting convention "does not carry into Word or PowerPoint". *c* is ruled out because it "cannot open, create or switch files itself". *d* is ruled out because "Cross-app work is not supported through Bedrock, Vertex AI, Azure AI Foundry or an LLM gateway".
58. **c**. Input that alone exceeds the window is refused with a 400 error on every model (module 1, page 1). *a* is ruled out because "Raising it does not enlarge the window". *b* is ruled out because "the API returns a 400 invalid_request_error" when the input alone exceeds the window. *d* is ruled out because "the same text produces about 30 percent more tokens than on earlier models".
59. **b and d**. Thinking tokens are "billed as output tokens" and leave less room for the visible answer inside max_tokens (module 1, page 1). *a* is ruled out because "max_tokens is a cap on output, not on context". *c* is ruled out because "a single response can be up to 128K tokens on the three large models and 64K on Haiku 4.5". *e* is ruled out because the tokens are "billed as output tokens".
60. **c**. The table lists Claude Opus 5.5 at $4 input and $20 output per million tokens, so output costs five times as much and shorter answers save the most (module 3, page 1). *a* is ruled out because the tokens are "billed as output tokens". *b* is ruled out because the lineup lists "Price per million tokens, input / output". *d* is ruled out because "Larger windows raise the ceiling; they do not remove the need to decide what goes in".

</details>
