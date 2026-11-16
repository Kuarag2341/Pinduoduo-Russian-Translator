package com.replit.pinduoduo.localizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalizationModuleTest {
    private fun module(
        initialLanguage: String = "ru",
        russian: Map<String, String> = mapOf(
            "native.search" to "Поиск",
            "web.product.material" to "Материал",
            "web.empty.translation" to ""
        ),
        chinese: Map<String, String> = mapOf(
            "native.search" to "搜索",
            "web.product.material" to "材质"
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
        val localization = module()

        assertEquals("Поиск", localization.translate("native.search", "搜索"))
    }

    @Test
    fun `missing key always returns original text`() {
        val localization = module()

        assertEquals(
            "发货地",
            localization.translate("web.product.unknown", "发货地")
        )
    }

    @Test
    fun `empty translation always returns original text`() {
        val localization = module()

        assertEquals(
            "原始文本",
            localization.translate("web.empty.translation", "原始文本")
        )
    }

    @Test
    fun `switches between Russian and Chinese catalogs`() {
        val localization = module(initialLanguage = "zh-CN")

        assertEquals("zh", localization.currentLanguage())
        assertEquals("搜索", localization.translate("native.search", "搜索"))

        assertTrue(localization.selectLanguage("ru"))
        assertEquals("ru", localization.currentLanguage())
        assertEquals("Поиск", localization.translate("native.search", "搜索"))

        assertTrue(localization.selectLanguage("zh"))
        assertEquals("搜索", localization.translate("native.search", "搜索"))
    }

    @Test
    fun `unsupported language is rejected without changing current locale`() {
        val localization = module(initialLanguage = "ru")

        assertFalse(localization.selectLanguage("fr"))
        assertEquals("ru", localization.currentLanguage())
        assertEquals("Поиск", localization.translate("native.search", "搜索"))
    }

    @Test
    fun `WebView adapter uses default text for missing keys`() {
        val localization = module()
        val webViewI18n = WebViewI18nAdapter(localization)

        assertEquals(
            "原始标签",
            webViewI18n.getText("product", "not_in_catalog", "原始标签")
        )
        assertEquals(
            "Материал",
            webViewI18n.getText("product", "material", "材质")
        )
        assertEquals(
            "原始标签",
            webViewI18n.getText("", "material", "原始标签")
        )
    }

    @Test
    fun `loads UTF-8 catalogs from local resources`() {
        val localization = LocalizationModule(PropertiesTranslationCatalog())
        assertTrue(localization.selectLanguage("ru"))

        assertEquals("Поиск", localization.translate("native.search", "搜索"))
        assertEquals("Отправка из", localization.translate("web.product.shipping_from", "发货地"))
    }
}
