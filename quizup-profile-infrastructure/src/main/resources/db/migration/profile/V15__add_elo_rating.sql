ALTER TABLE progression_entry
    ADD COLUMN rating integer NOT NULL DEFAULT 200,
    ADD COLUMN peak_rating integer NOT NULL DEFAULT 200,
    ADD COLUMN rated_games integer NOT NULL DEFAULT 0,
    ADD COLUMN provisional boolean NOT NULL DEFAULT true;
