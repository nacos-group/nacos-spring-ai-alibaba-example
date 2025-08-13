package komachi.sion.a2a.server.autoconfiguration;

import io.a2a.jsonrpc.handler.JSONRPCHandler;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCard;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 *
 * @author xiweng.yy
 */
@Configuration
public class AgentHandlerConfiguration {
    
    private ExecutorService executor;
    
    @PostConstruct
    public void init() {
        executor = Executors.newCachedThreadPool();
    }
    
    @PreDestroy
    public void close() {
        executor.shutdown();
    }
    
    @Bean
    public Executor executor() {
        return executor;
    }
    
    @Bean
    public JSONRPCHandler jsonrpcHandler(AgentCard agentCard, RequestHandler requestHandler) {
        return new JSONRPCHandler(agentCard, requestHandler);
    }
    
    @Bean
    public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager,
            PushNotificationConfigStore pushConfigStore, PushNotificationSender pushSender) {
        return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore, pushSender, executor);
    }
    
    @Bean
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }
    
    @Bean
    public QueueManager queueManager() {
        return new InMemoryQueueManager();
    }
    
    @Bean
    public PushNotificationConfigStore pushConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }
    
    @Bean
    public PushNotificationSender pushSender(PushNotificationConfigStore pushConfigStore) {
        return new BasePushNotificationSender(pushConfigStore);
    }
}
