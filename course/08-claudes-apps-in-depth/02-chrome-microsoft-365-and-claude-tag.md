# Claude in Chrome, Claude for Microsoft 365 and Claude Tag

**Level:** Foundations · **Module 8:** Claude's apps in depth · **Page 2 of 3**
**Exams:** AS3, AS5, AS6 (and X: this module goes beyond what the Associate guide names)

**After this page you can** say what each of three embedded Claude apps reads and does, what permissions and approval
modes control it, who administers it, and where a person must still check the work.

Checked against the Claude help centre pages "Use Claude in Chrome safely" and "Claude in Chrome permissions guide",
the Claude documentation pages for Claude for Excel and "Work across M365 apps", and the Claude Tag help and
documentation pages ("What is Claude Tag?", "Get started", "Set up Claude Tag"), read on 2026-10-02. Claude Tag was in
public beta on that date, and all three products change often; the official page decides when it differs.

## Why it matters

These three bring Claude to where work already happens: the browser, the Office documents and the team chat
channel. That is convenient, and it is also where the exam's governance questions live. Each app sees what is in front
of it (a web page, an open workbook, a channel thread), each can act and not only answer, and each has different
administrators. A candidate who knows the distinguishing facts picks the right app for a scenario and the right
safeguard for its risk.

## The idea

### Claude in Chrome

Claude in Chrome is a browser extension, in the Chrome side panel, on the paid plans (Pro, Max, Team, Enterprise). It
is not supported on other Chromium browsers or on mobile. Claude sees what is on the page and acts on it: reading,
clicking, typing, navigating and filling forms, with the logins you already have, on the tab in front of you. It
suits in-the-moment browsing tasks such as summarising or comparing tabs, pulling details from a page into a note, and
walking through a site step by step while you watch. Recurring browser tasks can run on a schedule.

**What it captures.** Claude takes screenshots of the active tabs, and the safety page says whatever is visible
in them becomes part of the conversation. So a tab with another customer's record open is part of the context.

**Permission modes** (the permissions guide):

| Mode | What happens |
|---|---|
| **Manually approve** | Claude pauses and asks before each action; you allow or deny |
| **Automatically approve** | Claude keeps working and reviews each action for safety, blocking what it judges unsafe |
| **Skip all approvals** | Claude does not pause and nothing checks its actions: only for complete trust |

Site permissions come in two sizes: allow one action, or "always allow actions on this site". Even with an always-allow,
certain things still need your explicit approval: file downloads, entering sensitive information and granting
authorisations. Some actions are off limits whatever the mode. The permissions guide lists "Making purchases or
financial transactions" among the actions Claude is prohibited from regardless of permission mode (checked 2026-10-02),
and the same list includes creating accounts, permanent deletions and, notably, completing instructions found in emails or web content. On
Team and Enterprise plans admins can set allowlists and blocklists of sites for everyone.

**The risks** are those of any tool that reads untrusted content and acts. The page's example is a harmless-looking
to-do list or email containing invisible text that tells Claude to fetch bank statements. Claude's classifiers screen
incoming content and each action, and the page reports a low attack success rate in Anthropic's internal testing, while
saying plainly that the risk is not zero. Two further facts: the page says Claude in Chrome is not available to
organisations covered by HIPAA and recommends against using it on pages that contain regulated data; and the user
remains responsible for everything done on their behalf, such as messages sent, purchases made and data changed.

Good habits: start with trusted sites; avoid unfamiliar pages or those with content from unknown people; use a
separate browser profile without sensitive accounts; prefer Manually approve for anything that matters; and do not
manage financial accounts, legal documents or medical information through it.

### Claude for Microsoft 365

Claude for Microsoft 365 is a set of add-ins for **Excel, PowerPoint, Word and Outlook**, installed from Microsoft
AppSource, for the paid plans. Each runs in the sidebar of its app and works on what is open there. Taking Excel as
the example (the page for each add-in follows the same pattern):

- **What it does.** Answers questions about the open workbook with cell-level citations you can click; changes
  assumptions while keeping formula relationships intact; finds the source of errors; builds a model from a
  description or fills a template; sorts, filters and edits pivot tables.
- **Instructions** are set per add-in, in its settings: a formatting convention such as thousand separators applies
  to every Excel conversation and does not carry into Word or PowerPoint.
- **Across apps.** With "Let Claude work across files" turned on in each add-in (on by default for Pro and Max,
  off by default for Team and Enterprise, and set per device), one conversation can read an Excel model and write
  a Word memo or a slide. Claude can read and write only files that are open at that moment, and it cannot open,
  create or switch files itself. Skills you enabled apply in the right app. Cross-app work is not supported through
  Bedrock, Vertex AI, Azure AI Foundry or an LLM gateway.
- **Admins.** Organisation admins deploy the add-ins through the Microsoft 365 Admin Center, and Team and Enterprise
  owners can switch cross-app work off in Organization settings, Office agents.
- **Data handling.** Inputs and outputs are deleted from Anthropic's backend within 30 days. Chat history is stored
  in your browser, not on Anthropic's servers. The add-ins do not inherit your organisation's custom data-retention
  settings, and their activity is not in the Enterprise audit logs; for Enterprise organisations with the Compliance
  API enabled, add-in sessions are included in it (a public beta on that date).
- **Limits the page states.** Not recommended for final client deliverables without human review, audit-critical
  calculations without verification, or models with highly sensitive or regulated data without proper controls.
  Macros and VBA, and data tables, are unsupported. It warns that files from outside sources can contain hidden
  instructions: only use it with trusted spreadsheets, start from a trusted copy before widespread edits, review
  changes before finalising, and read each confirmation Claude asks for.

### Claude Tag

Claude Tag is Claude working in your team's Slack workspace: you tag `@Claude` in a conversation and it takes on real
work, using your organisation's tools and the shared context. It replaced the earlier Claude in Slack experience in
August 2026 and was in public beta on 2026-10-02. It is available on Team and Enterprise plans only, on Anthropic's own
service, and not for organisations with zero data retention or customer-managed encryption, because it stores channel
memory and session transcripts.

**Where you tag decides what it can use and who sees the result.**

| Where | Whose tools and access | Who sees it | Who pays |
|---|---|---|---|
| **Channel** | What an admin set up for that channel; everyone there gets the same access | Everyone in the channel, including Claude's checklist and results | The organisation |
| **Direct message** | Your own claude.ai account and your own connectors | Only you | Your own account |
| **Group DM** | The access an admin set for the workspace | The people in the group | The organisation |

What it can read: the thread it was tagged in (including earlier messages when mentioned mid-thread), files you attach
within size limits, and other public channels only by searching, as a person would. Private channels and DMs are read
only from inside them. A Slack canvas is not readable. Tasks run in the cloud, so Claude keeps working after you
close Slack; once it is in a thread it follows every reply. You can set a channel's standing instructions by telling
Claude there, and they go to channel memory, which admins can review and delete.

**Administration.** Setup needs the Owner role in a Claude organisation on a Team or Enterprise plan and a Slack
workspace admin to install the app. It pairs the workspace, launches in chosen channels, and sets a monthly spend
limit. For tools beyond Slack, Claude gets accounts of its own, with the narrowest role that covers the work,
so its actions appear in each tool's audit log under its own name and its access can be cut off without touching
anyone else's. Admins can restrict who can invoke Claude and where it works, and an organisation can disconnect a
workspace, which permanently deletes that workspace's Claude data.

**The habit the page asks for:** read Claude's work in proportion to what is at stake. A summary you can skim;
something going to a customer or changing a system gets a careful read, and you can ask it to show its work in the
same thread.

### Telling the three apart

| App | Where it works | What it reads | Biggest governance point |
|---|---|---|---|
| **Claude in Chrome** | The Chrome side panel | Pages and tabs you have open, through screenshots | Untrusted web content can carry injected instructions; not for HIPAA organisations |
| **Claude for Microsoft 365** | Excel, PowerPoint, Word, Outlook sidebars | Files and mail currently open | Outside files can carry hidden instructions; activity not in the Enterprise audit logs |
| **Claude Tag** | Slack channels and DMs | The thread and what an admin connected | Where you tag decides whose access is used and who sees it |

## Traps

1. **Choosing Skip all approvals for convenience.** Nothing checks the actions then; the page reserves it for complete
   trust, and even other modes keep protected actions behind your approval.
2. **Assuming an add-in's activity is in the audit log.** The Microsoft 365 add-ins do not appear in Enterprise audit
   logs and do not inherit custom retention settings.
3. **Tagging Claude in a channel for a private task.** Work in a channel is visible to everyone there and uses the
   channel's access; a personal task belongs in a direct message.

## Quiz

1. An operations lead runs Claude in Chrome in Automatically approve mode on a supplier portal. A page there holds
   white-on-white text telling the assistant to open a payment form. Which behaviour does the page describe?
   - **a**: Claude follows the text, since the portal is a site the lead chose to visit
   - **b**: The extension strips hidden text before reading, so it never reaches Claude
   - **c**: Screening switches off in this mode, since the lead has delegated the decisions
   - **d**: Screening covers each step, and financial transactions stay off limits

2. An Enterprise team with the Compliance API enabled wants one record of the Excel add-in's use, and wants its
   custom data-retention rule to govern it. What does the page support?
   - **a**: Sessions are included as a public beta, but the deletion schedule does not carry over
   - **b**: Neither the API nor the audit logs cover it, so no central record exists
   - **c**: Chat history sits on Anthropic's servers, so the deletion schedule governs it
   - **d**: Use shows in the audit logs, and the custom deletion schedule applies to it

3. A manager wants Claude Tag to draft a delicate reply from her saved connectors, hidden from the team, with the
   usage counted against her individual account. Where should she write to it?
   - **a**: In a channel where an admin set up the connectors for everyone there
   - **b**: In a direct message, whose access and cost belong to the sender
   - **c**: In a channel, then ask Claude to hide the reply afterwards
   - **d**: In a group direct message with one trusted colleague

<details>
<summary>Answer key</summary>

1. **d**. The page describes classifiers that screen incoming content and each action, and purchases and other
   financial transactions are off limits whatever the mode. *a* is ruled out because the page lists "completing
   instructions found in emails or web content" among the actions that are off limits. *b* is ruled out because the
   page reports an attack rate while "saying plainly that the risk is not zero". *c* is ruled out because in this
   mode "Claude keeps working and reviews each action for safety, blocking what it judges unsafe".
2. **a**. The page says add-in sessions are included in the Compliance API, and that the add-ins "do not inherit
   your organisation's custom data-retention settings". *d* is ruled out because "their activity is not in the
   Enterprise audit logs". *b* is ruled out because "add-in sessions are included in it" for Enterprise
   organisations with the Compliance API enabled. *c* is ruled out because "Chat history is stored in your browser,
   not on Anthropic's servers".
3. **b**. A direct message uses "Your own claude.ai account and your own connectors", and only you see it (the
   table). *a* is ruled out because a channel gives "What an admin set up for that channel; everyone there gets the
   same access" and the organisation pays. *d* is ruled out because a group direct message uses "The access an admin
   set for the workspace" and is paid by the organisation. *c* is ruled out because "Work in a channel is visible to
   everyone there and uses the channel's access", so hiding the reply later does not undo that.

</details>
