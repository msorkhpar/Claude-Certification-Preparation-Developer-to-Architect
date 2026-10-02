import java.util.List;

/** The given data model; solutions implement PromptBuilder.build. Null or empty means "absent". */
record Doc(String name, String text) {}
record Example(String input, String output) {}
record Spec(String task, String role, String context, List<Doc> documents, List<Example> examples,
            List<String> constraints, String outputFormat) {}
