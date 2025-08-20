package komachi.sion.a2a.client.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 *
 *
 * @author xiweng.yy
 */
@RestController
@RequestMapping("/")
public class TestController {
    
    private final ChatClient dashScopeChatClient;
    
    public TestController(ChatClient chatClient) {
        this.dashScopeChatClient = chatClient;
    }
    
    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "你好，请随机生成一个数字") String query) {
        return dashScopeChatClient.prompt(query).call().content();
    }
    
    /**
     * ChatClient 流式调用
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(
            @RequestParam(value = "query", defaultValue = "你好，请随机生成一个数字") String query,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Flux<ChatClientResponse> chatClientResponse = dashScopeChatClient.prompt(query).stream().chatClientResponse();
        return chatClientResponse.mapNotNull(ChatClientResponse::chatResponse).map(new StreamResultFunction())
                .filter(StringUtils::hasLength);
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
