package rs.ftn.dis.iot.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testovi za MeasurementConsumer — RedisTemplate/HashOperations su mokovani.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class MeasurementConsumerTest {

    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final HashOperations hashOps = mock(HashOperations.class);
    private final MeasurementConsumer consumer = new MeasurementConsumer(redisTemplate);

    @Test
    void stateUpdater_writesToRedisHash() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        Map<String, Object> m = Map.of("deviceId", "dev1", "temperature", 22.0);

        consumer.stateUpdater().accept(m);

        verify(hashOps).putAll("device:state:dev1", m);
    }

    @Test
    void stateUpdater_skipsWhenDeviceIdNull() {
        Map<String, Object> m = Map.of("temperature", 22.0); // nema deviceId

        consumer.stateUpdater().accept(m);

        verify(hashOps, never()).putAll(anyString(), anyMap());
    }

    @Test
    void getState_returnsHashEntries() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        Map<Object, Object> stored = Map.of("temperature", 22.0, "breach", false);
        when(hashOps.entries("device:state:dev1")).thenReturn(stored);

        assertThat(consumer.getState("dev1")).isEqualTo(stored);
    }
}
