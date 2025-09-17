package komachi.sion.a2a.client.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 *
 * @author xiweng.yy
 */
@RestController
@RequestMapping("/")
public class TestController {
    
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestController.class);
    
    private final BaseAgent rootAgent;
    
    public TestController(BaseAgent rootAgent) {
        this.rootAgent = rootAgent;
    }
    
    @GetMapping("sync")
    public Object sync(@RequestParam("question") String question) throws GraphStateException, GraphRunnerException {
        System.out.println(question);
        return rootAgent.invoke(Map.of("messages", List.of(new UserMessage(question)))).orElseThrow().value("messages")
                .orElseThrow();
    }
    
    @GetMapping("stream")
    public Flux<String> stream(@RequestParam("question") String question) throws GraphStateException, GraphRunnerException {
        System.out.println(question);
        return rootAgent.stream(Map.of("messages", List.of(new UserMessage(question)))).map(output -> {
            LOGGER.info("[SELF-DEBUG] stream agent invoke : `{}`", output.toString());
            return output.toString();
        });
    }
}
