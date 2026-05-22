package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.ProfileUser;

import java.util.Optional;

public interface UserPort {

    Optional<ProfileUser> findById(String userId);
}

