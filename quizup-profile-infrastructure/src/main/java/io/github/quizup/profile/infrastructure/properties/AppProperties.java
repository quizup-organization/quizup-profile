package io.github.quizup.profile.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propriétés applicatives de {@code quizup-profile} ({@code app.*}) en record immuable.
 */
@ConfigurationProperties("app")
public record AppProperties(
        @DefaultValue SeedData seedData,
        @DefaultValue Activity activity) {

    public record SeedData(
            @DefaultValue("false") boolean enabled) {
    }

    public record Activity(
            @DefaultValue("Europe/Paris") String zone) {
    }
}
