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
