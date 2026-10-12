package io.github.quizup.profile.application.saga;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.PseudonymGenerator;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CreateProfileSaga - Orchestration : création du profil dès qu'un utilisateur est enregistré
 * par le service identity.
 *
 * <p>Idempotente <b>sans lecture de projection</b> : la commande de création est idempotente dans
 * l'agrégat ({@code CREATE_IF_MISSING}, no-op si le profil existe déjà). La saga ne dépend donc
 * pas de l'état courant d'un read model (replay-safe) — voir
 * {@code best-practices/.backend/anti-patterns.md}.</p>
 */
@Saga
@ProcessingGroup("create-profile-saga")
public class CreateProfileSaga {

    @Autowired
    private transient CommandGateway commandGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "userId")
    public void on(UserEvent.UserRegisteredEvent event) {
        // Fire-and-forget : sendAndWait bloque le thread du tracking processor (perf).
        commandGateway.send(
                new ProfileCommand.CreateProfileCommand(
                        event.userId(),
                        event.email(),
                        PseudonymGenerator.generate()
                )
        );

        SagaLifecycle.end();
    }
}
