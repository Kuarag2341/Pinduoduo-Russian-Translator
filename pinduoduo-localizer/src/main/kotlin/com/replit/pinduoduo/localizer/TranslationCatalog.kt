package com.replit.pinduoduo.localizer

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

interface TranslationCatalog {
    fun find(locale: LocaleCode, key: String): String?

    fun entries(locale: LocaleCode, section: DictionarySection): Map<String, String> = emptyMap()

    fun hasSection(locale: LocaleCode, section: DictionarySection): Boolean = false

    fun sourceText(key: String): String? = find(LocaleCode.CHINESE, key)
}

class MapTranslationCatalog(
    translations: Map<LocaleCode, Map<String, String>>
) : TranslationCatalog {
    private val entries = translations.mapValues { (_, values) -> values.toMap() }

    override fun find(locale: LocaleCode, key: String): String? =
        entries[locale]?.get(key)
}

/**
 * Loads UTF-8 .properties catalogs from the classpath:
 * i18n/ru.properties and i18n/zh.properties.
 */
class PropertiesTranslationCatalog(
    private val classLoader: ClassLoader =
        Thread.currentThread().contextClassLoader
            ?: PropertiesTranslationCatalog::class.java.classLoader
) : TranslationCatalog {
    private val cache =
        ConcurrentHashMap<Pair<LocaleCode, DictionarySection>, Map<String, String>>()

    override fun find(locale: LocaleCode, key: String): String? {
        val section = DictionarySection.fromKey(key) ?: return null
        return entries(locale, section)[key]
    }

    override fun entries(
        locale: LocaleCode,
        section: DictionarySection
    ): Map<String, String> =
        cache.computeIfAbsent(locale to section) { load(locale, section) }

    override fun hasSection(locale: LocaleCode, section: DictionarySection): Boolean =
        classLoader.getResource(resourcePath(locale, section)) != null

    private fun load(
        locale: LocaleCode,
        section: DictionarySection
    ): Map<String, String> {
        val properties = Properties()
        val stream = classLoader.getResourceAsStream(resourcePath(locale, section))
            ?: return emptyMap()

        stream.use {
            InputStreamReader(it, StandardCharsets.UTF_8).use(properties::load)
        }

        return properties.stringPropertyNames().associateWith(properties::getProperty)
    }

    private fun resourcePath(locale: LocaleCode, section: DictionarySection): String =
        "i18n/${locale.tag}/${section.fileName}.properties"
}
