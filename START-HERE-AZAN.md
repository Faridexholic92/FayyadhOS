# Azan lima waktu — continuation delivery

## Status
Implemented on the supplied **FayyadhOS-5-Waktu-Glow-Jarvis-source.zip**, continuing the unfinished azan work from the prior chat. This is the current handoff for azan; older delivery reports in this source are historical. In particular, the earlier claim that working azan had been “preserved” was inaccurate: the supplied baseline had prayer times/glow, but no azan scheduler/player/audio import. Those are added here.

## Cara guna
1. Buka **Settings → Azan · Lima waktu**. Pilihan yang sama ada melalui **OK pada bar waktu solat → Azan · audio & tetapan**.
2. Pilih zon JAKIM yang betul melalui menu waktu solat. Tiada zon dipilih secara automatik.
3. Pilih **audio azan umum** menggunakan pemilih fail Android (contohnya rakaman anda pada USB). Audio Subuh berasingan adalah pilihan; jika tiada, Subuh menggunakan audio umum.
4. Benarkan **alarm tepat** dan **notifikasi** jika Android memerlukannya.
5. Tekan **Uji bunyi audio umum** dan sahkan bunyi sebelum mengaktifkan automatik. Hentikan melalui butang **Hentikan audio** atau tindakan **Hentikan** pada notifikasi.
6. Aktifkan automatik dan semak medan **Seterusnya**.

**Azan OFF secara lalai. Tiada rakaman audio dibekalkan.** Pilih rakaman yang anda berhak guna. Jika TV tiada pemilih dokumen, import tidak boleh disiapkan sehingga pemilih fail yang sesuai tersedia.

## Implemented behavior
- Exact, opt-in scheduling for Subuh, Zohor, Asar, Maghrib and Isyak; never Syuruk. Uses validated JAKIM schedule cache and Asia/Kuala_Lumpur time, not the TV's timezone or hardcoded prayer times.
- One explicit exact alarm at a time, advancing to the next prayer. Missing cache/permissions do not produce guessed times or an inexact fallback.
- Persistent duplicate protection; no retrospective replay of a missed azan. Alarms received more than 90 seconds late are skipped. An on-time alarm may be accepted up to 90 seconds after its scheduled boundary, to tolerate OS delivery delay.
- Boot, package replacement, timezone/clock changes and exact-alarm permission grant rebuild future scheduling; they do not directly play audio. Force-stop/power-off is not bypassed.
- A network-constrained daily Android job refreshes validated cache while enabled. A recovery job requests missing schedules, with backoff. Next month is prefetched from day 28 when fetching succeeds. OS job timing/network availability is not guaranteed.
- Periodic/recovery jobs have independent cancellation/completion state, and cache writes are serialized to prevent shared temporary-file races.
- User-selected local audio copied into private app storage, with 25 MiB/file limit, atomic replacement and rejection of empty/oversized files or arbitrary HTTP/file URLs. MIME checks are not codec validation: test the actual recording on the TV.
- Media playback foreground service, silent ongoing notification with Stop, preparation/error handling and a 15-minute playback ceiling. Partial wake lock is limited to MediaPlayer playback lifecycle; it does not turn on a powered-off TV.
- **No audio-focus request, media pause command, global volume change or TTS azan.** This avoids deliberately interrupting another app. Audio may overlap; firmware/other apps may behave differently. Physical-TV behavior is NOT yet verified.
- Default OFF; disabling cancels both refresh jobs and the alarm and stops this app's audio. Deleting audio removes private copies, not originals on USB/storage.
- New Settings entry appended without reindexing existing menu items. Five-prayer row/glow, weather, wallpaper, rain and Jarvis Speedtest remain; no home layout redesign, new library, API key or external backend is required for azan.

## Verification performed here
- JDK 17, Gradle 8.9, Android SDK 34.
- Command: `gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest`. Android lint was run separately as `:app:lintDebug`.
- Android tests: **300 total; 299 passed, 0 failures, 0 errors, 1 skipped**. Fresh XML and build/lint evidence are in `verification/azan/`.
- Azan coverage includes five prayer boundaries/no sunrise, MYT timezone, exact/notification permission denial, one alarm, duplicate/early/late rejection, 90-second edge, returning Home at the boundary, rollback protection, next-day Subuh, missing/deleted cache, recovery cancellation, local import limits, Subuh fallback, Stop, no audio-focus request and Settings access.
- Existing regression coverage includes five-prayer glow/layout, Jarvis/Speedtest, weather, rain and personalization. UI frames from tests are fixtures, not physical-device screenshots.
- **Lint is not clean:** 45 existing errors (44 MissingTranslation entries in strings.xml and one embedded byte-order mark in Config.java), plus 76 warnings. Both error-bearing files were compared byte-for-byte with the supplied ZIP and are unchanged. No lint errors point to azan code. These are documented, not suppressed or represented as fixed. New azan warnings include hardcoded Malay UI strings, synchronous SharedPreferences commits used for durable alarm/duplicate state, and an InputStream warning (the stream is closed through the nested try-with-resources). The debug compile/test build succeeds independently of lint.
- The rain menu regression expected eight Settings entries; updated it to expect nine and verify Azan is appended at index 8 while existing positions remain. No rain production code changed.
- Backend historical-stage tests: **12 passed**, provider mocked; no live Groq request made.
- APK signature verified using `apksigner verify --print-certs`. New debug certificate; NOT the original production signing key.
- JarvisPanel, SpeedProbe and other listed protected files checked byte-for-byte against the supplied baseline; see RESULTS.json.
- Robolectric service tests use simulated media/Android behavior (including a Bluetooth compatibility shadow); they do **not** prove real decoding, audible output, wake behavior or coexistence with another app.
- No connected Android TV. No physical-TV install, D-pad pass, live azan trigger, real movie coexistence, standby/reboot acceptance, live JAKIM verification, live speed test or live AI validation is claimed.

## Install/update safety
- Debug/test APK, application ID `dev.farred.fayyadhos.tv.jarvis`, versionName `2.8-master`, versionCode `10` retained from the supplied source.
- **Do not uninstall the working launcher, clear its data or disable the stock launcher just to install this APK.** The new debug signing certificate may not match an installed copy, causing update rejection. A safe in-place update needs a build signed with the installed app's original key through your secure build process. Existing saved settings/API keys must not be sacrificed to bypass signature mismatch.
- Production distribution still needs original release signing and appropriate version increments. Signing keys, user audio and user credentials are not included in source.
- Test first with automatic azan OFF, then import/test audio and grant permissions. Verify a real prayer trigger, notification Stop, another app playing, reboot, network loss, timezone change and standby on the actual TV before relying on it.

## APK checksum
`dbb2ff949ccbc6720653c5dd05b764d5f9071ac69336858c39936452619ddd64`

Source includes complete buildable project, this handoff, azan changes patch and fresh verification evidence. Old reports/verification folders are preserved as history, not represented as fresh tests.
