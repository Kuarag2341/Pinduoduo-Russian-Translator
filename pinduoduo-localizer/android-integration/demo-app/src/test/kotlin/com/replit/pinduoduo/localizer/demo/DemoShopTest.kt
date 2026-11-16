package com.replit.pinduoduo.localizer.demo

import android.content.Context
import com.replit.pinduoduo.localizer.android.AndroidI18nAdapter
import com.replit.pinduoduo.localizer.android.AndroidLocalization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DemoShopTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `Russian selection uses AAR dictionary and demo translations`() {
        clearLanguage()
        val localization = createDemoLocalization(context)
        val strings = AndroidI18nAdapter(localization)

        assertTrue(localization.selectLanguage("ru"))
        assertEquals(
            "Ищите товары",
            strings.getString("search.input.goods_hint", "搜索你要的商品"),
        )
        assertEquals(
            "Демонстрация — тестовые данные",
            strings.getString(DemoString.NOTICE, "演示 — 测试数据"),
        )
    }

    @Test
    fun `Chinese selection is saved and restored by the Android localization facade`() {
        clearLanguage()
        val first = createDemoLocalization(context)
        assertTrue(first.selectLanguage("ru"))
        assertTrue(first.selectLanguage("zh-CN"))

        val recreated = createDemoLocalization(context)
        assertEquals("zh", recreated.currentLanguage())
        assertEquals(
            "本地商店演示",
            AndroidI18nAdapter(recreated).getString(DemoString.APP_TITLE, "本地商店演示"),
        )
    }

    @Test
    fun `missing translations return the supplied original Chinese text`() {
        clearLanguage()
        val localization = createDemoLocalization(context)
        assertTrue(localization.selectLanguage("ru"))

        assertEquals(
            "未知的演示字段",
            AndroidI18nAdapter(localization).getString("demo.not_in_catalog", "未知的演示字段"),
        )
    }

    @Test
    fun `local cart adds removes and totals demo products without a server`() {
        var cart = DemoCartState()
        cart = cart.add("shirt").add("shirt").add("mug")

        assertEquals(3, cart.itemCount())
        assertEquals(217, cart.totalYuan())

        cart = cart.removeOne("shirt")
        assertEquals(2, cart.itemCount())
        assertEquals(128, cart.totalYuan())

        cart = cart.removeOne("shirt").removeOne("shirt")
        assertEquals(1, cart.itemCount())
        assertEquals(mapOf("mug" to 1), cart.quantities)
    }

    private fun clearLanguage() {
        context.getSharedPreferences("localizer_demo_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
