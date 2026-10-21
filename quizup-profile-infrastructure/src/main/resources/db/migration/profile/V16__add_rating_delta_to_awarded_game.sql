ALTER TABLE progression_awarded_game
    ADD COLUMN rating_delta integer NOT NULL DEFAULT 0,
    ADD COLUMN rated boolean NOT NULL DEFAULT false;
