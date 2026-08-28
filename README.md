# SpeedLab

[![Android Build](https://github.com/theneet0/SpeedTest-EXP/actions/workflows/android.yml/badge.svg)](https://github.com/theneet0/SpeedTest-EXP/actions/workflows/android.yml)

SpeedLab is a native, privacy-focused internet speed test for Android. It performs real application-layer HTTPS transfers, supports genuinely independent download-only and upload-only modes, and keeps settings and history on the device.

The project is optimized for Android 14 and newer while retaining a practical minSdk of 26. The default package is com.example.speedlab; the package, app name, built-in server catalog, and profile defaults are centralized and can be changed without redesigning the app.

## Download

The current installable beta APK, source archive, release notes, and SHA-256 checksums are available on the [SpeedLab v0.9.0 Beta release page](https://github.com/theneet0/SpeedTest-EXP/releases/tag/v0.9.0).

## Current feature set

- Real download-only, upload-only, and combined tests
- Native HTTPS streaming with controlled parallel connections
- Automatic latency-based server selection, manual selection, and custom HTTPS endpoints
- Current, average, peak, and final throughput values
- Median ping, consecutive-sample jitter, stage progress, traffic count, and selected server
- Lightweight custom Compose speedometer and live throughput graph
- Data Saver, Balanced, Accurate, and validated Custom profiles
- Mbps, MB/s, Kbps, and Gbps display units
- Wi-Fi, cellular, Ethernet, VPN, other, and disconnected network detection
- Optional warning before a metered or cellular test
- Room-backed local history with details, deletion, clearing, filters, sorting, and recent-result comparison
- Android Sharesheet support for plain text and JSON results
- CSV history export through FileProvider, without storage permission
- System, light, and dark themes
- User-facing errors plus an optional local diagnostics panel
- No advertising, analytics, trackers, account, or location permission

## Architecture

The project intentionally uses one Android application module and a small set of clear boundaries:

- ui: Compose screens, custom drawing, theme, and SpeedLabViewModel
- engine: the replaceable SpeedTestEngine contract, native HTTP implementation, server model, events, and measurement math
- network: modern ConnectivityManager monitoring without location access
- data: Preferences DataStore settings and Room history
- share: text, JSON, and scoped CSV sharing
- model: validated settings, profiles, unit conversion, traffic estimates, and pure calculations

AppContainer is a deliberately small application-level dependency container. It avoids a dependency-injection framework and keeps startup and APK size low. A future provider only needs to implement SpeedTestEngine or supply compatible SpeedServer endpoints; the ViewModel and UI consume engine events rather than provider-specific APIs.

## Speed-test engine

NativeHttpSpeedTestEngine uses HttpURLConnection over normal TLS. Download payloads are streamed into reusable memory buffers and discarded. Upload payloads use fixed-length streaming requests backed by reusable zero-filled buffers. No speed-test payload is written to disk and no large temporary upload file is created.

All transferred-byte counters are Long. Throughput timing uses System.nanoTime(), so wall-clock changes cannot corrupt a measurement. The sampler runs at 4–6 visible updates per second in the standard profiles. Parallel worker count, duration, timeout, sample interval, byte cap, and request chunk size are controlled by the selected profile.

Cancellation cancels the owning coroutine job and explicitly disconnects every tracked HttpURLConnection. The ViewModel rejects duplicate starts. It survives activity recreation and rotation; the active job remains in the ViewModel. There is no persistent foreground service in this version, so Android may stop a test if the entire process is removed while backgrounded.

### Upload-only behavior

Upload Only performs server discovery when automatic selection is enabled, five tiny latency probes, and an upload warm-up followed by the real upload measurement. It does not run a download throughput test or a download warm-up. Discovery and latency use only one-byte download responses plus normal HTTP/TLS overhead.

### Download-only behavior

Download Only performs discovery, latency, a download warm-up, and the real download measurement. It does not send a throughput-sized upload body.

### Final result calculation

The displayed live average includes samples observed during the measured stage. The final reported result uses a steady-state trimmed mean. It discards the first 20% of throughput samples to reduce connection ramp-up bias, sorts the remaining finite non-negative samples, trims the lowest and highest 10%, and averages the remainder.

Peak speed is the highest untrimmed interval sample. Ping is the median of five small HTTPS probes. Jitter is the mean absolute difference between consecutive probe latencies. This approach is deterministic and covered by unit tests.

## Servers and providers

Server configuration is centralized in engine/SpeedServer.kt. The first-party catalog currently contains Cloudflare's public anycast speed endpoint:

- download: https://speed.cloudflare.com/__down?bytes={bytes}
- upload: https://speed.cloudflare.com/__up

No Cloudflare SDK or code is bundled. Availability and use of the remote service remain subject to the provider's policies.

Automatic mode probes all configured reachable candidates with minimal requests and selects the lowest-latency result. Manual mode uses a selected built-in server. Custom mode requires a server name and two HTTPS URLs. The custom download URL may contain a literal {bytes} token; otherwise SpeedLab appends a bytes query parameter. The upload endpoint must accept a binary HTTPS POST body and return a 2xx status. Cleartext HTTP is intentionally rejected, Android cleartext traffic is disabled, redirects retain normal platform behavior, and certificate validation is never bypassed.

To add a provider with a different protocol, create another SpeedTestEngine implementation and bind it in AppContainer. Provider-specific server discovery should remain inside that engine so the UI does not acquire endpoint logic.

## Profiles and estimated traffic

| Profile | Per-direction duration | Connections | Total traffic cap |
|---|---:|---:|---:|
| Data Saver | 5 s | 2 | 24 MB |
| Balanced | 8 s | 4 | 160 MB |
| Accurate | 12 s | 8 | 512 MB |
| Custom | 2–60 s | 1–16 | 5–2048 MB |

The app shows the configured upper bound before a test and actual transferred bytes afterward. TLS, HTTP headers, retransmissions below the application layer, and discovery overhead mean carrier accounting can be slightly higher. A fast link can reach the byte cap before the time limit.

## Permissions

| Permission | Purpose |
|---|---|
| INTERNET | Perform the requested speed test |
| ACCESS_NETWORK_STATE | Detect availability, transport, metering, and network changes |

There is no location, phone-state, notification, advertising ID, or storage permission. CSV files are created in the application cache and exposed temporarily through a scoped FileProvider URI.

## Build requirements

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Gradle 8.9 (wrapper included)

Build and test from the repository root:

    ./gradlew clean
    ./gradlew testDebugUnitTest
    ./gradlew assembleDebug

The standard output is:

    app/build/outputs/apk/debug/app-debug.apk

The GitHub Actions workflow runs tests, builds the debug APK, renames it to SpeedLab-debug.apk, creates SpeedLab-source.zip, and uploads both in the SpeedLab-deliverables artifact. The debug APK uses the normal Android debug signing key and is intended for direct testing, not Play Store release.

## Tests

Pure JVM tests cover monotonic byte/time throughput calculations, Mbps/MB/s/Kbps/Gbps conversion, steady-state trimmed aggregation, median ping and jitter, profile traffic estimates, directional budgets, custom configuration bounds, and HTTPS server validation.

A test-only FakeSpeedTestEngine is included for deterministic ViewModel or UI test expansion. It is under src/test and cannot be packaged into a release APK.

## Dependencies and licenses

SpeedLab uses Kotlin, AndroidX Core, Jetpack Compose/Material 3, Lifecycle, Preferences DataStore, Room, JUnit, AndroidX Test, and the standard Android/Gradle toolchain. AndroidX libraries use the Apache License 2.0; Kotlin and Gradle retain their respective upstream licenses. No Ookla binary, private API, reverse-engineered protocol, chart library, analytics SDK, or provider SDK is included.

## Accuracy and known limitations

SpeedLab measures application-layer HTTPS throughput to the selected endpoint, not the modem's theoretical link rate. Results can vary with server load, routing, VPN overhead, radio conditions, TCP/TLS behavior, device power management, and traffic shaping. A single global anycast built-in provider limits cross-provider comparison until more vetted servers are added.

Packet loss is omitted because Android cannot provide a reliable, unprivileged, transport-independent loss value for these HTTPS flows without presenting a misleading metric. Background continuation is also omitted because a compliant Android 14 foreground service and persistent notification would add behavior and permissions that are unnecessary for a short, user-initiated test. Network changes during a test stop the run and produce a clear error instead of merging measurements from different transports.
