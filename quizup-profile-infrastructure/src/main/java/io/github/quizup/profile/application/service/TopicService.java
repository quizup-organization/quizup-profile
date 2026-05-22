package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.port.out.TopicPort;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TopicService implements TopicPort {

    private final QueryGateway queryGateway;

    public TopicService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public Optional<ProfileTopic> findById(String topicId) {
        try {
            Topic topic = queryGateway.query(
                    new TopicQuery.GetTopicByIdQuery(topicId),
                    ResponseTypes.instanceOf(Topic.class)
            ).join();
            return Optional.of(new ProfileTopic(topic.topicId(), topic.name()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}

