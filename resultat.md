### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/aggregate/ProfileAggregate.java
```java
package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.GameResult;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Getter
@Aggregate
public class ProfileAggregate {

    @AggregateIdentifier
    private String profileId;

    private String userId;

    private int wins;
    private int losses;
    private int draws;

    private int gamesPlayed;
    private double winLossRatio;

    private final Map<String, ProfileTopicProgression> topicProgressions = new HashMap<>();

    protected ProfileAggregate() {
    }

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        if (StringUtils.isBlank(command.userId())) {
            throw new ProfileProblems.MissingUserIdProblem(command.profileId());
        }

        apply(
                new ProfileEvent.ProfileCreatedEvent(
                        command.profileId(),
                        command.userId(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.RecordGameResultCommand command) {
        if (StringUtils.isBlank(command.topicId())) {
            throw new ProfileProblems.MissingTopicIdProblem(profileId);
        }

        if (command.result() == null) {
            throw new ProfileProblems.MissingMatchResultProblem(profileId);
        }

        ProfileStatsSnapshot nextStats = computeNextGlobalStats(command.result());

        apply(
                new ProfileEvent.ProfileMatchResultRecordedEvent(
                        profileId,
                        command.topicId(),
                        command.result(),
                        nextStats.gamesPlayed(),
                        nextStats.wins(),
                        nextStats.losses(),
                        nextStats.draws(),
                        nextStats.winLossRatio(),
                        Instant.now()
                )
        );
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.userId = event.userId();
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winLossRatio = 0.0d;
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileMatchResultRecordedEvent event) {
        this.gamesPlayed = event.totalGamesPlayed();
        this.wins = event.totalWins();
        this.losses = event.totalLosses();
        this.draws = event.totalDraws();
        this.winLossRatio = event.winLossRatio();

        ProfileTopicProgression progression = topicProgressions.computeIfAbsent(
                event.topicId(),
                ProfileTopicProgression::new
        );
        progression.record(event.result());
    }

    private ProfileStatsSnapshot computeNextGlobalStats(GameResult result) {
        int nextGamesPlayed = gamesPlayed + 1;
        int nextWins = wins;
        int nextLosses = losses;
        int nextDraws = draws;

        switch (result) {
            case WIN -> nextWins++;
            case LOSS -> nextLosses++;
            case DRAW -> nextDraws++;
        }

        double nextRatio = calculateWinLossRatio(nextWins, nextLosses);
        return new ProfileStatsSnapshot(nextGamesPlayed, nextWins, nextLosses, nextDraws, nextRatio);
    }

    private static double calculateWinLossRatio(int wins, int losses) {
        if (losses == 0) {
            return wins;
        }
        return (double) wins / losses;
    }

    private record ProfileStatsSnapshot(
            int gamesPlayed,
            int wins,
            int losses,
            int draws,
            double winLossRatio
    ) {
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/aggregate/ProfileTopicProgression.java
```java
package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.model.GameResult;
import lombok.Getter;

/**
 * Sous-agregat qui encapsule les stats d'un profil pour un topic donne.
 */
@Getter
public class ProfileTopicProgression {

    private final String topicId;

    private int experience;
    private int level;

    private int gamesPlayed;
    private int wins;
    private int losses;
    private int draws;
    private double winLossRatio;

    public ProfileTopicProgression(String topicId) {
        this.topicId = topicId;
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winLossRatio = 0.0d;
        this.experience = 0;
        this.level = 0;
    }

    public void record(GameResult result) {
        gamesPlayed++;
        switch (result) {
            case WIN -> wins++;
            case LOSS -> losses++;
            case DRAW -> draws++;
        }
        this.winLossRatio = calculateWinLossRatio(wins, losses);
    }

    private static double calculateWinLossRatio(int wins, int losses) {
        if (losses == 0) {
            return wins;
        }
        return (double) wins / losses;
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

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/command/ProfileCommand.java
```java
package io.github.quizup.profile.domain.command;

import io.github.quizup.profile.domain.model.GameResult;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface ProfileCommand {
    String profileId();

    record CreateProfileCommand(
            @TargetAggregateIdentifier String profileId,
            String userId
    ) implements ProfileCommand {
    }

    record RecordGameResultCommand(
            @TargetAggregateIdentifier String profileId,
            String topicId,
            String score,
            GameResult result
    ) implements ProfileCommand {
    }
}

```

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/exception/ProfileProblems.java
```java
package io.github.quizup.profile.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;

import java.util.Map;

public interface ProfileProblems {

    class MissingUserIdProblem extends ProfileProblem {
        public MissingUserIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingUserId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "User ID required",
                    "A userId is required to create profile " + profileId,
                    null);
        }
    }

    class MissingPseudonymProblem extends ProfileProblem {
        public MissingPseudonymProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingPseudonym",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Pseudonym required",
                    "A pseudonym is required to create profile " + profileId,
                    null);
        }
    }

    class InvalidPseudonymProblem extends ProfileProblem {
        public InvalidPseudonymProblem(String profileId, String pseudonym) {
            super(profileId, "urn:quizup:profile:invalidPseudonym",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Invalid pseudonym",
                    "Pseudonym must contain between 3 and 20 characters",
                    Map.of("pseudonym", pseudonym == null ? "" : pseudonym));
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

    class MissingMatchResultProblem extends ProfileProblem {
        public MissingMatchResultProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingMatchResult",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Match result required",
                    "A match result is required to update profile " + profileId,
                    null);
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

### ./quizup-profile-domain/src/main/java/io/github/quizup/profile/domain/event/ProfileEvent.java
```java
package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.GameResult;

import java.time.Instant;

public interface ProfileEvent {
    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            String userId,
            Instant createdAt
    ) implements ProfileEvent {
    }

    record ProfileMatchResultRecordedEvent(
            String profileId,
            String topicId,
            GameResult result,
            int totalGamesPlayed,
            int totalWins,
            int totalLosses,
            int totalDraws,
            double winLossRatio,
            Instant recordedAt
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

