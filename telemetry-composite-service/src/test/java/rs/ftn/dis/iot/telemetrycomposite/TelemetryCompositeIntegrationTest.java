package rs.ftn.dis.iot.telemetrycomposite;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit test za composite agregaciju — nizvodni pozivi su mokovani (MockRestServiceServer),
 * circuit breaker je pravi Resilience4j. Proverava spajanje odgovora i fallback kad servis padne.
 */
class TelemetryCompositeIntegrationTest {

    private MockRestServiceServer server;
    private TelemetryCompositeIntegration integration;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        CircuitBreakerFactory<?, ?> circuitBreakerFactory = new Resilience4JCircuitBreakerFactory(
                CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(), null);
        integration = new TelemetryCompositeIntegration(restTemplate, circuitBreakerFactory);
    }

    @Test
    void aggregatesAllThreeSources() {
        server.expect(requestTo("http://device-registry-service:8081/device/dev1"))
                .andRespond(withSuccess(
                        "{\"deviceId\":\"dev1\",\"name\":\"Sensor\",\"location\":\"Kitchen\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://monitoring-service:8082/monitoring/dev1/state"))
                .andRespond(withSuccess(
                        "{\"deviceId\":\"dev1\",\"temperature\":22.5,\"breach\":false}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://alert-service:8083/alert/dev1"))
                .andRespond(withSuccess(
                        "[{\"alertId\":\"alert-1\",\"status\":\"ACTIVE\"}]",
                        MediaType.APPLICATION_JSON));

        TelemetryAggregate aggregate = integration.getAggregate("dev1");

        assertThat(aggregate.getDevice()).containsEntry("location", "Kitchen");
        assertThat(aggregate.getState()).containsEntry("temperature", 22.5);
        assertThat(aggregate.getAlerts()).hasSize(1);
        server.verify();
    }

    @Test
    void fallsBackToPartialAggregate_whenAlertServiceDown() {
        server.expect(requestTo("http://device-registry-service:8081/device/dev1"))
                .andRespond(withSuccess(
                        "{\"deviceId\":\"dev1\",\"location\":\"Kitchen\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://monitoring-service:8082/monitoring/dev1/state"))
                .andRespond(withSuccess(
                        "{\"temperature\":22.5}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://alert-service:8083/alert/dev1"))
                .andRespond(withServerError()); // alert-service pada

        TelemetryAggregate aggregate = integration.getAggregate("dev1");

        // device i state su i dalje tu; alerts padaju na prazan fallback
        assertThat(aggregate.getDevice()).containsEntry("location", "Kitchen");
        assertThat(aggregate.getState()).containsEntry("temperature", 22.5);
        assertThat(aggregate.getAlerts()).isEmpty();
    }
}
