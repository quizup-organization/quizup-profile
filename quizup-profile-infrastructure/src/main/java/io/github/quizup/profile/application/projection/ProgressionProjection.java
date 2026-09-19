package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
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

    public ProgressionProjection(ProgressionRepositoryPort progressionRepositoryPort) {
        this.progressionRepositoryPort = progressionRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(ProgressionEvent.XpAwardedEvent event) {
        PlayerProgress current = progressionRepositoryPort.findById(event.userId())
                .orElseGet(() -> PlayerProgress.empty(event.userId()));

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
