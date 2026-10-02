import json
from pathlib import Path

import httpx2
import pytest

from harness import CaptureLeak, scan_text, scrub
from harness.capture import RecordingTransport
from harness.scrub import check_capture
from harness.scripted import message, scripted_client, text, ScriptedTransport
import anthropic

EXAMPLE = Path(__file__).resolve().parents[1] / "examples" / "illustrative_exchange.json"


def test_shipped_example_is_clean():
    check_capture(EXAMPLE)


@pytest.mark.parametrize("planted,kind", [
    ('"note": "key sk-ant-api03-AbCdEfGhIjKlMnOpQrSt"', "api key"),
    ('"note": "see req_011CXabcdefghijkl"', "request id"),
    ('"request_id": "x"', "request id"),
    ('"organization_id": "11111111-2222-3333-4444-555555555555"', "organisation or account id"),
    ('"note": "org_0123456789ab"', "organisation or account id"),
    ('"note": "owner someone@mail.test"', "email address"),
    ('"x-api-key": "abc"', "authorization header"),
])
def test_planted_leak_is_found(tmp_path, planted, kind):
    clean = EXAMPLE.read_text()
    dirty = clean.replace('"illustrative": true,', '"illustrative": true, ' + planted + ',', 1)
    assert dirty != clean, "plant did not change the file"
    p = tmp_path / "capture.json"
    p.write_text(dirty)
    assert kind in {k for k, _ in scan_text(dirty)}
    with pytest.raises(CaptureLeak):
        check_capture(p)


def test_placeholder_values_are_not_flagged():
    assert scan_text('{"email": "contact@example.com", "key": "placeholder"}') == []


def test_scrub_drops_identifying_keys_at_any_depth():
    out = scrub({"a": {"request_id": "r", "headers": {"x": 1}, "keep": 1}, "l": [{"organization_id": "o", "k": 2}]})
    assert out == {"a": {"keep": 1}, "l": [{"k": 2}]}


def test_recorder_scrubs_and_the_written_capture_passes_the_check(tmp_path):
    body = message([text("hi")])
    body["request_id"] = "req_AAAAAAAAAAAA"  # something a real error or beta body could carry
    inner = ScriptedTransport(body)
    rec = RecordingTransport(inner, "claude-sonnet-5-5", "anthropic 1.11.0", "2026-01-01")
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0, http_client=httpx2.Client(transport=rec))
    client.messages.create(model="claude-sonnet-5-5", max_tokens=8, messages=[{"role": "user", "content": "hi"}])
    out = tmp_path / "cap.json"
    rec.write(out, "recorded in a test, no live call")
    saved = json.loads(out.read_text())
    assert "request_id" not in json.dumps(saved) and saved["exchanges"][0]["expect"]["messages"] == 1
