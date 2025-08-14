package org.example.service;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class MockDataService {
    
    private final Map<String, NamingService> nacosClientMaps;
    
    private final String mockServiceName = "random.service.name.%d";
    
    @Value("${nacos.address:127.0.0.1:8848}")
    private String nacosAddress;
    
    @Value("${nacos.subscriber.count:5}")
    private int nacosSubscriberCount;
    
    @Value("${nacos.subscribe.service.name:com.test.SyncCallbackService}")
    private String nacosSubscribeServiceName;
    
    public MockDataService() {
        this.nacosClientMaps = new HashMap<>();
    }
    
    @PostConstruct
    public void init() throws NacosException {
        buildNacosClient();
        doSubService();
        doRandomRegisterService();
    }
    
    private void buildNacosClient() throws NacosException {
        for (int i = 0; i < nacosSubscriberCount; i++) {
            NamingService namingService = NacosFactory.createNamingService(nacosAddress);
            nacosClientMaps.put(String.valueOf(i), namingService);
        }
    }
    
    private void doSubService() throws NacosException {
        for (int i = 0; i < nacosSubscriberCount; i++) {
            nacosClientMaps.get(String.valueOf(i)).subscribe(nacosSubscribeServiceName, event -> {
            });
        }
    }
    
    private void doRandomRegisterService() throws NacosException {
        Random random = new Random();
        for (int i = 0; i < nacosSubscriberCount; i++) {
            NamingService namingService = nacosClientMaps.get(String.valueOf(i));
            int randomId = random.nextInt(nacosSubscriberCount);
            String serviceName = String.format(mockServiceName, randomId);
            namingService.registerInstance(serviceName, "127.0.0.1", 8080 + i);
        }
    }
}
