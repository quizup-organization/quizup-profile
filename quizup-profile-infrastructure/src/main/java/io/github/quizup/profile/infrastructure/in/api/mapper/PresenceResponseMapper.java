package io.github.quizup.profile.infrastructure.in.api.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.infrastructure.in.api.response.PresenceResponse;

public final class PresenceResponseMapper {

    private PresenceResponseMapper() {
    }

    public static PresenceResponse toResponse(PlayerPresence presence) {
        return new PresenceResponse(presence.userId(), presence.status(), presence.lastSeenAt());
    }

    public static PageResponse<PresenceResponse> toResponse(PageResult<PlayerPresence> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, PresenceResponseMapper::toResponse);
    }
}
