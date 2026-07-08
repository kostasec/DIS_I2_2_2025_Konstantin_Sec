package rs.ftn.dis.iot.deviceregistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracioni test: prava MySQL baza (Testcontainers).
 * Uređaj se snima kroz repozitorijum (pravi JPA + MySQL), a čita kroz REST endpoint —
 * proverava perzistenciju pragova, substituciju podrazumevanih i posejane uređaje.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DeviceRegistryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    DeviceRepository repository;

    @Autowired
    TestRestTemplate rest;

    @Test
    void persistsThresholds_andReadsViaEndpoint() {
        repository.save(new DeviceEntity("itDev", "Sensor", "Room", "multi", 0.03, 50.0));

        ResponseEntity<DeviceThresholds> resp =
                rest.getForEntity("/device/itDev/thresholds", DeviceThresholds.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().coThreshold()).isEqualTo(0.03);
        assertThat(resp.getBody().tempThreshold()).isEqualTo(50.0);
    }

    @Test
    void substitutesDefaults_whenThresholdsNull() {
        repository.save(new DeviceEntity("itDev2", "Sensor", "Room", "multi")); // pragovi null

        ResponseEntity<DeviceThresholds> resp =
                rest.getForEntity("/device/itDev2/thresholds", DeviceThresholds.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().coThreshold()).isEqualTo(0.01);   // default
        assertThat(resp.getBody().tempThreshold()).isEqualTo(40.0); // default
    }

    @Test
    void seededDevice_hasPerDeviceThresholds() {
        // DataInitializer seje Kitchen (00:0f:00:70:91:0a) sa co 0.02 / temp 45
        ResponseEntity<DeviceThresholds> resp =
                rest.getForEntity("/device/00:0f:00:70:91:0a/thresholds", DeviceThresholds.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().coThreshold()).isEqualTo(0.02);
        assertThat(resp.getBody().tempThreshold()).isEqualTo(45.0);
    }

    @Test
    void createsDeviceViaPost_andReadsBack() {
        DeviceEntity dev = new DeviceEntity("postDev", "Garage Sensor", "Garage", "multi", 0.015, 42.0);

        ResponseEntity<DeviceEntity> created = rest.postForEntity("/device", dev, DeviceEntity.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().getDeviceId()).isEqualTo("postDev"); // dokaz da deserijalizacija radi

        ResponseEntity<DeviceThresholds> thresholds =
                rest.getForEntity("/device/postDev/thresholds", DeviceThresholds.class);
        assertThat(thresholds.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(thresholds.getBody().coThreshold()).isEqualTo(0.015);
        assertThat(thresholds.getBody().tempThreshold()).isEqualTo(42.0);
    }
}
