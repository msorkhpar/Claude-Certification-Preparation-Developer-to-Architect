// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { chunkSections, reindex, search } from "./pipeline.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Two documents, split into chunks that carry their title and section.
const annual = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.";
const monthly = "# Monthly plan\n## Cancellation\nYou can cancel at any time.";
const chunks = [...chunkSections("annual", annual), ...chunkSections("monthly", monthly)];
for (const chunk of chunks) console.log("chunk:", chunk.id, "|", chunk.text);

// Search ranks the chunks that share the most (and the most specific) words with the question.
console.log("search:", search(chunks, "can I cancel within 14 days", 2));
console.log("search, annual only:", search(chunks, "cancel", 2, new Set(["annual"])));

// The annual document changes: reindex replaces its chunks and leaves the other alone.
const changed = annual.replace("14 days", "30 days");
const [newChunks, report] = reindex(chunks, { annual: changed, monthly });
console.log("reindex report:", JSON.stringify(report));
