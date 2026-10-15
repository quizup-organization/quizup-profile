-- V10: Compteurs cumules des familles Precision et Rapidite.
-- Faits d'exploit du duel (score parfait, reponses rapides) : comptes meme en entrainement,
-- comme le sont les badges PERFECT / ECLAIR.
ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS perfect_duels INTEGER NOT NULL DEFAULT 0;

ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS fast_answers_total INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN progression_entry.perfect_duels IS 'Duels parfaits cumules (badge Precision)';
COMMENT ON COLUMN progression_entry.fast_answers_total IS 'Reponses rapides cumulees (badge Rapidite)';
