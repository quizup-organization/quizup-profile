package io.github.quizup.profile.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active le scheduler Spring (balayeur de présence hors ligne, activités planifiées).
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
