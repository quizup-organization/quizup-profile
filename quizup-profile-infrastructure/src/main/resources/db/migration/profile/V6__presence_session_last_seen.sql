-- Présence : TTL de secours des sessions (filet si le BFF disparaît sans déconnexion propre).
-- Le BFF renouvelle `last_seen_at` par lot (heartbeat batch) ; le balayeur supprime les sessions
-- silencieuses depuis plus de SESSION_LEASE_TTL et confirme le passage hors ligne.
ALTER TABLE presence_session
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP;

UPDATE presence_session
SET last_seen_at = connected_at
WHERE last_seen_at IS NULL;

ALTER TABLE presence_session
    ALTER COLUMN last_seen_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_presence_session_last_seen
    ON presence_session(last_seen_at);
