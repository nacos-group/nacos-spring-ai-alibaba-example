package komachi.sion.mcp.consumer.controller;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Function;

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
        return chatClientResponse.mapNotNull(ChatClientResponse::chatResponse).map(new StreamResultFunction())
                .filter(StringUtils::hasLength);
    }
    
    private Prompt buildPrompt(String query) {
        return promptTemplateFactory.getTemplate("nacos-prompt-cn").create(Map.of("query", query));
    }
    
    private class StreamResultFunction implements Function<ChatResponse, String> {
        
        private boolean isInAssistant = false;
        
        private boolean isInResult = false;
        
        @Override
        public String apply(ChatResponse response) {
            if (response.getResult() == null || response.getResult().getOutput() == null
                    || response.getResult().getOutput().getText() == null) {
                return "";
            }
            String output = "";
            if (StringUtils.hasLength(response.getResult().getOutput().getText())) {
                output = response.getResult().getOutput().getText();
                if (!isInResult) {
                    output = "<br/>------ Result ------<br/>" + output ;
                    isInResult = true;
                    isInAssistant = false;
                }
            } else {
                output = (String) response.getResult().getOutput().getMetadata().get("reasoningContent");
                if (!StringUtils.hasLength(output)) {
                    return output;
                }
                if (!isInAssistant) {
                    output = "<br/>------ Thinking ------<br/>" + output;
                    isInAssistant = true;
                    isInResult = false;
                }
            }
            return output;
        }
    }
}
