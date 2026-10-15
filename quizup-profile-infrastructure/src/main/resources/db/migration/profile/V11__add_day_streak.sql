-- V11: Serie de jours joues (assiduite) dans la progression.
-- Meme regle pure que la projection d'activite (ActivityRules) : un jour actif prolonge la
-- serie si c'est la veille, la relance a 1 sinon, et rejouer le meme jour ne change rien.
ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS current_day_streak INTEGER NOT NULL DEFAULT 0;

ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS best_day_streak INTEGER NOT NULL DEFAULT 0;

ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS last_active_date DATE;

COMMENT ON COLUMN progression_entry.current_day_streak IS 'Serie courante de jours joues';
COMMENT ON COLUMN progression_entry.best_day_streak IS 'Meilleure serie de jours joues (badge Assiduite)';
COMMENT ON COLUMN progression_entry.last_active_date IS 'Dernier jour joue (fuseau activite), NULL si aucun';
