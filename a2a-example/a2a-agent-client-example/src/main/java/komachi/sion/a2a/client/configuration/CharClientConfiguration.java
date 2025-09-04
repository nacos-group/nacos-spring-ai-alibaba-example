package komachi.sion.a2a.client.configuration;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.util.Utils;
import komachi.sion.a2a.client.remote.NacosA2aRemoteAgent;
import komachi.sion.a2a.client.remote.RemoteAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author xiweng.yy
 */
@Configuration
public class CharClientConfiguration {
    
    private static final String SYSTEM_PROMPT = "## Role\n" + "\n"
            + "You are an `orchestrator` of sub-agents. You need to analyze the user's problem and based on the information you know about the sub-agents, select the correct sub-agent and generate the corresponding task to call the sub-agent.\n"
            + "If there is no suitable sub-agent, you should answer users' problem by yourself.\n" + "\n"
            + "## Sub Agent List \n" + "\n"
            + "Sub agent list is format by `JSON`, it should be a JSON array, each JSON object is a sub agent. The Sub agent JSON will include it's name, description and skills. You should according to the description and skills to choose sub agents to solve users' problem.\n"
            + "\n" + "{sub_agent_list_json}\n" + "\n" + "## How to call sub agent\n" + "\n"
            + "If you think another agent is better for answering the question according to its description, call `transfer_to_agent_$sub_agent_name` tool to transfer the question to that agent. you should replace $sub_agent_name by agent names from sub_agent_list_json.\n"
            + "When transferring, do not generate any text other than the tool call.\n";
    
    private static final String COMMON_QUESTION_ANSWER_PROMPT = "## Role \n\n"
            + "You are a helpful assistant, you should try you best to answer users' problem according your knowledge.";
    
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallback... toolCallbacks)
            throws JsonProcessingException {
        List<Map<String, Object>> agentList = new LinkedList<>();
        for (ToolCallback each : toolCallbacks) {
            if (each instanceof RemoteAgent agent) {
                agentList.add(agent.remoteAgentInfo());
            }
        }
        String subAgentListJson = Utils.OBJECT_MAPPER.writeValueAsString(agentList);
        return chatClientBuilder.defaultSystem(promptSystemSpec -> {
            promptSystemSpec.text(SYSTEM_PROMPT);
            promptSystemSpec.params(Collections.singletonMap("sub_agent_list_json", subAgentListJson));
        }).defaultToolCallbacks(toolCallbacks).build();
    }
    
    @Bean
    @Primary
    public BaseAgent OrchestratorAgent(ChatModel chatModel, A2aMaintainerService a2aMaintainerService)
            throws GraphStateException, NacosException {
        ReactAgent commonQuestionAnswerAgent = ReactAgent.builder().name("common_question_answer_agent")
                .description("Agent to answer common question").instruction(COMMON_QUESTION_ANSWER_PROMPT)
                .model(chatModel).outputKey("messages").build();
//        NacosA2aRemoteAgent blogWriterAgent = new NacosA2aRemoteAgent.Builder().a2aMaintainerService(
//                a2aMaintainerService).name("Blog_Writing_Agent").build();
        NacosA2aRemoteAgent nacosAgent = new NacosA2aRemoteAgent.Builder().a2aMaintainerService(a2aMaintainerService)
                .name("Nacos_Agent").build();
        KeyStrategyFactory stateFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("input", new ReplaceStrategy());
            keyStrategyHashMap.put("output", new ReplaceStrategy());
            keyStrategyHashMap.put("article", new ReplaceStrategy());
            keyStrategyHashMap.put("reviewed_article", new ReplaceStrategy());
            return keyStrategyHashMap;
        };
//        return LlmRoutingAgent.builder().name("orchestrator").description("An orchestrator agent").model(chatModel)
//                .subAgents(List.of(commonQuestionAnswerAgent, blogWriterAgent, nacosAgent)).state(stateFactory)
//                .inputKey("input").outputKey("output").build();
        return LlmRoutingAgent.builder().name("orchestrator").description("An orchestrator agent").model(chatModel)
                .subAgents(List.of(commonQuestionAnswerAgent, nacosAgent)).state(stateFactory)
                .inputKey("input").outputKey("messages").build();
    }
}
