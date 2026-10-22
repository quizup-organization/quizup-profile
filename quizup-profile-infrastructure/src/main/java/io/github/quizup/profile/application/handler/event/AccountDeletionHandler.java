package io.github.quizup.profile.application.handler.event;

import io.github.quizup.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import io.github.quizup.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Effacement local du profil, de la progression et de la présence à la suppression du compte.
 * Les événements Axon correspondants sont purgés par le job d'effacement dédié.
 */
@Component
@ProcessingGroup("account-deletion-profile")
public class AccountDeletionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final AxonEventStoreErasureService eventStoreErasureService;

    public AccountDeletionHandler(JdbcTemplate jdbcTemplate,
                                  AxonEventStoreErasureService eventStoreErasureService) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventStoreErasureService = eventStoreErasureService;
    }

    @EventHandler
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        String userId = event.userId();
        jdbcTemplate.update("DELETE FROM progression_activity_day_game WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_activity_day WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_activity WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_awarded_game WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_topic_games WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_topic_entry WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_badge WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM progression_entry WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM presence_session WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM presence_entry WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM profile_entry WHERE user_id = ?", userId);
        eventStoreErasureService.eraseAggregates(List.of(
                userId,
                "progress:" + userId,
                "badges:" + userId));
    }
}
