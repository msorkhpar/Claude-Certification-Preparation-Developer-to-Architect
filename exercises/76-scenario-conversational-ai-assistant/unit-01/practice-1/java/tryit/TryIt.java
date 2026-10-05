import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        ConversationReview.Policy policy = new ConversationReview.Policy(12, 10, 80, 3);
        // A batch of conversations: billing mostly resolved, a safety case handed off, one overlong smalltalk.
        List<ConversationReview.Conversation> batch = new ArrayList<>();
        for (int i = 0; i < 3; i++) batch.add(new ConversationReview.Conversation("c", "billing", 5, true, "none", false, false, false));
        batch.add(new ConversationReview.Conversation("c", "billing", 5, false, "requested", true, false, false));
        batch.add(new ConversationReview.Conversation("c", "smalltalk", 5, true, "none", false, false, false));
        batch.add(new ConversationReview.Conversation("c", "smalltalk", 15, true, "none", false, false, false));
        batch.add(new ConversationReview.Conversation("c", "safety", 5, false, "safety", true, false, true));
        batch.add(new ConversationReview.Conversation("c", "billing", 5, true, "none", false, true, false));
        System.out.println(ConversationReview.review(batch, policy));
    }
}
