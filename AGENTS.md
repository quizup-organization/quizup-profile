# AGENTS.md — quizup-profile

> Service **référence** des patterns hexagonaux QuizUp : agrégat + saga + projection.
> Architecture : Axon Framework (CQRS/EDA) + JPA (projection) + JWT Resource Server (SDK).
> Pour les règles de patterns :
> [`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

Propriétaire des **données modifiables** du profil utilisateur (`displayName`, `bio`, `country`).
Identity (`quizup-identity`) reste le IdP : identité immuable + JWT (lecture du profil côté
frontend = claims du JWT). Ce service est **créé par événement** (saga sur
`UserRegisteredEvent` d'identity) et mis à jour **uniquement par le propriétaire**.

Il est aussi la **source unique d'existence et de nom d'affichage** des utilisateurs pour les
autres services : social et matchmaking n'interrogent plus identity mais **profile** (queries
`ProfileQuery.ProfileExistsByIdQuery` / `ProfileQuery.FindProfileQuery` sur le bus partagé).
Le `NameGenerator` (nom d'affichage déterministe généré à la création) vit dans ce domaine.

Il porte enfin la **progression du joueur** (RPG) : XP totale + XP **par thème**, niveau, titre
honorifique et badges, attribués de façon idempotente à la fin de chaque duel (saga sur
`GameEndedEvent` de `quizup-game`). L'agrégat `PlayerProgressAggregate` est identifié par
`progress:<userId>` (namespacé pour ne pas entrer en collision avec le `ProfileAggregate`).

Il expose aussi la **présence joueur** (`/api/presence`) : read-model éphémère piloté par le cycle
de vie des **sessions temps réel STOMP** (connexion/déconnexion), utilisé pour les pastilles
« en ligne » et le forfait des duels. Plus de battement de cœur ni de polling.

**Package** : `io.github.quizup.profile`

**Avatar** : hors périmètre (pas d'infrastructure de stockage d'images — à trancher en ADR).

---

## 2. Endpoints REST

### `ProfileController` — `/api/profiles` (`@CrossOrigin`)

| Méthode  | Chemin                    | Handler                          | Response                     |
|----------|---------------------------|----------------------------------|------------------------------|
| GET      | `/api/profiles/{userId}`  | `getProfileById(String)`         | `ProfileResponse`            |
| PUT      | `/api/profiles/{userId}`  | `updateProfile(String, body)`    | `200 OK` (403 si non propriétaire) |
| POST     | `/api/profiles/search`    | `search(SearchRequest)`          | `PageResponse<ProfileResponse>` |

**DTO** : `ProfileResponse(userId, email, displayName, bio, country, Instant createdAt, Instant updatedAt)`

La recherche filtre sur les colonnes `@Searchable` de la projection (`displayName`, `email`).

### `ProgressionController` — `/api/profiles`

| Méthode | Chemin                                    | Handler                     | Response                    |
|---------|-------------------------------------------|-----------------------------|-----------------------------|
| GET     | `/api/profiles/{userId}/progress`         | `getProgress(String)`       | `ProgressionResponse`       |
| GET     | `/api/profiles/{userId}/progress/{topicId}` | `getTopicProgress(...)`   | `TopicProgressResponse`     |

**DTO** : `ProgressionResponse(userId, xpTotal, level, title, xpForNextLevel, badges[], topics[])`
et `TopicProgressResponse(topicId, xp, level)`. Un joueur sans duel retourne un niveau 1 / 0 XP.

### `PresenceController` — `/api/presence`

Présence joueur : **read-model éphémère, non event-sourcé** (tables `presence_entry` +
`presence_session`, pas d'agrégat Axon). La session STOMP est le signal : `SessionConnectedEvent`
bascule le joueur `ONLINE` ; la fermeture de sa dernière session programme, après
`PresenceRules.DISCONNECT_GRACE = 15 s`, une échéance Axon (`DeadlineManager`) qui le bascule
`OFFLINE` et publie `PlayerWentOfflineEvent` (consommé par game pour le forfait). Toute reconnexion
avant l'échéance annule le passage hors ligne. Les présences sont repassées `OFFLINE` et les
sessions purgées au démarrage (elles meurent avec l'instance). Notification temps réel sur STOMP
`/topic/presence/{userId}` (route gateway `profile-service-ws`). L'authentification de la trame
STOMP `CONNECT` est portée par le SDK (`StompAuthChannelInterceptor`).

| Méthode | Chemin                          | Handler                       | Response                          |
|---------|---------------------------------|-------------------------------|-----------------------------------|
| GET     | `/api/presence/{userId}`        | `get(String)`                 | `PresenceResponse`                |
| POST    | `/api/presence/search`          | `search(SearchRequest)`       | `PageResponse<PresenceResponse>`  |

**DTO** : `PresenceResponse(userId, PresenceStatus status, Instant lastSeenAt)`. La recherche est
**paginée standard** (`SearchRequest` → `PageResponse`, cf. pattern commun) : `PresenceEntity`
expose `@Searchable` sur `userId` (STRING) et `lastSeenAt` (DATE), chaîne
`PresenceQueryService` (`QueryGateway`) → `PresenceQueryHandler` (`@QueryHandler`) →
`PresenceRepositoryPort.findAll` (`JpaSearchAdapter`). Le batch se fait via un filtre
`userId IN [...]`. Le client web maintient une connexion STOMP persistante vers `profile` (le
`CONNECT` alimente la présence) ; il n'y a plus d'endpoint heartbeat.

### `ActivityController` — `/api/profiles`

Activité journalière (streak façon « contributions »), **read-model dérivé** des fins de partie
(tables `progression_activity` + `progression_activity_day`, pas d'agrégat). `ActivityProjection`
consomme `GameEvent.GameEndedEvent` (service `quizup-game`) : pour chaque joueur humain non-bot,
avance la série via `ActivityRules` (idempotent par jour) et incrémente le compteur du jour.
Le découpage du jour utilise `app.activity.zone` (défaut `Europe/Paris`). Les runs async « record »
(`GameRunRecordedEvent`, sans XP) ne comptent pas ; le replay compte.

| Méthode | Chemin                                | Handler                          | Response           |
|---------|---------------------------------------|----------------------------------|--------------------|
| GET     | `/api/profiles/{userId}/activity`     | `getActivity(userId, from, to)`  | `ActivityResponse` |

**DTO** : `ActivityResponse(userId, currentStreak, longestStreak, lastActiveDate, totalActiveDays, days[])`
où `days[]` = `ActivityDayResponse(date, games)`. Fenêtre par défaut : 365 derniers jours.

---

## 3. Use cases (ports entrants — `domain/port/in/`)

- `GetProfileUseCase` — récupération par userId (404 si inconnu)
- `UpdateProfileUseCase` — mise à jour (propriétaire uniquement)
- `CreateProfileUseCase` — création (utilisée par le seeding système ; en nominal par la saga)
- `CheckProfileUseCase` — vérification d'existence (`existsById`)
- `SearchProfileUseCase` — recherche paginée (pattern SDK `SearchRequest` → `PageResponse`)
- `GetProgressionUseCase` — progression globale et par thème

**Queries** (`domain/query/ProfileQuery.java`) : `GetProfileQuery`, `FindProfileQuery`
(lecture légère, retourne `Optional<Profile>` — c'est elle que consomment
social/matchmaking), `ProfileExistsByIdQuery` (consommée par social), `ProfileSearchQuery`.
**Queries progression** (`ProgressionQuery.java`) : `GetProgressionQuery`, `GetTopicProgressionQuery`.

**Ports sortants locaux** : `ProfileRepositoryPort` (persistance de la projection, dont
`existsById`), `ProgressionRepositoryPort` (projection de progression).

---

## 4. Dépendances inter-services

**Conso** : `quizup-identity-domain` (artifact Maven) — la saga
`CreateProfileSaga` (`@StartSaga @SagaEventHandler(associationProperty = "userId")`) consomme
`UserEvent.UserRegisteredEvent` via le bus Kafka partagé (transport Axon distributed). La saga est
**idempotente** (garde sur `ProfileRepositoryPort.findById` : pas de commande si le profil existe).

**Seeding système** : `ProfileDataSeeder` (`CommandLineRunner`, `app.seed-data.enabled`) garantit
les profils des utilisateurs système (`QuizUpConstants.SYSTEM_USER_ID`),
indépendamment de la saga : si `profile` n'était pas abonné au flux Kafka quand `identity` a publié
`UserRegisteredEvent`, ces profils seraient sinon manquants. Il passe par les use cases
`CheckProfileUseCase` + `CreateProfileUseCase` (pas de port direct) et est idempotent.

**Conso progression** : `quizup-game-domain` (artifact Maven) — la saga
`AwardProgressSaga` (`@SagaEventHandler(associationProperty = "gameId")`) consomme
`GameEvent.GameEndedEvent` et envoie un `ProgressionCommand.AwardXpCommand` par joueur humain
(idempotence par `gameId` portée par l'agrégat). Le compte système (`QuizUpConstants.SYSTEM_USER_ID`) est ignoré.

**Aucun QueryGateway sortant** : identity n'est jamais interrogé à l'exécution.

**Fourniture (sortant)** : les queries `ProfileQuery.FindProfileQuery` /
`ProfileExistsByIdQuery` sont consommées par `quizup-social` et `quizup-matchmaking` (bus
partagé) — ces services ne dépendent plus de `quizup-identity-domain`.

### Enrichissements progression

- `ProgressionResponse` porte `duelStats` (joués, victoires, défaites, winrate, meilleur score,
  meilleure série) et `TopicProgressResponse` porte `title` (titre par thème).
- Badges implémentés : `FIRST_WIN`, `PERFECT`, `LIGHTNING` (« Éclair », 5 bonnes réponses < 3 s),
  `STREAK_MASTER` (« Série de feu », 10 victoires consécutives dans un thème).
- `AwardXpCommand` / `XpAwardedEvent` portent `correctAnswers` + `fastAnswers` (issus de
  `GameEndedEvent`). Compteurs `duelStats` maintenus en projection.
- **Activité journalière** : `ActivityProjection` alimente `progression_activity` (série
  courante/record, dernier jour actif) et `progression_activity_day` (parties par jour) depuis
  `GameEndedEvent` ; exposée par `GET /api/profiles/{userId}/activity`.
