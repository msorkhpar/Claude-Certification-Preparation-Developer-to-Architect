"""Stand-in for the Messages API: scripted and replayed exchanges plugged into the official SDK's
HTTP transport hook, plus the capture scrubber. Standard library plus the pinned `anthropic` SDK."""
from .scripted import ScriptedTransport, scripted_client
from .replay import ReplayTransport, load_exchange
from .scrub import scan_text, scrub, CaptureLeak

__all__ = ["ScriptedTransport", "scripted_client", "ReplayTransport", "load_exchange",
           "scan_text", "scrub", "CaptureLeak"]
