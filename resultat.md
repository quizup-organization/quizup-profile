### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/aggregate/ProfileAggregate.java
```java
package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.event.ProfileTopicEvent;
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

import static io.github.quizup.profile.domain.model.ProfileRules.MAX_RECENT_GAMES;
import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Getter
@Aggregate
public class ProfileAggregate {

    @AggregateIdentifier
    private String profileId;

    private int level;
    private int experience;

    private int wins;
    private int losses;
    private int draws;

    private int winStreak;
    private int lossStreak;
    private int drawStreak;

    private Set<Badge> badges;
    private Deque<ProfileGame> games;
    private Map<String, ProfileTopicAggregate> topics;

    private Instant createdAt;
    private Instant updatedAt;

    protected ProfileAggregate() {
    }

    // ── Command Handlers ────────────────────────────────────────────────────

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        apply(
                new ProfileEvent.ProfileCreatedEvent(
                        command.profileId(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.AddGameResultCommand command) {
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

        if (command.opponentScore() < 0) {
            throw new ProfileProblems.InvalidGameScoreProblem(command.profileId(), command.opponentScore());
        }

        ProfileGame game = ProfileGame.builder()
                .gameId(command.gameId())
                .topicId(command.topicId())
                .opponentId(command.opponentId())
                .opponentName(command.opponentName())
                .playerScore(command.playerScore())
                .opponentScore(command.opponentScore())
                .result(command.result())
                .playedAt(command.playedAt())
                .build();

        Instant now = Instant.now();
        String topicId = command.topicId();
        GameResult result = command.result();

        int experienceEarned = ProfileRules.computeXpEarned(command.playerScore(), result);

        // ── Topic events ────────────────────────────────────────────────────

        boolean isNewTopic = !topics.containsKey(topicId);

        if (isNewTopic) {
            apply(
                    new ProfileTopicEvent.ProfileTopicCreatedEvent(
                            profileId,
                            topicId,
                            now
                    )
            );
        }

        ProfileTopicAggregate topicAggregate = isNewTopic
                ? new ProfileTopicAggregate(topicId, now)
                : topics.get(topicId);

        apply(
                new ProfileTopicEvent.TopicExperienceIncreasedEvent(
                        profileId,
                        topicId,
                        experienceEarned,
                        now
                )
        );

        int topicExperience = topicAggregate.getExperience() + experienceEarned;
        int topicLevel = ProfileRules.computeLevelFromXp(topicExperience);

        if (topicLevel > topicAggregate.getLevel()) {
            apply(
                    new ProfileTopicEvent.TopicLevelIncreasedEvent(
                            profileId,
                            topicId,
                            topicLevel,
                            now
                    )
            );
        }

        switch (result) {
            case WIN -> apply(
                    new ProfileTopicEvent.TopicWinsIncreasedEvent(
                            profileId,
                            topicId,
                            topicAggregate.getWins() + 1,
                            now
                    )
            );
            case LOSS -> apply(
                    new ProfileTopicEvent.TopicLossesIncreasedEvent(
                            profileId,
                            topicId,
                            topicAggregate.getLosses() + 1,
                            now
                    )
            );
            case DRAW -> apply(
                    new ProfileTopicEvent.TopicDrawsIncreasedEvent(
                            profileId,
                            topicId,
                            topicAggregate.getDraws() + 1,
                            now
                    )
            );
        }

        ProfileGame topicPreviousGame = (topicAggregate.getGames() != null && !topicAggregate.getGames().isEmpty())
                ? topicAggregate.getGames().peekLast()
                : null;
        Streak topicStreak = ProfileRules.computeStreak(
                ProfileStreak.of(topicAggregate.getWinStreak(), topicAggregate.getLossStreak(), topicAggregate.getDrawStreak()),
                game,
                topicPreviousGame
        );
        applyTopicStreakEvents(profileId, topicId, result, topicAggregate, topicStreak, now);

        apply(new ProfileTopicEvent.TopicGamePlayedEvent(profileId, topicId, game, now));

        // ── Global events ───────────────────────────────────────────────────

        apply(new ProfileEvent.ExperienceIncreasedEvent(profileId, experienceEarned, now));

        int newGlobalXp = experience + experienceEarned;
        int newGlobalLevel = ProfileRules.computeLevelFromXp(newGlobalXp);
        if (newGlobalLevel > level) {
            apply(new ProfileEvent.LevelIncreasedEvent(profileId, newGlobalLevel, now));
        }

        switch (result) {
            case WIN -> apply(
                    new ProfileEvent.WinsIncreasedEvent(
                            profileId,
                            wins + 1,
                            now
                    )
            );
            case LOSS -> apply(new ProfileEvent.LossesIncreasedEvent(profileId, losses + 1, now));
            case DRAW -> apply(new ProfileEvent.DrawsIncreasedEvent(profileId, draws + 1, now));
        }

        ProfileGame globalPreviousGame = (games != null && !games.isEmpty()) ? games.peekLast() : null;
        Streak globalStreak = ProfileRules.computeStreak(
                ProfileStreak.of(winStreak, lossStreak, drawStreak),
                game,
                globalPreviousGame
        );
        applyGlobalStreakEvents(profileId, result, globalStreak, now);

        apply(new ProfileEvent.GamePlayedEvent(profileId, game, now));
    }

    // ── Event Sourcing Handlers ─────────────────────────────────────────────

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.experience = 0;
        this.level = 0;
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.badges = new HashSet<>();
        this.topics = new HashMap<>();
        this.games = new ArrayDeque<>(MAX_RECENT_GAMES);
        this.createdAt = event.createdAt();
        this.updatedAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(ProfileTopicEvent.ProfileTopicCreatedEvent event) {
        topics.put(event.topicId(), new ProfileTopicAggregate(event.topicId(), event.createdAt()));
    }

    @EventSourcingHandler
    public void on(ProfileTopicEvent event) {
        ProfileTopicAggregate topic = topics.get(event.topicId());
        if (topic == null) return;
        switch (event) {
            case ProfileTopicEvent.TopicExperienceIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLevelIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicWinsIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLossesIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicDrawsIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicWinStreakIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicWinStreakEndedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLossStreakIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLossStreakEndedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicDrawStreakIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicDrawStreakEndedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicGamePlayedEvent e -> topic.on(e);
            default -> { /* ProfileTopicCreatedEvent traité séparément */ }
        }
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ExperienceIncreasedEvent event) {
        this.experience += event.experienceEarned();
        this.updatedAt = event.earnedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LevelIncreasedEvent event) {
        this.level = event.level();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.WinsIncreasedEvent event) {
        this.wins = event.wins();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LossesIncreasedEvent event) {
        this.losses = event.losses();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.DrawsIncreasedEvent event) {
        this.draws = event.draws();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.WinStreakIncreasedEvent event) {
        this.winStreak = event.winStreak();
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.WinStreakEndedEvent event) {
        this.winStreak = 0;
        this.updatedAt = event.endedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LossStreakIncreasedEvent event) {
        this.lossStreak = event.lossStreak();
        this.winStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LossStreakEndedEvent event) {
        this.lossStreak = 0;
        this.updatedAt = event.endedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.DrawStreakIncreasedEvent event) {
        this.drawStreak = event.drawStreak();
        this.winStreak = 0;
        this.lossStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.DrawStreakEndedEvent event) {
        this.drawStreak = 0;
        this.updatedAt = event.endedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.GamePlayedEvent event) {
        if (this.games.size() >= MAX_RECENT_GAMES) {
            this.games.pollFirst();
        }
        this.games.addLast(event.game());
        this.updatedAt = event.playedAt();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void applyTopicStreakEvents(String profileId,
                                        String topicId,
                                        GameResult result,
                                        ProfileTopicAggregate topic,
                                        Streak newStreak,
                                        Instant now) {
        switch (result) {
            case WIN -> {
                if (topic.getLossStreak() > 0)
                    apply(new ProfileTopicEvent.TopicLossStreakEndedEvent(profileId, topicId, now));
                if (topic.getDrawStreak() > 0)
                    apply(new ProfileTopicEvent.TopicDrawStreakEndedEvent(profileId, topicId, now));
                apply(new ProfileTopicEvent.TopicWinStreakIncreasedEvent(profileId, topicId, newStreak.winStreak(), now));
            }
            case LOSS -> {
                if (topic.getWinStreak() > 0)
                    apply(new ProfileTopicEvent.TopicWinStreakEndedEvent(profileId, topicId, now));
                if (topic.getDrawStreak() > 0)
                    apply(new ProfileTopicEvent.TopicDrawStreakEndedEvent(profileId, topicId, now));
                apply(new ProfileTopicEvent.TopicLossStreakIncreasedEvent(profileId, topicId, newStreak.lossStreak(), now));
            }
            case DRAW -> {
                if (topic.getWinStreak() > 0)
                    apply(new ProfileTopicEvent.TopicWinStreakEndedEvent(profileId, topicId, now));
                if (topic.getLossStreak() > 0)
                    apply(new ProfileTopicEvent.TopicLossStreakEndedEvent(profileId, topicId, now));
                apply(new ProfileTopicEvent.TopicDrawStreakIncreasedEvent(profileId, topicId, newStreak.drawStreak(), now));
            }
        }
    }

    private void applyGlobalStreakEvents(String profileId, GameResult result, Streak newStreak, Instant now) {
        switch (result) {
            case WIN -> {
                if (lossStreak > 0) apply(new ProfileEvent.LossStreakEndedEvent(profileId, now));
                if (drawStreak > 0) apply(new ProfileEvent.DrawStreakEndedEvent(profileId, now));
                apply(new ProfileEvent.WinStreakIncreasedEvent(profileId, newStreak.winStreak(), now));
            }
            case LOSS -> {
                if (winStreak > 0) apply(new ProfileEvent.WinStreakEndedEvent(profileId, now));
                if (drawStreak > 0) apply(new ProfileEvent.DrawStreakEndedEvent(profileId, now));
                apply(new ProfileEvent.LossStreakIncreasedEvent(profileId, newStreak.lossStreak(), now));
            }
            case DRAW -> {
                if (winStreak > 0) apply(new ProfileEvent.WinStreakEndedEvent(profileId, now));
                if (lossStreak > 0) apply(new ProfileEvent.LossStreakEndedEvent(profileId, now));
                apply(new ProfileEvent.DrawStreakIncreasedEvent(profileId, newStreak.drawStreak(), now));
            }
        }
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/aggregate/ProfileTopicAggregate.java
```java
package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.event.ProfileTopicEvent;
import io.github.quizup.profile.domain.model.ProfileGame;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

import static io.github.quizup.profile.domain.model.ProfileRules.MAX_RECENT_GAMES;

@Getter
public class ProfileTopicAggregate {

    private String topicId;

    private int level;
    private int experience;

    private int wins;
    private int losses;
    private int draws;

    private int winStreak;
    private int lossStreak;
    private int drawStreak;

    private Deque<ProfileGame> games;

    private Instant createdAt;
    private Instant updatedAt;

    protected ProfileTopicAggregate() {
    }

    public ProfileTopicAggregate(String topicId, Instant createdAt) {
        this.topicId = topicId;
        this.experience = 0;
        this.level = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.games = new ArrayDeque<>();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // ── EventSourcingHandlers ───────────────────────────────────────────────

    public void on(ProfileTopicEvent.ProfileTopicCreatedEvent event) {
        this.topicId = event.topicId();
        this.experience = 0;
        this.level = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.games = new ArrayDeque<>();
        this.createdAt = event.createdAt();
        this.updatedAt = event.createdAt();
    }

    public void on(ProfileTopicEvent.TopicExperienceIncreasedEvent event) {
        this.experience += event.experienceEarned();
        this.updatedAt = event.earnedAt();
    }

    public void on(ProfileTopicEvent.TopicLevelIncreasedEvent event) {
        this.level = event.level();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicWinsIncreasedEvent event) {
        this.wins = event.wins();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicLossesIncreasedEvent event) {
        this.losses = event.losses();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicDrawsIncreasedEvent event) {
        this.draws = event.draws();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicWinStreakIncreasedEvent event) {
        this.winStreak = event.winStreak();
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicWinStreakEndedEvent event) {
        this.winStreak = 0;
        this.updatedAt = event.endedAt();
    }

    public void on(ProfileTopicEvent.TopicLossStreakIncreasedEvent event) {
        this.lossStreak = event.lossStreak();
        this.winStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicLossStreakEndedEvent event) {
        this.lossStreak = 0;
        this.updatedAt = event.endedAt();
    }

    public void on(ProfileTopicEvent.TopicDrawStreakIncreasedEvent event) {
        this.drawStreak = event.drawStreak();
        this.winStreak = 0;
        this.lossStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicDrawStreakEndedEvent event) {
        this.drawStreak = 0;
        this.updatedAt = event.endedAt();
    }

    public void on(ProfileTopicEvent.TopicGamePlayedEvent event) {
        if (this.games == null) {
            this.games = new ArrayDeque<>();
        }
        if (this.games.size() >= MAX_RECENT_GAMES) {
            this.games.pollFirst();
        }
        this.games.addLast(event.game());
        this.updatedAt = event.playedAt();
    }
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/ProfileStreak.java
```java
package io.github.quizup.profile.domain.model;

public record ProfileStreak(
        int winStreak,
        int lossStreak,
        int drawStreak
) implements Streak {

    public static ProfileStreak of(int winStreak, int lossStreak, int drawStreak) {
        return new ProfileStreak(winStreak, lossStreak, drawStreak);
    }
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/GameResult.java
```java
package io.github.quizup.profile.domain.model;

public enum GameResult {
    WIN,
    LOSS,
    DRAW
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/Profile.java
```java
package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
public record Profile(
        String profileId,
        int totalExperience,
        int level,
        int wins,
        int losses,
        int draws,
        int winStreak,
        int lossStreak,
        int drawStreak,
        Map<String, ProfileTopic> topics,
        List<ProfileGame> games,
        Instant createdAt,
        Instant updatedAt
) implements Statistics {

    @Override
    public int level() {
        return level;
    }

    public static Profile empty(String profileId, Instant now) {
        return new Profile(
                profileId,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                Map.of(),
                List.of(),
                now,
                now
        );
    }
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/Badge.java
```java
package io.github.quizup.profile.domain.model;

public enum Badge {
    FIRST_VICTORY,  // Première victoire toutes catégories
    PERFECTIONIST,  // Score parfait 160/160 + victoire
    FIRE_STREAK     // 10 victoires consécutives sur un même thème
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/ProfileRules.java
```java
package io.github.quizup.profile.domain.model;

import java.util.Optional;

public final class ProfileRules {

    public static final int MAX_RECENT_GAMES = 10;
    public static final int PERFECT_GAME_SCORE = 160;

    public static final int GAME_BONUS = 40;
    public static final int VICTORY_BONUS = 50;

    private static final int LEVEL_FACTOR = 25;

    private ProfileRules() {
    }

    /**
     * XP totale nécessaire pour atteindre le début du niveau {@code level}.
     * Niveau 0 → 0 XP. Niveau 1 → 100 XP. etc.
     */
    public static int computeXpForLevel(int level) {
        return (int) ((Math.pow(level, 2) + 3.0 * level) * LEVEL_FACTOR);
    }


    /**
     * Niveau correspondant à {@code totalXp} XP accumulés.
     */
    public static int computeLevelFromXp(int totalExperience) {
        return (int) ((-3 + Math.sqrt(9 + 4.0 * totalExperience / LEVEL_FACTOR)) / 2);
    }

    /**
     * Calcule l'expérience gagnée pour une partie.
     * = score_partie + GAME_BONUS [+ VICTORY_BONUS si victoire]
     */
    public static int computeXpEarned(int gameScore, GameResult result) {
        return gameScore + GAME_BONUS + (result == GameResult.WIN ? VICTORY_BONUS : 0);
    }


    public static Streak computeStreak(Streak streak, ProfileGame currentGame, ProfileGame previousGame) {
        GameResult previousResult = Optional.ofNullable(previousGame).map(ProfileGame::result).orElse(null);
        return computeStreak(streak, currentGame.result(), previousResult);
    }

    public static Streak computeStreak(Streak streak, GameResult currentResult, GameResult previousResult) {
        return switch (currentResult) {
            case WIN -> new ProfileStreak(
                    previousResult == GameResult.WIN ? streak.winStreak() + 1 : 1,
                    0,
                    0
            );
            case LOSS -> new ProfileStreak(
                    0,
                    previousResult == GameResult.LOSS ? streak.lossStreak() + 1 : 1,
                    0
            );
            case DRAW -> new ProfileStreak(
                    0,
                    0,
                    previousResult == GameResult.DRAW ? streak.drawStreak() + 1 : 1);
        };
    }
}```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/ProfileTopic.java
```java
package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Builder(toBuilder = true)
public record ProfileTopic(
        String topicId,
        int totalExperience,
        int level,
        int wins,
        int losses,
        int draws,
        List<ProfileGame> games,
        int winStreak,
        int drawStreak,
        int lossStreak,
        Instant createdAt,
        Instant updatedAt
) implements Statistics {

    @Override
    public int level() {
        return level;
    }

    public static ProfileTopic empty(String topicId, Instant now) {
        return new ProfileTopic(
                topicId,
                0,
                0,
                0,
                0,
                0,
                Collections.emptyList(),
                0,
                0,
                0,
                now,
                now
        );
    }
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/Statistics.java
```java
package io.github.quizup.profile.domain.model;

import java.util.List;

public interface Statistics extends Streak {

    default int level() {
        return ProfileRules.computeLevelFromXp(totalExperience());
    }

    default int experience() {
        return experienceInCurrentLevel();
    }

    int totalExperience();

    /**
     * XP totale requise pour ENTRER dans le niveau actuel.
     * Exemple : si level=5, retourne le seuil d'entrée du niveau 5.
     */
    default int experienceAtCurrentLevel() {
        return ProfileRules.computeXpForLevel(level());
    }

    /**
     * XP totale requise pour ENTRER dans le niveau suivant.
     * Exemple : si level=5, retourne le seuil d'entrée du niveau 6.
     */
    default int experienceAtNextLevel() {
        return ProfileRules.computeXpForLevel(level() + 1);
    }

    /**
     * XP gagnée dans le niveau actuel (pour la barre de progression UI).
     */
    default int experienceInCurrentLevel() {
        return totalExperience() - experienceAtCurrentLevel();
    }

    /**
     * XP totale nécessaire pour compléter le niveau actuel (dénominateur de la barre).
     */
    default int experienceRequiredToCompleteCurrentLevel() {
        return experienceAtNextLevel() - experienceAtCurrentLevel();
    }

    int wins();

    int losses();

    int draws();

    List<ProfileGame> games();

    default int totalGames() {
        return wins() + losses() + draws();
    }

    default int winPercentage() {
        if (totalGames() == 0) {
            return 0;
        }
        return (int) ((wins() * 100.0) / totalGames());
    }

    default int lossPercentage() {
        if (totalGames() == 0) {
            return 0;
        }
        return (int) ((losses() * 100.0) / totalGames());
    }

    default int drawPercentage() {
        if (totalGames() == 0) {
            return 0;
        }
        return (int) ((draws() * 100.0) / totalGames());
    }
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/Streak.java
```java
package io.github.quizup.profile.domain.model;

public interface Streak {

    int winStreak();

    int lossStreak();

    int drawStreak();
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/model/ProfileGame.java
```java
package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record ProfileGame(
        String gameId,
        String topicId,
        String opponentId,
        String opponentName,
        int playerScore,
        int opponentScore,
        GameResult result,
        Instant playedAt
) {
}
```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/command/ProfileCommand.java
```java
package io.github.quizup.profile.domain.command;

import io.github.quizup.profile.domain.model.GameResult;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;

public interface ProfileCommand {
    String profileId();

    record CreateProfileCommand(
            @TargetAggregateIdentifier String profileId
    ) implements ProfileCommand {
    }

    record AddGameResultCommand(
            @TargetAggregateIdentifier String profileId,
            String gameId,
            String topicId,
            String opponentId,
            String opponentName,
            int playerScore,
            int opponentScore,
            GameResult result,
            Instant playedAt
    ) implements ProfileCommand {
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/port/out/ProfileRepositoryPort.java
```java
package io.github.quizup.profile.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileTopic;

import java.util.Optional;

public interface ProfileRepositoryPort {

    void save(Profile profile);

    Optional<Profile> findById(String profileId);

    boolean existsById(String profileId);

    PageResult<Profile> findAll(SearchCriteria searchCriteria);

    PageResult<ProfileTopic> findTopicsByProfileId(String profileId, SearchCriteria searchCriteria);
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/port/in/AddGameResultUseCase.java
```java
package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;

import java.util.concurrent.CompletableFuture;

public interface AddGameResultUseCase {

    CompletableFuture<String> addGameResult(ProfileCommand.AddGameResultCommand command);
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/port/in/CreateProfileUseCase.java
```java
package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;

import java.util.concurrent.CompletableFuture;

public interface CreateProfileUseCase {

    CompletableFuture<String> create(ProfileCommand.CreateProfileCommand command);

    default CompletableFuture<String> create(String profileId) {
        return create(new ProfileCommand.CreateProfileCommand(profileId));
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/port/in/SearchProfileTopicsUseCase.java
```java
package io.github.quizup.profile.domain.port.in;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchProfileTopicsUseCase {

    CompletableFuture<PageResult<ProfileTopic>> searchTopics(ProfileQuery.SearchProfileTopicsQuery query);

    default CompletableFuture<PageResult<ProfileTopic>> searchTopics(String profileId,
                                                                      List<FilterCriteria> filters,
                                                                      List<SortCriteria> sorts,
                                                                      PageCriteria page) {
        return searchTopics(new ProfileQuery.SearchProfileTopicsQuery(profileId, filters, sorts, page));
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/port/in/GetProfileUseCase.java
```java
package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.concurrent.CompletableFuture;

public interface GetProfileUseCase {

    CompletableFuture<Profile> getById(ProfileQuery.GetProfileByIdQuery query) throws ProfileProblems.ProfileNotFoundProblem;

    default CompletableFuture<Profile> getById(String profileId) throws ProfileProblems.ProfileNotFoundProblem {
        return getById(new ProfileQuery.GetProfileByIdQuery(profileId));
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/port/in/SearchProfileUseCase.java
```java
package io.github.quizup.profile.domain.port.in;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchProfileUseCase {

    CompletableFuture<PageResult<Profile>> search(ProfileQuery.SearchProfileQuery query);

    default CompletableFuture<PageResult<Profile>> search(List<FilterCriteria> filters,
                                                          List<SortCriteria> sorts,
                                                          PageCriteria page) {
        return search(new ProfileQuery.SearchProfileQuery(filters, sorts, page));
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/query/ProfileQuery.java
```java
package io.github.quizup.profile.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;

import java.util.List;

public interface ProfileQuery {

	record GetProfileByIdQuery(String profileId) implements ProfileQuery {
	}

	record ProfileExistsByIdQuery(String profileId) implements ProfileQuery {
	}

	record SearchProfileQuery(
			List<FilterCriteria> filters,
			List<SortCriteria> sorts,
			PageCriteria page
	) implements ProfileQuery, SearchQuery {
	}

	record SearchProfileTopicsQuery(
			String profileId,
			List<FilterCriteria> filters,
			List<SortCriteria> sorts,
			PageCriteria page
	) implements ProfileQuery, SearchQuery {
	}
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/exception/ProfileProblems.java
```java
package io.github.quizup.profile.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;

import java.util.Map;

public interface ProfileProblems {

    class MissingProfileIdProblem extends ProfileProblem {
        public MissingProfileIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingProfileId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Profile ID required",
                    "A profileId is required to handle profile commands",
                    null);
        }
    }

    class ProfileNotFoundProblem extends ProfileProblem {
        public ProfileNotFoundProblem(String profileId) {
            super(profileId, "urn:quizup:profile:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Profile not found",
                    "The profile " + profileId + " was not found",
                    null);
        }
    }

    class MissingTopicIdProblem extends ProfileProblem {
        public MissingTopicIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingTopicId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Topic ID required",
                    "A topicId is required to record a match result on profile " + profileId,
                    null);
        }
    }

    class MissingGameResultProblem extends ProfileProblem {
        public MissingGameResultProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingGameResult",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game result required",
                    "A game result is required to update profile " + profileId,
                    null);
        }
    }

    class MissingGameIdProblem extends ProfileProblem {
        public MissingGameIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingGameId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game ID required",
                    "A gameId is required to record a match result on profile " + profileId,
                    null);
        }
    }

    class InvalidGameScoreProblem extends ProfileProblem {
        public InvalidGameScoreProblem(String profileId, int score) {
            super(profileId, "urn:quizup:profile:invalidGameScore",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Invalid game score",
                    "The score must be greater than or equal to 0",
                    Map.of("score", score));
        }
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/exception/ProfileProblem.java
```java
package io.github.quizup.profile.domain.exception;

import io.github.quizup.common.domain.exception.BaseProblem;
import io.github.quizup.common.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class ProfileProblem extends BaseProblem {

    private final String profileId;

    protected ProfileProblem(
            String profileId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(type, category, title, detail, mergeContext(context, profileId));
        this.profileId = profileId;
    }

    protected ProfileProblem(
            String profileId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(profileId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context);
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String profileId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("profileId", profileId);
        return merged;
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/event/ProfileTopicEvent.java
```java
package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.ProfileGame;

import java.time.Instant;

public interface ProfileTopicEvent extends ProfileEvent {

    String topicId();

    record ProfileTopicCreatedEvent(
            String profileId,
            String topicId,
            Instant createdAt
    ) implements ProfileTopicEvent {
    }

    record TopicExperienceIncreasedEvent(
            String profileId,
            String topicId,
            int experienceEarned,
            Instant earnedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLevelIncreasedEvent(
            String profileId,
            String topicId,
            int level,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicWinsIncreasedEvent(
            String profileId,
            String topicId,
            int wins,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLossesIncreasedEvent(
            String profileId,
            String topicId,
            int losses,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicDrawsIncreasedEvent(
            String profileId,
            String topicId,
            int draws,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicWinStreakIncreasedEvent(
            String profileId,
            String topicId,
            int winStreak,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicWinStreakEndedEvent(
            String profileId,
            String topicId,
            Instant endedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLossStreakIncreasedEvent(
            String profileId,
            String topicId,
            int lossStreak,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLossStreakEndedEvent(
            String profileId,
            String topicId,
            Instant endedAt
    ) implements ProfileTopicEvent {
    }

    record TopicDrawStreakIncreasedEvent(
            String profileId,
            String topicId,
            int drawStreak,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicDrawStreakEndedEvent(
            String profileId,
            String topicId,
            Instant endedAt
    ) implements ProfileTopicEvent {
    }

    record TopicGamePlayedEvent(
            String profileId,
            String topicId,
            ProfileGame game,
            Instant playedAt
    ) implements ProfileTopicEvent {

    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/event/ProfileEvent.java
```java
package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.ProfileGame;

import java.time.Instant;

public interface ProfileEvent {
    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            Instant createdAt
    ) implements ProfileEvent {
    }

    record ExperienceIncreasedEvent(
            String profileId,
            int experienceEarned,
            Instant earnedAt
    ) implements ProfileEvent {
    }

    record LevelIncreasedEvent(
            String profileId,
            int level,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record WinsIncreasedEvent(
            String profileId,
            int wins,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record LossesIncreasedEvent(
            String profileId,
            int losses,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record DrawsIncreasedEvent(
            String profileId,
            int draws,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record WinStreakIncreasedEvent(
            String profileId,
            int winStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record WinStreakEndedEvent(
            String profileId,
            Instant endedAt
    ) implements ProfileEvent {
    }

    record LossStreakIncreasedEvent(
            String profileId,
            int lossStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record LossStreakEndedEvent(
            String profileId,
            Instant endedAt
    ) implements ProfileEvent {
    }

    record DrawStreakIncreasedEvent(
            String profileId,
            int drawStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record DrawStreakEndedEvent(
            String profileId,
            Instant endedAt
    ) implements ProfileEvent {
    }

    record GamePlayedEvent(
            String profileId,
            ProfileGame game,
            Instant playedAt
    ) implements ProfileEvent {
    }

    // GameResultAddedEvent supprimé — remplacé par des événements granulaires (event sourcing pur)

    record BadgeUnlockedEvent(
            String profileId,
            Badge badge,
            Instant unlockedAt
    ) implements ProfileEvent {
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/ProfileServiceApplication.java
```java
package io.github.quizup.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
    }
}
```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/handler/query/ProfileQueryHandler.java
```java
package io.github.quizup.profile.application.handler.query;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class ProfileQueryHandler {

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileQueryHandler(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    @QueryHandler
    public Profile handle(ProfileQuery.GetProfileByIdQuery query) {
        return profileRepositoryPort.findById(query.profileId())
                .orElseThrow(() -> new ProfileProblems.ProfileNotFoundProblem(query.profileId()));
    }

    @QueryHandler
    public boolean handle(ProfileQuery.ProfileExistsByIdQuery query) {
        return profileRepositoryPort.existsById(query.profileId());
    }

    @QueryHandler
    public PageResult<Profile> handle(ProfileQuery.SearchProfileQuery query) {
        return profileRepositoryPort.findAll(query);
    }

    @QueryHandler
    public PageResult<ProfileTopic> handle(ProfileQuery.SearchProfileTopicsQuery query) {
        if (!profileRepositoryPort.existsById(query.profileId())) {
            throw new ProfileProblems.ProfileNotFoundProblem(query.profileId());
        }
        return profileRepositoryPort.findTopicsByProfileId(query.profileId(), query);
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/saga/ProfileUserSaga.java
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
public class ProfileUserSaga {

    private static final Logger logger = LoggerFactory.getLogger(ProfileUserSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Getter
    @Setter
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

        commandGateway.send(
                new ProfileCommand.CreateProfileCommand(
                        event.userId()
                )
        );
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/saga/ProfileGameSaga.java
```java
package io.github.quizup.profile.application.saga;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.GameResult;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
public class ProfileGameSaga {

    private static final Logger logger = LoggerFactory.getLogger(ProfileGameSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Getter
    @Setter
    private String gameId;

    @StartSaga
    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameEndedEvent event) {
        this.gameId = event.gameId();
        logger.info("GameEndedProfileSaga processing game results: gameId={}", event.gameId());

        commandGateway.send(
                new ProfileCommand.AddGameResultCommand(
                        event.player1Id(),
                        event.gameId(),
                        event.topicId(),
                        event.player2Id(),
                        event.player2Name(),
                        event.player1FinalScore(),
                        event.player2FinalScore(),
                        determineResult(event.player1Id(), event.winnerId()),
                        event.endedAt()
                )
        );

        if (!QuizUpConstants.BOT_USER_ID.equals(event.player2Id())) {
            commandGateway.send(
                    new ProfileCommand.AddGameResultCommand(
                            event.player2Id(),
                            event.gameId(),
                            event.topicId(),
                            event.player1Id(),
                            event.player1Name(),
                            event.player2FinalScore(),
                            event.player1FinalScore(),
                            determineResult(event.player2Id(), event.winnerId()),
                            event.endedAt()
                    )
            );
        }
    }

    private GameResult determineResult(String playerId, String winnerId) {
        if (winnerId == null) {
            return GameResult.DRAW;
        }
        return winnerId.equals(playerId) ? GameResult.WIN : GameResult.LOSS;
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/service/ProfileQueryService.java
```java
package io.github.quizup.profile.application.service;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.axon.PageResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileTopicsUseCase;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProfileQueryService implements GetProfileUseCase, SearchProfileUseCase, SearchProfileTopicsUseCase {

    private final QueryGateway queryGateway;

    public ProfileQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Profile> getById(ProfileQuery.GetProfileByIdQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Profile.class));
    }

    @Override
    public CompletableFuture<PageResult<Profile>> search(ProfileQuery.SearchProfileQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(Profile.class));
    }

    @Override
    public CompletableFuture<PageResult<ProfileTopic>> searchTopics(ProfileQuery.SearchProfileTopicsQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(ProfileTopic.class));
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/service/ProfileCommandService.java
```java
package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.port.in.AddGameResultUseCase;
import io.github.quizup.profile.domain.port.in.CreateProfileUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProfileCommandService implements CreateProfileUseCase, AddGameResultUseCase {

    private final CommandGateway commandGateway;

    public ProfileCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public CompletableFuture<String> create(ProfileCommand.CreateProfileCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> addGameResult(ProfileCommand.AddGameResultCommand command) {
        return commandGateway.send(command);
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/application/projection/ProfileProjection.java
```java
package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.event.ProfileTopicEvent;
import io.github.quizup.profile.domain.model.*;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProfileProjection {

    private static final Logger logger = LoggerFactory.getLogger(ProfileProjection.class);

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileProjection(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    // ── Profile events ──────────────────────────────────────────────────────

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        logger.debug("Projecting ProfileCreatedEvent: profileId={}", event.profileId());
        profileRepositoryPort.save(Profile.empty(event.profileId(), event.createdAt()));
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ExperienceIncreasedEvent event) {
        logger.debug("Projecting ExperienceIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .totalExperience(profile.totalExperience() + event.experienceEarned())
                        .updatedAt(event.earnedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LevelIncreasedEvent event) {
        logger.debug("Projecting LevelIncreasedEvent: profileId={}, level={}", event.profileId(), event.level());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .level(event.level())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.WinsIncreasedEvent event) {
        logger.debug("Projecting WinsIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .wins(event.wins())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LossesIncreasedEvent event) {
        logger.debug("Projecting LossesIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .losses(event.losses())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.DrawsIncreasedEvent event) {
        logger.debug("Projecting DrawsIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .draws(event.draws())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.WinStreakIncreasedEvent event) {
        logger.debug("Projecting WinStreakIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .winStreak(event.winStreak())
                        .lossStreak(0)
                        .drawStreak(0)
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.WinStreakEndedEvent event) {
        logger.debug("Projecting WinStreakEndedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .winStreak(0)
                        .updatedAt(event.endedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LossStreakIncreasedEvent event) {
        logger.debug("Projecting LossStreakIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .lossStreak(event.lossStreak())
                        .winStreak(0)
                        .drawStreak(0)
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LossStreakEndedEvent event) {
        logger.debug("Projecting LossStreakEndedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .lossStreak(0)
                        .updatedAt(event.endedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.DrawStreakIncreasedEvent event) {
        logger.debug("Projecting DrawStreakIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .drawStreak(event.drawStreak())
                        .winStreak(0)
                        .lossStreak(0)
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.DrawStreakEndedEvent event) {
        logger.debug("Projecting DrawStreakEndedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .drawStreak(0)
                        .updatedAt(event.endedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.GamePlayedEvent event) {
        logger.debug("Projecting GamePlayedEvent: profileId={}, gameId={}", event.profileId(), event.game().gameId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile -> {
            List<ProfileGame> updatedGames = new ArrayList<>(profile.games());
            updatedGames.add(event.game());
            if (updatedGames.size() > ProfileRules.MAX_RECENT_GAMES) {
                updatedGames.removeFirst();
            }
            profileRepositoryPort.save(profile.toBuilder()
                    .games(updatedGames)
                    .updatedAt(event.playedAt())
                    .build());
        });
    }

    // ── Topic events ────────────────────────────────────────────────────────

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.ProfileTopicCreatedEvent event) {
        logger.debug("Projecting ProfileTopicCreatedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile -> {
            Map<String, ProfileTopic> updatedTopics = new HashMap<>(profile.topics());
            updatedTopics.put(event.topicId(), ProfileTopic.empty(event.topicId(), event.createdAt()));
            profileRepositoryPort.save(profile.toBuilder().topics(updatedTopics).build());
        });
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicExperienceIncreasedEvent event) {
        logger.debug("Projecting TopicExperienceIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .totalExperience(topic.totalExperience() + event.experienceEarned())
                .updatedAt(event.earnedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLevelIncreasedEvent event) {
        logger.debug("Projecting TopicLevelIncreasedEvent: profileId={}, topicId={}, level={}", event.profileId(), event.topicId(), event.level());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .level(event.level())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicWinsIncreasedEvent event) {
        logger.debug("Projecting TopicWinsIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .wins(event.wins())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLossesIncreasedEvent event) {
        logger.debug("Projecting TopicLossesIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .losses(event.losses())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicDrawsIncreasedEvent event) {
        logger.debug("Projecting TopicDrawsIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .draws(event.draws())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicWinStreakIncreasedEvent event) {
        logger.debug("Projecting TopicWinStreakIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .winStreak(event.winStreak())
                .lossStreak(0)
                .drawStreak(0)
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicWinStreakEndedEvent event) {
        logger.debug("Projecting TopicWinStreakEndedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .winStreak(0)
                .updatedAt(event.endedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLossStreakIncreasedEvent event) {
        logger.debug("Projecting TopicLossStreakIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .lossStreak(event.lossStreak())
                .winStreak(0)
                .drawStreak(0)
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLossStreakEndedEvent event) {
        logger.debug("Projecting TopicLossStreakEndedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .lossStreak(0)
                .updatedAt(event.endedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicDrawStreakIncreasedEvent event) {
        logger.debug("Projecting TopicDrawStreakIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .drawStreak(event.drawStreak())
                .winStreak(0)
                .lossStreak(0)
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicDrawStreakEndedEvent event) {
        logger.debug("Projecting TopicDrawStreakEndedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .drawStreak(0)
                .updatedAt(event.endedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicGamePlayedEvent event) {
        logger.debug("Projecting TopicGamePlayedEvent: profileId={}, topicId={}, gameId={}", event.profileId(), event.topicId(), event.game().gameId());
        updateTopic(event.profileId(), event.topicId(), topic -> {
            List<ProfileGame> updatedGames = new ArrayList<>(topic.games());
            updatedGames.add(event.game());
            if (updatedGames.size() > ProfileRules.MAX_RECENT_GAMES) {
                updatedGames.removeFirst();
            }
            return topic.toBuilder()
                    .games(updatedGames)
                    .updatedAt(event.playedAt())
                    .build();
        });
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private void updateTopic(String profileId, String topicId,
                             java.util.function.UnaryOperator<ProfileTopic> updater) {
        profileRepositoryPort.findById(profileId).ifPresent(profile -> {
            ProfileTopic existing = profile.topics().getOrDefault(topicId, ProfileTopic.empty(topicId, java.time.Instant.now()));
            Map<String, ProfileTopic> updatedTopics = new HashMap<>(profile.topics());
            updatedTopics.put(topicId, updater.apply(existing));
            profileRepositoryPort.save(profile.toBuilder().topics(updatedTopics).build());
        });
    }
}
```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/out/persistence/repository/ProfileJpaRepository.java
```java
package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, String>, JpaSpecificationExecutor<ProfileEntity> {
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/out/persistence/entity/ProfileEntity.java
```java
package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.common.domain.model.search.FieldType;
import io.github.quizup.common.domain.model.search.Searchable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

@Setter
@Getter
@Entity
@Table(name = "profile_entry", indexes = {
        @Index(name = "idx_profile_total_xp", columnList = "total_experience"),
        @Index(name = "idx_profile_level", columnList = "level"),
        @Index(name = "idx_profile_wins", columnList = "wins")
})
public class ProfileEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "profile_id", nullable = false, length = 255)
    private String profileId;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "total_experience", nullable = false)
    private int totalExperience;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "level", nullable = false)
    private int level;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "wins", nullable = false)
    private int wins;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "losses", nullable = false)
    private int losses;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "draws", nullable = false)
    private int draws;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "win_streak", nullable = false)
    private int winStreak;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "loss_streak", nullable = false)
    private int lossStreak;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "draw_streak", nullable = false)
    private int drawStreak;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_topic_statistics_entry", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "topic_id")
    private Map<String, TopicStatisticsEmbeddable> topics = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_game_entry", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "position_idx")
    private List<GameResultEmbeddable> games = new ArrayList<>();

    @Searchable(type = FieldType.DATE)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Searchable(type = FieldType.DATE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/out/persistence/entity/TopicStatisticsEmbeddable.java
```java
package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Embeddable
public class TopicStatisticsEmbeddable {

    @Column(name = "total_experience", nullable = false)
    private int totalExperience;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "draws", nullable = false)
    private int draws;

    @Column(name = "win_streak", nullable = false)
    private int winStreak;

    @Column(name = "loss_streak", nullable = false)
    private int lossStreak;

    @Column(name = "draw_streak", nullable = false)
    private int drawStreak;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/out/persistence/entity/GameResultEmbeddable.java
```java
package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.profile.domain.model.GameResult;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Embeddable
public class GameResultEmbeddable {

    @Column(name = "game_id", nullable = false, length = 255)
    private String gameId;

    @Column(name = "topic_id", nullable = false, length = 255)
    private String topicId;

    @Column(name = "opponent_id", nullable = false, length = 255)
    private String opponentId;

    @Column(name = "opponent_name", length = 255)
    private String opponentName;

    @Column(name = "player_score", nullable = false)
    private int playerScore;

    @Column(name = "opponent_score", nullable = false)
    private int opponentScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 20)
    private GameResult result;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/out/persistence/mapper/ProfileEntityMapper.java
```java
package io.github.quizup.profile.infrastructure.out.persistence.mapper;

import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileGame;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.infrastructure.out.persistence.entity.GameResultEmbeddable;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.TopicStatisticsEmbeddable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProfileEntityMapper {

    private ProfileEntityMapper() {
    }

    public static Profile toDomain(ProfileEntity entity) {
        Map<String, ProfileTopic> topics = new HashMap<>();
        entity.getTopics().forEach((topicId, value) ->
                topics.put(
                        topicId,
                        new ProfileTopic(
                                topicId,
                                value.getTotalExperience(),
                                value.getLevel(),
                                value.getWins(),
                                value.getLosses(),
                                value.getDraws(),
                                new ArrayList<>(),
                                value.getWinStreak(),
                                value.getDrawStreak(),
                                value.getLossStreak(),
                                value.getCreatedAt(),
                                value.getUpdatedAt()
                        )
                )
        );

        List<ProfileGame> games = entity.getGames().stream()
                .map(ProfileEntityMapper::toDomain)
                .toList();

        return new Profile(
                entity.getProfileId(),
                entity.getTotalExperience(),
                entity.getLevel(),
                entity.getWins(),
                entity.getLosses(),
                entity.getDraws(),
                entity.getWinStreak(),
                entity.getLossStreak(),
                entity.getDrawStreak(),
                topics,
                games,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ProfileEntity toEntity(Profile profile) {
        ProfileEntity entity = new ProfileEntity();
        entity.setProfileId(profile.profileId());
        entity.setTotalExperience(profile.totalExperience());
        entity.setLevel(profile.level());
        entity.setWins(profile.wins());
        entity.setLosses(profile.losses());
        entity.setDraws(profile.draws());
        entity.setWinStreak(profile.winStreak());
        entity.setLossStreak(profile.lossStreak());
        entity.setDrawStreak(profile.drawStreak());

        Map<String, TopicStatisticsEmbeddable> topics = new HashMap<>();
        profile.topics().forEach((topicId, stats) -> {
            TopicStatisticsEmbeddable embeddable = new TopicStatisticsEmbeddable();
            embeddable.setTotalExperience(stats.totalExperience());
            embeddable.setLevel(stats.level());
            embeddable.setWins(stats.wins());
            embeddable.setLosses(stats.losses());
            embeddable.setDraws(stats.draws());
            embeddable.setWinStreak(stats.winStreak());
            embeddable.setLossStreak(stats.lossStreak());
            embeddable.setDrawStreak(stats.drawStreak());
            embeddable.setCreatedAt(stats.createdAt());
            embeddable.setUpdatedAt(stats.updatedAt());
            topics.put(topicId, embeddable);
        });
        entity.setTopics(topics);

        List<GameResultEmbeddable> games = profile.games().stream()
                .map(ProfileEntityMapper::toEntity)
                .toList();
        entity.setGames(games);

        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());
        return entity;
    }

    private static ProfileGame toDomain(GameResultEmbeddable embeddable) {
        return new ProfileGame(
                embeddable.getGameId(),
                embeddable.getTopicId(),
                embeddable.getOpponentId(),
                embeddable.getOpponentName(),
                embeddable.getPlayerScore(),
                embeddable.getOpponentScore(),
                embeddable.getResult(),
                embeddable.getPlayedAt()
        );
    }

    private static GameResultEmbeddable toEntity(ProfileGame gameResult) {
        GameResultEmbeddable embeddable = new GameResultEmbeddable();
        embeddable.setGameId(gameResult.gameId());
        embeddable.setTopicId(gameResult.topicId());
        embeddable.setOpponentId(gameResult.opponentId());
        embeddable.setOpponentName(gameResult.opponentName());
        embeddable.setPlayerScore(gameResult.playerScore());
        embeddable.setOpponentScore(gameResult.opponentScore());
        embeddable.setResult(gameResult.result());
        embeddable.setPlayedAt(gameResult.playedAt());
        return embeddable;
    }
}
```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/out/persistence/adapter/ProfileRepositoryAdapter.java
```java
package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.model.search.SortDirection;
import io.github.quizup.common.domain.model.search.DefaultPageResult;
import io.github.quizup.common.domain.model.search.DefaultSortCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ProfileEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProfileJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

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

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProfileTopic> findTopicsByProfileId(String profileId, SearchCriteria searchCriteria) {
        Optional<Profile> maybeProfile = findById(profileId);
        if (maybeProfile.isEmpty()) {
            return PageResult.unpaged();
        }

        int pageNumber = Optional.ofNullable(searchCriteria)
                .map(SearchCriteria::page)
                .map(p -> p.number() == null ? 0 : p.number())
                .orElse(0);
        int pageSize = Optional.ofNullable(searchCriteria)
                .map(SearchCriteria::page)
                .map(p -> p.size() == null ? 20 : p.size())
                .orElse(20);

        List<ProfileTopic> topics = maybeProfile.get().topics().values().stream().toList();
        if (topics.isEmpty()) {
            return new DefaultPageResult<>(
                    Collections.emptyList(),
                    pageNumber,
                    pageSize,
                    0,
                    0,
                    Collections.emptyList(),
                    true,
                    true,
                    true
            );
        }

        List<SortCriteria> sorts = Optional.ofNullable(searchCriteria)
                .map(SearchCriteria::sorts)
                .orElse(Collections.emptyList());
        if (sorts.isEmpty()) {
            sorts = List.of(
                    new DefaultSortCriteria("totalExperience", SortDirection.DESC),
                    new DefaultSortCriteria("wins", SortDirection.DESC)
            );
        }

        Comparator<ProfileTopic> comparator = toComparator(sorts.getFirst());
        for (int i = 1; i < sorts.size(); i++) {
            comparator = comparator.thenComparing(toComparator(sorts.get(i)));
        }

        List<ProfileTopic> sorted = topics.stream().sorted(comparator).toList();


        if (pageSize <= 0) {
            pageSize = sorted.size();
        }

        int fromIndex = Math.max(0, pageNumber * pageSize);
        int toIndex = Math.min(sorted.size(), fromIndex + pageSize);
        List<ProfileTopic> content = fromIndex >= sorted.size()
                ? Collections.emptyList()
                : sorted.subList(fromIndex, toIndex);

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) sorted.size() / pageSize);
        return new DefaultPageResult<>(
                content,
                pageNumber,
                pageSize,
                sorted.size(),
                totalPages,
                sorts,
                pageNumber == 0,
                pageNumber >= Math.max(0, totalPages - 1),
                content.isEmpty()
        );
    }

    private Comparator<ProfileTopic> toComparator(SortCriteria sort) {
        Comparator<ProfileTopic> comparator = switch (sort.property()) {
            case "topicId" -> Comparator.comparing(ProfileTopic::topicId, Comparator.nullsLast(String::compareTo));
            case "wins" -> Comparator.comparingInt(ProfileTopic::wins);
            case "losses" -> Comparator.comparingInt(ProfileTopic::losses);
            case "draws" -> Comparator.comparingInt(ProfileTopic::draws);
            case "totalExperience" -> Comparator.comparingInt(ProfileTopic::totalExperience);
            default -> Comparator.comparingInt(ProfileTopic::totalExperience);
        };

        return sort.direction() == SortDirection.ASC ? comparator : comparator.reversed();
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/in/api/ProfileController.java
```java
package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.domain.model.search.DefaultPageCriteria;
import io.github.quizup.common.domain.model.search.DefaultSortCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.model.search.SortDirection;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileTopicsUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.ProfileResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileTopicResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(ProfileController.ENDPOINT)
public class ProfileController {

    public static final String ENDPOINT = "/api/profiles";

    private final GetProfileUseCase getProfileUseCase;
    private final SearchProfileUseCase searchProfileUseCase;
    private final SearchProfileTopicsUseCase searchProfileTopicsUseCase;

    public ProfileController(GetProfileUseCase getProfileUseCase,
                             SearchProfileUseCase searchProfileUseCase,
                             SearchProfileTopicsUseCase searchProfileTopicsUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.searchProfileUseCase = searchProfileUseCase;
        this.searchProfileTopicsUseCase = searchProfileTopicsUseCase;
    }

    @GetMapping("/{profileId}")
    public CompletableFuture<ResponseEntity<ProfileResponse>> getById(@PathVariable String profileId) {
        return getProfileUseCase.getById(profileId)
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<ProfileResponse>>> search(@RequestBody SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchProfileUseCase.search(criteria.filters(), criteria.sorts(), criteria.page())
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{profileId}/topics")
    public CompletableFuture<ResponseEntity<PageResponse<ProfileTopicResponse>>> searchTopics(
            @PathVariable String profileId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction
    ) {
        PageCriteria pageCriteria = new DefaultPageCriteria(size, page);
        List<SortCriteria> sorts = buildSorts(sort, direction);

        return searchProfileTopicsUseCase.searchTopics(profileId, Collections.emptyList(), sorts, pageCriteria)
                .thenApply(ProfileResponseMapper::toTopicResponse)
                .thenApply(ResponseEntity::ok);
    }

    private List<SortCriteria> buildSorts(List<String> properties, SortDirection direction) {
        if (properties == null || properties.isEmpty()) {
            return List.of(
                    new DefaultSortCriteria("totalExperience", SortDirection.DESC),
                    new DefaultSortCriteria("wins", SortDirection.DESC)
            );
        }

        return properties.stream()
                .map(property -> new DefaultSortCriteria(property, direction))
                .map(SortCriteria.class::cast)
                .toList();
    }
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/in/api/response/ProfileStatisticsResponse.java
```java
package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;

public record ProfileStatisticsResponse(
        int totalExperience,
        int level,
        int experience,
        int experienceAtCurrentLevel,
        int experienceAtNextLevel,
        int experienceRequiredToCompleteCurrentLevel,
        int wins,
        int losses,
        int draws,
        int totalGames,
        int winPercentage,
        int lossPercentage,
        int drawPercentage
) implements Serializable {
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/in/api/response/GameResultResponse.java
```java
package io.github.quizup.profile.infrastructure.in.api.response;

import io.github.quizup.profile.domain.model.GameResult;

import java.io.Serializable;
import java.time.Instant;

public record GameResultResponse(
        String gameId,
        String topicId,
        String opponentId,
        String opponentName,
        int playerScore,
        int opponentScore,
        GameResult result,
        Instant playedAt
) implements Serializable {
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/in/api/response/ProfileResponse.java
```java
package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record ProfileResponse(
        String profileId,
        int totalExperience,
        int level,
        int experience,
        int experienceAtCurrentLevel,
        int experienceAtNextLevel,
        int experienceRequiredToCompleteCurrentLevel,
        int wins,
        int losses,
        int draws,
        int totalGames,
        int winPercentage,
        int lossPercentage,
        int drawPercentage,
        int winStreak,
        int lossStreak,
        int drawStreak,
        List<GameResultResponse> games,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/in/api/response/ProfileTopicResponse.java
```java
package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;

public record ProfileTopicResponse(
        String topicId,
        int totalExperience,
        int level,
        int experience,
        int experienceAtCurrentLevel,
        int experienceAtNextLevel,
        int experienceRequiredToCompleteCurrentLevel,
        int wins,
        int losses,
        int draws,
        int totalGames,
        int winPercentage,
        int lossPercentage,
        int drawPercentage,
        int winStreak,
        int lossStreak,
        int drawStreak
) implements Serializable {
}

```

### ./quizup-profile-infrastructure/src/main/java/io/github/quizup/profile/infrastructure/in/api/mapper/ProfileResponseMapper.java
```java
package io.github.quizup.profile.infrastructure.in.api.mapper;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileGame;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.model.Statistics;
import io.github.quizup.profile.infrastructure.in.api.response.GameResultResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileStatisticsResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileTopicResponse;

import java.util.Comparator;

public final class ProfileResponseMapper {

    private ProfileResponseMapper() {
    }

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.profileId(),
                profile.totalExperience(),
                profile.level(),
                profile.experience(),
                profile.experienceAtCurrentLevel(),
                profile.experienceAtNextLevel(),
                profile.experienceRequiredToCompleteCurrentLevel(),
                profile.wins(),
                profile.losses(),
                profile.draws(),
                profile.totalGames(),
                profile.winPercentage(),
                profile.lossPercentage(),
                profile.drawPercentage(),
                profile.winStreak(),
                profile.lossStreak(),
                profile.drawStreak(),
                profile.games().stream()
                        .sorted(Comparator.comparing(ProfileGame::playedAt).reversed())
                        .map(ProfileResponseMapper::toResponse)
                        .toList(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    public static PageResponse<ProfileResponse> toResponse(PageResult<Profile> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, ProfileResponseMapper::toResponse);
    }

    public static PageResponse<ProfileTopicResponse> toTopicResponse(PageResult<ProfileTopic> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, ProfileResponseMapper::toTopicResponse);
    }

    private static ProfileStatisticsResponse toStatisticsResponse(Statistics statistics) {
        return new ProfileStatisticsResponse(
                statistics.totalExperience(),
                statistics.level(),
                statistics.experience(),
                statistics.experienceAtCurrentLevel(),
                statistics.experienceAtNextLevel(),
                statistics.experienceRequiredToCompleteCurrentLevel(),
                statistics.wins(),
                statistics.losses(),
                statistics.draws(),
                statistics.totalGames(),
                statistics.winPercentage(),
                statistics.lossPercentage(),
                statistics.drawPercentage()
        );
    }

    private static GameResultResponse toResponse(ProfileGame gameResult) {
        return new GameResultResponse(
                gameResult.gameId(),
                gameResult.topicId(),
                gameResult.opponentId(),
                gameResult.opponentName(),
                gameResult.playerScore(),
                gameResult.opponentScore(),
                gameResult.result(),
                gameResult.playedAt()
        );
    }

    private static ProfileTopicResponse toTopicResponse(ProfileTopic topic) {
        return new ProfileTopicResponse(
                topic.topicId(),
                topic.totalExperience(),
                topic.level(),
                topic.experience(),
                topic.experienceAtCurrentLevel(),
                topic.experienceAtNextLevel(),
                topic.experienceRequiredToCompleteCurrentLevel(),
                topic.wins(),
                topic.losses(),
                topic.draws(),
                topic.totalGames(),
                topic.winPercentage(),
                topic.lossPercentage(),
                topic.drawPercentage(),
                topic.winStreak(),
                topic.lossStreak(),
                topic.drawStreak()
        );
    }
}

```

