# Android integration module

Самостоятельный Android Library Module для существующего Kotlin-модуля
`pinduoduo-localizer`. Он подключает исходники ядра и словари по относительным
путям Gradle, не копируя их и не изменяя APK.

## Состав

- `localizer-android/` — Android-библиотека (`compileSdk 35`, `minSdk 23`).
- `AndroidAssetTranslationCatalog` — читает общие `i18n/ru` и `i18n/zh` из assets.
- `AndroidLocalization` — хранит выбор `ru`/`zh` в `SharedPreferences` и вызывает
  существующее ядро перевода.
- `AndroidI18nAdapter` — переводит исходную строку либо строковый ресурс,
  переданный хост-приложением.
- `WebViewI18nAdapter` — локальный интерфейс поиска для будущего мостика хостового
  WebView. Он сам не подключается к WebView и не создаёт JavaScript-интерфейс.

## Сборка и тесты

Нужны JDK 17+, Android SDK 35 и Gradle 8.9. В корне этого проекта:

```sh
./gradlew :localizer-android:assembleDebug
./gradlew :localizer-android:testDebugUnitTest
```

При установленном Gradle 8.9 без wrapper команда эквивалентна:

```sh
gradle -p pinduoduo-localizer/android-integration \
  :localizer-android:assembleDebug \
  :localizer-android:testDebugUnitTest
```

## Граница интеграции

Это библиотека и проверяемые адаптеры, а не подключение к Pinduoduo. Для
интеграции нужны разрешённые исходники хост-приложения: вызов
`AndroidI18nAdapter` в точках получения текста и явное решение, какие ключи
использовать. Для WebView потребуются исходники хоста и его собственный
безопасный мост; этот модуль не внедряется в страницы Pinduoduo и не меняет
их DOM. Авторизация, платежи, серверная логика, APK и механизмы защиты не
затрагиваются.
