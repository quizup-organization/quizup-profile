-- V1: Schema initial complet du module profile
--
-- Projections read-only mises a jour via les events du domaine Profile / Progression.
-- Tables : profil, progression (XP/niveau/badges), presence (ephemere), activite journaliere.

CREATE TABLE IF NOT EXISTS profile_entry (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    bio VARCHAR(300),
    country VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Index pour recherche par email
CREATE INDEX idx_profile_entry_email ON profile_entry(email);

-- Index pour recherche par nom de joueur
CREATE INDEX idx_profile_entry_display_name ON profile_entry(display_name);

-- Commentaires pour documentation
COMMENT ON TABLE profile_entry IS 'Table des profils utilisateur - projection read-only mise a jour via Event Handlers';
COMMENT ON COLUMN profile_entry.user_id IS 'Identifiant unique de l''utilisateur (UUID)';
COMMENT ON COLUMN profile_entry.email IS 'Email de l''utilisateur (denormalise depuis identity pour la recherche)';
COMMENT ON COLUMN profile_entry.display_name IS 'Nom public du joueur (modifiable par le proprietaire)';
COMMENT ON COLUMN profile_entry.bio IS 'Bio libre de l''utilisateur (max 300 caracteres)';
COMMENT ON COLUMN profile_entry.country IS 'Pays de l''utilisateur (max 100 caracteres)';
COMMENT ON COLUMN profile_entry.created_at IS 'Date de creation du profil';
COMMENT ON COLUMN profile_entry.updated_at IS 'Date de derniere mise a jour du profil';

CREATE TABLE IF NOT EXISTS progression_entry (
    user_id VARCHAR(255) PRIMARY KEY,
    xp_total INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(100) NOT NULL,
    games_played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    best_score INTEGER NOT NULL DEFAULT 0,
    current_win_streak INTEGER NOT NULL DEFAULT 0,
    best_win_streak INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS progression_topic_entry (
    user_id VARCHAR(255) NOT NULL,
    topic_id VARCHAR(255) NOT NULL,
    xp INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, topic_id),
    CONSTRAINT fk_progression_topic_user
        FOREIGN KEY (user_id) REFERENCES progression_entry(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS progression_badge (
    user_id VARCHAR(255) NOT NULL,
    badge VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, badge),
    CONSTRAINT fk_progression_badge_user
        FOREIGN KEY (user_id) REFERENCES progression_entry(user_id) ON DELETE CASCADE
);

COMMENT ON TABLE progression_entry IS 'Progression globale par joueur (XP totale, niveau, titre)';
COMMENT ON TABLE progression_topic_entry IS 'XP cumulee par joueur et par theme';
COMMENT ON TABLE progression_badge IS 'Badges debloques par joueur';

CREATE TABLE IF NOT EXISTS presence_entry (
    user_id      VARCHAR(255) PRIMARY KEY,
    status       VARCHAR(10)  NOT NULL,   -- ONLINE, OFFLINE
    last_seen_at TIMESTAMP
);

CREATE INDEX idx_presence_entry_status ON presence_entry(status);
CREATE INDEX idx_presence_entry_last_seen ON presence_entry(last_seen_at);

CREATE TABLE IF NOT EXISTS presence_session (
    session_id   VARCHAR(255) PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    connected_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_presence_session_user ON presence_session(user_id);

CREATE TABLE IF NOT EXISTS progression_activity (
    user_id          VARCHAR(255) PRIMARY KEY,
    current_streak   INTEGER NOT NULL DEFAULT 0,
    longest_streak   INTEGER NOT NULL DEFAULT 0,
    last_active_date DATE
);

CREATE TABLE IF NOT EXISTS progression_activity_day (
    user_id       VARCHAR(255) NOT NULL,
    activity_date DATE         NOT NULL,
    games_count   INTEGER      NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, activity_date)
);

-- Journal des attributions d'XP par partie (idempotence de la progression au rejeu).
CREATE TABLE IF NOT EXISTS progression_awarded_game (
    user_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, game_id),
    CONSTRAINT fk_progression_awarded_game_user
        FOREIGN KEY (user_id) REFERENCES progression_entry(user_id) ON DELETE CASCADE
);

-- Journal des parties comptées par jour (idempotence du compteur d'activité au rejeu).
CREATE TABLE IF NOT EXISTS progression_activity_day_game (
    user_id       VARCHAR(255) NOT NULL,
    activity_date DATE         NOT NULL,
    game_id       VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, activity_date, game_id)
);

CREATE INDEX IF NOT EXISTS idx_progression_activity_day_game_user_date
    ON progression_activity_day_game(user_id, activity_date);
