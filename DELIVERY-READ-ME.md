# FayyadhOS TV — Petak Solat & Kesan Cuaca

## Hasil
APK DEBUG berjaya dibina daripada projek Java Android yang sama; bukan aplikasi pengganti.
Output projek: `app/build/outputs/apk/debug/app-debug.apk`.
Package/version kekal `dev.farred.fayyadhos.tv.jarvis`, `2.8-master` / 10.
Signature debug APK sah, tetapi **SIJIL BERBEZA daripada APK Jarvis terdahulu**. Tiada release signer digunakan.
Android akan menolak kemas kini terus di atas APK terdahulu yang menggunakan sijil itu.
Private key debug terdahulu tidak ditemui dalam ruang kerja yang tersedia. Untuk upgrade
tanpa kehilangan data, APK mesti ditandatangani dengan private key yang sama dengan aplikasi
terpasang. Jangan uninstall launcher aktif untuk memaksa pemasangan. Source penuh disediakan
untuk build/sign menggunakan persekitaran/key asal; tiada kunci diminta dalam chat.

## Perubahan sebenar
- Satu petak glass di bawah jam menggantikan strip bawah pada Dashboard sahaja.
  Bergilir Subuh, Zohor, Asar, Maghrib, Isyak setiap 8 saat. Syuruk maklumat tambahan.
  Badge frosted SEMASA sentiasa menunjukkan sela waktu semasa, tidak hilang apabila slot bergilir.
  Subuh tamat diserlah pada Syuruk; sebelum Subuh dan Syuruk–Zohor tiada waktu semasa
  dituntut. Ini dasar paparan jadual, bukan fatwa had waktu. Skrin legacy masih ada enam kolum.
- Kiri/kanan menukar slot, fokus/dialog menjeda giliran. OK > Jadual lengkap memaparkan
  enam waktu: Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak. Negeri/zon dipilih pengguna.
  Laluan D-pad: header Down → petak Down → aplikasi, dan Up untuk kembali.
- Sumber solat ditukar daripada api.waktusolat.app kepada HTTPS **terus e-Solat JAKIM rasmi**
  atas permintaan pengguna. Tiada backend, kunci API, GPS, framework atau dependency baharu.
- POST duration dengan `datestart`/`dateend` mendapatkan sebulan. Status, zon, tarikh,
  susunan enam waktu, pendua dan kelengkapan bulan disemak sebelum cache atomik diganti.
  Cache baharu `jakim-esolat-v1` terasing daripada cache pihak ketiga lama (tidak dipadam).
  Sambungan Internet diperlukan sekali untuk mengisi cache rasmi selepas naik taraf.
  Cache hanya memaparkan hari/zonnya yang sepadan, dan asal/tarikh cache ditandakan.
  Respons invalid atau offline tidak menggantikan cache baik.
- Cache disemak semula setiap tengah malam Malaysia apabila Home aktif; selepas TV tidur,
  semakan dilakukan bila Home dibuka semula. Kegagalan cuba lagi selepas 15 minit.
  Tiada servis/alarm/wake-lock latar belakang. Network Focus sedia ada dihormati: jika
  ia diaktifkan, pulihkan tetapan atau gunakan Refresh manual untuk solat.
- Hujan rintik seluruh skrin: maksimum 48 streaks ringan bersama maksimum 96 titisan glass
  sedia ada, tanpa menyekat input atau mengubah susun atur. Tetapan baharu yang belum
  disimpan default ON/ikut ramalan; tetapan OFF pengguna yang memang disimpan kekal OFF.
- Kesan hujan mengikut ramalan tempoh MET Malaysia/data.gov.my yang sah dan segar,
  **BUKAN pengesanan hujan sebenar**. Tiada hujan bagi clear/unknown/cache luput.
  Daily summary tidak mengaktifkan hujan atau kilat.
- Kilat lembut setempat **OFF secara default**, pilihan dalam Rain-on-Glass · Weather.
  Jika dipilih, hanya ramalan ribut petir mengaktifkan satu bolt rendah kontras setiap
  18–30 saat. Tiada flash putih seluruh skrin atau audio. Matikan jika sensitif cahaya.
  Moving OFF/system animator scale 0 menghentikan motion/kilat; dialog/background menjeda efek.
- Wallpaper, katalog aplikasi sebenar, launch intent, ikon aplikasi yang ditemui dan Jarvis
  direct-Groq daripada penghantaran lalu dikekalkan. Tiada mikrofon atau backend ditambah.
  Gambar Android generik pada PNG ialah ikon package-manager ujian, bukan ikon pengganti APK.

## Pengesahan penghantaran ini
- Ujian penuh: **258 kes; 257 lulus, 1 skipped, 0 failure/error**.
  Yang skipped ialah ujian MET live opt-in; ia bukan bukti API MET live berjaya.
- `:app:testDebugUnitTest :app:assembleDebug` berjaya. Signature APK disahkan apksigner;
  manifest APK tiada RECORD_AUDIO. Sijil TIDAK sama dengan debug Jarvis terdahulu;
  in-place upgrade dengan signer ini tidak boleh. Binaan release/minified tidak dibuat.
- Ujian baharu meliputi respons rasmi SBH07, kesegaran cache pada 00:00 MYT, kegagalan
  offline/HTML/bulan tidak lengkap tanpa menimpa cache, sempadan Subuh/Syuruk/Zohor/Asar/
  Maghrib/Isyak, tiada jadual semalam sebagai hari ini, giliran lima slot, pause fokus/dialog/
  background, lightning opt-in dan reduced motion. Regresi launcher/Jarvis turut dijalankan.
- API rasmi SBH07: GET today, GET tarikh 25 Okt 2026, POST duration dua hari dan sebulan
  Oktober 2026 menerima HTTP 200/status OK!/zon SBH07. POST sebulan mengandungi 31 baris.
  Ini ujian rangkaian dari Linux curl, **bukan** bukti sambungan dalam APK pada TV.
  Respons/evidence JSON dilampirkan tanpa cookies/header HTTP atau HTML portal.
- Lint masih **GAGAL: 45 errors / 47 warnings diwarisi**;
  tiada signature isu baharu berbanding penghantaran Jarvis terdahulu. Tiada suppression
  atau blanket baseline ditambah. Amaran baharu sementara telah dibetulkan sebelum penghantaran.
- Native-view render diuji 720p/1080p/4K; tiga PNG pratonton ialah fixture dengan watermark.
  Semakan visual membuang outline teks badge yang berganda dan mengalihkan bolt daripada header.
  Ini bukan sertifikasi pixel-perfect mockup atau prestasi GPU pada TV sebenar.
- Host software-Canvas performance JSON turut disertakan; tiada tuntutan prestasi meningkat
  atau bacaan CPU/memori TV. Maksimum partikel, stop apabila inactive dan adaptive fallback diuji.

## Belum disahkan / batasan
- Tiada Android TV fizikal, remote/IME sebenar, pemasangan/naik taraf APK atau overscan diuji.
- SSL/HTTP JAKIM pada firmware TV, semua 60 zon, tengah malam pada TV tidur, dan penggunaan
  sumber/GPU TV belum diuji. Alarm untuk membangunkan TV memang tidak dibina.
- Cuaca live semasa, ketepatan hujan setempat dan Groq dengan kunci/model pengguna tidak
  diuji dalam fasa ini. Papar ialah proksi ramalan wilayah Kinarut, bukan sensor Kinarut.
- Jarvis direct-key/Keystore daripada fasa sebelumnya dikekalkan, dengan batasan sama:
  firmware/hardware Keystore dan inferens Groq sebenar belum disahkan. Keystore bukan jaminan
  kunci mustahil diekstrak pada peranti compromised/rooted. Tiada kunci diminta dalam chat.
- Jangan uninstall launcher berfungsi/disable stock Home untuk memaksa pemasangan jika
  sijil tidak sepadan. APK ini debug-signed; keserasian production signing belum dibuktikan.

## Cara guna
1. Pasang APK debug hanya jika sesuai dengan peranti/signer anda.
2. Fokus petak solat > OK > pilih negeri dan zon bandar JAKIM. Untuk Kinarut, semak zon
   SBH07 dalam senarai kawasan Papar/Putatan/Pantai Barat; jangan meneka zon daripada cuaca.
3. Pilih Refresh prayer times. Apabila data diterima, putuskan Internet dan semak label cache.
4. Kiri/kanan untuk slot; alihkan fokus untuk auto rotate; OK > Jadual lengkap untuk enam waktu.
5. Settings > Rain-on-Glass · Weather: Enabled ON dan Follow forecast ON. Jika mahu, pilih
   Kilat lembut > Aktifkan. Manual preview ialah simulasi dan tidak membuktikan cuaca sebenar.
6. Kekalkan automatic date/time pada TV. Network Focus mesti dipulihkan untuk refresh automatik.

## Fail berubah dalam kesinambungan ini
- **Diubah** `README-FAYYADHOS-TV.md` — Panduan semasa, migrasi cache, keselamatan, setup, dasar semasa/midnight dan batasan.
- **Diubah** `app/src/main/java/com/gathin/tvhome/DashboardActivity.java` — Petak di bawah jam, bounded greeting, laluan D-pad dan pause giliran semasa dialog.
- **Diubah** `app/src/main/java/com/gathin/tvhome/PrayerPanel.java` — Petak bergilir, badge frosted, lifecycle, cache/status, jadual lengkap; legacy strip dikekalkan.
- **Diubah** `app/src/main/java/com/gathin/tvhome/PrayerRepository.java` — Transport terus JAKIM, validasi bulan penuh, cache rasmi terasing dan kesegaran harian MYT.
- **Diubah** `app/src/main/java/com/gathin/tvhome/PrayerTimes.java` — Nama Melayu, sela waktu semasa dan tick tepat pada sempadan waktu/tengah malam.
- **Diubah** `app/src/main/java/com/gathin/tvhome/RainController.java` — Thunder ditentukan ramalan tempoh yang sah, bukan preview/daily summary.
- **Diubah** `app/src/main/java/com/gathin/tvhome/RainGlassView.java` — Streaks terhad, bolt rendah kontras, reduced motion, rendering tanpa per-frame allocation baharu.
- **Diubah** `app/src/main/java/com/gathin/tvhome/RainOptions.java` — Default automatik untuk tetapan belum disimpan, hormati OFF sedia ada, lightning opt-in.
- **Diubah** `app/src/main/java/com/gathin/tvhome/RainSettingsDialog.java` — Pilihan lightning berserta amaran cahaya dan penerangan.
- **Diubah** `app/src/test/java/com/gathin/tvhome/DashboardRedesignTest.java` — Ujian regresi diselaraskan secara eksplisit untuk sumber/cache/geometry/default baharu; tingkah laku lama yang relevan tetap diuji.
- **Diubah** `app/src/test/java/com/gathin/tvhome/DashboardTest.java` — Ujian regresi diselaraskan secara eksplisit untuk sumber/cache/geometry/default baharu; tingkah laku lama yang relevan tetap diuji.
- **Diubah** `app/src/test/java/com/gathin/tvhome/RainFeatureTest.java` — Ujian regresi diselaraskan secara eksplisit untuk sumber/cache/geometry/default baharu; tingkah laku lama yang relevan tetap diuji.
- **Diubah** `app/src/test/java/com/gathin/tvhome/RainPerformanceTest.java` — Laporan host turut merekod had 48 streaks; bukan benchmark TV.
- **Diubah** `app/src/test/java/com/gathin/tvhome/WeatherWidgetTest.java` — Ujian regresi diselaraskan secara eksplisit untuk sumber/cache/geometry/default baharu; tingkah laku lama yang relevan tetap diuji.
- **Ditambah** `app/src/main/res/values/solat_weather_strings.xml` — Format label Melayu dengan placeholder; tiada amaran lint baharu.
- **Ditambah** `app/src/test/java/com/gathin/tvhome/PrayerWeatherUpgradeTest.java` — Ujian cache/midnight/badge/giliran/reduced-motion dan capture PNG simulasi.
- **Ditambah** `app/src/test/resources/jakim-SBH07-2026-10-OFFICIAL-CAPTURE.json` — Fixture respons rasmi JAKIM Oktober 2026, 31 hari SBH07.
- **Ditambah** `app/src/test/resources/jakim-shape-SWK08-2026-10-TEST-ADAPTED.json` — Fixture bentuk rasmi yang ditukar daripada fixture pihak ketiga lama; bukan respons live rasmi SWK08.

Kesinambungan ini: 14 fail diubah, 4 ditambah. Delta kumulatif daripada ZIP asal: 25 diubah, 11 ditambah, 1 dipadam (resource backend lama dari fasa terdahulu sahaja). Source ZIP mengandungi projek penuh, patch, log dan evidence. SDK/JDK, build cache, private signing key dan credentials tidak disertakan.
