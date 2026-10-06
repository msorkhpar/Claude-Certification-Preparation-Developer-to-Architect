#!/usr/bin/env python3
"""Rebuild exercises/<module>/tests/quiz.json for the named module prefixes only, with the repo's own builder logic.
usage: python3 build_one.py <repo root> <module prefix> [...]"""
import json
import re
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
sys.path.insert(0, str(root / "tools"))
import build_quiz_json as b  # noqa: E402

prefixes = sys.argv[2:]
for folder in sorted(p for p in (root / "course").iterdir() if p.is_dir() and re.match(r"(0[1-9]|[1-9][0-9])-", p.name)):
    if not any(folder.name.startswith(x) for x in prefixes):
        continue
    quizzes = []
    for page in sorted(folder.glob("*.md")):
        md = page.read_text()
        for (kind, questions, keys), paras in zip(b.parse_page_quizzes(md), b.key_paragraphs(md)):
            for n, ((stem, opts), key, para) in enumerate(zip(questions, keys, paras), start=1):
                entry = {"id": f"{page.stem}#{b.QID[kind]}{n}", "page": page.name, "scope": b.SCOPE[kind], "stem": stem,
                         "options": opts, "key": key, "explanation": b.explanations(para, key)}
                if b.select_count(stem):
                    entry["select"] = b.select_count(stem)
                quizzes.append(entry)
    out = root / "exercises" / folder.name / "tests"
    out.mkdir(parents=True, exist_ok=True)
    (out / "quiz.json").write_text(json.dumps({"module": folder.name, "quizzes": quizzes}, indent=2, ensure_ascii=False) + "\n")
    print(f"{folder.name}: {len(quizzes)} questions")
