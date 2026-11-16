package com.replit.pinduoduo.localizer.android

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.replit.pinduoduo.localizer.LocalizationModule
import com.replit.pinduoduo.localizer.TranslationCatalog

/** Small persistence boundary so locale behavior can be tested without an Android app. */
interface LocalePreferenceStore {
    fun readLanguageTag(): String?
    fun writeLanguageTag(languageTag: String)
}

private class SharedPreferencesLocaleStore(context: Context) : LocalePreferenceStore {
    private val preferences = context.getSharedPreferences(
        "pinduoduo_localizer_locale",
        MODE_PRIVATE
    )

    override fun readLanguageTag(): String? = preferences.getString(KEY_LANGUAGE, null)

    override fun writeLanguageTag(languageTag: String) {
        preferences.edit().putString(KEY_LANGUAGE, languageTag).apply()
    }

    private companion object {
        const val KEY_LANGUAGE = "selected_language"
    }
}

/**
 * Android-facing locale facade. The Context constructor uses app assets and
 * SharedPreferences; the injectable constructor is useful to host apps and tests.
 */
class AndroidLocalization(
    catalog: TranslationCatalog,
    private val preferenceStore: LocalePreferenceStore,
    defaultLanguageTag: String = "zh"
) {
    constructor(context: Context, defaultLanguageTag: String = "zh") : this(
        catalog = AndroidAssetTranslationCatalog(context.applicationContext),
        preferenceStore = SharedPreferencesLocaleStore(context.applicationContext),
        defaultLanguageTag = defaultLanguageTag
    )

    private val module = LocalizationModule(
        catalog = catalog,
        initialLanguage = preferenceStore.readLanguageTag() ?: defaultLanguageTag
    )

    fun currentLanguage(): String = module.currentLanguage()

    /** Unsupported tags are rejected and are not persisted. */
    fun selectLanguage(languageTag: String): Boolean {
        if (!module.selectLanguage(languageTag)) return false
        preferenceStore.writeLanguageTag(module.currentLanguage())
        return true
    }

    fun translate(key: String, originalText: String): String =
        module.translate(key, originalText)

    fun format(key: String, originalPattern: String, vararg arguments: Any?): String =
        module.format(key, originalPattern, *arguments)
}
