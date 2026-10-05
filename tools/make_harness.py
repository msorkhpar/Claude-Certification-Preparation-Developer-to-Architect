#!/usr/bin/env python3
"""Drop the run-output harness (tools/harness/) into every practice folder, in the layout each language needs.

The files are generated, git-ignored and never committed: tools/harness/ holds the one tracked copy. A starter, reference or
plant that declares a logger gets its debug lines in the JUnit report as the framework's `[log...]` marker; one that declares
none runs exactly as before (the drop-ins change only what is recorded, never a verdict).

Layout per practice (<p> = exercises/<module>/<unit>/<practice>):
  <p>/python/pytest.ini                                  beside the tests
  <p>/typescript/logger.ts, junit-file.mjs               starter, reference and plants import it as: import { logger } from "../logger.ts";
  <p>/java/tests/studyforge/StudyforgeLog.java           Java test source set is the folder `tests`
  <p>/kotlin/src/test/java/studyforge/StudyforgeLog.java Kotlin's tests folder is compiled by kotlinc; the Java extension sits in the default Java test dir
  <p>/{java,kotlin}/src/test/resources/junit-platform.properties
  <p>/{java,kotlin}/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension
Lesson examples: examples/<example>/typescript/logger.ts, imported as: import { logger } from "./logger.ts";
Logger lines (one visible line at the top of the file): Python `log = logging.getLogger(__name__)`;
Java `private static final System.Logger LOG = System.getLogger(Gate.class.getName());`; Kotlin `private val log = System.getLogger("gate")`;
TypeScript `const log = logger("gate")`.

usage: tools/make_harness.py [--modules REGEX]   (default: all modules)
"""
import argparse
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
H = ROOT / "tools" / "harness"


def put(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(H / src, dst)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--modules", default=".*")
    rx = re.compile(ap.parse_args().modules)
    n = 0
    for module in sorted((ROOT / "exercises").iterdir()):
        if not (module.is_dir() and rx.search(module.name)):
            continue
        for spec in sorted(module.glob("*/*/cases.json")):
            p = spec.parent
            if (p / "python").is_dir():
                put("pytest.ini", p / "python" / "pytest.ini")
            if (p / "typescript").is_dir():
                put("logger.ts", p / "typescript" / "logger.ts")
                put("junit-file.mjs", p / "typescript" / "junit-file.mjs")
            if (p / "java").is_dir():
                put("StudyforgeLog.java", p / "java" / "tests" / "studyforge" / "StudyforgeLog.java")
            if (p / "kotlin").is_dir():
                put("StudyforgeLog.java", p / "kotlin" / "src" / "test" / "java" / "studyforge" / "StudyforgeLog.java")
            for lang in ("java", "kotlin"):
                if (p / lang).is_dir():
                    res = p / lang / "src" / "test" / "resources"
                    put("junit-platform.properties", res / "junit-platform.properties")
                    put("org.junit.jupiter.api.extension.Extension", res / "META-INF" / "services" / "org.junit.jupiter.api.extension.Extension")
            n += 1
    for ex in sorted((ROOT / "examples").glob("*/typescript")):   # a lesson example imports it as: import { logger } from "./logger.ts";
        if rx.search(ex.parent.name):
            put("logger.ts", ex / "logger.ts")
    print(f"make_harness: {n} practices")


if __name__ == "__main__":
    sys.exit(main())
