package komachi.sion.mcp.provider.service;

import com.alibaba.nacos.client.utils.ClientBasicParamUtil;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * @author xiweng.yy
 */
@Service
public class NacosProviderService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NacosProviderService.class);
    
    private static final String NACOS_ACCESS_TOKEN_URL = "%s/nacos/v3/auth/user/login";
    
    private final Map<String, NacosInfo> nacosHostMap;
    
    public NacosProviderService() {
        this.nacosHostMap = new ConcurrentHashMap<>(2);
    }
    
    @Tool(description = "List all nacos clusters already registered and login")
    public String listNacosClusters() {
        List<String> result = new ArrayList<>(nacosHostMap.keySet());
        return JacksonUtils.toJson(result);
    }
    
    @Tool(description = "Get nacos detail information by nacos cluster name, the information includes nacos hosts and accessToken, accessToken is optional.")
    public String getNacosInformation(@ToolParam(description = "nacos cluster name") String nacosName) {
        NacosInfo nacosInfo = nacosHostMap.get(nacosName);
        if (null == nacosInfo) {
            return String.format(
                    "Nacos cluster name `%s` not found, Please use Tool `loginNacos` to login and register first.",
                    nacosName);
        }
        NacosInfo result = new NacosInfo();
        result.nacosHost = nacosInfo.nacosHost;
        result.accessToken = nacosInfo.accessToken;
        result.username = nacosInfo.username;
        result.password = ClientBasicParamUtil.desensitiseParameter(nacosInfo.password);
        return JacksonUtils.toJson(result);
    }
    
    @Tool(description = "Login and register nacos cluster.")
    public String loginNacos(@ToolParam(description = "nacos cluster name") String nacosName,
            @ToolParam(description = "nacos cluster access host with port, if port missing, default is 8848") String nacosHost,
            @ToolParam(description = "nacos cluster username", required = false) String username,
            @ToolParam(description = "nacos cluster password", required = false) String password) {
        NacosInfo nacosInfo = new NacosInfo();
        nacosInfo.nacosHost = nacosHost;
        nacosHostMap.put(nacosName, nacosInfo);
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            nacosInfo.username = username;
            nacosInfo.password = password;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                if (!nacosHost.startsWith("http")) {
                    nacosHost = "http://" + nacosHost;
                }
                String uri = String.format(NACOS_ACCESS_TOKEN_URL, nacosHost);
                HttpPost httpPost = new HttpPost(uri);
                List<NameValuePair> formParams = new ArrayList<>();
                formParams.add(new BasicNameValuePair("username", username));
                formParams.add(new BasicNameValuePair("password", password));
                httpPost.setEntity(new UrlEncodedFormEntity(formParams));
                try (CloseableHttpResponse response = client.execute(httpPost)) {
                    // 获取状态码
                    // 获取响应内容
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JsonNode responseObj = JacksonUtils.toObj(responseBody);
                    nacosInfo.accessToken = responseObj.get("accessToken").asText();
                }
            } catch (IOException | ParseException e) {
                LOGGER.error("Login to Nacos cluster `{}` failed. ", nacosHost, e);
                return String.format("Login to Nacos cluster `%s` failed %s. ", nacosHost, e.getMessage());
            }
        }
        return String.format("Login to Nacos cluster `%s` success.", nacosHost);
    }
    
    public static class NacosInfo {
        
        public String nacosHost;
        
        public String accessToken;
        
        public String username;
        
        public String password;
    }
}
