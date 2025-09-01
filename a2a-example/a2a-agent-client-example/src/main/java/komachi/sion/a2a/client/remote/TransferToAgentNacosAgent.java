package komachi.sion.a2a.client.remote;

import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.A2A;
import io.a2a.client.A2AClient;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.TextPart;
import io.a2a.util.Utils;
import io.modelcontextprotocol.spec.McpSchema;
import komachi.sion.a2a.server.autoconfiguration.utils.AgentCardConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author xiweng.yy
 */
@Service
public class TransferToAgentNacosAgent implements ToolCallback, RemoteAgent {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferToAgentNacosAgent.class);
    
    private final io.a2a.spec.AgentCard agentCard;
    
    private final A2AClient a2AClient;

    private final A2aMaintainerService a2aMaintainerService;
    
    public TransferToAgentNacosAgent(A2aMaintainerService a2aMaintainerService) throws A2AClientError, NacosException {
        this.a2aMaintainerService = a2aMaintainerService;
//        this.agentCard = A2A.getAgentCard("http://localhost:9999");
        this.agentCard = getAgentCardFromNacos("Nacos_Agent");
        this.a2AClient = new A2AClient(agentCard);
    }
    
    @Override
    public ToolDefinition getToolDefinition() {
        Map<String, Object> properties = Map.of("arg0",
                Map.of("type", "string", "description", "The users' question about Nacos"));
        McpSchema.JsonSchema mcpSchema = new McpSchema.JsonSchema("object", properties, null, false, null, null);
        try {
            return ToolDefinition.builder().name("transfer_to_agent_nacos_agent")
                    .description(this.agentCard.description())
                    .inputSchema(Utils.OBJECT_MAPPER.writeValueAsString(mcpSchema)).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String call(String toolInput) {
        Message message = A2A.toUserMessage(toolInput);
        MessageSendParams params = new MessageSendParams.Builder().message(message).configuration(
                new MessageSendConfiguration.Builder().blocking(true)
                        .acceptedOutputModes(Collections.singletonList("text/plain")).build()).build();
        try {
            SendMessageResponse response = a2AClient.sendMessage(params);
            LOGGER.info("Message sent with ID: {}", response.getId());
            EventKind result = response.getResult();
            if (result instanceof Message responseMessage) {
                StringBuilder textBuilder = new StringBuilder();
                if (responseMessage.getParts() != null) {
                    for (Part<?> part : responseMessage.getParts()) {
                        if (part instanceof TextPart textPart) {
                            LOGGER.info("[TMP SHOW] message text info: {}", textPart.getText());
                            textBuilder.append(textPart.getText());
                        }
                    }
                }
                return textBuilder.toString();
            }
        } catch (A2AServerException e) {
            throw new RuntimeException(e);
        }
        return "No agent result.";
    }
    
    @Override
    public Map<String, Object> remoteAgentInfo() throws JsonProcessingException {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "nacos_agent");
        result.put("description", agentCard.description());
        result.put("skills", agentCard.skills());
        return result;
    }

    private io.a2a.spec.AgentCard getAgentCardFromNacos(String agentName) throws NacosException {
        AgentCard nacosAgentCard = a2aMaintainerService.getAgentCard(agentName, "public");
        return AgentCardConverterUtil.convertToA2aAgentCard(nacosAgentCard);
    }
}
