# FayyadhOS 2.8-weather · binaan ujian

## Perubahan
- Apps dipindah ke bawah (y=400 dalam design space 960×540), lebar carousel 880. Empty state turut dipindah.
- Cuaca di kanan bawah butang Settings (x=690,y=96), jam kekal di tengah.
- Wallpaper automatik ON secara lalai. Tiada hujan: Kinabalu Sunset asal; berawan, hujan dan ribut: tiga variasi imej AI Kinabalu yang dibekalkan secara offline.
- Menggunakan state ramalan tempoh semasa daripada RainController. Tiada API, permission atau library tambahan.
- Ini RAMALAN, bukan pengesanan hujan secara langsung. Tiada hujan tidak bermaksud matahari sedang bersinar.
- Data kosong, tidak dikenali, luput 24 jam, timestamp masa hadapan atau lokasi salah memulihkan wallpaper manual. Ringkasan harian sahaja tidak memilih latar automatik. Cache sah boleh mengaktifkan latar walaupun offline.
- State yang tidak berubah tidak decode bitmap semula. Nama wallpaper manual tidak ditimpa.

## Tetapan
Settings → Wallpaper. Tajuk memaparkan Ikut cuaca: ON/OFF.
Butang Cuaca auto: OFF mematikan mod yang sedang ON; Cuaca auto: ON mengaktifkannya semula.
Memilih mana-mana imej manual mematikan mod automatik dan menyimpan pilihan itu.
Preview hujan tidak menukar keadaan ramalan atau memaksa latar hujan.

Azan lima waktu, Jarvis, SpeedProbe, WeatherWidget dan renderer RainGlassView dibandingkan byte-for-byte dengan ZIP yang dimuat naik dan tidak diubah.
Azan kekal opt-in: Settings → Azan · Lima waktu → pilih audio sendiri, beri kebenaran yang diminta, uji, kemudian aktifkan. Tiada audio azan dimasukkan ke APK.

## Pengesahan
- assembleDebug: BUILD SUCCESSFUL.
- 308 ujian: 307 lulus, 0 gagal, 0 error, 1 ujian live cuaca dilangkau.
- Ujian baharu: geometri 720p/1080p/4K, empat state melalui controller sebenar dengan fixture, cache stale/future/wrong-location/unknown, daily-summary fallback, pilihan manual, re-enable auto, bitmap reuse dan preview.
- Paparan native Robolectric diperiksa pada 720p, 1080p dan 4K, serta empat latar. Aplikasi contoh dan data cuaca dalam screenshot ialah fixture ujian, bukan data TV sebenar. Ikon Android generik ialah ikon fixture, bukan pengganti ikon apps production.
- Lint: 45 Error, 76 Warning. BUKAN lint clean. Kedua-dua fail yang mengandungi ralat (Config.java dan res/values/strings.xml) tidak berubah berbanding ZIP sumber asal. Laporan penuh disertakan tanpa suppress.
- Belum diuji pada TV fizikal, remote fizikal, pertukaran cuaca live, atau azan semasa standby/bersama apps lain.

## Pemasangan selamat
Application ID: dev.farred.fayyadhos.tv.jarvis
Version: 2.8-weather / code 11; min SDK 26, target SDK 34.
APK DEBUG/ujian, bukan release production. Sijil debug dijana dalam sesi ini; bukan sijil APK sesi lama.
Jika update gagal kerana signature, JANGAN uninstall launcher yang berfungsi, clear data atau disable stock launcher. Update serasi memerlukan signing key asal. Signing key tidak dimasukkan ke ZIP source.
SHA-256 certificate: 90ba44406eb2ce04b8c7396716c3b978cedf7db41b3907d60bbfa046baa38bb5
SHA-256 APK: 0b28760d36c1cc60c3f7759392be017281268d99c1fdae62471717d4fee3afb6

## Bina semula
JDK 17, Gradle 8.9, Android SDK 34. Tetapkan ANDROID_HOME ke SDK sendiri.
`gradle :app:assembleDebug :app:testDebugUnitTest`
Fail local.properties, build cache dan keystore tidak dibekalkan. app/build.gradle menggunakan release signing environment asal jika pengguna menyediakan key sendiri melalui aliran selamat.
Laporan current: verification/weather-layout/. Laporan dalam folder verification lain ialah laporan versi terdahulu.
