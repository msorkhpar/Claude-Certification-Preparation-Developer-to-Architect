import { readdirSync } from "node:fs";
import { spawnSync } from "node:child_process";

// The SDK packages come from the toolchain's offline cache, then the tests run under the JUnit reporter.
const here = import.meta.dirname;
const install = spawnSync("npm", ["ci", "--offline", "--ignore-scripts", "--no-audit", "--no-fund"], { cwd: here, stdio: "inherit" });
if (install.status !== 0) process.exit(install.status ?? 1);
const test = readdirSync(here).find((name) => name.endsWith(".test.ts"));
const tests = spawnSync("node", ["--test", "--test-reporter=spec", "--test-reporter-destination=stdout",
  "--test-reporter=./junit-file.mjs", "--test-reporter-destination=stdout", test], { cwd: here, stdio: "inherit" });
process.exit(tests.status ?? 1);
