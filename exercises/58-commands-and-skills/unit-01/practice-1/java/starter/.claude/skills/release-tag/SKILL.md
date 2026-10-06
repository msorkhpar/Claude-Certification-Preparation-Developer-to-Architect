---
name: release-tag
description: Tag a release and push the tag.
allowed-tools: Bash
---
Cut release $version.

1. Check that the working tree is clean with `git status`.
2. Create an annotated tag with `git tag -a $version -m "Release $version"`.
3. Push only that tag with `git push origin $version`.
