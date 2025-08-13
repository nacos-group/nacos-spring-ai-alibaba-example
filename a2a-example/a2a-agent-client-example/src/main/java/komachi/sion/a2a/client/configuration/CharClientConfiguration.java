package komachi.sion.a2a.client.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.util.Utils;
import komachi.sion.a2a.client.remote.RemoteAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
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
}
