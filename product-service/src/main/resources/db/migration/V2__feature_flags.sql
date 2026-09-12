-- V2: feature_flags table. Existing databases baselined V1 without replaying it
-- (they predate flags), so the new table ships here. IF NOT EXISTS keeps
-- fresh installs (which got it from V1) idempotent.
CREATE TABLE IF NOT EXISTS feature_flags (
    key VARCHAR(255) PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    description VARCHAR(255)
);
