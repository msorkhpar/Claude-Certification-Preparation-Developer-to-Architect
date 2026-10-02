# Fine-tuning, feedback training and what training gives

**Level:** Foundations · **Module 2:** How models are made · **Page 2 of 2**
**Exams:** X (beyond the exam blueprints; the ideas behind AS3, AS6 and DV4 questions)

**After this page you can** explain how a pretrained model becomes an assistant, name the transformer and
attention at the level of an idea, and list what training does and does not give a model.

Checked against the Anthropic glossary, the Constitutional AI research page and the constitution pages on 2026-10-02. Concepts only: no training practice, and no claim
about the internals of a specific Claude model.

## Why it matters

When a model refuses a request, apologises too much, or follows an instruction it was never given, the
explanation is usually in how it was trained to behave, not in the prompt. Knowing the stages lets you tell
a behaviour you can change with a prompt from one you cannot, and lets you answer exam questions that ask
what a model "knows", "remembers" or "was trained to do" without guessing.

## The idea

### The stages, in order

1. **Pretraining** (page 1): learn to continue text on a very large corpus. The result is capable and
   unruly.
2. **Fine-tuning:** "further training a pretrained language model using additional data. This causes the
   model to start representing and mimicking the patterns and characteristics of the fine-tuning dataset."
   The glossary adds: "Claude is not a bare language model; it has already been fine-tuned to be a helpful
   assistant."
3. **Feedback training.** Two ideas are worth knowing.

Source: Glossary, Claude API documentation.

### Learning from human preferences (RLHF)

Reinforcement learning from human feedback is described in the glossary as a technique "used to train a
pretrained language model to behave in ways that are consistent with human preferences". Humans rank two or
more example answers; "the reinforcement learning process encourages the model to prefer outputs that are
similar to the higher-ranked ones." This is how helpfulness and instruction following get layered on top
of raw text continuation.

### Learning from written principles (Constitutional AI)

Preference data from humans is expensive, hard to scale and hard to inspect. Anthropic's research on
**Constitutional AI** replaces much of it with a short written list of principles, a "constitution", and
trains in two phases:

1. **A supervised phase, critique and revision.** "We sample from an initial model, then generate
   self-critiques and revisions, and then finetune the original model on revised responses." The model
   critiques its own answers against the principles and rewrites them; the revised answers become
   training data.
2. **A reinforcement learning phase, from AI feedback (RLAIF).** The refined model produces pairs of
   answers, "a model" judges which of the two is better under the principles, and a **preference model**
   is trained from this dataset of AI preferences. That preference model then supplies the reward signal
   for reinforcement learning, the role human rankings play in RLHF.

The research page states the consequence for harmlessness: no human labels are needed for it, and "the
only human oversight is provided through a list of rules or principles."

Source: Constitutional AI: Harmlessness from AI Feedback, Anthropic research.

The principles are public and have been rewritten. Anthropic published a new version of Claude's
constitution on 2026-01-21, a detailed description of its intentions for Claude's values and behaviour.
It prefers judgment to rigid rules: "We generally favor cultivating good values and judgment over strict
rules and decision procedures", and "we try to explain any rules we do want Claude to follow", so that
Claude can understand the reasoning behind a rule rather than only obey it.

Sources: Claude's constitution, and the 2026-01-21 update note on Anthropic's constitution news page.

The idea to keep for the exams is the effect: behaviour is shaped by stated principles that people can
read, not only by thousands of unexplained rankings. The glossary describes the framework behind this as HHH, **helpful, honest,
harmless**: a helpful AI tries to do the task well; an honest one gives accurate information and
acknowledges "its limitations and uncertainties"; a harmless one declines to assist with dangerous or
unethical acts and says why. The glossary calls HHH "a research framework that informs how Claude is trained",
a goal and a direction, not a certificate on any single reply.

### The transformer and attention, as an idea

Almost every modern language model is built on the **transformer** architecture. The one idea worth carrying
is **attention**: when the model decides what token comes next, every position in the context can look at
every earlier position and weigh how relevant each one is. That is how a pronoun finds its noun and how a
question finds the sentence that answers it.

Two practical consequences, both used later in the course:

- Attention is a **limited resource spread across the context**. As the context grows, relevant material
  can compete with irrelevant material, which the context windows page names *context rot* ("accuracy and
  recall degrade" as token count grows). This is why where you place information and how much you include
  both matter (modules 6, 29 and 64).
- Everything the model uses is **in the window or in the weights**. There is no third place such as a
  notebook it consults. If it needs a fact you have, put the fact in the window.

### What training gives, and what it does not

| Training gives | Training does not give |
|---|---|
| Fluent language, many languages, code | A guarantee that any statement is true |
| Broad general knowledge to a cut-off date | Knowledge of anything after the cut-off |
| Following instructions and a helpful manner | Access to your files, systems or today's date |
| Tendencies toward honest, harmless replies | A rule that can never be broken |
| Reasoning patterns it can apply to new problems | Perfect arithmetic, counting or spelling |
| A fixed set of weights | Memory of your previous conversations |

Read the right-hand column as the reason for the rest of the course: knowledge you need, tools you need and
guarantees you need all come from what you build around the model.

## Traps

1. **Treating "trained to be honest" as "never wrong".** The glossary frames HHH as how Claude is trained,
   not a promise about each answer. Verification is still the user's job.
2. **Thinking fine-tuning is the default way to add knowledge.** The glossary says the Claude API does not
   currently offer fine-tuning (a statement about the Claude API, not about every platform); the standard route is to put the knowledge in the context (module 28 covers
   retrieval).
3. **Assuming a refusal or a style is a prompt bug.** Some behaviours come from training. A prompt can
   steer within them, but an instruction that fights the training may not win.

## Quiz

1. A stakeholder asks why a deployed assistant answers a question with an answer, when the raw model it
   started from tends to continue the question with more questions. Which part of how models are made does
   most to explain the gap?
   - **a**: Pretraining on a far larger corpus than the raw model saw
   - **b**: A larger attention mechanism that lets the model reread the question
   - **c**: A separate look-up store of answers that the assistant consults
   - **d**: A later round of tuning on curated examples and replies ranked by people

2. A company wants Claude to answer from its product manual, which is revised every month, through the Claude
   API. An engineer proposes fine-tuning on each new version. What is the best response?
   - **a**: Include the current edition in every call, since the context is the standard route
   - **b**: Go ahead with monthly tuning, then patch the gaps through corrections made in chat
   - **c**: Tune the model once on the manual so its weights keep the facts for good
   - **d**: Place the edition in the prompt and also tune the model, to be safe

<details>
<summary>Answer key</summary>

1. **d**. Fine-tuning and feedback training are the stages that turn a text continuer into an assistant (the stages section). *a* is ruled out because the stage list says of pretraining that "The result is capable and unruly", and a bigger corpus does not change that. *b* is ruled out because in attention "every position in the context can look at every earlier position", in the raw model as much as in the assistant. *c* is ruled out because "There is no third place such as a notebook it consults": everything the model uses is in the window or in the weights.
2. **a**. Knowledge that changes monthly belongs in the context, and the glossary says the Claude API does not currently offer fine-tuning (the second trap and the table). *b* is ruled out because the table lists "Memory of your previous conversations" among the things training does not give, so chat corrections patch nothing. *c* is ruled out because the table credits training with "Broad general knowledge to a cut-off date" and nothing newer, so weights cannot track a monthly revision. *d* is ruled out because "the Claude API does not currently offer fine-tuning", so the added step is not available through this route.

</details>

## Module quiz

This quiz covers both pages of the module.

1. An analyst taught Claude a set of internal abbreviations in a chat last week. In a new chat today it
   ignores them, and the analyst concludes something broke. Which explanation fits?
   - **a**: Unfamiliar abbreviations were split into odd pieces, so the model could not match them again
   - **b**: Weights stay fixed in use, so the earlier session left nothing behind to reuse
   - **c**: The context window filled up last week, so the oldest abbreviations were pushed out
   - **d**: Chats feed a nightly fine-tuning round, and this week's round skipped the abbreviations

2. A localisation lead notices that one paragraph needs noticeably more tokens in one language than in
   another. Which explanation fits best?
   - **a**: The model silently translates the harder language before reading it, which adds pieces
   - **b**: Preference training from human rankings sets how many pieces each language costs
   - **c**: Its learned vocabulary stores longer chunks for the script that dominated the corpus
   - **d**: The translated text simply contains more words, and tokens track words one to one

3. A reviewer waves through a number in a report because the assistant is built to be honest. Which reply is
   the soundest?
   - **a**: Honesty goals cover harmful topics only, so numbers are exempt
   - **b**: A system prompt can switch the honesty framework on for a task
   - **c**: Honesty work verifies each figure against its sources before release
   - **d**: That describes a goal, not a guarantee, so key claims still need checking

4. A team is automating four chores. Which one should be done by a deterministic program instead of the model
   alone?
   - **a**: Verifying every product code is exactly eight characters long
   - **b**: Rewriting release notes for a non-technical audience of customers
   - **c**: Proposing likely causes for an intermittent failing test
   - **d**: Sorting incoming emails by urgency from the message text

<details>
<summary>Answer key</summary>

1. **b**. Weights do not change while you chat, so a new conversation has no trace of the old one (page 1, weights section). *a* is ruled out because the tokenizer copes with unseen text, since "a word the corpus never contained, still encodes, from smaller pieces", and the model only needs the abbreviations in its input (page 1). *c* is ruled out because "what looks like learning inside one conversation is the context window doing its job", and a new chat starts with an empty window (page 1). *d* is ruled out because "A conversation with Claude does not teach it anything that carries to the next conversation", so no nightly round learns from chats (page 1).
2. **c**. The vocabulary was learned from a corpus, so a script that was common gets longer pieces and a rarer one splits into more (page 1, what the tokenizer does to your work). *a* is ruled out because "text must be cut into pieces and each piece mapped to an integer" before the model reads anything, so the count is set by the cutting and not by a hidden translation step (page 1). *b* is ruled out because the tokenizer is "built before pretraining and fixed afterwards", so later feedback training cannot change what a language costs (page 1). *d* is ruled out because a token is not a word: "a Claude token is about 3.5 English characters", and "the exact number can vary depending on the language used" (page 1).
3. **d**. The glossary frames HHH as how Claude is trained, and the first trap says verification is still the user's job. *a* is ruled out because the honest part of HHH means "an honest one gives accurate information and acknowledges" its limits, so it reaches beyond harmful topics. *b* is ruled out because "an instruction that fights the training may not win", so a system prompt cannot switch a training framework on. *c* is ruled out because the table lists "A guarantee that any statement is true" among the things training does not give.
4. **a**. Exact character work is a tokenizer-level weakness, so code should do it (page 1, first trap). *b* is ruled out because the table credits training with "Fluent language, many languages, code", which covers rewording for customers. *c* is ruled out because the table credits training with "Reasoning patterns it can apply to new problems", which covers proposing causes. *d* is ruled out because the table lists "Following instructions and a helpful manner", and a model reads message text well.

</details>
