package rs.ftn.dis.iot.alert;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testovi za AlertConsumer — RedisTemplate/ListOperations su mokovani.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class AlertConsumerTest {

    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final ListOperations listOps = mock(ListOperations.class);
    private final AlertConsumer consumer = new AlertConsumer(redisTemplate);

    @Test
    void alertHandler_pushesAlertToRedisList() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        Map<String, Object> measurement = Map.of("deviceId", "dev1", "temperature", 45.0, "breach", true);

        consumer.alertHandler().accept(measurement);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(listOps).rightPush(eq("device:alerts:dev1"), captor.capture());

        Map<String, Object> alert = (Map<String, Object>) captor.getValue();
        assertThat(alert).containsEntry("deviceId", "dev1");
        assertThat(alert).containsEntry("type", "THRESHOLD_BREACH");
        assertThat(alert).containsEntry("status", "ACTIVE");
        assertThat((String) alert.get("alertId")).startsWith("alert-");
        assertThat(alert).containsEntry("measurement", measurement);
        assertThat(alert).containsKey("createdAt");
    }

    @Test
    void alertHandler_skipsWhenDeviceIdNull() {
        Map<String, Object> measurement = Map.of("temperature", 45.0); // nema deviceId

        consumer.alertHandler().accept(measurement);

        verify(listOps, never()).rightPush(anyString(), any());
    }

    @Test
    void getAlerts_returnsList() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        List<Object> stored = List.of(Map.of("alertId", "alert-123"));
        when(listOps.range("device:alerts:dev1", 0, -1)).thenReturn(stored);

        assertThat(consumer.getAlerts("dev1")).isEqualTo(stored);
    }

    @Test
    void getAlerts_nullFromRedis_returnsEmptyList() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range("device:alerts:dev1", 0, -1)).thenReturn(null);

        assertThat(consumer.getAlerts("dev1")).isEmpty();
    }
}
