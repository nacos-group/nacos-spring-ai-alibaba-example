package komachi.sion.a2a.server.configuration;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author xiweng.yy
 */
@Configuration
public class ChatClientConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatClientConfiguration.class);
    
    private static final String SYSTEM_PROMPT =
            "An assistant or maintainer for nacos. You only try to answer nacos' question. "
                    + "If user ask not nacos relative question, Please answer with apology. \n When you answer Nacos' question, "
                    + "you can try to use relative tools to query data and do analyze. If no suitable tools found, please answer Nacos' question by your knowledge.\n";
    
//    private static final String SYSTEM_PROMPT =
//            "An assistant or maintainer for nacos. You only try to answer nacos' question. "
//                    + "If user ask not nacos relative question, Please answer with apology. \n When you answer Nacos' question, "
//                    + "you can try to use relative tools to query data and do analyze. If no suitable tools found, please answer Nacos' question by your knowledge.\n"
//                    + "Example: \n - question: What's the Nacos. Answer: Nacos is an open-source platform developed by Alibaba for dynamic service discovery, configuration management, and service governance. It is designed to simplify the architecture of cloud-native applications and microservices by providing key capabilities:\n"
//                    + "\n" + "## Key Features\n" + "1. Service Discovery\n" + "\n"
//                    + "- Register and discover services (like Eureka/Consul).\n"
//                    + "- Supports health checks (active/passive) for service instances.\n"
//                    + "- Integrates with Spring Cloud, Dubbo, and Kubernetes.\n" + "\n"
//                    + "2. Dynamic Configuration Management\n" + "\n"
//                    + "- Centrally manage configurations across environments.\n"
//                    + "- Push updates to services in real-time.\n"
//                    + "- Supports multiple formats (properties, YAML, JSON, etc.).\n" + "\n"
//                    + "3.Service Metadata and Routing\n" + "\n" + "- Manage service metadata, weights, and labels.\n"
//                    + "- Enable traffic routing (e.g., blue-green deployments).\n" + "\n"
//                    + "4. DNS-Based Service Addressing\n" + "\n"
//                    + "- Use DNS protocols to resolve service names to IP addresses.\n"
//                    + "- Works seamlessly with Kubernetes and hybrid clouds.\n" + "\n" + "5. Distributed Coordination\n"
//                    + "\n" + "- Support for ephemeral instances and CP/AP consistency models.\n" + "\n"
//                    + "## Why Use Nacos?\n" + "\n"
//                    + "- Unified Platform: Combines service discovery + config management.\n"
//                    + "- Lightweight: Easy to deploy (standalone or cluster).\n"
//                    + "- Cloud-Agnostic: Works with Kubernetes, Spring Cloud, and Dubbo.\n"
//                    + "- Enterprise-Ready: Used by Alibaba in production for high-scale scenarios.\n" + "\n"
//                    + "Official Website: https://nacos.io";
    
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
            @Qualifier("loadbalancedMcpAsyncToolCallbacks") ToolCallbackProvider tools) {
        return chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
//        return chatClientBuilder.defaultSystem(SYSTEM_PROMPT).defaultToolCallbacks(tools).build();
    }
    
    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder().name("Nacos_Agent").description(
                        "Answer question about Nacos and do some maintain and query operation about Nacos Cluster.")
                .url("http://localhost:9999/a2a/").version("1.0.0")
                .documentationUrl("https://nacos.io/docs/latest/overview/").capabilities(
                        new AgentCapabilities.Builder().streaming(true).pushNotifications(true)
                                .stateTransitionHistory(true).build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text")).skills(Collections.singletonList(
                        new AgentSkill.Builder().id("answer_nacos_question").name("Answer Nacos' question")
                                .description("Answer all question about Nacos").tags(Collections.singletonList("Nacos"))
                                .examples(List.of("hi", "What's the Nacos?")).build())).protocolVersion("0.2.5")
                .build();
    }
    
    @Bean
    public ReactAgent nacosAgent(AgentCard agentCard, ChatModel chatModel,
            @Qualifier("loadbalancedMcpAsyncToolCallbacks") ToolCallbackProvider tools) throws GraphStateException {
        return ReactAgent.builder().name(agentCard.name()).description(agentCard.description()).model(chatModel)
                .instruction(SYSTEM_PROMPT).tools(Arrays.asList(tools.getToolCallbacks())).preToolHook(state -> {
                    LOGGER.info("Current Agent `{}` prepare to call tool", agentCard.name());
                    return Map.of();
                }).postToolHook(state -> {
                    LOGGER.info("Current Agent `{}` call tool completed", agentCard.name());
                    return Map.of();
                }).preLlmHook(state -> {
                    LOGGER.info("Current Agent `{}` prepare to call llm", agentCard.name());
                    return Map.of();
                }).postLlmHook(state -> {
                    LOGGER.info("Current Agent `{}` call llm completed", agentCard.name());
                    return Map.of();
                }).outputKey("output").build();
    }
}
