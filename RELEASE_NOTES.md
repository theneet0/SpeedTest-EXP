# SpeedLab v0.9.1 Beta

SpeedLab v0.9.1 is an ARM64 preview release focused on a complete Material 3 redesign and a cleaner release pipeline.

## Highlights

- Canonical Material 3 components throughout the test, history, and settings screens
- Material You dynamic color on Android 12 and newer
- New center-aligned app bar, Material navigation bar, icons, cards, chips, dialogs, and progress-ring speed display
- Edge-to-edge Android layout with system, light, and dark appearance modes
- ARM64-v8a-only APK output with no x86, x86-64, or 32-bit ARM native libraries
- Real Download Only, Upload Only, and Download + Upload tests
- Live throughput, average, peak, ping, jitter, progress, graph, traffic, and diagnostics
- Automatic GitHub prerelease publication after a new app version passes tests and builds successfully

## Installation

Download **SpeedLab-v0.9.1-arm64-v8a-beta.apk** from the Assets section and open it on an ARM64 Android device. Android may ask you to allow installation from your browser or file manager.

The APK is debug-signed for direct testing. It is optimized for Android 14 and newer and supports Android 8.0 or newer.

## Provider status

This preview retains the existing native HTTPS provider while the requested Speedtest.net migration is being resolved. SpeedLab does not contain an unlicensed Ookla SDK, reverse-engineered Speedtest.net endpoint, or redistributed Ookla CLI binary. A fully integrated Speedtest.net build requires licensed Ookla SDK materials; alternatively, the app can hand off to the official Speedtest app or website without importing its results.

## Technical notes

Packet loss is intentionally omitted because a reliable unprivileged measurement is not available across Android network transports. Persistent background testing is not enabled in this version. The attached **SHA256SUMS.txt** file contains integrity hashes for both downloadable deliverables.
