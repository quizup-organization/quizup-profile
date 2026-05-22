# Plan de correction — Module `quizup-profile`

> Ce document liste **toutes les erreurs** de l'implémentation actuelle et fournit les corrections exactes à appliquer. Suivre l'ordre de la section 5 (ordre d'application).

---

## 1. Synthèse des problèmes critiques

| # | Fichier | Problème | Sévérité |
|---|---------|----------|----------|
| P1 | `ProfileEvent.java` | Les events embarquent `Profile profile` (l'état complet) → si le modèle évolue, le replay Axon casse | 🔴 Critique |
| P2 | `GameResultProjection.java` | `@EventHandler` qui envoie des commandes via `CommandGateway` → anti-pattern Axon absolu. Une projection **ne doit jamais** envoyer de commandes | 🔴 Critique |
| P3 | `ProfileAggregate.java` | Absence d'idempotence : `AddGameResultCommand` avec le même `gameId` est traité deux fois en cas de replay | 🔴 Critique |
| P4 | `UserService.java` | `user.name()` → `User` n'a pas de champ `name()`. Erreur de compilation | 🔴 Critique |
| P5 | `ProfileCommand.AddGameResultCommand` | Prend un objet `GameResult` complet (avec `topicName`, `opponentName`) → une commande ne doit pas contenir des données dénormalisées résolues à la volée | 🔴 Critique |
| P6 | `ProfileProjection.java` | Sauvegarde `event.profile()` directement → couplé à la structure du modèle, pas à l'état de l'event | 🟠 Majeur |
| P7 | `ProfileJpaRepository.java` | N'étend pas `JpaSpecificationExecutor` → `POST /search` (leaderboard) impossible | 🟠 Majeur |
| P8 | `ProfileRepositoryPort.java` | Manque `findAll(SearchCriteria)` → pas de recherche paginée | 🟠 Majeur |
| P9 | `ProfileRepositoryAdapter.java` | Pas de `JpaSearchAdapter` → search non implémenté | 🟠 Majeur |
| P10 | `UserSaga.java` | Nom non conforme (`UserSaga` au lieu de `UserRegisteredProfileSaga`) + `@EndSaga` manquant | 🟡 Mineur |
| P11 | `TopicStatisticsEmbeddable.java` | Manque `winStreak` (streaks par thème prévus dans le domaine) | 🟡 Mineur |
| P12 | `ProfileEntity.java` | `@Searchable` manquant sur `level`, `globalTotalExperience`, `globalWins` (nécessaires pour le leaderboard) + absence d'index | 🟡 Mineur |
| P13 | `ProfileController.java` | Manque `POST /search` (S5 leaderboard) | 🟡 Mineur |
| P14 | `CheckProfileUseCase.java` | Non utilisé nulle part — superflu, à supprimer | 🟡 Mineur |
| P15 | `ProfileQueryService.java` | Implémente `CheckProfileUseCase` supprimée → à nettoyer | 🟡 Mineur |
| P16 | `ProfileQuery.java` | Manque `SearchProfileQuery` pour le leaderboard | 🟡 Mineur |
| P17 | `GameResultProjection.java` | Résout `topicName` et `opponentName` synchrone avec `.join()` → risque de deadlock sur le thread Axon | 🟠 Majeur |

---

## 2. Corrections détaillées

### P1 + P6 — `ProfileEvent.java` : supprimer `Profile` des events

**Principe Axon** : un event ne doit contenir que les données **produites par l'action**, pas l'état complet de l'agrégat. Si `Profile` évolue structurellement, les anciens events stockés en base deviennent illisibles lors du replay.

**Remplacer intégralement :**

```java
// domain/event/ProfileEvent.java
package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.BadgeType;
import io.github.quizup.profile.domain.model.GameResultType;

import java.time.Instant;
import java.util.Set;

public interface ProfileEvent {

    String profileId();

    /**
     * Profil créé. profileId == userId.
     */
    record ProfileCreatedEvent(
            String profileId,
            Instant createdAt
    ) implements ProfileEvent {}

    /**
     * Résultat de partie enregistré.
     * Contient toutes les valeurs calculées POST-mise à jour
     * pour permettre à la projection de reconstruire l'état sans recalculer.
     */
    record GameResultRecordedEvent(
            String profileId,
            // Données de la partie
            String gameId,
            String topicId,
            String opponentId,
            int playerScore,
            int opponentScore,
            GameResultType result,
            // Valeurs calculées (état global après)
            int xpEarned,
            int newGlobalTotalExperience,
            int newGlobalWins,
            int newGlobalLosses,
            int newGlobalDraws,
            int newWinStreak,
            int newLossStreak,
            int newDrawStreak,
            // État thème après
            String topicTotalExperience,      // sérialisé comme int, voir note ci-dessous
            int newTopicTotalExperience,
            int newTopicWins,
            int newTopicLosses,
            int newTopicDraws,
            int newTopicWinStreak,
            // Badges nouvellement débloqués (peut être vide)
            Set<BadgeType> newBadges,
            boolean leveledUp,
            Instant recordedAt
    ) implements ProfileEvent {}
}
```

> **Note** : le champ `topicTotalExperience` (mal nommé ci-dessus) doit être `String topicTotalExperience` → corriger en `int newTopicTotalExperience` uniquement. Le `topicId` dans les champs topic permet à la projection de savoir quel thème mettre à jour.

**Version finale correcte de `ProfileEvent.java` :**

```java
package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.BadgeType;
import io.github.quizup.profile.domain.model.GameResultType;

import java.time.Instant;
import java.util.Set;

public interface ProfileEvent {

    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            Instant createdAt
    ) implements ProfileEvent {}

    record GameResultRecordedEvent(
            String profileId,
            String gameId,
            String topicId,
            String opponentId,
            int playerScore,
            int opponentScore,
            GameResultType result,
            int xpEarned,
            // État global POST-mise à jour
            int newGlobalTotalExperience,
            int newGlobalWins,
            int newGlobalLosses,
            int newGlobalDraws,
            int newWinStreak,
            int newLossStreak,
            int newDrawStreak,
            // État thème POST-mise à jour
            int newTopicTotalExperience,
            int newTopicWins,
            int newTopicLosses,
            int newTopicDraws,
            int newTopicWinStreak,
            // Badges débloqués dans cette partie
            Set<BadgeType> newBadges,
            boolean leveledUp,
            Instant recordedAt
    ) implements ProfileEvent {}
}
```

---

### P2 — `GameResultProjection.java` : SUPPRIMER ce fichier

Ce fichier est une **saga déguisée**. En Axon, un `@EventHandler` (projection) **ne doit jamais** envoyer de commandes. C'est le rôle exclusif d'une saga.

**Action** : Supprimer `GameResultProjection.java`. La logique d'écoute de `GameEvent.GameEndedEvent` et d'envoi de `AddGameResultCommand` doit être dans une saga.

**Créer à la place** : `GameEndedProfileSaga.java` (voir section 2.5).

---

### P3 — `ProfileAggregate.java` : ajouter l'idempotence + corriger les events

L'agrégat doit :
1. Stocker les `gameId` déjà traités (`processedGameIds`)
2. Retourner silencieusement si le `gameId` est déjà présent
3. Émettre le nouvel event `GameResultRecordedEvent` sans `Profile` complet

**Version corrigée complète :**

```java
package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.*;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.*;

import static io.github.quizup.profile.domain.model.ProfileRules.*;
import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Getter
@Aggregate
public class ProfileAggregate {

    @AggregateIdentifier
    private String profileId;

    private int winStreak;
    private int lossStreak;
    private int drawStreak;

    private int globalTotalExperience;
    private int globalWins;
    private int globalLosses;
    private int globalDraws;

    private Map<BadgeType, Badge> badges;
    private Map<String, TopicStatistics> topicStatistics;

    // Idempotence : gameIds déjà traités
    private Set<String> processedGameIds;

    private Instant createdAt;

    protected ProfileAggregate() {}

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        if (StringUtils.isBlank(command.profileId())) {
            throw new ProfileProblems.MissingProfileIdProblem(command.profileId());
        }

        apply(new ProfileEvent.ProfileCreatedEvent(command.profileId(), Instant.now()));
    }

    @CommandHandler
    public void handle(ProfileCommand.AddGameResultCommand command) {
        if (StringUtils.isBlank(command.profileId())) {
            throw new ProfileProblems.MissingProfileIdProblem(command.profileId());
        }
        if (StringUtils.isBlank(command.gameId())) {
            throw new ProfileProblems.MissingGameIdProblem(command.profileId());
        }
        if (StringUtils.isBlank(command.topicId())) {
            throw new ProfileProblems.MissingTopicIdProblem(command.profileId());
        }
        if (command.result() == null) {
            throw new ProfileProblems.MissingGameResultProblem(command.profileId());
        }
        if (command.playerScore() < 0) {
            throw new ProfileProblems.InvalidGameScoreProblem(command.profileId(), command.playerScore());
        }

        // Idempotence : ignorer silencieusement si déjà traité
        if (processedGameIds.contains(command.gameId())) {
            return;
        }

        int xpEarned = ProfileRules.computeXpEarned(command.playerScore(), command.result());

        // Calcul état global
        int newGlobalXp = this.globalTotalExperience + xpEarned;
        int newGlobalWins   = command.result() == GameResultType.WIN  ? globalWins  + 1 : globalWins;
        int newGlobalLosses = command.result() == GameResultType.LOSS ? globalLosses + 1 : globalLosses;
        int newGlobalDraws  = command.result() == GameResultType.DRAW ? globalDraws  + 1 : globalDraws;

        // Calcul streaks
        int newWinStreak  = command.result() == GameResultType.WIN  ? winStreak  + 1 : 0;
        int newLossStreak = command.result() == GameResultType.LOSS ? lossStreak + 1 : 0;
        int newDrawStreak = command.result() == GameResultType.DRAW ? drawStreak + 1 : 0;

        // Calcul état thème
        TopicStatistics current = topicStatistics.getOrDefault(
                command.topicId(), TopicStatistics.empty(command.topicId()));
        int newTopicXp      = current.totalExperience() + xpEarned;
        int newTopicWins    = command.result() == GameResultType.WIN  ? current.wins()   + 1 : current.wins();
        int newTopicLosses  = command.result() == GameResultType.LOSS ? current.losses() + 1 : current.losses();
        int newTopicDraws   = command.result() == GameResultType.DRAW ? current.draws()  + 1 : current.draws();
        int newTopicStreak  = command.result() == GameResultType.WIN  ? current.winStreak() + 1 : 0;

        // Badges
        Set<BadgeType> newBadges = computeNewBadges(
                command, newGlobalWins + newGlobalLosses + newGlobalDraws,
                newWinStreak, newTopicStreak,
                ProfileRules.computeLevelFromXp(newTopicXp));

        boolean leveledUp = ProfileRules.computeLevelFromXp(newGlobalXp)
                > ProfileRules.computeLevelFromXp(this.globalTotalExperience);

        apply(new ProfileEvent.GameResultRecordedEvent(
                profileId,
                command.gameId(),
                command.topicId(),
                command.opponentId(),
                command.playerScore(),
                command.opponentScore(),
                command.result(),
                xpEarned,
                newGlobalXp,
                newGlobalWins,
                newGlobalLosses,
                newGlobalDraws,
                newWinStreak,
                newLossStreak,
                newDrawStreak,
                newTopicXp,
                newTopicWins,
                newTopicLosses,
                newTopicDraws,
                newTopicStreak,
                newBadges,
                leveledUp,
                Instant.now()
        ));
    }

    // =============================================
    // EVENT SOURCING HANDLERS
    // =============================================

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId             = event.profileId();
        this.winStreak             = 0;
        this.lossStreak            = 0;
        this.drawStreak            = 0;
        this.globalTotalExperience = 0;
        this.globalWins            = 0;
        this.globalLosses          = 0;
        this.globalDraws           = 0;
        this.badges                = new EnumMap<>(BadgeType.class);
        this.topicStatistics       = new HashMap<>();
        this.processedGameIds      = new HashSet<>();
        this.createdAt             = event.createdAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.GameResultRecordedEvent event) {
        this.globalTotalExperience = event.newGlobalTotalExperience();
        this.globalWins            = event.newGlobalWins();
        this.globalLosses          = event.newGlobalLosses();
        this.globalDraws           = event.newGlobalDraws();
        this.winStreak             = event.newWinStreak();
        this.lossStreak            = event.newLossStreak();
        this.drawStreak            = event.newDrawStreak();

        TopicStatistics updatedTopic = new TopicStatistics(
                event.topicId(),
                event.newTopicTotalExperience(),
                event.newTopicWins(),
                event.newTopicLosses(),
                event.newTopicDraws(),
                event.newTopicWinStreak()
        );
        this.topicStatistics.put(event.topicId(), updatedTopic);

        event.newBadges().forEach(badgeType ->
                this.badges.putIfAbsent(badgeType, new Badge(badgeType, event.recordedAt())));

        this.processedGameIds.add(event.gameId());
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private Set<BadgeType> computeNewBadges(
            ProfileCommand.AddGameResultCommand command,
            int newTotalGames,
            int newWinStreak,
            int newTopicWinStreak,
            int newTopicLevel) {

        Set<BadgeType> awarded = new HashSet<>();
        Instant now = Instant.now();

        if (command.result() == GameResultType.WIN && globalWins == 0 && !badges.containsKey(BadgeType.FIRST_WIN)) {
            awarded.add(BadgeType.FIRST_WIN);
        }
        if (command.playerScore() >= PERFECT_GAME_SCORE && !badges.containsKey(BadgeType.PERFECT_SCORE)) {
            awarded.add(BadgeType.PERFECT_SCORE);
        }
        if (newWinStreak >= FIRE_STREAK_5_THRESHOLD && !badges.containsKey(BadgeType.FIRE_STREAK_5)) {
            awarded.add(BadgeType.FIRE_STREAK_5);
        }
        if (newWinStreak >= FIRE_STREAK_10_THRESHOLD && !badges.containsKey(BadgeType.FIRE_STREAK_10)) {
            awarded.add(BadgeType.FIRE_STREAK_10);
        }
        if (newTotalGames >= VETERAN_100_THRESHOLD && !badges.containsKey(BadgeType.VETERAN_100)) {
            awarded.add(BadgeType.VETERAN_100);
        }
        if (newTopicLevel >= SPECIALIST_LEVEL_THRESHOLD && !badges.containsKey(BadgeType.SPECIALIST)) {
            awarded.add(BadgeType.SPECIALIST);
        }
        return awarded;
    }
}
```

---

### P4 — `ProfileCommand.AddGameResultCommand` : paramètres plats

Une commande ne doit pas embarquer un objet `GameResult` pré-construit (qui contient `topicName`, `opponentName` résolus à la volée). Ces données dénormalisées n'appartiennent pas à la commande de modification d'état.

**Remplacer `ProfileCommand.java` :**

```java
package io.github.quizup.profile.domain.command;

import io.github.quizup.profile.domain.model.GameResultType;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface ProfileCommand {

    String profileId();

    /**
     * Crée un profil. profileId == userId (convention obligatoire).
     */
    record CreateProfileCommand(
            @TargetAggregateIdentifier String profileId
    ) implements ProfileCommand {}

    /**
     * Enregistre un résultat de partie.
     * gameId garantit l'idempotence.
     * Pas de topicName/opponentName : données de présentation, pas de domaine.
     */
    record AddGameResultCommand(
            @TargetAggregateIdentifier String profileId,
            String gameId,
            String topicId,
            String opponentId,
            int playerScore,
            int opponentScore,
            GameResultType result
    ) implements ProfileCommand {}
}
```

**Mettre à jour `AddGameResultUseCase.java` en conséquence :**

```java
package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.GameResultType;
import java.util.concurrent.CompletableFuture;

public interface AddGameResultUseCase {

    CompletableFuture<String> addGameResult(ProfileCommand.AddGameResultCommand command);
}
```

---

### P2 (suite) — Créer `GameEndedProfileSaga.java`

Ce fichier **remplace** `GameResultProjection.java` (à supprimer).

```java
package io.github.quizup.profile.application.saga;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.GameResultType;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enregistre les résultats de partie sur les profils des deux joueurs.
 * One-shot : @StartSaga + @EndSaga sur le même handler.
 * Bot (QuizUpConstants.BOT_USER_ID) ignoré.
 */
@Saga
public class GameEndedProfileSaga {

    private static final Logger logger = LoggerFactory.getLogger(GameEndedProfileSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Getter @Setter
    private String gameId;

    @StartSaga
    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameEndedEvent event) {
        this.gameId = event.gameId();
        logger.info("GameEndedProfileSaga: processing game results for profiles, gameId={}", event.gameId());

        // Player 1
        commandGateway.send(new ProfileCommand.AddGameResultCommand(
                event.player1Id(),   // profileId == userId
                event.gameId(),
                event.topicId(),
                event.player2Id(),
                event.player1FinalScore(),
                event.player2FinalScore(),
                determineResult(event.player1Id(), event.winnerId())
        ));

        // Player 2 — skip si bot
        if (!QuizUpConstants.BOT_USER_ID.equals(event.player2Id())) {
            commandGateway.send(new ProfileCommand.AddGameResultCommand(
                    event.player2Id(),
                    event.gameId(),
                    event.topicId(),
                    event.player1Id(),
                    event.player2FinalScore(),
                    event.player1FinalScore(),
                    determineResult(event.player2Id(), event.winnerId())
            ));
        }
    }

    private GameResultType determineResult(String playerId, String winnerId) {
        if (winnerId == null) return GameResultType.DRAW;
        return winnerId.equals(playerId) ? GameResultType.WIN : GameResultType.LOSS;
    }
}
```

---

### P4 — `UserService.java` : `user.name()` → `user.email()`

Le record `User` dans `quizup-identity-domain` n'a pas de champ `name()`. Utiliser `email()` ou supprimer le champ `name` du `ProfileUser`.

**Option recommandée** : le `ProfileUser` ne doit contenir que ce dont le profil a besoin. Supprimer `name` de `ProfileUser` et `UserService`. Les noms/pseudos affichés côté front viennent directement du `userId` résolu par le front via `/api/users/{userId}`.

**Supprimer `ProfileUser.java`, `UserPort.java`, `UserService.java`** car ils ne sont plus utilisés (la commande `AddGameResultCommand` ne contient plus `opponentName`).

Si `UserPort` est conservé pour une future fonctionnalité, corriger ainsi :

```java
// UserService.java — correction minimale
@Override
public Optional<ProfileUser> findById(String userId) {
    try {
        User user = queryGateway.query(
                new UserQuery.GetUserQuery(userId),
                ResponseTypes.instanceOf(User.class)
        ).join();
        // User n'a pas de name(), utiliser l'email ou l'userId comme display name
        return Optional.of(new ProfileUser(user.userId(), user.email()));
    } catch (Exception ignored) {
        return Optional.empty();
    }
}
```

**Si `TopicPort` / `TopicService` / `UserPort` / `UserService` sont supprimés**, supprimer aussi `ProfileTopic.java` et `ProfileUser.java`.

---

### P5 — `ProfileProjection.java` : reconstruction à partir des champs de l'event

La projection ne doit plus lire `event.profile()` (supprimé). Elle reconstruit l'état à partir des champs atomiques de l'event.

**Version corrigée :**

```java
package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.model.*;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProfileProjection {

    private static final Logger logger = LoggerFactory.getLogger(ProfileProjection.class);

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileProjection(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        logger.debug("Projecting ProfileCreatedEvent: profileId={}", event.profileId());
        Profile profile = Profile.empty(event.profileId(), event.createdAt());
        profileRepositoryPort.save(profile);
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.GameResultRecordedEvent event) {
        logger.debug("Projecting GameResultRecordedEvent: profileId={}, gameId={}",
                event.profileId(), event.gameId());

        profileRepositoryPort.findById(event.profileId()).ifPresent(existing -> {

            // Mise à jour des statistiques par thème
            Map<String, TopicStatistics> updatedTopics = new HashMap<>(existing.topicStatistics());
            updatedTopics.put(event.topicId(), new TopicStatistics(
                    event.topicId(),
                    event.newTopicTotalExperience(),
                    event.newTopicWins(),
                    event.newTopicLosses(),
                    event.newTopicDraws(),
                    event.newTopicWinStreak()
            ));

            // Mise à jour des badges
            Map<BadgeType, Badge> updatedBadges = new EnumMap<>(BadgeType.class);
            updatedBadges.putAll(existing.badges());
            event.newBadges().forEach(badgeType ->
                    updatedBadges.putIfAbsent(badgeType, new Badge(badgeType, event.recordedAt())));

            // Mise à jour de l'historique — insertion en tête + troncature
            List<GameResult> updatedRecent = buildUpdatedRecentGames(
                    existing.recentGameResults(), event);

            profileRepositoryPort.save(existing.toBuilder()
                    .globalStatistics(new GlobalStatistics(
                            event.newGlobalTotalExperience(),
                            event.newGlobalWins(),
                            event.newGlobalLosses(),
                            event.newGlobalDraws()))
                    .winStreak(event.newWinStreak())
                    .lossStreak(event.newLossStreak())
                    .drawStreak(event.newDrawStreak())
                    .topicStatistics(updatedTopics)
                    .badges(updatedBadges)
                    .recentGameResults(updatedRecent)
                    .updatedAt(event.recordedAt())
                    .build());
        });
    }

    private List<GameResult> buildUpdatedRecentGames(
            List<GameResult> existing, ProfileEvent.GameResultRecordedEvent event) {

        GameResult newEntry = GameResult.builder()
                .gameId(event.gameId())
                .topicId(event.topicId())
                .opponentId(event.opponentId())
                .playerScore(event.playerScore())
                .opponentScore(event.opponentScore())
                .result(event.result())
                .playedAt(event.recordedAt())
                .build();

        List<GameResult> result = new ArrayList<>();
        result.add(newEntry);
        result.addAll(existing);
        if (result.size() > ProfileRules.MAX_RECENT_GAMES) {
            return result.subList(0, ProfileRules.MAX_RECENT_GAMES);
        }
        return result;
    }
}
```

---

### P6 — `Profile.java` : retirer `topicName`/`opponentName` de `GameResult`

Ces champs de présentation ne doivent pas être dans le modèle de domaine.

**`GameResult.java` corrigé :**

```java
package io.github.quizup.profile.domain.model;

import lombok.Builder;
import java.time.Instant;

@Builder(toBuilder = true)
public record GameResult(
        String gameId,
        String topicId,
        String opponentId,
        int playerScore,
        int opponentScore,
        GameResultType result,
        Instant playedAt
) {}
```

**`GameResultResponse.java` corrigé** (les noms sont résolus côté front via les IDs) :

```java
package io.github.quizup.profile.infrastructure.in.api.response;

import io.github.quizup.profile.domain.model.GameResultType;
import java.io.Serializable;
import java.time.Instant;

public record GameResultResponse(
        String gameId,
        String topicId,
        String opponentId,
        int playerScore,
        int opponentScore,
        GameResultType result,
        Instant playedAt
) implements Serializable {}
```

---

### P7 + P8 + P9 — Search/Leaderboard

**`ProfileRepositoryPort.java` :**

```java
package io.github.quizup.profile.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.profile.domain.model.Profile;

import java.util.Optional;

public interface ProfileRepositoryPort {
    void save(Profile profile);
    Optional<Profile> findById(String profileId);
    boolean existsById(String profileId);
    PageResult<Profile> findAll(SearchCriteria searchCriteria);  // ← AJOUTER
}
```

**`ProfileJpaRepository.java` :**

```java
package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, String>,
        JpaSpecificationExecutor<ProfileEntity> {}   // ← AJOUTER JpaSpecificationExecutor
```

**`ProfileRepositoryAdapter.java` — ajouter `JpaSearchAdapter` :**

```java
package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ProfileEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProfileJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ProfileRepositoryAdapter implements ProfileRepositoryPort {

    private final ProfileJpaRepository profileJpaRepository;
    private final JpaSearchAdapter<ProfileEntity> searchAdapter;

    public ProfileRepositoryAdapter(ProfileJpaRepository profileJpaRepository) {
        this.profileJpaRepository = profileJpaRepository;
        this.searchAdapter = new JpaSearchAdapter<>(
                profileJpaRepository,
                new AnnotationSearchableEntity(ProfileEntity.class)
        );
    }

    @Override
    @Transactional
    public void save(Profile profile) {
        profileJpaRepository.save(ProfileEntityMapper.toEntity(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Profile> findById(String profileId) {
        return profileJpaRepository.findById(profileId).map(ProfileEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String profileId) {
        return profileJpaRepository.existsById(profileId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Profile> findAll(SearchCriteria searchCriteria) {
        return searchAdapter.findAll(searchCriteria).map(ProfileEntityMapper::toDomain);
    }
}
```

---

### P10 — `UserSaga.java` : renommer + `@EndSaga`

Renommer en `UserRegisteredProfileSaga.java`. L'annotation `@EndSaga` est la façon idiomatique Axon vs `SagaLifecycle.end()` manuel (les deux fonctionnent, mais `@EndSaga` est plus explicite).

```java
package io.github.quizup.profile.application.saga;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
public class UserRegisteredProfileSaga {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredProfileSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Getter @Setter
    private String userId;

    @StartSaga
    @EndSaga
    @SagaEventHandler(associationProperty = "userId")
    public void on(UserEvent.UserRegisteredEvent event) {
        this.userId = event.userId();

        if (QuizUpConstants.BOT_USER_ID.equals(event.userId())) {
            logger.debug("Skipping profile creation for BOT user");
            return;
        }

        logger.info("Creating profile for new user: userId={}", event.userId());
        commandGateway.send(new ProfileCommand.CreateProfileCommand(event.userId()));
    }
}
```

---

### P11 — `TopicStatistics.java` + `TopicStatisticsEmbeddable.java` : ajouter `winStreak`

```java
// TopicStatistics.java
@Builder(toBuilder = true)
public record TopicStatistics(
        String topicId,
        int totalExperience,
        int wins,
        int losses,
        int draws,
        int winStreak   // ← AJOUTER
) implements Statistics {

    public static TopicStatistics empty(String topicId) {
        return new TopicStatistics(topicId, 0, 0, 0, 0, 0);
    }
}
```

```java
// TopicStatisticsEmbeddable.java — ajouter le champ
@Column(name = "win_streak", nullable = false)
private int winStreak;   // ← AJOUTER
```

---

### P12 — `ProfileEntity.java` : `@Searchable` manquants + indexes

```java
// Ajouter @Searchable sur les champs du leaderboard
@Searchable(type = FieldType.NUMBER)
@Column(name = "global_total_experience", nullable = false)
private int globalTotalExperience;

@Searchable(type = FieldType.NUMBER)
@Column(name = "global_wins", nullable = false)
private int globalWins;

// Ajouter les indexes sur la classe
@Table(name = "profile_entry", indexes = {
        @Index(name = "idx_profile_total_xp", columnList = "global_total_experience"),
        @Index(name = "idx_profile_wins",     columnList = "global_wins"),
        @Index(name = "idx_profile_level",    columnList = "global_level")   // si colonne level dénormalisée
})
```

---

### P13 — `ProfileController.java` : ajouter `POST /search`

Ajouter `SearchProfileUseCase` et l'endpoint de leaderboard.

```java
package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.ProfileResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(ProfileController.ENDPOINT)
public class ProfileController {

    public static final String ENDPOINT = "/api/profiles";

    private final GetProfileUseCase getProfileUseCase;
    private final SearchProfileUseCase searchProfileUseCase;

    public ProfileController(GetProfileUseCase getProfileUseCase,
                             SearchProfileUseCase searchProfileUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.searchProfileUseCase = searchProfileUseCase;
    }

    /**
     * Profil complet d'un utilisateur.
     * Utilisé par : S4 (page profil), S18/S19 (résultats post-duel).
     */
    @GetMapping("/{profileId}")
    public CompletableFuture<ResponseEntity<ProfileResponse>> getById(@PathVariable String profileId) {
        return getProfileUseCase.getById(profileId)
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Recherche paginée — leaderboard global (S5).
     * Tri recommandé : sorts: [{ field: "globalTotalExperience", direction: "DESC" }]
     */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<ProfileResponse>>> search(
            @RequestBody SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchProfileUseCase.search(criteria.filters(), criteria.sorts(), criteria.page())
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}
```

---

### P14 + P15 — Supprimer `CheckProfileUseCase`

Ce use case n'est utilisé nulle part. Le supprimer de `ProfileQueryService` et supprimer le fichier.

**`ProfileQueryService.java` corrigé :**

```java
@Service
public class ProfileQueryService implements GetProfileUseCase, SearchProfileUseCase {
    // Supprimer : CheckProfileUseCase
    // ...
}
```

---

### P16 — `ProfileQuery.java` : ajouter `SearchProfileQuery`

```java
package io.github.quizup.profile.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;

import java.util.List;

public interface ProfileQuery {

    record GetProfileByIdQuery(String profileId) implements ProfileQuery {}

    record ProfileExistsByIdQuery(String profileId) implements ProfileQuery {}

    record SearchProfileQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements ProfileQuery, SearchQuery {}  // ← AJOUTER
}
```

---

## 3. Fichiers à créer (manquants)

### `SearchProfileUseCase.java` — NOUVEAU

```java
package io.github.quizup.profile.domain.port.in;

import io.github.quizup.common.domain.model.search.*;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchProfileUseCase {

    CompletableFuture<PageResult<Profile>> search(ProfileQuery.SearchProfileQuery query);

    default CompletableFuture<PageResult<Profile>> search(
            List<FilterCriteria> filters, List<SortCriteria> sorts, PageCriteria page) {
        return search(new ProfileQuery.SearchProfileQuery(filters, sorts, page));
    }
}
```

### Handler pour `SearchProfileQuery` dans `ProfileQueryHandler.java`

```java
// Ajouter dans ProfileQueryHandler
@QueryHandler
public PageResult<Profile> handle(ProfileQuery.SearchProfileQuery query) {
    return profileRepositoryPort.findAll(query);
}
```

### `ProfileResponseMapper.java` — correction pour `PageResult`

```java
// Ajouter la méthode manquante
public static PageResponse<ProfileResponse> toResponse(PageResult<Profile> pageResult) {
    return SearchResponseMapper.toSearchResponse(pageResult, ProfileResponseMapper::toResponse);
}
```

---

## 4. Fichiers à supprimer

| Fichier | Raison |
|---------|--------|
| `GameResultProjection.java` | Remplacé par `GameEndedProfileSaga.java` |
| `UserSaga.java` | Remplacé par `UserRegisteredProfileSaga.java` |
| `CheckProfileUseCase.java` | Non utilisé |
| `ProfileTopic.java` | Non nécessaire (voir P4) |
| `ProfileUser.java` | Non nécessaire (voir P4) |
| `TopicPort.java` | Non nécessaire si `GameResult` n'a plus `topicName` |
| `UserPort.java` | Non nécessaire si `GameResult` n'a plus `opponentName` |
| `TopicService.java` | Implémentation de `TopicPort` supprimé |
| `UserService.java` | Implémentation de `UserPort` supprimé |

---

## 5. Ordre d'application des corrections

```
1.  Supprimer les 9 fichiers listés en section 4
2.  ProfileEvent.java              → réécriture complète (P1)
3.  ProfileCommand.java            → réécriture (P5)
4.  GameResult.java                → supprimer topicName/opponentName (P6)
5.  TopicStatistics.java           → ajouter winStreak (P11)
6.  ProfileAggregate.java          → réécriture complète (P3 + alignement events)
7.  Profile.java                   → supprimer userId séparé si redondant avec profileId
8.  ProfileRepositoryPort.java     → ajouter findAll(SearchCriteria) (P8)
9.  ProfileQuery.java              → ajouter SearchProfileQuery (P16)
10. AddGameResultUseCase.java      → mettre à jour la signature (P5)
11. SearchProfileUseCase.java      → créer (section 3)
12. CheckProfileUseCase.java       → supprimer
13. ProfileJpaRepository.java      → ajouter JpaSpecificationExecutor (P7)
14. TopicStatisticsEmbeddable.java → ajouter winStreak (P11)
15. ProfileEntity.java             → ajouter @Searchable + @Table indexes (P12)
16. GameResultEmbeddable.java      → supprimer topicName/opponentName
17. ProfileEntityMapper.java       → aligner avec les changements de modèle
18. ProfileRepositoryAdapter.java  → ajouter JpaSearchAdapter + findAll (P9)
19. ProfileProjection.java         → réécriture (P2/P6)
20. ProfileQueryHandler.java       → ajouter handle(SearchProfileQuery)
21. ProfileQueryService.java       → supprimer CheckProfileUseCase, ajouter SearchProfileUseCase
22. UserRegisteredProfileSaga.java → créer (renommage + @EndSaga) (P10)
23. GameEndedProfileSaga.java      → créer (P2)
24. GameResultResponse.java        → supprimer topicName/opponentName
25. ProfileResponseMapper.java     → ajouter toResponse(PageResult), aligner GameResult
26. ProfileController.java         → ajouter POST /search + SearchProfileUseCase (P13)
```

---

## 6. Checklist de validation finale

```bash
# Aucun CommandGateway dans une projection
grep -r "CommandGateway\|commandGateway" \
  quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/projection/ \
  --include="*.java"
# → 0 résultats

# Aucun Profile/GameResult complet dans les events
grep -r "Profile profile\|GameResult gameResult" \
  quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/event/ \
  --include="*.java"
# → 0 résultats

# processedGameIds présent dans l'agrégat (idempotence)
grep -r "processedGameIds" \
  quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/aggregate/ \
  --include="*.java"
# → doit retourner des résultats

# @EndSaga présent dans les deux sagas
grep -r "@EndSaga" \
  quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/saga/ \
  --include="*.java"
# → doit retourner 2 occurrences

# Beans saga transient
grep -B1 "@Autowired" \
  quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/saga/ \
  -r --include="*.java"
# → tous les @Autowired précédés de "transient"

# JpaSpecificationExecutor présent
grep -r "JpaSpecificationExecutor" \
  quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/out/persistence/ \
  --include="*.java"
# → doit retourner ProfileJpaRepository

# Aucun user.name() (compilation)
grep -r "\.name()" \
  quizup-profile-infrastructure/src/main/java/ \
  --include="*.java"
# → 0 résultats (ou uniquement sur des types qui ont bien un champ name())
```