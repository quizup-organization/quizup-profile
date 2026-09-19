# quizup-profile

Gestion des **profils modifiables** des utilisateurs (nom public, bio, pays) :

- **Lecture** : le profil est lu côté frontend depuis le JWT (identity) ; ce service expose
  `GET /api/profiles/{userId}` et `POST /api/profiles/search` (projection JPA).
- **Écriture** : `PUT /api/profiles/{userId}` — propriétaire uniquement (403 sinon).
- **Création** : saga déclenchée par `UserRegisteredEvent` du service identity.

> Voir `AGENTS.md` pour le détail des endpoints, use cases et dépendances.
