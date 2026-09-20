# Kaamelott Nuit

Une application Android pour s'endormir en écoutant **ses propres fichiers audio** de Kaamelott. Choisissez un dossier sur votre téléphone, réglez la durée d'écoute et lancez un passage. Le mode de sélection intelligente privilégie les passages moins écoutés ; un mode entièrement aléatoire est aussi disponible.

L'application **ne contient aucun épisode ni fichier audio**. Vous devez disposer de vos propres fichiers et avoir le droit de les utiliser. Ce projet non officiel n'est pas affilié aux créateurs ou ayants droit de Kaamelott.

## Utilisation

1. Installez l'application sur un appareil Android (Android 7.0 ou plus récent).
2. Placez vos fichiers audio sur le téléphone (WebM, Opus, M4A ou MP3).
3. Dans l'application, sélectionnez le dossier qui les contient.
4. Choisissez une durée et lancez l'écoute. Vous pourrez aussi parcourir les épisodes et lancer un passage précis.

## Télécharger

La [page du projet](https://knulp222.github.io/Kam/) propose l'APK Android 1.3 fourni par l'auteur. Le fichier est également conservé dans [`docs/downloads/Kaamelott-Nuit-1.3.apk`](docs/downloads/Kaamelott-Nuit-1.3.apk).

SHA-256 : `f89517aa06dd4e6ec70f88cb5829dbc6690ac5a62eb7dfd978262db0f3c534bc`.

## Construire depuis le code source

Ouvrez ce dépôt dans Android Studio avec le SDK Android 36. Le projet ne contient actuellement pas les scripts et le JAR du Gradle Wrapper ; Android Studio ou une installation locale de Gradle compatible sera nécessaire pour synchroniser et construire le projet. La configuration `release` attend une clé de signature privée (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) et ne peut pas produire un APK de distribution sans elle. Une compilation `debug` nécessite également un `debug.keystore` local, selon la configuration actuelle.

Le site GitHub Pages est dans `docs/` ; sa source de publication doit être configurée sur la branche `main` et le dossier `/docs` dans les paramètres du dépôt.
