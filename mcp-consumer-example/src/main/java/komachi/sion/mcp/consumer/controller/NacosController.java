package komachi.sion.mcp.consumer.controller;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 *
 *
 * @author xiweng.yy
 */
@RestController
@RequestMapping("/nacos")
public class NacosController {
    
    private final ChatClient dashScopeChatClient;
    
    private final ConfigurablePromptTemplateFactory promptTemplateFactory;
    
    public NacosController(ChatClient chatClient, ConfigurablePromptTemplateFactory promptTemplateFactory) {
        this.dashScopeChatClient = chatClient;
        this.promptTemplateFactory = promptTemplateFactory;
    }
    
    @GetMapping("/simple/chat")
    public String simpleChat(
            @RequestParam(value = "query", defaultValue = "你好，我的环境中有哪些Nacos集群？") String query) {
        return dashScopeChatClient.prompt(buildPrompt(query)).call().content();
    }
    
    /**
     * ChatClient 流式调用
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(
            @RequestParam(value = "query", defaultValue = "你好，我的环境中有哪些Nacos集群？") String query,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Flux<ChatClientResponse> chatClientResponse = dashScopeChatClient.prompt(buildPrompt(query)).stream()
                .chatClientResponse();
        return chatClientResponse.mapNotNull(ChatClientResponse::chatResponse).map(r -> {
            if (r.getResult() == null || r.getResult().getOutput() == null
                    || r.getResult().getOutput().getText() == null) {
                return "";
            }
            if (StringUtils.hasLength(r.getResult().getOutput().getText())) {
                return r.getResult().getOutput().getText();
            }
            return (String) r.getResult().getOutput().getMetadata().get("reasoningContent");
        }).filter(StringUtils::hasLength);
    }
    
    private Prompt buildPrompt(String query) {
        return promptTemplateFactory.getTemplate("nacos-prompt").create(Map.of("query", query));
    }
}
