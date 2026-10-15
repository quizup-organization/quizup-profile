-- V12: Compteurs de badges hors duel (alimentes par les autres services).
-- Social : abonnes gagnes (cumules, un desabonnement ne retire rien).
-- Defis : defis nominatifs lances (quizup-matchmaking est proprietaire des defis).
ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS followers_gained INTEGER NOT NULL DEFAULT 0;

ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS challenges_sent INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN progression_entry.followers_gained IS 'Abonnes gagnes cumules (badge Social)';
COMMENT ON COLUMN progression_entry.challenges_sent IS 'Defis nominatifs lances (badge Defis)';
