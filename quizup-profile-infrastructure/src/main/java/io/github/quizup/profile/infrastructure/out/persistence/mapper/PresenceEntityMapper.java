package io.github.quizup.profile.infrastructure.out.persistence.mapper;

import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceEntity;

public final class PresenceEntityMapper {

    private PresenceEntityMapper() {
    }

    public static PlayerPresence toDomain(PresenceEntity entity) {
        return PlayerPresence.builder()
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .lastSeenAt(entity.getLastSeenAt())
                .build();
    }

    public static PresenceEntity toEntity(PlayerPresence presence) {
        PresenceEntity entity = new PresenceEntity();
        entity.setUserId(presence.userId());
        entity.setStatus(presence.status());
        entity.setLastSeenAt(presence.lastSeenAt());
        return entity;
    }
}
