package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceSessionEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.PresenceEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.PresenceJpaRepository;
import io.github.quizup.profile.infrastructure.out.persistence.repository.PresenceSessionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class PresenceRepositoryAdapter implements PresenceRepositoryPort {

    private static final Logger logger = LoggerFactory.getLogger(PresenceRepositoryAdapter.class);

    private final PresenceJpaRepository presenceJpaRepository;
    private final PresenceSessionJpaRepository presenceSessionJpaRepository;
    private final JpaSearchAdapter<PresenceEntity> presenceJpaSearchAdapter;

    public PresenceRepositoryAdapter(PresenceJpaRepository presenceJpaRepository,
                                     PresenceSessionJpaRepository presenceSessionJpaRepository) {
        this.presenceJpaRepository = presenceJpaRepository;
        this.presenceSessionJpaRepository = presenceSessionJpaRepository;
        this.presenceJpaSearchAdapter =
                new JpaSearchAdapter<>(presenceJpaRepository, new AnnotationSearchableEntity(PresenceEntity.class));
    }

    @Override
    @Transactional
    public PlayerPresence save(PlayerPresence presence) {
        PresenceEntity saved = presenceJpaRepository.save(PresenceEntityMapper.toEntity(presence));
        return PresenceEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerPresence> findById(String userId) {
        return presenceJpaRepository.findById(userId).map(PresenceEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PlayerPresence> findAll(SearchCriteria searchCriteria) {
        return presenceJpaSearchAdapter.findAll(searchCriteria, PresenceEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void addSession(String sessionId, String userId) {
        PresenceSessionEntity session = new PresenceSessionEntity();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setConnectedAt(Instant.now());
        presenceSessionJpaRepository.save(session);
    }

    @Override
    @Transactional
    public void removeSession(String sessionId) {
        presenceSessionJpaRepository.deleteById(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSessions(String userId) {
        return presenceSessionJpaRepository.countByUserId(userId);
    }

    /**
     * Les sessions STOMP vivent dans l'instance : au redémarrage, toutes sont mortes. On purge
     * donc les sessions et on repasse toutes les présences {@code OFFLINE} pour éviter les
     * faux « en ligne » après un crash.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void resetSessionsOnStartup() {
        long sessions = presenceSessionJpaRepository.count();
        presenceSessionJpaRepository.deleteAll();
        int presences = presenceJpaRepository.updateStatusForAll(PresenceStatus.OFFLINE);
        logger.info("Presence sessions purged on startup: {} session(s), {} presence(s) reset offline",
                sessions, presences);
    }
}
