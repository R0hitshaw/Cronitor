# Cronitor — Cron Job Monitor & Alerting Platform

A Spring Boot service for monitoring scheduled jobs. Register any cron job, receive heartbeat pings (start/finish/fail), and get alerted when runs are missed, slow, or failing.

---

## Quick start

### 1. Start infrastructure
```bash
docker-compose up -d
```
This brings up PostgreSQL on `:5432` and Redis on `:6379`.

### 2. Run the app
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
App starts on `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Core concepts

### Register a job
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nightly billing job",
    "slug": "nightly-billing-job",
    "cronExpression": "0 0 2 * * *",
    "gracePeriodSeconds": 300
  }'
```
The `slug` is the URL-safe ID your cron job uses when pinging.
The `cronExpression` follows the 6-field Spring format: `seconds minutes hours day month weekday`.

### Instrument your cron job
Add three curl calls (or use the Java/Python client):

```bash
# At job start
curl -X POST http://localhost:8080/api/ping/nightly-billing-job/start \
  -H "Content-Type: application/json" \
  -d '{"runId": "'"$(date +%Y%m%d)"'", "message": "Starting billing run"}'

# ... your job runs ...

# On success
curl -X POST http://localhost:8080/api/ping/nightly-billing-job/finish \
  -H "Content-Type: application/json" \
  -d '{"runId": "'"$(date +%Y%m%d)"'", "message": "Processed 1234 invoices"}'

# On failure (in your error handler / catch block)
curl -X POST http://localhost:8080/api/ping/nightly-billing-job/fail \
  -H "Content-Type: application/json" \
  -d '{"runId": "'"$(date +%Y%m%d)"'", "message": "DB connection timeout"}'
```

### How missed-run detection works
After every `/finish` ping, Cronitor computes `next_expected_at` from your cron expression.
A background scheduler checks every 60 seconds for jobs where:
```
next_expected_at + grace_period_seconds < NOW()
```
Any such job is marked `MISSED` and its alert rules are triggered.

---

## API reference

| Endpoint | Description |
|---|---|
| `POST /api/jobs` | Register a job |
| `GET  /api/jobs` | List all jobs |
| `GET  /api/jobs/{id}` | Job detail |
| `PUT  /api/jobs/{id}` | Update job config |
| `DELETE /api/jobs/{id}` | Delete job |
| `POST /api/ping/{slug}/start` | Job started |
| `POST /api/ping/{slug}/finish` | Job succeeded |
| `POST /api/ping/{slug}/fail` | Job failed |

Full interactive docs: `/swagger-ui.html`

---

## Tech stack

- Java 17, Spring Boot 3.2
- PostgreSQL 16 (via Spring Data JPA + Flyway)
- Redis (alert deduplication)
- Spring `@Scheduled` for the monitor engine
- AWS SNS / SES for alerts (Week 2)
- React + Recharts dashboard (Week 3)
- Docker + AWS EC2/RDS deployment (Week 4)
