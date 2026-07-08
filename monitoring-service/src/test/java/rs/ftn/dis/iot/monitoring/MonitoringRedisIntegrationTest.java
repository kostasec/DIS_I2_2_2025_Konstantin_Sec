package rs.ftn.dis.iot.monitoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracioni test: pravi Redis (Testcontainers).
 * MeasurementConsumer se instancira nad pravim RedisTemplate-om (ista serijalizacija kao RedisConfig)
 * i proverava round-trip upisa/čitanja stanja uređaja.
 */
@Testcontainers
class MonitoringRedisIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2").withExposedPorts(6379);

    private LettuceConnectionFactory factory;
    private MeasurementConsumer consumer;

    @BeforeEach
    void setUp() {
        factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();

        consumer = new MeasurementConsumer(template);
    }

    @AfterEach
    void tearDown() {
        factory.destroy();
    }

    @Test
    void writesAndReadsStateFromRealRedis() {
        Map<String, Object> measurement = new HashMap<>();
        measurement.put("deviceId", "dev1");
        measurement.put("temperature", 22.5);
        measurement.put("breach", false);

        consumer.stateUpdater().accept(measurement);
        Map<Object, Object> state = consumer.getState("dev1");

        assertThat(state).containsEntry("deviceId", "dev1");
        assertThat(state).containsEntry("temperature", 22.5);
        assertThat(state).containsEntry("breach", false);
    }

    @Test
    void getState_emptyForUnknownDevice() {
        assertThat(consumer.getState("unknown")).isEmpty();
    }
}
