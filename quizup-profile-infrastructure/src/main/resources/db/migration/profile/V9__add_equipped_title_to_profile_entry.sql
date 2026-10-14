-- V9: Titre thématique porté par le joueur (affiché sur le profil, les cartes et les listes).
-- Clé stable `topicId:GRADE` (cf. PlayerTitle), effaçable (NULL = aucun titre affiché).
ALTER TABLE profile_entry
    ADD COLUMN IF NOT EXISTS equipped_title VARCHAR(128);

COMMENT ON COLUMN profile_entry.equipped_title IS 'Clé du titre thématique porté (topicId:GRADE), NULL = aucun';
