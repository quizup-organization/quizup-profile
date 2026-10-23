-- V17: flag serveur de fin (ou skip) de l'onboarding du joueur.
ALTER TABLE profile_entry
    ADD COLUMN IF NOT EXISTS onboarding_completed_at TIMESTAMP;
