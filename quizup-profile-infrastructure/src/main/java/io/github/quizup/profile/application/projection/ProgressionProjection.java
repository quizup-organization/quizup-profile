package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.ActivityRules;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.PlayerActivity;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.infrastructure.properties.AppProperties;
import io.github.quizup.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ProgressionProjection — maintient la projection read-only de la progression
 * (XP totale, XP par thème, niveau, titre, badges).
 */
@Component
@ProcessingGroup("progression-projection")
public class ProgressionProjection {

    private static final Logger logger = LoggerFactory.getLogger(ProgressionProjection.class);

    private final ProgressionRepositoryPort progressionRepositoryPort;
    private final ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort;
    private final ZoneId activityZone;

    public ProgressionProjection(ProgressionRepositoryPort progressionRepositoryPort,
                                 ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort,
                                 AppProperties properties) {
        this.progressionRepositoryPort = progressionRepositoryPort;
        this.awardedGameRepositoryPort = awardedGameRepositoryPort;
        this.activityZone = ZoneId.of(properties.activity().zone());
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
        if (!awardedGameRepositoryPort.record(event.userId(), event.gameId(), event.xp(),
                event.ratingDelta(), event.ratedOpponentId() != null)) {
            return;
        }

        Map<String, Integer> xpByTopic = new HashMap<>(current.xpByTopic());
        xpByTopic.merge(event.topicId(), event.xp(), Integer::sum);

        int xpTotal = current.xpTotal() + event.xp();
        int level = ProgressionRules.levelFor(xpTotal);

        boolean humanDuel = !event.botGame();
        int currentWinStreak = humanDuel
                ? (event.won() ? current.currentWinStreak() + 1 : 0)
                : current.currentWinStreak();
        int bestWinStreak = Math.max(current.bestWinStreak(), currentWinStreak);

        // Assiduité : même règle pure que la projection d'activité (idempotente par jour), mais
        // **uniquement sur les parties contre des humains** — la progression affichée doit refléter
        // ce qui peut réellement faire progresser un badge (un entraînement n'en acquiert aucun).
        int currentDayStreak = current.currentDayStreak();
        int bestDayStreak = current.bestDayStreak();
        LocalDate lastActiveDate = current.lastActiveDate();
        if (humanDuel) {
            PlayerActivity activity = ActivityRules.advance(
                    PlayerActivity.builder()
                            .userId(current.userId())
                            .currentStreak(current.currentDayStreak())
                            .longestStreak(current.bestDayStreak())
                            .lastActiveDate(current.lastActiveDate())
                            .build(),
                    event.awardedAt().atZone(activityZone).toLocalDate());
            currentDayStreak = activity.currentStreak();
            bestDayStreak = activity.longestStreak();
            lastActiveDate = activity.lastActiveDate();
        }

        // Faits d'exploit cumulés : badges Précision / Rapidité / Vision — duels entre joueurs
        // uniquement (un entraînement ne fait progresser aucun badge).
        int perfectDuels = humanDuel
                ? current.perfectDuels() + (event.gameScore() >= ProgressionRules.PERFECT_SCORE ? 1 : 0)
                : current.perfectDuels();
        int fastAnswersTotal = humanDuel
                ? current.fastAnswersTotal() + event.fastAnswers()
                : current.fastAnswersTotal();
        int imageCorrectTotal = humanDuel
                ? current.imageCorrectTotal() + event.imageCorrectAnswers()
                : current.imageCorrectTotal();

        // Parties humaines par thème (les duels bot restent exclus, comme les stats V/N/D).
        Map<String, Integer> gamesByTopic = new HashMap<>(current.gamesByTopic());
        if (humanDuel) {
            gamesByTopic.merge(event.topicId(), 1, Integer::sum);
        }

        progressionRepositoryPort.save(current.toBuilder()
                .xpTotal(xpTotal)
                .level(level)
                .title(ProgressionRules.titleFor(level))
                .xpByTopic(xpByTopic)
                .gamesByTopic(gamesByTopic)
                .gamesPlayed(current.gamesPlayed() + (humanDuel ? 1 : 0))
                .wins(current.wins() + (humanDuel && event.won() ? 1 : 0))
                .losses(current.losses() + (humanDuel && !event.won() && !event.draw() ? 1 : 0))
                .draws(current.draws() + (humanDuel && event.draw() ? 1 : 0))
                .bestScore(humanDuel ? Math.max(current.bestScore(), event.gameScore()) : current.bestScore())
                .currentWinStreak(currentWinStreak)
                .bestWinStreak(bestWinStreak)
                .perfectDuels(perfectDuels)
                .fastAnswersTotal(fastAnswersTotal)
                .imageCorrectTotal(imageCorrectTotal)
                .currentDayStreak(currentDayStreak)
                .bestDayStreak(bestDayStreak)
                .lastActiveDate(lastActiveDate)
                .updatedAt(event.awardedAt())
                .rating(event.rating())
                .peakRating(event.peakRating())
                .ratedGames(event.ratedGames())
                .provisional(event.provisional())
                .build());

        logger.info("Progression projetée: userId={}, +{} XP, total={}, Elo={} (+{})",
                event.userId(), event.xp(), xpTotal, event.rating(), event.ratingDelta());
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

    /**
     * Compteurs **hors duel** (abonnés gagnés, défis lancés) : les badges sont déjà évalués par
     * l'agrégat, on ne fait ici qu'exposer la progression au BFF.
     */
    @EventHandler
    @Transactional
    public void on(ProgressionEvent.BadgeCounterRecordedEvent event) {
        PlayerProgress current = progressionRepositoryPort.findById(event.userId())
                .orElseGet(() -> PlayerProgress.empty(event.userId()));

        PlayerProgress updated = switch (event.metric()) {
            case FOLLOWERS_GAINED -> current.toBuilder()
                    .followersGained(current.followersGained() + 1)
                    .updatedAt(event.recordedAt())
                    .build();
            case CHALLENGES_SENT -> current.toBuilder()
                    .challengesSent(current.challengesSent() + 1)
                    .updatedAt(event.recordedAt())
                    .build();
            default -> null;
        };
        if (updated == null) {
            return;
        }

        progressionRepositoryPort.save(updated);
        logger.debug("Compteur de badge projeté: userId={}, métrique={}",
                event.userId(), event.metric());
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
