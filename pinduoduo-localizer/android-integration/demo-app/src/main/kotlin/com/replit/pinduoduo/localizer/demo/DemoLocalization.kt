package com.replit.pinduoduo.localizer.demo

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.replit.pinduoduo.localizer.DictionarySection
import com.replit.pinduoduo.localizer.LocaleCode
import com.replit.pinduoduo.localizer.TranslationCatalog
import com.replit.pinduoduo.localizer.android.AndroidAssetTranslationCatalog
import com.replit.pinduoduo.localizer.android.AndroidLocalization
import com.replit.pinduoduo.localizer.android.LocalePreferenceStore

/**
 * App-only demo copy. These entries are test UI text, not strings extracted
 * from Pinduoduo and not additions to the audited Pinduoduo dictionary.
 */
object DemoString {
    const val APP_TITLE = "demo.app.title"
    const val NOTICE = "demo.notice"
    const val LOCAL_ONLY = "demo.local_only"
    const val LANGUAGE_CHINESE = "demo.language.chinese"
    const val LANGUAGE_RUSSIAN = "demo.language.russian"
    const val CART_TITLE = "demo.cart.title"
    const val CART_EMPTY = "demo.cart.empty"
    const val CART_LOCAL_NOTE = "demo.cart.local_note"
    const val CART_TOTAL = "demo.cart.total"
    const val ADD_TO_CART = "demo.action.add_to_cart"
    const val VIEW_DETAILS = "demo.action.view_details"
    const val ADD = "demo.action.add"
    const val REMOVE = "demo.action.remove"
    const val SEARCH_EMPTY = "demo.search.empty"
    const val PRODUCT_SHIRT = "demo.product.shirt.name"
    const val PRODUCT_SHIRT_DESCRIPTION = "demo.product.shirt.description"
    const val PRODUCT_SHOES = "demo.product.shoes.name"
    const val PRODUCT_SHOES_DESCRIPTION = "demo.product.shoes.description"
    const val PRODUCT_MUG = "demo.product.mug.name"
    const val PRODUCT_MUG_DESCRIPTION = "demo.product.mug.description"
    const val PRODUCT_BAG = "demo.product.bag.name"
    const val PRODUCT_BAG_DESCRIPTION = "demo.product.bag.description"
    const val CATEGORY_CLOTHING = "demo.category.clothing"
    const val CATEGORY_SHOES = "demo.category.shoes"
    const val CATEGORY_HOME = "demo.category.home"
    const val CATEGORY_BAGS = "demo.category.bags"

    internal val russian = mapOf(
        APP_TITLE to "Демо-магазин",
        NOTICE to "Демонстрация — тестовые данные",
        LOCAL_ONLY to "Не подключается к Pinduoduo",
        LANGUAGE_CHINESE to "中文",
        LANGUAGE_RUSSIAN to "Русский",
        CART_TITLE to "Корзина",
        CART_EMPTY to "Корзина пуста",
        CART_LOCAL_NOTE to "Корзина работает только в демо и не отправляется на сервер",
        CART_TOTAL to "Итого",
        ADD_TO_CART to "В корзину",
        VIEW_DETAILS to "Подробнее",
        ADD to "Добавить",
        REMOVE to "Убрать",
        SEARCH_EMPTY to "Тестовые товары не найдены",
        PRODUCT_SHIRT to "Хлопковая рубашка",
        PRODUCT_SHIRT_DESCRIPTION to "Тестовая рубашка. Размер и цвет указаны для демонстрации.",
        PRODUCT_SHOES to "Лёгкие кроссовки",
        PRODUCT_SHOES_DESCRIPTION to "Тестовая обувь для демонстрации карточки товара.",
        PRODUCT_MUG to "Керамическая кружка",
        PRODUCT_MUG_DESCRIPTION to "Тестовая кружка для домашнего использования.",
        PRODUCT_BAG to "Дорожный рюкзак",
        PRODUCT_BAG_DESCRIPTION to "Тестовый рюкзак с условными характеристиками.",
        CATEGORY_CLOTHING to "Одежда",
        CATEGORY_SHOES to "Обувь",
        CATEGORY_HOME to "Для дома",
        CATEGORY_BAGS to "Сумки",
    )

    internal val chinese = mapOf(
        APP_TITLE to "本地商店演示",
        NOTICE to "演示 — 测试数据",
        LOCAL_ONLY to "不连接拼多多",
        LANGUAGE_CHINESE to "中文",
        LANGUAGE_RUSSIAN to "Русский",
        CART_TITLE to "购物车",
        CART_EMPTY to "购物车为空",
        CART_LOCAL_NOTE to "购物车仅用于演示，不会发送到服务器",
        CART_TOTAL to "合计",
        ADD_TO_CART to "加入购物车",
        VIEW_DETAILS to "查看详情",
        ADD to "增加",
        REMOVE to "移除",
        SEARCH_EMPTY to "没有找到演示商品",
        PRODUCT_SHIRT to "棉麻衬衫",
        PRODUCT_SHIRT_DESCRIPTION to "演示衬衫。尺码和颜色仅用于展示。",
        PRODUCT_SHOES to "轻便运动鞋",
        PRODUCT_SHOES_DESCRIPTION to "用于展示商品详情的演示鞋款。",
        PRODUCT_MUG to "陶瓷马克杯",
        PRODUCT_MUG_DESCRIPTION to "用于演示家居用品的测试杯子。",
        PRODUCT_BAG to "旅行双肩包",
        PRODUCT_BAG_DESCRIPTION to "带有示例属性的测试背包。",
        CATEGORY_CLOTHING to "服装",
        CATEGORY_SHOES to "鞋类",
        CATEGORY_HOME to "家居",
        CATEGORY_BAGS to "箱包",
    )
}

private class DemoTranslationCatalog(
    private val pinduoduoCatalog: TranslationCatalog,
) : TranslationCatalog {
    override fun find(locale: LocaleCode, key: String): String? =
        demoEntries(locale)[key] ?: pinduoduoCatalog.find(locale, key)

    override fun entries(
        locale: LocaleCode,
        section: DictionarySection,
    ): Map<String, String> = pinduoduoCatalog.entries(locale, section)

    override fun hasSection(locale: LocaleCode, section: DictionarySection): Boolean =
        pinduoduoCatalog.hasSection(locale, section)

    override fun sourceText(key: String): String? =
        DemoString.chinese[key] ?: pinduoduoCatalog.sourceText(key)

    override fun resourceName(key: String): String? = pinduoduoCatalog.resourceName(key)

    private fun demoEntries(locale: LocaleCode): Map<String, String> = when (locale) {
        LocaleCode.RUSSIAN -> DemoString.russian
        LocaleCode.CHINESE -> DemoString.chinese
    }
}

/** Keeps demo UI strings separate while delegating the existing APK dictionary to the AAR. */
fun createDemoLocalization(context: Context): AndroidLocalization =
    AndroidLocalization(
        catalog = DemoTranslationCatalog(
            AndroidAssetTranslationCatalog(context.applicationContext),
        ),
        preferenceStore = DemoLocalePreferenceStore(context.applicationContext),
        defaultLanguageTag = "zh",
    )

private class DemoLocalePreferenceStore(context: Context) : LocalePreferenceStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)

    override fun readLanguageTag(): String? = preferences.getString(LANGUAGE_KEY, null)

    override fun writeLanguageTag(languageTag: String) {
        preferences.edit().putString(LANGUAGE_KEY, languageTag).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "localizer_demo_preferences"
        const val LANGUAGE_KEY = "selected_language"
    }
}
