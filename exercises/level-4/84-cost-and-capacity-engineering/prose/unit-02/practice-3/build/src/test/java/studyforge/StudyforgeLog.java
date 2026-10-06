// Provenance: studyforge release/claude-cert-support, docs/authoring/run-output/StudyforgeLog.java (copied unchanged below this line)
package studyforge;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test harness only: sends every System.Logger line (all levels) to stderr as `[log:<test>] LEVEL logger: message`,
 * so the report can show it under the test that was running. Registered by ServiceLoader; learner code never imports it.
 */
public final class StudyforgeLog implements BeforeEachCallback {
    private static volatile String current = "";

    static {
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) root.removeHandler(handler);
        root.setLevel(Level.ALL);
        Logger.getLogger("org.junit").setLevel(Level.WARNING);
        Logger.getLogger("org.opentest4j").setLevel(Level.WARNING);
        root.addHandler(new Handler() {
            private final SimpleFormatter formatter = new SimpleFormatter();

            @Override public void publish(LogRecord record) {
                String level = record.getLevel() == Level.FINE || record.getLevel() == Level.FINER || record.getLevel() == Level.FINEST
                        ? "DEBUG" : record.getLevel().getName().replace("SEVERE", "ERROR").replace("WARNING", "WARN");
                for (String line : formatter.formatMessage(record).split("\n", -1)) {
                    System.err.println("[log:" + current + "] " + level + " " + record.getLoggerName() + ": " + line);
                }
            }

            @Override public void flush() {}

            @Override public void close() {}
        });
    }

    @Override public void beforeEach(ExtensionContext context) {
        current = context.getDisplayName();
    }
}
