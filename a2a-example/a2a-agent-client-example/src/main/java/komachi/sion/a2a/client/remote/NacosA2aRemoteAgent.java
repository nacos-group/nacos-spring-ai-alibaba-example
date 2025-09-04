package komachi.sion.a2a.client.remote;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import komachi.sion.a2a.server.autoconfiguration.utils.AgentCardConverterUtil;

import java.util.Map;
import java.util.Optional;

/**
 *
 *
 * @author xiweng.yy
 */
public class NacosA2aRemoteAgent extends BaseAgent {
    
    private final A2aRemoteAgent a2aRemoteAgent;
    
    private final AgentCard agentCard;
    
    private NacosA2aRemoteAgent(AgentCard agentCard, String outputKey) throws GraphStateException {
        this.agentCard = agentCard;
        this.a2aRemoteAgent = A2aRemoteAgent.builder().name(agentCard.getName()).description(agentCard.getDescription())
                .agentCard(AgentCardConverterUtil.convertToA2aAgentCard(agentCard)).outputKey(outputKey).build();
    }
    
    @Override
    public String name() {
        return this.a2aRemoteAgent.name();
    }
    
    @Override
    public String description() {
        return this.a2aRemoteAgent.description();
    }
    
    @Override
    public String outputKey() {
        return this.a2aRemoteAgent.outputKey();
    }
    
    @Override
    public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
            throws GraphStateException {
        return a2aRemoteAgent.asAsyncNodeAction(inputKeyFromParent, outputKeyToParent);
    }
    
    @Override
    public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
        return a2aRemoteAgent.invoke(input);
    }
    
    @Override
    public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) throws GraphStateException, GraphRunnerException {
        return a2aRemoteAgent.schedule(scheduleConfig);
    }
    
    @Override
    public AsyncGenerator<NodeOutput> stream(Map<String, Object> input)
            throws GraphStateException, GraphRunnerException {
        return a2aRemoteAgent.stream(input);
    }
    
    public static class Builder {
        
        private A2aMaintainerService a2aMaintainerService;
        
        private String name;
        
        private String outputKey = "messages";
        
        public Builder a2aMaintainerService(A2aMaintainerService a2aMaintainerService) {
            this.a2aMaintainerService = a2aMaintainerService;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }
        
        public NacosA2aRemoteAgent build() throws GraphStateException, NacosException {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Name must be provided");
            }
            if (null == this.a2aMaintainerService) {
                throw new IllegalArgumentException("Nacos client can't be null");
            }
            AgentCard agentCard = this.a2aMaintainerService.getAgentCard(name, Constants.DEFAULT_NAMESPACE_ID);
            return new NacosA2aRemoteAgent(agentCard, outputKey);
        }
    }
}
