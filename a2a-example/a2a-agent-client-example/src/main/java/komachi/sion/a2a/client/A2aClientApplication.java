package komachi.sion.a2a.client;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 *
 *
 * @author xiweng.yy
 */
@SpringBootApplication
public class A2aClientApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(A2aClientApplication.class, args);
    }
    
    @Bean
    public OpenTelemetry getOpenTelemetry() {
        return GlobalOpenTelemetry.get();
    }
}
