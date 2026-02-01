# PetBuddyCentral Monitoring Stack

## Quick Start

1. **Start the monitoring stack:**

   ```bash
   cd monitoring
   docker-compose -f docker-compose.monitoring.yml up -d
   ```

2. **Verify Prometheus is scraping:**
   - Open: http://localhost:9092
   - Go to Status → Targets
   - All 3 services should show "UP" (green)

3. **Access Grafana:**
   - Open: http://localhost:3000
   - Login: `admin` / `admin`

4. **Add Prometheus Data Source:**
   - Click: Configuration (⚙️) → Data Sources → Add data source
   - Select: Prometheus
   - URL: `http://prometheus:9090`
   - Click: Save & Test

5. **Import JVM Dashboard:**
   - Click: Dashboards (☰) → Import
   - Dashboard ID: `4701` (JVM Micrometer)
   - Or try: `11378` (Spring Boot 2.1+ System Monitor)
   - Select Prometheus as data source
   - Click: Import

## Services Monitored

| Service            | Port | Metrics Endpoint                          |
| ------------------ | ---- | ----------------------------------------- |
| api-gateway        | 8080 | http://localhost:8080/actuator/prometheus |
| profile-rescue-svc | 9090 | http://localhost:9090/actuator/prometheus |
| social-stream-svc  | 9091 | http://localhost:9091/actuator/prometheus |

## Key Metrics to Watch

**JVM Metrics:**

- `jvm_memory_used_bytes` - Heap usage
- `jvm_gc_pause_seconds` - GC pause times

**Application Metrics:**

- `http_server_requests_seconds` - API response times
- `grpc_server_handled_total` - gRPC request counts

**RabbitMQ Metrics:**

- `rabbitmq_published_total` - Messages published
- `spring_rabbitmq_listener_*` - Queue consumer stats

**Database Metrics:**

- `hikaricp_connections_active` - Active DB connections
- `spring_data_repository_invocations_seconds` - Query times

## Stopping the Stack

```bash
docker-compose -f docker-compose.monitoring.yml down
```

## Memory Usage

- Prometheus: ~300MB
- Grafana: ~300MB
- **Total**: ~600MB (leaves 7.4GB for Java services on 8GB RAM)
