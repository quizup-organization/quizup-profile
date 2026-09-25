package io.github.quizup.profile.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.profile.domain.model.Profile;

import java.util.List;
import java.util.Optional;

/**
 * Port sortant - Lecture/écriture des profils depuis la persistance.
 */
public interface ProfileRepositoryPort {

    /**
     * Persiste un profil (création ou mise à jour).
     *
     * @param profile le modèle domaine à persister
     */
    void save(Profile profile);

    /**
     * Trouve un profil par son identifiant utilisateur.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return le profil trouvé, ou Optional.empty() s'il n'existe pas
     */
    Optional<Profile> findById(String userId);

    /**
     * Trouve plusieurs profils par leurs identifiants (résolution en lot).
     *
     * @param userIds les identifiants recherchés
     * @return les profils existants (ordre non garanti)
     */
    List<Profile> findByIds(List<String> userIds);

    /**
     * Recherche paginée de profils selon des critères.
     *
     * @param searchCriteria les critères de recherche
     * @return la page de profils correspondante
     */
    SearchResponse<Profile> findAll(SearchRequest request);

    /**
     * Vérifie si un profil existe pour cet utilisateur.
     *
     * @param userId l'identifiant de l'utilisateur à vérifier
     * @return true si un profil existe
     */
    boolean existsById(String userId);
}
