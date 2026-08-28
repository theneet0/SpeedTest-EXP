# SpeedLab v0.9.0 Beta

This is the first public test release of SpeedLab, a lightweight native Android internet speed-test application focused on privacy, controlled traffic usage, and real independent transfer modes.

## Highlights

- Real Download Only, Upload Only, and Download + Upload tests
- Upload Only does not run a download throughput test
- Native HTTPS streaming with configurable parallel connections
- Automatic, manual, and custom HTTPS server selection
- Live speedometer, throughput graph, ping, jitter, average, and peak speed
- Data Saver, Balanced, Accurate, and Custom profiles
- Wi-Fi, cellular, Ethernet, and VPN detection with mobile-data warnings
- Local Room history, comparison, filters, CSV export, and text/JSON sharing
- System, light, and dark themes
- No advertising, analytics, trackers, account, or location permission

## Installation

Download **SpeedLab-v0.9.0-beta.apk** from the Assets section below and open it on your Android device. Android may ask you to allow installation from your browser or file manager.

The APK is debug-signed for direct testing. It is optimized for Android 14 and newer and supports Android 8.0 or newer.

## Technical notes

The built-in provider uses Cloudflare's public HTTPS speed endpoint. Custom compatible HTTPS download and upload endpoints can be configured in Settings. Packet loss is intentionally omitted because a reliable unprivileged measurement is not available across Android network transports. Persistent background testing is not enabled in this version.

The attached **SHA256SUMS.txt** file contains integrity hashes for both downloadable deliverables.
