package com.replit.pinduoduo.localizer

/**
 * Platform-neutral adapter for a future WebView bridge.
 * The caller supplies the original page text as defaultValue.
 */
class WebViewI18nAdapter(
    private val localization: LocalizationModule
) {
    fun getLanguage(): String = localization.currentLanguage()

    fun getText(module: String, key: String, defaultValue: String): String {
        val safeModule = module.trim()
        val safeKey = key.trim()
        if (safeModule.isEmpty() || safeKey.isEmpty()) return defaultValue

        return localization.translate("$safeModule.$safeKey", defaultValue)
    }
}
