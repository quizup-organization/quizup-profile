package io.github.quizup.profile.application.saga;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
public class UserSaga {

    @Autowired
    private transient CommandGateway commandGateway;

    @Getter
    @Setter
    private String userId;

    @StartSaga
    @SagaEventHandler(associationProperty = "userId")
    public void on(UserEvent.UserRegisteredEvent event) {
        this.userId = event.userId();
        commandGateway.send(new ProfileCommand.CreateProfileCommand(event.userId()));
        SagaLifecycle.end();
    }
}

