#!/usr/bin/env python3
"""A PreToolUse hook: reads one event from standard input and answers with its exit code, standard output and standard error.

Exit code 0 with nothing printed gives no opinion; exit code 2 blocks the call and the standard error is the reason; exit code 0 with
a JSON permissionDecision of deny also refuses it. Write the rules the statement lists.
"""
import sys

sys.exit(0)
