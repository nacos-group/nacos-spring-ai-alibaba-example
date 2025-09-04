package komachi.sion.a2a.server.autoconfiguration;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerFactory;
import komachi.sion.a2a.server.autoconfiguration.configuration.AgentHandlerConfiguration;
import komachi.sion.a2a.server.autoconfiguration.properties.NacosA2aProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

/**
 *
 *
 * @author xiweng.yy
 */
@EnableConfigurationProperties({NacosA2aProperties.class})
@AutoConfiguration(after = {AgentHandlerConfiguration.class})
@ConditionalOnProperty(prefix = "spring.ai.alibaba.a2a.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosA2aRegisterAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(A2aMaintainerService.class)
    public A2aMaintainerService a2aMaintainerService(NacosA2aProperties nacosA2aProperties) throws NacosException {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacosA2aProperties.getServerAddr());
        properties.setProperty(PropertyKeyConst.NAMESPACE, nacosA2aProperties.getNamespace());
        properties.setProperty(PropertyKeyConst.USERNAME, nacosA2aProperties.getUsername());
        properties.setProperty(PropertyKeyConst.PASSWORD, nacosA2aProperties.getPassword());
        return AiMaintainerFactory.createAiMaintainerService(properties);
    }
}
