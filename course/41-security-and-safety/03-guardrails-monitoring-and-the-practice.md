# Layered guardrails, monitoring and the practice

**Level:** Developer · **Module 41:** Security and safety · **Page 3 of 3**
**Exams:** DV6; A2.3

**After this page you can** layer input guardrails, structural controls and output guardrails around an agent, reduce prompt leaks without hurting the task, record identity and decisions so that abuse is noticed, say what Claude Code's telemetry and `ConfigChange` hook add, and finish the module's practice, an injection-resistant tool gate.

Checked on 2026-10-03 against the Claude API documentation pages "Mitigate jailbreaks and prompt injections" and "Reduce prompt leak", the Claude Code security, hooks guide and monitoring pages. Nothing here was run against a live service. The practice runs offline in Python, TypeScript, Java and Kotlin.

## Why it matters

No single control stops an attack, so the exam asks how controls combine and where each sits in the path of a request. It also asks the operational questions: what you log, who gets alerted and what you do about a user who keeps trying. A design that has only a prevention layer learns nothing from an attack that gets through.

## The idea

### Layers around the model

The documentation ends its list with a section on chaining: "Combine strategies for robust protection." Read the path of one request and put a control at each step.

| Layer | Control | Source of the idea |
|---|---|---|
| Input | A harmlessness screen on user text; validation of known patterns | The user is the adversary |
| Structure | Untrusted content only in tool results, JSON-encoded, with its source named and the policy in the system prompt | Page 1 |
| Privilege | Narrow tools, data and actions; a stricter gate after untrusted text was read | Page 2 |
| Tool output | A small-model screen on what a tool returns, before Claude acts on it | The documentation's screen pattern |
| Action | A hook or gate that decides each call, failing closed | Page 2 |
| Output | Redaction of keys, addresses and card numbers; keyword filtering for leaks | Prompt-leak page |
| Record | An audit of every decision, and alerts on repeated denials | This page |

The screen of tool output works like this: "Run each tool, pass its raw output to a small classifier call with Claude Haiku 4.5, and only return the content as a `tool_result` block if the screen reports no injection attempt." If the verdict is positive, "return an error or a stripped summary in the `tool_result` block instead of the raw content, and consider surfacing the attempt to the user." A structured-output schema makes the verdict a value your code can branch on. The pattern screen of page 1 is the cheap first pass, and the classifier is the second.

Two cautions. First, each layer adds cost and latency, and a false positive blocks real work, so measure both. Second, the documentation advises to "Red-team your own agent": test with documents, emails and tool outputs that deliberately contain injection attempts, and confirm that "Claude ignores them and that your screening and confirmation steps catch the rest." A control you have not attacked is a control you have only hoped for.

### Reducing prompt leaks

A prompt leak exposes what you expected to keep hidden in the prompt. The documentation is modest about it: "While no method is foolproof, the strategies below can significantly reduce the risk", and it warns that leak-proofing "can add complexity that may degrade performance". So the first advice is to try monitoring before prompt engineering: "output screening and post-processing, to try to catch instances of prompt leak." The strategies are to separate context from queries, to filter Claude's outputs with regular expressions, keyword filtering or a prompted model, to avoid unnecessary proprietary details ("If Claude doesn't need it to perform the task, don't include it"), and to audit regularly. The cheapest to apply is the third, because it adds no complexity. A secret that is not in the prompt cannot leak from it, which is also the logic of least privilege.

### Monitoring: identity and access

A control that blocks should also record. For every gate decision keep the actor (a user, a session or an agent), the tool, the decision, the reason and the arguments with secrets redacted, because a log that holds a raw key has become a leak of its own. From the record you can answer the questions that matter: who tried what, which denials repeat, which sessions were tainted. The documentation names the response to repeat offenders: "Adjust responses and consider throttling or banning users who repeatedly attempt to circumvent your application's guardrails." That needs a threshold. The practice uses three denials by one actor, which raises an alert, and treats a question to a person (`ask`) as a normal event and not a denial.

Claude Code adds its own monitoring for teams. "Track Claude Code usage, costs, and tool activity across your organization by exporting telemetry data through OpenTelemetry (OTel)." Metrics are time series, and events carry tool activity. A `ConfigChange` hook fires when a settings file changes, with matchers for the user, project, local and policy settings, and "To block a change from taking effect, exit with code 2". Together with managed settings, it lets an organisation notice or refuse a change to the permission rules, which is access monitoring applied to the configuration itself.

### The practice

The practice is the code layer, an injection-resistant tool gate. It is a design the course proposes from the documented advice, and not a product feature. You write, in your language of choice (Python, TypeScript, Java or Kotlin):

- `screen`, which names the injection signals in a text, in a fixed order, ignoring letter case;
- `wrap_untrusted`, which returns a tool result holding one line of compact JSON with the source and `"trust": "untrusted"`, or an error that names the signals and shows none of the text;
- `redact`, which replaces keys, addresses and Luhn-valid card numbers with fixed markers;
- a `Gate`, which decides `read_file`, `write_file`, `bash`, `fetch` and `send_email` calls as `allow`, `ask` or `deny` with a fixed reason. It keeps paths inside the project, refuses secret files and protected folders, limits `bash` to read-only commands and refuses chaining, checks hosts and recipient domains exactly, and gets stricter once `mark_untrusted` has been called;
- an audit with redacted arguments, `alerts` for actors with three denials, and `hook_response`, which shapes a decision as a `PreToolUse` hook answer.

The Java and Kotlin folders give you a small `Json` helper, because those two have no JSON library in the course's offline image. The statement lists every rule and every reason string. The starter fails every test, the reference passes, and each planted wrong solution fails on an assertion.

## Traps

1. **Relying on one layer.** A screen alone misses paraphrases, a prompt alone is not a grant, and a sandbox alone does not cover file tools.
2. **Logging raw arguments.** A key in an audit line is a leak. Redact before writing.
3. **Failing open.** A gate that allows what it cannot parse has an attacker's favourite hole.
4. **Counting questions as attacks.** An `ask` is the gate working, and alerts on it bury the real ones.

## Quiz

1. A team wants to notice a user who keeps trying to defeat the rules of its application. What does the documentation suggest?
   - **a**: Adjust its responses to the offender, and consider throttling or a ban
   - **b**: Tighten the system prompt, so that it refuses harder next time
   - **c**: Keep no logs of refused requests, so that no attack leaves a record
   - **d**: Raise the request limit of that person, so the rules are tested less often

2. A team wants to cut the risk of a prompt leak without hurting the work. Which step adds no complexity?
   - **a**: Remove proprietary details that are not needed for the job
   - **b**: Add many rules that forbid repeating text from the instructions
   - **c**: Rewrite the prompt so that instructions are stated twice over
   - **d**: Stop monitoring altogether, because screening slows down replies

3. A gate writes each decision to an audit with the call's inputs, one of which is an API key. What should it do?
   - **a**: Skip the audit entirely for calls that carry a secret of any kind
   - **b**: Store the raw arguments, because the audit needs every detail of the call
   - **c**: Replace the secret with a fixed marker before the record is saved
   - **d**: Encrypt the record and keep the key in the same file as the record

<details>
<summary>Answer key</summary>

1. **a**. The page quotes the documentation: "Adjust responses and consider throttling or banning users who repeatedly attempt to circumvent your application's guardrails", and says that needs a record and a threshold. *b* is ruled out because the page says leak-proofing and extra prompt text "can add complexity that may degrade performance", and a longer prompt records nothing. *c* is ruled out because "A control that blocks should also record", and without a record you cannot answer "who tried what". *d* is ruled out because the page says to "consider throttling or banning users who repeatedly attempt to circumvent your application's guardrails", and a higher limit gives them room instead.
2. **a**. The page says "If Claude doesn't need it to perform the task, don't include it", and that a secret not in the prompt "cannot leak from it". *b* is ruled out because leak-proofing "can add complexity that may degrade performance". *c* is ruled out because the page lists no such step and warns against added complexity: "While no method is foolproof, the strategies below can significantly reduce the risk". *d* is ruled out because the page says to try monitoring first: "output screening and post-processing, to try to catch instances of prompt leak."
3. **c**. The page says to record "the arguments with secrets redacted, because a log that holds a raw key has become a leak of its own". *b* is ruled out because the audit keeps "the arguments with secrets redacted" and not the raw ones. *a* is ruled out because the audit exists to answer "who tried what, which denials repeat, which sessions were tainted", and a call that carries a secret is one worth recording. *d* is ruled out because a key kept beside the record is still in the log, and "a log that holds a raw key has become a leak of its own", whatever the file is encrypted with.

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A mail assistant reads inbound messages. Which combination gives the best protection against a hostile message?
   - **a**: A longer system prompt, with a firm tone in every instruction given
   - **b**: Encoded results, narrow tools and a gate that tightens once outside text is present
   - **c**: A pattern list on its own, run before the model reads each inbound message
   - **d**: A larger context window, so that the model can weigh each message better

2. After an agent has read an untrusted web page, which request should be refused or escalated?
   - **a**: Listing a directory, which is a read-only command
   - **b**: Reading a file inside the project folder, which changes nothing
   - **c**: Sending mail, which is left for a person to do
   - **d**: Fetching a plain address with no query string and no fragment

3. A team builds a hook to stop recursive forced deletes. Which design choice should the hook follow?
   - **a**: Block every shell command, whatever its purpose or its origin
   - **b**: Match the text `rm -rf` and allow whatever it cannot parse at all
   - **c**: Match the text `rm -rf` and rely on the model's own caution alone
   - **d**: Parse the command into parts, see through wrappers and block what it cannot read

4. A team's key is stored in the environment for a CI job. Which practice fits both CI and an agent in production?
   - **a**: Write the value into the workflow file, so every runner has it
   - **b**: Keep the value outside the model's reach, and redact it in logs
   - **c**: Put the value in the memory file, so Claude never forgets it
   - **d**: Print the value in the first line of the audit for later checks

<details>
<summary>Answer key</summary>

1. **b**. The layers table puts structure (JSON encoding), privilege (narrow tools, "a stricter gate after untrusted text was read") and a decision gate in the path of one request. *a* is ruled out because "Whatever the injected text asks for, the agent can do only what its grants allow", and tone adds no grant. *c* is ruled out because a pattern list "catches the phrasing someone thought of and misses the paraphrase". *d* is ruled out because a larger window changes nothing about who wrote the text, and the page's structure rule is to "Put untrusted content only in tool results".
2. **c**. The page says that in a tainted session "Sending mail is refused until a person does it". *b* is ruled out because "Reads stay allowed, since they change nothing". *a* is ruled out because "Reads stay allowed, since they change nothing", and a read-only command is a read. *d* is ruled out because only a network call "that carries data in its URL, a query string or a fragment asks", so a plain address stays allowed.
3. **d**. The page says a hook can "parse the command into its parts", should "fail closed", and that a command "that cannot be parsed, or an event the hook cannot read, is blocked and not waved through". *b* is ruled out because "A guard that exits 0 on a parse error is a guard an attacker can walk through". *c* is ruled out because "A memory line asks, and a hook enforces", and the model's caution is not a control. *a* is ruled out because the page asks for a guard on the destructive case and no opinion on the safe case: "it should answer the safe case with no opinion".
4. **b**. The page says "a secret never enters anything the model can read or a file anyone can commit", and "In logs and results, redact before writing." *a* is ruled out because "Never commit API keys or OAuth tokens directly to your repository." *c* is ruled out because a memory file is read by the model and committed, and the rule is that a secret "never enters anything the model can read". *d* is ruled out because "A key that appears in a tool result or an audit line has left your control."

</details>
