/** Given: what one action returns: the content for the tool result (a String or a list of blocks) and whether it failed. */
record Outcome(Object content, boolean isError) {}
