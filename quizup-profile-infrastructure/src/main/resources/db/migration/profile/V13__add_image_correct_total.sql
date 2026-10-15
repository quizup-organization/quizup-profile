-- V13: Bonnes reponses cumulees sur les questions illustrees (badge Vision / « Oeil de lynx »).
-- Alimente par quizup-game (GameEndedEvent : player1/player2ImageCorrectAnswers, release 8.5.0).
ALTER TABLE progression_entry
    ADD COLUMN IF NOT EXISTS image_correct_total INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN progression_entry.image_correct_total IS 'Bonnes reponses sur questions illustrees (badge Vision)';
