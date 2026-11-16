# Pinduoduo Русский Браузер — Android

Нативное Android-приложение открывает настоящий мобильный сайт Pinduoduo
(`https://mobile.yangkeduo.com/`) внутри WebView и переводит читаемый китайский
DOM-текст на русский с помощью Google ML Kit Translation на устройстве.
Приложение не делает снимки экрана, не использует OCR и не рисует экранный overlay.

## Возможности

- Навигация назад, вперёд, домой и обновить; индикатор загрузки и адрес сайта.
- JavaScript, DOM storage, cookies и third-party cookies для работы сайта.
- Перевод исходного текста и новых DOM-элементов через `MutationObserver`.
- Китайский → русский через ML Kit (`zh` → `ru`) и постоянный локальный кэш.
- Числа, цены, валюты и распространённые единицы измерения остаются в исходном виде.
- Изображения и URL ссылок не изменяются; приложение не подменяет каталог или товары.
- Внешние app-ссылки передаются Android. Перевод ограничен доменами Pinduoduo /
  Yangkeduo и не выполняется на посторонних сайтах.
- Для передачи текста используется ограниченный WebMessageListener: только основной
  frame и доверенные домены маркетплейса.

Первое использование ML Kit может потребовать интернета для загрузки китайской и
русской языковых моделей. После загрузки перевод текста выполняется на устройстве.
Интернет также нужен для самого сайта и его динамического содержимого.

## Сборка debug APK локально

Требуются JDK 17, Android SDK Platform 35, Build Tools 35.0.0 и Gradle 8.9.
Android Gradle Plugin в проекте — 8.7.3. Откройте эту папку в Android Studio либо
запустите команды из `native-android/`:

```sh
gradle --version
gradle :app:assembleDebug
```

Готовый APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

В корне GitHub-репозитория должны лежать:

```text
pinduoduo-russian-browser-source.zip
.github/workflows/build.yml
```

ZIP содержит `artifacts/pinduoduo-translator/native-android/` и копию workflow.
Сам workflow должен быть размещён отдельно в `.github/workflows/build.yml`, чтобы
GitHub Actions обнаружил его. Он распаковывает из ZIP только Android Gradle-проект,
поэтому не перезаписывает workflow в репозитории.

Запустите **Actions → Pinduoduo Russian Browser Android → Run workflow** или
отправьте commit с ZIP/workflow. Сборка использует JDK 17, Gradle 8.9 и Android SDK
35. Готовый APK сохраняется как Actions artifact
`pinduoduo-russian-browser-debug-apk` по пути:

```text
artifacts/pinduoduo-translator/native-android/app/build/outputs/apk/debug/app-debug.apk
```

При обновлении Android-кода пересоберите `pinduoduo-russian-browser-source.zip`,
чтобы архив и GitHub Actions собирали одну и ту же версию.

## Ограничения

- Переводятся доступные DOM-текст и выбранные текстовые атрибуты. Текст внутри
  изображений, canvas, видео и cross-origin iframe не распознаётся и не переводится.
- Длина русского текста может менять вёрстку; сайт может заменить переведённый DOM
  при собственном перерисовывании.
- Возможность входа, работы CAPTCHA, оформления заказа, оплаты, доставки и внешних
  приложений определяется Pinduoduo, регионом и установленными на телефоне
  приложениями. Работа этих функций не гарантируется.
- SSL-ошибки не обходятся. Если Android System WebView не поддерживает
  ограниченный WebMessageListener, сайт откроется, но перевод будет отключён.
- В рабочей среде Replit нет локальных JDK, Gradle и Android SDK, поэтому APK здесь
  не собран. Сборку нужно подтвердить в GitHub Actions или Android Studio.
