# Structured Telemetry Logging Guide

## Overview
This application uses OpenTelemetry with structured logging to track telemetry events with attributes, similar to the pattern requested.

## Structured Logger Usage

The `StructuredLogger` class provides a convenient way to emit telemetry logs with attributes.

### Basic Example

```java
@Service
public class MyService {
    
    private final StructuredLogger structuredLogger;
    
    public MyService(StructuredLogger structuredLogger) {
        this.structuredLogger = structuredLogger;
    }
    
    public void processCustomer(String customerId) {
        var attributes = StructuredLogger.createAttributes(
            "customer.id", customerId,
            "operation", "customer-search",
            "application", "vollmed-web"
        );
        structuredLogger.info("Customer query performed", attributes);
    }
}
```

### Log Levels

- `info()` - Informational messages
- `debug()` - Debug level messages
- `warn()` - Warning messages
- `error()` - Error messages with optional exception

### Error Example with Exception

```java
try {
    // operation
} catch (Exception e) {
    var attributes = StructuredLogger.createAttributes(
        "operation", "customer-search",
        "customer.id", "12345"
    );
    structuredLogger.error("Failed to search customer", e, attributes);
}
```

## How It Works

1. **SLF4J Logging**: All messages are logged to SLF4J with the log level and application name
2. **OpenTelemetry Tracing**: Creates spans with custom attributes for distributed tracing
3. **Attributes**: Custom attributes are added as span attributes for telemetry collection

## Current Implementation

- **MedicoService**: Logs all doctor operations (list, register, update, delete) with structured attributes
- **ConsultaService**: Logs all appointment operations with detailed context

## Output Example

### SLF4J Console Output
```
INFO [vollmed-web] - Doctor registration initiated
INFO [vollmed-web] - Doctor registered successfully
```

### OpenTelemetry Telemetry Data
Span name: "Doctor registration initiated"
Attributes:
- log.level: INFO
- application: vollmed-web
- timestamp: 1691234567890
- operation: register-doctor
- email: doctor@example.com
- crm: 123456
- entity: medico
