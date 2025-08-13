package komachi.sion.a2a.client.remote;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

/**
 *
 *
 * @author xiweng.yy
 */
public interface RemoteAgent {
    
    Map<String, Object> remoteAgentInfo() throws JsonProcessingException;
}
