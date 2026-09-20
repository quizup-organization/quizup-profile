package io.github.quizup.profile.application.handler.event;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.port.out.ProgressionMetricsPort;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Alimente les KPI métier de progression : profil, XP, niveaux, badges.
 * <p>Handlers {@link DisallowReplay} : un replay de projection ne réincrémente pas les compteurs.
 */
@Component
public class ProgressionMetricsEventHandler {

    private final ProgressionMetricsPort metrics;

    public ProgressionMetricsEventHandler(ProgressionMetricsPort metrics) {
        this.metrics = metrics;
    }

    @EventHandler
    @DisallowReplay
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        metrics.profileCreated();
    }

    @EventHandler
    @DisallowReplay
    public void on(ProfileEvent.ProfileUpdatedEvent event) {
        metrics.profileUpdated();
    }

    @EventHandler
    @DisallowReplay
    public void on(ProgressionEvent.XpAwardedEvent event) {
        metrics.xpAwarded(event.topicId(), event.xp(), event.won());
    }

    @EventHandler
    @DisallowReplay
    public void on(ProgressionEvent.LevelReachedEvent event) {
        metrics.levelReached(event.level());
    }

    @EventHandler
    @DisallowReplay
    public void on(ProgressionEvent.BadgeEarnedEvent event) {
        metrics.badgeEarned(event.badge() == null ? "unknown" : event.badge().name());
    }
}
