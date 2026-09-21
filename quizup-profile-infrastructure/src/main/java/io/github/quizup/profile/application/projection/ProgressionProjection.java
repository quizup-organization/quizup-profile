package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ProgressionProjection — maintient la projection read-only de la progression
 * (XP totale, XP par thème, niveau, titre, badges).
 */
@Component
public class ProgressionProjection {

    private static final Logger logger = LoggerFactory.getLogger(ProgressionProjection.class);

    private final ProgressionRepositoryPort progressionRepositoryPort;
    private final ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort;

    public ProgressionProjection(ProgressionRepositoryPort progressionRepositoryPort,
                                 ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort) {
        this.progressionRepositoryPort = progressionRepositoryPort;
        this.awardedGameRepositoryPort = awardedGameRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(ProgressionEvent.XpAwardedEvent event) {
        PlayerProgress existing = progressionRepositoryPort.findById(event.userId()).orElse(null);
        PlayerProgress current = existing != null ? existing : PlayerProgress.empty(event.userId());

        // La ligne `progression_entry` doit exister avant d'écrire le journal d'attribution.
        if (existing == null) {
            progressionRepositoryPort.save(current);
        }

        // Idempotence par clé métier (userId, gameId) : un rejeu ne recompte pas l'XP.
        if (!awardedGameRepositoryPort.record(event.userId(), event.gameId())) {
            return;
        }

        Map<String, Integer> xpByTopic = new HashMap<>(current.xpByTopic());
        xpByTopic.merge(event.topicId(), event.xp(), Integer::sum);

        int xpTotal = current.xpTotal() + event.xp();
        int level = ProgressionRules.levelFor(xpTotal);

        int currentWinStreak = event.won() ? current.currentWinStreak() + 1 : 0;
        int bestWinStreak = Math.max(current.bestWinStreak(), currentWinStreak);

        progressionRepositoryPort.save(current.toBuilder()
                .xpTotal(xpTotal)
                .level(level)
                .title(ProgressionRules.titleFor(level))
                .xpByTopic(xpByTopic)
                .gamesPlayed(current.gamesPlayed() + 1)
                .wins(current.wins() + (event.won() ? 1 : 0))
                .losses(current.losses() + (event.won() ? 0 : 1))
                .bestScore(Math.max(current.bestScore(), event.gameScore()))
                .currentWinStreak(currentWinStreak)
                .bestWinStreak(bestWinStreak)
                .updatedAt(event.awardedAt())
                .build());

        logger.info("Progression projetée: userId={}, +{} XP, total={}, niveau={}",
                event.userId(), event.xp(), xpTotal, level);
    }

    @EventHandler
    @Transactional
    public void on(ProgressionEvent.LevelReachedEvent event) {
        progressionRepositoryPort.findById(event.userId())
                .ifPresent(progress -> progressionRepositoryPort.save(progress.toBuilder()
                        .level(event.level())
                        .title(event.title())
                        .updatedAt(event.reachedAt())
                        .build()));
    }

    @EventHandler
    @Transactional
    public void on(ProgressionEvent.BadgeEarnedEvent event) {
        progressionRepositoryPort.findById(event.userId())
                .ifPresent(progress -> {
                    Set<Badge> badges = new HashSet<>(progress.badges());
                    badges.add(event.badge());
                    progressionRepositoryPort.save(progress.toBuilder()
                            .badges(badges)
                            .updatedAt(event.earnedAt())
                            .build());
                });
    }
}
