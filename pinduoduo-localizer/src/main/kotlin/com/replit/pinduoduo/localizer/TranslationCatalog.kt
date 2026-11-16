package com.replit.pinduoduo.localizer

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

interface TranslationCatalog {
    fun find(locale: LocaleCode, key: String): String?
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
    private val cache = ConcurrentHashMap<LocaleCode, Map<String, String>>()

    override fun find(locale: LocaleCode, key: String): String? =
        cache.computeIfAbsent(locale, ::load)[key]

    private fun load(locale: LocaleCode): Map<String, String> {
        val resourcePath = "i18n/${locale.tag}.properties"
        val properties = Properties()
        val stream = classLoader.getResourceAsStream(resourcePath) ?: return emptyMap()

        stream.use {
            InputStreamReader(it, StandardCharsets.UTF_8).use(properties::load)
        }

        return properties.stringPropertyNames().associateWith(properties::getProperty)
    }
}
