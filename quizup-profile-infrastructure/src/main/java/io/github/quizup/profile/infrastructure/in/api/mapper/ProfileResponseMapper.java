package io.github.quizup.profile.infrastructure.in.api.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;


public final class ProfileResponseMapper {
    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.userId(),
                profile.email(),
                profile.displayName(),
                profile.bio(),
                profile.country(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    public static PageResponse<ProfileResponse> toResponse(PageResult<Profile> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, ProfileResponseMapper::toResponse);
    }
}
