package komachi.sion.a2a.server.utils;

import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentSkill;
import io.a2a.spec.AgentCapabilities;

public class AgentCardConverterUtil {

    public static AgentCard convertToNacosAgentCard(io.a2a.spec.AgentCard agentCard) {
        AgentCard card = new AgentCard();
        card.setName(agentCard.name());
        card.setDescription(agentCard.description());
        card.setUrl(agentCard.url());
        card.setVersion(agentCard.version());
        card.setDocumentationUrl(agentCard.documentationUrl());
        card.setCapabilities(convertToNacosAgentCapabilities(agentCard.capabilities()));
        card.setDefaultInputModes(agentCard.defaultInputModes());
        card.setDefaultOutputModes(agentCard.defaultOutputModes());
        card.setSkills(agentCard.skills().stream().map(AgentCardConverterUtil::convertToNacosAgentSkill).toList());
        card.setProtocolVersion(agentCard.protocolVersion());

        return card;
    }

    public static com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities convertToNacosAgentCapabilities(AgentCapabilities capabilities) {
        com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities nacosCapabilities = new com.alibaba.nacos.api.ai.model.a2a.AgentCapabilities();
        nacosCapabilities.setStreaming(capabilities.streaming());
        nacosCapabilities.setPushNotifications(capabilities.pushNotifications());
        nacosCapabilities.setStateTransitionHistory(capabilities.stateTransitionHistory());
        return nacosCapabilities;
    }

    public static AgentSkill convertToNacosAgentSkill(io.a2a.spec.AgentSkill agentSkill) {
        AgentSkill skill = new AgentSkill();
        skill.setId(agentSkill.id());
        skill.setName(agentSkill.name());
        skill.setDescription(agentSkill.description());
        skill.setTags(agentSkill.tags());
        skill.setExamples(agentSkill.examples());
        skill.setInputModes(agentSkill.inputModes());
        skill.setOutputModes(agentSkill.outputModes());

        return skill;
    }
}
