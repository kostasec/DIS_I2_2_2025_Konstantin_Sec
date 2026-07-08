package rs.ftn.dis.iot.processing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;
import java.util.function.Function;

@Configuration
public class MeasurementProcessor {

    private final ThresholdService thresholdService;

    public MeasurementProcessor(ThresholdService thresholdService) {
        this.thresholdService = thresholdService;
    }

    @Bean
    public Function<Map<String, Object>, Message<Map<String, Object>>> processRawMeasurement() {
        return measurement -> {
            System.out.println("Processing measurement: " + measurement);

            String deviceId = (String) measurement.get("deviceId");
            double co = toDouble(measurement.getOrDefault("co", 0.0));
            double temp = toDouble(measurement.getOrDefault("temperature", 0.0));

            // Pragovi se sinhrono povlače iz device-registry-service po uređaju.
            DeviceThresholds thresholds = thresholdService.getThresholds(deviceId);
            boolean breach = co > thresholds.coThreshold() || temp > thresholds.tempThreshold();

            measurement.put("validated", true);
            measurement.put("breach", breach);

            String destination = breach ? "threshold-breaches" : "valid-measurements";

            return MessageBuilder
                    .withPayload(measurement)
                    .setHeader("spring.cloud.stream.sendto.destination", destination)
                    .build();
        };
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}
