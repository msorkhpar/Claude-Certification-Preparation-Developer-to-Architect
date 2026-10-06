# Projects, standing instructions and knowledge you keep current

**Level:** Foundations · **Module 7:** Claude in the apps · **Page 1 of 3**
**Exams:** AS5, AS3 (and AS2, AS4)

**After this page you can** set up a Project with standing instructions and a knowledge base, decide what belongs
in the instructions and what belongs in the knowledge, and keep both current so the answers stay right.

Checked against the Claude help centre pages on Projects, on Google Workspace connectors and on chat search and
memory, read on 2026-10-02, and against the Associate exam guide (domain 5, version 1.0, July 2026). The apps change
quickly: plan limits, names of settings and beta features below are what the pages said on that date, and the
official page is the authority when it differs.

## Why it matters

Domain 5 of the Associate exam (Configuration and Knowledge Management, 12 percent) asks you to configure a Project
with instructions and knowledge sources, manage uploaded knowledge and connectors, write effective system-level
instructions, and maintain all of it. The typical question is not "what is a Project" but "the answers are wrong
for a month now: what went stale?" A Project is only as good as the least current thing in it.

## The idea

### What a Project is

The help centre describes a Project as a self-contained workspace with its own chat histories and knowledge base.
Inside one you can upload documents, text, code or other files, write project instructions, and hold focused chats
that start with all of that already in view. A free account can hold up to five Projects; the page gives no limit for the paid plans. Projects use plan allowance like any other chat.

Three pieces matter for the exam:

| Piece | What it is | What goes in it |
|---|---|---|
| **Instructions** | Standing rules every chat in the Project starts with | Role, audience, tone, format, what to do when unsure, what never to do |
| **Knowledge** | Files and synced documents Claude can draw on | Reference material: policies, price lists, style guides, past examples |
| **Chats** | Separate conversations inside the workspace | The actual tasks; each can be restarted without losing the first two |

Instructions say **how to work**. Knowledge says **what is true here**. A rule such as "answer in plain language for
a non-technical reader and say so when the documents do not cover the question" is an instruction. The price list
itself is knowledge. Mixing them is the commonest set-up mistake: a price pasted into the instructions can never
be refreshed by updating a file.

### How much fits: context and retrieval

Everything in a Project competes for the context window (module 1). On the paid plans the help centre says that when a
Project approaches the context limit Claude enables RAG mode, retrieval of the relevant parts, which expands
the Project's capacity, by up to ten times. Two consequences follow. A big knowledge base is possible, and
a retrieved passage is only as good as its source, so a muddled file produces muddled answers at scale.

### Sharing a Project

On the Team and Enterprise plans a Project can be shared. The permission levels are **can view** (read-only access
to contents, knowledge and instructions) and **can edit** (change instructions, knowledge and who has access). The page
says organisation-wide sharing is available by default unless admins disable it. Treat sharing as publishing: anyone
with access can read the instructions and every file in the knowledge base, so do not put in what the audience may
not see.

### Memory is separate from the knowledge base

Claude's memory (the chat search and memory page) saves things such as your role, projects and working preferences,
and you can say "remember this" or edit it in Settings, Memory. Two facts matter here. **Each project has its own
separate memory space and dedicated project summary**, so one Project's context does not leak into another. And
memory is not a substitute for a knowledge file: it holds preferences and context that Claude picked up, not the
authoritative text of a policy. On Team and Enterprise plans owners decide whether memory is available at all, and
the page says memory is not available to organisations with HIPAA, public-sector or custom data-retention
agreements. Incognito chats are available only outside Projects, but memory can be switched off for a single chat
inside a Project before the first message is sent.

### Connecting a document instead of copying it

The Google Workspace connector page says a Drive document added to a private Project's knowledge syncs from Drive,
so you are always working with the latest version. Three details to know: it reads text content only, images
embedded in a document are not processed, and Claude can open only files your own Google account can open. Shared
Projects do not offer the Drive option on that page, which matters when a team wants one shared source.

The sentence that decides most questions: an uploaded file is a copy as of the day you uploaded it, while a synced
document follows its source. That distinction is a course working rule built on the page, not a quoted sentence,
and it is the reason a team that updates a price list monthly should sync it rather than re-upload it each time.

### Keeping instructions and knowledge current

Plan this from the first day, because nothing alerts you when a file goes stale.

1. **Name an owner and a review date.** Each source has a person who answers for it. Put the review date in a
   short note at the top of the instructions or in a calendar, so a stale file is a missed appointment, not a
   surprise.
2. **One source of record.** If the policy exists in the wiki and as a PDF in the Project, one of them is wrong
   within a quarter. Keep the knowledge base a mirror of the source of record, or sync the source itself.
3. **Remove superseded versions.** Two versions of one document side by side invite Claude to quote either. Delete
   the old file; do not ask Claude to prefer the newer one, because an instruction is a request and a deleted file
   is a guarantee (the lesson of module 5).
4. **Version the files you control.** Put a version or date in the file name and the first line (`pricing-2026-10`),
   and keep the earlier versions outside the Project, in your normal storage, so you can go back.
5. **Judge the source before you add it.** Quality in is quality out: prefer the final approved text over a draft, a
   primary document over a forwarded summary, and a short clean file over a thick scan with unreadable pages.
6. **Change instructions one thing at a time.** After a change, run the same three or four test requests you used
   before and compare. If you change the tone, the format and the scope together you cannot tell which change helped
   (the controlled-fix rule of module 6).
7. **Ask Claude to cite its source in the answer.** A reply that names the file it used shows at once when it used an
   old one.

<!-- illustrative -->
A hand-scripted check, labelled illustrative. The reply is invented for this page.

```text
Project:   "Sales answers". Instructions: audience is the sales team; quote prices only from the pricing
           file named below and name the file you used; if no file covers the question, say so.
Request:   "What is the list price of the Standard plan?"
Reply:     "The Standard plan is 120 per seat per year (source: pricing-2026-03.pdf)."
Review:    the file name is March. The pricing file for October exists in Drive. The stale answer is
           traceable in one glance, and the fix is to replace the file, not to reword the question.
```
<!-- /illustrative -->

## Traps

1. **Putting changing facts in the instructions.** A figure typed into the instructions is a second copy that no
   file update will touch.
2. **Keeping every version "just in case".** Old and new documents side by side give Claude two sources to quote.
   Remove the old one from the Project and keep it in your own storage.
3. **Assuming sharing is private.** A shared Project exposes its instructions and knowledge to everyone with access;
   an unreviewed file can reveal more than you meant.

## Quiz

1. A sales rep's private Project holds a price list saved as a file in March. Finance revises that list every month
   in a cloud-hosted document, and the rep keeps quoting March figures. What is the best fix?
   - **a**: Add each month's file next to the old ones and keep every one of them in view
   - **b**: Tell the instructions to favour whichever figure looks most recent in the files
   - **c**: Ask Claude to mention any figure it feels unsure about at the end of each reply
   - **d**: Swap the snapshot for a synced Drive link that tracks each new edition

2. In March a colleague pastes the refund percentage into a Project's instructions. In June the policy changes, the
   team deletes the March policy file and uploads the June one, yet July replies still quote the March percentage.
   What explains it?
   - **a**: An earlier figure lives on in the standing rules, which the swap never touched
   - **b**: Memory carried the March percentage over from an earlier conversation
   - **c**: Retrieval over a large knowledge base returned an older passage than the June file
   - **d**: Sharing the Project keeps showing members the percentage they saw first

3. An operations lead supports several outside organisations, whose background material and house rules must never
   mix. Which set-up fits?
   - **a**: One Project, with each organisation's work kept in a chat of its own
   - **b**: One Project, with each organisation's rules headed by its name in the instructions
   - **c**: A Project apiece, each holding only its own instructions and reference files
   - **d**: Plain chats outside Projects, with memory keeping each organisation's details

<details>
<summary>Answer key</summary>

1. **d**. A synced document follows its source, so the next read gets the latest version (connecting a document instead of copying it). *b* is ruled out because "an instruction is a request and a deleted file is a guarantee", and the old figure would still be in the knowledge base. *c* is ruled out because "nothing alerts you when a file goes stale", so Claude has no reason to doubt a March figure and the stale file stays in place. *a* is ruled out because "Two versions of one document side by side invite Claude to quote either".
2. **a**. Instructions say how to work and files say what is true, so a figure typed into the instructions is a
   second copy that no file update will touch (the first trap). *b* is ruled out because memory "holds preferences
   and context that Claude picked up, not the authoritative text of a policy", and here the figure was pasted into
   the instructions, not picked up in a conversation. *c* is ruled out because the team
   deleted the March file, and "a deleted file is a guarantee" that no older passage remains to retrieve. *d* is ruled
   out because "anyone with access can read the instructions and every file in the knowledge base" describes who
   sees the content, not whether it is current.
3. **c**. Each Project is a self-contained space, so "one Project's context does not leak into another" (the memory paragraph). *b* is ruled out because instructions are "Standing rules every chat in the Project starts with", so headings by name still put every organisation's rules into every chat. *a* is ruled out because a Project lets you "hold focused chats that start with all of that already in view", so a chat of its own still sees every organisation's files and rules. *d* is ruled out because "memory is not a substitute for a knowledge file" and holds preferences, not each organisation's authoritative documents.

</details>
