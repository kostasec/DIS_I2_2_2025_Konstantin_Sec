package rs.ftn.dis.iot.processing;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integracioni test: pravi Kafka broker (Testcontainers).
 * Proverava da processing rutira breach na 'threshold-breaches', a normalno na 'valid-measurements'.
 * ThresholdService je mokovan (fiksni pragovi) da bi se izolovao Kafka tok od registry HTTP poziva.
 */
@SpringBootTest
@Testcontainers
class ProcessingRoutingIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @MockitoBean
    ThresholdService thresholdService;

    @BeforeEach
    void stubThresholds() {
        when(thresholdService.getThresholds(any()))
                .thenReturn(new DeviceThresholds("x", 0.01, 40.0));
    }

    @Test
    void routesBreachAndNormalToCorrectTopics() {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(
                producerProps(), new StringSerializer(), new StringSerializer());
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                     consumerProps(), new StringDeserializer(), new StringDeserializer())) {

            consumer.subscribe(List.of("valid-measurements", "threshold-breaches"));

            producer.send(new ProducerRecord<>("raw-measurements",
                    "{\"deviceId\":\"breachDev\",\"co\":0.05,\"temperature\":20}"));
            producer.send(new ProducerRecord<>("raw-measurements",
                    "{\"deviceId\":\"normalDev\",\"co\":0.005,\"temperature\":20}"));
            producer.flush();

            Map<String, String> topicByDevice = new HashMap<>();
            await().atMost(30, TimeUnit.SECONDS).until(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    if (r.value().contains("breachDev")) topicByDevice.put("breachDev", r.topic());
                    if (r.value().contains("normalDev")) topicByDevice.put("normalDev", r.topic());
                }
                return topicByDevice.containsKey("breachDev") && topicByDevice.containsKey("normalDev");
            });

            assertThat(topicByDevice.get("breachDev")).isEqualTo("threshold-breaches");
            assertThat(topicByDevice.get("normalDev")).isEqualTo("valid-measurements");
        }
    }

    private Properties producerProps() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        return p;
    }

    private Properties consumerProps() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "it-verifier");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return p;
    }
}
