<div align="center">

# ⏱️ Cronitor

### Self-Hosted Cron Job Monitoring & Alerting Platform

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![React](https://img.shields.io/badge/React-Vite-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

**Cronitor** is a self-hosted cron job monitoring platform. Register your scheduled jobs, instrument them with simple HTTP pings, and get alerted instantly via Email or Slack when a job is missed, fails, or runs longer than expected.

</div>

---

## ✨ Features

### 🔍 Job Monitoring
- **Job Registry** — Register cron jobs with name, slug, cron expression, and configurable grace period
- **Heartbeat Pings** — Instrument any job with simple `curl` or HTTP calls to `/api/ping/{slug}/start`, `/finish`, and `/fail`
- **Status Tracking** — Real-time job status across five states: `PENDING`, `HEALTHY`, `RUNNING`, `MISSED`, `FAILED`
- **Grace Period Support** — Configurable per-job tolerance window before a late run is marked as missed (default: 5 minutes)

### 🚨 Smart Alerting
- **Missed Run Detection** — `MonitorEngine` polls every 60 seconds and fires alerts when `next_expected_at + grace_period < NOW()`
- **Failure Alerts** — Triggered immediately when a job pings `/fail`
- **Duration Anomaly Detection** — Detects jobs running more than 2× their 10-run rolling average duration
- **Alert Deduplication** — Redis-backed dedup prevents repeat alerts within a configurable TTL window (default: 1 hour)
- **Alert History** — All fired alerts are persisted with severity (`CRITICAL` / `WARNING`) and can be manually resolved

### 📣 Notification Channels
Each job can have multiple notification channels configured independently:
- **Email** — via SMTP (Gmail / any SMTP provider)
- **Slack** — via Incoming Webhooks
- **SNS / Webhook** — extensible channel interface ready to plug in AWS SNS or custom webhooks

### 📊 Stats & History
- **Execution History** — Paginated log of every run with start time, duration, status, and exit message
- **Job Stats** — Success rate, P95 duration, total run counts per job
- **Nightly Cleanup** — Auto-purges resolved alerts older than the retention window (default: 90 days)

### 🔐 Security
- **JWT Authentication** — Stateless token-based auth for all API endpoints
- **API Keys** — Per-user API keys for programmatic access from cron job scripts
- **Spring Security** — Full security filter chain with role-based access

---

## 🏗️ Architecture

```
Cronitor
├── MonitorEngine          (scheduled, runs every 60s)
│   ├── checkMissedRuns()  → queries overdue jobs from DB
│   └── checkDurationAnomalies() → compares elapsed vs rolling avg
│
├── AlertDispatcher        (central alert router)
│   ├── Redis dedup check  → suppresses repeat alerts within TTL
│   ├── Fetches notification channels from DB per job
│   ├── Routes to AlertChannel strategy (Email / Slack / SNS)
│   └── Persists to alert_history
│
└── HeartbeatService       (ping handler)
    ├── /start → creates JobExecution, sets status = RUNNING
    ├── /finish → closes execution, computes next_expected_at
    └── /fail → closes execution as FAILED, triggers alert
```

---

## 🗂️ Project Structure

```
Cronitor/
├── docker-compose.yml               # Postgres, Redis, Mailpit, frontend, backend
├── frontend/                        # React + Vite frontend
├── src/main/java/com/cronitor/
│   ├── controller/
│   │   ├── AuthController.java      # Login / register
│   │   ├── JobController.java       # CRUD + notification channels
│   │   ├── PingController.java      # /start, /finish, /fail heartbeats
│   │   ├── StatsController.java     # Stats + paginated execution history
│   │   └── AlertController.java     # Alert history + manual resolve
│   ├── service/
│   │   ├── MonitorEngine.java       # Scheduled polling — missed runs + anomalies
│   │   ├── HeartbeatService.java    # Ping logic
│   │   ├── JobRegistrationService.java
│   │   ├── StatsService.java
│   │   ├── AlertService.java
│   │   └── AuthService.java
│   ├── alert/
│   │   ├── AlertDispatcher.java     # Redis dedup + channel routing
│   │   ├── AlertEvent.java          # Immutable event carrier (MISSED/FAILED/ANOMALY)
│   │   ├── AlertChannel.java        # Strategy interface
│   │   ├── EmailAlertChannel.java
│   │   └── SlackAlertChannel.java
│   ├── domain/                      # JPA entities
│   │   ├── MonitoredJob.java
│   │   ├── JobExecution.java
│   │   ├── ExecutionEvent.java
│   │   ├── AlertHistory.java
│   │   ├── NotificationChannel.java # JSONB config per channel type
│   │   ├── ApiKey.java
│   │   └── User.java
│   ├── repository/                  # Spring Data JPA repositories
│   ├── security/                    # JWT filter, user details, config
│   ├── config/                      # App config, CORS, scheduling
│   ├── dto/                         # Request/Response DTOs
│   ├── exception/                   # Global error handling
│   └── util/
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    └── db/migration/
        ├── V1__create_monitored_jobs.sql
        ├── V2__create_executions.sql
        ├── V3__create_alerts.sql
        └── V4__create_users_and_api_keys.sql
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.x |
| Database | PostgreSQL 16, Flyway (schema migrations) |
| Caching / Dedup | Redis 7 |
| Security | Spring Security, JWT (JJWT) |
| Persistence | Spring Data JPA / Hibernate |
| Email | Spring Mail (SMTP) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Frontend | React, Vite, TypeScript |
| Dev Mail | Mailpit (local SMTP catcher) |
| Containerization | Docker, Docker Compose |
| Build | Maven |

---

## 🚀 Getting Started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) & Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/R0hitshaw/Cronitor.git
cd Cronitor
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```env
POSTGRES_DB=cronitor
POSTGRES_USER=cronitor_user
POSTGRES_PASSWORD=yourpassword
JWT_SECRET=your_super_secret_jwt_key_here

# Optional: real SMTP credentials (Mailpit is used by default in dev)
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=
```

### 3. Start all services

```bash
docker-compose up --build
```

This starts:
- **PostgreSQL** on port `5432`
- **Redis** on port `6379`
- **Mailpit** (dev email catcher) on ports `1025` (SMTP) and `8025` (web UI)
- **Spring Boot backend** on port `8080`
- **React frontend** on port `5173`

### 4. Access the application

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Mailpit (dev emails) | http://localhost:8025 |

---

## 📡 API Reference

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT |

### Jobs
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/jobs` | Register a new monitored job |
| `GET` | `/api/jobs` | List all jobs with current status |
| `GET` | `/api/jobs/{id}` | Get job by ID |
| `PUT` | `/api/jobs/{id}` | Update job config (cron expression, grace period) |
| `DELETE` | `/api/jobs/{id}` | Delete job and all its history |
| `POST` | `/api/jobs/{id}/channels` | Add notification channel (EMAIL, SLACK, SNS) |
| `GET` | `/api/jobs/{id}/channels` | List notification channels for a job |

### Pings (heartbeats from your cron jobs)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/ping/{slug}/start` | Record job start |
| `POST` | `/api/ping/{slug}/finish` | Record job success — resets missed-run clock |
| `POST` | `/api/ping/{slug}/fail` | Record job failure — triggers FAILED alert |

### Stats & History
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/jobs/{id}/stats` | Success rate, P95 duration, run counts |
| `GET` | `/api/jobs/{id}/executions` | Paginated execution history |

### Alerts
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/alerts/history` | Paginated alert history (all jobs) |
| `POST` | `/api/alerts/{id}/resolve` | Manually resolve a firing alert |

---

## 🔧 Instrumenting Your Cron Jobs

Add three simple `curl` calls to any cron job:

```bash
#!/bin/bash
BASE_URL="http://your-cronitor-host:8080"
SLUG="nightly-billing-job"

# 1. Signal start
curl -s -X POST "$BASE_URL/api/ping/$SLUG/start" \
     -H "Content-Type: application/json" \
     -d '{"message": "Billing job started"}'

# Your job logic here
python run_billing.py
EXIT_CODE=$?

# 2. Signal finish or fail
if [ $EXIT_CODE -eq 0 ]; then
  curl -s -X POST "$BASE_URL/api/ping/$SLUG/finish" \
       -H "Content-Type: application/json" \
       -d '{"message": "Completed successfully"}'
else
  curl -s -X POST "$BASE_URL/api/ping/$SLUG/fail" \
       -H "Content-Type: application/json" \
       -d '{"message": "Exited with code '$EXIT_CODE'"}'
fi
```

---

## ⚙️ Configuration Reference

Key settings in `application.yml`:

```yaml
cronitor:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000         # 24 hours (ms)
  alert:
    dedup-ttl-seconds: 3600      # Suppress duplicate alerts for 1 hour
    retention-days: 90           # Auto-purge resolved alerts after 90 days
  monitor:
    check-interval-ms: 60000     # Poll for missed runs every 60 seconds
```

---

## 📊 Job Status Reference

| Status | Description |
|---|---|
| `PENDING` | Registered but no ping received yet |
| `HEALTHY` | Last run completed on time |
| `RUNNING` | Job has pinged `/start` but not yet `/finish` |
| `MISSED` | Passed `next_expected_at + grace_period` with no `/finish` ping |
| `FAILED` | Job explicitly pinged `/fail` |

---

## 🚨 Alert Types & Severity

| Alert Type | Severity | Trigger |
|---|---|---|
| `MISSED` | CRITICAL | Job did not ping `/finish` before deadline |
| `FAILED` | CRITICAL | Job pinged `/fail` |
| `DURATION_ANOMALY` | WARNING | Elapsed time > 2× rolling average (last 10 runs) |

---

## 🧪 Running Tests

```bash
./mvnw test
```

Tests use H2 in-memory database — no external services needed.

---

## 📦 Building for Production

```bash
# Backend JAR
./mvnw clean package -DskipTests

# All services via Docker
docker-compose up --build -d
```

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 👨‍💻 Author

**Rohit Kumar Shaw**  
Backend Developer · Java & Spring Boot · AWS · PostgreSQL · Redis  
[GitHub](https://github.com/R0hitshaw)

---

## 📄 License

This project is open source. See the repository for license details.