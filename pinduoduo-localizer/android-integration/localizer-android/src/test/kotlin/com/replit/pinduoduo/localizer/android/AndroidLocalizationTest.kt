package com.replit.pinduoduo.localizer.android

import com.replit.pinduoduo.localizer.DictionarySection
import com.replit.pinduoduo.localizer.LocaleCode
import com.replit.pinduoduo.localizer.MapTranslationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

class AndroidLocalizationTest {
    private val resourceLoader = javaClass.classLoader
    private val catalog = AndroidAssetTranslationCatalog { path ->
        resourceLoader?.getResourceAsStream(path)
    }

    @Test
    fun `loads existing UTF-8 dictionaries from Android asset paths`() {
        assertTrue(catalog.hasSection(LocaleCode.RUSSIAN, DictionarySection.HOME))
        assertTrue(catalog.hasSection(LocaleCode.CHINESE, DictionarySection.HOME))
        assertEquals("Главная", catalog.find(LocaleCode.RUSSIAN, "home.tab.home"))
        assertEquals("首页", catalog.find(LocaleCode.CHINESE, "home.tab.home"))
        assertNotNull(catalog.resourceName("home.tab.home"))
    }

    @Test
    fun `persists supported locale changes and rejects unsupported tags`() {
        val store = MemoryLocalePreferenceStore("zh")
        val localization = AndroidLocalization(catalog, store)

        assertEquals("zh", localization.currentLanguage())
        assertTrue(localization.selectLanguage("ru-RU"))
        assertEquals("ru", localization.currentLanguage())
        assertEquals("ru", store.readLanguageTag())
        assertEquals("Главная", localization.translate("home.tab.home", "首页"))

        assertFalse(localization.selectLanguage("fr"))
        assertEquals("ru", localization.currentLanguage())
        assertEquals("ru", store.readLanguageTag())
    }

    @Test
    fun `falls back to Chinese original for missing or empty translations`() {
        val localization = AndroidLocalization(catalog, MemoryLocalePreferenceStore("ru"))

        assertEquals(
            "原始中文标签",
            localization.translate("system.not_in_dictionary", "原始中文标签")
        )

        val emptyTranslationCatalog = MapTranslationCatalog(
            mapOf(
                LocaleCode.RUSSIAN to mapOf("home.tab.home" to ""),
                LocaleCode.CHINESE to mapOf("home.tab.home" to "首页")
            )
        )
        val emptyTranslationLocalization = AndroidLocalization(
            emptyTranslationCatalog,
            MemoryLocalePreferenceStore("ru")
        )
        assertEquals(
            "首页",
            emptyTranslationLocalization.translate("home.tab.home", "")
        )
    }

    @Test
    fun `WebView adapter supports Russian and keeps original text on missing keys`() {
        val localization = AndroidLocalization(catalog, MemoryLocalePreferenceStore("ru"))
        val webViewAdapter = WebViewI18nAdapter(localization)

        assertEquals("ru", webViewAdapter.getLanguage())
        assertEquals(
            "Отследить доставку",
            webViewAdapter.getText("delivery", "track", "查看物流")
        )
        assertEquals(
            "原始文字",
            webViewAdapter.getText("not-a-module", "missing", "原始文字")
        )
        assertEquals(
            "原始文字",
            webViewAdapter.getText(" ", "track", "原始文字")
        )
    }

    private class MemoryLocalePreferenceStore(
        private var languageTag: String? = null
    ) : LocalePreferenceStore {
        override fun readLanguageTag(): String? = languageTag

        override fun writeLanguageTag(languageTag: String) {
            this.languageTag = languageTag
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class AndroidLocalizationPersistenceTest {
    @Test
    fun `shared preferences locale survives facade recreation and unsupported tags`() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("pinduoduo_localizer_locale", 0)
            .edit()
            .clear()
            .commit()

        val firstLocalization = AndroidLocalization(context)
        assertEquals("zh", firstLocalization.currentLanguage())
        assertTrue(firstLocalization.selectLanguage("ru-RU"))
        assertEquals("ru", firstLocalization.currentLanguage())

        val recreatedLocalization = AndroidLocalization(context)
        assertEquals("ru", recreatedLocalization.currentLanguage())
        assertFalse(recreatedLocalization.selectLanguage("fr"))
        assertEquals("ru", recreatedLocalization.currentLanguage())

        val localizationAfterRejectedTag = AndroidLocalization(context)
        assertEquals("ru", localizationAfterRejectedTag.currentLanguage())
    }
}
