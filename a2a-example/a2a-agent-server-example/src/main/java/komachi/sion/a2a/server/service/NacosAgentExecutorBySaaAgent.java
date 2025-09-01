package komachi.sion.a2a.server.service;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
//            Map<String, Object> input = Map.of("input", sb.toString().trim());
            Map<String, Object> input = Map.of("messages", List.of(new UserMessage(sb.toString().trim())));
            var result = executeAgent.invoke(input);
            String outputText = result.get().data().containsKey("output")
                    ? String.valueOf(result.get().data().get("output")) : "No output key in result.";
            
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
            }
            else if (require_user_input) {
                taskUpdater.startWork(taskUpdater.newAgentMessage(List.of(new TextPart(outputText)), Map.of()));
            }
            else {
                taskUpdater.addArtifact(List.of(new TextPart(outputText)), UUID.randomUUID().toString(),
                        "conversation_result", Map.of("output", outputText));
                taskUpdater.complete();
            }
        }
        catch (Exception e) {
            LOGGER.error("Agent execution failed", e);
            eventQueue.enqueueEvent(A2A.toAgentMessage("Agent execution failed: " + e.getMessage()));
        }
    }
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    }
}
