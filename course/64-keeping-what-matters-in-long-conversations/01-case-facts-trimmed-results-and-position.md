# Case facts, trimmed results and where a fact sits

**Level:** Architect · **Module 64:** Keeping what matters in long conversations · **Page 1 of 2**
**Exams:** A5.1; S1, S3

**After this page you can** say what a progressive summary loses and keep the transactional facts of a case in a block outside it, trim a tool's output to the fields the next decision needs, place key findings and headings so that position effects do not hide them, and ask upstream agents for structured facts with their sources and dates when the agent downstream has a small budget.

Checked on 2026-10-04 against the exam guide's task statement 5.1 and the Claude API documentation page on prompting best practices (the long-context section), and against module 29's pages on context editing and compaction. Nothing here called a model: the example is the bookkeeping of a support conversation, written as plain code (`examples/64-case-facts`), and the numbers it prints come from its sample data. This page is about what to keep and where to put it. Module 29 covers the API features that clear or compact context, and this page does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* progressive summarisation risks "condensing numerical values, percentages, dates, and customer-stated expectations into vague summaries"; the "lost in the middle" effect, "models reliably process information at the beginning and end of long inputs but may omit findings from middle sections"; tool results that accumulate and "consume tokens disproportionately to their relevance (e.g., 40+ fields per order lookup when only 5 are relevant)"; and the need to pass "complete conversation history in subsequent API requests". Its skills are a persistent "case facts" block included in each prompt outside the summarised history, trimming verbose tool output, key findings at the beginning of aggregated inputs with explicit section headers, and subagent outputs that carry metadata (dates, source locations, methodology). *What the documentation says now (read 2026-10-04):* for long inputs, put "longform data at the top" of the prompt above the query, instructions and examples, with the query at the end ("Queries at the end can improve response quality by up to 30 percent in tests"), structure multi-document input with tags, and for long documents ask the model to quote the relevant parts first. The documentation gives the placement advice and the measured effect of the query's position; the named effect of the middle is the guide's, and the exam keys it. On the exam, the answers are: facts outside the summary, trimmed tool output, key findings first under headers.

## Why it matters

A support agent handles a refund over forty turns. At turn thirty the context holds the customer's first message, twenty order lookups of forty fields each, and a summary that was rewritten three times. The summary now says "the customer is waiting for a refund soon". The amount of $129.50 and the promise of an answer by 30 September are gone, and nobody notices until the agent quotes a different figure. Nothing failed in the API. The conversation outgrew its own memory and the part that was lost was the part that mattered. Scenarios S1 (support) and S3 (multi-agent research) test whether you design what the context keeps.

## The idea

### What a progressive summary loses

A summary written to save space keeps the story and drops the specifics. Each round compresses the previous summary, so a detail that survived once can be rounded off the next time. The details most at risk are the ones that an answer later depends on exactly: amounts, percentages, dates, order numbers, and what the customer said they expected. "A refund of $129.50 by 30 September" becomes "a refund soon". A reader of the summary cannot tell that anything was lost, which is why the loss has to be prevented and not noticed.

### Case facts outside the summary

The remedy is to take the transactional facts out of the text that gets summarised. The agent keeps a small structured block, the case facts: amount, dates, order numbers, statuses, the customer's stated expectation. The block is rebuilt into every request, in a fixed place, and it is updated by code or by a tool, not by a rewrite of prose. The summary then covers the story (what was discussed and tried), and the facts block covers the numbers. In a session with several issues, each issue has its own entry in the block, so that two refunds in one conversation never share an amount.

The example's `case_facts_block` renders such a block, one line per fact with the value and the day it was read. The day matters: a fact has an age, which the second page uses.

A check closes the loop. After a summary is written, compare it with the facts and flag any fact whose exact value no longer appears in it. The practice's `missing_from_summary` does exactly that, and a flagged summary is rewritten with the value restored or left to the block.

### Trim tool output before it accumulates

An order lookup returns forty fields, of which the return decision needs five. If the whole result stays in the conversation, it is carried and paid for on every later request, and twenty lookups put eight hundred fields where a hundred would do. The fix sits between the tool and the conversation: keep the fields that the decisions of this agent use, with their exact values (never rounded or reworded), and drop the rest. The example's `shrink` does it from a list per tool. Two cautions: trim by a list written for the decisions the agent makes, not by length (cutting the end of a record can cut the field that matters), and keep an identifier so that the full record can be fetched again if a later question needs a dropped field.

### Where a fact sits

The guide's position effect says that a long input's middle is where findings go missing. The practical answers are three, and the documentation backs the first and the last:

- Put the long material first and the question last. The question at the end can improve quality on complex inputs.
- Put the key findings and the case facts at the beginning of the aggregated input, as a short block, and then the detail.
- Organise the detail under explicit headers (and tags around each document), so that the model, and the next person reading the prompt, can find a section by name.

The example's `assemble` produces that order: case facts, key findings, the documents each under a header, and the question. Asking the model to quote the relevant passages before answering is a fourth tool for long documents.

### Complete history, and a small downstream budget

The Messages API keeps no conversation: every request carries the whole history it needs, and a request that sends only the newest message gets answers as though it were the first message. Trimming and summarising are therefore decisions about what to resend, and not ways to avoid resending. Resending the whole transcript is not the answer either: it is the cost that the summary was meant to avoid, and every tool result in it is paid for again.

The same discipline applies between agents. When a subagent's output feeds an agent with a small context budget, ask for structured data: the key facts, citations with their locations, relevance scores, and the dates the facts hold for, and not the full prose and the chain of reasoning. Page 2 of module 69 returns to this for provenance.

### The example

<!-- example: m64-case-facts tabs: python,typescript,java,kotlin -->
<!-- /example -->

## Traps

1. **"Summarise the conversation every few turns; the summary keeps everything important."** It is tempting because a summary reads as complete. The exam rejects it: each round rounds off amounts, dates and expectations into vague words. The facts go into a block outside the summary.
2. **"Keep every tool result in the conversation; the model can find what it needs."** It is tempting because nothing is lost. The exam rejects it: forty fields per lookup crowd out what matters and are paid for on every later request. Trim to the fields the decision uses.
3. **"Put the findings in the middle, after the raw documents, where they belong logically."** It is tempting because it follows the order of the work. The exam rejects it: the middle is where findings go missing. Key findings go first under a header, the question last.
4. **"Send only the latest message; the earlier ones are in the summary."** It is tempting because it is cheap. The exam rejects it: the API keeps no conversation, and the facts and the history the answer depends on must be in the request.

## Quiz

1. Scenario S1, customer support resolution agent. A refund agent condenses the conversation every ten turns. At turn forty the condensed text says only that the customer awaits "a refund soon"; the sum and the promised day from turn six are gone, and the agent quotes something else. Which change fits best?
   - **a**: Condense each fifth turn instead, so that less is rounded off in each round
   - **b**: Keep amounts, dates and statuses in a structured block on each request
   - **c**: Tell the condensing prompt to preserve each number that it meets in the text
   - **d**: Send the full transcript on each request in place of the condensed text

2. Scenario S1, customer support resolution agent. A lookup returns 42 fields, and the agent needs five of them to decide on a return. After thirty lookups in one conversation, answers are slower and costlier. Which change fits best?
   - **a**: Move to a model with a bigger window so that all thirty results fit
   - **b**: Cut each result to its first 500 characters before it is kept in the chat
   - **c**: Pass on the handful the decision uses, word for word, plus an id
   - **d**: Rewrite each result as one sentence of prose before it is kept in the chat

<details>
<summary>Answer key</summary>

1. **b**. The exact values leave the text that gets condensed, so no round of condensing can round them off. *a* is ruled out because "Each round compresses the previous summary, so a detail that survived once can be rounded off the next time", and more rounds means more rounding. *c* is ruled out because "A reader of the summary cannot tell that anything was lost", so an instruction inside the prose gives no check. *d* is ruled out because "Resending the whole transcript is not the answer either".
2. **c**. The decision's fields are kept with their exact values and the rest is dropped before it accumulates. *a* is ruled out because the full result "is carried and paid for on every later request", whatever the window. *b* is ruled out because the advice is to "trim by a list written for the decisions the agent makes, not by length". *d* is ruled out because the fields are kept "with their exact values (never rounded or reworded)".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
