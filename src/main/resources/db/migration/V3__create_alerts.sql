CREATE TABLE alert_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL REFERENCES monitored_jobs(id) ON DELETE CASCADE,
    condition       VARCHAR(100)    NOT NULL,    -- MISSED | FAILED | DURATION_ANOMALY | SUCCESS_RATE_LOW
    threshold_value INT,                         -- e.g. duration_ms threshold, or % success rate
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_alert_rules_job_id ON alert_rules (job_id);

-- ----------------------------------------------------------

CREATE TABLE alert_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id         UUID            REFERENCES alert_rules(id) ON DELETE SET NULL,
    job_id          UUID            NOT NULL REFERENCES monitored_jobs(id) ON DELETE CASCADE,
    execution_id    UUID            REFERENCES job_executions(id) ON DELETE SET NULL,
    severity        VARCHAR(50)     NOT NULL DEFAULT 'WARNING',   -- INFO | WARNING | CRITICAL
    message         TEXT            NOT NULL,
    resolved        BOOLEAN         NOT NULL DEFAULT false,
    fired_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ
);

CREATE INDEX idx_alert_history_job_id  ON alert_history (job_id);
CREATE INDEX idx_alert_history_fired   ON alert_history (fired_at DESC);
CREATE INDEX idx_alert_history_resolved ON alert_history (resolved);

-- ----------------------------------------------------------

CREATE TABLE notification_channels (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL REFERENCES monitored_jobs(id) ON DELETE CASCADE,
    channel_type    VARCHAR(50)     NOT NULL,    -- EMAIL | SLACK | SNS | WEBHOOK
    config_json     JSONB           NOT NULL,    -- {"email":"ops@acme.com"} or {"webhook_url":"..."}
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_channels_job_id ON notification_channels (job_id);
