/** The given type: one item's result. ok is true with a value, or false with the error that work threw. */
record Outcome<R>(boolean ok, R value, Throwable error) {}
