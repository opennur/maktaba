# Maktaba

[Read this README in Bahasa Indonesia](README.id.md)

Maktaba is a production-ready Android reader for the
[OpenITI](https://github.com/OpenITI/RELEASE) corpus. It provides a searchable
catalog, cached downloads, an offline reader.

## Release Status

Maktaba `1.0.0` is ready for production packaging.

- Application ID: `org.maktaba.app`
- Minimum Android version: Android 8.0, API 26
- Compile and target SDK: 35
- Java and Kotlin target: JVM 17
- OpenITI release: `v2025.1.9`
- Release builds use R8 and resource shrinking.

Configure production signing outside the repository before distributing the
release APK.

## Features

- Imports the complete metadata catalog from the pinned OpenITI release.
- Searches books by title, author, or OpenITI URI.
- Shows available versions and their metadata.
- Downloads individual texts into the app's local cache.
- Provides a local library of downloaded books.
- Reads cached books offline after download and indexing.
- Supports right-to-left reading for Arabic, Persian, and Ottoman Turkish texts.
- Includes table of contents navigation, font-size controls, and in-book search.
- Saves reading progress and bookmarks.
- Supports standard text selection in the reader.

## Network and Storage

The first launch needs network access to import the OpenITI catalog. Refreshing
the catalog and downloading a new book also require network access. Catalog data,
download state, reader blocks, search indexes, bookmarks, and reading progress
are stored locally by the app.

## Requirements

- JDK 17
- Android SDK 35
- Android device or emulator running API 26 or later
- Network access for Gradle dependencies and the initial catalog import

## Build and Verify

Use the included Gradle wrapper from the repository root:

```shell
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:assembleRelease
```

For a connected emulator or device, run the instrumentation tests with:

```shell
./gradlew :app:connectedDebugAndroidTest
```

For a debug APK, run:

```shell
./gradlew :app:assembleDebug
```

## Release Artifacts

- Current unsigned release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Before distribution, configure the release signing key, build the release
variant, and verify the signed APK on a clean Android installation. A signed
build will normally be written as `app-release.apk`.

The release signing configuration is optional in local builds. Set these Gradle
properties in a user-level `~/.gradle/gradle.properties` file or in CI secrets:

```properties
Maktaba_RELEASE_STORE_FILE=/absolute/path/to/maktaba-release.jks
Maktaba_RELEASE_STORE_PASSWORD=...
Maktaba_RELEASE_KEY_ALIAS=maktaba
Maktaba_RELEASE_KEY_PASSWORD=...
```

## Production Checklist

- Update `versionCode` and `versionName` in `app/build.gradle.kts` for each release.
- Configure signing through local Gradle properties or the CI secret store.
- Run unit tests, lint, instrumentation-test compilation, and the release build.
- Verify an upgrade with existing catalog, downloads, bookmarks, and progress data.
- Verify first-launch catalog import and catalog refresh.
- Verify download, offline reading, search, bookmarks, progress, text selection, and the hidden 20-tap export gesture.
- Confirm attribution and distribution requirements for the OpenITI data release.
- Publish the privacy notice from [PRIVACY.md](PRIVACY.md) and attribution from [NOTICE.md](NOTICE.md).

## Data Source

Maktaba imports metadata and downloads texts from the pinned
[OpenITI `v2025.1.9` release](https://github.com/OpenITI/RELEASE/tree/v2025.1.9).
OpenITI content remains subject to the upstream project's licensing and
attribution requirements.

## Project Structure

- `app/src/main/java/org/maktaba/app/data`: OpenITI networking, parsing, SQLite persistence, and caching.
- `app/src/main/java/org/maktaba/app/ui`: Catalog, library, book details, and reader screens.
- `app/src/test`: Unit tests for catalog parsing, release URL behavior, and text parsing.
- `app/src/androidTest`: Android instrumentation tests for database migrations.
- `.github/workflows/android.yml`: CI verification and unsigned release artifact build.
