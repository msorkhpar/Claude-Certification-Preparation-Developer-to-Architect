---
name: publish
description: Tag the release and create the GitHub release from the notes
allowed-tools: Bash(git tag *) Bash(gh release create *)
---
Create the tag `v$ARGUMENTS`, then create the GitHub release from the latest release notes.
Report the URL of the release and nothing else.
