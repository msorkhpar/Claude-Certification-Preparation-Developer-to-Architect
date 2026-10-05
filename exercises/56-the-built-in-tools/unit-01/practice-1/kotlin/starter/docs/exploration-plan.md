# Exploring an unfamiliar service

Do not read every file first. Build the picture in small steps, and let each result choose the next one.

1. Find the entry points with Grep: search for `def main`, route decorators and the names of command handlers.
2. Find files by their names with Glob, for example `**/*_test.py` or `src/**/handlers/*.py`.
<!-- TODO 5 of 6 (finish this to pass e5): steps 3 and 4. Step 3 reads the entry point files in full with Read and follows their imports one hop at a time. Step 4 lists the names each wrapper module exports and searches the repository for each exported name with Grep before tracing a function through the wrappers. -->
5. Write what you learn to `notes/findings.md`, with the file and line for every claim.

## When an edit does not apply

<!-- TODO 6 of 6 (finish this to pass e6): three numbered remedies, in this order: repeat the text with more surrounding lines until it is unique; use replace_all when every occurrence should change; and as a last resort Read the whole file and Write it back with the change. Remove the personal path below for e7. -->
The author's clone was at /home/dev/inventory.
