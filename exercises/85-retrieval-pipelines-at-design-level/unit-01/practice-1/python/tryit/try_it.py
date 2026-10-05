"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from pipeline import chunk_sections, reindex, search

# Two documents, split into chunks that carry their title and section.
annual = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund."
monthly = "# Monthly plan\n## Cancellation\nYou can cancel at any time."
chunks = chunk_sections("annual", annual) + chunk_sections("monthly", monthly)
for chunk in chunks:
    print("chunk:", chunk.id, "|", chunk.text)

# Search ranks the chunks that share the most (and the most specific) words with the question.
print("search:", search(chunks, "can I cancel within 14 days", k=2))
print("search, annual only:", search(chunks, "cancel", k=2, allowed_docs={"annual"}))

# The annual document changes: reindex replaces its chunks and leaves the other alone.
changed = annual.replace("14 days", "30 days")
new_chunks, report = reindex(chunks, {"annual": changed, "monthly": monthly})
print("reindex report:", report)
