package med.voll.web_application.infra.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StructuredLogger {

    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    private final Tracer tracer;
    private final String applicationName = "vollmed-web";

    public StructuredLogger() {
        this.tracer = GlobalOpenTelemetry.getTracer(applicationName);
    }

    public void info(String message, Map<String, String> attributes) {
        logWithAttributes(message, "INFO", attributes);
    }

    public void info(String message) {
        logWithAttributes(message, "INFO", new HashMap<>());
    }

    public void debug(String message, Map<String, String> attributes) {
        logWithAttributes(message, "DEBUG", attributes);
    }

    public void debug(String message) {
        logWithAttributes(message, "DEBUG", new HashMap<>());
    }

    public void warn(String message, Map<String, String> attributes) {
        logWithAttributes(message, "WARN", attributes);
    }

    public void warn(String message) {
        logWithAttributes(message, "WARN", new HashMap<>());
    }

    public void error(String message, Map<String, String> attributes) {
        logWithAttributes(message, "ERROR", attributes);
    }

    public void error(String message) {
        logWithAttributes(message, "ERROR", new HashMap<>());
    }

    public void error(String message, Exception exception, Map<String, String> attributes) {
        attributes.put("exception.type", exception.getClass().getSimpleName());
        attributes.put("exception.message", exception.getMessage());
        logWithAttributes(message, "ERROR", attributes);
    }

    private void logWithAttributes(String message, String severity, Map<String, String> attributes) {
        try {
            // Log via SLF4J
            logger.info("{} [{}] - {}", severity, applicationName, message);
            
            // Add telemetry via OpenTelemetry Tracer
            Span span = tracer.spanBuilder(message)
                    .setAttribute("log.level", severity)
                    .setAttribute("application", applicationName)
                    .setAttribute("timestamp", System.currentTimeMillis())
                    .startSpan();
            
            // Add custom attributes
            attributes.forEach((key, value) -> {
                try {
                    span.setAttribute(key, value);
                } catch (Exception e) {
                    logger.debug("Failed to set attribute {} = {}", key, value, e);
                }
            });
            
            span.end();
        } catch (Exception e) {
            logger.warn("Failed to emit telemetry log: {}", e.getMessage(), e);
        }
    }

    public static Map<String, String> createAttributes(String... keyValues) {
        Map<String, String> attributes = new HashMap<>();
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must have even length");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            attributes.put(keyValues[i], keyValues[i + 1]);
        }
        return attributes;
    }
}

