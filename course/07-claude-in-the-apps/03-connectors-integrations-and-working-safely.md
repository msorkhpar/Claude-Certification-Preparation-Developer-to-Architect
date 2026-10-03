# Connectors, integrations and working safely

**Level:** Foundations · **Module 7:** Claude in the apps · **Page 3 of 3**
**Exams:** AS5, AS6 (and AS3, AS4)

**After this page you can** connect Claude to the tools where your work lives, say what a connector can and cannot
reach, govern the set of connectors you run, and share what you build without leaking what you should not.

Checked against the Claude help centre pages on connectors, Google Workspace connectors and skills, the Privacy
Center article on data use for model training (page dated 2026-03-16) and the Team plan page, read on 2026-10-02, and
against the Associate exam guide (domains 5 and 6, version 1.0, July 2026). Plans and settings are as those pages
read on that date.

## Why it matters

Domain 5 asks you to manage uploaded knowledge and connectors such as Google Drive and Gmail; domain 6 asks you to
apply data-sensitivity and privacy considerations. Connectors are where the two meet: a connector brings live data
into the conversation, so it is both the cure for stale files (page 1) and a new route by which data leaves its
home. The exam question is usually "which setting, which person, which permission": a small governance decision, not
a technical one.

## The idea

### What a connector is

The help centre says connectors let Claude access your apps and services, retrieve your data and take actions
inside the connected service. They work across chat, the desktop app, Cowork and Claude Code, and rest on the Model
Context Protocol (MCP), which Level 2 teaches. The Connectors Directory lists each connector with its use
cases, whether it can read, write or both, and its availability.

There are two shapes. **Remote connectors** reach an MCP server from Anthropic's cloud, not from your device, so the
server must be reachable on the public internet. **Desktop extensions** are for the desktop app only and run a
server locally. A **custom connector** is one you or your company add yourself, and it is where care matters most.

### The permission rule

The sentence to memorise: Claude inherits each person's permissions from the connected service. If someone cannot
open a file, channel or record in the source system, the connector cannot reach it from Claude for them either.
Three consequences follow:

- Each person signs in to the service themselves (unless the organisation set up shared credentials), so two
  colleagues can get different answers to the same question because they can see different files.
- A connector cannot give anyone more access than they already have. It does not bypass the source system's
  permissions.
- Fixing a connector's reach is done in the source system, by changing who can open what, not by rewording an
  instruction.

On Team and Enterprise plans an owner must enable connectors for the organisation before members can use them.
Owners can set what the tools may do: each tool can be **always allowed**, **needs approval** or **blocked**, so an
organisation can let Claude read a system and require a human click before it writes to it. Action restrictions work
alongside the source system's permissions; they never widen them.

### Tool settings you will meet

In the "+" menu or the "/" command you can choose how connector tools are loaded. The default Auto mode suits most
people, and the page suggests On demand when you have ten or more connectors, so that the conversation is not
crowded with tools. A free account can add one custom connector; paid plans have full custom connector support.

### Google Workspace as the worked example

The Google Workspace connectors let Claude search and read mail, draft and send messages, work with calendar
events and search, read and file Drive documents. Points an exam writer likes:

- Claude can open and change only files your Google account can access; sharing settings still rule.
- The help article says retrieved data is kept with its chat, so deleting the chat deletes the retrieved data.
- The article states that Anthropic does not train its models on your Gmail, Drive or Calendar connector data.
  The Privacy Center article on consumer data adds the other half: raw connector content is excluded from
  model improvement unless you copy it into a chat yourself. What you paste is chat content.
- Reading is a lower-risk setting than sending. A connector that can send mail as you needs an approval rule.

### Governing connectors: the checklist

1. **Connect only what the work needs.** Each connector is an extra route for data and for injected instructions
   (page 2, and module 10).
2. **Prefer the directory.** The help centre says to connect to servers from trusted organisations only, and that
   a custom connector has not been verified by Anthropic. Anything outside the directory is untrusted until you have
   reviewed who runs it.
3. **Read before you approve the scopes.** At sign-in the service lists what the connector may do. Deny what the job
   does not need.
4. **Separate read from write.** Allow reading freely where the data is not sensitive; keep "needs approval" on
   anything that sends, deletes or posts.
5. **Review the list on a schedule.** Remove connectors nobody uses, and re-check the ones whose provider changed.
   Remote tools can change after you approve them, so install-time trust can age.
6. **Keep the source of record clean.** A connector shows Claude what is in the system, mistakes included. If the
   shared drive holds three versions of a policy, Claude will see three.

### Browser and office integrations at a glance

Claude also works where you already are. **Claude in Chrome** is a browser extension on the paid plans that acts on
web pages for you. **Claude for Microsoft 365** adds Claude to Excel, PowerPoint, Word and Outlook. Both read what
is open in front of them and can act, so the permission and prompt-injection ideas above apply to them in force.
Module 8 teaches both, with Cowork and Claude Tag.

### Working safely and sharing

- **Treat the sharing list as the audience.** A shared Project, a published artifact and a shared skill show their
  content to everyone with access (pages 1 and 2).
- **Classify before you add.** If the data class is not allowed in the tool, it does not go in a Project, a chat or
  an artifact; module 10 gives the classes and the anonymise-first rule.
- **Use the controls that exist.** Incognito chats keep a conversation out of memory and history; per-chat memory
  switches and organisation settings are controls, and a quiet instruction not to store something is not one.
- **Name what Claude did.** When AI-assisted work goes to others, follow your organisation's disclosure rule
  (module 5).

## Traps

1. **Treating a connector as a permission upgrade.** Claude inherits your access and no more; a missing result often
   means the person cannot see the file.
2. **Enabling write actions by default.** A connector that can send or delete should need approval; reading and
   writing are different risks.
3. **Adding a custom connector from an unfamiliar provider.** The page says only to connect to servers from trusted
   organisations, and unverified servers can carry hidden instructions.

## Quiz

1. A manager links the company's cloud storage, then asks Claude to summarise a restricted finance directory. Claude
   cannot find it, although an administrator colleague sees it in the same storage. What explains this?
   - **a**: The tool carries the signed-in person's rights, which exclude that location
   - **b**: The tool reads only material above a certain age, and that area is newer
   - **c**: The workspace holding the chat must be shared before the area becomes visible
   - **d**: The area is too big for retrieval to cope with in a single request

2. A company connects its helpdesk to Claude. Agents may read tickets and add internal notes freely, but ending a ticket must wait for a person's click, and trainees must not see escalated tickets. Which design meets both rules?
   - **a**: Require approval to close, and write the group's limit into the connector's instructions
   - **b**: Allow every tool without approval and expect the helpdesk to stop the trainees
   - **c**: Require approval to close, and leave the group's limit in the source system itself
   - **d**: Require approval to close, and give that group wider access through Claude's settings

3. A team lead finds a handy link on a forum that would add a custom connector in seconds. What is the best step?
   - **a**: Install it, then watch the first few replies for anything odd
   - **b**: Install it on the free plan first, since that permits one trial server
   - **c**: Check who operates it, and which scopes it requests, before approving
   - **d**: Install it in On demand mode, which restricts what the server can do

<details>
<summary>Answer key</summary>

1. **a**. "Claude inherits each person's permissions from the connected service", so the manager's reach, not the administrator's, decides what appears. *b* is ruled out because the page names permissions as the cause of a missing result ("the connector cannot reach it from Claude for them either") and says nothing about the age of material. *c* is ruled out because "Fixing a connector's reach is done in the source system", not by sharing a workspace. *d* is ruled out because the page ties a missing result to access, as in "the connector cannot reach it from Claude for them either", and never to size.
2. **c**. Each tool can be set to need approval, and Claude inherits each person's permissions, so a restriction on one group belongs in the source system. *a* is ruled out because "Fixing a connector's reach is done in the source system,
   by changing who can open what, not by rewording an instruction". *b* is ruled out because an organisation can
   "let Claude read a system and require a human click before it writes to it", and always allowing every tool gives
   that up. *d* is ruled out because "A connector cannot give anyone more access than they already have".
3. **c**. The checklist says to connect only to servers from trusted organisations and to read the scopes before approving them. *b* is ruled out because "a custom connector has not been verified by Anthropic", whatever the plan allows. *a* is ruled out because "Remote tools can change after you approve them", so a good first reply proves nothing about later behaviour. *d* is ruled out because "the page suggests On demand when you have ten or more connectors", which is about tool loading and not about what a server may do.

</details>

## Module quiz

This quiz covers every page of the module.

1. A recruiter's Project answers hiring questions from a policy file that HR revises each quarter in a shared drive.
   The recruiter wants current answers without re-uploading, and wants candidate notes unseen by others who have
   access. Which design fits?
   - **a**: Upload the latest version each period, and share the space so HR can check it
   - **b**: Sync the source document, and leave private material out of the knowledge
   - **c**: Type the policy wording into the instructions, since those are always read
   - **d**: Switch on Research for every question, so the wording is looked up on the web

2. A department wants a monthly report in one layout, built from figures kept in a shared spreadsheet and delivered
   to a client in editable form. Which combination fits best?
   - **a**: A Project for the data, a plugin for the buyer and a skill for the output
   - **b**: Research for the data, memory for the procedure and an inline reply for the output
   - **c**: A skill for the procedure, a connector for the data, an artifact for the output
   - **d**: Three separate chats, one per stage, with material copied across by hand

3. A new hire installs a free skill found on a public forum. Its page says the skill also needs a custom connector
   to the hire's work email, and the hire's plan allows both. Which step belongs before installing?
   - **a**: Confirm that the plan lists skills and custom connectors, then install it
   - **b**: Trial it in a private chat first and judge it by the first few replies
   - **c**: Install it on the hire's account only, so colleagues stay out of reach
   - **d**: Inspect the package contents and confirm who operates the server it calls

4. Two colleagues ask the same shared Project about one finance folder. One receives a summary, the other is told
   nothing was found. Both reached the storage through the same custom connector, which the owner enabled. What
   should the owner check first?
   - **a**: Reconnect the storage for the second colleague under the owner's credentials
   - **b**: Reword the Project instructions to search the whole folder
   - **c**: Switch the connector to On demand so tools stop crowding the chat
   - **d**: Compare what each of them is allowed to see in the source system

<details>
<summary>Answer key</summary>

1. **b**. A synced document follows its source, and the knowledge base is readable by everyone with access, so private notes stay out (pages 1 and 3). *a* is ruled out because "an uploaded file is a copy as of the day you uploaded it", and sharing exposes everything the knowledge holds. *c* is ruled out because "A figure typed into the instructions is a second copy that no file update will touch". *d* is ruled out because "Do not use it for a question that one document answers", and a policy file is that case.
2. **c**. A skill carries a repeatable procedure, a connector reads live data, and an artifact is a deliverable someone can open and edit. *b* is ruled out because "A short answer stays inline in the chat", which does not suit a file to be edited. *a* is ruled out because a plugin is "A role's bundle of procedures and connections", and not the means of handing one deliverable to one buyer. *d* is ruled out because "A skill is a reusable procedure Claude pulls in when the task matches", which copying by hand gives up.
3. **d**. A skill carries instructions and scripts that Claude follows, and a custom connector "has not been
   verified by Anthropic", so the contents and the operator need checking first. *a* is ruled out because "A skill
   can carry scripts and instructions that Claude follows", so plan availability says nothing about safety. *b* is
   ruled out because the page says to "review code dependencies and bundled resources first", and a first reply
   cannot show them. *c* is ruled out because "The page names prompt injection and data exfiltration as the most
   significant risks", and the mailbox in danger is the installer's own.
4. **d**. Claude inherits each person's permissions, so two people can get different answers because they can see
   different files. *a* is ruled out because a connector "does not bypass the source system's permissions". *b* is
   ruled out because "Fixing a connector's reach is done in the source system, by changing who can open what, not by
   rewording an instruction". *c* is ruled out because On demand is suggested "when you have ten or more connectors,
   so that the conversation is not crowded with tools", which is about loading and not about reach.

</details>
