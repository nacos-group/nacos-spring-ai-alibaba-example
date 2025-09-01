package komachi.sion.a2a.server.service;

import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import komachi.sion.a2a.server.autoconfiguration.utils.AgentCardConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 *
 *
 * @author xiweng.yy
 */
public class NacosAgentRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NacosAgentRegistry.class);
    
    private final AgentCard agentCard;
    
    private final A2aMaintainerService a2aMaintainerService;
    
    public NacosAgentRegistry(io.a2a.spec.AgentCard agentCard, A2aMaintainerService a2aMaintainerService) {
        this.agentCard = AgentCardConverterUtil.convertToNacosAgentCard(agentCard);
        this.a2aMaintainerService = a2aMaintainerService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        LOGGER.info("自动注册Agent{}到Nacos中", agentCard.getName());
        try {
            a2aMaintainerService.registerAgent(agentCard, "public");
            LOGGER.info("自动注册Agent{}到Nacos成功", agentCard.getName());
        } catch (NacosException e) {
            LOGGER.warn("Auto Register Agent {} failed", agentCard.getName(), e);
        }
    }
}
