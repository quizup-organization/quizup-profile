package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.ActivityView;
import io.github.quizup.profile.domain.port.in.GetActivityUseCase;
import io.github.quizup.profile.domain.query.ActivityQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/**
 * Handler Axon — activité journalière. Expose le use case historique sur le bus distribué pour
 * que le BFF (surface unique) puisse le lire.
 */
@Component
public class ActivityQueryHandler {

    private final GetActivityUseCase getActivityUseCase;

    public ActivityQueryHandler(GetActivityUseCase getActivityUseCase) {
        this.getActivityUseCase = getActivityUseCase;
    }

    @QueryHandler
    public ActivityView handle(ActivityQuery.GetActivityQuery query) {
        return getActivityUseCase.get(query.userId(), query.from(), query.to());
    }
}
