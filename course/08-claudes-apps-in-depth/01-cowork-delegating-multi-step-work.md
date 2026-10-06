# Cowork: delegating multi-step work

**Level:** Foundations · **Module 8:** Claude's apps in depth · **Page 1 of 3**
**Exams:** AS3, AS4, AS6 (and X: this module goes beyond what the Associate guide names)

**After this page you can** decide whether a task belongs in chat or in Cowork, set up a Cowork task with the right
files, connectors and approval mode, and apply the safety practices the help centre asks for.

Checked against the Claude help centre pages "Get started with Claude Cowork", "Use Claude Cowork safely", "Claude
Cowork and chat are one Claude" and the Anthropic Academy pages on Cowork, read on 2026-10-02. Cowork is a fast-moving
product: where this page says "the page said", it reports the help centre on that date, and the official page decides
when it differs.

## Why it matters

Module 7 covered what you do inside a conversation. Cowork changes the shape of the work: you hand over a goal and
come back to a deliverable. That shift, from steering every turn to delegating a multi-step job, raises a new
question in domains 3, 4 and 6: which jobs suit delegation, what may the agent touch, and who is responsible for what
it did while you were away. Module 5's delegation and diligence ideas now carry real consequences.

## The idea

### What Cowork is

The help centre says Cowork brings Claude Code's agentic capabilities to knowledge work beyond coding. You describe
an outcome; Claude makes a plan, breaks complex work into smaller tasks when needed, coordinates parallel
workstreams, uses the tools and files it needs and hands back a deliverable such as a formatted document, organised
files or synthesised research. There is no terminal.

| | Chat | Cowork |
|---|---|---|
| Shape | A conversation you steer turn by turn | A delegation: describe the goal once, check back |
| Best for | A one-off question, a thought partner, work light on files and sources | A clear deliverable that touches your files or tools |
| Where it runs | The conversation | Tasks run in the cloud on Anthropic's servers; sessions can continue when your computer is offline |
| Files | What you upload | Folders you connect on the desktop app, without manual uploads |

The Academy guidance condenses the choice: reach for chat when you have a one-off question or want a thought partner,
and reach for Cowork when there is a clear deliverable and the work touches your files or tools. The help centre now
describes a new experience that merges chat and Cowork into one conversation, so you ask for what you need and
Claude decides which tool to use. It is rolling out gradually, starting with Pro and Max plans, so your account may not
have it yet. The distinction above is still the one to reason with, because the exam asks
what kind of work suits which style.

### Requirements, in the help centre's words

Cowork needs a paid plan (Pro, Max, Team or Enterprise). Local file access, browser use and computer automation
need the Claude desktop app for macOS or Windows to be open and connected. Web access to Cowork is available on
Pro, Max and Team plans. Multi-step tasks use more of your allowance than quick answers, so the page advises
grouping related work together and checking usage in Settings.

### The parts you configure

- **Folders and folder instructions.** On the desktop app Claude can read, write and organise files in the folders
  you connect, and you can add folder instructions that give project-specific context. It reaches only folders you
  explicitly connected.
- **Projects and memory.** Projects keep files, instructions and context together (module 7). Memory is shared
  between chat and Cowork when Cowork runs in the cloud; local Cowork sessions do not use memory.
- **Connectors.** The same connectors as in chat (module 7), with the same rule: Claude inherits your permissions
  and no more.
- **Skills and plugins.** A skill is one playbook; a plugin bundles skills, connectors and sub-agents around a job,
  and Anthropic publishes plugins for roles such as finance, legal, sales and marketing. The Academy lesson on plugins
  says they teach Claude your team's way of working, and recommends starting with one skill, testing it and then
  scaling to a plugin you share.
- **Scheduled tasks.** A recurring task, set with a `/schedule` command, runs in the cloud, so it does not need your
  computer awake or the desktop app open. Review the output after each run.
- **Approval mode.** **Manual** asks before every action; **Auto** has Claude check each action for safety and block
  what it judges unsafe, but it still runs on its own.

### A worked task

A project manager wants a weekly status pack from a folder of meeting notes, a tracker spreadsheet and the team's
chat channel.

1. **Choose the style.** There is a clear deliverable and the inputs live in files and tools, so this is Cowork, not
   a chat.
2. **Prepare the input.** A dedicated folder holds only the notes and the tracker. Folder instructions say who the
   audience is and what format the pack takes.
3. **Describe the outcome, including the check.** "Produce a one-page status pack. List any figure you could not
   verify against the tracker, and any action item with no owner."
4. **Choose the mode.** The first runs use **Manual**, because the task is new and writes files. After a few clean
   runs the manager may move to **Auto** for the routine part.
5. **Review.** The manager opens the pack, checks the figures that matter against the tracker and the unowned items
   against the notes, and sends it as their own work (module 5).
6. **Schedule with care.** Only once the output is reliably right is a weekly schedule added, with a note to review
   each run.

### Safe use: what the help centre asks of you

The safety page names three risks and a set of habits.

- **Prompt injection.** Malicious instructions can sit in external content Claude reads (an email, a web page, a
  document). The risk exists when Claude can read untrusted content and also take consequential actions, so keep
  those two apart where you can.
- **Deletion.** Claude can read, write and permanently delete files in the folders you connect. It needs your explicit
  permission before a permanent deletion, and you should keep backups of important files.
- **Unattended runs.** Scheduled tasks run while you are away, so you cannot watch them.

The habits: grant access selectively and avoid financial documents, credentials and personal records; create a
dedicated working folder; use **Manual** approval for sensitive files, new tools and hard-to-undo actions; watch for
unexpected patterns, such as access to files or sites you did not mention, and stop the task if something looks off;
start scheduled tasks with low-risk work such as summaries; and only give Claude internet access to sites you trust.

One distinction is easy to miss. The cloud environment is an isolated, temporary environment created for that session,
which cannot reach your home or company network and is removed when the session ends. But the page says that
isolation "doesn't limit what Claude reads or does" through the connected tools: the sandbox protects your network,
not your mailbox. Computer use, where Claude works on your screen, has no sandbox between Claude and what is on the
screen, so block sensitive apps and start with low-stakes tasks.

On Team and Enterprise plans, owners get monitoring and control: Cowork events can be streamed to the organisation's
observability tools, Cowork on web and mobile is captured in the Compliance API, skills and plugins can be scanned,
and owners can turn web search off. You remain responsible for what is published or sent, purchases made, data
changed and actions taken by scheduled tasks on your behalf.

## Traps

1. **Using Cowork for a one-line question.** Chat is faster and cheaper for light work; multi-step tasks use more of
   the allowance.
2. **Granting the whole drive "to save time".** A dedicated folder limits what an injected instruction or a mistake
   can reach.
3. **Scheduling before trusting.** A recurring task repeats its mistakes unattended. Start simple and review each run.

## Quiz

1. A finance analyst wants Claude to turn a folder of invoices into a reconciled spreadsheet and a summary, handled
   on its own while they are in meetings. Which choice fits?
   - **a**: Add the invoices to a Project, then request the sheet and summary in chat
   - **b**: Upload the invoices to a chat and ask for both outputs in a single turn
   - **c**: Attach the whole company drive so that nothing the task needs is missing
   - **d**: Delegate it to Cowork with a dedicated directory, then inspect the result

2. A team lead sets up a weekly Cowork task that reads a shared inbox and replies to customers. On the first
   scheduled run no one is watching. What should the team lead change first?
   - **a**: Limit early runs to summarising messages, so someone reviews each output
   - **b**: Start in Auto mode, as it checks every action for safety on its own
   - **c**: Connect the folder of past replies too, so drafts can match earlier answers
   - **d**: Run it daily rather than weekly, so that any mistakes surface sooner

3. A user connects a documents folder and their mailbox to Cowork. One file carries an invisible instruction telling
   Claude to mail the folder's contents to an address. The cloud environment is isolated and temporary. Why can this
   still cause harm?
   - **a**: The hidden text can reach the home network from inside the sandbox
   - **b**: The hidden text stays in the sandbox and acts again in later sessions
   - **c**: The hidden text can send messages via a linked tool the sandbox leaves open
   - **d**: The hidden text lets Claude reach folders the user never connected

<details>
<summary>Answer key</summary>

1. **d**. There is a clear deliverable that touches files, so it is a delegation, and a dedicated folder keeps the reach narrow (the worked task). *b* is ruled out because chat is best for "work light on files and sources", and a folder of invoices is heavy on files. *c* is ruled out because the safety habits say to "create a dedicated working folder" and not to grant whole drives. *a* is ruled out because a Project still works as chat, "A conversation you steer turn by turn", while "A clear deliverable that touches your files or tools" that runs on its own during meetings is what Cowork is for.
2. **a**. Replying to customers is a consequential action, and the page says to start scheduled tasks with low-risk work such as summaries and to review the output after each run. *b* is ruled out because Auto mode checks each action "but it still runs on its own", and the habits recommend Manual approval for new tools and hard-to-undo actions. *c* is ruled out because more context leaves the cause in place: "Scheduled tasks run while you are away, so you cannot watch them", and the replies would still go out unchecked. *d* is ruled out because more frequent runs only repeat errors faster: "A recurring task repeats its mistakes unattended".
3. **c**. The isolation "doesn't limit what Claude reads or does" through the connected tools, so the protection covers the network and not what Claude can send through the connected mailbox. *b* is ruled out because the environment "is removed when the session ends", so nothing in it carries into a later session. *a* is ruled out because the sandbox "cannot reach your home or company network", which is the part isolation does protect. *d* is ruled out because Claude "reaches only folders you explicitly connected", and the harm here comes from what it already reaches.

</details>
