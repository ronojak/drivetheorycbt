# Release Guide

1) Prepare signing (do not commit secrets)
- Create a keystore: `keytool -genkey -v -keystore release.keystore -alias drivetheory -keyalg RSA -keysize 2048 -validity 10000`
- Add to `~/.gradle/gradle.properties` or project `gradle.properties` (local only):
```
RELEASE_STORE_FILE=/absolute/path/to/release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=drivetheory
RELEASE_KEY_PASSWORD=...
```

2) Build release AAB
- `./gradlew bundleRelease`
- Output: `app/build/outputs/bundle/release/app-release.aab`

3) Test
- Install debug APK for local testing: `./gradlew installDebug`
- Run tests: `./gradlew testDebugUnitTest`
- Optional: `./gradlew connectedAndroidTest` with emulator/device

4) Play Console
- Upload AAB, fill listing, content rating, and privacy policy.

