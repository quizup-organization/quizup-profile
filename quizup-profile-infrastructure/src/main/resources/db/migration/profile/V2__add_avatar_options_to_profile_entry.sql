-- V2: Ajout des options d'avatar (DiceBear style micah, serialisees en JSON) au profil.

ALTER TABLE profile_entry
    ADD COLUMN IF NOT EXISTS avatar_options VARCHAR(2000);

COMMENT ON COLUMN profile_entry.avatar_options IS 'Options DiceBear (style micah) serialisees en JSON, choisies par le proprietaire ; NULL = avatar derive du userId';
