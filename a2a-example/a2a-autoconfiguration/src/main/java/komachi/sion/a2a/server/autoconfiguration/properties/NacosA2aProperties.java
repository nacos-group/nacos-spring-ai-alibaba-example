package komachi.sion.a2a.server.autoconfiguration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.a2a.nacos")
public class NacosA2aProperties {
    
    public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.nacos";
    
    public static final String DEFAULT_ADDRESS = "127.0.0.1:8848";
    
    String namespace = "public";
    
    String serverAddr = DEFAULT_ADDRESS;
    
    String username;
    
    String password;
    
    public String getNamespace() {
        return namespace;
    }
    
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    public String getServerAddr() {
        return serverAddr;
    }
    
    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}

