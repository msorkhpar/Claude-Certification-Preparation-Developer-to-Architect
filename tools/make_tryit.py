#!/usr/bin/env python3
"""The "try it" file of a practice: scaffold it, run it, check it. One small file per language, edited by the reader.

A practice's Run button executes the reader's own code and shows what it printed and logged: no tests, no grade (Submit grades).
Each practice language carries one runnable file, kept in  <practice>/<lang>/tryit/ :
  python      try_it.py     (logging.basicConfig(level=logging.DEBUG, ...))
  typescript  try-it.ts     (logTo("try-it") from the harness logger)
  java        TryIt.java    (public static void main; java.util.logging root logger + ConsoleHandler at ALL)
  kotlin      TryIt.kt      (fun main(); the same logging switch)
The file builds the object the practice asks for with the same stand-in the first `m*` test uses, calls it on the statement's
example and prints what comes back ("history size: 2"). It lives in the main source set, so for Java and Kotlin the course's
build.gradle.kts lists `tryit` beside the solution folder and registers the `tryIt` task (mainClass TryIt / TryItKt).

usage:
  tools/make_tryit.py scaffold <practice-dir>            write the four skeletons (never overwrites a file)
  tools/make_tryit.py run <practice-dir> <lang> <variant>  run the file against `starter`, `reference` or a plant; prints its output
  tools/make_tryit.py check [--modules REGEX]            every practice that has a tryit folder has all four files, the logging switch,
                                                         a print, and (JVM) the build.gradle.kts task; exit 1 otherwise
Java and Kotlin runs use $GRADLE (default `gradle`) offline; wrap the whole command in ../.heavy-slot/run-heavy.sh.
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES = {"python": "try_it.py", "typescript": "try-it.ts", "java": "TryIt.java", "kotlin": "TryIt.kt"}
SWITCH = {
    "python": "logging.basicConfig(level=logging.DEBUG",
    "typescript": 'logTo("try-it")',
    "java": "handler.setLevel(Level.ALL)",
    "kotlin": "level = Level.ALL",
}
TASK = {"java": 'mainClass.set("TryIt")', "kotlin": 'mainClass.set("TryItKt")'}

SKELETON = {
    "python": '''"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from MODULE import CLASS

# A stand-in for the API, like the one the tests use for the first main case m1.
# TODO: copy that setup here, call the class on the statement's example, then print the results:
# print("history size:", ...)
''',
    "typescript": '''// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { CLASS } from "./MODULE.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A stand-in for the API, like the one the tests use for the first main case m1.
// TODO: copy that setup here, call the class on the statement's example, then print the results:
// console.log("history size:", ...);
''',
    "java": '''import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        // A stand-in for the API, like the one the tests use for the first main case m1.
        // TODO: copy that setup here, call the class on the statement's example, then print the results:
        // System.out.println("history size: " + ...);
    }
}
''',
    "kotlin": '''import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\\$s %5\\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A stand-in for the API, like the one the tests use for the first main case m1.
    // TODO: copy that setup here, call the class on the statement's example, then print the results:
    // println("history size: ${...}")
}
''',
}


def practices(rx):
    for spec in sorted((ROOT / "exercises").glob("*/*/*/cases.json")):
        if rx.search(spec.parts[-4]):
            yield spec.parent


def scaffold(practice: Path):
    for lang, name in FILES.items():
        target = practice / lang / "tryit" / name
        if target.exists():
            print(f"kept {target.relative_to(ROOT)}")
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        main = next((f.stem for f in sorted((practice / lang / "starter").iterdir()) if f.suffix in (".py", ".ts")), "MODULE")
        target.write_text(SKELETON[lang].replace("MODULE", main).replace("CLASS", "Thing"), encoding="utf-8")
        print(f"wrote {target.relative_to(ROOT)}")
    for lang in ("java", "kotlin"):
        build = practice / lang / "build.gradle.kts"
        text = build.read_text(encoding="utf-8")
        key = "java" if lang == "java" else "kotlin"
        old = f"main {{ {key}.setSrcDirs(listOf(solution)) }}"
        if old in text:
            text = text.replace(old, f'main {{ {key}.setSrcDirs(listOf(solution, "tryit")) }}')
        if "tryIt" not in text:
            text += ('\n// Run: the reader\'s own try-it file, with the logger turned up (no tests, no grade): gradle -Psolution=reference tryIt\n'
                     f'tasks.register<JavaExec>("tryIt") {{ classpath = sourceSets["main"].runtimeClasspath; {TASK[lang]} }}\n')
        build.write_text(text, encoding="utf-8")
        print(f"updated {build.relative_to(ROOT)}")


def run(practice: Path, lang: str, variant: str) -> int:
    work = ROOT / ".tryit-out" / "_".join(practice.parts[-3:]) / lang
    shutil.rmtree(work, ignore_errors=True)
    if lang in ("java", "kotlin"):
        gradle = os.environ.get("GRADLE", "gradle").split()
        return subprocess.call([*gradle, "--offline", "-q", "-p", str(practice / lang), f"-Psolution={variant}", "tryIt"])
    work.mkdir(parents=True)
    for source in sorted((practice / lang / variant).iterdir()):
        if source.is_dir():  # a project folder (.github, governance, docs) travels with the file
            shutil.copytree(source, work / source.name)
            continue
        text = source.read_text(encoding="utf-8")
        # the workspace holds the harness logger beside the file, as the site builds it
        shutil.copy(source, work / source.name) if lang == "python" else (work / source.name).write_text(text.replace('from "../logger.ts"', 'from "./logger.ts"'), encoding="utf-8")
    for source in (practice / lang / "tryit").iterdir():
        shutil.copy(source, work / source.name)
    if lang == "typescript":
        shutil.copy(ROOT / "tools" / "harness" / "logger.ts", work / "logger.ts")
        return subprocess.call(["node", str(work / FILES[lang])])
    return subprocess.call([sys.executable, str(work / FILES[lang])], env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"})


def check(rx) -> int:
    bad, seen = [], 0
    for practice in practices(rx):
        if not any((practice / lang / "tryit").is_dir() for lang in FILES):
            continue
        seen += 1
        for lang, name in FILES.items():
            file = practice / lang / "tryit" / name
            where = file.relative_to(ROOT)
            if not file.is_file():
                bad.append(f"{where}: missing")
                continue
            text = file.read_text(encoding="utf-8")
            if SWITCH[lang] not in text:
                bad.append(f"{where}: does not turn the logger up ({SWITCH[lang]})")
            if not re.search(r"print|println|console\.log", text):
                bad.append(f"{where}: prints nothing")
            if "TODO" in text:
                bad.append(f"{where}: still has a TODO")
            if lang in TASK:
                build = (practice / lang / "build.gradle.kts").read_text(encoding="utf-8")
                if "tryIt" not in build or TASK[lang] not in build or '"tryit"' not in build:
                    bad.append(f"{practice / lang / 'build.gradle.kts'}: lacks the tryIt task or the tryit source folder")
    print(f"make_tryit: {seen} practice(s) with a try-it file")
    for line in bad:
        print("  " + line)
    return 1 if bad else 0


def main() -> int:
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("scaffold"); s.add_argument("practice")
    r = sub.add_parser("run"); r.add_argument("practice"); r.add_argument("lang", choices=list(FILES)); r.add_argument("variant")
    c = sub.add_parser("check"); c.add_argument("--modules", default=".*")
    a = ap.parse_args()
    if a.cmd == "scaffold":
        scaffold(Path(a.practice).resolve()); return 0
    if a.cmd == "run":
        return run(Path(a.practice).resolve(), a.lang, a.variant)
    return check(re.compile(a.modules))


if __name__ == "__main__":
    sys.exit(main())
