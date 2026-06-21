CREATE TABLE job_executions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id        UUID            NOT NULL REFERENCES monitored_jobs(id) ON DELETE CASCADE,
    run_id        VARCHAR(255),                  -- caller-supplied idempotency key
    status        VARCHAR(50)     NOT NULL DEFAULT 'RUNNING',
    started_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    finished_at   TIMESTAMPTZ,
    duration_ms   BIGINT,
    exit_message  TEXT,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_executions_job_id    ON job_executions (job_id);
CREATE INDEX idx_executions_status    ON job_executions (status);
CREATE INDEX idx_executions_started   ON job_executions (started_at DESC);

COMMENT ON COLUMN job_executions.run_id IS 'Optional caller-supplied ID for idempotency (e.g. a UUID from your CI system)';
COMMENT ON COLUMN job_executions.status IS 'RUNNING | SUCCESS | FAILED';

-- ----------------------------------------------------------

CREATE TABLE execution_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id  UUID            NOT NULL REFERENCES job_executions(id) ON DELETE CASCADE,
    event_type    VARCHAR(50)     NOT NULL,      -- START | FINISH | FAIL | LOG
    message       TEXT,
    occurred_at   TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_execution_id ON execution_events (execution_id);

COMMENT ON COLUMN execution_events.event_type IS 'START | FINISH | FAIL | LOG';
