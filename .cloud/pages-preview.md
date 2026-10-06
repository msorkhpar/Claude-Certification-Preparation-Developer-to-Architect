# Cloud brief: a complete static preview of this course on GitHub Pages

Work only on branch `cloud/pages-preview`, created from this branch. Push only that branch.
Delete this `.cloud/` folder in your last commit and put your hand-back (at most 25 lines)
in the last commit's message.

## The goal

This repository is a course's learner tree. Its pages are built HTML under `.studyforge/`
(313 lesson pages, plus `index.html`). `.github/workflows/pages.yml` runs
`python3 .github/preview/preview.py . _site` on every push to `main` and deploys `_site`
to GitHub Pages as a read-only preview.

The preview script was written for an earlier, simpler course. Make it produce a preview
of THIS course in which everything that can work without a server works, and everything
that needs the local server is removed and replaced by the short note the script already
uses. Run `python3 .github/preview/preview.py . <out>` and serve `<out>` with
`python3 -m http.server` under a sub-path like `/<repo>/` to check it.

## What a first run showed

- **Search:** the index is loaded by script (`.studyforge/assets/search-index.js`,
  `search-index-0.js`, `search-index-1.js`, `minisearch.js`), not by `href`/`src`, so the
  walk does not copy it. The search box must work in the preview.
- **Run and the try-it file:** pages carry a Run button for a practice's try-it file and
  `example-run.js` / `example-run.css`, which call the local server. Remove them like Submit.
- **Check my files:** configuration practices have a Run that lists the learner's files
  through the server. Remove it the same way.
- **Server links:** nine pages still reference `/api/` paths. None may remain.
- **Not yet checked; verify each one works statically or is cleanly removed:**
  - the mock exams (`mock-exam.js`, `mock-form*.js`): they grade in the page;
  - the in-place quiz stepper (`review.js`);
  - the language modes (`modes.js`): one language at a time, with the out-of-mode topics locked;
  - slide decks (`deck.js`);
  - reading progress and marks (browser storage only, never the server);
  - the editor links and the "open in editor" links of code examples.

## Rules

- `preview.py` stays one file that uses only the Python standard library, starts no process,
  reads nothing outside the tree, writes only into the output, and names no account, host or
  repository. Keep its existing structure, style and comment density. Every edit is a
  deterministic edit of the built page; never hand-edit files under `.studyforge/`.
- Never hide a server part with CSS. Remove it, and put the existing note in its place.
- Keep it working for a course without these features: every new edit must be a no-op on a
  page that lacks the feature.
- No personal data, keys, conversation or source names in any file or commit.

## Proof (in the hand-back)

- Counts of pages and files, and size, before and after your change.
- A headless-browser run over every preview page (served under a sub-path) with zero console
  errors and zero failed requests. Check that search returns results, a module quiz and a mock
  exam can be answered and graded, a language mode switch works, and no Run, Submit,
  check-my-files or editor control is left.
- `grep` over the output: no `/api/`, no `example-run`, and no reference into `.studyforge`.
- Tests: add a small standard-library test script under `.github/preview/` only if you need
  one; the framework will carry the real tests.
