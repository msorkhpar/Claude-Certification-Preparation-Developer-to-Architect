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
is the context window doing its job. Apps with memory features or Projects can look as if they learn
across chats, but they work the same way: saved notes are put back into the context, and the weights
stay as they were. The glossary adds a scoped fact about changing the weights: "The Claude API does not
currently offer fine-tuning, but ask your Anthropic contact if you are interested in exploring this
option." The statement is about the Claude API only. Amazon Bedrock, a cloud platform, offered
fine-tuning of one older model, Claude 3 Haiku, first as a preview (2024-07) and then, from 2024-11-01, as a generally available feature in the US West (Oregon) region (the AWS "What's New" post "Fine-tuning for Anthropic's Claude 3 Haiku in Amazon Bedrock is now generally available"; the AWS
Machine Learning Blog post "Fine-tune Anthropic's Claude 3 Haiku in Amazon Bedrock to boost model accuracy
and quality", dated 2024-07-10, which now carries a notice that Claude 3 Haiku reached end of life on
2026-09-10); the Anthropic page "Claude in Amazon Bedrock" lists no fine-tuning among its supported features, and none of the models in the table of
module 3 is offered for fine-tuning on the Claude API. Changing the weights is a separate act from calling
the model, not something an API call does.

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

The example trains a toy version on ten words and shows the effect. It is a teaching sketch with six
merges, not Claude's tokenizer.

<!-- example: m2-toy-bpe tabs: python,typescript -->
```python
"""A toy byte-pair tokenizer. It is not Claude's tokenizer: it shows why tokens are not words."""
from collections import Counter


def train(corpus, merges):
    """Learn merge rules: repeatedly join the most frequent adjacent pair (ties: first seen)."""
    words = [list(w) for w in corpus.split()]
    rules = []
    for _ in range(merges):
        pairs = Counter()
        for w in words:
            for a, b in zip(w, w[1:]):
                pairs[(a, b)] += 1
        if not pairs:
            break
        best = max(pairs, key=lambda p: (pairs[p], -list(pairs).index(p)))
        rules.append(best)
        words = [_merge(w, best) for w in words]
    return rules


def _merge(word, pair):
    out, i = [], 0
    while i < len(word):
        if i + 1 < len(word) and (word[i], word[i + 1]) == pair:
            out.append(word[i] + word[i + 1])
            i += 2
        else:
            out.append(word[i])
            i += 1
    return out


def encode(word, rules):
    pieces = list(word)
    for rule in rules:
        pieces = _merge(pieces, rule)
    return pieces


def main():
    corpus = "low low low lower lower lowest newest newest widest widest"
    rules = train(corpus, 6)
    print("merges:", " ".join("+".join(r) for r in rules))
    for word in ["low", "lowest", "newer", "widest", "lowish"]:
        print(f"{word:7} -> {' | '.join(encode(word, rules))}")


if __name__ == "__main__":
    main()
```
```text
merges: l+o lo+w e+s es+t low+e lowe+r
low     -> low
lowest  -> low | est
newer   -> n | e | w | e | r
widest  -> w | i | d | est
lowish  -> low | i | s | h
```
```typescript
// A toy byte-pair tokenizer. It is not Claude's tokenizer: it shows why tokens are not words.
export type Pair = [string, string];

function merge(word: string[], pair: Pair): string[] {
  const out: string[] = [];
  let i = 0;
  while (i < word.length) {
    if (i + 1 < word.length && word[i] === pair[0] && word[i + 1] === pair[1]) {
      out.push(word[i] + word[i + 1]);
      i += 2;
    } else {
      out.push(word[i]);
      i += 1;
    }
  }
  return out;
}

// Learn merge rules: repeatedly join the most frequent adjacent pair (ties: first seen).
export function train(corpus: string, merges: number): Pair[] {
  let words = corpus.split(/\s+/).filter(Boolean).map((w) => [...w]);
  const rules: Pair[] = [];
  for (let n = 0; n < merges; n++) {
    const counts = new Map<string, number>();
    for (const w of words) {
      for (let i = 0; i + 1 < w.length; i++) {
        const key = w[i] + "\u0000" + w[i + 1];
        counts.set(key, (counts.get(key) ?? 0) + 1);
      }
    }
    if (counts.size === 0) break;
    let best = "";
    let bestCount = -1;
    for (const [key, count] of counts) {
      if (count > bestCount) {
        best = key;
        bestCount = count;
      }
    }
    const pair = best.split("\u0000") as Pair;
    rules.push(pair);
    words = words.map((w) => merge(w, pair));
  }
  return rules;
}

export function encode(word: string, rules: Pair[]): string[] {
  let pieces = [...word];
  for (const rule of rules) pieces = merge(pieces, rule);
  return pieces;
}

export function main(): void {
  const corpus = "low low low lower lower lowest newest newest widest widest";
  const rules = train(corpus, 6);
  console.log("merges:", rules.map((r) => r.join("+")).join(" "));
  for (const word of ["low", "lowest", "newer", "widest", "lowish"]) {
    console.log(`${word.padEnd(7)} -> ${encode(word, rules).join(" | ")}`);
  }
}

if (process.argv[1] && import.meta.url.endsWith(process.argv[1].split("/").pop()!)) main();
```
```text
merges: l+o lo+w e+s es+t low+e lowe+r
low     -> low
lowest  -> low | est
newer   -> n | e | w | e | r
widest  -> w | i | d | est
lowish  -> low | i | s | h
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

1. An assistant is asked to tally the letter r in a long part number and gives a wrong, confident figure.
   The team wants a fix that holds for every part number. Which approach is best?
   - **a**: Hand the counting to a small script and use its output
   - **b**: Add a system line demanding an exact character count every time
   - **c**: Move to a model with a larger context window available
   - **d**: Run the same question five times and take the most common figure

2. A team corrects the assistant by hand after it repeats a mistake across many chats, and expects it to do
   better next week. They call the API directly and store no history. Which expectation is accurate?
   - **a**: The weights absorb the fix and apply it to every later session by default
   - **b**: A tuned copy of the model is created under the account for later calls
   - **c**: Only a request that carries the amended guidance in its own input can benefit
   - **d**: The provider's overnight training folds the chats into the model

<details>
<summary>Answer key</summary>

1. **a**. The model works on pieces, not letters, so a character-level tally is an approximation and a small program answers it exactly (the tokenizer section and the first trap). *b* is ruled out because because "a model answers it by approximation", so a demand for exactness does not change how the part number is cut into pieces. *c* is ruled out because because "The window is how much text fits per request", a capacity that does not change how a part number is cut into pieces. *d* is ruled out because because "Questions about individual characters (how many letters, which letter is third, reverse this string) are harder for it than they look", so repeating the question repeats the weakness.
2. **c**. Weights are fixed at inference and what looks like learning is the context window (the weights section), so only a call that carries the fix in its input can use it. *a* is ruled out because because "A conversation with Claude does not teach it anything that carries to the next conversation". *b* is ruled out because because "The Claude API does not currently offer fine-tuning", so no tuned copy is made by calling it. *d* is ruled out because because "Weights are fixed at inference time; a later session starts from the same model", so chats do not change it next week.

</details>
