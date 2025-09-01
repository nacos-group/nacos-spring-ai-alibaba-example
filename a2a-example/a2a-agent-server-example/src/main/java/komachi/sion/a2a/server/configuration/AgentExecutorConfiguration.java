package komachi.sion.a2a.server.configuration;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCard;
import komachi.sion.a2a.server.service.NacosAgentExecutorByChatClient;
import komachi.sion.a2a.server.service.NacosAgentExecutorBySaaAgent;
import komachi.sion.a2a.server.service.NacosAgentRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 *
 *
 * @author xiweng.yy
 */
@Configuration
public class AgentExecutorConfiguration {
    
    @Bean
//    @Primary
    public AgentExecutor agentExecutorByChatClient(ChatClient chatClient,
            @Qualifier("loadbalancedMcpAsyncToolCallbacks") ToolCallbackProvider tools) {
        return new NacosAgentExecutorByChatClient(chatClient, tools);
    }
    
    @Bean
        @Primary
    public AgentExecutor agentExecutorBySaaAgent(ReactAgent agent) {
        return new NacosAgentExecutorBySaaAgent(agent);
    }
    
    @Bean
    public NacosAgentRegistry nacosAgentRegistry(AgentCard agentCard, A2aMaintainerService a2aMaintainerService) {
        return new NacosAgentRegistry(agentCard, a2aMaintainerService);
    }
}
