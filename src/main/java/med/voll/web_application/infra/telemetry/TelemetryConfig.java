package med.voll.web_application.infra.telemetry;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelemetryConfig {

    private final MeterRegistry meterRegistry;

    public TelemetryConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeCustomMetrics();
    }

    private void initializeCustomMetrics() {
        // Doctor metrics
        meterRegistry.counter("doctors.registered.total");
        meterRegistry.counter("doctors.updated.total");
        meterRegistry.counter("doctors.deleted.total");
        meterRegistry.gauge("doctors.active", 0);

        // Appointment metrics
        meterRegistry.counter("appointments.booked.total");
        meterRegistry.counter("appointments.updated.total");
        meterRegistry.counter("appointments.cancelled.total");
        meterRegistry.gauge("appointments.pending", 0);

        // Error metrics
        meterRegistry.counter("errors.registration.total");
        meterRegistry.counter("errors.business.rules.total");
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDoctorRegistration() {
        meterRegistry.counter("doctors.registered.total").increment();
    }

    public void recordDoctorUpdate() {
        meterRegistry.counter("doctors.updated.total").increment();
    }

    public void recordDoctorDelete() {
        meterRegistry.counter("doctors.deleted.total").increment();
    }

    public void recordAppointmentBooked() {
        meterRegistry.counter("appointments.booked.total").increment();
    }

    public void recordAppointmentUpdated() {
        meterRegistry.counter("appointments.updated.total").increment();
    }

    public void recordAppointmentCancelled() {
        meterRegistry.counter("appointments.cancelled.total").increment();
    }

    public void recordRegistrationError() {
        meterRegistry.counter("errors.registration.total").increment();
    }

    public void recordBusinessRuleError() {
        meterRegistry.counter("errors.business.rules.total").increment();
    }
}
