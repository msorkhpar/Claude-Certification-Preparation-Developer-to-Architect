# Choosing the surface for the job

**Level:** Foundations · **Module 8:** Claude's apps in depth · **Page 3 of 3**
**Exams:** AS3, AS4, AS6 (and X: this module goes beyond what the Associate guide names)

**After this page you can** take a real task, choose the app or feature that fits it, say what that choice can reach
and what it must not, and defend the choice in terms of reach, visibility and risk.

Checked against the pages named on pages 1 and 2 of this module and on pages 1 to 3 of module 7, all read on
2026-10-02, and against the Associate exam guide (domains 3, 4 and 6, version 1.0, July 2026). The tasks below are
invented for this page; the facts about each app come from the official pages as read on that date.

## Why it matters

Domain 3 asks you to select product features for a task, and domain 4 to integrate Claude into existing workflows,
augmenting or redesigning them. A scenario question gives you a task and several real features, and the wrong
options fail for one of three reasons: they cannot reach what the task needs, they show the work to the wrong people,
or they can do more than the task justifies. This page gives you a checklist that sorts the options in that order.

## The idea

### Four questions that choose the surface

1. **What is the shape of the work?** A question or a draft: plain chat. Recurring work with fixed rules and
   reference files: a Project. A deliverable to hand over: an artifact. A multi-step job over files and tools:
   Cowork. A task inside an open Office file: the Microsoft 365 add-in. A step-by-step task on a website you
   trust: Claude in Chrome. A shared task in a team channel: Claude Tag.
2. **What must it reach?** Local folders, a connector's data, the open page, the open workbook, the thread. Pick the
   surface that reaches exactly that, and no more.
3. **Who must see it?** Claude Tag in a channel shows everything to everyone in the channel; a direct message does
   not. A shared Project, a published artifact and a shared skill show their content to everyone with access.
4. **How much can go wrong?** Reading is lower risk than writing, sending, deleting or buying. Prefer a surface and
   an approval mode where a person confirms the consequential steps.

Then ask the governance question of module 10: is the data allowed in this tool at all?

### Six worked tasks

**1. A quarterly board brief.** The inputs are last quarter's briefs, the style guide and fresh market facts.
*Choice:* a Project holding the style guide as instructions and the earlier briefs as knowledge, Research for the
market section, and an artifact for the finished document. *Why:* it is recurring work with fixed rules (Project),
the market part is an open question needing many sources (Research), and the result is a document to hand over
(artifact). *Check:* the figures that matter against their cited sources before the brief goes anywhere.

**2. Reconciling a month of supplier invoices into a spreadsheet and a summary.** *Choice:* Cowork on a dedicated
folder, in Manual approval for the first runs. *Why:* a clear deliverable from files, a multi-step job, no need to
steer each turn. *Must not:* get the whole drive, or run on a schedule before the output has been proved right.

**3. Updating a financial model and refreshing the deck and memo that quote it.** *Choice:* the Microsoft 365
add-ins with cross-app work on, from trusted files. *Why:* the work lives in open Excel, PowerPoint and Word
files, and context passes between them. *Must not:* produce a final client deliverable without human review, or
touch a workbook from an unknown source without a trusted copy.

**4. Comparing three suppliers' public pricing pages and filling a quote form.** *Choice:* Claude in Chrome on those
trusted sites, in Manually approve mode, with a browser profile that holds no sensitive accounts. *Why:* the work is a
step-by-step task on live pages you can watch. *Must not:* be left in Skip all approvals, or run with a bank or
medical tab open (visible tabs become part of the conversation).

**5. Reviewing the open items on a launch checklist with a team.** *Choice:* Claude Tag in the project channel, so the
thread is visible and anyone can steer it. *Why:* the work is shared and the team's context already lives there.
*Must not:* be used in the channel for a private or sensitive request; that belongs in a direct message, and a
sensitive HR matter belongs with the people and policy that govern it, not in a tool whose results the whole channel
can read.

**6. Drafting a one-off reply to a customer's complaint.** *Choice:* plain chat. *Why:* a short, single draft needs
none of the machinery above; adding a Project or Cowork would add setup, allowance use and risk with no gain.
*Check:* the facts it asserts, since you own what you send.

### What each can reach, and what it must not

| Surface | Can reach | Must not be given |
|---|---|---|
| Chat and Projects | What you upload, sync or paste; connectors under your own permissions | Data classes the organisation forbids; a shared Project holding material the audience may not see |
| Artifacts | What you put in; shared storage if enabled | Sensitive entries where storage is shared |
| Research | The web and connected sources | A job one document answers |
| Cowork | Folders you connect, connectors, the web, optionally the screen | Financial documents, credentials, personal records; a first schedule with consequential actions |
| Claude in Chrome | The pages and tabs in front of it, with your logins | Regulated data (not for HIPAA organisations), financial accounts, unfamiliar sites |
| Claude for Microsoft 365 | The files and mail open at that moment | Untrusted workbooks; final unreviewed deliverables; highly sensitive data without controls |
| Claude Tag | The thread, what an admin connected, public channels by search | Private requests in a public channel; anything the admin has not approved |

The pattern behind the column on the right: **least reach, least action, most visibility to the right people**. Give
the surface only what the task needs, keep a person on the consequential steps, and make sure the people who should
see the work can.

### Keeping the choice honest

A surface is chosen once and then forgotten, so revisit it when the work changes: a task that began as a one-off
and became weekly deserves a Project or a skill; a Cowork job that now touches new folders deserves a fresh look at
access; a Claude Tag channel that gained guests deserves a check of who can invoke Claude. These products change
quickly, so confirm features and plan limits on the official page the day you decide.

## Traps

1. **Picking the most powerful surface.** Cowork or a scheduled task for a one-line draft adds risk and cost; match
   the surface to the shape of the work.
2. **Ignoring who sees the result.** A channel shows everything to everyone in it, so a personal or sensitive task
   goes in a direct message or stays out of the tool.
3. **Forgetting that tabs and files are context.** Chrome sees what is visible and the add-ins see what is open;
   close what the task does not need.

## Quiz

1. A recruiter needs one short, polite decline email for a single candidate, with the facts supplied. Which surface
   suits it best?
   - **a**: Plain chat, with a quick check of the details before sending
   - **b**: Cowork on a dedicated folder holding the candidate's file
   - **c**: A shared Project holding every past letter, opened to the team
   - **d**: A Research run that gathers sources on rejection etiquette

2. A product team wants one visible place in Slack to chase open checklist entries, where anyone can steer the work.
   One entry is a pay dispute. Which approach fits?
   - **a**: Keep routine items in the thread, and flag the HR matter there as sensitive
   - **b**: Keep routine items in the thread, and move the HR matter to a shared Project
   - **c**: Move every item to a direct message, and post the results to the thread
   - **d**: Keep routine items in the thread, and keep the HR matter out of the tool

3. An analyst is editing a pricing workbook in Excel, and has the sales deck and cover memo built on its figures
   open beside it in PowerPoint and Word. A discount rate changes, and the new numbers must reach all three. Which
   surface fits best?
   - **a**: Claude Tag in a channel, with the three files attached
   - **b**: A Project whose knowledge base holds the workbook, deck and memo
   - **c**: The Microsoft 365 add-ins, set to let Claude act across files
   - **d**: Cowork on a connected folder holding copies of the three files

<details>
<summary>Answer key</summary>

1. **a**. A single short draft needs none of the machinery; plain chat plus a check of the facts is enough (task 6). *b* is ruled out because for a single draft "adding a Project or Cowork would add setup, allowance use and risk with no gain". *c* is ruled out because "A shared Project, a published artifact and a shared skill show their content to everyone with access", which is a poor home for candidate details. *d* is ruled out because Research is for an open question with many sources, and the table lists "A job one document answers" as what Research must not be given.
2. **d**. A channel suits shared work that anyone can steer (task 5), while a sensitive HR matter belongs
   with the people and policy that govern it, so the pay dispute stays out of the tool. *a* is ruled out because a sensitive HR matter belongs "not in a tool
   whose results the whole channel can read", and a sensitivity mark does not hide it. *b* is ruled out because the table says chat surfaces must not be given
   "a shared Project holding material the audience may not see", and the team would see the dispute. *c* is ruled out because the channel is chosen "so the
   thread is visible and anyone can steer it", and work done in a direct message leaves nobody else able to steer.
3. **c**. The work lives in open Excel, PowerPoint and Word files and context passes between them (task 3). *b* is ruled out because a Project suits "Recurring work with fixed rules and reference files", and does not edit the open files. *a* is ruled out because Claude Tag reaches "The thread, what an admin connected, public channels by search", while the figures must change in "The files and mail open at that moment". *d* is ruled out because the checklist gives Cowork "A multi-step job over files and tools" and gives "A task inside an open Office file" to the add-in, and these files are open in their apps, so copies in a folder would leave them as they were.

</details>

## Module quiz

This quiz covers every page of the module.

1. A team sets up a Cowork job that reads supplier meeting notes and updates the rows of a shared action tracker.
   They want it in Auto approval, running weekly from the first week, and checking last year's notes kept elsewhere
   on the drive. Which plan does the module support?
   - **a**: Approve actions by hand, open the drive wide to the job, then run it every week
   - **b**: Move it to the Excel add-in, let it reach notes on the drive, then run it weekly
   - **c**: Approve actions by hand, use one dedicated folder, then schedule after clean runs
   - **d**: Leave Auto approval on, give it one folder for both years, then schedule once runs are clean

2. A consultant opens a spreadsheet from an unknown sender in Excel with the add-in, and asks Claude to update the
   assumptions. The cells hide text telling Claude to send data elsewhere. Which handling fits?
   - **a**: Ask Claude in Chrome to open the sender's page and compare the figures
   - **b**: Turn on cross-app work, so that a second document checks each change
   - **c**: Make the edits manually, and keep the assistant off this workbook
   - **d**: Rely on Auto approval, which blocks anything unsafe in Office files

3. Contractors from a partner firm are added to a project channel where Claude Tag files and updates tickets for
   staff in the company's ticket tool. The contractors must not be able to file or change tickets through Claude.
   What should the organisation do?
   - **a**: Have an admin choose which members are allowed to call on the assistant
   - **b**: Disconnect the workspace from Claude until the contractors have left
   - **c**: Have the staff use Claude only through their own direct messages
   - **d**: Give Claude's ticket account the narrowest role that covers the work

4. Each month a facilities manager wants help to go through contractors' repair write-ups stored as files, update a
   spreadsheet of overdue jobs, and share the list where all their colleagues can see it and steer the follow-ups.
   Which combination fits best?
   - **a**: Plain chat with the write-ups uploaded, then Claude Tag in the team channel
   - **b**: Cowork on a dedicated reports folder, then Claude Tag in the team channel
   - **c**: Cowork on one folder of the write-ups, then Claude Tag in a direct message
   - **d**: The Excel add-in on the open spreadsheet, then Claude Tag in the team channel

<details>
<summary>Answer key</summary>

1. **c**. The habits say to use Manual approval for new tools and hard-to-undo actions, to grant access selectively,
   and to start scheduled tasks simple and review each run (Cowork page). *a* is ruled out because the habits say to
   "grant access selectively and avoid financial documents, credentials and personal records", and an entire drive
   does the opposite. *b* is ruled out because the add-in can "read and write only files that are open at that
   moment". *d* is ruled out because the first runs use Manual "because the task is new and writes files", and
   updating the tracker's rows writes files from the very first run.
2. **c**. The add-in page says to "only use it with trusted spreadsheets", and a workbook from an unknown sender with hidden instructions is not one. *b* is ruled out because with cross-app work "one conversation can read an Excel model and write a Word memo or a slide", which widens what the hidden text can reach. *a* is ruled out because the Chrome habits say to "avoid unfamiliar pages or those with content from unknown people", and the sender's page is one. *d* is ruled out because the Auto mode described is for Chrome and Cowork, and the add-in page warns that "files from outside sources can contain hidden instructions".
3. **a**. In a channel everyone gets the same access, and "Admins can restrict who can invoke Claude and where it
   works", which is the check a channel that gained guests deserves (pages 2 and 3). *d* is ruled out because "the
   narrowest role that covers the work" still lets Claude file and change tickets, and in a channel "everyone there gets the same
   access", so the contractors could use it too. *b* is ruled out because disconnecting a workspace "permanently
   deletes that workspace's Claude data", far more than the change needs. *c* is ruled out because a direct message
   changes only the staff's route to "Your own claude.ai account and your own connectors", and the channel set-up
   stays open to the contractors.
4. **b**. A multi-step job over files is Cowork, kept to a dedicated folder, and work the whole team must see and steer belongs in a channel (pages 1 and 3). *a* is ruled out because chat is best for "work light on files and sources", and a month of write-ups plus a spreadsheet to update is a multi-step job over files. *c* is ruled out because "Claude Tag in a channel shows everything to everyone in the channel; a direct message does not", so the colleagues could neither see the list nor steer the follow-ups. *d* is ruled out because "Claude can read and write only files that are open at that moment" and "it cannot open, create or switch files itself", so the add-in cannot work through the stored write-ups.

</details>
