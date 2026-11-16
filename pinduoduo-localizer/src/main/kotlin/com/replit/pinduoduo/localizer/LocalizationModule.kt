package com.replit.pinduoduo.localizer

import java.util.IllegalFormatException
import java.util.Locale

class LocalizationModule(
    private val catalog: TranslationCatalog,
    initialLanguage: String = LocaleCode.CHINESE.tag
) {
    private val localeController = LocaleController(initialLanguage)

    fun currentLanguage(): String = localeController.current().tag

    fun selectLanguage(languageTag: String): Boolean =
        localeController.select(languageTag)

    fun translate(key: String, originalText: String): String {
        if (key.isBlank()) return originalText.ifBlank { key }

        val localizedText = catalog.find(localeController.current(), key)
        return localizedText?.takeIf(String::isNotBlank)
            ?: originalText.takeIf(String::isNotBlank)
            ?: catalog.sourceText(key).orEmpty()
    }

    /**
     * Formats a translated template only when it preserves the source template's
     * format tokens. On a malformed translation, formats the original pattern.
     */
    fun format(key: String, originalPattern: String, vararg arguments: Any?): String {
        val translatedPattern = translate(key, originalPattern)
        val selectedPattern = if (formatTokens(translatedPattern) == formatTokens(originalPattern)) {
            translatedPattern
        } else {
            originalPattern
        }

        return try {
            String.format(Locale.ROOT, selectedPattern, *arguments)
        } catch (_: IllegalFormatException) {
            try {
                String.format(Locale.ROOT, originalPattern, *arguments)
            } catch (_: IllegalFormatException) {
                originalPattern
            }
        }
    }

    private fun formatTokens(pattern: String): List<String> =
        FORMAT_TOKEN.findAll(pattern).map { it.value }.toList()

    private companion object {
        val FORMAT_TOKEN =
            Regex("%(?:\\d+\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?[tT]?[a-zA-Z%]")
    }
}
