package komachi.sion.a2a.server.service;

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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author xiweng.yy
 */
@Service
public class NacosAgentExecutor implements AgentExecutor {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(NacosAgentExecutor.class);
    
    private final ChatClient chatClient;
    
    public NacosAgentExecutor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        Message message = context.getParams().message();
        StringBuilder sb = new StringBuilder();
        for (Part<?> each : message.getParts()) {
            if (Part.Kind.TEXT.equals(each.getKind())) {
                sb.append(((TextPart) each).getText()).append("\n");
            } else {
                LOGGER.warn("Ignore Unsupported part kind: {}", each.getKind());
            }
        }
        if (null != context.getParams().configuration() && context.getParams().configuration().blocking()) {
            String result = chatClient.prompt(sb.toString()).call().content();
            eventQueue.enqueueEvent(A2A.toAgentMessage(result));
        } else {
            doAsyncTask(context, eventQueue, sb.toString());
        }
    }
    
    private void doAsyncTask(RequestContext context, EventQueue eventQueue, String string) {
        Task task = buildNewTaskIfAbsent(context, eventQueue);
        TaskUpdater updater = buildNewTask(context, eventQueue);
        Flux<ChatResponse> chatResponse = chatClient.prompt(string).stream().chatResponse();
        chatResponse.subscribe(new TokenByTokenSubscriber(updater, context));
//        chatResponse.subscribe(new TypedSubscriber(updater, context));
        waitTaskCompleted(task);
    }
    
    private Task buildNewTaskIfAbsent(RequestContext context, EventQueue eventQueue) {
        Task task = context.getTask();
        if (null == task) {
            task = new Task.Builder().id(context.getTaskId()).contextId(context.getContextId())
                    .status(new TaskStatus(TaskState.SUBMITTED)).history(context.getMessage()).build();
            eventQueue.enqueueEvent(task);
        }
        return task;
    }
    
    private TaskUpdater buildNewTask(RequestContext context, EventQueue eventQueue) {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.startWork();
        return updater;
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
    
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        Task task = context.getTask();
        if (null == task) {
            return;
        }
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.cancel();
    }
    
    private Message buildMessage(RequestContext context, ChatResponse response) {
        AssistantMessage message = response.getResult().getOutput();
        Message result = null;
        Map<String, Object> responseMetadata = message.getMetadata();
        if (responseMetadata.containsKey("reasoningContent") && StringUtils.hasLength(
                responseMetadata.get("reasoningContent").toString())) {
            result = new Message.Builder().contextId(context.getContextId()).role(Message.Role.AGENT)
                    .parts(new TextPart((String) message.getMetadata().get("reasoningContent")))
                    .metadata(Collections.singletonMap("messageType", message.getMetadata().get("messageType")))
                    .build();
        } else {
            result = new Message.Builder().contextId(context.getContextId()).role(Message.Role.AGENT)
                    .parts(new TextPart(message.getText())).build();
        }
        return result;
    }
    
    private class TokenByTokenSubscriber implements Subscriber<ChatResponse> {
        
        protected final TaskUpdater updater;
        
        protected final RequestContext context;
        
        protected Subscription subscription;
        
        public TokenByTokenSubscriber(TaskUpdater updater, RequestContext context) {
            this.updater = updater;
            this.context = context;
        }
        
        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            this.subscription.request(1);
        }
        
        @Override
        public void onNext(ChatResponse chatResponse) {
            Message message = buildMessage(context, chatResponse);
            updater.startWork(message);
            this.subscription.request(1);
        }
        
        @Override
        public void onError(Throwable throwable) {
            updater.fail(A2A.toAgentMessage(throwable.getMessage()));
        }
        
        @Override
        public void onComplete() {
            updater.complete();
        }
    }
    
    private class TypedSubscriber extends TokenByTokenSubscriber {
        
        private String status = "starting";
        
        private StringBuffer llmThinking = new StringBuffer();
        
        private StringBuffer llmResult = new StringBuffer();
        
        public TypedSubscriber(TaskUpdater updater, RequestContext context) {
            super(updater, context);
        }
        
        @Override
        public void onNext(ChatResponse chatResponse) {
            try {
                Message message = buildMessage(context, chatResponse);
                if (null != message.getMetadata() && message.getMetadata().containsKey("messageType")) {
                    status = "thinking";
                    llmThinking.append(((TextPart) message.getParts().get(0)).getText());
                } else {
                    if ("thinking".equals(status)) {
                        Message thinkingMessage = updater.newAgentMessage(
                                Collections.singletonList(new TextPart(llmThinking.toString())),
                                Collections.singletonMap("messageType", "ASSISTANT"));
                        updater.startWork(thinkingMessage);
                    }
                    status = "result";
                    llmResult.append(((TextPart) message.getParts().get(0)).getText());
                }
            } catch (Exception e) {
                LOGGER.error("onNext in TypedSubscriber", e);
            } finally {
                this.subscription.request(1);
            }
        }
        
        @Override
        public void onComplete() {
            try {
                updater.addArtifact(Collections.singletonList(new TextPart(llmResult.toString())), null, "answers",
                        Collections.emptyMap());
                updater.complete();
            } catch (Exception e) {
                LOGGER.error("onComplete in TypedSubscriber", e);
            }
        }
    }
}
