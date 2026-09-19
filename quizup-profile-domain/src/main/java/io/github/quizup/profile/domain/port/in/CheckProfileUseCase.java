package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — vérification de l'existence d'un profil (utilisé par le seeding système).
 */
public interface CheckProfileUseCase {

    CompletableFuture<Boolean> existsById(ProfileQuery.ProfileExistsByIdQuery query);

    default CompletableFuture<Boolean> existsById(String userId) {
        return existsById(new ProfileQuery.ProfileExistsByIdQuery(userId));
    }
}
