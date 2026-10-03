import java.util.List;

/** A piece of a document: its id is "<doc>#<n>". */
record Chunk(String id, String doc, String text) {}

/** A document of the corpus. */
record Doc(String id, String text) {}

/** A test query and the ids of the documents that answer it. */
record Query(String query, List<String> relevant) {}
