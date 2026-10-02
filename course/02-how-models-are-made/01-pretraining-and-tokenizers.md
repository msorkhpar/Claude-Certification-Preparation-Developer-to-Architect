# Pretraining and tokenizers

**Level:** Foundations · **Module 2:** How models are made · **Page 1 of 2**
**Exams:** X (beyond the exam blueprints; the ideas behind AS3, DV2 and DV4 questions)

**After this page you can** describe in plain words how a language model is pretrained, explain why a
tokenizer splits text into pieces that are not words, and say what that does to counting and spelling
tasks.

Checked against the Anthropic glossary on 2026-10-02 and by running the toy tokenizer below in the course
container. This module is concepts only: there is no training practice, and nothing here describes the
internals of any particular Claude model.

## Why it matters

No exam asks you to train a model. They do ask why a model cannot count the letters in a word, why a prompt
in one language costs more than the same prompt in another, and why a model "knows" something it was never
shown by you. The answers sit one level down, in how the model was made. A reader who has the picture
makes better calls about what to ask a model and what to hand to code.

## The idea

### Pretraining: learn to continue text

The glossary defines pretraining as "the initial process of training language models on a large unlabeled
corpus of text", where, for Claude's kind of model, the model is trained "to predict the next word, given
the previous context of text in the document".

Source: Glossary, Claude API documentation.

Read that as a game played billions of times. Take a piece of text, hide the next token, ask the model to
guess it, and adjust the model's numbers (its **weights**) so the guess gets slightly better. Nobody labels
the data; the text itself is the answer key. To get good at continuing text of every kind, the model has to
absorb grammar, facts, styles, code, reasoning patterns, and the shape of arguments, because all of these
make the next token more predictable.

Two things follow that the glossary states and the exams use:

- A pretrained model "is not inherently good at answering questions or following instructions". It
  continues text. Asked a question, it may continue with more questions, like a list on a quiz page.
- Everything it knows came from the corpus up to a cut-off date (page 2 of module 1), and nothing in it
  can be looked up like a database record. Facts are spread across the weights as tendencies, which is
  why recall can be wrong in a fluent way.

### The weights do not change while you chat

Training adjusts weights. Using the model (called inference) does not. A conversation with Claude does not
teach it anything that carries to the next conversation; what looks like learning inside one conversation
is the context window doing its job. This is also why the glossary notes that the Claude API "does not
currently offer fine-tuning" and tells readers to ask their Anthropic contact: changing the weights is a
separate act from calling the model, not something an API call does.

### Tokenizers: how text becomes numbers

A model works on numbers, so text must be cut into pieces and each piece mapped to an integer. The cutting
is done by a **tokenizer**, which is built before pretraining and fixed afterwards. The glossary: "Larger
tokens enable data efficiency during inference and pretraining (and are used when possible), while smaller
tokens allow a model to handle uncommon or never-before-seen words."

A common way to build one is **byte-pair encoding**. Start with single characters. Count which neighbouring
pair appears most in a large corpus, join that pair into a new piece, and repeat many thousands of times.
Frequent words end up as one piece; rare words are built from several smaller pieces; any text at all can
still be spelled out from the smallest pieces. The result is a vocabulary of sub-word units, which is why
tokens are "not words".

The example trains a toy version on eleven words and shows the effect. It is a teaching sketch with six
merges, not Claude's tokenizer.

<!-- example: m2-toy-bpe tabs: python,typescript -->
```python
EXAMPLE_PYTHON
```
```text
EXAMPLE_OUT_PY
```
```typescript
EXAMPLE_TS
```
```text
EXAMPLE_OUT_TS
```
<!-- /example -->

(Java and Kotlin readers: the logic is a loop over a list of strings and ports directly.)

Read the output: the frequent word `low` became a single token, `lowest` is two, and `lowish`, a word the
corpus never contained, still encodes, from smaller pieces. Real tokenizers behave the same way at a much
larger scale.

### What the tokenizer does to your work

- **Counting and spelling.** The model never sees the letters of `lowest` as six separate things; it sees
  two pieces. Questions about individual characters (how many letters, which letter is third, reverse this
  string) are harder for it than they look. Hand them to code.
- **Cost and limits by language and content.** The glossary says a Claude token is about 3.5 English
  characters and "the exact number can vary depending on the language used". Code, numbers and languages
  that were rare in the corpus tend to need more tokens per character.
- **Tokenizer changes are model changes.** A new tokenizer means different counts for the same text,
  which module 1 showed for the Claude 4.7 generation and later.

## Traps

1. **Asking for exact character work.** "How many r's are in this word?" is a tokenizer-level question. A
   one-line program answers it exactly; a model answers it by approximation.
2. **Equating a larger window with a larger vocabulary.** The window is how much text fits per request; the
   vocabulary is the set of pieces the tokenizer knows. They are unrelated settings.
3. **Believing the model learns from your chats.** Weights are fixed at inference time; a later session
   starts from the same model unless the provider releases a new one.

## Quiz

1. A user asks an assistant to count how many times a particular letter appears in a long product
   identifier and gets a wrong, confident number. What explains the failure, and what is the sound remedy?
   - **a**: The model sees sub-word pieces, so exact tallies belong in code
   - **b**: The window ran out, so the code should be shortened
   - **c**: The sampler was too cold, so it should be raised
   - **d**: The weights were stale, so the model should be retrained

2. A team says: "Our assistant made the same mistake in forty chats last week, so after we corrected it in
   chat it should now know better." Which statement is accurate?
   - **a**: Corrections in a conversation update its weights for later sessions
   - **b**: Corrections persist only if the API temperature is lowered
   - **c**: Corrections apply within that conversation's context only
   - **d**: Corrections are saved once the model is fine-tuned by default

<details>
<summary>Answer key</summary>

1. **a**. The tokenizer section explains that the model works on pieces, not letters, so character-level tallies are approximate; code counts exactly (the first trap). *b* is ruled out because a short code fits any window and the error is not a cut-off. *c* is ruled out because sampling temperature changes variety, not letter-level access. *d* is ruled out because retraining does not change how text is cut into pieces for a given model.
2. **c**. What looks like learning is the context window; weights are fixed at inference (the weights section). *a* is ruled out for the same reason. *b* is ruled out because temperature has no storage role. *d* is ruled out because the glossary says the API does not currently offer fine-tuning and, in any case, fine-tuning is a deliberate separate process.

</details>
