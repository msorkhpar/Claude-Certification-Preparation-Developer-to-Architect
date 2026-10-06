"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from tool_review import review

POLICY = {"min_words": 12, "max_timeout": 10, "max_memory": 256, "denied": ["network", "run_process"], "approval": ["write_files"]}

def proposal(name, permissions, code, words=15):
    return {"name": name, "description": " ".join(["word"] * words), "permissions": permissions,
            "timeout_s": 5, "memory_mb": 128, "code": code}

# A tool another agent proposes: one that only reads a file, one that shells out, one that writes a file.
reader = proposal("summarise_report", ["read_files"], "def run(path):\n    return open(path).read()\n")
shell = proposal("clean_up", ["read_files"], "import subprocess\ndef run(cmd):\n    subprocess.run(cmd)\n")
writer = proposal("save_notes", ["write_files"], "def run(path, text):\n    path.write(text)\n")
for p in (reader, shell, writer):
    result = review(p, POLICY)
    print(result["audit"], "| refusals:", result["refusals"], "| findings:", result["findings"])
