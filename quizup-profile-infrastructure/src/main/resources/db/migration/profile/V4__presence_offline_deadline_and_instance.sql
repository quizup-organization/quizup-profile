-- Présence : l'échéance de passage hors ligne remplace la deadline Axon (TTL en base),
-- et les sessions sont rattachées à l'instance BFF propriétaire (purge ciblée au redémarrage).
ALTER TABLE presence_entry
    ADD COLUMN IF NOT EXISTS offline_deadline_at TIMESTAMP;

ALTER TABLE presence_session
    ADD COLUMN IF NOT EXISTS instance_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_presence_entry_offline_deadline
    ON presence_entry(offline_deadline_at);

CREATE INDEX IF NOT EXISTS idx_presence_session_instance
    ON presence_session(instance_id);
