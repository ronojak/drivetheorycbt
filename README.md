# DriveTheory CBT

Offline-first Android app for randomized driving theory exams. Built with Kotlin, Jetpack Compose, and Room.

## Features
- Timed MCQ exams with randomized questions/options
- Local scoring and attempt history
- Offline-first (seed questions bundled in assets)

## Quick Start
- Requirements: Android Studio Giraffe+ (JDK 17), Android SDK 34
- Build debug: `./gradlew assembleDebug`
- Run tests: `./gradlew testDebugUnitTest`
- Install debug: `./gradlew installDebug`

## Android SDK Setup
- Ensure the Android SDK is installed (Android Studio > Settings/Preferences > Appearance & Behavior > System Settings > Android SDK).
- Provide the SDK path via one of:
  - Set env var `ANDROID_SDK_ROOT` (or `ANDROID_HOME`), or
  - Create `local.properties` with `sdk.dir=/absolute/path/to/Android/Sdk`.
- You can auto-generate `local.properties` by running: `bash scripts/setup-local-properties.sh`.
- See examples in `local.properties.example`.

## Project Layout
- `app/src/main/java/com/drivetheory/cbt/` — source
  - `engine/` exam logic, `data/` repositories/Room, `domain/` models, `presentation/` Compose UI
- `app/src/main/assets/seed/` — question files. The app merges all `*.json` and `*.jsonl` here.
  - Starter: `questions_seed.json` (+ optional `questions_sample_extra.json`)
  - See `docs/QUESTION_FORMAT.md` to import your full bank.
- `docs/` — architecture, release guide, TODOs

## Release Build
- Set signing keys in `~/.gradle/gradle.properties` (do not commit):
```
RELEASE_STORE_FILE=/abs/path/release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=drivetheory
RELEASE_KEY_PASSWORD=...
```
- Build AAB: `./gradlew bundleRelease` (see `docs/RELEASE.md`)

## Contributing
- See `AGENTS.md` for structure, style, and PR guidelines.

## Branding (Logo & App Icon)
- Landing page logo: place your WebP at `app/src/main/assets/brand/logo.webp`. The Home screen will load it automatically.
- App icon: in Android Studio, use Tools → Asset Studio → Launcher Icons. Select your WebP and generate into `mipmap-*/`. Keep the name `ic_launcher` to use the existing manifest mapping.
