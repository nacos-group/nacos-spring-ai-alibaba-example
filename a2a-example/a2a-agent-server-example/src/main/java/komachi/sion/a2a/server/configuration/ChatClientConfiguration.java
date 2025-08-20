package komachi.sion.a2a.server.configuration;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 *
 *
 * @author xiweng.yy
 */
@Configuration
public class ChatClientConfiguration {
    
    private static final String SYSTEM_PROMPT = "An assistant or maintainer for nacos. You only try to answer nacos' problem. If user ask not nacos relative problem, Please answer with apology.";
    
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }
    
    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("Nacos_Agent")
                .description("Answer question about Nacos and do some maintain and query operation about Nacos Cluster.")
                .url("http://localhost:9999/a2a/")
                .version("1.0.0")
                .documentationUrl("https://nacos.io/docs/latest/overview/")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("answer_nacos_question")
                        .name("Answer Nacos' question")
                        .description("Answer all question about Nacos")
                        .tags(Collections.singletonList("Nacos"))
                        .examples(List.of("hi", "What's the Nacos?"))
                        .build()))
                .protocolVersion("0.2.5")
                .build();
    }
}
