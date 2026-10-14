-- Parties humaines jouees par joueur et par theme (duels bot exclus, comme les stats V/N/D) :
-- alimente la repartition du donut « Sujets joues » du profil.
CREATE TABLE IF NOT EXISTS progression_topic_games (
    user_id VARCHAR(255) NOT NULL,
    topic_id VARCHAR(255) NOT NULL,
    games INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, topic_id),
    CONSTRAINT fk_progression_topic_games_user
        FOREIGN KEY (user_id) REFERENCES progression_entry(user_id) ON DELETE CASCADE
);

COMMENT ON TABLE progression_topic_games IS 'Parties humaines jouees par joueur et par theme';
