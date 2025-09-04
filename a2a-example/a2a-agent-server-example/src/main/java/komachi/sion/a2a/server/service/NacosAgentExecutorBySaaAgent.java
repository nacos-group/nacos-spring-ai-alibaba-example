package komachi.sion.a2a.server.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *
 *
 * @author xiweng.yy
 */
public class NacosAgentExecutorBySaaAgent implements AgentExecutor {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(NacosAgentExecutorBySaaAgent.class);
    
    private final BaseAgent executeAgent;
    
    public NacosAgentExecutorBySaaAgent(BaseAgent executeAgent) {
        this.executeAgent = executeAgent;
    }
    
    private Task new_task(Message request) {
        String context_id_str = request.getContextId();
        if (context_id_str == null || context_id_str.isEmpty()) {
            context_id_str = java.util.UUID.randomUUID().toString();
        }
        String id = java.util.UUID.randomUUID().toString();
        if (request.getTaskId() != null && !request.getTaskId().isEmpty()) {
            id = request.getTaskId();
        }
        return new Task(id, context_id_str, new TaskStatus(TaskState.SUBMITTED), null, List.of(request), null);
    }
    
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        try {
            Message message = context.getParams().message();
            StringBuilder sb = new StringBuilder();
            for (Part<?> each : message.getParts()) {
                if (Part.Kind.TEXT.equals(each.getKind())) {
                    sb.append(((TextPart) each).getText()).append("\n");
                }
            }
            Map<String, Object> input = Map.of("messages", List.of(new UserMessage(sb.toString().trim())));
            boolean isStreaming =
                    context.getCallContext().getState().containsKey("streaming") && (boolean) context.getCallContext()
                            .getState().get("streaming");
            if (isStreaming) {
                doTaskStream(input, context, eventQueue);
            } else {
                doTask(input, context, eventQueue);
            }
        } catch (Exception e) {
            LOGGER.error("Agent execution failed", e);
            eventQueue.enqueueEvent(A2A.toAgentMessage("Agent execution failed: " + e.getMessage()));
        }
    }
    
    private void doTaskStream(Map<String, Object> input, RequestContext context, EventQueue eventQueue)
            throws GraphStateException, GraphRunnerException {
        AsyncGenerator<NodeOutput> generator = executeAgent.stream(input);
        Task task = context.getTask();
        if (task == null) {
            task = new_task(context.getMessage());
            eventQueue.enqueueEvent(task);
        }
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        taskUpdater.submit();
        generator.forEachAsync(new ReactAgentNodeOutputConsumer(taskUpdater)).thenAccept(o -> taskUpdater.complete());
        waitTaskCompleted(task);
    }
    
    private void doTask(Map<String, Object> input, RequestContext context, EventQueue eventQueue)
            throws GraphStateException, GraphRunnerException {
        var result = executeAgent.invoke(input);
        String outputText = result.get().data().containsKey(executeAgent.outputKey()) ? String.valueOf(
                result.get().data().get(executeAgent.outputKey())) : "No output key in result.";
        
        Task task = context.getTask();
        if (task == null) {
            task = new_task(context.getMessage());
            eventQueue.enqueueEvent(task);
        }
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        boolean is_task_complete = true;
        boolean require_user_input = false;
        if (!is_task_complete && !require_user_input) {
            taskUpdater.startWork(taskUpdater.newAgentMessage(List.of(new TextPart(outputText)), Map.of()));
        } else if (require_user_input) {
            taskUpdater.startWork(taskUpdater.newAgentMessage(List.of(new TextPart(outputText)), Map.of()));
        } else {
            taskUpdater.addArtifact(List.of(new TextPart(outputText)), UUID.randomUUID().toString(),
                    "conversation_result", Map.of("output", outputText));
            taskUpdater.complete();
        }
    }
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    }
    
    private void waitTaskCompleted(Task task) {
        while (!task.getStatus().state().equals(TaskState.COMPLETED) && !task.getStatus().state()
                .equals(TaskState.CANCELED)) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
    }
    
    private class ReactAgentNodeOutputConsumer implements Consumer<NodeOutput> {
        
        private final TaskUpdater taskUpdater;
        
        private final AtomicInteger artifactNum;
        
        private ReactAgentNodeOutputConsumer(TaskUpdater taskUpdater) {
            this.taskUpdater = taskUpdater;
            this.artifactNum = new AtomicInteger();
        }
        
        @Override
        public void accept(NodeOutput nodeOutput) {
            if (nodeOutput.isSTART()) {
                return;
            }
            String content = "";
            String innerContent = "";
            if (nodeOutput instanceof StreamingOutput) {
                String chunkContent = ((StreamingOutput) nodeOutput).chunk();
                if (!StringUtils.hasLength(chunkContent)) {
                    return;
                }
                innerContent = JSON.toJSONString(Map.of(nodeOutput.node(), chunkContent));
                content = chunkContent;
            } else {
                JSONObject outputJson = new JSONObject();
                outputJson.put("data", nodeOutput.state().data());
                outputJson.put("node", nodeOutput.node());
                innerContent = JSON.toJSONString(outputJson);
            }
            LOGGER.info("innerContent: {}", innerContent);
            if ("preLlm".equals(nodeOutput.node()) || "postLlm".equals(nodeOutput.node())) {
                return;
            }
            if ("preTool".equals(nodeOutput.node())) {
                content = "正在调用Tool,请等待结果";
                Message message = taskUpdater.newAgentMessage(Collections.singletonList(new TextPart(content)),
                        Collections.emptyMap());
                taskUpdater.startWork(message);
                return;
            }
            if ("tool".equals(nodeOutput.node())) {
                content = "Tool调用已完成";
                Message message = taskUpdater.newAgentMessage(Collections.singletonList(new TextPart(content)),
                        Collections.emptyMap());
                taskUpdater.startWork(message);
                return;
            }
            if ("postTool".equals(nodeOutput.node())) {
                return;
            }
            
            taskUpdater.addArtifact(Collections.singletonList(new TextPart(content)), null,
                    String.valueOf(artifactNum.incrementAndGet()), Map.of());
        }
    }
}
