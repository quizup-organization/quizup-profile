package io.github.quizup.profile.application.saga;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
public class UserRegisteredProfileSaga {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredProfileSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Getter
    @Setter
    private String userId;

    @StartSaga
    @EndSaga
    @SagaEventHandler(associationProperty = "userId")
    public void on(UserEvent.UserRegisteredEvent event) {
        this.userId = event.userId();

        if (QuizUpConstants.BOT_USER_ID.equals(event.userId())) {
            logger.debug("Skipping profile creation for BOT user");
            return;
        }

        logger.info("Creating profile for new user: userId={}", event.userId());
        commandGateway.send(new ProfileCommand.CreateProfileCommand(event.userId()));
    }
}

