package komachi.sion.a2a.server;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @author xiweng.yy
 */
@Configuration
public class RootAgentConfiguration {
    
    private static final String SYSTEM_PROMPT =
            "An assistant or maintainer for nacos. You only try to answer nacos' question. "
                    + "If user ask not nacos relative question, Please answer with apology. \n When you answer Nacos' question, "
                    + "you can try to use relative tools to query data and do analyze. If no suitable tools found, please answer Nacos' question by your knowledge.\n";
    
    @Bean
    public BaseAgent rootAgent(ChatModel chatModel) throws GraphStateException {
        return ReactAgent.builder().name("Nacos_New_Agent").description(
                        "Answer question about Nacos and do some maintain and query operation about Nacos Cluster.")
                .model(chatModel).instruction(SYSTEM_PROMPT).outputKey("output").build();
    }
}
