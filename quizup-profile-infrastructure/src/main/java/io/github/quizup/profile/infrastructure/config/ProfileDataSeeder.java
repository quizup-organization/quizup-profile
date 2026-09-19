package io.github.quizup.profile.infrastructure.config;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.profile.domain.port.in.CheckProfileUseCase;
import io.github.quizup.profile.domain.port.in.CreateProfileUseCase;
import org.axonframework.modelling.command.AggregateStreamCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;

/**
 * ProfileDataSeeder — garantit l'existence des profils des utilisateurs système (admin, bot).
 *
 * <p>Ces utilisateurs sont créés par {@code quizup-identity} au démarrage. Si {@code profile}
 * n'était pas abonné au flux d'événements à cet instant, la saga {@code CreateProfileSaga} peut
 * ne jamais les traiter : ce seeder crée alors directement les profils manquants.</p>
 *
 * <p>Idempotent : on saute la création si la projection connaît déjà le profil ; sinon la
 * commande constructeur échoue sur {@link AggregateStreamCreationException} (profil présent dans
 * l'event store mais projection en retard) et l'erreur est ignorée.</p>
 */
@Component
public class ProfileDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProfileDataSeeder.class);

    private final CheckProfileUseCase checkProfileUseCase;
    private final CreateProfileUseCase createProfileUseCase;

    @Value("${app.seed-data.enabled:false}")
    private boolean seedDataEnabled;

    public ProfileDataSeeder(CheckProfileUseCase checkProfileUseCase,
                             CreateProfileUseCase createProfileUseCase) {
        this.checkProfileUseCase = checkProfileUseCase;
        this.createProfileUseCase = createProfileUseCase;
    }

    @Override
    public void run(String... args) {
        if (!seedDataEnabled) {
            logger.info("Profile data seeding is disabled (app.seed-data.enabled=false)");
            return;
        }

        logger.info("=== Starting Profile Data Seeding ===");

        seedProfile(QuizUpConstants.ADMIN_USER_ID, QuizUpConstants.ADMIN_USER_EMAIL,
                QuizUpConstants.ADMIN_USER_NAME, "Admin");
        seedProfile(QuizUpConstants.BOT_USER_ID, QuizUpConstants.BOT_USER_EMAIL,
                QuizUpConstants.BOT_USER_NAME, "Bot");

        logger.info("=== Profile Data Seeding Completed ===");
    }

    /**
     * Crée un profil s'il n'existe pas déjà dans l'event store (la commande constructeur échoue
     * si l'agrégat existe). Package-private pour les tests.
     */
    void seedProfile(String userId, String email, String displayName, String label) {
        if (Boolean.TRUE.equals(checkProfileUseCase.existsById(userId).join())) {
            logger.info("{} profile already exists, skipping creation", label);
            return;
        }

        try {
            createProfileUseCase.create(userId, email, displayName).join();
            logger.info("✓ {} profile created: {}", label, userId);
        } catch (CompletionException exception) {
            if (isAggregateAlreadyExists(exception)) {
                logger.info("{} profile already exists in the event store, skipping creation", label);
            } else {
                logger.error("Failed to seed {} profile", label, exception);
            }
        }
    }

    private boolean isAggregateAlreadyExists(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof AggregateStreamCreationException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
