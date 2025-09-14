package komachi.sion.a2a.client.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 *
 *
 * @author xiweng.yy
 */
@RestController
@RequestMapping("/")
public class TestController {
    
    private final BaseAgent rootAgent;
    
    public TestController(BaseAgent rootAgent) {
        this.rootAgent = rootAgent;
    }
    
    @GetMapping("test")
    public Object test(@RequestParam("question") String question) throws GraphStateException, GraphRunnerException {
        System.out.println(question);
        StringBuffer result = new StringBuffer();
        rootAgent.stream(Map.of("messages", List.of(new UserMessage(question)))).forEachAsync(output -> {
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
                System.out.println(innerContent);
                if (StringUtils.hasLength(content)) {
                    result.append(content);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
        return result.toString();
    }
}
