package rs.ftn.dis.iot.deviceregistry;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

/**
 * Unit testovi za DataInitializer — proverava seed, backfill i no-op ponašanje.
 * run() seje 3 ugrađena uređaja.
 */
class DataInitializerTest {

    private final DeviceRepository repository = mock(DeviceRepository.class);
    private final DataInitializer initializer = new DataInitializer(repository);

    @Test
    void seedsNewDevices_whenAbsent() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());

        initializer.run();

        verify(repository, times(3)).save(any(DeviceEntity.class));
    }

    @Test
    void backfillsThresholds_whenNull() {
        // Svaki poziv findById vraća svež uređaj sa null pragovima.
        when(repository.findById(anyString())).thenAnswer(inv ->
                Optional.of(new DeviceEntity(inv.getArgument(0), "n", "l", "multi")));

        initializer.run();

        ArgumentCaptor<DeviceEntity> captor = ArgumentCaptor.forClass(DeviceEntity.class);
        verify(repository, times(3)).save(captor.capture());
        captor.getAllValues().forEach(d -> {
            assertThat(d.getCoThreshold()).isNotNull();
            assertThat(d.getTempThreshold()).isNotNull();
        });
    }

    @Test
    void noOp_whenDevicesFullyPopulated() {
        when(repository.findById(anyString())).thenAnswer(inv ->
                Optional.of(new DeviceEntity(inv.getArgument(0), "n", "l", "multi", 0.01, 40.0)));

        initializer.run();

        verify(repository, never()).save(any());
    }
}
