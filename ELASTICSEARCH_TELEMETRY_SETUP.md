# OpenTelemetry + Elasticsearch Integration Guide

## Problem Solved

Previously, telemetry events from login, consulta, and page access were **not being exported to Elasticsearch** because:

1. ❌ **OpenTelemetry SDK wasn't properly initialized** - dependencies present but no working exporter
2. ❌ **Missing OTLP Collector** - exporter expected a collector on port 4317 that didn't exist
3. ❌ **No auto-instrumentation** - Spring requests, SQL queries, etc. weren't being captured
4. ❌ **Spans created but not exported** - logs to console only, no external storage

## Solution Implemented

### 1. **OpenTelemetry SDK Initialization**
- Created `OpenTelemetryConfiguration` class to properly initialize OTLP exporter
- Configured `SdkTracerProvider` with batch processor
- Set up service resource attributes (service name, version)

### 2. **Docker Stack**
- **Elasticsearch** - Stores telemetry data
- **Kibana** - Visualize logs, traces, and metrics
- **OpenTelemetry Collector** - Receives OTLP data and exports to Elasticsearch

### 3. **Auto-Instrumentation**
- Added `opentelemetry-spring-boot-starter` for automatic request tracing
- Enabled instrumentation for:
  - Spring WebMVC (HTTP requests)
  - JDBC (database queries)
  - Hibernate (ORM operations)

## Setup Instructions

### Step 1: Start the Observability Stack

```bash
# Navigate to project root
cd c:\Users\User\Documents\Projetos Java\3865-seguranca-java

# Start Elasticsearch, Kibana, and OTLP Collector
docker-compose up -d
```

Wait for services to be ready (~30 seconds):
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`
- OTLP Collector gRPC: `localhost:4317`

### Step 2: Run Your Application

Build and run the Spring Boot application normally. It will automatically:
1. Initialize OpenTelemetry SDK with OTLP exporter
2. Send traces/metrics/logs to the collector on port 4317
3. Collector routes data to Elasticsearch

### Step 3: View in Kibana

#### View Traces
1. Navigate to **Kibana** → http://localhost:5601
2. Go to **Observability** → **Traces**
3. You'll see traces for:
   - `POST /login` - Login events with authentication details
   - `GET /medicos` - Doctor listing queries
   - `POST /consulta` - Appointment bookings
   - All HTTP requests with database queries

#### View Logs
1. Go to **Observability** → **Logs**
2. Filter by `service.name: "vollmed-web"`
3. See structured logs with attributes:
   ```
   {
     "service.name": "vollmed-web",
     "operation": "register-doctor",
     "email": "doctor@example.com",
     "crm": "123456"
   }
   ```

#### View Metrics
1. Go to **Observability** → **Metrics**
2. See Prometheus metrics exported by Actuator

## What Gets Telemetry

### Automatic (via Spring Boot Starter)
- ✅ HTTP requests (`GET /medicos`, `POST /login`, etc.)
- ✅ Database queries via JDBC/Hibernate
- ✅ Request/response timing
- ✅ Status codes and errors

### Manual (via StructuredLogger)
- ✅ Login attempts (`operation: "login"`)
- ✅ Doctor registration (`operation: "register-doctor"`)
- ✅ Appointment booking (`operation: "book-appointment"`)
- ✅ Business rule violations
- ✅ Custom attributes (email, CRM, patient, etc.)

## Example Trace Flow

When user logs in:
```
POST /login
├── Authentication (Spring Security span)
├── DaoAuthenticationProvider check
├── Database query for user
└── Session creation

All with:
- Timing information
- Status (success/failure)
- Error details if failed
```

## Testing

### 1. Trigger Login Event
```
Visit http://localhost:8080/login
Login with: joao@email.com / password
```

### 2. Trigger Doctor Listing Event
```
After login, visit http://localhost:8080/medicos
```

### 3. Check Elasticsearch
```bash
# List indices
curl http://localhost:9200/_cat/indices

# Query recent logs
curl "http://localhost:9200/logs-*/_search?pretty" | jq .

# Query traces
curl "http://localhost:9200/traces-*/_search?pretty" | jq .
```

### 4. View in Kibana
- Open http://localhost:5601
- Create data views for `logs-*`, `traces-*`, `metrics-*`
- Explore traces and logs

## Configuration

Key environment variables in `application.properties`:
```properties
otel.exporter.otlp.endpoint=http://localhost:4317
otel.resource.attributes=service.name=vollmed-web
otel.traces.sampler=always_on
```

To change sampling (send only 10% of traces):
```properties
otel.traces.sampler=traceidratio
otel.traces.sampler.arg=0.1
```

## Troubleshooting

**Issue**: No data appearing in Kibana
- Check if Elasticsearch is running: `curl http://localhost:9200`
- Check if Collector is running: `docker logs $(docker ps -q --filter name=otel-collector)`
- Check application logs for connection errors to `localhost:4317`

**Issue**: High disk usage
- Elasticsearch can use significant space. Set retention:
```bash
curl -X PUT "localhost:9200/logs-*/_settings?pretty" -H 'Content-Type: application/json' -d'
{
  "index.lifecycle.name": "logs",
  "index.lifecycle.rollover_alias": "logs"
}'
```

**Issue**: Slow queries
- Check JDBC instrumentation not overwhelming system
- Disable if needed: `otel.instrumentation.jdbc.enabled=false`

## Stopping the Stack

```bash
docker-compose down -v  # -v removes volumes
```

## References

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Spring Boot Starter for OTel](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/spring/spring-boot-autoconfigure)
- [Kibana Observability](https://www.elastic.co/guide/en/kibana/current/xpack-observability.html)
