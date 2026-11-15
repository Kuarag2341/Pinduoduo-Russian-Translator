# Android MVP

This folder contains the installable Android application source. It targets Android 10 (API 29) and newer and uses Kotlin, Jetpack Compose, MediaProjection, a touch-through overlay, ML Kit Chinese OCR, and on-device Chinese-to-Russian translation.

For high-confidence OCR lines with a clean, mostly uniform background around the text, the overlay paints an opaque color patch inside the recognized text bounds and renders Russian there with adaptive sizing and wrapping. If the background is not confidently uniform, it uses the earlier semi-opaque label style. Lines with missing, low-confidence, implausible, or overlapping bounds are skipped rather than risking covering nearby controls; lines containing digits or currency symbols are also skipped to protect prices and numeric labels. Screen frames are reprocessed during scrolling, and the capture surface is resized when the display dimensions change.

## Build a debug APK

Open `native-android` in Android Studio with Android SDK 35 and JDK 17 installed, then run **Build > Build APK(s)**. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

If Gradle is installed separately, run `gradle :app:assembleDebug` from this directory.

The current Replit build environment does not include an Android SDK, JDK, or Gradle installation, so it cannot produce or validate an APK here. The source is ready for an Android Studio build.

## Privacy and permissions

- Android asks for screen-capture permission each time translation starts.
- Overlay permission is requested only when the user presses the start button.
- Screenshots are processed in memory on the device and are not uploaded.
- ML Kit downloads its Chinese-to-Russian translation model on first use; the model then works offline.
- The translation windows use `FLAG_NOT_TOUCHABLE`, so taps and gestures continue to reach Pinduoduo.
- Text replacement is a visual overlay, not a modification of Pinduoduo's native UI. Complex backgrounds and uncertain OCR boxes use the previous label style or are skipped.
- Translation cache is stored locally and is limited to 1,000 entries.
