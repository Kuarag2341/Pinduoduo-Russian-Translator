package com.replit.pinduoduo.localizer.android

/**
 * Local lookup facade for a host-owned WebView translation bridge.
 *
 * This class is not injected into any WebView and is not exposed as a JavaScript
 * interface. The app with authorized source code must decide what page content
 * is eligible and invoke this adapter from its own bridge.
 */
class WebViewI18nAdapter(
    private val localization: AndroidLocalization
) {
    fun getLanguage(): String = localization.currentLanguage()

    fun getText(module: String, key: String, defaultValue: String): String {
        val safeModule = module.trim()
        val safeKey = key.trim()
        if (safeModule.isEmpty() || safeKey.isEmpty()) return defaultValue
        return localization.translate("$safeModule.$safeKey", defaultValue)
    }
}
