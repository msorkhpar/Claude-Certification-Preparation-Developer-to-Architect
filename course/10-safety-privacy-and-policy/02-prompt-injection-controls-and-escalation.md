# Prompt injection, organisational controls and escalation

**Level:** Foundations · **Module 10:** Safety, privacy and policy · **Page 2 of 2**
**Exams:** AS6 (and DV6, AS4)

**After this page you can** explain prompt injection to a colleague and name the habits that limit it, list the controls
an organisation can set across Claude's apps, and say where to take a policy conflict and what to do while you wait.

Checked against the Claude help centre pages on safe use of Cowork and Claude in Chrome, the connector, skills and
Team plan pages, the Claude for Excel and Microsoft 365 documentation, the Claude Tag setup page, and the Claude API
documentation on mitigating jailbreaks and prompt injections, all read on 2026-10-02, and against the Associate exam
guide (domain 6, version 1.0, July 2026). The developer side of injection is the subject of module 41; this page is the
user's side. Settings and plan limits change: the official page decides when it differs.

## Why it matters

The Associate exam's governance domain includes following organisational policies and escalating what you cannot or
should not decide yourself. The Developer exam's security domain begins with the same hazard seen from the
builder's side. Claude's apps now read your mail, your files and the web and can act on them, which turns an old
risk (a person tricked by a message) into a new one (a tool tricked by a message). You need to recognise the pattern,
know the controls and know who to tell.

## The idea

### Prompt injection, explained for users

Claude follows instructions written in language. It cannot always tell an instruction from you apart from an instruction
hidden in content it was only meant to read. **Prompt injection** is an attack that plants instructions in content
Claude will read, such as a web page, an email, a document, a spreadsheet or a connector's reply, hoping it will obey them
as if you had written them. The help centre's example: a seemingly harmless to-do list or email can contain invisible
text instructing Claude to retrieve your bank statements. A related attack, a **jailbreak**, is a user's own effort to
get a model to ignore its safeguards; the Usage Policy prohibits deliberately bypassing guardrails.

**The condition for harm.** The Cowork safety page puts it precisely: the risk exists when Claude can read untrusted
content **and** take consequential actions. Take either away and the danger drops. A chat that can only read has
nothing to do with an injected command; a tool that acts only on content you wrote has nothing to be tricked by. Real
products combine both, so the controls aim to separate them.

**Why it is not solved.** Anthropic's pages describe layers: training the model to resist, classifiers that screen
content and actions, permissions, blocklists and confirmations for high-risk steps. Even so, the Chrome page states
that the risk is not zero and the Excel page tells you to use the add-in only with trusted spreadsheets, because
files from outside can carry hidden instructions. The honest summary for a colleague is "safer than before, not safe
enough to stop paying attention".

**What a user does:**

1. **Limit what Claude reads to sources you trust.** Be wary of unfamiliar sites, forwarded files, downloaded
   templates and content from unknown people, and open them in a separate profile or a trusted copy.
2. **Limit what Claude can do.** Connect only what the task needs; give a dedicated folder, not the drive; keep sensitive
   accounts out of the browser Claude uses.
3. **Keep a person on consequential steps.** Use Manual approval for sending, deleting, buying and posting, and read each
   confirmation before agreeing. Do not press Allow out of habit.
4. **Watch for strange behaviour.** Is it touching files or sites you did not mention? Stop the task.
5. **Install only trusted skills, plugins and connectors.** The skills page names prompt injection and data
   exfiltration as the most significant risks; a remote tool can change after you approve it.
6. **Report it.** An odd instruction found in a document, or a task that did something unexpected, goes to your
   security or IT contact, because the same file may reach others.

**The same idea for a builder, in one line** (module 41): third-party content is delivered as data, never in the system
prompt, the model is told that such content must not override the user's request, and the agent gets the least
privilege that lets it work, so a successful injection can do little.

### Organisational controls at a glance

On the Team and Enterprise plans, the people who run the organisation hold settings that individuals cannot override. An
Associate need not configure them but should know they exist and which one answers which concern.

| Concern | Control the pages describe |
|---|---|
| Who can use a feature | Owners enable connectors, memory, skills and other features for the organisation; members cannot use what is off |
| What a connector may do | Per tool: always allowed, needs approval or blocked; action limits work alongside the source system's permissions |
| What is shared inside | Project sharing and organisation-wide sharing can be limited; skills can be provisioned to everyone or shared with named people |
| Untrusted skills and plugins | Scanning of uploaded skills and plugins for malicious content (Enterprise) |
| Where the web can reach | Web search can be turned off for chat and Cowork; Claude in Chrome sites can be allow-listed or blocklisted |
| Who is who | Single sign-on, automatic provisioning, role-based access |
| What is recorded | Audit logs, a Compliance API, and streaming of Cowork events to the organisation's monitoring tools |
| How long data is kept | Retention settings, including custom retention or zero data retention for Enterprise agreements |
| Cost | Spend controls at the organisation and per person; a monthly spend limit for Claude Tag |
| Where Claude acts in a chat tool | Claude Tag access limited to chosen channels and people, with its own accounts in other tools, each with the narrowest role that covers the work, so its actions are traceable and its access can be cut off without touching anyone else's |

Two cautions. First, controls interact with features: a few features are unavailable under strict data agreements
(Claude Tag where zero data retention or customer-managed encryption applies, memory under HIPAA or custom retention
agreements, Claude in Chrome for HIPAA organisations, and Cowork, which the HIPAA-ready plans page says is not yet covered under Anthropic's business associate agreement), so a strict setting can switch a feature off. Second, coverage is
not uniform: the Microsoft 365 add-ins are outside the Enterprise audit logs and do not inherit custom retention
settings, so "we log everything" needs checking product by product.

### Where to escalate a policy conflict

A policy conflict is a moment when what you are asked or want to do clashes with a policy, a law or a limit of the
tool: a manager asks you to upload customer records to speed a report; a decision about a person would go out with
no qualified reviewer; two policies disagree; you cannot tell whether a use is allowed.

1. **Stop at the edge.** Do not proceed on the hope it is probably fine, and do not work around the rule (a personal
   account, a different tool, a rewording that hides the data). Missing or unclear policy is not permission.
2. **Read the written policy.** Find the actual text on data classes, approved tools and AI use, and note the clause.
3. **Name the conflict plainly and take it to the right person.** For a data question: your manager and the organisation's
   data-protection or privacy owner. For a policy that seems to forbid a legitimate task: the policy owner, who can
   grant an exception or change the rule. For a suspected leak or injection: your security or IT contact straight away.
   For a technical limit beyond your role: the architects and developers, which the Associate credential itself names
   as the route for complex or technical work. For a tool setting you cannot change: the Claude administrator.
4. **Offer a compliant alternative while you wait.** Anonymised data, a smaller scope or a manual step often lets
   the work continue.
5. **Write down what you asked and what was decided.** A decision you can show protects you and the next person.

The exam pattern: the best answer **raises the conflict with the person who owns the policy and proposes a compliant
path**, and the wrong answers either comply silently with something unsafe, refuse everything, or quietly find a way around.

## Traps

1. **Treating an assistant's confirmation prompt as a formality.** The prompt is the control; clicking Allow without
   reading it hands your approval to whatever was injected.
2. **Assuming a strong setting covers every product.** Controls differ by product; the add-ins sit outside the audit
   logs, and some features switch off under strict data agreements.
3. **Escalating late, or not at all.** A leak or a suspicious file is reported quickly; waiting lets it spread.

## Quiz

1. A company lets Claude read incoming support emails and also move money to buyers, both unattended. An email
   carries hidden text telling it to pay a stranger. Which change stops the attack most reliably while the support workflow keeps running?
   - **a**: Tell Claude in standing instructions to ignore emails that change its task
   - **b**: Have a staff member review and approve each transfer before it goes out
   - **c**: Add a home-built screen that scores each email before Claude reads it
   - **d**: Switch to a stronger model trained to resist hidden text in emails

2. A security team must show which actions the company's Slack agent took in its ticket tool, under a name that is
   not a person's, and must be able to shut the agent out while every staff account stays usable. Which setup meets
   both?
   - **a**: Let it act in the ticket tool under the login of whoever tagged it
   - **b**: Limit where it answers in Slack to chosen channels and people
   - **c**: Give it a separate login, with rights limited to what its work needs
   - **d**: Rely on the Enterprise audit logs for every Claude product

3. A manager asks an analyst to put a full customer export into Claude to get a report by tonight. Policy forbids regulated
   personal data in the tool. What is the best step?
   - **a**: Upload the export anyway, since the manager approved the request verbally
   - **b**: Offer an aggregated version and take the clash to the rule's owner
   - **c**: Decline the report and tell the manager the policy forbids it
   - **d**: Paste the export into a personal account that has no organisational policy attached

<details>
<summary>Answer key</summary>

1. **b**. The risk "exists when Claude can read untrusted content and take consequential actions", and a person on
   the consequential step removes the second condition while the first stays. *a* is ruled out because harm needs reading and acting together, "Take either away and the danger drops", and a standing instruction takes neither away. *d* is ruled out because the page's summary is "safer than before, not safe enough to stop paying attention", and a stronger model leaves reading and acting in place. *c*
   is ruled out because a screen leaves Claude both reading the mail and moving money, while "the controls aim to separate them" and the habit is to "Keep a person on consequential steps".
2. **c**. Claude Tag acts through its own accounts, each with the narrowest role, so its actions are traceable and its access can be cut off
   separately (the controls table). *a* is ruled out because the agent's "access can be cut off without touching
   anyone else's" only when it does not borrow someone's login. *b* is ruled out because "access limited to chosen channels and people" governs where it
   acts in the chat tool, while its traceable name in the ticket tool comes from "its own accounts in other tools". *d* is ruled out because
   "coverage is not uniform: the Microsoft 365 add-ins are outside the Enterprise audit logs", and a log cannot shut the agent out.
3. **b**. The best answer "raises the conflict with the person who owns the policy and proposes a compliant path". *a* is ruled out because "Missing or unclear policy is not permission", and a verbal approval is not an exception from the policy owner. *c* is ruled out because a refusal alone skips the step "Offer a compliant alternative while you wait", and the wrong answers include those that "refuse everything, or quietly find a way around". *d* is ruled out because "do not work around the rule (a personal account, a different tool, a rewording that hides the data)".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A staffing firm lets Claude sort CVs into two piles for hiring managers, and also deploys it as the public face
   of its applicant helpdesk, answering applicants directly. Which pair of duties does the Usage Policy impose?
   - **a**: A qualified reviewer for the piles, and an AI notice only if an applicant asks
   - **b**: An AI notice on each pile, and a qualified reviewer for helpdesk answers only
   - **c**: Removal of candidates' names from the piles, and a yearly audit of helpdesk answers
   - **d**: A qualified reviewer for each result, and an AI disclosure when each chat begins

2. A clinic's analyst wants Claude's help with a sheet of patient names and diagnoses, and the clinic is under a
   strict data contract. Which opening step fits?
   - **a**: Strip identifiers first and use only features its agreements allow
   - **b**: Upload it to a consumer account where chats are not used for training
   - **c**: Replace names with codes, keep the key in the sheet, and upload it
   - **d**: Ask an owner to unlock the blocked features, then upload the sheet whole

3. An analyst discovers that a shared document contains a hidden line aimed at AI tools, telling them to send a summary to
   an outside address. What should they do?
   - **a**: Report it to security and keep the file from assistants
   - **b**: Delete the line from the file and carry on working as before
   - **c**: Open the file in an assistant to see whether the line really works
   - **d**: Ask the author if the line is theirs, and wait for a reply

4. A hospital group on a HIPAA-ready Enterprise plan asks whether staff may turn on memory, and also use Cowork on a
   shared folder of patient letters. Which answer fits the module?
   - **a**: Enable both for an owner-approved pilot group
   - **b**: Leave memory off and give Cowork a dedicated folder
   - **c**: Enable memory only for staff without patient letters
   - **d**: Keep both switched off across the organisation

<details>
<summary>Answer key</summary>

1. **d**. Employment decisions are high-risk, so a qualified professional reviews them, and a consumer-facing
   chatbot must disclose that it is AI at the beginning of each chat session. *a* is ruled out because a consumer-facing chatbot discloses "at a minimum at the beginning of each chat session", not only when asked. *b* is ruled out because
   a "qualified professional in the field must review the content or decision", so the CV sorting needs the
   reviewer, not the helpdesk alone. *c* is ruled out because the review comes "before it is disseminated or finalised", not as a yearly
   audit afterwards.
2. **a**. Regulated data stays out of a tool unless the contract allows it, so identifiers go first, and strict
   agreements switch some features off. *d* is ruled out because "a strict setting can switch a feature off", so an
   owner cannot enable what the agreement removes. *b* is ruled out because regulated data goes "Not in a tool
   unless policy and the contract explicitly allow it; otherwise anonymise first", whatever the plan's training
   terms. *c* is ruled out because "Pseudonymised data is still personal data under many rules, because the key can
   reverse it".
3. **a**. A suspicious file is reported to the security or IT contact straight away, because the same file may reach others. *b* is ruled out because an odd instruction found in a document "goes to your security or IT contact", and deleting the line skips that report while "the same file may reach others". *c* is ruled out because Claude's apps "read your mail, your files and the web and can act on them", so opening the file in one hands the planted instruction to a tool that can carry it out. *d* is ruled out because "A leak or a suspicious file is reported quickly; waiting lets it spread", and the route is the security or IT contact, not the author.
4. **d**. Memory is unavailable under HIPAA agreements and Cowork is not yet covered by the business associate
   agreement, so both stay off, and anything further goes to the person who owns the policy. *a* is ruled out because "a strict setting can
   switch a feature off", so an owner cannot enable what the agreement removes. *b* is ruled out because Cowork is
   "not yet covered under Anthropic's business associate agreement", and a dedicated folder limits reach, not
   coverage. *c* is ruled out because "the memory feature is not available to organisations with HIPAA, public-sector or custom retention agreements", which applies to the organisation, not to chosen staff.

</details>
