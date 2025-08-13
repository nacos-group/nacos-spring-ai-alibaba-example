package komachi.sion.a2a.client.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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
    
}
