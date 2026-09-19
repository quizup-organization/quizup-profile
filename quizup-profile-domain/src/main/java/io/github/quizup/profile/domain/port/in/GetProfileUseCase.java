package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : récupération d'un profil par ID.
 * Implémenté par ProfileQueryService dans application/service/
 */
public interface GetProfileUseCase {

    /**
     * Récupère un profil par son identifiant utilisateur.
     *
     * @param query query contenant l'identifiant de l'utilisateur
     * @return un CompletableFuture contenant le profil
     * @throws ProfileProblems.ProfileNotFoundProblem si le profil n'existe pas
     */
    CompletableFuture<Profile> getById(ProfileQuery.GetProfileQuery query) throws ProfileProblems.ProfileNotFoundProblem;

    default CompletableFuture<Profile> getById(String userId) throws ProfileProblems.ProfileNotFoundProblem {
        return getById(new ProfileQuery.GetProfileQuery(userId));
    }
}
