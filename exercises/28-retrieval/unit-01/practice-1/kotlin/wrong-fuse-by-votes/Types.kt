/** A piece of a document: its id is "<doc>#<n>". */
data class Chunk(val id: String, val doc: String, val text: String)

/** A document of the corpus. */
data class Doc(val id: String, val text: String)

/** A test query and the ids of the documents that answer it. */
data class Query(val query: String, val relevant: List<String>)

typealias Scorer = (String, String) -> Double
