# Pinduoduo Russian Browser (Android)

This native Android app opens Pinduoduo's live mobile shopping site in Android WebView. It translates Chinese DOM text in place with the on-device ML Kit Chinese-to-Russian model and the existing local translation cache. It does not create product, price, or translation mock data.

## Features

- Starts at `https://mobile.yangkeduo.com/`, the live mobile shopping site.
- WebView JavaScript, DOM storage, cookies, third-party cookies, and normal site navigation are enabled for shopping flows.
- Back, forward, home, refresh, loading progress, current host, SSL errors, safe browsing, and external app links are handled.
- A `MutationObserver` translates initial and dynamically inserted page text, including content added while scrolling; it also translates text-node-backed search placeholders and accessible labels.
- The AndroidX WebMessageListener bridge is origin-restricted to Pinduoduo/Yangkeduo domains and accepts messages only from the main frame.
- Existing ML Kit translation and local cache are used. The first translation may need an internet connection to download Google's Chinese-to-Russian model; later translation is on-device.
- Numeric values, currencies, and common product specification tokens are preserved verbatim while surrounding Chinese text is translated. Images and non-text page elements are left untouched.

## Build locally

Use Android Studio with JDK 17 and Android SDK 35. The project uses Android Gradle Plugin 8.7.3, which requires Gradle 8.9. If Gradle is installed:

```sh
gradle :app:assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

`.github/workflows/android-apk.yml` installs JDK 17, Gradle 8.9, and Android SDK 35, builds the debug APK, and uploads it as a workflow artifact. Run it with **Actions → Android APK → Run workflow**, or push a change affecting the Android project.

## Site access and limitations

- A network probe using an Android-style user agent received HTTP 200 and a populated shopping page from `mobile.yangkeduo.com`. This confirms the public endpoint responded to the probe; it is not a test inside Android WebView or on a phone.
- `m.pinduoduo.com/ru/` currently serves corporate information, not the marketplace, so the app opens the live mobile shopping domain instead.
- The site controls account availability, region, captcha/anti-bot checks, login, inventory, delivery, checkout, and accepted payment methods. This app cannot guarantee that Pinduoduo permits WebView login or that every order/payment path works; some flows may require Pinduoduo, WeChat, Alipay, or another installed app.
- Translation applies to readable DOM text and selected text attributes only. Text embedded in images, canvas, video, or cross-origin iframes is not translated. Layout can shift when Russian text is longer, and the site may overwrite DOM changes during client-side rerenders.
- If the installed Android System WebView lacks the origin-restricted message-listener feature, the real site still opens but automatic translation is disabled with a status message.
- SSL failures are canceled rather than bypassed. Non-HTTP links such as payment/app schemes are handed to Android; behavior depends on installed apps and the destination.
- This workspace has no local JDK, Gradle, Android SDK, emulator, or device, so the Android APK and actual WebView checkout/login flows cannot be tested here. GitHub Actions is configured but has not yet run.
