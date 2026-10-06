# Plan mode or direct execution

| Task | Mode | Why |
|---|---|---|
| Fix a typo in an error message | direct | the change is clear and fits in one diff |
| Restructure the monolith into services, across dozens of files | plan | several valid designs, so explore before changing anything |
| Add a validation check to one handler, with a clear spec | direct | the scope is known and small |
| Migrate to a new auth library, touching many files, with several valid designs | plan | the design decisions come before the edits |
| Rename a local variable in one function | direct | trivial and contained |
| Build a feature whose requirements are unclear, so interview first | plan | the questions come before the code |
