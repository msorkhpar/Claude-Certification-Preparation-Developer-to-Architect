#!/usr/bin/env python3
"""Write exercises/<module>/tests/quiz.json from the quiz sections of the module's pages.

The page is the source: stems, options and the folded key (best option, then one reason per other option).
tools/check_quiz.py then checks the file against the page and the wording rules.
usage: tools/build_quiz_json.py
"""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_quiz import ROOT, parse_page_quizzes  # noqa: E402

QID = {"Quiz": "q", "Module quiz": "m", "Mock exam": "x"}
SCOPE = {"Quiz": "page", "Module quiz": "module", "Mock exam": "level"}
GROUP = re.compile(r"(?:\*[a-d]\*(?:, and |, | and )?)+")


def explanations(key_paragraph, key_letter):
    text = key_paragraph.strip()
    text = re.sub(r"^\*\*[a-d]\*\*\.\s*", "", text)
    out = {}
    matches = list(GROUP.finditer(text))
    first = matches[0].start() if matches else len(text)
    out[key_letter] = text[:first].strip()
    for i, m in enumerate(matches):
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        reason = text[m.end():end].strip()
        reason = re.sub(r"^(is|are) ruled out because ", "Ruled out because ", reason)
        for letter in re.findall(r"\*([a-d])\*", m.group(0)):
            out[letter] = reason
    return out


def key_paragraphs(md):
    result = []
    for m in re.finditer(r"^## (Quiz|Module quiz|Mock exam)\n(.*?)(?=^## |\Z)", md, re.S | re.M):
        _, _, keyblock = m.group(2).partition("<details>")
        paras = re.findall(r"^\d+\. (\*\*[a-d]\*\*.*?)(?=^\d+\. |\n</details>|\Z)", keyblock, re.S | re.M)
        result.append(paras)
    return result


def main():
    for folder in sorted(p for p in (ROOT / "course").iterdir() if p.is_dir() and re.match(r"(0[1-9]|1[0-9]|2[0-3])-", p.name)):
        quizzes = []
        for page in sorted(folder.glob("*.md")):
            md = page.read_text()
            parsed = parse_page_quizzes(md)
            keyparas = key_paragraphs(md)
            for (kind, questions, keys), paras in zip(parsed, keyparas):
                for n, ((stem, opts), key, para) in enumerate(zip(questions, keys, paras), start=1):
                    quizzes.append({
                        "id": f"{page.stem}#{QID[kind]}{n}",
                        "page": page.name,
                        "scope": SCOPE[kind],
                        "stem": stem,
                        "options": opts,
                        "key": key,
                        "explanation": explanations(para, key),
                    })
        out = ROOT / "exercises" / folder.name / "tests"
        out.mkdir(parents=True, exist_ok=True)
        (out / "quiz.json").write_text(json.dumps({"module": folder.name, "quizzes": quizzes}, indent=2, ensure_ascii=False) + "\n")
        print(f"{folder.name}: {len(quizzes)} questions")


if __name__ == "__main__":
    main()
