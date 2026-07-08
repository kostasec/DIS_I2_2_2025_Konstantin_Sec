package rs.ftn.dis.iot.telemetrycomposite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class TelemetryCompositeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryCompositeServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}