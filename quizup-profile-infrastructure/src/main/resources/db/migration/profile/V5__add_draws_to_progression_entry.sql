-- Égalités (duels humains terminés sans vainqueur), stats V/N/D.
ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS draws INTEGER NOT NULL DEFAULT 0;
