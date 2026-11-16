package com.replit.pinduoduo.localizer.android

import android.content.Context
import com.replit.pinduoduo.localizer.DictionarySection
import com.replit.pinduoduo.localizer.LocaleCode
import com.replit.pinduoduo.localizer.TranslationCatalog
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads the existing pinduoduo-localizer dictionaries from packaged Android assets.
 * Missing assets return empty catalog data so the core can use its source-text fallback.
 */
class AndroidAssetTranslationCatalog internal constructor(
    private val openAsset: (String) -> InputStream?
) : TranslationCatalog {

    constructor(context: Context) : this({ path ->
        try {
            context.applicationContext.assets.open(path)
        } catch (_: IOException) {
            null
        }
    })

    private val cache =
        ConcurrentHashMap<Pair<LocaleCode, DictionarySection>, Map<String, String>>()
    private val provenance: Map<String, String> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadProperties("i18n/provenance.properties")
    }

    override fun find(locale: LocaleCode, key: String): String? {
        val section = DictionarySection.fromKey(key) ?: return null
        return entries(locale, section)[key]
    }

    override fun entries(
        locale: LocaleCode,
        section: DictionarySection
    ): Map<String, String> =
        cache.computeIfAbsent(locale to section) {
            loadProperties("i18n/${locale.tag}/${section.fileName}.properties")
        }

    override fun hasSection(locale: LocaleCode, section: DictionarySection): Boolean =
        openAsset("i18n/${locale.tag}/${section.fileName}.properties")
            ?.use { true }
            ?: false

    override fun resourceName(key: String): String? = provenance[key]

    private fun loadProperties(path: String): Map<String, String> {
        val stream = try {
            openAsset(path)
        } catch (_: IOException) {
            null
        } ?: return emptyMap()

        val properties = Properties()
        stream.use {
            InputStreamReader(it, StandardCharsets.UTF_8).use(properties::load)
        }
        return properties.stringPropertyNames().associateWith(properties::getProperty)
    }
}
