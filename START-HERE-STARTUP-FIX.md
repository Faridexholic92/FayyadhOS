# FayyadhOS TV Fix · 2.8.1 / code 12

## Pasang dan uji dahulu
Ini app BERASINGAN dengan applicationId `dev.farred.fayyadhos.tv.jarvis.fix` dan nama **FayyadhOS TV Fix**. Bukan update kepada APK lama `dev.farred.fayyadhos.tv.jarvis`.
Signing key APK lama tidak tersedia dalam sesi pembaikan; sijil baharu tidak serasi untuk update. Tidak perlu uninstall atau clear data APK lama / launcher asal untuk memasang varian ini. Tetapan dan audio azan lama tidak dipindahkan secara automatik.
1. Pasang APK baharu, kemudian pilih Open / Buka **FayyadhOS TV Fix**.
2. Pastikan dashboard muncul dan remote boleh digunakan.
3. Hanya selepas itu, jika mahu, Settings → Set default Home → pilih **FayyadhOS TV Fix** jika firmware menawarkan pilihan itu.
4. Jangan disable launcher sistem atau factory reset. Jika masih gagal, jangan pilih Fix sebagai Home; hantar model TV/Android dan log crash jika tersedia.
Azan OFF pada pemasangan baharu; pilih audio sendiri dan aktifkan selepas ujian bunyi. Tiada rakaman azan dibekalkan.

## Punca yang berjaya dihasilkan semula
APK 2.8-weather yang dihantar sebelum ini gagal pada emulator Android 9 sebelum Activity bermula: `Unable to get provider com.gathin.tvhome.ThemeProvider` / `ClassNotFoundException`.
Audit class_defs DEX sebenar mendapati **0 kelas aplikasi (selain R/resource)** dan semua 12 komponen manifest tiada definisi. Pemeriksaan nama kelas sebagai string sahaja tidak memadai.
Binaan terdahulu telah dijalankan dengan proses Gradle bertindih pada folder output sama; ini kemungkinan mekanisme kerosakan pembungkusan. Log TV pengguna belum diperoleh, jadi bukti crash di sini ialah reproduksi APK lama dalam emulator.

## Pembetulan
- Build bersih satu proses Gradle; tiada proses assemble/test/lint bertindih pada direktori projek yang sama.
- `tools/verify-apk.py` membaca DEX class definitions dan menolak APK jika mana-mana komponen manifest hilang. Final APK mempunyai **350 kelas aplikasi**, semua **12 komponen** lengkap.
- `build-verified-debug.sh` menjalankan clean + assemble + regresi dalam satu panggilan, diikuti audit APK.
- Kategori MAIN/LAUNCHER ditambah untuk app list Android biasa; MAIN/HOME/DEFAULT dan LEANBACK_LAUNCHER dikekalkan. Ketiadaan LAUNCHER bukan punca crash yang direproduksi.
- Layout apps bawah, cuaca kanan dan wallpaper mengikut ramalan dikekalkan. Tiada perubahan pada kod dashboard/rain/azan/Jarvis daripada pembetulan sebelumnya; perubahan companion terhad kepada applicationId, label, versi dan konfigurasi test-signing.

## Pengesahan APK akhir (bukan screenshot mockup)
Android 9 / API 28 AOSP x86_64 emulator, paparan landscape 1280×720, software emulation:
- APK lama dipasang dan kegagalan provider direproduksi.
- APK Fix dipasang sementara APK lama masih dipasang; kedua-dua package disahkan wujud.
- **Tiga cold starts** melalui force-stop → launch; setiap kali proses hidup dan DashboardActivity menjadi resumed activity.
- Tetapkan Home dalam emulator, buka Settings Android dan tekan Home: kembali ke DashboardActivity. Screenshot sebenar `fixed-home.png` dilampirkan dalam verification/startup-fix.
- Query HOME, LAUNCHER, LEANBACK_LAUNCHER menemui DashboardActivity.
- Crash buffer kosong selepas ujian app. Emulator software sempat memaparkan ANR System UI semasa boot/beban emulasi; dialog itu dipulihkan dan screenshot final bersih diperiksa. Ini bukan benchmark prestasi TV.
- APK signature diverifikasi. SHA-256 APK: `b410338eab08fc783b9bc7442901856ea74adc590aaf3ea2ac43b3f7ec1884f1`.

Regresi kod semasa clean build: 308 ujian, 307 lulus, 0 gagal, 0 error, 1 live weather dilangkau. Suite itu dijalankan sebelum pertukaran applicationId/label kepada varian companion. APK companion akhir dibina secara bersiri selepas suite dan diuji install/launch/Home di atas. Laporan XML/log disertakan.
Lint tidak dijalankan semula dalam pembaikan ini. Audit terdahulu melaporkan 45 ralat dan 76 amaran pada source asal; jangan anggap lint clean. Bukti terdahulu kekal di verification/weather-layout.
Belum diuji pada TV fizikal pengguna; tiada jaminan khusus OEM, firmware, standby azan atau coexistence audio. Jangan jadikan Home sebelum ujian Open pada TV berjaya.

## Bina semula
JDK 17, Gradle 8.9, Android SDK 34, min API 26. Jalankan `build-verified-debug.sh` (atau set GRADLE kepada executable Gradle sendiri). Jangan jalankan proses Gradle lain secara serentak pada folder yang sama.
Signing key tidak disertakan dalam ZIP. Untuk sijil test yang stabil, sediakan keystore sendiri dan set FAYYADHOS_DEBUG_KEYSTORE; debug config menggunakan alias/password lalai Android debug. Release signing kekal menggunakan environment asal. Jangan letak key/password production dalam source.
Fail START-HERE-WEATHER.md dan laporan lama merujuk build terdahulu dan digantikan oleh dokumen ini untuk pemasangan.
