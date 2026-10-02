import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;

public class Anth {
    public static String describe() {
        AnthropicClient c = AnthropicOkHttpClient.builder().apiKey("placeholder").baseUrl("http://127.0.0.1:9").build();
        MessageCreateParams p = MessageCreateParams.builder().model("claude-sonnet-5-5").maxTokens(16).addUserMessage("hi").build();
        return c.getClass().getSimpleName() + " " + p.model().toString();
    }
}
