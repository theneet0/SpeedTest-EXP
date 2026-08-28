# SpeedLab

SpeedLab is a lightweight native Android internet speed-test application for Android 14 and newer. The project uses Kotlin, Jetpack Compose, and Material 3.

This first bootstrap build establishes the Android project and automated APK packaging. The functional speed-test engine, settings, history, sharing, and diagnostics are implemented in subsequent source revisions.

## Build

Use JDK 17 and Android SDK 35, then run `./gradlew testDebugUnitTest assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
