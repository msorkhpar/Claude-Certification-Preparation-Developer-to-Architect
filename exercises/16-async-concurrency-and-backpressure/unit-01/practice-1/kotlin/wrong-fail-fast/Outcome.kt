/** The given type: one item's result. ok is true with a value, or false with the error that work threw. */
data class Outcome<R>(val ok: Boolean, val value: R?, val error: Throwable?)
