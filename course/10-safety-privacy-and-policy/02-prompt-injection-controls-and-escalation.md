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
| Where Claude acts in a chat tool | Claude Tag access limited to chosen channels and people, with its own accounts in other tools so its actions are traceable |

Two cautions. First, controls interact with features: a few features are unavailable under strict data agreements
(Claude Tag where zero data retention or customer-managed encryption applies, memory under HIPAA or custom retention
agreements, Claude in Chrome for HIPAA organisations), so a strict setting can switch a feature off. Second, coverage is
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

1. A user asks Claude in Chrome to compare supplier pages. One page carries hidden text telling the assistant to open the
   user's webmail and forward invoices. Which pair of conditions made this attack possible?
   - **a**: The user ran the extension on a work laptop, and on a fast home connection
   - **b**: Untrusted content could be read, and consequential actions could be taken
   - **c**: The page used large images, and the browser was not fully up to date
   - **d**: The supplier was unknown, and the user had saved no earlier chats

2. An Enterprise administrator wants Claude to view requests in a helpdesk connector, yet insist on a human click before
   any request is resolved. Which control matches?
   - **a**: A monthly spend limit applied to the whole workspace, set by an owner
   - **b**: A switch that turns web search off for chat and Cowork across the organisation
   - **c**: Scanning of uploaded skills and plugins for malicious content before they are used
   - **d**: A per-tool permission, allowed for looking up and needing approval for changing

3. A manager asks an analyst to put a full customer export into Claude to get a report by tonight. Policy forbids regulated
   personal data in the tool. What is the best step?
   - **a**: Upload the export anyway, since the manager approved the request verbally
   - **b**: Explain the clash, offer an aggregated version, and ask the rule's owner
   - **c**: Refuse to produce the report and say nothing more about the matter to anyone
   - **d**: Paste the export into a personal account that has no organisational policy attached

<details>
<summary>Answer key</summary>

1. **b**. The page states the condition for harm: "the risk exists when Claude can read untrusted content and take consequential actions". *a* is ruled out because "Take either away and the danger drops", and a laptop or a connection is neither of the two conditions. *c* is ruled out because the attack plants instructions in content, and the page says the first habit is "Limit what Claude reads to sources you trust", not to tidy images or updates. *d* is ruled out because an earlier chat history plays no part in the condition for harm, which is "read untrusted content and take consequential actions".
2. **d**. The per-tool setting lets reading run while a status change needs approval (the controls table). *b* is ruled out because the table places web search under "Where the web can reach", which does not touch connector actions. *c* is ruled out because scanning belongs to "Untrusted skills and plugins", not to what a connector may do. *a* is ruled out because a spend limit belongs under "Spend controls at the organisation and per person", which is cost and not permission.
3. **b**. The best answer "raises the conflict with the person who owns the policy and proposes a compliant path". *a* is ruled out because "Missing or unclear policy is not permission", and a verbal approval is not an exception from the policy owner. *c* is ruled out because the wrong answers include those that "refuse everything, or quietly find a way around", and an aggregated report is a compliant path. *d* is ruled out because "do not work around the rule (a personal account, a different tool, a rewording that hides the data)".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A recruiter wants Claude to screen applicants' CVs into "advance" and "reject" lists that go to hiring managers. Which
   step meets the Usage Policy and good practice?
   - **a**: Let Claude finalise both sets, since it explains every ranking it gives
   - **b**: A qualified person reviews the outcomes, and recipients are told AI assisted
   - **c**: Remove candidates' names, then send the sets out with no review at all
   - **d**: Run the screening in a personal account, so the content stays private to her

2. A colleague forwards a spreadsheet from an outside vendor and asks Claude for a summary with the Excel add-in. Rows contain
   white text addressed to the assistant. What is the most appropriate precaution?
   - **a**: Use a trusted copy and check each prompt, since external files can hide commands
   - **b**: Turn on cross-app mode, so that a second document can check what the first one holds
   - **c**: Delete the add-in's saved instructions so that nothing exists for the text to override
   - **d**: Rely on the audit log to catch whatever damage might be done afterwards

3. An analyst discovers that a shared document contains a hidden line aimed at AI tools, telling them to send a summary to
   an outside address. What should she do?
   - **a**: Tell the security contact now, and keep that item away from assistants
   - **b**: Delete the line and carry on as before, since it is now harmless to everyone
   - **c**: Try the line in a spare chat to find out whether it really works as claimed
   - **d**: Raise it at the next team meeting, which falls a few weeks away in the calendar

4. A hospital group that has a HIPAA agreement asks whether staff may turn on memory and use Claude in Chrome on patient pages.
   Which answer is correct?
   - **a**: Yes, since an owner can enable any feature across the whole organisation
   - **b**: Yes, if staff keep incognito mode switched on at all times during use
   - **c**: No: both are unavailable or not recommended under that arrangement
   - **d**: Only Chrome, since memory has no data retention at all after a session

<details>
<summary>Answer key</summary>

1. **b**. Hiring decisions are a high-risk use: a qualified professional reviews them and recipients are told AI helped. *a* is ruled out because "qualified professional in the field must review the content or decision before it is disseminated or finalised", and a model's own explanation is not a reviewer. *c* is ruled out because the review must happen "before it is disseminated or finalised", and removing names does not replace it. *d* is ruled out because "do not work around the rule (a personal account, a different tool, a rewording that hides the data)".
2. **a**. The page says external files can carry hidden instructions, so the precaution is a trusted copy and careful confirmations. *b* is ruled out because more reach is not a precaution: the user habit is "Limit what Claude can do". *c* is ruled out because the hidden text is in the data, and the page warns that "files from outside can carry hidden instructions", whatever the add-in's own settings say. *d* is ruled out because "the Microsoft 365 add-ins are outside the Enterprise audit logs and do not inherit custom retention settings", so there is nothing to rely on.
3. **a**. A suspicious file is reported to the security or IT contact straight away, because the same file may reach others. *b* is ruled out because deleting the line leaves others unaware, and "the same file may reach others". *c* is ruled out because running it in a tool that reads untrusted content re-creates the danger, and "Take either away and the danger drops". *d* is ruled out because "A leak or a suspicious file is reported quickly; waiting lets it spread."
4. **c**. Memory is not available under HIPAA agreements, and Claude in Chrome is not available to HIPAA organisations. *b* is ruled out because "the memory feature is not available to organisations with HIPAA, public-sector or custom retention agreements", which incognito mode does not change. *a* is ruled out because "a strict setting can switch a feature off", so an owner cannot enable what the agreement removes. *d* is ruled out because the page names both features among those unavailable under strict data agreements: "Claude in Chrome is not available to HIPAA organisations".

</details>
