package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.ProfileTopic;

import java.util.Optional;

public interface TopicPort {

    Optional<ProfileTopic> findById(String topicId);
}

