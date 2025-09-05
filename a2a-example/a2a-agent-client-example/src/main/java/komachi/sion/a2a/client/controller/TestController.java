package komachi.sion.a2a.client.controller;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 *
 *
 * @author xiweng.yy
 */
@RestController
@RequestMapping("/")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    private final ChatClient dashScopeChatClient;
    
    private final BaseAgent baseAgent;
    
    public TestController(@Qualifier("nacosChatClient") ChatClient chatClient, BaseAgent baseAgent) {
        this.dashScopeChatClient = chatClient;
        this.baseAgent = baseAgent;
    }
    
    @GetMapping("/simple/chat")
    public String simpleChat(
            @RequestParam(value = "query", defaultValue = "你好，能告诉我一些关于阿里云的信息吗") String query) {
        return dashScopeChatClient.prompt(query).call().content();
    }
    
    /**
     * ChatClient 流式调用
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(
            @RequestParam(value = "query", defaultValue = "你好，能告诉我一些关于阿里云的信息吗") String query) {
        Flux<ChatClientResponse> chatClientResponse = dashScopeChatClient.prompt(query).stream().chatClientResponse();
        return chatClientResponse.mapNotNull(ChatClientResponse::chatResponse).map(new StreamResultFunction())
                .filter(StringUtils::hasLength);
    }
    
    @GetMapping("/agent/chat")
    public String agentChat(
            @RequestParam(value = "query", defaultValue = "你好，能告诉我一些关于阿里云的信息吗") String query)
            throws GraphStateException, GraphRunnerException {
        return baseAgent.invoke(Map.of("input", query)).orElseThrow().toString();
    }
    
    @GetMapping("/agent/stream")
    public Flux<String> agentStream(
            @RequestParam(value = "query", defaultValue = "你好，能告诉我一些关于阿里云的信息吗") String query)
            throws GraphStateException, GraphRunnerException {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
//        Context current = Context.current();
        new Thread(() -> {
//            try (Scope scope = current.makeCurrent()) {
            try {
                baseAgent.stream(Map.of("input", query)).forEachAsync(output -> {
                    try {
                        String nodeName = output.node();
                        String content = "";
                        String innerContent = "";
                        if (output instanceof StreamingOutput streamingOutput) {
                            innerContent = JSON.toJSONString(Map.of(nodeName, streamingOutput.chunk()));
                            content = streamingOutput.chunk();
                        } else {
                            JSONObject nodeOutput = new JSONObject();
                            nodeOutput.put("data", output.state().data());
                            nodeOutput.put("node", nodeName);
                            innerContent = JSON.toJSONString(nodeOutput);
                        }
                        logger.info("innerContent: {}", innerContent);
                        if (StringUtils.hasLength(content)) {
                            sink.tryEmitNext(content);
                        }
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }).thenAccept(v -> {
                    // 正常完成
                    sink.tryEmitComplete();
                }).exceptionally(e -> {
                    sink.tryEmitError(e);
                    return null;
                });
            } catch (GraphStateException | GraphRunnerException e) {
                throw new RuntimeException(e);
            }
        }).start();
        
        return sink.asFlux().subscribeOn(Schedulers.parallel()).publishOn(Schedulers.parallel())
                .doOnCancel(() -> logger.info("Client disconnected from stream"))
                .doOnError(e -> logger.error("Error occurred during streaming", e));
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
                    output = "<br/>------ Result ------<br/>" + output;
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
