package io.github.quizup.profile.application.saga;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.NameGenerator;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CreateProfileSaga - Orchestration : création du profil
 * dès qu'un utilisateur est enregistré par le service identity.
 *
 * <p>Idempotente : si le profil existe déjà (rejeu d'événement, seeding système), la commande
 * de création n'est pas renvoyée.</p>
 */
@Saga
public class CreateProfileSaga {

    private static final Logger logger = LoggerFactory.getLogger(CreateProfileSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient ProfileRepositoryPort profileRepositoryPort;

    @StartSaga
    @SagaEventHandler(associationProperty = "userId")
    public void on(UserEvent.UserRegisteredEvent event) {
        if (profileRepositoryPort.findById(event.userId()).isPresent()) {
            logger.debug("Profile already exists, skipping creation: userId={}", event.userId());
            return;
        }

        logger.info("User registered, creating profile: userId={}", event.userId());

        commandGateway.send(
                new ProfileCommand.CreateProfileCommand(
                        event.userId(),
                        event.email(),
                        NameGenerator.generate()
                )
        );
    }
}
