CREATE TABLE monitored_jobs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(255)        NOT NULL,
    slug                  VARCHAR(255)        NOT NULL UNIQUE,
    cron_expression       VARCHAR(100)        NOT NULL,
    grace_period_seconds  INT                 NOT NULL DEFAULT 300,
    status                VARCHAR(50)         NOT NULL DEFAULT 'PENDING',
    last_ping_at          TIMESTAMPTZ,
    next_expected_at      TIMESTAMPTZ,
    created_at            TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ         NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_slug        ON monitored_jobs (slug);
CREATE INDEX idx_jobs_status      ON monitored_jobs (status);
CREATE INDEX idx_jobs_next_expected ON monitored_jobs (next_expected_at);

COMMENT ON COLUMN monitored_jobs.slug IS 'URL-safe identifier used in ping endpoints, e.g. billing-job';
COMMENT ON COLUMN monitored_jobs.grace_period_seconds IS 'Extra seconds allowed past next_expected_at before marking as MISSED';
COMMENT ON COLUMN monitored_jobs.status IS 'PENDING | HEALTHY | RUNNING | MISSED | FAILED';
