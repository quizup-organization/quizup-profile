package io.github.quizup.profile.infrastructure.out.metrics.adapter;

import io.github.quizup.profile.domain.port.out.ProgressionMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Adapter Micrometer du {@link ProgressionMetricsPort}.
 * <p>Tags communs {@code application}/{@code environment}/{@code version} ajoutés par le SDK.
 */
@Component
public class ProgressionMetricsAdapter implements ProgressionMetricsPort {

    private final MeterRegistry registry;

    public ProgressionMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void profileCreated() {
        increment("quizup.profile.profiles.created");
    }

    @Override
    public void profileUpdated() {
        increment("quizup.profile.profiles.updated");
    }

    @Override
    public void xpAwarded(String topicId, int xp, boolean won) {
        String topic = topicId == null || topicId.isBlank() ? "unknown" : topicId;
        String victory = Boolean.toString(won);

        Counter.builder("quizup.profile.xp.awarded")
                .tag("topic", topic)
                .tag("won", victory)
                .register(registry)
                .increment();

        DistributionSummary.builder("quizup.profile.xp.amount")
                .tag("won", victory)
                .register(registry)
                .record(Math.max(xp, 0));
    }

    @Override
    public void levelReached(int level) {
        increment("quizup.profile.levels.reached");
        DistributionSummary.builder("quizup.profile.level")
                .register(registry)
                .record(Math.max(level, 0));
    }

    @Override
    public void badgeEarned(String badge) {
        Counter.builder("quizup.profile.badges.earned")
                .tag("badge", badge == null || badge.isBlank() ? "unknown" : badge)
                .register(registry)
                .increment();
    }

    private void increment(String name) {
        Counter.builder(name).register(registry).increment();
    }
}
