package rs.ftn.dis.iot.processing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit testovi za ThresholdService — HTTP sloj je mokovan (MockRestServiceServer),
 * pa se testira kesiranje, obrada null-a i fallback na podrazumevane pragove.
 */
class ThresholdServiceTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private ThresholdService service;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new ThresholdService(builder);
    }

    @Test
    void fetchesThresholdsFromRegistry() {
        server.expect(requestTo("http://device-registry-service/device/dev1/thresholds"))
                .andRespond(withSuccess(
                        "{\"deviceId\":\"dev1\",\"coThreshold\":0.02,\"tempThreshold\":45.0}",
                        MediaType.APPLICATION_JSON));

        DeviceThresholds t = service.getThresholds("dev1");

        assertThat(t.deviceId()).isEqualTo("dev1");
        assertThat(t.coThreshold()).isEqualTo(0.02);
        assertThat(t.tempThreshold()).isEqualTo(45.0);
        server.verify();
    }

    @Test
    void cachesThresholds_secondCallDoesNotHitRegistry() {
        server.expect(requestTo("http://device-registry-service/device/dev1/thresholds"))
                .andRespond(withSuccess(
                        "{\"deviceId\":\"dev1\",\"coThreshold\":0.02,\"tempThreshold\":45.0}",
                        MediaType.APPLICATION_JSON));

        DeviceThresholds first = service.getThresholds("dev1");
        DeviceThresholds second = service.getThresholds("dev1"); // treba iz kesa

        assertThat(second).isEqualTo(first);
        server.verify(); // tačno jedan HTTP poziv je bio očekivan
    }

    @Test
    void nullDeviceId_returnsDefaults_withoutHttp() {
        DeviceThresholds t = service.getThresholds(null);

        assertThat(t.coThreshold()).isEqualTo(ThresholdService.DEFAULT_CO_THRESHOLD);
        assertThat(t.tempThreshold()).isEqualTo(ThresholdService.DEFAULT_TEMP_THRESHOLD);
        server.verify(); // nijedan HTTP poziv nije očekivan
    }

    @Test
    void registryFailure_fallsBackToDefaults() {
        server.expect(requestTo("http://device-registry-service/device/dev1/thresholds"))
                .andRespond(withServerError());

        DeviceThresholds t = service.getThresholds("dev1");

        assertThat(t.coThreshold()).isEqualTo(ThresholdService.DEFAULT_CO_THRESHOLD);
        assertThat(t.tempThreshold()).isEqualTo(ThresholdService.DEFAULT_TEMP_THRESHOLD);
    }
}
