# The usage policy and sensitive data

**Level:** Foundations · **Module 10:** Safety, privacy and policy · **Page 1 of 2**
**Exams:** AS6 (and DV6, AS2)

**After this page you can** say what the Anthropic Usage Policy asks of you and of the people who rely on your
output, sort the data you handle into classes, decide what may go into which tool, and anonymise before you upload.

Checked against the Anthropic Usage Policy (the page showed an effective date of 15 September 2025 when read on
2026-10-02) and the Privacy Center articles on data use for model training (consumer page dated 2026-03-16,
commercial page dated 2026-08-18), consumer retention and commercial retention, read on 2026-10-02, and against the
Associate exam guide (domain 6, version 1.0, July 2026). Policies change; the official pages decide when they differ.
This page is a study aid and not legal advice.

## Why it matters

Domain 6 of the Associate exam (Governance, Risk, and Responsible Use, 15 percent) asks you to identify appropriate and
inappropriate use cases, apply data-sensitivity, regulatory and privacy considerations, follow organisational AI
policies and understand the ethical implications of AI use. The same ideas sit in the Developer exam's security
domain. The recurring scenario is a person with a useful task and a file they should not upload: the right answer
is almost never "refuse the task" and almost never "upload and hope"; it is to change the data so the task can proceed
within policy.

## The idea

### The Usage Policy in three parts

The policy has three parts: **universal usage standards** that apply to everyone, **high-risk use case requirements**
for consumer-facing uses in sensitive fields, and **additional guidelines** for chatbots, products serving minors,
agentic use and servers built on the Model Context Protocol (MCP). The page says that agentic use must still comply with the
policy.

**Universal standards.** The prohibited categories on the page, in the course's own words:

1. breaking applicable laws, including infringing intellectual property;
2. compromising critical infrastructure;
3. compromising computer and network systems (malware, unauthorised access, bypassing security controls);
4. developing weapons;
5. inciting violence, extremism or hateful behaviour;
6. compromising privacy or identity: sharing personal information without consent, gathering private information without
   permission, and impersonating a human in communications;
7. compromising children's safety;
8. creating psychologically harmful content, such as promoting self-harm or harassment;
9. creating or spreading misinformation, including false medical or scientific information and impersonation;
10. undermining democratic processes, such as deceptive political content or automated messages that hide their artificial
    origin;
11. criminal-justice, censorship and surveillance uses, such as tracking a person's location without consent or scoring
    people's trustworthiness;
12. fraudulent, abusive or predatory practices, such as phishing, fake reviews, falsified documents, and plagiarising
    or submitting AI-assisted work without proper permission or attribution;
13. abusing the platform, such as evading a ban through another account or deliberately bypassing guardrails;
14. generating sexually explicit content.

You do not need to memorise the list, but you should recognise a category from a description, and notice that two of
them touch ordinary office work: privacy (personal information about others) and attribution (passing off AI-assisted work).

**High-risk use cases.** The policy singles out uses where output directly affects individuals or consumers: legal
interpretation, healthcare decisions, insurance underwriting and claims, financial decisions and advice, employment
and housing decisions, academic testing and admissions, and media or journalistic content. Two requirements apply. A
**qualified professional in the field must review** the content or decision before it is disseminated or finalised
(human-in-the-loop). And if model output is presented directly to individuals or consumers, you must **disclose that AI
helped produce it**, at a minimum at the beginning of each session. Separately, consumer-facing chatbots must also disclose that the user is talking to AI rather than a human, at a minimum at the beginning of each chat session. A person drafting a letter for their own review is
not outside the rule's spirit: the more the output decides something about a person, the more a qualified human must
sit between the model and that person.

### Appropriate and inappropriate use, as a test

When a scenario asks whether a use is appropriate, ask four questions in order:

1. **Is the purpose allowed?** Does it fall in a prohibited category?
2. **Is it high-risk?** Does the output decide or advise about a person in law, health, money, work or education?
   If so, a qualified person reviews it and recipients are told AI helped.
3. **Is the data allowed here?** (The next sections.)
4. **Does our own policy say more?** The organisation's AI policy can be stricter than the Usage Policy and decides
   which tools and data are permitted at work.

### Data classes

Organisations label data by how much harm its exposure would cause. The names vary; the idea is the same, and the
exam asks you to apply your organisation's scheme. This course uses four:

| Class | Examples | Typical rule for an AI tool |
|---|---|---|
| **Public** | Published prices, press releases | Free to use |
| **Internal** | Meeting notes, drafts, org charts | Allowed in tools the organisation has approved |
| **Confidential** | Contracts, unreleased financials, customer lists | Only in approved tools, with a need to know, and often with identifiers removed |
| **Regulated** | Health records, payment-card data, government IDs, personal data under GDPR | Not in a tool unless policy and the contract explicitly allow it; otherwise anonymise first |

The word "internal" in a scenario ("it's only for internal analysis") is a favourite wrong-answer lure: internal use
does not change the class of the data. The sample question in the Associate guide turns on exactly this: a
spreadsheet of customer names and account numbers, a policy restricting regulated personal data, and the answer is to
remove or anonymise the identifiers before uploading.

### Anonymise before you upload

You can usually keep the task and drop the identity. Techniques, from strongest to weakest:

- **Aggregate.** Send totals and trends, not rows ("revenue by region by month").
- **Remove direct identifiers.** Names, addresses, phone numbers, e-mail addresses, account and ID numbers.
- **Replace with codes (pseudonymise).** Customer 0417 instead of a name, with the key kept somewhere that Claude
  never sees. Pseudonymised data is still personal data under many rules, because the key can reverse it.
- **Generalise.** Age bands instead of birth dates; a region instead of a street.
- **Remove free text you have not read.** Notes columns hide names and details.

Then check for **re-identification**: a rare job title, a small town and an exact date can identify a person without a
name. If the combination is unusual, generalise further. Anonymising is a step you do and check; asking Claude to
"ignore the names" or "not retain" the file is a request, and it does not satisfy a policy control.

### What happens to what you type

A tool's data terms depend on the plan, so know which you are on. The Privacy Center pages, in summary:

- **Consumer plans (Free, Pro, Max).** Your chats and coding sessions are used to improve Claude only if you choose to
  allow it in your privacy settings (or take part in a specific programme), or if a conversation is flagged for a policy
  review. Incognito chats are not used to improve Claude. Where improvement is on, data may be kept in de-identified
  form for up to five years; where it is off, a deleted chat leaves the back end within 30 days. Raw content fetched
  through a connector is excluded unless you copy it into the chat.
- **Commercial plans (Team, Enterprise, the API).** By default Anthropic does not use inputs or outputs from
  commercial products to train models. Inputs and outputs are deleted within 30 days by default, with custom retention or
  zero data retention available where agreed.
- **Both.** Giving feedback with the thumbs buttons stores the conversation for up to five years, de-linked from your
  identity. Content flagged for a Usage Policy violation can be kept for up to two years, and trust-and-safety scores
  up to seven.

Two product rules to remember from module 7 and module 8: the memory feature is not available to organisations with
HIPAA, public-sector or custom retention agreements, and Claude in Chrome is not available to HIPAA organisations and
is not recommended on pages with regulated data. A tool's availability can itself be a compliance signal.

## Traps

1. **"It is internal, so it is fine."** Internal use leaves the data class unchanged. A regulated field stays regulated.
2. **Telling Claude not to keep the data.** An instruction to forget is not a control; the control is what you upload,
   the plan's data terms and your organisation's settings.
3. **Treating a pseudonym as anonymous.** A code with a key held elsewhere is reversible, and rare combinations can
   identify people without names.

## Quiz

1. An HR coordinator wants Claude to summarise survey comments by department. The comments include employees' names
   and health details, and policy restricts regulated personal data. Which step fits?
   - **a**: Upload the comments unchanged and tell Claude to leave all names out of the summary
   - **b**: Get a manager's approval and upload the file to a tool the team already likes
   - **c**: Swap each name for a code and treat the file as anonymous
   - **d**: Strip identifiers and medical specifics, keeping the grouping field

2. A team uses Claude to draft decision letters about loan applications that go straight to applicants. Under the Usage
   Policy which pair of safeguards applies to this use?
   - **a**: A qualified reviewer before sending, and disclosure that AI helped
   - **b**: A reviewer who samples letters after sending, and an AI notice in each
   - **c**: A second model's approval of each letter, and removal of applicants' names
   - **d**: A loan officer's sign-off on each letter, and no AI notice after that

3. A consultant says: "These sales figures are for internal analysis only, so the customer identities can stay in
   the file." The analyst must compare how each customer group's spending changed between two years, and policy restricts regulated personal data.
   What is the best response?
   - **a**: Drop the names but keep each customer's exact purchase dates and home town
   - **b**: Keep the file as it is, since internal analysis lowers the data's class to internal
   - **c**: Aggregate to totals per segment and per period, then remove names and account numbers
   - **d**: Keep the file and ask Claude not to retain the figures after the session ends

<details>
<summary>Answer key</summary>

1. **d**. The task can proceed if the data changes: remove identifiers and sensitive specifics, keep what the
   summary needs, then check for re-identification. *a* is ruled out because asking Claude to leave names out "is a
   request, and it does not satisfy a policy control". *b* is ruled out because the organisation's policy "decides which tools and data are permitted at work", and a manager's liking for a tool does not change that. *c* is ruled out because "Pseudonymised
   data is still personal data under many rules, because the key can reverse it".
2. **a**. Financial decisions that affect individuals are high-risk: a qualified professional reviews them and recipients are told AI helped. *b* is ruled out because the review must happen "before it is disseminated or finalised", and a sample checked after sending comes too late. *c* is ruled out because the reviewer must be a "qualified professional in the field", and a second model is not one. *d* is ruled out because when output is presented directly to individuals "you must disclose that AI helped produce it", whoever reviewed it.
3. **c**. Aggregation is the strongest technique, a comparison of groups needs totals and not rows, and the class is set by
   the data. *a* is ruled out because "a rare job title, a small town and an exact date can identify a person without a
   name", and a comparison of groups needs neither the dates nor the towns. *b* is ruled out because "internal use does not change the class of the data". *d* is ruled out
   because asking Claude not to retain the file "is a request, and it does not satisfy a policy control".

</details>
