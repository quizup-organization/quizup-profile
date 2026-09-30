package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileRules;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.UnaryOperator;

/**
 * ProfileProjection - Event Handlers pour maintenir la projection read-only
 * des profils (lecture : GET par id, recherche paginée).
 *
 * <p>Un handler par événement de champ : la projection ne patche que le champ concerné.</p>
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
                .pseudonym(event.pseudonym())
                .language(ProfileRules.DEFAULT_LANGUAGE)
                .createdAt(event.createdAt())
                .updatedAt(event.createdAt())
                .build();

        profileRepositoryPort.save(profile);

        logger.info("Profile projected: userId={}", event.userId());
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfilePseudonymUpdatedEvent event) {
        update(event.userId(), event.updatedAt(),
                profile -> profile.toBuilder().pseudonym(event.pseudonym()).build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileBioUpdatedEvent event) {
        update(event.userId(), event.updatedAt(),
                profile -> profile.toBuilder().bio(event.bio()).build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileCountryUpdatedEvent event) {
        update(event.userId(), event.updatedAt(),
                profile -> profile.toBuilder().country(event.country()).build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileAvatarUpdatedEvent event) {
        update(event.userId(), event.updatedAt(),
                profile -> profile.toBuilder().avatarOptions(event.avatarOptions()).build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileLanguageUpdatedEvent event) {
        update(event.userId(), event.updatedAt(),
                profile -> profile.toBuilder().language(event.language()).build());
    }

    private void update(String userId, Instant updatedAt, UnaryOperator<Profile> patch) {
        profileRepositoryPort.findById(userId)
                .ifPresent(profile -> profileRepositoryPort.save(
                        patch.apply(profile).toBuilder()
                                .updatedAt(updatedAt)
                                .build()
                ));
    }
}
