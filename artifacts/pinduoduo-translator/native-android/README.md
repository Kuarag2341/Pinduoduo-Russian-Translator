# Android MVP

This folder contains the installable Android application source. It targets Android 10 (API 29) and newer and uses Kotlin, Jetpack Compose, MediaProjection, a touch-through overlay, ML Kit Chinese OCR, and on-device Chinese-to-Russian translation.

## Build a debug APK

Open `native-android` in Android Studio with Android SDK 35 and JDK 17 installed, then run **Build > Build APK(s)**. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

If Gradle is installed separately, run `gradle :app:assembleDebug` from this directory.

The current Replit build environment does not include an Android SDK or Gradle installation, so it cannot produce or validate an APK here. The source is ready for an Android Studio build.

## Privacy and permissions

- Android asks for screen-capture permission each time translation starts.
- Overlay permission is requested only when the user presses the start button.
- Screenshots are processed in memory on the device and are not uploaded.
- ML Kit downloads its Chinese-to-Russian translation model on first use; the model then works offline.
- The translation windows use `FLAG_NOT_TOUCHABLE`, so taps and gestures continue to reach Pinduoduo.
- Translation cache is stored locally and is limited to 1,000 entries.
