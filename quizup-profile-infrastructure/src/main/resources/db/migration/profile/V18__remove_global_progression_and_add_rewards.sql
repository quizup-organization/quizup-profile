ALTER TABLE progression_entry
    DROP COLUMN IF EXISTS xp_total,
    DROP COLUMN IF EXISTS level,
    DROP COLUMN IF EXISTS title;

CREATE TABLE player_reward (
    user_id VARCHAR(255) NOT NULL,
    reward_code VARCHAR(64) NOT NULL,
    source VARCHAR(32) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, reward_code)
);
