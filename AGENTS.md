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
`ProfileQuery.ProfileExistsByIdQuery` / `ProfileQuery.GetProfileQuery` sur le bus partagé).
Le `NameGenerator` (nom d'affichage déterministe généré à la création) vit dans ce domaine.

Il porte enfin la **progression du joueur** (RPG) : XP totale + XP **par thème**, niveau, titre
honorifique et badges, attribués de façon idempotente à la fin de chaque duel (saga sur
`GameEndedEvent` de `quizup-game`). L'agrégat `PlayerProgressAggregate` est identifié par
`progress:<userId>` (namespacé pour ne pas entrer en collision avec le `ProfileAggregate`).

Il expose aussi la **présence joueur** (`/api/presence`) : read-model éphémère piloté par le cycle
de vie des **sessions temps réel STOMP** (connexion/déconnexion), utilisé pour les pastilles
« en ligne » et le forfait des duels. Plus de battement de cœur ni de polling.

**Package** : `io.github.quizup.profile`

**Avatar** : le profil porte `avatarOptions` (JSON des options DiceBear, style `micah`) choisi par
le propriétaire. **Aucun stockage d'image** : l'avatar est régénéré côté client de façon déterministe
à partir de ces options (repli sur `userId` si `null`).

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
## 3. Use cases (ports entrants — `domain/port/in/`)

- `GetProfileUseCase` — récupération par userId (404 si inconnu)
- `UpdateProfileUseCase` — mise à jour (propriétaire uniquement)
- `CreateProfileUseCase` — création (utilisée par le seeding système ; en nominal par la saga)
- `CheckProfileUseCase` — vérification d'existence (`existsById`)
- `SearchProfileUseCase` — recherche paginée (pattern SDK `SearchRequest` → `SearchResponse`)
- `GetProgressionUseCase` — progression globale et par thème

**Queries** (`domain/query/ProfileQuery.java`) : `GetProfileQuery` (consommée par
social/matchmaking pour le nom d'affichage), `ProfileExistsByIdQuery` (consommée par social),
`ProfileSearchQuery`.
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

**Fourniture (sortant)** : les queries `ProfileQuery.GetProfileQuery` /
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
