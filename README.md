# quizup-profile (staging local)

Structure cible en 2 sous-modules Maven :

- `quizup-profile-domain`
- `quizup-profile-infrastructure`

Seul `quizup-profile-domain` est importable inter-services.

Le code applicatif, lorsqu'il existe, reste organise sous `io.github.quizup.profile.application.*` dans le module infrastructure.

## V1 implementee

- Creation automatique du profil via `UserSaga` a partir de `UserRegisteredEvent` (`profileId = userId`).
- Endpoint unique `GET /api/profiles/{profileId}` avec payload complet pour l'UI:
  - streaks (win/loss/draw)
  - statistiques globales et par theme (avec metriques derivees: niveau, pourcentages, progression XP)
  - badges debloques
  - 10 derniers resultats de parties (avec `opponentName` et `topicName`)
- Projection des resultats de partie a partir de `GameEndedEvent`.
- Persistence JPA/Flyway des projections profil.

## Build local

```bash
cd services/quizup-identity
mvn -DskipTests install

cd services/quizup-theme
mvn -DskipTests install

cd services/quizup-profile
mvn -DskipTests install
```

