# Jarvis GroqCloud backend

## Status and scope

This backend adds a real Groq HTTP request path, not a local model or canned fallback.
There was no backend in the supplied archive. Node 22+ standard library only; no npm install.
The existing Android **Read again** button first reads the native network report (if voice output is enabled), then requests a short AI explanation. The AI result uses the existing Internet-result text region. No new panel, input, keyboard, speech recognition or microphone permission is added.

**Not live-verified in the repair environment:** no Groq key, deployed HTTPS host or TV connection was supplied. Automated provider-success/error tests use explicitly marked fixtures. They are not evidence of successful Groq inference.

## Server configuration

Set secrets using your hosting platform's secret store/environment, not Android or Git:

- `GROQ_API_KEY`: provider key, server only.
- `JARVIS_DEVICE_TOKEN`: at least 32 cryptographically random characters, unique to this TV/deployment. This is a restricted backend-access credential, **not** the Groq key. Provision its matching value to the TV. Rotate if compromised.
- `GROQ_MODEL`: optional; default `llama-3.1-8b-instant`.
- `PORT`: optional; default 8080.

Start: `node backend/server.mjs`.

It binds **127.0.0.1**. Publish it through your existing trusted HTTPS reverse proxy with a publicly valid certificate. Do not expose loopback HTTP directly or disable certificate validation. Route **POST /api/jarvis** to this process. Use a 2 KiB body cap, a 10-second client-body timeout, and an upstream timeout of at least 15 seconds at the proxy. Do not log Authorization headers, request bodies or secret environment values. Do not enable open CORS; it is unnecessary for a native Android client. Keep process secrets out of crash reporting.

The backend validates the configured model against Groq's actual `/openai/v1/models` list before inference and caches successful validation for one hour. It fails clearly if unavailable; it does not quietly switch models. The default was listed in official Groq production-model documentation when inspected; account-specific availability still requires this authenticated check.

One authenticated TV, one in-flight request, six attempts/minute. A proxy-level connection/request limit is also recommended against unauthenticated flooding. A fleet requires per-device credentials and distributed quotas; the intentionally small in-process limiter is not a fleet gateway.

## Provision the TV without typing on it

### Managed production TVs

Use the existing EMM/device-policy controller's **managed application configuration** for package `dev.farred.fayyadhos.tv.jarvis`:

- `jarvis_endpoint`: full URL such as `https://YOUR-HOST/api/jarvis` (replace YOUR-HOST).
- `jarvis_device_token`: matching restricted device token.

The app declares these restrictions in `res/xml/jarvis_restrictions.xml`. The EMM/device owner, not this app, writes the restrictions. They take priority over private preferences. The provider key must never be provisioned to Android.

### Debug-only unmanaged-TV verification

For a debug build, an authorized administrator can use ADB `run-as` to write app-private `shared_prefs/jarvis-backend.xml` **while the application is stopped**. Create an XML preferences map locally with string entries `endpoint` and `device_token`, XML-escaping both values, then pipe it to the package's private file through `adb shell run-as dev.farred.fayyadhos.tv.jarvis`. Create `shared_prefs` first if needed. Do not put the token in shell command arguments, shell history, screenshots or reports. Restrict the provisioning file's permissions and delete it after use. Restart the app afterward.

`run-as` is unavailable on a normal release build. **Unmanaged release TVs need an approved private provisioning mechanism before deployment.** None existed in the supplied source; no exported setup receiver, new text field, hardcoded token or insecure public configuration file was added as a workaround. Do not root or weaken the TV's security to provision it. Agree on managed provisioning or a secure provisioning flow before building a production rollout.

## Data and behavior

Only the predefined `explain_network` action is accepted. The server independently creates its prompts. Context is allowlisted: transport category, Android validation boolean, VPN transport boolean, Wi-Fi link Mbps, and an optional earlier user-triggered measured download estimate. No SSID, IP address, serial, location, installed-app list or free-form prompt is sent.

The download measurement is explicitly labelled earlier/estimated, not a live reading. No upload is measured. AI cannot launch apps or change Android settings. Native app launching, Wi-Fi settings, prayer times and weather remain native/existing flows and never depend on Groq. There is no AI polling or automatic AI invocation on rendering.

Provider errors are never copied to the client; responses use safe codes. Failures never return synthetic successful AI text. Requests have a shared 12-second server deadline; Android has bounded connect/read timeouts, bounded bodies and refuses redirects/cleartext URLs. Closing Jarvis cancels its transport/future, shuts down its executor/TTS, removes callbacks, and guards late UI updates.

## Verification

- `node --test backend/server.test.mjs`: HTTP tests with mocked provider transport, no billable calls.
- `node backend/live-check.mjs`: **real** models-list and inference calls using server environment secrets. It refuses to run without credentials. Records safe response metadata/text; do not substitute fixture output for this evidence.
- After HTTPS deployment and device provisioning, select **Jarvis → Read again** using the TV remote. Check the real provider output appears in the existing region. Test a missing key, invalid key, provider timeout/outage, rapid repeat presses, and close/reopen while loading. Restore valid credentials after deliberate failure tests.

A backend-only live check does not prove TV-to-backend HTTPS/focus behavior. Both must be tested separately.

Official provider references: https://console.groq.com/docs/models and https://console.groq.com/docs/api-reference.
