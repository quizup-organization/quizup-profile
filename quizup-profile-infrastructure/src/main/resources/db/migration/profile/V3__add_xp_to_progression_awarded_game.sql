-- XP réellement attribuée par partie (lecture « XP par partie »).
-- Nullable : les lignes antérieures à cette migration n'ont pas d'XP connue côté read model.
ALTER TABLE progression_awarded_game
    ADD COLUMN IF NOT EXISTS xp INTEGER;
