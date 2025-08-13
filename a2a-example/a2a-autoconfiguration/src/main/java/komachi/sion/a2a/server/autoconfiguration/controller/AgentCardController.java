package komachi.sion.a2a.server.autoconfiguration.controller;

import io.a2a.jsonrpc.handler.JSONRPCHandler;
import io.a2a.spec.AgentCard;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 *
 * @author xiweng.yy
 */
@RestController
@RequestMapping("/")
public class AgentCardController {
    
    private final JSONRPCHandler jsonRpcHandler;
    
    public AgentCardController(JSONRPCHandler jsonRpcHandler) {
        this.jsonRpcHandler = jsonRpcHandler;
    }
    
    @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentCard getAgentCard() {
        return jsonRpcHandler.getAgentCard();
    }
    
}
