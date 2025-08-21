package komachi.sion.a2a.client.configuration;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerFactory;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class NacosA2aConfiguration {
    @Bean
    public A2aMaintainerService a2aMaintainerService() throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", "127.0.0.1:8848");
        return A2aMaintainerFactory.createA2aMaintainerService(properties);
    }
}

