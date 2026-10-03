# Exploring an unfamiliar service

Read every file first. Build the picture in small steps, and let each result choose the next one.

1. Find the entry points with Grep: search for `def main`, route decorators and the names of command handlers.
2. Find files by their names with Glob, for example `**/*_test.py` or `src/**/handlers/*.py`.
3. Read the entry point files in full with Read, then follow their imports one hop at a time.
4. Before tracing a function through wrapper modules, list the names each wrapper exports, then search the repository for each exported name with Grep.
5. Write what you learn to `notes/findings.md`, with the file and line for every claim.

## When an edit does not apply

1. If Edit says the text appears more than once, repeat it with more surrounding lines until it is unique.
2. If every occurrence should change, use replace_all.
3. If no unique anchor exists, Read the whole file and Write it back with the change.
