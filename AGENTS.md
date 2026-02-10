# Repository Guidelines

## Project Structure & Module Organization
- Source code (Android/Kotlin) lives under `app/src/main/java/com/drivetheory/cbt`. UI is in `presentation/` (screens, components), domain in `domain/`, data in `data/`, and core exam logic in `engine/`.
- Tests: unit in `app/src/test`, instrumentation in `app/src/androidTest`.
- Assets: seed data or prepackaged DB in `app/src/main/assets/{seed,db}`.
- Documentation: current architecture at `DriveTheory_CBT_Loadable_Architecture_and_Project_Structure_FULL.md`. Prefer moving docs to `docs/` as the project grows.

## Build, Test, and Development Commands
- Build debug APK: `./gradlew assembleDebug`
- Install on device/emulator: `./gradlew installDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumentation/UI tests: `./gradlew connectedAndroidTest` (requires emulator/device)
- Lint/format (if configured): `./gradlew lint ktlintFormat detekt`
- Clean: `./gradlew clean`
Run from Android Studio for quick iteration; ensure JDK 17 and Android SDK installed.

## Coding Style & Naming Conventions
- Kotlin + Jetpack Compose; 4-space indent; avoid wildcard imports; max line length 100.
- Packages mirror layers: `core`, `data`, `domain`, `engine`, `presentation`.
- Naming: `PascalCase` for types, `camelCase` for members, `UPPER_SNAKE_CASE` for constants.
- Suffixes: `*Screen`, `*ViewModel`, `*UseCase`, `*Repository(Impl)`, `*Entity`, `*Dto`.
- Document public APIs with KDoc; prefer small, focused files.

## Testing Guidelines
- Frameworks: JUnit4/5, MockK, Turbine (flows), Espresso for UI; Robolectric optional for JVM UI.
- Place unit tests alongside feature packages under `app/src/test/...`; name as `FooBarTest.kt`.
- Instrumented tests under `app/src/androidTest/...`; name as `FooBarIT.kt`.
- Coverage target: ≥80% for `engine` and `domain`; include edge cases for randomization and scoring.

## Commit & Pull Request Guidelines
- Use Conventional Commits: e.g., `feat(engine): implement scoring rules`, `test(domain): add attempt mapping tests`.
- PRs: concise description, linked issues, screenshots/GIFs for UI, validation steps, and notes on data migrations.
- Before opening: run `./gradlew lint testDebugUnitTest` and, when relevant, `connectedAndroidTest`.

## Security & Configuration Tips
- Do not commit keystores, `local.properties`, or real proprietary question banks. Store example seed data under `app/src/main/assets/seed/`.
- Gate exam mode with `FLAG_SECURE`; consider SQLCipher for DB encryption when enabled.
