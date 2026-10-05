#!/usr/bin/env python3
"""Check the quizzes of Levels 1 to 3 (every module so far): pages agree with quiz.json, shape and wording rules hold.

Rules (CLAUDE.md quiz rules that a script can check):
  - every question has four options a-d and a key among them; a multiple-response question (stem ends
    "(Select two.)") has five options a-e and exactly that many keyed letters, "**a and c**." in the folded key;
  - the key option shares no content word (after simple stemming, hyphens split) with the stem, and no stem is
    contained in a longer word on the other side ("send" in "resend", "want" in "unwanted");
  - the key is at most 1.3 times the mean length of the distractors, in characters (warning when the key is the
    longest option in more than 40 percent of a module's questions);
  - the folded key gives every option its own explanation sentence (no merged "a, b, c are ..." sentence);
  - the folded key on the page names the same letter as quiz.json, in the same order;
  - quiz.json explains every option;
  - every non-key option's explanation quotes, in double quotes, a phrase of at least 4 words, and every quoted
    phrase of 4 words or more in it appears verbatim on the page the quiz closes (for a module quiz: on any page of
    the module; for a mock or pool item: on any page in its level scope; quiz sections excluded);
  - the folded key on the page quotes the same phrases as quiz.json;
  - no doubled adjacent word in a stem, option or explanation; no stem or option ends on a preposition, article or
    conjunction (cut-off text);
  - a module question's stem shares at most half of its content stems with any page question of the same module;
  - form tell: not all three distractors carry an absolute marker (always, never, only, every, all, none, ...) while
    the key carries none, and the key is not the only hedged option (some, may, can, usually, where needed);
  - the key is the longest option in at most 40 percent of a module's questions (warning above 30 percent); the mock
    exam is counted on its own questions;
  - no two questions of the course so far have stem+key token sets with Jaccard similarity of 0.5 or more;
  - the stem holds no evaluative word that names the key's quality (balanced, safest, proper, correct way, ...).
  - every page carries a quiz section (Quiz or Mock exam), except an exam-readiness page: a page whose header line
    reads "**Module N:** Exam readiness ..." teaches how the exams work, not a topic an exam scenario tests, so it
    has no quiz; its module's mock exam pages are quizzes and are checked as usual.
usage: tools/check_quiz.py [module-folder-prefix ...]   exit 1 on any finding
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STOP = set("""a an the and or but of to in on at for from with by as is are was were be been being it its this that these those
not no do does did can could should would will may might must than then so if when what which who whom whose how why where
into over under about after before between each every any all some one two three four more most less least only also just
their there they them he she his her you your we our i me my us has have had get got use used using via per""".split())


def words(text):
    return {w for w in re.findall(r"[a-z]+(?:'[a-z]+)?", text.lower().replace("’", "'")) if w not in STOP and len(w) > 2}


SUFFIXES = ("ers", "ions", "ments", "ing", "ion", "ment", "ed", "er", "es", "ly", "s")


def stemmed(word):
    """Lowercase stem: strip one common suffix and a doubled final consonant; kept when it has 4 letters or more."""
    for suf in SUFFIXES:
        if word.endswith(suf) and len(word) - len(suf) >= 3:
            word = word[: -len(suf)]
            break
    if len(word) > 3 and word[-1] == word[-2] and word[-1] not in "aeiou":
        word = word[:-1]
    return word if len(word) >= 4 else None


def stems(text):
    return {x for x in (stemmed(w) for w in words(text)) if x}


def stem_of(stem):
    return words(re.sub(r"`[^`]*`", " ", stem))


QID = {"Quiz": "q", "Module quiz": "m", "Mock exam": "x"}
# A mock question is checked against the prose of the modules up to the number given here (44 when a module is not listed).
MOCK_SCOPE = {78: 77, 94: 93}
# Mock modules whose key explanation must name the page that answers it, as (module N, page M).
NAMED_PAGE_MOCKS = ("44", "78", "94")
MIN_QUOTE_WORDS = 4
MAX_STEM_OVERLAP = 0.5
QUOTE = re.compile(r'"([^"]+)"')


def contained_echo(key_text, stem_text):
    """Pairs (stem, longer word) where a stem of one side sits inside a longer word of the other side."""
    hits = set()
    for s in stems(stem_text):
        hits |= {(s, w) for w in words(key_text) if s in w and len(w) > len(s)}
    for s in stems(key_text):
        hits |= {(s, w) for w in words(stem_text) if s in w and len(w) > len(s)}
    return hits


def norm(text):
    """Page and quotation text for verbatim matching: no markup, curly quotes straightened, one space, lower case."""
    text = text.replace("’", "'").replace("“", '"').replace("”", '"')
    return re.sub(r"\s+", " ", re.sub(r"[*`]", "", text)).strip().lower()


READINESS = re.compile(r"^\*\*Level:\*\*.*\*\*Module \d+:\*\* Exam readiness\b", re.M)


def check_page_has_quiz(name, md):
    """An ordinary page has a quiz section; an exam-readiness page (module title 'Exam readiness') may have none."""
    if READINESS.search(md) or parse_page_quizzes(md):
        return []
    return [f"{name}: no quiz section (only an exam-readiness page may go without)"]


def prose_of(md):
    """The page text before its quiz sections."""
    return re.split(r"^## (?:Quiz|Module quiz|Mock exam)\n", md, maxsplit=1, flags=re.M)[0]


def check_quotes(qid, explanation, key, prose):
    keyset = {key} if isinstance(key, str) else set(key)
    """Every non-key option is ruled out by a quotation of 4 or more words that appears verbatim in the prose."""
    problems = []
    page = norm(prose)
    for letter in sorted(explanation):
        problems += [p for p in check_text_shape(f"{qid} option {letter} explanation", explanation[letter])
                     if "doubled" in p]
        if letter in keyset:
            continue
        quotes = [q for q in QUOTE.findall(explanation[letter]) if len(q.split()) >= MIN_QUOTE_WORDS]
        if not quotes:
            problems.append(f"{qid}: option {letter} has no quoted phrase of {MIN_QUOTE_WORDS} words or more in its explanation")
        else:
            problems += [f"{qid}: option {letter} quotes {q!r}, which is not verbatim on the page"
                         for q in quotes if norm(q) not in page]
    return problems


def check_key_quotes(qid, para, explanation):
    """The folded key on the page quotes the same phrases as the quiz.json explanations (a multiple-response item
    repeats the key's explanation under each keyed letter, so the phrases are compared as sets)."""
    on_page = {norm(q) for q in QUOTE.findall(para.replace("“", '"').replace("”", '"'))}
    in_json = {norm(q) for text in explanation.values() for q in QUOTE.findall(text)}
    if on_page != in_json:
        return [f"{qid}: the folded key on the page and quiz.json quote different phrases"]
    return []


def check_named_page(qid, text):
    """A Developer mock question's key explanation names the page that answers it, as (module N, page M), and quotes
    a phrase of 4 or more words that appears verbatim in that page's prose."""
    m = re.search(r"\(module (\d+), page (\d+)\)", text)
    if not m:
        return [f"{qid}: the key explanation does not name its page as (module N, page M)"]
    folders = sorted((ROOT / "course").glob(f"{int(m.group(1)):02d}-*"))
    pages = sorted(folders[0].glob("*.md")) if folders else []
    if not pages or not 1 <= int(m.group(2)) <= len(pages):
        return [f"{qid}: the named page (module {m.group(1)}, page {m.group(2)}) does not exist"]
    named = norm(prose_of(pages[int(m.group(2)) - 1].read_text()))
    quotes = [q for q in QUOTE.findall(text) if len(q.split()) >= MIN_QUOTE_WORDS]
    if not any(norm(q) in named for q in quotes):
        return [f"{qid}: the key explanation quotes nothing of 4 or more words from the named page (module {m.group(1)}, page {m.group(2)})"]
    return []


def check_duplicate(qid, stem, page_stems):
    """A module question must not restate a page question of the same module."""
    mine = stems(re.sub(r"`[^`]*`", " ", stem))
    for other_id, other in page_stems.items():
        theirs = stems(re.sub(r"`[^`]*`", " ", other))
        if mine and len(mine & theirs) / len(mine) > MAX_STEM_OVERLAP:
            return [f"{qid}: stem shares {len(mine & theirs)} of {len(mine)} content stems with {other_id}"]
    return []


LENGTH_RATIO = 1.3
LONGEST_WARN = 0.3
LONGEST_FAIL = 0.4
ABSOLUTE = re.compile(r"\b(always|never|only|every|all|none|nil|ignore[sd]?|ignoring|forbid\w*|regardless|guaranteed?|exact(?:ly)?)\b", re.I)
HEDGE = re.compile(r"\b(some|may|can|usually|where needed)\b", re.I)
GIVEAWAY = re.compile(r"\b(balanced|safest|proper(?:ly)?|correct way|right way|best[- ]practices?)\b", re.I)
NEAR_DUP = 0.5


def check_form_tell(qid, opts, key):
    """The key must not be the only option of its kind: all-absolute distractors, or a lone hedged key."""
    others = [v for k, v in opts.items() if k != key]
    problems = []
    if len(others) == 3 and all(ABSOLUTE.search(v) for v in others) and not ABSOLUTE.search(opts[key]):
        problems.append(f"{qid}: form tell: all three distractors carry an absolute marker, the key none")
    if HEDGE.search(opts[key]) and not any(HEDGE.search(v) for v in others):
        problems.append(f"{qid}: form tell: the key alone is hedged")
    return problems


def check_giveaway(qid, stem):
    """The stem must not carry an evaluative word that names the key's quality."""
    hit = GIVEAWAY.search(re.sub(r"`[^`]*`", " ", stem))
    return [f"{qid}: stem word {hit.group(1).lower()!r} names the key's quality"] if hit else []


def jaccard(a, b):
    return len(a & b) / len(a | b) if a | b else 0.0


def check_near_duplicates(items):
    """items: {qid: (stem, key text)}; every pair with Jaccard similarity of stem+key stems >= NEAR_DUP is a finding."""
    sets = {qid: stems(re.sub(r"`[^`]*`", " ", stem) + " " + key) for qid, (stem, key) in items.items()}
    ids = sorted(sets)
    return [f"{a} and {b}: near-duplicate questions (Jaccard {jaccard(sets[a], sets[b]):.2f})"
            for i, a in enumerate(ids) for b in ids[i + 1:] if jaccard(sets[a], sets[b]) >= NEAR_DUP]


def longest_verdict(label, hits, total):
    """Return (finding, warning) for the share of questions whose key is the longest option."""
    if not total:
        return None, None
    share = hits / total
    msg = f"{label}: key is the longest option in {hits} of {total} questions ({share:.0%})"
    if share > LONGEST_FAIL:
        return msg + f", limit {LONGEST_FAIL:.0%}", None
    return None, (msg if share > LONGEST_WARN else None)


def key_length_ratio(opts, key):
    others = [len(v) for k, v in opts.items() if k != key]
    return len(opts[key]) / (sum(others) / len(others)) if others else 0.0


def key_is_longest(opts, key):
    return all(len(opts[key]) > len(v) for k, v in opts.items() if k != key)


TRUNCATION_ENDINGS = set("in on at to of for with from by the a an and or but".split())
WORD = re.compile(r"[A-Za-z']+")


def check_text_shape(label, text):
    """A doubled adjacent word, or an ending on a preposition, article or conjunction (cut-off text)."""
    problems = []
    plain = re.sub(r"`[^`]*`", " ", text)
    dup = re.search(r"\b([A-Za-z']+)[ \t]+\1\b", plain, re.I)
    if dup:
        problems.append(f"{label}: doubled word {dup.group(1).lower()!r}")
    tail = WORD.findall(re.sub(r"[^A-Za-z']+$", "", text))
    if label.split()[-1] != "explanation" and tail and tail[-1].lower() in TRUNCATION_ENDINGS:
        problems.append(f"{label}: ends with {tail[-1]!r}, looks truncated")
    return problems


def check_question(qid, stem, opts, key):
    """Wording findings for one question (stem, {letter: text}, key letter)."""
    problems = check_text_shape(f"{qid} stem", stem)
    for letter, text in sorted(opts.items()):
        problems += check_text_shape(f"{qid} option {letter}", text)
    if key not in opts:
        return problems
    plain = re.sub(r"`[^`]*`", " ", stem)
    shared = words(opts[key]) & stem_of(stem)
    if shared:
        problems.append(f"{qid}: key repeats stem words {sorted(shared)}")
    echoed = stems(opts[key]) & stems(plain)
    if echoed and not shared:
        problems.append(f"{qid}: key echoes the stem by stem {sorted(echoed)}")
    inside = contained_echo(opts[key], plain)
    if inside and not shared and not echoed:
        problems.append(f"{qid}: key echoes the stem inside a longer word {sorted(inside)}")
    problems += check_form_tell(qid, opts, key)
    problems += check_giveaway(qid, stem)
    ratio = key_length_ratio(opts, key)
    if ratio > LENGTH_RATIO:
        problems.append(f"{qid}: key is {ratio:.2f} times the mean distractor length (limit {LENGTH_RATIO})")
    return problems


SELECT = re.compile(r"\(Select (two|three|four|five|six|seven|eight|nine|[2-9])\.\)\s*$")
WORDNUM = {"two": 2, "three": 3, "four": 4, "five": 5, "six": 6, "seven": 7, "eight": 8, "nine": 9}


def select_count(stem):
    """N when the stem ends with the marker (Select N.), else None."""
    m = SELECT.search(stem)
    if not m:
        return None
    w = m.group(1)
    return WORDNUM[w] if w in WORDNUM else int(w)


def check_multi(qid, stem, opts, key):
    """Findings for one question that may be multiple-response: the marker and the keyed letters agree, N is below
    the number of options, a multiple-response item has options a-e, and its keys obey the wording rules."""
    keys = [key] if isinstance(key, str) else list(key)
    n = select_count(stem)
    problems = []
    if len(keys) == 1 and n is not None:
        problems.append(f"{qid}: the stem says (Select {n}.) but the key has one letter")
        return problems
    if len(keys) > 1 and n is None:
        problems.append(f"{qid}: the key has {len(keys)} letters but the stem has no (Select N.) marker")
        return problems
    if len(keys) == 1:
        return check_question(qid, stem, opts, keys[0])
    if n != len(keys):
        problems.append(f"{qid}: the stem says (Select {n}.) but the key has {len(keys)} letters")
    if len(set(keys)) != len(keys) or keys != sorted(keys):
        problems.append(f"{qid}: key letters {keys} must be distinct and in alphabetical order")
    if len(keys) >= len(opts):
        problems.append(f"{qid}: {len(keys)} keyed letters of {len(opts)} options leaves no distractor")
        return problems
    if sorted(opts) != list("abcde"):
        problems.append(f"{qid}: a multiple-response question has options a-e, found {sorted(opts)}")
    if any(k not in opts for k in keys):
        return problems
    problems += check_text_shape(f"{qid} stem", stem)
    for letter, text in sorted(opts.items()):
        problems += check_text_shape(f"{qid} option {letter}", text)
    plain = re.sub(r"`[^`]*`", " ", re.sub(SELECT, "", stem))
    for k in keys:
        shared = words(opts[k]) & stem_of(plain)
        if shared:
            problems.append(f"{qid}: key {k} repeats stem words {sorted(shared)}")
        elif stems(opts[k]) & stems(plain):
            problems.append(f"{qid}: key {k} echoes the stem by stem {sorted(stems(opts[k]) & stems(plain))}")
        elif contained_echo(opts[k], plain):
            problems.append(f"{qid}: key {k} echoes the stem inside a longer word {sorted(contained_echo(opts[k], plain))}")
    problems += check_giveaway(qid, stem)
    others = [len(v) for k, v in opts.items() if k not in keys]
    ratio = (sum(len(opts[k]) for k in keys) / len(keys)) / (sum(others) / len(others))
    if ratio > LENGTH_RATIO:
        problems.append(f"{qid}: the keys are {ratio:.2f} times the mean distractor length (limit {LENGTH_RATIO})")
    if all(ABSOLUTE.search(opts[k]) is None for k in keys) and all(ABSOLUTE.search(v) for k, v in opts.items() if k not in keys):
        problems.append(f"{qid}: form tell: every distractor carries an absolute marker, no key does")
    if all(HEDGE.search(opts[k]) for k in keys) and not any(HEDGE.search(v) for k, v in opts.items() if k not in keys):
        problems.append(f"{qid}: form tell: the keys alone are hedged")
    return problems


def keys_longest(opts, key):
    """True when the longest option is a keyed one (for a multiple-response question), else the single-key rule."""
    keys = [key] if isinstance(key, str) else list(key)
    top = max(len(v) for v in opts.values())
    return any(len(opts[k]) == top and sum(1 for v in opts.values() if len(v) == top) == 1 for k in keys)


MOCK_TELL_MODULES = ("11", "44", "78", "94")
REASON = re.compile(r"(,\s+(since|because|as)\b|\bso that\b)", re.I)
AUXILIARY = set("is are was were has have had does do did will would can could should must may might".split())
FINITE_VERBS = set("""loads load runs run returns return reads read keeps keep stays stay holds hold counts count applies apply
allows allow blocks block refuses refuse costs cost needs need fails fail works work shows show makes make gives give
takes take sets set sends send writes write changes change covers cover requires require uses use treats treat passes pass
drops drop moves move expands expand matches match asks ask stops stop becomes become comes come goes go carries carry""".split())


def reason_clause(text):
    return bool(REASON.search(text))


def main_part(text):
    return re.split(r"[,;]", text, maxsplit=1)[0]


def has_finite_verb(text):
    """Conservative: a finite auxiliary or a listed verb after a subject, anywhere in the option."""
    return any(w.lower() in AUXILIARY or w.lower() in FINITE_VERBS for w in WORD.findall(text)[1:])


def option_tokens(text):
    return frozenset(re.sub(r"[^a-z0-9 ]+", " ", text.lower()).split())


def check_mock_item(qid, opts, key):
    """Tells that single out the key of a mock or pool item: reason clauses, identical options, an odd form."""
    keys = [key] if isinstance(key, str) else list(key)
    rest = [k for k in sorted(opts) if k not in keys]
    problems = []
    keyed = [reason_clause(opts[k]) for k in keys]
    others = [reason_clause(opts[k]) for k in rest]
    if (any(others) and not any(keyed)) or (any(keyed) and not any(others)):
        problems.append(f"{qid}: reason-clause asymmetry: the key set {'carries' if any(keyed) else 'lacks'} a reason clause that the other options {'lack' if any(keyed) else 'carry'}")
    letters = sorted(opts)
    for i, a in enumerate(letters):
        for b in letters[i + 1:]:
            ta, tb = option_tokens(opts[a]), option_tokens(opts[b])
            if ta == tb or (len(ta | tb) and len(ta & tb) / len(ta | tb) >= 0.8):
                problems.append(f"{qid}: options {a} and {b} are duplicates or near duplicates")
    if len(keys) == 1 and rest:
        lead = lambda text: [w for w in WORD.findall(text) if w.lower() not in ("a", "an", "the")][:1]
        firsts = [lead(opts[k]) for k in rest]
        if all(firsts) and len({f[0].lower() for f in firsts}) == 1 and [w.lower() for w in lead(opts[keys[0]])] != [firsts[0][0].lower()]:
            problems.append(f"{qid}: the key is the odd one out in form: every other option opens with {firsts[0][0]!r}")
        aux = {k: bool(AUXILIARY & {w.lower() for w in WORD.findall(opts[k])[1:]}) for k in opts}
        verbs = {k: has_finite_verb(opts[k]) for k in opts}
        if all(aux[k] for k in rest) and not verbs[keys[0]]:
            problems.append(f"{qid}: the key is the odd one out in form: a phrase among full clauses")
        if not any(verbs[k] for k in rest) and aux[keys[0]]:
            problems.append(f"{qid}: the key is the odd one out in form: a full clause among phrases")
    return problems


MOCK_EXTREME = 0.25


def check_mock_extremes(label, rows):
    """rows: [(opts, key letter)] for the single-answer items of one mock page."""
    problems = []
    total = len(rows)
    longest = sum(1 for o, k in rows if all(len(o[k]) > len(v) for x, v in o.items() if x != k))
    shortest = sum(1 for o, k in rows if all(len(o[k]) < len(v) for x, v in o.items() if x != k))
    for name, hits in (("longest", longest), ("shortest", shortest)):
        if total and hits / total > MOCK_EXTREME:
            problems.append(f"{label}: key is the {name} option in {hits} of {total} single-answer items ({hits / total:.0%}), limit {MOCK_EXTREME:.0%}")
    return problems


def check_key_paragraph(qid, para, key, letters="abcd"):
    """The folded key: key letter(s) first, then one sentence per other option, none merged."""
    problems = []
    keys = [key] if isinstance(key, str) else list(key)
    text = re.sub(r"^\*\*[a-e](?: and [a-e])*\*\*\.\s*", "", para.strip())
    groups = re.findall(r"(?:\*[a-e]\*(?:, and |, | and )?)+", text)
    singles = [g for g in groups if len(re.findall(r"\*([a-e])\*", g)) == 1]
    merged = [g for g in groups if len(re.findall(r"\*([a-e])\*", g)) > 1]
    if merged:
        problems.append(f"{qid}: folded key merges options ({merged[0].strip()}); give each its own sentence")
    found = sorted(re.findall(r"\*([a-e])\*", " ".join(singles)))
    want = sorted(set(letters) - set(keys))
    if found != want and not merged:
        problems.append(f"{qid}: folded key explains {found}, want one sentence each for {want}")
    return problems


def key_paragraphs(md):
    result = []
    for m in re.finditer(r"^## (Quiz|Module quiz|Mock exam)\n(.*?)(?=^## |\Z)", md, re.S | re.M):
        _, _, keyblock = m.group(2).partition("<details>")
        result.append(re.findall(r"^\d+\. (\*\*[a-e](?: and [a-e])*\*\*.*?)(?=^\d+\. |\n</details>|\Z)", keyblock, re.S | re.M))
    return result


def parse_page_quizzes(md):
    """Return a list of (heading, [(stem, {letter: text})], [key letters]) per quiz section."""
    out = []
    for m in re.finditer(r"^## (Quiz|Module quiz|Mock exam)\n(.*?)(?=^## |\Z)", md, re.S | re.M):
        body = m.group(2)
        qblock, _, keyblock = body.partition("<details>")
        questions = []
        for q in re.finditer(r"^\d+\. (.*?)(?=^\d+\. |\Z)", qblock, re.S | re.M):
            lines = q.group(1).strip().split("\n")
            stem_lines, opts, last = [], {}, None
            for line in lines:
                om = re.match(r"\s*- \*\*([a-e])\*\*: (.*)", line)
                if om:
                    opts[om.group(1)] = om.group(2).strip()
                    last = om.group(1)
                elif not opts:
                    stem_lines.append(line.strip())
                elif last and line.strip():
                    opts[last] += " " + line.strip()
            questions.append((" ".join(stem_lines), opts))
        keys = [k if " and " not in k else k.split(" and ") for k in re.findall(r"^\d+\. \*\*([a-e](?: and [a-e])*)\*\*", keyblock, re.M)]
        out.append((m.group(1), questions, keys))
    return out


def check_module(folder):
    problems = []
    qj = ROOT / "exercises" / folder.name / "tests" / "quiz.json"
    if not qj.exists():
        return [f"{folder.name}: missing {qj.relative_to(ROOT)}"]
    data = json.loads(qj.read_text())
    by_id = {q["id"]: q for q in data["quizzes"]}
    seen = set()
    longest = {}
    all_items = {}
    mock_rows = {}
    pages = sorted(folder.glob("*.md"))
    module_prose = "\n".join(prose_of(p.read_text()) for p in pages)
    page_stems = {}
    for page in pages:
        for kind, questions, _ in parse_page_quizzes(page.read_text()):
            if kind == "Quiz":
                for n, (stem, _) in enumerate(questions, start=1):
                    page_stems[f"{page.stem}#q{n}"] = stem
    level_prose, level_stems = None, {}
    if any(k == "Mock exam" for pg in pages for k, _, _ in parse_page_quizzes(pg.read_text())):
        top = MOCK_SCOPE.get(int(folder.name[:2]), 44)
        in_scope = lambda f: f.is_dir() and re.match(r"\d\d-", f.name) and int(f.name[:2]) <= top
        level_prose = "\n".join(prose_of(pg.read_text()) for f in sorted(ROOT.joinpath("course").iterdir())
                                if in_scope(f) for pg in sorted(f.glob("*.md")))
        for f in sorted(ROOT.joinpath("course").iterdir()):
            if in_scope(f):
                for pg in sorted(f.glob("*.md")):
                    for kind, questions, _ in parse_page_quizzes(pg.read_text()):
                        for n, (stem, _) in enumerate(questions, start=1):
                            level_stems[f"{pg.stem}#{QID[kind]}{n}"] = stem
    for page in pages:
        md = page.read_text()
        problems += check_page_has_quiz(page.name, md)
        for kind, questions, keys in parse_page_quizzes(md):
            if len(keys) != len(questions):
                problems.append(f"{page.name}: {kind} has {len(questions)} questions but {len(keys)} keys")
            for n, (stem, opts) in enumerate(questions, start=1):
                qid = f"{page.stem}#{QID[kind]}{n}"
                q = by_id.get(qid)
                if q is None:
                    problems.append(f"{qid}: not in quiz.json")
                    continue
                seen.add(qid)
                want_letters = "abcde" if select_count(stem) else "abcd"
                if sorted(opts) != list(want_letters):
                    problems.append(f"{qid}: options are {sorted(opts)}, want {want_letters[0]}-{want_letters[-1]}")
                if q["stem"] != stem or q["options"] != opts:
                    problems.append(f"{qid}: page text differs from quiz.json")
                key = keys[n - 1] if n - 1 < len(keys) else None
                if key != q["key"]:
                    problems.append(f"{qid}: page key {key} differs from quiz.json key {q['key']}")
                if sorted(q.get("explanation", {})) != list(want_letters):
                    problems.append(f"{qid}: quiz.json must explain every option {want_letters[0]}-{want_letters[-1]}")
                if q.get("select") != select_count(stem):
                    problems.append(f"{qid}: quiz.json select {q.get('select')} differs from the page marker {select_count(stem)}")
                problems += check_multi(qid, stem, opts, q["key"])
                if kind == "Mock exam" and folder.name[:2] in MOCK_TELL_MODULES:
                    problems += check_mock_item(qid, opts, q["key"])
                    if isinstance(q["key"], str):
                        mock_rows.setdefault(page.stem, []).append((opts, q["key"]))
                scope_prose = {"Quiz": prose_of(md), "Module quiz": module_prose, "Mock exam": level_prose}[kind]
                problems += check_quotes(qid, q.get("explanation", {}), q["key"], scope_prose)
                if kind == "Module quiz":
                    problems += check_duplicate(qid, stem, page_stems)
                if kind == "Mock exam":
                    problems += check_duplicate(qid, stem, {k: v for k, v in level_stems.items() if not k.startswith(page.stem + "#")})
                    if folder.name[:2] in NAMED_PAGE_MOCKS:
                        problems += check_named_page(qid, q.get("explanation", {}).get(q["key"] if isinstance(q["key"], str) else q["key"][0], ""))
                paras = key_paragraphs(md)
                idx = [k for k, _, _ in parse_page_quizzes(md)].index(kind)
                if n - 1 < len(paras[idx]):
                    problems += check_key_paragraph(qid, paras[idx][n - 1], q["key"], want_letters)
                    problems += check_key_quotes(qid, paras[idx][n - 1], q.get("explanation", {}))
                grp = longest.setdefault("mock exam" if kind == "Mock exam" else "module", [0, 0])
                grp[0] += keys_longest(opts, q["key"])
                grp[1] += 1
    for page_name, rows in sorted(mock_rows.items()):
        problems += check_mock_extremes(page_name, rows)
    for qid in by_id:
        if qid not in seen:
            problems.append(f"{qid}: in quiz.json but not on any page")
    for name, (hits, total) in sorted(longest.items()):
        finding, warning = longest_verdict(f"{folder.name} ({name})", hits, total)
        if finding:
            problems.append(finding)
        if warning:
            print(f"warning: {warning}")
    keys_used = [q["key"] for q in data["quizzes"] if isinstance(q["key"], str)]
    for letter in "abcd":
        if keys_used and keys_used.count(letter) > (len(keys_used) + 1) // 2:
            problems.append(f"{folder.name}: key letter {letter} is overused ({keys_used.count(letter)} of {len(keys_used)})")
    return problems


def main(argv):
    folders = sorted(p for p in (ROOT / "course").iterdir() if p.is_dir() and re.match(r"(0[1-9]|[1-9][0-9])-", p.name))
    if argv:
        folders = [f for f in folders if any(f.name.startswith(a) for a in argv)]
    total = 0
    items = {}
    for f in sorted(p for p in (ROOT / "course").iterdir() if p.is_dir() and re.match(r"(0[1-9]|[1-9][0-9])-", p.name)):
        for pg in sorted(f.glob("*.md")):
            for kind, questions, keys in parse_page_quizzes(pg.read_text()):
                for n, ((stem, opts), key) in enumerate(zip(questions, keys), start=1):
                    ks = [key] if isinstance(key, str) else key
                    if all(k in opts for k in ks):
                        items[f"{f.name[:2]}/{pg.stem[:2]}#{QID[kind]}{n}"] = (stem, " ".join(opts[k] for k in ks))
    for p in check_near_duplicates(items):
        print(p)
        total += 1
    for f in folders:
        probs = check_module(f)
        total += len(probs)
        for p in probs:
            print(p)
        print(f"{f.name}: {'ok' if not probs else str(len(probs)) + ' finding(s)'}")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
