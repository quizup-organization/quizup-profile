# quizup-profile (staging local)

Structure cible en 2 sous-modules Maven :

- `quizup-profile-domain`
- `quizup-profile-infrastructure`

Seul `quizup-profile-domain` est importable inter-services.

Le code applicatif, lorsqu'il existe, reste organise sous `io.github.quizup.profile.application.*` dans le module infrastructure.

