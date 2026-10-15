# AGENTS.md — quizup-profile

> Service **référence** des patterns hexagonaux QuizUp : agrégat + saga + projection.
> Architecture : Axon Framework (CQRS/EDA) + JPA (projection) + JWT Resource Server (SDK).
> Pour les règles de patterns :
> [`../../best-practices/.backend/folder-structure.md`](../../best-practices/.backend/folder-structure.md).

---

## 1. Rôle

Propriétaire des **données modifiables** du profil utilisateur (`pseudonym`, `bio`, `country`,
`avatarOptions`, `bannerUrl`, `language`).
Identity (`quizup-identity`) reste le IdP : identité immuable + JWT (lecture du profil côté
frontend = claims du JWT). Ce service est **créé par événement** (saga sur
`UserRegisteredEvent` d'identity) et mis à jour **uniquement par le propriétaire**, **champ par
champ** (une commande/événement/endpoint par champ ; no-op si la valeur est inchangée).

Il est aussi la **source unique d'existence et de pseudonyme** des utilisateurs pour les
autres services : social et matchmaking n'interrogent plus identity mais **profile** (queries
`ProfileQuery.ProfileExistsByIdQuery` / `ProfileQuery.GetProfileQuery` sur le bus partagé).
Le `PseudonymGenerator` (pseudonyme déterministe généré à la création) vit dans ce domaine.

Il porte enfin la **progression du joueur** (RPG) : XP totale + XP **par thème**, niveau et titre
honorifique (`PlayerProgressAggregate`, `progress:<userId>`) et **badges**
(`BadgeAggregate`, `badges:<userId>` — namespacés pour ne pas entrer en collision avec le
`ProfileAggregate`). Les deux sont attribués de façon idempotente à la fin de chaque duel (sagas
sur `GameEndedEvent` de `quizup-game`).

Il expose aussi la **présence joueur** (`/api/presence`) : read-model éphémère piloté par le cycle
de vie des **sessions temps réel STOMP** (connexion/déconnexion) et les **heartbeats** du broker
(le BFF ferme un client silencieux en ~30 s) — utilisé pour les pastilles « en ligne » et le
forfait des duels. Côté web, un onglet non visible est déconnecté proprement ; la grâce de 15 s
absorbe les bascules rapides.

**Passage hors ligne — TTL en base + balayeur** (pas de deadline Axon) :

- la fermeture de la dernière session arme `presence_entry.offline_deadline_at` (+15 s de grâce) ;
  un balayeur périodique (`PresenceOfflineSweeper`, 5 s) confirme la transition par un `UPDATE`
  conditionnel (idempotent, multi-instances) et publie `PlayerWentOfflineEvent` ;
- **bail de session** : chaque session porte `last_seen_at`, renouvelé par le BFF en batch
  (`RenewPresenceSessionsCommand`, toutes les 10 s). Une session sans renouvellement depuis
  `SESSION_LEASE_TTL` (30 s) est supprimée par le balayeur (crash d'instance, partition réseau),
  ce qui évite toute session fantôme même si le BFF disparaît sans déconnexion propre ;
- les sessions portent `instance_id` : au démarrage, le BFF purge en plus ses propres sessions
  antérieures à son boot (`ResetInstanceSessionsCommand`) et déconnecte ses sessions locales à
  l'arrêt (`@PreDestroy`). Les commandes de présence du BFF sont retentées tant que le routage
  distribué n'est pas prêt.

**Package** : `io.github.quizup.profile`

**Avatar** : le profil porte `avatarOptions` (JSON des options DiceBear, styles `micah`, `lorelei` ou
`notionists`, cf. `web-applications/quizup-web/src/shared/avatar/styles/`) choisi par
le propriétaire. **Aucun stockage d'image** : l'avatar est régénéré côté client de façon déterministe
à partir de ces options (repli sur `userId` si `null`).

**Bannière** : le profil porte `bannerUrl` (URL http(s) ≤1024, `null` = aucune), uploadée via
`POST /api/media` (BFF → MinIO) puis attachée par `PUT /api/profiles/{userId}/banner` ; l'image est
affichée derrière l'avatar des fiches joueur (web). Migration `V7__add_banner_url_to_profile_entry.sql`.

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
## 3. Use cases (ports entrants — `domain/port/in/`)

- `GetProfileUseCase` — récupération par userId (404 si inconnu)
- `UpdateProfileUseCase` — mise à jour par champ (pseudonyme, bio, pays, avatar, bannière, langue) ;
  propriétaire uniquement, no-op si la valeur est inchangée
- `CreateProfileUseCase` — création (utilisée par le seeding système ; en nominal par la saga)
- `CheckProfileUseCase` — vérification d'existence (`existsById`)
- `SearchProfileUseCase` — recherche paginée (pattern SDK `SearchRequest` → `SearchResponse`)
- `GetProgressionUseCase` — progression globale et par thème

**Queries** (`domain/query/ProfileQuery.java`) : `GetProfileQuery` (consommée par
social/matchmaking pour le pseudonyme), `ProfileExistsByIdQuery` (consommée par social),
`ProfileSearchQuery`.
**Queries progression** (`ProgressionQuery.java`) : `GetProgressionQuery`, `GetTopicProgressionQuery`.

**Ports sortants locaux** : `ProfileRepositoryPort` (persistance de la projection, dont
`existsById`), `ProgressionRepositoryPort` (projection de progression).

---

## 4. Dépendances inter-services

**Conso** : `quizup-identity-domain` (artifact Maven) — la saga
`CreateProfileSaga` (`@StartSaga @SagaEventHandler(associationProperty = "userId")`) consomme
`UserEvent.UserRegisteredEvent` via le bus Kafka partagé (transport Axon distributed). La saga est
**idempotente sans lecture de projection** : `CreateProfileCommand` est idempotente dans
`ProfileAggregate` (`@CreationPolicy(CREATE_IF_MISSING)`, no-op si le profil existe déjà) — la saga
est replay-safe et ne dépend d'aucun read model
(`best-practices/.backend/anti-patterns.md` § 4).

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

- `ProgressionResponse` porte `duelStats` (joués, victoires, défaites, égalités, winrate, meilleur
  score, meilleure série) et `TopicProgressResponse` porte `title` (titre par thème).
- **Courbe de niveaux explicite et à départ accéléré** (remplace l'ancien `1 + √(xp/100)`) :
  `ProgressionRules.xpThreshold(L) = (L−1)·(50L − 20)`, soit un palier de `100·L − 20` XP entre
  les niveaux `L` et `L+1` — N2 = 80 XP (≈1 duel), N5 = 920 (≈8), N10 = 4 320 (≈36),
  N20 = 18 620 (≈155). `levelFor` / `xpForNextLevel` / `xpFor` gardent leur signature ; le
  niveau est recalculé à la lecture, la nouvelle courbe se répercute donc sans migration.
- **Maîtrise par sujet** (`MasteryGrade`) : échelle **distincte** du niveau de compte, portée par
  l'XP cumulée **dans le sujet**, en 7 grades à table de seuils explicite
  (`Novice 0 · Apprenti 300 · Pratiquant 1 000 · Confirmé 2 500 · Expert 6 000 · Maître 14 000 ·
  Légende 30 000`). C'est le support des **titres thématiques**.
- **Titres portés** (`PlayerTitle`, clé stable `topicId:GRADE`) : le joueur choisit le titre
  affiché sur son profil, les cartes et les listes. On persiste la **clé**, jamais le libellé :
  celui-ci se compose à l'affichage (grade + nom du sujet, ex. « Confirmé · Histoire »), donc il
  suit le renommage du sujet et la langue du lecteur. Champ `profile_entry.equipped_title`
  (migration `V9`), commande `UpdateEquippedTitleCommand` (propriétaire uniquement, clé validée,
  `null` efface) ; l'**appartenance** du titre (grade réellement atteint) est vérifiée par
  l'appelant, qui prépare la commande.
- **Catalogue de badges** (`Badge`) organisé en **familles × paliers × métrique** : `BadgeFamily`
  (`VOLUME`, `MASTERY`, `EXPLOIT`, `COLLECTION`, puis `SERIES`, `ASSIDUITY`, `SOCIAL`,
  `CHALLENGES`, `EXPLORATION`, `PRECISION`, `SPEED`), `BadgeTier` (`BRONZE → ARGENT → OR`),
  `BadgeMetric` (`HUMAN_DUELS`, `HUMAN_WINS`, `TOPIC_MASTERY`, `DISTINCT_TOPICS`, `WIN_STREAK`,
  `PERFECT_DUELS`, `FAST_ANSWERS`, `DAILY_STREAK`, `FOLLOWERS_GAINED`, `CHALLENGES_SENT`,
  `EARNED_BADGES`, `ONE_SHOT`) et `target` — ce qui permet une **progression visible** (« 7/10
  duels ») et laisse une raison de continuer après le bronze. **Un palier = une constante**
  (`DUELIST_BRONZE`, … `DUELIST_OR`) : les badges acquis restent un simple ensemble de noms,
  **sans migration** ; les 4 codes historiques (`FIRST_WIN`, `PERFECT`, `LIGHTNING`,
  `STREAK_MASTER`) sont préservés (test dédié).
  Familles : **Volume** (duels/victoires entre joueurs), **Maîtrise** (XP du meilleur sujet),
  **Exploit** (faits ponctuels : première victoire, duel parfait, 5 réponses rapides, série de
  victoires), **Collection** (nombre de badges acquis), **Série** (`WIN_STREAK` : victoires
  consécutives — En série / Imbattable / Invincible), **Assiduité** (`DAILY_STREAK` : jours
  joués d'affilée, même règle pure que la projection d'activité — Régulier 3 / Accro 7 /
  Increvable 30), **Exploration** (`DISTINCT_TOPICS` :
  sujets joués — Curieux / Touche-à-tout / Encyclopédie), **Précision** (`PERFECT_DUELS` : duels
  parfaits cumulés — Métronome 10 / Machine à points 50 / Sans-faute absolu 100) et **Rapidité**
  (`FAST_ANSWERS` : réponses rapides cumulées — Doigt sur la gâchette 50 / Rafale 200 / Réflexe
  absolu 500), **Social** (`FOLLOWERS_GAINED` : abonnés gagnés, cumulés — Populaire 5 /
  Célébrité 50 / Icône 200) et **Défis** (`CHALLENGES_SENT` : défis nominatifs lancés —
  Challenger 1 / Provocateur 10 / Maître du défi 50).
  > **Compteurs hors duel** (Social, Défis) : les sagas `SocialBadgesSaga` (sur
  > `UserFollowedEvent` de `quizup-social`) et `ChallengeBadgesSaga` (sur
  > `ChallengeCreatedEvent` de `quizup-matchmaking`, propriétaire des défis) envoient un
  > `RecordBadgeCounterCommand` ; l'agrégat incrémente le compteur de la métrique
  > (`BadgeCounterRecordedEvent`, consommé par la projection) puis réévalue. Ces services ne sont
  > **pas modifiés** : `quizup-profile-infrastructure` consomme seulement leurs contrats (pins
  > `quizup-social-domain` / `quizup-matchmaking-domain`). Les désabonnements ne retirent rien
  > (un badge est définitif) et la livraison étant au-moins-une-fois, un rejeu ne peut que faire
  > progresser un badge cosmétique.
- `AwardXpCommand` / `XpAwardedEvent` portent `correctAnswers`, `fastAnswers`, `draw` et
  `botGame` (issus de `GameEndedEvent`). **Règle produit** : un duel contre bot est un
  **entraînement** — `ProgressionRules.xpFor(score, won, botGame)` vaut **0 XP** (la progression
  ne vient que des duels entre joueurs) ; les **badges restent évalués** (faits du duel), la
  **série journalière** est créditée, et le duel est exclu des stats V/N/D (colonne `draws`,
  migr. V5) et du classement. Les égalités sont comptées comme telles (jamais en défaite).
  Compteurs `duelStats` maintenus en projection, les événements antérieurs (`botGame=false`,
  `draw=false`) restent comptés. Le **jour local** du duel (`app.activity.zone`) est préparé par
  la saga des badges et porté par la commande (l'agrégat reste pur).
- **Attribution des paliers** (`BadgeRules`, règles **pures** testées sans Axon) : les badges
  vivent dans un **agrégat dédié `BadgeAggregate`** (`badges:<userId>`), séparé de
  `PlayerProgressAggregate` (qui ne porte plus que XP/niveau/titre). La saga
  **`AwardBadgesSaga`** consomme `XpAwardedEvent` (qui porte déjà tous les faits du duel) et
  envoie un `EvaluateBadgesCommand` ; `BadgeAggregate` maintient ses propres compteurs (duels et
  victoires entre joueurs, XP et série de victoires par sujet, duels parfaits et réponses rapides
  cumulés, sujets joués), les met à jour **au replay** depuis `BadgeFactsRecordedEvent`
  (idempotent par `gameId`), puis évalue les faits et émet un `BadgeEarnedEvent` par badge
  nouvellement acquis. L'évaluation est **générique par métrique** (tout badge dont la cible est
  atteinte), donc **ajouter un palier au catalogue l'active sans toucher à l'agrégat**. La famille
  `COLLECTION` se débloque **en cascade** (un badge acquis peut franchir un palier de collection,
  qui en franchit un autre). Le volume et la maîtrise ne progressent **que** sur les duels entre
  joueurs, alors que les exploits (faits du duel : score parfait, réponses rapides) restent acquis
  **même en entraînement**. Ordre d'émission stable (`EnumSet`) → replay déterministe. Le read
  model des badges est inchangé (badges écrits par la projection) ; deux compteurs ont été
  ajoutés à `progression_entry` pour l'affichage de la progression (`perfect_duels`,
  `fast_answers_total`, migration `V10` ; `current_day_streak` / `best_day_streak` /
  `last_active_date`, migration `V11` ; `followers_gained` / `challenges_sent`, migration
  `V12`).
- **Exposition** : `MasteryGrade`, `Badge` (famille/palier/cible), les titres thématiques
  (`equippedTitle`) et les badges sont exposés par le BFF (`/api/profiles/{id}/badges`,
  `/titles`, `/title`) et affichés par le web (collection de badges par familles, sélecteur de
  titre porté). Le titre de **compte** (`ProgressionRules.titleFor`) reste le libellé transitoire
  affiché tant qu'aucun titre thématique n'est porté.
  > ⚠️ **Ordre de déploiement obligatoire** : les nouveaux codes de badges (`DUELIST_*`,
  > `WINNER_*`, `MASTER_*`, `COLLECTOR_*`, `SERIES_*`, `ASSIDUITY_*`, `EXPLORER_*`,
  > `PRECISION_*`, `SPEED_*`, `SOCIAL_*`, `CHALLENGES_*`) et les familles/métriques ajoutées
  > (`SERIES`, `ASSIDUITY`, `SOCIAL`, `CHALLENGES`, `EXPLORATION`, `PRECISION`, `SPEED` ;
  > `DISTINCT_TOPICS`, `WIN_STREAK`, `PERFECT_DUELS`, `FAST_ANSWERS`, `DAILY_STREAK`,
  > `FOLLOWERS_GAINED`, `CHALLENGES_SENT`) sont des constantes
  > d'énumération qui traversent le query bus. Le Jackson par défaut **échoue sur une constante
  > inconnue** (`READ_UNKNOWN_ENUM_VALUES_AS_NULL` désactivé) : un BFF encore épinglé sur
  > l'ancienne version casserait la lecture de `PlayerProgress`. Il faut donc **release profile →
  > bump du pin dans le BFF → déployer le BFF, puis déployer `quizup-profile`** (le BFF à jour
  > tolère les deux jeux de badges, l'ancien non).
- **Activité journalière** : `ActivityProjection` (règle pure `ActivityRules`, réutilisée par
  l'agrégat de badges pour la famille Assiduité) alimente `progression_activity` (série
  courante/record, dernier jour actif) et `progression_activity_day` (parties par jour) depuis
  `GameEndedEvent` ; capability conservée côté service mais **non exposée par le BFF pour le MVP**
  (le panneau web a été retiré, à rebrancher plus tard).

### Vues BFF (queries dédiées)

- `PresenceQuery.GetPresencesByIdsQuery(userIds)` → `List<PlayerPresence>` : présences en lot
  (les joueurs sans ligne sont absents ; `404` REST = jamais connecté).
- `ProgressionQuery.GetGamesXpQuery(userId, gameIds)` → `List<GameXp>` : XP réellement attribuée
  par partie, lue depuis le journal `progression_awarded_game` (colonne `xp`, migr. V3).
- `ProfileQuery.GetProfileSuggestionsQuery(nameQuery, limit)` : suggestions ⌘K par nom.
- `ProfileQuery.CountImageReferencesQuery(imageUrls)` → `List<ImageReferenceCount>` : comptage batch
  des bannières de profil référencées, réservé à la **console média admin** du BFF
  (`/api/admin/media`, `ROLE_ADMIN` — une suppression d'objet référencé est bloquée).
- `GetProgressionsByIdsQuery` est désormais consommée par le BFF (niveaux des listes de personnes) ;
  `SearchProfileUseCase` / `SearchChallengeUseCase` restent pour les futures surfaces d'administration.
