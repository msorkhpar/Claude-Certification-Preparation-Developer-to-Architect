# Architect scenario question pool

**Level:** Architect · **Module 78:** Exam readiness 3 · **Page 5 of 5**
**Exams:** A1 to A5 (CCAR-F; scenarios S2 to S5, over the content of modules 45 to 77)

**After this page you can** practise the four scenarios that the two Architect mock exams cover only in part, and fill the gaps that those mocks leave in the scenarios S2 to S5.

The real exam draws four scenarios from a bank of six and sets 60 items on them. The two mock exams of this module cover S1 and S6 twice and each of S2 to S5 once, with fifteen questions apiece. This page adds **32 questions** for S2 to S5, eight for each, so that every one of the six scenarios has a pool of at least twenty-three questions, one and a half times the fifteen that a single mock asks. Every question is the course's own, written fresh; no question comes from a live exam, and none repeats a question of a page quiz, a module quiz or a mock exam. The real exam has multiple-choice and multiple-response items; the questions on this page are single-answer items, and the multiple-response items are in the two mock exams.

## How to use it

1. **Draw a form.** For a full rehearsal, take one of the two mock exams of pages 3 and 4. To strengthen one scenario, take the questions of its row below, and add the fifteen of the mock that uses it.
2. **Answer every question,** then read the explanations. The guide describes no penalty for a wrong answer.
3. **Score by scenario and by domain,** using the tables below, and revisit the modules in the last column for a miss.

## What it covers

| Questions | Scenario | Count |
|---|---|---|
| 1 to 8 | S2 code generation with Claude Code | 8 |
| 9 to 16 | S3 multi-agent research system | 8 |
| 17 to 24 | S4 developer productivity | 8 |
| 25 to 32 | S5 Claude Code in CI | 8 |

| Domain | Count | Questions |
|---|---|---|
| A1 Agentic architecture and orchestration | 4 | 9, 14, 19, 30 |
| A2 Tool design and MCP integration | 4 | 10, 17, 23, 24 |
| A3 Claude Code configuration and workflows | 16 | 1, 2, 3, 4, 5, 7, 8, 18, 20, 21, 22, 25, 27, 28, 29, 32 |
| A4 Prompt engineering and structured output | 2 | 6, 26 |
| A5 Context management and reliability | 6 | 11, 12, 13, 15, 16, 31 |

For a miss, open the page that the question's explanation names, as (module, page), and reread the passage it quotes. The modules most often named are S2: 71, 57, 56, S3: 72, 66, 69, 46, S4: 73, 55, S5: 74, 50.

## Mock exam

This question pool covers the scenarios S2 to S5 of the content of Level 3, modules 45 to 77. Choose one answer for each question, or the number the question states.

1. Scenario S2. A team places a CLAUDE.md in the folder of its payments service and expects every session in the repository to carry its rules from the first message. Sessions that never touch that folder do not show them. What explains it?
   - **a**: The file loads only for sessions started by its owner, since it is a personal setting
   - **b**: The file loads at launch but is pushed out first when context fills, since it has the lowest rank
   - **c**: The file loads on demand, since files in subdirectories load only when read
   - **d**: The file loads only when a slash command names it, since commands choose memory

2. Scenario S2. A repository holds a command file at .claude/commands/deploy.md and, after a refactor, also a skill at .claude/skills/deploy/SKILL.md. A developer types /deploy. What runs?
   - **a**: Both, one after the other, since the name maps to each file
   - **b**: The packaged definition, since it wins on a shared name
   - **c**: Neither, since a clash of names disables the command
   - **d**: The command file, since older definitions keep their place

3. Scenario S2. A rule file in a project's rules folder has no paths list. The team assumes that it applies only to the files its title mentions. When does it load?
   - **a**: At the start of every session, since nothing restricts its scope
   - **b**: Never, since an unscoped rule has no target
   - **c**: Only when a developer names it in a command, since unscoped rules wait for a call
   - **d**: Only when a file of its area is opened, since rules load by relevance

4. Scenario S2. A rule file for handlers lists the path pattern src/api/*.ts, and a developer opens src/api/v2/orders.ts. Does the rule load?
   - **a**: No, since rule files read only exact file names
   - **b**: Yes, since the extension alone decides the match
   - **c**: Yes, since a star matches any depth below the folder
   - **d**: No, since a lone star stays within one level

5. Scenario S2. A repository keeps shared guidance in AGENTS.md beside its memory file, and a developer adds a CLAUDE.local.md for their notes. Afterwards the assistant stops following the shared guidance. What explains it?
   - **a**: The personal file overrides the shared guidance line by line
   - **b**: Local files switch off every shared file in the repository
   - **c**: Only one family is read, so its sibling needs importing
   - **d**: AGENTS.md is read only in unattended runs

6. Scenario S2. A team asks for a validation function with a paragraph of description, and the output handles the main scenario but guesses at awkward values. What does the page add to the prompt?
   - **a**: Two or three worked pairs with expected answers, one at a boundary
   - **b**: A request to plan before coding, with no worked pairs shown beside it
   - **c**: A longer paragraph on how careful and exact the code must be
   - **d**: A larger model, to infer the awkward inputs

7. Scenario S2. To avoid match failures, a developer tells Claude Code to rewrite the whole document for a one-line fix in an existing source file. What does the page say?
   - **a**: Keep the whole-file write if the file is under a hundred lines
   - **b**: Use the edit tool, since replacing everything lets damage slip through unseen
   - **c**: Use the search tool, since it can find the line and also replace it
   - **d**: Keep the whole-file write, since it never fails to match

8. Scenario S2. A team writes one skill for each kind of code, expecting each skill to apply by itself whenever a file of its kind is opened. What happens?
   - **a**: Each skill applies when its folder is opened, since folders are the unit of loading
   - **b**: Each skill applies by itself, since skills watch the open files
   - **c**: Only the newest skill applies, since later skills outrank earlier ones
   - **d**: Matching a path never launches one, since scoped rules are what load by location

9. Scenario S3. Before synthesis even starts, the lead of a four-worker research team has no room left because every worker returned everything it fetched. Which change fits?
   - **a**: The coordinator condenses each output in its own context and keeps the summary
   - **b**: The synthesis agent receives every output directly, without the coordinator
   - **c**: Each worker returns its full output split over several shorter messages
   - **d**: Each one saves its results outside and hands back a lightweight reference

10. Scenario S3. The search agent is given a tool that writes notes to a shared drive so that findings are saved, and the report agent is given web search to check a figure. A review applies the least-privilege rule. What does it conclude?
   - **a**: Trim each to its own job, since each holds the other's kind of access
   - **b**: Both are right, since extra tools make the agents more flexible
   - **c**: Only the report agent is wrong, since the search agent must be able to save its findings
   - **d**: Both are right if each tool is logged, since logging controls the risk

11. Scenario S3. A search for one scope fails at first, and a retry through an alternative query later succeeds. The report's list of errors still shows the first failure. What is wrong?
   - **a**: The scope should be marked partial, since an error once occurred there
   - **b**: The retry should have been hidden from the coordinator
   - **c**: It should have been removed, since a newer result covered that topic
   - **d**: Nothing, since the list is a history of every failure that occurred

12. Scenario S3. A tool in a subagent's chain keeps failing, and the subagent simply stops with a generic message. The team wants the system to adapt. Which behaviour does Anthropic's article support?
   - **a**: End the whole run at the first failure so that nothing wrong is reported
   - **b**: Tell the worker about the broken step, so that it adjusts
   - **c**: Return an empty result marked successful so that the run continues
   - **d**: Hide the failure from the agent so that it keeps its plan

13. Scenario S3. A search subagent hits a rate limit twice and succeeds on the third try, and the team's design passes every failure up to the coordinator for a decision. What does the page recommend?
   - **a**: Keep passing everything up to the coordinator, which sees the whole run
   - **b**: Report "search unavailable" and keep the report short
   - **c**: Stop the run and tell the user that it failed outright
   - **d**: Handle passing faults locally, and send up only what stays unresolved

14. Scenario S3. To spare the coordinator's context, a developer lets the search and analysis subagents pass findings directly to each other. What does the page say?
   - **a**: Allow it, since a shorter path saves tokens for the coordinator's own window
   - **b**: Allow only a route through the hub, since that gives one place to observe
   - **c**: Allow it if each pair logs its exchanges to a shared file
   - **d**: Allow it for the analysis agent only, since it reads documents

15. Scenario S3. To keep the writer's input short, the coordinator has the synthesis step compress all findings into prose and tells the writer to add sources afterwards. What does the page say?
   - **a**: It is sound for numbers only, since prose carries words well but not figures
   - **b**: It is sound, since short input is easier for the writer to draft
   - **c**: It is unsound, since claim-origin pairs must travel and sentences cannot restore them
   - **d**: It is sound if the writer searches again for each claim it makes

16. Scenario S3. A report's table gives a figure with no mention of the survey behind it, and a reader later sees it disagree with an audit figure and takes it for an error. What was left out?
   - **a**: The method, since it is part of the finding
   - **b**: The reader's rank, since figures vary by audience
   - **c**: A rounding note, since small differences come from rounding
   - **d**: A longer sentence, since tables lack context by nature

17. Scenario S4. To make a ticket tool safer, a developer writes a settings rule that denies it only when the argument names a closed ticket, using a parenthesised pattern on the MCP tool. What does the rule do?
   - **a**: It denies the whole server, since parentheses widen the pattern
   - **b**: It narrows the tool to closed tickets, as path patterns narrow file tools
   - **c**: It asks the user for each ticket, since a pattern implies a prompt
   - **d**: It matches nothing, since an entry of that form is skipped

18. Scenario S4. A team commits a .mcp.json whose ticket server's location is written with a fallback, and whose token is written with no fallback. A reviewer objects to the first. What is the verdict?
   - **a**: Wrong, since a non-secret entry is suited to a placeholder
   - **b**: Wrong, since expansion does not work in the url field
   - **c**: Right, but the token should have a default as well, for a working clone
   - **d**: Wrong, since a fallback in any field is a credential in the file

19. Scenario S4. An engineer investigated a payments module on Monday. Overnight a colleague rewrote half of it, and on Tuesday the engineer wants to carry on. Which session choice does the page key?
   - **a**: Run two fresh sessions and repeat the whole analysis in each, then merge the two
   - **b**: Begin anew with a short summary, since old results describe files as they were
   - **c**: Resume Monday's session, since the model remembers the analysis
   - **d**: Fork Monday's session, since a branch keeps the analysis and the history

20. Scenario S4. A pull request changes .mcp.json to add a new server, and the reviewer treats it as a configuration tweak. What does the page say?
   - **a**: It is reviewed by the platform team alone, since developers cannot judge servers
   - **b**: It needs no review, since a configuration file cannot run code
   - **c**: It needs scrutiny like code, since entries load unprompted in unattended runs
   - **d**: It needs a review only if the server is remote, since local ones are trusted

21. Scenario S4. A team denies the shell tool for its generator agent and concludes that the agent can no longer change files outside its folder. Is the conclusion sound?
   - **a**: Yes, since a deny rule on one tool covers its neighbours
   - **b**: Yes, since all writes pass through the shell
   - **c**: No, but only because the agent could ask a person to approve
   - **d**: No, since writing to disk has separate rules that must be set too

22. Scenario S4. A developer defines a server named github in their local scope with one address, and the committed file defines the same name with headers and another address. What does Claude Code use?
   - **a**: Only the highest-ranking entry, taken whole, since no merging happens there
   - **b**: A blend, headers from one and address from the other, since merging is the default
   - **c**: The committed file's entry, since a team file always outranks a personal one
   - **d**: Both entries, connected twice, since each source keeps its own server

23. Scenario S4. Every integration in a team's setup is flagged to be present from the opening message of each session, on the theory that tools should always be at hand. What follows?
   - **a**: Costs fall, since the definitions are cached once they have been loaded
   - **b**: Crowded context and worse selection return, since lazy loading is bypassed
   - **c**: Selection improves, since the model sees every definition up front
   - **d**: Nothing, since definitions never count against the window

24. Scenario S4. A team wants an exploring helper that cannot change anything and can also query the company's knowledge server, and an engineer says the built-in read-only explorer already does that. What is missing?
   - **a**: Nothing: the built-in one reaches every connected server
   - **b**: A longer root memory file that names the server
   - **c**: A custom project agent whose tools include the lookup
   - **d**: A prompt line telling the main agent to avoid writing

25. Scenario S5. A headless step fails with a model error, and the job is configured to re-run itself until it passes. What does the page advise?
   - **a**: Extend the job timeout, since a longer wait is more likely to bring success
   - **b**: Turn the job green with an empty result, so that the pipeline moves forward
   - **c**: Report the breakdown and let a person decide, since repeats add expense
   - **d**: Re-run until it passes, since transient faults clear on their own over time

26. Scenario S5. A review prompt defines severity levels in words only, and the team finds that mediums and lows are mixed up. What does the page add to the criteria?
   - **a**: A longer definition for each rating, since more words fix the boundary
   - **b**: A request for fewer comments overall, since fewer means less noise in the output
   - **c**: An instruction to be conservative and report only important issues
   - **d**: A worked example per rating, since it shows where the boundary lies

27. Scenario S5. The pipeline's review step continues the very session that wrote the change, so that the context is already loaded. A reviewer asks why that is a finding. What is the reason?
   - **a**: It lets authors favour their own output, since a clean slate is the sharper check
   - **b**: It cannot run in a pipeline, since sessions are interactive by design
   - **c**: It loses the findings of earlier runs, since they are discarded
   - **d**: It costs more tokens, since the whole history is billed again

28. Scenario S5. A team runs a large review three times and keeps only the issues that two of the three runs report. Why does the page reject this?
   - **a**: It needs a larger window to hold three results at once during the final merge
   - **b**: It would hide real bugs found only occasionally, through the filter
   - **c**: It reports the same issues every time, so the filter changes nothing
   - **d**: It repeats the integration pass, which already compares the results of runs

29. Scenario S5. A script submits the overnight report to the batch interface, and an audit flags that its command lacks the headless flag. Is the finding right?
   - **a**: No, but only because batch jobs use a different flag for headless runs
   - **b**: No, since that rule covers calls starting with the CLI, not a sender
   - **c**: Yes, since every command in a pipeline needs the flag to run unattended
   - **d**: Yes, since batch jobs run through the interactive terminal tool

30. Scenario S5. To keep a multi-file review consistent, each file's pass is shown the findings of the previous files. What does the page say about it?
   - **a**: It makes passes stop being independent, since earlier mistakes become premises
   - **b**: It is wrong only for the last file, since its pass needs all of them
   - **c**: It is sound, since consistency needs shared memory
   - **d**: It is sound if the findings are summarised first, since summaries are neutral

31. Scenario S5. In a review with a pass for each file, one pass fails with an error, and the job script discards everything and starts over. What does the page advise?
   - **a**: Merge the failed file into the next pass
   - **b**: Retry the failed pass until it succeeds, however long
   - **c**: Report the breakage by name and let the other results stand
   - **d**: Discard the whole review and start it again from the first file

32. Scenario S5. An audit of a pipeline flags its test-generation job for holding the edit tool, citing the rule that review jobs must only read. Is the finding right?
   - **a**: Yes, since writing test files is a way to change things in a production system
   - **b**: Yes, since every job in the pipeline must be read-only
   - **c**: No, but only because a shell tool would be worse than the edit tool
   - **d**: No, since that limit binds inspections alone and this task writes by design

<details>
<summary>Answer key</summary>

1. **c**. A CLAUDE.md in a subdirectory is not loaded at launch but when Claude reads files in that directory (module 71, page 1). The page's words are "Files in subdirectories load on demand when Claude reads files in those directories." *a* is ruled out because the scope table gives a directory file to "Whoever works in that subdirectory", and not to its author alone. *b* is ruled out because the page names launch loading for "The root `CLAUDE.md` and the rule files without a `paths` list". *d* is ruled out because the page says a subdirectory file "is not loaded at launch", and nothing in it ties its loading to a command.
2. **b**. Custom commands have been merged into skills, and when a skill and a command file share a name the skill wins (module 71, page 1). Existing command files keep working. The page puts it as "when a skill and a command file share a name the skill wins". *a* is ruled out because one name leads to one definition: "A skill beats a command file of the same name." *c* is ruled out because "the existing command files keep working", and the name resolves to the skill. *d* is ruled out because an older file gets no priority on a clash, and the skill wins: "when a skill and a command file share a name the skill wins"; the older files keep working only where no skill shares the name.
3. **a**. The root file and the rule files without a paths list load at the start of every session at the same priority, so a rule meant for one area then costs context everywhere (module 71, page 1). The page puts it as "The root CLAUDE.md and the rule files without a paths list load at the start of every session, at the same priority". *b* is ruled out because the audit's finding `rule-loads-always` reports "a rule with no `paths` list". *c* is ruled out because "Without frontmatter it behaves like part of the project file and loads at launch.", so no command is needed. *d* is ruled out because the page says a rule with a `paths` list loads "when Claude works with a file that matches one of its globs", and this one has none.
4. **d**. In a paths glob the double star crosses folders and the single star stays inside one, so src/api/*.ts misses a file one level deeper (module 71, page 1). The page's example is src/api/**/*.ts for every TypeScript file below src/api/. The page puts it as "** crosses folders and * stays inside one". *a* is ruled out because a `paths` entry is "A `paths` entry is a glob", and globs match by pattern. *b* is ruled out because the page's example is "`src/api/**/*.ts` reaches every TypeScript file below `src/api/`", which needs the double star. *c* is ruled out because a single star does not cross folders: "`**` crosses folders and `*` stays inside one", and the documentation's table shows "`src/**/*` for all files under `src/`".
5. **c**. The local file counts as a CLAUDE.md, so Claude reads only CLAUDE.md files and AGENTS.md is skipped unless it is imported or the setting is changed (module 57, page 1). The page puts it as "reads only your CLAUDE.md files". *a* is ruled out because "All discovered files are concatenated into context rather than overriding each other". *b* is ruled out because only AGENTS.md is skipped, because "the local file counts as a CLAUDE.md". *d* is ruled out because the file "is skipped unless you import it or change the setting", whatever the kind of run.
6. **a**. Examples of input and output give the work a target: two or three cases with their expected results say what a paragraph of description leaves open, and the page's example is one valid address, one invalid and one edge (module 71, page 2). The page puts it as "Two or three cases with their expected results say what a paragraph of description leaves open". *b* is ruled out because a plan changes the order and not the target: "Plan mode tells Claude to research and propose changes without making them", while "Claude stops when the work looks done". *c* is ruled out because a paragraph is what leaves the cases open: "say what a paragraph of description leaves open". *d* is ruled out because the first recommendation is "Give Claude a check it can run: tests, a build, a screenshot to compare."
7. **b**. Write overwrites with the full content, so an accidental change elsewhere goes unseen, and for partial changes to an existing file the page says to use Edit (module 56, page 1). The page puts it as "Write overwrites with the full content, so an accidental change elsewhere goes unseen". *a* is ruled out because the page gives no size exception: "For partial changes to an existing file, use Edit." *c* is ruled out because the search tool only reads: the table gives `Grep` the job "Searches file contents", and the replacing job to Edit: "Replaces one exact string with another in a file". *d* is ruled out because the page names the cost of a whole-file write: "A wrong tool choice costs tokens and, with Write, risks the rest of the file."
8. **d**. A skill is invoked, or chosen by the model from its description, and a path match does not start it, so conventions that must apply by path go in rule files with a paths list (module 71, page 1). The page puts it as "a skill is invoked, or chosen by the model from its description, and a path match does not start it". *a* is ruled out because the page's rule files, not skills, carry "a `paths` list of globs". *b* is ruled out because "Skills load when they are invoked or chosen, not because a path matched." *c* is ruled out because skills do not replace each other by age: "A project-root skill and a nested one with one name both load".
9. **d**. Large results should not travel through the hub: subagents store their work in external systems and pass lightweight references back (module 72, page 1). The page's table names the failure "Large results passed through the hub". *a* is ruled out because the failure shows as "The coordinator's context fills with the raw output of its subagents", and condensing starts only after that output has arrived. *b* is ruled out because "The coordinator is the only place where the pieces meet." *c* is ruled out because "only its final message returns to the parent", so the output arrives in one message, whatever its size.
10. **a**. The search agent has the web tools and nothing that writes, and the report agent writes the report and calls nothing else, so each tool set is trimmed to the job (module 72, page 1). The least-privilege rule applies to each of the four subagents. The page puts it as "The search agent has the web tools and nothing that writes". *b* is ruled out because "more tools make each choice less reliable". *c* is ruled out because "an agent outside its specialisation misuses what it holds", and a note-writing tool is outside the search agent's job. *d* is ruled out because the least-privilege rule of module 54 "applies to each of the four", and a log acts after the privilege exists.
11. **c**. An error that a later result for the same scope made up for is dropped from the list of errors, since the report should list what is still wrong (module 72, page 2). The page puts it as "the report should list what is still wrong". *a* is ruled out because the status is "complete when every required scope has an `ok` result with at least one finding", and the retry gave this scope one. *b* is ruled out because the coordinator "decides" among alternatives, and it can decide "only if the error says what failed and what could be tried". *d* is ruled out because "an error that a later result for the same scope made up for is dropped from the list of errors".
12. **b**. The article says "Letting the agent know when a tool is failing and letting it adapt works surprisingly well", which is why a failure comes back as a result with its type, its query and alternatives (module 72, page 1). *a* is ruled out because "Send the timeout to a top-level handler that ends the whole run" is rejected, because "the scopes that worked are lost with it". *c* is ruled out because "it turns a failure into a false answer, and nothing downstream can recover from a failure it was never told about". *d* is ruled out because a plan cannot change on an error it never saw: the coordinator "can decide only if the error says what failed and what could be tried".
13. **d**. Transient failures are handled locally, and only what the subagent cannot resolve goes up, with what was attempted and the partial results (module 66, page 1). The page says "transient failures are handled locally". *a* is ruled out because "Pass every failure up; the coordinator decides about retries" is rejected. *b* is ruled out because the coordinator cannot choose a recovery without "the failure type, the query, the partial results and the alternatives". *c* is ruled out because "terminating on a single failure throws away the work of the other subagents".
14. **b**. All communication runs through the coordinator, for observability, one way of handling errors and control over what flows where (module 46, page 1). The page's words are "all communication runs through the coordinator". *a* is ruled out because "Let the subagents pass findings to each other to save the coordinator's context" is rejected. *c* is ruled out because "A subagent does not know the user's original question, the plan, the other subagents", so no pair has an exchange to log. *d* is ruled out because "The coordinator is the only place where the pieces meet."
15. **c**. The sources are lost in the compression and cannot be rebuilt from prose, so the claim-source mappings travel through every step (module 69, page 1). The page puts it as "the claim-source mappings travel through every step". *a* is ruled out because the loss is not limited to figures: "It is lost the moment a step turns findings into prose". *b* is ruled out because "Summarise the findings first; the writer will add sources later" is rejected. *d* is ruled out because "the sources are lost in the compression and cannot be rebuilt from prose".
16. **a**. The method is part of the finding, and without it a disagreement between a survey and an audit looks like an error (module 69, page 2). The page puts it as "the method is part of the finding". *b* is ruled out because the omission is a field of the finding and not a fact of the audience: "These are fields of the finding like the source and the date". *c* is ruled out because "A figure from a survey and a figure from an audit are not interchangeable", which is a difference of method and not of rounding. *d* is ruled out because the table is not the fault: "Financial and other numeric data: a table."
17. **d**. In a settings file a rule for an MCP tool that carries parentheses is skipped and matches nothing, so an argument pattern is not a way to narrow an MCP tool there (module 73, page 1). Allow or deny the tools by name. The page puts it as "In a settings file, a rule for an MCP tool that carries parentheses is skipped, so it matches nothing". *a* is ruled out because the whole-server form carries no parentheses: "A rule can name a whole server". *b* is ruled out because the page narrows a server by name: "allow the reading tools by name and deny the others by name". *c* is ruled out because "In a settings file, a deny rule with parentheses on an MCP tool is skipped", so it never reaches the point of asking.
18. **a**. The expression with a fallback expands to the variable if it is set and to the default otherwise, and a default is right for a value that is not secret and wrong for a token (module 73, page 1). The page puts it as "A default is right for a value that is not secret, such as the ticket server's address, and wrong for a token". *b* is ruled out because expansion "works in a server's `command`, `args`, `env`, `url` and `headers`". *c* is ruled out because "a default for a token would be a credential in the file". *d* is ruled out because the address is meant to have one: "The ticket address has a default and is optional."
19. **b**. Tool results in an old session describe files as they were, so after an overnight refactor the page keys a new session with a short structured summary and a fresh read of the files that matter (module 73, page 2). The page puts it as "Tool results in an old session describe files as they were". *a* is ruled out because the choice is about "what the old context is worth", and repeating everything wastes the findings that still hold. *c* is ruled out because "Resuming gives confident answers about code that is gone." *d* is ruled out because a fork is for "several directions" that start "from one shared analysis", not for code that changed.
20. **c**. The project's servers load without a prompt in claude -p and SDK runs, so a pull request that changes .mcp.json deserves the review of a change to code (module 73, page 1). The page puts it as "The project's servers load without a prompt in claude -p and SDK runs". *a* is ruled out because the sentence says "a pull request that changes `.mcp.json` deserves the review of a change to code". *b* is ruled out because "a server is code with access to your systems and a way into the model's context". *d* is ruled out because the page trusts no origin by default: "A server that came with a repository is code from somebody else."
21. **d**. Denying the shell does not limit the file tools, which have their own rules, so the generator's writes must be limited by an Edit rule for its folder (module 73, page 1). The page puts it as "Denying the shell does not limit the file tools, which have their own rules". *a* is ruled out because "For files, an `Edit` rule applies to every built-in tool that edits files". *b* is ruled out because "file permissions are checked against Edit and Read path rules only", and not against the shell. *c* is ruled out because the rules are checked in the order "deny, then ask, then allow", and the file tools hold rules of their own.
22. **a**. The same name is connected once, using the entire entry from the highest-precedence source, so "The entire server entry from that source is used" (module 55, page 1). *b* is ruled out because "fields are not merged across scopes". *c* is ruled out because the order is "local, project, user, plugin-provided servers, then claude.ai connectors", so the local definition ranks above the committed one. *d* is ruled out because "Claude Code connects to it once, using the definition from the highest-precedence source".
23. **b**. Loading every definition at the start brings back what tool search was avoiding: crowded context and worse selection (module 55, page 2). The page puts it as "Loading all of them at the start brings back what tool search was avoiding". *a* is ruled out because "each upfront tool consumes context that would otherwise be available for your conversation". *c* is ruled out because "Tool selection accuracy degrades with more than 30-50 tools loaded at once". *d* is ruled out because "the list costs context on every turn, since the definitions are sent each time".
24. **c**. Claude Code ships a read-only explorer itself, and a project subagent of your own can have the same read-only shape and be given the project's documentation server as well, with a tools line that lists only reading and searching tools (module 73, page 1). The page puts it as "a project subagent of your own can have the same read-only shape and be given the project's documentation server as well". *a* is ruled out because the built-in helper is "a read-only agent for file discovery, code search and codebase exploration", with no word of the team's own servers. *b* is ruled out because "the root file loads in every session and costs context each time". *d* is ruled out because "A sentence in the prompt is a request that the model weighs; the `tools` line is a list the subagent cannot go beyond."
25. **c**. When a run fails, re-running it repeats the cost with no reason to expect a different result, so the job reports the failure and a person decides (module 74, page 1). The page puts it as "When a run fails, re-running it repeats the cost with no reason to expect a different result". *a* is ruled out because "A longer job timeout only lets a loop run longer." *b* is ruled out because "A green job that said nothing is the failure the gate prevents". *d* is ruled out because a headless run needs an end and not an open-ended repeat: "A run with no person must end by itself", and a repeat brings "no reason to expect a different result".
26. **d**. Criteria have three lists, and each severity carries one example, because the example shows the model where the line between medium and low is, which a definition alone does not (module 74, page 2). The page puts it as "the example shows the model where the line between medium and low is, which a definition alone does not". *a* is ruled out because longer wording changes nothing: "What changes it is naming the cases." *b* is ruled out because "A sentence that asks for more care is a request; the criteria are the case list." *c* is ruled out because the vague phrases "do not say what to leave out", so they change nothing.
27. **a**. A reviewer should not be the author: a fresh context improves code review since Claude will not be biased toward code it just wrote, so the review runs as its own session (module 74, page 2). The page puts it as "A fresh context improves code review since Claude won't be biased toward code it just wrote". *b* is ruled out because the page asks that "In a pipeline that means the review runs as its own session", and does not say that sessions are interactive only; the pipeline description sets "`session` is `fresh`". *c* is ruled out because the earlier findings are a separate need that the run is given on top of a fresh session: "The second thing a review needs is memory of its own earlier output" and "`context` includes `prior_findings`". *d* is ruled out because the page gives authorship and not billing as the problem: "A reviewer should not be the author."
28. **b**. Keeping only what two of three runs agree on would hide real bugs that are found only now and then (module 74, page 2). The remedy that fits the cause is to divide the work into a pass for each file and an integration pass. The page puts it as "would hide real bugs that are found only now and then". *a* is ruled out because "A larger context window does not fix attention quality." *c* is ruled out because runs differ, which is why the filter drops some bugs: "the same pattern may be flagged in one file and approved in another" and "real bugs that are found only now and then". *d* is ruled out because the integration pass is about files and not about runs: "A separate integration pass then looks at what crosses files".
29. **b**. A batch job's command is a script that submits the work, so the -p rule applies to commands that start with claude and not to a script (module 74, page 1). The page puts it as "a batch job's command is a script that submits the work". *a* is ruled out because "There is no headless environment variable and no `--batch` flag on the command line". *c* is ruled out because the audit reports `no-print-flag` for "a command that starts `claude` without `-p` or `--print`", and a submitting script is no such command. *d* is ruled out because the batch is reached by "The Message Batches API, from a script".
30. **a**. Seeing earlier findings makes the passes dependent, so earlier mistakes become premises, and consistency is the job of the cross pass (module 50, page 2). The page says "the passes become dependent and the earlier mistakes become premises". *b* is ruled out because "Consistency is the job of the cross pass". *c* is ruled out because "Let the next file pass see the findings of the previous ones, for consistency" is rejected. *d* is ruled out because a file pass "does not need to know how many other files there are or what they found", in any form.
31. **c**. The failure is reported by file, and the rest goes on, since the other passes are valid (module 50, page 2). The page puts it as "The failure is reported by file, and the rest goes on". *a* is ruled out because the passes stay independent, since otherwise "the passes become dependent". *b* is ruled out because "the subagent retries a bounded number of times", and not until it succeeds. *d* is ruled out because "If one file pass fails, discard the review and start again" is rejected because "the other passes are valid".
32. **d**. A review changes nothing, so its tools are Read, Grep and Glob, while test generation writes test files and is not held to the read-only rule (module 74, page 2). The audit checks the rule only for reviews. The page puts it as "The audit checks the rule only for reviews". *a* is ruled out because "The test-generation job is different on purpose: it writes test files, so it has `Edit`". *b* is ruled out because the test-generation job "is not held to the read-only rule". *c* is ruled out because the review "changes nothing, so its tools are `Read`, `Grep` and `Glob`".

</details>
