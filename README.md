# Maktaba

Maktaba is an Android reader for the [OpenITI](https://github.com/OpenITI/RELEASE)
corpus. The first version is online-first: it imports the catalog from the pinned
OpenITI `v2025.1.9` release and downloads individual texts when requested.

## Build

```shell
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

The app needs network access on first launch to import the OpenITI catalog.
