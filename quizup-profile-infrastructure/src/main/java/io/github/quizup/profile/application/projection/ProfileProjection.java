package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProfileProjection - Event Handlers pour maintenir la projection read-only
 * des profils (lecture : GET par id, recherche paginée).
 */
@Component
@ProcessingGroup("profile-projection")
public class ProfileProjection {

    private static final Logger logger = LoggerFactory.getLogger(ProfileProjection.class);

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileProjection(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    /**
     * Écoute ProfileCreatedEvent et crée l'entrée de projection.
     */
    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        Profile profile = Profile.builder()
                .userId(event.userId())
                .email(event.email())
                .displayName(event.displayName())
                .createdAt(event.createdAt())
                .updatedAt(event.createdAt())
                .build();

        profileRepositoryPort.save(profile);

        logger.info("Profile projected: userId={}", event.userId());
    }

    /**
     * Écoute ProfileUpdatedEvent et met à jour l'entrée de projection.
     */
    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileUpdatedEvent event) {
        profileRepositoryPort.findById(event.userId())
                .ifPresent(profile -> profileRepositoryPort.save(
                        profile.toBuilder()
                                .displayName(event.displayName())
                                .bio(event.bio())
                                .country(event.country())
                                .updatedAt(event.updatedAt())
                                .build()
                ));
    }
}
