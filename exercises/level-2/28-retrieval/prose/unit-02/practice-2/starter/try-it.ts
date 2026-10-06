// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { buildChunks, evaluate, retrieve } from "./retrieval.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A tiny corpus like the test fixture: two documents, cut into windows of 14 words that overlap by 4.
const corpus = [
  { id: "refunds", text: "Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the " +
    "original payment method within five business days. Digital goods are not refundable after download." },
  { id: "shipping", text: "Shipping: orders over 50 euros ship free of charge. Standard delivery takes three to five business " +
    "days, express delivery takes one day." },
];
const chunks = buildChunks(corpus, 14, 4) ?? [];
console.log("chunks:", chunks.map((c) => c.id).join(", "));

const query = "free shipping threshold";
for (const mode of ["bm25", "embedding", "hybrid"]) {
  console.log(`${mode} top 3:`, retrieve(chunks, query, mode, 3).join(", "));
}

// Recall@3 over one question whose answer we know.
console.log("hybrid recall@3:", chunks.length ? evaluate(chunks, [{ query, relevant: ["shipping"] }], "hybrid", 3) : null);
