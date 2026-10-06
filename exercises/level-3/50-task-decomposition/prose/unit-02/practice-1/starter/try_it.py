"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from decompose import review_changes

FILES = [{"path": "api.py", "text": "def get(): ..."}, {"path": "db.py", "text": "def query(): ..."}, {"path": "ui.py", "text": "def show(): ..."}]
SCRIPT = {"api": (["api: no auth"], "api calls db.query(id)"), "db": ([], "db.query takes a name"), "ui": (["ui: unused"], "ui shows rows")}


def file_pass(path, text, part, parts):
    """A scripted stand-in for the model reviewing one file (or one part of it)."""
    findings, summary = SCRIPT[path.split(".")[0]]
    return {"findings": findings, "summary": summary}


def cross_pass(summaries):
    """The second look: it reads only the summaries, and finds what no single file shows."""
    return ["api passes id but db expects a name"]


# Each file is reviewed alone, then the cross pass reads their summaries.
result = review_changes(FILES, file_pass, cross_pass) or {}

for path, review in (result.get("files") or {}).items():
    print(path, "->", review["findings"], "| parts:", review["parts"])
print("cross findings:", result.get("cross"))
print("failed:", result.get("failed"), "| skipped:", result.get("skipped"))
