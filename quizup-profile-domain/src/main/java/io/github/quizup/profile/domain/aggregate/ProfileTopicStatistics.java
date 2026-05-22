package io.github.quizup.profile.domain.aggregate;

import lombok.Getter;
import org.axonframework.modelling.command.EntityId;

/**
 * Sous-agregat qui encapsule les stats d'un profil pour un topic donne.
 */
@Getter
public class ProfileTopicStatistics {

    @EntityId
    private final String topicId;

    public ProfileTopicStatistics(String topicId) {
        this.topicId = topicId;
    }
}

