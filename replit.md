# Pinduoduo Русский Браузер

Нативное Android-приложение открывает мобильный Pinduoduo в WebView и переводит
китайский DOM-текст на русский с помощью ML Kit на устройстве.

## Run & Operate

- `pnpm --filter @workspace/api-server run dev` — run the API server (port 5000)
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)

### Native Android APK

- Android source: `artifacts/pinduoduo-translator/native-android/`
- Build locally with JDK 17, Gradle 8.9 and Android SDK 35:
  `cd artifacts/pinduoduo-translator/native-android && gradle :app:assembleDebug`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- GitHub Actions workflow: `.github/workflows/build.yml`
- Source archive: `generated-artifacts/pinduoduo-russian-browser-source.zip`

## Stack

- pnpm workspaces, Node.js 24, TypeScript 5.9
- API: Express 5
- DB: PostgreSQL + Drizzle ORM
- Validation: Zod (`zod/v4`), `drizzle-zod`
- API codegen: Orval (from OpenAPI spec)
- Build: esbuild (CJS bundle)

## Where things live

- `artifacts/pinduoduo-translator/native-android/` — installable Kotlin/Jetpack Compose app
- `artifacts/pinduoduo-translator/app/` — Replit Expo preview for the same product
- `.github/workflows/build.yml` — extracts the source ZIP and builds the debug APK
- `artifacts/pinduoduo-translator/native-android/README.md` — Android features, build steps and limitations

## Architecture decisions

- The Kotlin Android Gradle project is the source for the installable APK; the Expo
  artifact is a Replit preview and does not replace the native WebView build.
- Pinduoduo page text is translated through an origin-restricted JavaScript bridge;
  screenshots, OCR, screen capture and accessibility overlays are not used.
- ML Kit translates on-device after the language model is downloaded; translations
  are stored in a local cache.
- Product images, links, prices, numbers and currency tokens remain unchanged.

## Product

The browser opens the live Pinduoduo mobile site, translates readable Chinese DOM
text into Russian, supports page navigation and preserves the marketplace's
non-text content. Login, checkout, payment and ordering depend on Pinduoduo and
cannot be guaranteed.

## Gotchas

- GitHub Actions expects `pinduoduo-russian-browser-source.zip` at the repository
  root and the workflow at `.github/workflows/build.yml`.
- Rebuild the ZIP after changing native Android sources; the workflow extracts
  only `artifacts/pinduoduo-translator/native-android/`.
- APK builds and real WebView login/checkout flows have not been verified in this
  Replit workspace because JDK, Gradle and Android SDK are unavailable here.

## Pointers

- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details
