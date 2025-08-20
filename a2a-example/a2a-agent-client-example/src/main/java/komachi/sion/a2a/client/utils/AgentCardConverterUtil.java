package komachi.sion.a2a.client.utils;

import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import io.a2a.spec.AgentCapabilities;

import java.util.List;
import java.util.stream.Collectors;

public class AgentCardConverterUtil {

    public static io.a2a.spec.AgentCard convertToA2aAgentCard(com.alibaba.nacos.api.ai.model.a2a.AgentCard agentCard) {
        if (agentCard == null) {
            return null;
        }

        return new io.a2a.spec.AgentCard.Builder()
                .name(agentCard.getName())
                .description(agentCard.getDescription())
                .url(agentCard.getUrl())
                .version(agentCard.getVersion())
                .documentationUrl(agentCard.getDocumentationUrl())
                .capabilities(convertToA2aAgentCapabilities(agentCard.getCapabilities()))
                .defaultInputModes(agentCard.getDefaultInputModes())
                .defaultOutputModes(agentCard.getDefaultOutputModes())
                .skills(convertToA2aAgentSkills(agentCard.getSkills()))
                .protocolVersion(agentCard.getProtocolVersion())
                .build();
    }

    public static io.a2a.spec.AgentCapabilities convertToA2aAgentCapabilities(com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities) {
        if (nacosCapabilities == null) {
            return null;
        }

        return new io.a2a.spec.AgentCapabilities.Builder()
                .streaming(nacosCapabilities.getStreaming())
                .pushNotifications(nacosCapabilities.getPushNotifications())
                .stateTransitionHistory(nacosCapabilities.getStateTransitionHistory())
                .build();
    }

    public static List<io.a2a.spec.AgentSkill> convertToA2aAgentSkills(List<com.alibaba.nacos.api.ai.model.a2a.AgentSkill> nacosSkills) {
        if (nacosSkills == null) {
            return null;
        }

        return nacosSkills.stream().map(AgentCardConverterUtil::transferAgentSkill).collect(Collectors.toList());
    }

    public static io.a2a.spec.AgentSkill transferAgentSkill(com.alibaba.nacos.api.ai.model.a2a.AgentSkill nacosSkill) {
        return new io.a2a.spec.AgentSkill.Builder()
                .id(nacosSkill.getId())
                .tags(nacosSkill.getTags())
                .examples(nacosSkill.getExamples())
                .name(nacosSkill.getName())
                .description(nacosSkill.getDescription())
                .inputModes(nacosSkill.getInputModes())
                .outputModes(nacosSkill.getOutputModes())
                .build();
    }
}
