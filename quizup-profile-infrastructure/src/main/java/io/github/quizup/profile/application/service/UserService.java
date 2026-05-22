package io.github.quizup.profile.application.service;

import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.query.UserQuery;
import io.github.quizup.profile.domain.model.ProfileUser;
import io.github.quizup.profile.domain.port.out.UserPort;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements UserPort {

    private final QueryGateway queryGateway;

    public UserService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public Optional<ProfileUser> findById(String userId) {
        try {
            User user = queryGateway.query(
                    new UserQuery.GetUserQuery(userId),
                    ResponseTypes.instanceOf(User.class)
            ).join();
            return Optional.of(new ProfileUser(user.userId(), user.name()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}

