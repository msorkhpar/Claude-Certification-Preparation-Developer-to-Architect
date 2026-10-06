// Provenance: studyforge release/claude-cert-support, docs/authoring/run-output/junit-file.mjs (copied unchanged below this line)
import { mkdirSync, writeFileSync } from "node:fs";
import { junit } from "node:test/reporters";

// Writes target/report.xml (JUnit) and keeps what the tests wrote to stdout and stderr as the report's <system-out>.
const cdata = (text) => `<![CDATA[${text.replaceAll("]]>", "]]]]><![CDATA[>")}]]>`;

export default async function* (source) {
  const printed = [];
  async function* watched() {
    for await (const event of source) {
      if (event.type === "test:stdout" || event.type === "test:stderr") printed.push(String(event.data.message));
      yield event;
    }
  }
  let xml = "";
  for await (const chunk of junit(watched())) xml += chunk;
  const text = printed.join("");
  const end = xml.lastIndexOf("</testsuites>");
  if (text && end !== -1) xml = `${xml.slice(0, end)}<system-out>${cdata(text)}</system-out>\n${xml.slice(end)}`;
  mkdirSync(new URL("./target/", import.meta.url), { recursive: true });
  writeFileSync(new URL("./target/report.xml", import.meta.url), xml);
}
