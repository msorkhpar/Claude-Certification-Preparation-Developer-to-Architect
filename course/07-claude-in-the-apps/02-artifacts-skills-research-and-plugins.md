# Artifacts, skills, research and plugins

**Level:** Foundations · **Module 7:** Claude in the apps · **Page 2 of 3**
**Exams:** AS3, AS5 (and AS2, AS4)

**After this page you can** pick between a plain chat reply, an artifact, a skill, research and a plugin for a given
task, say what each costs and risks, and choose the output format that suits the reader.

Checked against the Claude help centre pages on artifacts, skills, research and plugins and the Anthropic Academy
page on plugins in Cowork, read on 2026-10-02, and against the Associate exam guide (domains 2 and 3, version 1.0,
July 2026). Plan names, beta labels and limits are as the pages read on that date; the official page decides when
it differs.

## Why it matters

Domain 3 of the Associate exam (Product and Model Selection, 12 percent) lists "select appropriate Claude product
features (Projects, research mode, chat, artifacts)", and domain 2 asks you to pick an output format: artifact,
inline reply or structured data. Scenario questions describe a task and ask which feature fits. The wrong options are
real features used for the wrong job, so you need to know what each one is for, not only what it is called.

## The idea

### Artifacts: a deliverable beside the conversation

The help centre defines an artifact as anything Claude makes for you that you would put in front of someone: a design,
a deck, a document, a dashboard or a small interactive tool. It opens in its own panel beside the chat, so you
can edit and re-use it without scrolling back through a conversation. Types include documents, code, web pages,
images and diagrams, dashboards and interactive tools, and on the paid plans templates for documents, slides and
designs (labelled beta on the page).

- **When Claude makes one.** The page says Claude generates an artifact when the content is significant and
  self-contained, typically over 15 lines, and reusable on its own. A short answer stays inline in the chat.
- **How you change it.** Ask in the chat, edit it directly in the template types, or highlight text and use
  "Edit with Claude". To try another direction without losing the first, edit an earlier message: the chat
  branches and each branch has its own artifact.
- **Sharing.** Artifacts are private by default. You can share them with specific people or publish them. The page
  warns to open only artifacts from people you trust, because you are bringing someone else's code and content
  into your own chat.
- **AI-powered artifacts.** An artifact can call Claude itself, so a small tool you build can answer questions or
  generate content for the person using it. Their use counts against their own plan limits, not the creator's, and
  each person connects their own apps even in a shared artifact.
- **Storage.** On the paid plans an artifact can keep data, up to 20 MB, text only, either personal (private) or
  shared (visible to every user). The page's warning is the exam-relevant part: before entering anything sensitive,
  check whether the artifact uses shared storage.

**Choosing a format (domain 2).** A short factual answer goes inline. A document, a deck, a dashboard or a tool
that someone will open, edit or send is an artifact. Data that another program will read is structured data (a
table, or JSON when you ask for it). The reader and the next step decide the format, not the model's habit.

### Skills: a playbook Claude loads when it is relevant

The help centre defines skills as folders of instructions, scripts and resources that Claude loads dynamically to
improve performance on specialised tasks. They use progressive disclosure: Claude reviews which skills are
available, loads the relevant ones and applies them. Only the pertinent skills enter the context window, so a
library of skills does not crowd it.

| Kind | What it is |
|---|---|
| **Anthropic skills** | Built in, for creating Excel, Word, PowerPoint and PDF files; they run automatically when relevant |
| **Custom skills** | Yours or your organisation's: brand rules, a meeting-notes format, a recurring analysis |
| **Organisation skills** | Provisioned by Team and Enterprise owners to every member, who can toggle them |
| **Partner skills** | From other companies, built to work with their connectors |

A simple custom skill needs no code: Markdown instructions in the required folder structure, zipped and uploaded
under Customize, Skills. Skills are available on Free, Pro, Max, Team and Enterprise plans and need code execution
and file creation to be enabled. On Team and Enterprise plans they can be shared with named people or groups.

**Skill or Project instructions?** Project instructions apply to every chat in that Project. A skill is a reusable
procedure Claude pulls in when the task matches, in any chat where it is enabled. Rules about one body of work
belong in a Project; a procedure you want everywhere (the monthly report layout) belongs in a skill.

The security sentence to learn: install skills only from trusted sources. The page names prompt injection and data
exfiltration as the most significant risks, and says to review code dependencies and bundled resources first.
Enterprise plans can scan uploaded skills and plugins for malicious content.

### Research: many linked searches, with citations

The help centre says Research lets Claude run multiple searches that build on each other, deciding what to
investigate next, and return a thorough answer in minutes with easy-to-check citations. You switch it on from the
"+" menu in the chat. It needs a paid plan (Pro, Max, Team or Enterprise), and **web search must be turned on**.
It draws on connected internal sources (for example Gmail, Calendar and Docs when connected) as well as the web.
Research counts against the same limits as ordinary chat but can use them up faster because it retrieves many
sources.

Use Research when the question is open and broad: a market overview, a comparison of vendors, a literature scan.
Do not use it for a question that one document answers, or one you can check in a minute. The citations make
verification quicker, and the verification rule from module 5 still holds: check the claims that would hurt if wrong
against the cited source, because a citation shows where an answer came from, not that the source is right.

### Plugins in the apps

The Cowork documentation describes a plugin as a package that bundles skills, connectors and sub-agents around a
job; a plugin you add is saved to your account. Where a skill is one playbook, a plugin is several, plus the
connectors they depend on. Anthropic publishes plugins for common roles such as finance, legal, sales, marketing
and customer support, which you can install, customise or build. Module 8 shows plugins at work in Cowork. The
safety rule is the same as for skills: a plugin carries instructions and connections, so install only what you
trust and review before sharing.

### Choosing the feature

| The task | The feature |
|---|---|
| A one-off question, a draft, a quick comparison | Plain chat, inline reply |
| Recurring work that needs the same rules and reference files | A Project |
| A document, deck, dashboard or small tool to hand to someone | An artifact |
| A repeatable procedure used across many chats | A skill |
| An open question that needs many sources and citations | Research |
| A role's bundle of procedures and connections | A plugin |

## Traps

1. **Using Research for a lookup.** A question one document answers does not need many searches; it spends
   allowance and adds sources to check.
2. **Typing sensitive data into an artifact with shared storage.** The artifact's storage may be visible to every
   user who opens it; check the setting first.
3. **Installing a skill because it is convenient.** A skill can carry scripts and instructions that Claude follows;
   from an unknown source it is a route for prompt injection.

## Quiz

1. A consultant must hand a client a polished one-page dashboard that the client will open, adjust and forward. Which
   output form fits best?
   - **a**: A long inline reply, copied by hand into the client's tool
   - **b**: An artifact, shown in a panel next to the chat, ready to edit and share
   - **c**: A skill, built once and kept private to the consultant's account
   - **d**: A Research run that returns cited findings in a report

2. A marketing lead wants every monthly report across many unrelated chats to follow one house layout. Which
   feature carries that procedure best?
   - **a**: An artifact holding last month's report as a pattern
   - **b**: The instructions of one Project, which cover only that Project's chats
   - **c**: Research switched on in the chat each month, before drafting begins
   - **d**: A skill that Claude loads whenever the task matches it

3. A colleague shares a ZIP file of a "free time-saving skill" from an unknown site and asks you to upload it. What is
   the best response?
   - **a**: Upload it straight away, since skills only run when the task matches them
   - **b**: Inspect everything inside the archive first, and decline if nobody can vouch for it
   - **c**: Upload it for yourself only, so that no colleague is exposed to it
   - **d**: Upload it to a test chat and judge it by the first reply it gives back

<details>
<summary>Answer key</summary>

1. **b**. An artifact is a deliverable that opens in its own panel for editing and sharing (the artifacts section). *a* is ruled out because "A short answer stays inline in the chat", while a dashboard to be opened and adjusted belongs in a panel of its own. *c* is ruled out because a skill is "a reusable procedure Claude pulls in when the task matches", not a deliverable the client can open. *d* is ruled out because Research is for "an open question that needs many sources and citations", and this task has a known content to build.
2. **d**. A skill is a reusable procedure pulled in "in any chat where it is enabled" (the skill or Project instructions paragraph). *b* is ruled out because "Project instructions apply to every chat in that Project", and the reports sit in many unrelated chats. *c* is ruled out because "Use Research when the question is open and broad", and a fixed layout is not an open question. *a* is ruled out because an artifact is "a document, deck, dashboard or small tool to hand to someone" and does not make later chats follow a layout.
3. **b**. Skills carry instructions and scripts that Claude follows, so the page says to install skills only from trusted sources and to review what they bundle. *a* is ruled out because "from an unknown source it is a route for prompt injection", whether or not the task matches. *c* is ruled out because the risks named, "prompt injection and data exfiltration", apply to the person who installs the skill, not only to colleagues. *d* is ruled out because the first reply cannot show hidden scripts or instructions, which is why the page says to review "code dependencies and bundled resources" first.

</details>
