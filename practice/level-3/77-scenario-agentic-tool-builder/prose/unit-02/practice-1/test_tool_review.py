import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from tool_review import review

POLICY = {"min_words": 12, "max_timeout": 10, "max_memory": 256, "denied": ["network", "run_process"], "approval": ["write_files"]}
READ = "def run(path):\n    return open(path).read()\n"


def proposal(name="summarise_report", words=15, permissions=("read_files",), timeout_s=5, memory_mb=128, code=READ):
    return {"name": name, "description": " ".join(["word"] * words), "permissions": list(permissions), "timeout_s": timeout_s, "memory_mb": memory_mb, "code": code}


def out(name, decision, refusals=(), findings=(), used=("read_files",)):
    return {"name": name, "decision": decision, "refusals": list(refusals), "findings": list(findings), "used": list(used), "audit": f"{name}: {decision}"}


def test_m1_a_well_formed_read_only_tool_is_approved_and_leaves_an_audit_line():
    assert review(proposal(), POLICY) == out("summarise_report", "approve")


def test_e1_a_description_needs_at_least_the_minimum_number_of_words():
    assert review(proposal(words=12), POLICY)["findings"] == []
    short = review(proposal(words=11), POLICY)
    assert short["findings"] == ["short_description"] and short["decision"] == "revise"


def test_e2_the_timeout_and_the_memory_may_equal_their_limits_and_not_exceed_them():
    assert review(proposal(timeout_s=10, memory_mb=256), POLICY)["findings"] == []
    assert review(proposal(timeout_s=11), POLICY)["findings"] == ["timeout"]
    assert review(proposal(memory_mb=257), POLICY)["findings"] == ["memory"]


def test_e3_a_name_is_lower_case_snake_case_of_at_most_sixty_four_characters():
    assert review(proposal(name="a" * 64), POLICY)["findings"] == []
    assert review(proposal(name="a" * 65), POLICY)["findings"] == ["bad_name"]
    assert review(proposal(name="Summarise"), POLICY)["findings"] == ["bad_name"]
    assert review(proposal(name="ab"), POLICY)["findings"] == ["bad_name"]


def test_e4_forbidden_calls_in_the_code_refuse_the_tool_and_are_all_listed_in_order():
    code = READ + "eval(text)\nos.system('ls')\n"
    result = review(proposal(code=code), POLICY)
    assert result["decision"] == "refuse" and result["refusals"] == ["forbidden:eval(", "forbidden:os.system"]


def test_e5_a_permission_the_code_uses_without_declaring_it_or_a_denied_one_refuses_the_tool():
    sneaky = review(proposal(code=READ + "requests.get(url)\n"), POLICY)
    assert sneaky["decision"] == "refuse" and sneaky["refusals"] == ["undeclared:network"] and sneaky["used"] == ["network", "read_files"]
    declared = review(proposal(permissions=("read_files", "network"), code=READ + "requests.get(url)\n"), POLICY)
    assert declared["decision"] == "refuse" and declared["refusals"] == ["denied:network"]


def test_e6_a_declared_write_is_approved_only_with_a_gate_and_a_read_alone_is_approved_outright():
    writer = review(proposal(permissions=("write_files",), code="out.write(text)\n"), POLICY)
    assert writer == out("summarise_report", "approve_with_gate", used=("write_files",))
    assert review(proposal(), POLICY)["decision"] == "approve"


def test_e7_a_refusal_beats_a_revision_and_a_revision_beats_a_gate():
    both = review(proposal(words=3, code=READ + "eval(text)\n"), POLICY)
    assert both["decision"] == "refuse" and both["findings"] == ["short_description"]
    revise = review(proposal(words=3, permissions=("read_files", "write_files"), code=READ + "out.write(text)\n"), POLICY)
    assert revise["decision"] == "revise"


def test_e8_the_permissions_the_code_uses_are_reported_in_alphabetical_order():
    result = review(proposal(permissions=("read_files", "write_files"), code="shutil.copy(a, b)\nopen(a).read()\n"), POLICY)
    assert result["used"] == ["read_files", "write_files"]
