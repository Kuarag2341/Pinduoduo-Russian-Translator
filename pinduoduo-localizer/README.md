# Pinduoduo Localizer Core

Изолированный Kotlin/JVM-модуль первого этапа русификатора. Он содержит
локальные тестовые словари, выбор `ru`/`zh`, поиск перевода по ключу и единый
fallback на исходный текст.

Это не исходники Pinduoduo, не патч APK и не Android-приложение. Оригинальный
APK не используется модулем. Код пока не подключён к браузеру или приложению.

## Структура

- `src/main/kotlin/.../TranslationCatalog.kt` — чтение локальных `.properties`.
- `LocalizationModule.kt` — выбор языка и общий поиск с fallback.
- `AndroidI18nAdapter.kt` — тонкий адаптер для будущих Android resource keys.
- `WebViewI18nAdapter.kt` — тонкий адаптер для `module` / `key` / `defaultValue`.
- `src/main/resources/i18n/ru.properties` и `zh.properties` — небольшие
  демонстрационные словари; это тестовые переводы, не полный перевод магазина.
- `src/test/kotlin/.../LocalizationModuleTest.kt` — проверки переключения и
  fallback.

## Запуск тестов

Нужны JDK 17+ и Maven:

```sh
mvn test
```

Тестируется только JVM-модуль. Android Gradle-проект и APK не собираются.

## Будущая интеграция

В отдельной разрешённой Android-сборке `AndroidI18nAdapter` можно вызвать из
обёртки над Android I18n при запросе строки по имени ресурса. Для WebView
`WebViewI18nAdapter.getText(module, key, defaultValue)` соответствует будущему
локальному пути моста. Пока это чистые Kotlin-интерфейсы без Android SDK,
JavaScript-инъекций, сети и зависимости от внутренних классов Pinduoduo.

При отсутствии ключа или пустом переводе возвращается переданный оригинальный
текст. Перевод динамических произвольных текстов, товаров и отзывов здесь пока
не реализован: для них потребуется отдельный локальный источник переводов.
