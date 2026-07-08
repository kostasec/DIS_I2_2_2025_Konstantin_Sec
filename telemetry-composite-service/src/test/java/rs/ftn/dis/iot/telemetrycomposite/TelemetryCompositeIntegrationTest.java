package rs.ftn.dis.iot.telemetrycomposite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit test za composite agregaciju — tri nizvodna poziva su mokovana (MockRestServiceServer).
 * Proverava da se odgovori registry-ja, monitoring-a i alert-a spajaju u jedan agregat.
 */
class TelemetryCompositeIntegrationTest {

    private MockRestServiceServer server;
    private TelemetryCompositeIntegration integration;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        integration = new TelemetryCompositeIntegration(restTemplate);
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
}
