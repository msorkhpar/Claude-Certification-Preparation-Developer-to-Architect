/** The stream carried an error event, or ended before message_stop (errorType "incomplete_stream"). Given. */
class StreamError(val errorType: String, val detail: String) : RuntimeException("$errorType: $detail")
