package komachi.sion.mcp.provider.configuration;

import komachi.sion.mcp.provider.service.NacosProviderService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @author xiweng.yy
 */
@Configuration
public class McpToolCallbackProviderConfiguration {
    
    @Bean
    public ToolCallbackProvider nacosProviderTools(NacosProviderService nacosProviderService) {
        return MethodToolCallbackProvider.builder().toolObjects(nacosProviderService).build();
    }
}
