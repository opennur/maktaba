# Maktaba

[Baca README ini dalam Bahasa Inggris](README.md)

Maktaba adalah aplikasi pembaca Android yang siap dikemas untuk produksi untuk
korpus [OpenITI](https://github.com/OpenITI/RELEASE). Aplikasi ini menyediakan
katalog yang dapat dicari, unduhan tersimpan di cache, pembaca offline.

<img width="240" height="480" alt="1000023441" src="https://github.com/user-attachments/assets/73fa312c-ef22-4026-811e-6a347e05954a" />

<img width="240" height="480" alt="1000023441" src="https://github.com/user-attachments/assets/2b8c3204-9592-4b6f-8c26-610e7bf27be5" />

<img width="240" height="480" alt="1000023443" src="https://github.com/user-attachments/assets/03bd4c50-1a30-427a-92c3-fdd60f9f4db7" />

## Status Rilis

Maktaba `1.0.0` siap dikemas untuk rilis produksi.

- ID aplikasi: `org.maktaba.app`
- Versi Android minimum: Android 8.0, API 26
- SDK kompilasi dan target: 35
- Target Java dan Kotlin: JVM 17
- Rilis OpenITI: `v2025.1.9`
- Build rilis menggunakan R8 dan resource shrinking.

Build rilis selalu ditandatangani. Konfigurasikan kunci signing produksi di
luar repositori sebelum mendistribusikan APK; tanpa kunci tersebut, build lokal
dan CI menggunakan kunci debug Android sebagai fallback yang dapat diinstal.

## Fitur

- Mengimpor katalog metadata lengkap dari rilis OpenITI yang dipatok.
- Mencari buku berdasarkan judul, penulis, atau URI OpenITI.
- Menampilkan versi yang tersedia beserta metadatanya.
- Mengunduh teks individual ke cache lokal aplikasi.
- Menghapus versi yang telah diunduh dari Available versions sambil mempertahankan bookmark dan kemajuan membaca.
- Menyediakan perpustakaan lokal untuk buku yang telah diunduh.
- Membaca buku yang tersimpan secara offline setelah diunduh dan diindeks.
- Mendukung pembacaan dari kanan ke kiri untuk teks Arab, Persia, dan Turki Utsmani.
- Menyediakan navigasi daftar isi, pengaturan ukuran huruf, dan pencarian di dalam buku.
- Menyimpan kemajuan membaca dan bookmark.
- Mendukung pemilihan teks biasa di pembaca.

## Jaringan dan Penyimpanan

Peluncuran pertama memerlukan akses jaringan untuk mengimpor katalog OpenITI.
Penyegaran katalog dan pengunduhan buku baru juga memerlukan akses jaringan.
Data katalog, status unduhan, blok pembaca, indeks pencarian, bookmark, dan
kemajuan membaca disimpan secara lokal oleh aplikasi.

## Persyaratan

- JDK 17
- Android SDK 35
- Perangkat atau emulator Android dengan API 26 atau yang lebih baru
- Akses jaringan untuk dependensi Gradle dan impor katalog awal

## Build dan Verifikasi

Gunakan Gradle wrapper yang tersedia dari direktori root repositori:

```shell
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease
```

Instrumentation test saat ini tetap disimpan di `app/src/androidTest`, tetapi
dinonaktifkan dalam build Gradle dan workflow CI.

Untuk membuat APK debug, jalankan:

```shell
./gradlew :app:assembleDebug
```

## Artefak Rilis

- APK rilis bertanda tangan saat ini: `app/build/outputs/apk/release/app-release.apk`
- APK debug: `app/build/outputs/apk/debug/app-debug.apk`

Sebelum distribusi, konfigurasikan kunci penandatanganan rilis, buat varian
rilis, dan verifikasi APK yang telah ditandatangani pada instalasi Android yang
bersih. Build yang telah ditandatangani biasanya akan ditulis sebagai
`app-release.apk`.

Konfigurasi penandatanganan produksi bersifat opsional untuk build lokal.
Simpan properti berikut di `~/.gradle/gradle.properties` milik pengguna atau di
secret CI untuk menggantikan fallback kunci debug:

```properties
Maktaba_RELEASE_STORE_FILE=/absolute/path/to/maktaba-release.jks
Maktaba_RELEASE_STORE_PASSWORD=...
Maktaba_RELEASE_KEY_ALIAS=maktaba
Maktaba_RELEASE_KEY_PASSWORD=...
```

## Checklist Produksi

- Perbarui `versionCode` dan `versionName` di `app/build.gradle.kts` untuk setiap rilis.
- Konfigurasikan penandatanganan melalui properti Gradle lokal atau penyimpanan rahasia CI.
- Jalankan unit test, lint, dan build rilis.
- Verifikasi upgrade dengan data katalog, unduhan, bookmark, dan kemajuan yang sudah ada.
- Verifikasi impor katalog pada peluncuran pertama dan penyegaran katalog.
- Verifikasi unduhan, pembacaan offline, pencarian, bookmark, kemajuan, pemilihan teks.
- Pastikan persyaratan atribusi dan distribusi untuk rilis data OpenITI telah dipenuhi.
- Publikasikan pemberitahuan privasi dari [PRIVACY.md](PRIVACY.md) dan atribusi dari [NOTICE.md](NOTICE.md).

## Sumber Data

Maktaba mengimpor metadata dan mengunduh teks dari
[rilis OpenITI `v2025.1.9`](https://github.com/OpenITI/RELEASE/tree/v2025.1.9)
yang dipatok. Konten OpenITI tetap tunduk pada persyaratan lisensi dan atribusi
proyek sumber.

## Struktur Proyek

- `app/src/main/java/org/maktaba/app/data`: jaringan OpenITI, parsing, penyimpanan SQLite, dan cache.
- `app/src/main/java/org/maktaba/app/ui`: layar katalog, perpustakaan, detail buku, dan pembaca.
- `app/src/test`: unit test untuk parsing katalog, perilaku URL rilis, dan parsing teks.
- `app/src/androidTest`: instrumentation test Android untuk migrasi database yang tetap disimpan tetapi saat ini dinonaktifkan.
- `.github/workflows/android.yml`: verifikasi CI dan build artefak rilis bertanda tangan.
- `TESTING.md`: panduan test JVM lokal, Robolectric, ViewModel, parser, dan repository.
