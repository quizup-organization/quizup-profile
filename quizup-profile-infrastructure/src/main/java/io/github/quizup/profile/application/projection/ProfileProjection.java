package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProfileProjection {

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileProjection(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        profileRepositoryPort.save(event.profile());
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.GameResultRecordedEvent event) {
        profileRepositoryPort.save(event.profile());
    }
}

