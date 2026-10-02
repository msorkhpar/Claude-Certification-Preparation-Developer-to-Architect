# What Claude does well, and where it slips

**Level:** Foundations · **Module 4:** Capabilities and limits · **Page 1 of 2**
**Exams:** DV2, DV4 (and the judgment behind AS2, AS6)

**After this page you can** say which kinds of task suit Claude and which need a second mechanism, for
reasoning, coding, long documents, images, numbers and counting, and recent events, and name the fix for
each weakness.

Checked against the Anthropic documentation on 2026-10-02 (models overview, vision, context windows, token
counting, reduce hallucinations). Claims about weaknesses are limited to what those pages state or what
follows directly from them; each is marked.

## Why it matters

"Can Claude do this?" is the wrong question for an engineer; the useful one is "what must be around Claude
for this to be reliable?" Exam scenarios hide the answer in a weakness: a count that is off, a scanned
table misread, a fact after the cut-off, a very long input. Knowing the weakness points at the fix.

## The idea

### The broad picture

The models overview says current Claude models "excel in" top-tier results across reasoning, coding,
multilingual tasks, long-context handling, honesty and image processing, and that all current models
"support text and image input, text output, multilingual capabilities, vision, and tool use". Those are
strengths to build on. The rest of this page is where the pages themselves say to be careful.

Source: Models overview, Claude API documentation.

### Reasoning

Claude works through multi-step problems well, and the way you ask matters. Module 6 teaches prompting for
reasoning; page 2 of this module covers thinking at a glance. The limit to know: the reasoning is generated
text, produced token by token. A confident chain of steps can contain a wrong step that makes everything
after it wrong, so reasoning that decides something important needs a check that does not come from the same
reasoning (a test, a calculation in code, a second reader).

### Coding

Code generation, explanation and review are core uses; the models overview lists complex agentic coding as
the purpose of Opus 5.5 and everyday code generation for Sonnet 5.5. The limits are those of any
unreviewed contribution: code can look right and not run, call a function that does not exist (a
hallucination of the kind module 1 described), or pass the example it was shown and fail the next one.
What fixes it is the machinery engineers already own: run it, test it, review it. Claude Code (module 38)
is built around exactly that loop.

### Long documents

The window can hold up to 1M tokens on three of the four current models. Two cautions from the context
windows page: "more context isn't automatically better", because accuracy and recall degrade as the window
fills (context rot), and cost grows with every token you send. Practical rules, developed in modules 6, 29
and 64: put long documents first and the question last (the vision page makes the same point for text,
"placing long documents before your query improves results"), ask for supporting quotes, and send only what
the question needs.

### Vision

Claude reads images and PDFs. The vision documentation lists limits in plain terms, and they are exam
material:

- **People.** Claude cannot be used to name people in images, and refuses to.
- **Accuracy.** It "might hallucinate or make mistakes when interpreting low-quality, rotated, or very
  small images under 200 pixels".
- **Spatial reasoning.** Coordinates and localisation outputs are approximate.
- **Counting.** It "can give approximate counts of objects in an image but might not always be precisely
  accurate, especially with large numbers of small objects".
- **Synthetic images.** It "cannot determine whether an image is AI-generated".
- **Medical images.** It is "not designed to interpret complex diagnostic scans such as CTs or MRIs".
- **Generation.** Claude "is an image understanding model only"; it cannot create or edit images.

Source: Vision, Claude API documentation. The page's own closing advice: "Always carefully review and
verify Claude's image interpretations, especially for high-stakes use cases. The page adds: "Do not use Claude for tasks
requiring perfect precision or sensitive image analysis without human oversight."

Images also cost tokens: each 28 by 28 pixel patch is one visual token, so an image costs about
`ceil(width/28) x ceil(height/28)` tokens up to a per-model cap. A 1000 by 1000 image is 1,296 tokens on the
page's table. Module 30 teaches this in code.

### Maths and counting

This weakness is a consequence of two facts you already have, rather than a quotation. The model produces
text token by token (page 1 of module 1), and it sees sub-word pieces, not characters (module 2). So
exact arithmetic on long numbers, exact counts of letters, words or items, and exact string manipulation are
approximate unless the model does the work in a tool. The documentation's vision page says the same of
counting objects in pictures. The fix is not a better prompt; it is code. Give the model a calculator or a
code-execution tool, or have it write the program and run it yourself.

### Recency and private knowledge

The model's knowledge stops at its cut-off (June 2026 for the three large models, February 2025 reliable
for Haiku 4.5, on the models table) and it has no clock and no access to your systems. Anything recent,
private or fast-changing has to come in through the prompt, a retrieval step (module 28) or a tool
(module 26). Asked without them, it answers from what it has, which is how a stale or invented answer is
produced with a confident voice.

### The honest summary

| Task | Claude alone | What to add |
|---|---|---|
| Draft, summarise, translate, explain | Strong | Review for accuracy and fit |
| Write and refactor code | Strong | Tests, running it, review |
| Reason through a problem | Strong | An independent check on the decisive step |
| Analyse a long document | Strong, with care | Quotes, trimming, document first |
| Read an image or PDF | Strong, with limits | Verify, mind the listed weaknesses |
| Exact arithmetic, counts, string edits | Unreliable | A calculator or code tool |
| Recent or private facts | Absent | Context, retrieval or a tool |

## Traps

1. **Fixing an exact-count error by prompting harder.** "Count carefully" does not change that the model
   sees pieces. Move the counting into code.
2. **Trusting an image reading because it is detailed.** Detail is not accuracy; the vision page lists the
   conditions under which it is wrong.
3. **Filling the window because it is there.** A bigger window is capacity, not a reason to send everything;
   context rot makes curation part of the design.

## Quiz

1. A finance team pastes forty invoice amounts into a prompt and asks Claude for the sum. The reply reads
   fluently and is off by a few cents. What is the most dependable correction?
   - **a**: Let it call a code tool and pass on the tool's result
   - **b**: Add a closing line asking Claude to flag how sure it is of the total
   - **c**: Reformat the amounts into one column so the sum is easier to follow
   - **d**: Ask for the sum again with an instruction to double-check each step

2. An inspection app sends shelf photographs and asks Claude how many bottles are visible. Totals are
   sometimes wrong on crowded shelves. Which design response fits what the documentation says?
   - **a**: Shrink each photo heavily to simplify the scene, which makes the counts dependable
   - **b**: Crop each photo to a single row, which makes the counts exact
   - **c**: Label the figures approximate and verify the important ones against real stock
   - **d**: Switch to a larger model, which makes the counts dependable

<details>
<summary>Answer key</summary>

1. **a**. Exact arithmetic is a task for a tool, because the model works on tokens (the maths and counting section), and the first trap says to move such work into code. *d* is ruled out because a re-check request "does not change that the model sees pieces", and the section says the fix is not a better prompt. *b* is ruled out because reasoning "needs a check that does not come from the same reasoning", and the model's own report of certainty is not one. *c* is ruled out because exact arithmetic on long numbers is "approximate unless the model does the work in a tool", however the amounts are laid out.
2. **c**. The vision page says counts can be approximate, and its closing advice is to verify interpretations in high-stakes cases, so the figures are labelled as estimates and the ones that matter are checked. *b* is ruled out because because Claude "can give approximate counts of objects in an image but might not always be precisely accurate", whatever the crop. *a* is ruled out because because Claude "might hallucinate or make mistakes when interpreting low-quality, rotated, or very small images under 200 pixels", so heavy shrinking makes it worse. *d* is ruled out because because the page says "Do not use Claude for tasks requiring perfect precision or sensitive image analysis without human oversight", for any tier.

</details>
