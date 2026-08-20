# Testing Maktaba

Maktaba uses local JVM tests as its fast feedback loop. The test setup follows
the actual application architecture rather than adding unused Hilt, Room, or
Retrofit layers:

- Repository boundary: `MaktabaRepository`.
- Database: `SQLiteOpenHelper` with Robolectric coverage.
- Network: OkHttp with MockWebServer responses.
- Asynchronous code: Kotlin Coroutines and `MainDispatcherRule`.
- UI state: `StateFlow` and ViewModel tests.
- Parser and mapper logic: pure JUnit tests.

## Test Structure

```text
app/src/test/java/org/maktaba/app/
├── MaktabaViewModelTest.kt
├── testing/MainDispatcherRule.kt
└── data/
    ├── CatalogRecordMapperTest.kt
    ├── MaktabaDatabaseRobolectricTest.kt
    ├── OpenItiCatalogParserTest.kt
    ├── OpenItiMarkdownParserTest.kt
    ├── OpenItiReleaseTest.kt
    ├── OpenItiRepositoryTest.kt
    └── TextNormalizerTest.kt
```

`MaktabaViewModelTest` uses an explicit fake repository. This avoids MockK's
ByteBuddy runtime agent, which cannot attach in the project's Linux ARM64
sandbox, and keeps local tests deterministic. The repository interface still
allows MockK or another mocking library to be used on supported JVM/CI hosts.

## Run Tests

Run all local JVM tests without an emulator or device:

```shell
./gradlew :app:testDebugUnitTest
```

Run one test class:

```shell
./gradlew :app:testDebugUnitTest --tests "org.maktaba.app.MaktabaViewModelTest"
```

Run the full local quality checks:

```shell
./gradlew :app:testDebugUnitTest :app:lintDebug
```

## Robolectric

`MaktabaDatabaseRobolectricTest` and `OpenItiRepositoryTest` use Robolectric,
SQLite, and MockWebServer. They run in the JVM and never call the real OpenITI
service.

Robolectric's native runtime does not support Linux ARM64 in the current
upstream release. The Gradle test configuration excludes only those two
Robolectric classes on ARM64, while the pure JVM tests continue to run locally.
The GitHub Actions workflow runs the Robolectric tests on x86_64. Android
instrumentation tests remain in `app/src/androidTest` but are currently disabled.

## Coverage Focus

- Catalog parsing, missing fields, booleans, numeric values, and mapping.
- OpenITI mARkdown headings, paragraphs, continuation lines, metadata, and pages.
- Arabic normalization and FTS query generation.
- ViewModel loading, errors, download state, and search callbacks.
- Delete flow removes cached content while preserving the catalog version.
- Database migration without deleting existing rows.
- Catalog import and download fallback paths through MockWebServer.
