-- V7: Bannière de profil (visuel panoramique affiché derrière l'avatar sur les fiches joueur).
-- URL externe optionnelle (http/https), longueur max 1024, effaçable (NULL).
ALTER TABLE profile_entry
    ADD COLUMN IF NOT EXISTS banner_url VARCHAR(1024);

COMMENT ON COLUMN profile_entry.banner_url IS 'URL externe optionnelle de la bannière de profil (image panoramique)';
