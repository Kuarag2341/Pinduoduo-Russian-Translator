package com.replit.pinduoduo.localizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalizationModuleTest {
    private fun module(
        initialLanguage: String = "ru",
        russian: Map<String, String> = mapOf(
            "home.tab.home" to "Главная",
            "products.reviews.title" to "Отзывы о товаре",
            "system.test.empty" to ""
        ),
        chinese: Map<String, String> = mapOf(
            "home.tab.home" to "首页",
            "products.reviews.title" to "商品评价",
            "system.test.empty" to "原始系统文本"
        )
    ): LocalizationModule = LocalizationModule(
        catalog = MapTranslationCatalog(
            mapOf(
                LocaleCode.RUSSIAN to russian,
                LocaleCode.CHINESE to chinese
            )
        ),
        initialLanguage = initialLanguage
    )

    @Test
    fun `returns a Russian translation for a known key`() {
        assertEquals("Главная", module().translate("home.tab.home", "首页"))
    }

    @Test
    fun `missing key always returns original text`() {
        assertEquals(
            "未知的商品字段",
            module().translate("attributes.not_verified", "未知的商品字段")
        )
    }

    @Test
    fun `empty translation always returns source text`() {
        assertEquals(
            "原始系统文本",
            module().translate("system.test.empty", "原始系统文本")
        )
    }

    @Test
    fun `source catalog supplies Chinese fallback when caller text is empty`() {
        val localization = LocalizationModule(
            catalog = MapTranslationCatalog(
                mapOf(
                    LocaleCode.RUSSIAN to emptyMap(),
                    LocaleCode.CHINESE to mapOf("products.reviews.title" to "商品评价")
                )
            ),
            initialLanguage = "ru"
        )

        assertEquals("商品评价", localization.translate("products.reviews.title", ""))
    }

    @Test
    fun `switches between Russian and Chinese catalogs`() {
        val localization = module(initialLanguage = "zh-CN")

        assertEquals("zh", localization.currentLanguage())
        assertEquals("首页", localization.translate("home.tab.home", "首页"))

        assertTrue(localization.selectLanguage("ru"))
        assertEquals("ru", localization.currentLanguage())
        assertEquals("Главная", localization.translate("home.tab.home", "首页"))

        assertTrue(localization.selectLanguage("zh"))
        assertEquals("首页", localization.translate("home.tab.home", "首页"))
    }

    @Test
    fun `unsupported language is rejected without changing current locale`() {
        val localization = module(initialLanguage = "ru")

        assertFalse(localization.selectLanguage("fr"))
        assertEquals("ru", localization.currentLanguage())
        assertEquals("Главная", localization.translate("home.tab.home", "首页"))
    }

    @Test
    fun `WebView adapter uses default text for missing keys`() {
        val webViewI18n = WebViewI18nAdapter(module())

        assertEquals(
            "未知标签",
            webViewI18n.getText("products", "not_in_catalog", "未知标签")
        )
        assertEquals(
            "Отзывы о товаре",
            webViewI18n.getText("products", "reviews.title", "商品评价")
        )
        assertEquals(
            "原始标签",
            webViewI18n.getText("", "reviews.title", "原始标签")
        )
    }

    @Test
    fun `all section resources exist for Russian and Chinese`() {
        val catalog = PropertiesTranslationCatalog()

        DictionarySection.entries.forEach { section ->
            assertTrue(catalog.hasSection(LocaleCode.RUSSIAN, section), "Missing ru/${section.fileName}.properties")
            assertTrue(catalog.hasSection(LocaleCode.CHINESE, section), "Missing zh/${section.fileName}.properties")
        }

        assertTrue(catalog.entries(LocaleCode.RUSSIAN, DictionarySection.CART).isEmpty())
        assertTrue(catalog.entries(LocaleCode.CHINESE, DictionarySection.CART).isEmpty())
        assertTrue(catalog.entries(LocaleCode.RUSSIAN, DictionarySection.ATTRIBUTES).isEmpty())
        assertTrue(catalog.entries(LocaleCode.CHINESE, DictionarySection.ATTRIBUTES).isEmpty())
    }

    @Test
    fun `all section keys are unique and every Russian key has a Chinese source`() {
        val catalog = PropertiesTranslationCatalog()

        listOf(LocaleCode.RUSSIAN, LocaleCode.CHINESE).forEach { locale ->
            val keys = DictionarySection.entries.flatMap { section ->
                catalog.entries(locale, section).keys
            }
            assertEquals(keys.size, keys.toSet().size, "Duplicate $locale dictionary key")
        }

        DictionarySection.entries.forEach { section ->
            assertEquals(
                catalog.entries(LocaleCode.CHINESE, section).keys,
                catalog.entries(LocaleCode.RUSSIAN, section).keys,
                "ru/zh keys differ in ${section.fileName}"
            )
        }
    }

    @Test
    fun `every dictionary row has a unique confirmed APK resource reference`() {
        val catalog = PropertiesTranslationCatalog()
        val keys = DictionarySection.entries.flatMap { section ->
            catalog.entries(LocaleCode.CHINESE, section).keys
        }
        val resourceNames = keys.map { key ->
            assertNotNull(catalog.resourceName(key), "Missing APK resource reference for $key")
            catalog.resourceName(key)!!
        }

        assertEquals(resourceNames.size, resourceNames.toSet().size)
    }

    @Test
    fun `all dictionary strings are nonblank and use valid UTF-8 Cyrillic in Russian`() {
        val catalog = PropertiesTranslationCatalog()
        val russianEntries = DictionarySection.entries.flatMap { section ->
            catalog.entries(LocaleCode.RUSSIAN, section).entries
        }
        val chineseEntries = DictionarySection.entries.flatMap { section ->
            catalog.entries(LocaleCode.CHINESE, section).entries
        }

        assertTrue(russianEntries.isNotEmpty())
        assertTrue(russianEntries.all { it.key.isNotBlank() && it.value.isNotBlank() })
        assertTrue(chineseEntries.all { it.key.isNotBlank() && it.value.isNotBlank() })
        assertTrue(russianEntries.any { (_, value) -> value.any { it in 'А'..'я' } })
        assertTrue(chineseEntries.any { (_, value) -> value.any { it in '\u4E00'..'\u9FFF' } })
        assertNotNull(catalog.find(LocaleCode.RUSSIAN, "categories.title"))
    }

    @Test
    fun `every populated section resolves Russian and Chinese entries`() {
        val catalog = PropertiesTranslationCatalog()
        val russian = LocalizationModule(catalog, "ru")
        val chinese = LocalizationModule(catalog, "zh")

        DictionarySection.entries.forEach { section ->
            val ruEntries = catalog.entries(LocaleCode.RUSSIAN, section)
            val zhEntries = catalog.entries(LocaleCode.CHINESE, section)

            ruEntries.forEach { (key, translated) ->
                val source = zhEntries.getValue(key)
                assertEquals(translated, russian.translate(key, source), key)
                assertEquals(source, chinese.translate(key, "unused fallback"), key)
            }
        }
    }

    @Test
    fun `Russian format placeholders match the Chinese source in every section`() {
        val catalog = PropertiesTranslationCatalog()
        val formatToken = Regex("%(?:\\d+\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?[tT]?[a-zA-Z%]")

        DictionarySection.entries.forEach { section ->
            val ruEntries = catalog.entries(LocaleCode.RUSSIAN, section)
            val zhEntries = catalog.entries(LocaleCode.CHINESE, section)

            ruEntries.forEach { (key, translated) ->
                val source = zhEntries.getValue(key)
                assertEquals(
                    formatToken.findAll(source).map { it.value }.toList(),
                    formatToken.findAll(translated).map { it.value }.toList(),
                    "format tokens differ for $key"
                )
            }
        }
    }

    @Test
    fun `quantity and string parameters are preserved when formatting`() {
        val localization = LocalizationModule(PropertiesTranslationCatalog())
        assertTrue(localization.selectLanguage("ru"))

        assertEquals(
            "Показать ещё 3 товаров",
            localization.format(
                "orders.goods_more",
                "查看剩余%1\$s件商品",
                "3"
            )
        )
        assertEquals(
            "В избранное добавили 17 чел.",
            localization.format(
                "favorites.goods.count",
                "%1\$s人收藏",
                "17"
            )
        )
        assertEquals(
            "Посылок в заказе: 2",
            localization.format(
                "delivery.package_count",
                "该订单共%1\$s个包裹",
                "2"
            )
        )
    }

    @Test
    fun `malformed localized format falls back to formatted Chinese source`() {
        val localization = module(
            russian = mapOf("orders.count" to "Товаров: %1\$d"),
            chinese = mapOf("orders.count" to "商品数量：%1\$s")
        )
        assertTrue(localization.selectLanguage("ru"))

        assertEquals(
            "商品数量：3",
            localization.format("orders.count", "商品数量：%1\$s", "3")
        )
    }

    @Test
    fun `loads sectioned UTF-8 dictionaries from local resources`() {
        val localization = LocalizationModule(PropertiesTranslationCatalog())
        assertTrue(localization.selectLanguage("ru"))
        val androidI18n = AndroidI18nAdapter(localization)
        val webViewI18n = WebViewI18nAdapter(localization)

        assertEquals("Главная", androidI18n.getString("home.tab.home", "首页"))
        assertEquals(
            "Отследить доставку",
            webViewI18n.getText("delivery", "track", "查看物流")
        )
    }
}
