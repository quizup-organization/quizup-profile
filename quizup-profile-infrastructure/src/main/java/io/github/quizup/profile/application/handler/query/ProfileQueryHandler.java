package io.github.quizup.profile.application.handler.query;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler Axon — Point d'entrée des queries sur le bus Axon.
 * Délègue aux ports sortants. Ne contient aucune logique propre.
 * ProfileQueryService fait la même chose pour les appels locaux (intra-service).
 */
@Component
public class ProfileQueryHandler {

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileQueryHandler(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    @QueryHandler
    public Profile handle(ProfileQuery.GetProfileQuery query) {
        return profileRepositoryPort.findById(query.userId())
                .orElseThrow(() -> new ProfileProblems.ProfileNotFoundProblem(query.userId()));
    }

    @QueryHandler
    public boolean handle(ProfileQuery.ProfileExistsByIdQuery query) {
        return profileRepositoryPort.existsById(query.userId());
    }

    @QueryHandler
    public List<Profile> handle(ProfileQuery.GetProfilesByIdsQuery query) {
        return profileRepositoryPort.findByIds(query.userIds());
    }

    @QueryHandler
    public PageResult<Profile> handle(ProfileQuery.ProfileSearchQuery query) {
        return profileRepositoryPort.findAll(query);
    }
}
