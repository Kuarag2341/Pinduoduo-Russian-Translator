package com.replit.pinduoduo.localizer

class LocalizationModule(
    private val catalog: TranslationCatalog,
    initialLanguage: String = LocaleCode.CHINESE.tag
) {
    private val localeController = LocaleController(initialLanguage)

    fun currentLanguage(): String = localeController.current().tag

    fun selectLanguage(languageTag: String): Boolean =
        localeController.select(languageTag)

    fun translate(key: String, originalText: String): String {
        if (key.isBlank()) return originalText

        val localizedText = catalog.find(localeController.current(), key)
        return localizedText
            ?.takeIf(String::isNotBlank)
            ?: originalText
    }
}
