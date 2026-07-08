package rs.ftn.dis.iot.processing;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ThresholdConfig {

    /**
     * Load-balanced RestClient.Builder — poziv ka "device-registry-service"
     * se razrešava preko Eureka service discovery-ja (klijentski load balancing).
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
