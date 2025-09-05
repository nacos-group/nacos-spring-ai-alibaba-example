package komachi.sion.a2a.server.autoconfiguration.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.jsonrpc.handler.JSONRPCHandler;
import io.a2a.server.ServerCallContext;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.reactivestreams.FlowAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 *
 *
 * @author xiweng.yy
 */
@RestController
@RequestMapping("/a2a")
@ConditionalOnProperty(prefix = "spring.ai.alibaba.a2a", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentController {
    
    @Autowired
    private ObservationRegistry registry;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentController.class);
    
    private final JSONRPCHandler jsonRpcHandler;
    
    public AgentController(JSONRPCHandler jsonrpcHandler) {
        this.jsonRpcHandler = jsonrpcHandler;
    }
    
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object handleRequest(@RequestBody String body, @RequestHeader HttpHeaders headers) {
        boolean streaming = isStreamingRequest(body);
        ServerCallContext context = buildServerCallContext(headers, streaming);
        Object result = null;
        try {
            result = streaming ? handleStreamRequest(body, context) : handleNonStreamRequest(body, context);
        } catch (JsonProcessingException e) {
            result = new JSONRPCErrorResponse(null, new JSONParseError());
        }
        return result;
    }
    
    private static boolean isStreamingRequest(String requestBody) {
        try {
            JsonNode node = Utils.OBJECT_MAPPER.readTree(requestBody);
            JsonNode method = node != null ? node.get("method") : null;
            return method != null && (SendStreamingMessageRequest.METHOD.equals(method.asText())
                    || TaskResubscriptionRequest.METHOD.equals(method.asText()));
        } catch (Exception e) {
            return false;
        }
    }
    
    private Flux<?> handleStreamRequest(String body, ServerCallContext context) throws JsonProcessingException {
        Observation currentObservation = registry.getCurrentObservation();
        
        StreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, StreamingJSONRPCRequest.class);
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest req) {
            publisher = jsonRpcHandler.onMessageSendStream(req, context);
            LOGGER.info("get Stream publisher {}", publisher);
        } else if (request instanceof TaskResubscriptionRequest req) {
            publisher = jsonRpcHandler.onResubscribeToTask(req, context);
        } else {
            return Flux.just(generateErrorResponse(request, new UnsupportedOperationError()))
                    .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, currentObservation));
        } return Flux.from(FlowAdapters.toPublisher(publisher))
                .map((Function<JSONRPCResponse<?>, JSONRPCResponse<?>>) jsonrpcResponse -> {
                    //                    LOGGER.info("get response {}", jsonrpcResponse);
                    return jsonrpcResponse;
                    //                });
                    //                }).subscribeOn(Schedulers.parallel()).publishOn(Schedulers.parallel());
                }).delaySubscription(Duration.ofMillis(10))
                .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, currentObservation));
    }
    
    private JSONRPCResponse<?> handleNonStreamRequest(String body, ServerCallContext context)
            throws JsonProcessingException {
        NonStreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, NonStreamingJSONRPCRequest.class);
        if (request instanceof GetTaskRequest req) {
            return jsonRpcHandler.onGetTask(req, context);
        } else if (request instanceof CancelTaskRequest req) {
            return jsonRpcHandler.onCancelTask(req, context);
        } else if (request instanceof SetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.setPushNotificationConfig(req, context);
        } else if (request instanceof GetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.getPushNotificationConfig(req, context);
        } else if (request instanceof SendMessageRequest req) {
            return jsonRpcHandler.onMessageSend(req, context);
        } else if (request instanceof ListTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.listPushNotificationConfig(req, context);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.deletePushNotificationConfig(req, context);
        } else {
            return generateErrorResponse(request, new UnsupportedOperationError());
        }
    }
    
    private ServerCallContext buildServerCallContext(HttpHeaders httpRequest, boolean streaming) {
        Map<String, Object> state = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        for (String each : httpRequest.keySet()) {
            headers.put(each, httpRequest.getFirst(each));
        }
        state.put("headers", headers);
        state.put("streaming", streaming);
        return new ServerCallContext(null, state);
    }
    
    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }
}
